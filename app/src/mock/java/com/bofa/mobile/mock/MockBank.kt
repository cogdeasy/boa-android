package com.bofa.mobile.mock

import com.bofa.mobile.model.Account
import com.bofa.mobile.model.Customer
import com.bofa.mobile.model.Payee
import com.bofa.mobile.model.Payment
import com.bofa.mobile.model.Transaction
import com.bofa.mobile.util.AccountNumbers
import com.bofa.mobile.util.BankDates
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar
import java.util.Random

/**
 * In-memory copy of the core-banking local-profile seed data (users, customers, accounts) plus a
 * generated, deterministic transaction history. State lives for the life of the process.
 */
class MockBank {

    data class SeedUser(val username: String, val password: String, val role: String, val cif: String?)

    class MockException(val status: Int, val code: String, override val message: String) : RuntimeException(message)

    val users = listOf(
        SeedUser("dana.whitfield", "Welcome1!", "CUSTOMER", "100000001"),
        SeedUser("marcus.oyelaran", "Welcome1!", "CUSTOMER", "100000002"),
        SeedUser("teller01", "Teller#2019", "TELLER", null),
        SeedUser("ops.batch", "Ops#Batch1", "OPS", null)
    )

    private val customers = mapOf(
        "100000001" to Customer("100000001", "Dana", "Whitfield", "dana.whitfield@example.com", "PREFERRED", "VERIFIED"),
        "100000002" to Customer("100000002", "Marcus", "Oyelaran", "m.oyelaran@example.com", "STANDARD", "VERIFIED")
    )

    private val accounts = linkedMapOf<String, Account>()
    private val transactions = hashMapOf<String, MutableList<Transaction>>()
    private val payments = hashMapOf<String, MutableList<Payment>>()
    private val idempotency = hashMapOf<String, Any>()
    private var nextTxnId = 99000L
    private var transferSeq = 230
    private var paymentSeq = 5100

    private val payees = mapOf(
        "100000001" to listOf(
            Payee("PY-1001", "Con Edison", "UTILITY", "****7781"),
            Payee("PY-1002", "Verizon Wireless", "TELECOM", "****0412"),
            Payee("PY-1003", "Bank of America Visa Signature", "CREDIT_CARD", "****6630"),
            Payee("PY-1004", "Park Slope Property Mgmt", "RENT", "****2290")
        ),
        "100000002" to listOf(
            Payee("PY-2001", "PSEG Long Island", "UTILITY", "****1187"),
            Payee("PY-2002", "Spectrum", "TELECOM", "****9054")
        )
    )

    init {
        addAccount("104000123455", "100000001", "DDA-CHK", "Advantage Checking", "4812.33", "100.00", "2014-03-11")
        addAccount("104000123463", "100000001", "DDA-SAV", "Advantage Savings", "22150.00", "0.00", "2014-03-11")
        addAccount("211000048218", "100000002", "DDA-CHK", "Advantage Checking", "1045.12", "0.00", "2019-07-22")
        addAccount("211000048226", "100000002", "DDA-SAV", "Advantage Savings", "9300.00", "0.00", "2019-07-22")
        addAccount("307000555010", null, "GL-SUSP", "Suspense GL", "0.00", "0.00", "2009-01-02")
        accounts.values.filter { it.cif != null }.forEach { transactions[it.accountNumber] = generateHistory(it) }
    }

    fun authenticate(username: String?, password: String?): SeedUser? =
        users.firstOrNull { it.username == username && it.password == password }

    fun customer(cif: String): Customer =
        customers[cif] ?: throw MockException(404, "CUSTOMER_NOT_FOUND", "Customer not found")

    fun accountsFor(cif: String): List<Account> = accounts.values.filter { it.cif == cif }

    fun account(accountNumber: String): Account =
        accounts[accountNumber] ?: throw MockException(404, "ACCOUNT_NOT_FOUND", "Account not found")

    fun history(accountNumber: String): List<Transaction> = transactions[accountNumber] ?: emptyList()

    fun search(query: String, accountNumber: String?, cif: String): List<Transaction> {
        val scope = if (accountNumber != null) listOf(accountNumber) else accountsFor(cif).map { it.accountNumber }
        val q = query.trim().toUpperCase()
        return scope.flatMap { history(it) }.filter { it.description.toUpperCase().contains(q) }
    }

    fun payeesFor(cif: String): List<Payee> = payees[cif] ?: emptyList()

    fun paymentsFor(cif: String): List<Payment> = payments[cif] ?: emptyList()

