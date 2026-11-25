package com.bofa.mobile.mock

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.bofa.mobile.model.TransactionPage
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import java.math.BigDecimal

/**
 * Serves the core-banking and payments-hub contracts in-process so the app runs in the emulator
 * without a backend. Enforces HTTP Basic auth and CUSTOMER own-account access like core-banking.
 */
class MockBackendInterceptor @JvmOverloads constructor(
    private val bank: MockBank = shared,
    private val latencyMs: Long = 250
) : Interceptor {

    private val gson = Gson()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (latencyMs > 0) Thread.sleep(latencyMs)
        return try {
            val user = authenticate(request) ?: return error(request, 401, "UNAUTHORIZED", "Bad credentials")
            val body = route(request, user)
            respond(request, if (request.method() == "POST") 201 else 200, gson.toJson(body))
        } catch (e: MockBank.MockException) {
            error(request, e.status, e.code, e.message)
        }
    }

    private fun authenticate(request: Request): MockBank.SeedUser? {
        val header = request.header("Authorization") ?: return null
        if (!header.startsWith("Basic ")) return null
        val decoded = String(Base64.decode(header.substring(6), Base64.NO_WRAP), Charsets.UTF_8)
        val idx = decoded.indexOf(':')
        if (idx < 0) return null
        return bank.authenticate(decoded.substring(0, idx), decoded.substring(idx + 1))
    }

    private fun route(request: Request, user: MockBank.SeedUser): Any {
        val url = request.url()
        val path = url.encodedPath()
        val method = request.method()
        val segments = path.trim('/').split('/')

        if (path.startsWith("/api/v1/payments/")) {
            val cif = requireCif(user, url.queryParameter("cif") ?: bodyField(request, "cif"))
            return when {
                method == "GET" && path.endsWith("/payees") -> bank.payeesFor(cif)
                method == "GET" && path.endsWith("/history") -> bank.paymentsFor(cif)
                method == "POST" && path.endsWith("/bill") -> {
                    val json = body(request)
                    val from = ownAccount(user, json.str("fromAccount"))
                    bank.payBill(cif, from, json.str("payeeId"), amount(json.str("amount")), json.optStr("sendOn"))
                }
                method == "POST" && path.endsWith("/p2p") -> {
                    val json = body(request)
                    val from = ownAccount(user, json.str("fromAccount"))
                    val r = json.getAsJsonObject("recipient")
                    val recipient = r?.get("email")?.asString ?: r?.get("phone")?.asString
                        ?: throw MockBank.MockException(400, "INVALID_RECIPIENT", "Recipient is required")
                    bank.sendP2p(cif, from, recipient, amount(json.str("amount")))
                }
                else -> notFound()
            }
        }

        // /api/v1/<resource>/...
        val rest = segments.drop(2)
        return when {
            method == "GET" && rest == listOf("customers", "me") ->
                bank.customer(user.cif ?: throw MockBank.MockException(404, "CUSTOMER_NOT_FOUND", "No customer profile"))
            method == "GET" && rest.size == 2 && rest[0] == "customers" -> bank.customer(requireCif(user, rest[1]))
            method == "GET" && rest.size == 3 && rest[0] == "customers" && rest[2] == "accounts" ->
                bank.accountsFor(requireCif(user, rest[1]))
            method == "GET" && rest.size == 2 && rest[0] == "accounts" -> bank.account(ownAccount(user, rest[1]))
            method == "GET" && rest.size == 3 && rest[0] == "accounts" && rest[2] == "transactions" ->
                page(ownAccount(user, rest[1]), url.queryParameter("from"), url.queryParameter("to"),
                    url.queryParameter("page")?.toInt() ?: 0, url.queryParameter("size")?.toInt() ?: 50)
            method == "GET" && rest == listOf("transactions", "search") -> {
                val acct = url.queryParameter("accountNumber")?.let { ownAccount(user, it) }
                bank.search(url.queryParameter("q") ?: "", acct, user.cif ?: "")
            }
            method == "POST" && rest == listOf("transfers") -> {
                val json = body(request)
                bank.transfer(ownAccount(user, json.str("fromAccount")), ownAccount(user, json.str("toAccount")),
                    amount(json.str("amount")), json.optStr("memo"), json.optStr("idempotencyKey"))
            }
            else -> notFound()
        }
    }

    private fun page(account: String, from: String?, to: String?, page: Int, size: Int): TransactionPage {
        val filtered = bank.history(account).filter {
            (from == null || it.businessDate >= from) && (to == null || it.businessDate <= to)
        }
        val start = (page * size).coerceAtMost(filtered.size)
        val end = (start + size).coerceAtMost(filtered.size)
        return TransactionPage(filtered.subList(start, end), page, size, filtered.size.toLong())
    }

    private fun requireCif(user: MockBank.SeedUser, cif: String?): String {
        if (cif == null) throw MockBank.MockException(400, "CIF_REQUIRED", "cif is required")
        if (user.role == "CUSTOMER" && user.cif != cif) throw MockBank.MockException(403, "FORBIDDEN", "Access denied")
        return cif
    }

    private fun ownAccount(user: MockBank.SeedUser, accountNumber: String): String {
        val account = bank.account(accountNumber)
        if (user.role == "CUSTOMER" && account.cif != user.cif) {
            throw MockBank.MockException(403, "FORBIDDEN", "Access denied")
        }
        return accountNumber
    }

    private fun amount(raw: String): BigDecimal = try {
        BigDecimal(raw)
    } catch (e: NumberFormatException) {
        throw MockBank.MockException(400, "INVALID_AMOUNT", "Amount is not a valid decimal")
    }

    private fun body(request: Request): JsonObject {
        val buffer = Buffer()
        request.body()?.writeTo(buffer)
        return gson.fromJson(buffer.readUtf8(), JsonObject::class.java) ?: JsonObject()
    }

    private fun bodyField(request: Request, name: String): String? =
        if (request.method() == "POST") body(request).optStr(name) else null

    private fun JsonObject.str(name: String): String =
        optStr(name) ?: throw MockBank.MockException(400, "MISSING_FIELD", "$name is required")

    private fun JsonObject.optStr(name: String): String? =
        get(name)?.takeIf { !it.isJsonNull }?.asString

    private fun notFound(): Nothing = throw MockBank.MockException(404, "NOT_FOUND", "No handler")

    private fun error(request: Request, status: Int, code: String, message: String): Response =
        respond(request, status, gson.toJson(mapOf("code" to code, "message" to message)))

    private fun respond(request: Request, status: Int, json: String): Response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(status)
            .message(if (status < 400) "OK" else "Error")
            .body(ResponseBody.create(JSON, json))
            .build()

    companion object {
        private val JSON = MediaType.parse("application/json; charset=utf-8")

        @JvmStatic
        val shared: MockBank by lazy { MockBank() }
    }
}
