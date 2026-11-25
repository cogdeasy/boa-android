package com.bofa.mobile.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Date helpers. All platform dates are America/New_York business dates. */
public final class BankDates {

    public static final TimeZone BANK_TZ = TimeZone.getTimeZone("America/New_York");
    private static final String WIRE_DATE = "yyyy-MM-dd";
    private static final String WIRE_TIMESTAMP = "yyyy-MM-dd'T'HH:mm:ss";

    private BankDates() {
    }

    public static String today() {
        return formatDate(new Date());
    }

    public static String daysAgo(int days) {
        Calendar c = Calendar.getInstance(BANK_TZ);
        c.add(Calendar.DAY_OF_YEAR, -days);
        return formatDate(c.getTime());
    }

    public static String formatDate(Date date) {
        SimpleDateFormat f = new SimpleDateFormat(WIRE_DATE, Locale.US);
        f.setTimeZone(BANK_TZ);
        return f.format(date);
    }

    public static String formatTimestamp(Date date) {
        SimpleDateFormat f = new SimpleDateFormat(WIRE_TIMESTAMP, Locale.US);
        f.setTimeZone(BANK_TZ);
        return f.format(date);
    }

    /** "2026-11-02" -> "Nov 2, 2026" */
    public static String display(String wireDate) {
        if (wireDate == null) {
            return "";
        }
        try {
            SimpleDateFormat in = new SimpleDateFormat(WIRE_DATE, Locale.US);
            in.setTimeZone(BANK_TZ);
            Date d = in.parse(wireDate.length() > 10 ? wireDate.substring(0, 10) : wireDate);
            SimpleDateFormat out = new SimpleDateFormat("MMM d, yyyy", Locale.US);
            out.setTimeZone(BANK_TZ);
            return out.format(d);
        } catch (ParseException e) {
            return wireDate;
        }
    }
}
