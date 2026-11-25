package com.northbridge.mobile.util

/** 12-digit DDA numbers: 3-digit branch, 8-digit serial, Luhn check digit. */
object AccountNumbers {

    @JvmStatic
    fun mask(accountNumber: String?): String {
        if (accountNumber == null || accountNumber.length < 4) return "****"
        return "*".repeat(accountNumber.length - 4) + accountNumber.takeLast(4)
    }

    @JvmStatic
    fun lastFour(accountNumber: String?): String =
        if (accountNumber == null || accountNumber.length < 4) "" else accountNumber.takeLast(4)

    @JvmStatic
    fun isValid(accountNumber: String?): Boolean {
        if (accountNumber == null || accountNumber.length != 12 || !accountNumber.all { it.isDigit() }) return false
        var sum = 0
        accountNumber.reversed().forEachIndexed { i, c ->
            var d = c - '0'
            if (i % 2 == 1) {
                d *= 2
                if (d > 9) d -= 9
            }
            sum += d
        }
        return sum % 10 == 0
    }

    @JvmStatic
    fun branch(accountNumber: String): String = accountNumber.substring(0, 3)
}