    @Synchronized
    fun transfer(from: String, to: String, amount: BigDecimal, memo: String?, key: String?, channel: String = "TRANSFER",
                 description: String? = null): Map<String, Any> {
        if (key != null && idempotency.containsKey(key)) {
            throw MockException(409, "DUPLICATE", "A transfer with this idempotency key was already posted")
        }
        val source = account(from)
        val target = account(to)
        if (amount.signum() <= 0) throw MockException(400, "INVALID_AMOUNT", "Amount must be greater than zero")
        if (amount > BigDecimal("10000.00")) throw MockException(422, "LIMIT_EXCEEDED", "Amount exceeds your daily transfer limit")
        if (BigDecimal(source.availableBalance) < amount) throw MockException(422, "NSF", "Insufficient available funds")

        val now = Calendar.getInstance(BankDates.BANK_TZ).time
        post(source, amount.negate(), channel, description ?: "ONLINE TRANSFER TO ...${AccountNumbers.lastFour(to)} ${memo ?: ""}".trim(), now)
        if (target.cif != null) {
            post(target, amount, channel, description?.let { "TRANSFER FROM ...${AccountNumbers.lastFour(from)}" }
                ?: "ONLINE TRANSFER FROM ...${AccountNumbers.lastFour(from)} ${memo ?: ""}".trim(), now)
        } else {
            target.ledgerBalance = money(BigDecimal(target.ledgerBalance).add(amount))
            target.availableBalance = money(BigDecimal(target.availableBalance).add(amount))
        }
        transferSeq++
        val result = mapOf(
            "transferId" to "TRF-${BankDates.today().replace("-", "")}-${transferSeq.toString().padStart(6, '0')}",
            "status" to "POSTED",
            "postedAt" to BankDates.formatTimestamp(now),
            "businessDate" to BankDates.today()
        )
        if (key != null) idempotency[key] = result
        return result
    }

    @Synchronized
    fun payBill(cif: String, from: String, payeeId: String, amount: BigDecimal, sendOn: String?): Map<String, Any> {
        val payee = payeesFor(cif).firstOrNull { it.payeeId == payeeId }
            ?: throw MockException(404, "PAYEE_NOT_FOUND", "Payee not found")
        transfer(from, SUSPENSE_GL, amount, null, null, "BILLPAY", "BILL PAY ${payee.name.toUpperCase()}")
        val id = "BP-${++paymentSeq}"
        record(cif, Payment(id, "BILL", from, payee.name, money(amount), "SCHEDULED", sendOn ?: BankDates.today()))
        return mapOf("paymentId" to id, "status" to "SCHEDULED")
    }

    @Synchronized
    fun sendP2p(cif: String, from: String, recipient: String, amount: BigDecimal): Map<String, Any> {
        transfer(from, SUSPENSE_GL, amount, null, null, "P2P", "P2P SEND TO ${recipient.toUpperCase()}")
        val id = "P2P-${++paymentSeq}"
        record(cif, Payment(id, "P2P", from, recipient, money(amount), "SENT", BankDates.today()))
        return mapOf("paymentId" to id, "status" to "SENT")
    }

    private fun record(cif: String, payment: Payment) {
        payments.getOrPut(cif) { mutableListOf() }.add(0, payment)
    }

    private fun post(account: Account, amount: BigDecimal, channel: String, description: String, at: java.util.Date) {
        val ledger = BigDecimal(account.ledgerBalance).add(amount)
        account.ledgerBalance = money(ledger)
        account.availableBalance = money(BigDecimal(account.availableBalance).add(amount))
        val txn = Transaction(
            ++nextTxnId, account.accountNumber, BankDates.formatTimestamp(at), BankDates.formatDate(at),
            if (amount.signum() < 0) "DEBIT" else "CREDIT", channel, money(amount), money(ledger), description, "POSTED"
        )
        transactions.getOrPut(account.accountNumber) { mutableListOf() }.add(0, txn)
    }

    private fun addAccount(number: String, cif: String?, product: String, nickname: String, ledger: String,
                           hold: String, opened: String) {
        val a = Account()
        a.accountNumber = number
        a.maskedNumber = AccountNumbers.mask(number)
        a.cif = cif
        a.productCode = product
        a.nickname = nickname
        a.status = "OPEN"
        a.ledgerBalance = ledger
        a.holdAmount = hold
        a.availableBalance = money(BigDecimal(ledger).subtract(BigDecimal(hold)))
        a.openedOn = opened
        accounts[number] = a
    }

