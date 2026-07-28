package com.ledger.salesmanager.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public class InvoiceNumberGenerator {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** e.g. INV-20260704153012-482 — timestamp + random suffix avoids collisions
     *  without needing an extra DB round-trip to peek at the last invoice number. */
    public static String generate() {
        String stamp = LocalDateTime.now().format(FMT);
        int suffix = ThreadLocalRandom.current().nextInt(100, 999);
        return "INV-" + stamp + "-" + suffix;
    }
}
