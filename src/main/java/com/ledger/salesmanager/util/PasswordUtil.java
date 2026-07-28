package com.ledger.salesmanager.util;

import org.mindrot.jbcrypt.BCrypt;

/** Thin wrapper around jBCrypt so the rest of the app never touches the algorithm directly. */
public class PasswordUtil {

    public static String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }

    public static boolean matches(String plainPassword, String hash) {
        try {
            return BCrypt.checkpw(plainPassword, hash);
        } catch (IllegalArgumentException e) {
            // Malformed hash in DB — fail closed.
            return false;
        }
    }
}