    /** Walks backwards from today's ledger balance so running balances reconcile. */
    private fun generateHistory(account: Account): MutableList<Transaction> {
        val random = Random(account.accountNumber.toLong())
        val result = mutableListOf<Transaction>()
        var balance = BigDecimal(account.ledgerBalance)
        val cal = Calendar.getInstance(BankDates.BANK_TZ)
        val count = if (account.isSavings) 24 else 140
        for (i in 0 until count) {
            cal.add(Calendar.HOUR_OF_DAY, -(6 + random.nextInt(if (account.isSavings) 300 else 30)))
            val (desc, channel, amount) = if (account.isSavings) savingsEntry(random, i) else checkingEntry(random)
            val at = cal.time
            result.add(Transaction(
                ++nextTxnId, account.accountNumber, BankDates.formatTimestamp(at), BankDates.formatDate(at),
                if (amount.signum() < 0) "DEBIT" else "CREDIT", channel, money(amount), money(balance), desc, "POSTED"
            ))
            balance = balance.subtract(amount)
        }
        return result
    }

    private fun checkingEntry(r: Random): Triple<String, String, BigDecimal> {
        val roll = r.nextInt(100)
        return when {
            roll < 6 -> Triple("BOFA PAYROLL DIR DEP", "ACH", BigDecimal("2864.17"))
            roll < 22 -> Triple(COFFEE[r.nextInt(COFFEE.size)], "CARD", amt(r, 3, 14).negate())
            roll < 44 -> Triple(GROCERY[r.nextInt(GROCERY.size)], "CARD", amt(r, 18, 160).negate())
            roll < 60 -> Triple(DINING[r.nextInt(DINING.size)], "CARD", amt(r, 12, 95).negate())
            roll < 70 -> Triple(TRANSIT[r.nextInt(TRANSIT.size)], "CARD", amt(r, 3, 60).negate())
            roll < 78 -> Triple(RETAIL[r.nextInt(RETAIL.size)], "CARD", amt(r, 15, 240).negate())
            roll < 83 -> Triple("ATM WITHDRAWAL 1104 FLATBUSH AVE", "ATM", BigDecimal(listOf(40, 60, 100, 200)[r.nextInt(4)]).setScale(2).negate())
            roll < 88 -> Triple("BILL PAY CON EDISON", "BILLPAY", amt(r, 60, 180).negate())
            roll < 92 -> Triple("P2P SEND TO J.ALVAREZ", "P2P", amt(r, 10, 120).negate())
            roll < 95 -> Triple("MOBILE CHECK DEPOSIT", "BRANCH", amt(r, 50, 600))
            roll < 97 -> Triple("ONLINE TRANSFER TO ...3463", "TRANSFER", BigDecimal("250.00").negate())
            else -> Triple("ACH DEBIT GEICO AUTO INS", "ACH", BigDecimal("142.60").negate())
        }
    }

    private fun savingsEntry(r: Random, i: Int): Triple<String, String, BigDecimal> = when {
        i % 4 == 0 -> Triple("INTEREST PAYMENT", "INTEREST", amt(r, 8, 24))
        i % 7 == 3 -> Triple("ONLINE TRANSFER TO ...3455", "TRANSFER", BigDecimal("500.00").negate())
        else -> Triple("ONLINE TRANSFER FROM ...3455", "TRANSFER", BigDecimal("250.00"))
    }

    private fun amt(r: Random, min: Int, max: Int): BigDecimal =
        BigDecimal(min + r.nextInt(max - min) + r.nextInt(100) / 100.0).setScale(2, RoundingMode.HALF_EVEN)

    companion object {
        const val SUSPENSE_GL = "307000555010"

        fun money(v: BigDecimal): String = v.setScale(2, RoundingMode.HALF_EVEN).toPlainString()

        private val COFFEE = listOf("STARBUCKS STORE #08812", "BLUE BOTTLE COFFEE BKLYN", "DUNKIN #344519", "VARSITY COFFEE CO")
        private val GROCERY = listOf("WHOLE FDS MKT #10233", "TRADER JOE'S #558", "KEY FOOD 7TH AVE", "WEGMANS #112")
        private val DINING = listOf("SHAKE SHACK #0190", "CHIPOTLE 2291", "JOE'S PIZZA CARMINE", "SWEETGREEN FLATIRON")
        private val TRANSIT = listOf("MTA*NYCT PAYGO", "UBER TRIP", "LYFT RIDE", "SHELL OIL 57442")
        private val RETAIL = listOf("TARGET T-1263", "AMAZON MKTPLACE PMTS", "CVS/PHARMACY #02211", "HOME DEPOT #6177")
    }
}
