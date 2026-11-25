package com.northbridge.mobile.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AccountNumbersTest {

    @Test
    public void masksAllButLastFourDigits() {
        assertEquals("********3455", AccountNumbers.mask("104000123455"));
    }

    @Test
    public void seedAccountsPassLuhnCheck() {
        assertTrue(AccountNumbers.isValid("104000123455"));
        assertTrue(AccountNumbers.isValid("211000048226"));
    }

    @Test
    public void rejectsBadCheckDigit() {
        assertFalse(AccountNumbers.isValid("104000123456"));
    }
}
