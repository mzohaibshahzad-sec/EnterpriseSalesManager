package com.ledger.salesmanager.util;

import com.ledger.salesmanager.model.User;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;

/**
 * Holds the currently authenticated user for the running application
 * instance, and enforces an idle-timeout automatic logout — a required
 * enterprise security control.
 */
public class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();
    public static SessionManager getInstance() { return INSTANCE; }

    private User currentUser;
    private Instant lastActivity = Instant.now();
    private static final Duration IDLE_TIMEOUT = Duration.ofMinutes(10);
    private Consumer<Void> onAutoLogout;

    private SessionManager() {}

    public void login(User user) {
        this.currentUser = user;
        touch();
    }

    public void logout() {
        this.currentUser = null;
    }

    public User getCurrentUser() { return currentUser; }
    public boolean isLoggedIn() { return currentUser != null; }

    public void touch() { lastActivity = Instant.now(); }

    public boolean isIdleTimedOut() {
        return isLoggedIn() && Duration.between(lastActivity, Instant.now()).compareTo(IDLE_TIMEOUT) > 0;
    }

    public void setOnAutoLogout(Consumer<Void> callback) { this.onAutoLogout = callback; }

    public void checkAndHandleIdleTimeout() {
        if (isIdleTimedOut()) {
            logout();
            if (onAutoLogout != null) onAutoLogout.accept(null);
        }
    }
}
