package com.ledger.salesmanager.service;

import com.ledger.salesmanager.dao.LoginHistoryDAO;
import com.ledger.salesmanager.dao.UserDAO;
import com.ledger.salesmanager.dao.ActivityLogDAO;
import com.ledger.salesmanager.model.Role;
import com.ledger.salesmanager.model.User;
import com.ledger.salesmanager.util.PasswordUtil;
import com.ledger.salesmanager.util.SessionManager;

import java.util.Optional;

/**
 * Handles username/password verification (step 1 of login). The Owner
 * role additionally requires OtpService.sendLoginOtp / verifyLoginOtp
 * (step 2) before SessionManager.login() is actually called — see
 * LoginController / OtpVerificationController for the orchestration.
 */
public class AuthService {

    private final UserDAO userDAO = new UserDAO();
    private final LoginHistoryDAO loginHistoryDAO = new LoginHistoryDAO();
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();

    public static class AuthResult {
        public final boolean success;
        public final String errorMessage;
        public final User user;
        public final boolean requiresOtp;

        private AuthResult(boolean success, String errorMessage, User user, boolean requiresOtp) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.user = user;
            this.requiresOtp = requiresOtp;
        }

        static AuthResult fail(String msg) { return new AuthResult(false, msg, null, false); }
        static AuthResult okDirect(User u) { return new AuthResult(true, null, u, false); }
        static AuthResult okNeedsOtp(User u) { return new AuthResult(true, null, u, true); }
    }

    /** Step 1: verify credentials. Owners are flagged as requiring OTP before session starts. */
    public AuthResult verifyCredentials(String username, String password) {
        Optional<User> maybeUser = userDAO.findByUsername(username.trim());
        if (maybeUser.isEmpty()) {
            return AuthResult.fail("Username ya password ghalat hai.");
        }
        User user = maybeUser.get();
        if (!user.isActive()) {
            return AuthResult.fail("Ye account deactivate ho chuka hai. Owner se rabta karein.");
        }
        if (!PasswordUtil.matches(password, user.getPasswordHash())) {
            loginHistoryDAO.record(user.getId(), "FAILED", "local");
            return AuthResult.fail("Username ya password ghalat hai.");
        }
        if (user.getRole() == Role.OWNER) {
            return AuthResult.okNeedsOtp(user);
        }
        return AuthResult.okDirect(user);
    }

    /** Called after OTP is verified (Owner) or immediately (Salesperson) to finalize login. */
    public void finalizeLogin(User user) {
        SessionManager.getInstance().login(user);
        userDAO.updateLastLogin(user.getId());
        loginHistoryDAO.record(user.getId(), "SUCCESS", "local");
        activityLogDAO.log(user.getId(), "LOGIN", user.getRole() + " logged in");
    }

    public void logout() {
        User current = SessionManager.getInstance().getCurrentUser();
        if (current != null) {
            activityLogDAO.log(current.getId(), "LOGOUT", current.getRole() + " logged out");
        }
        SessionManager.getInstance().logout();
    }

    // ---- Simple RBAC guards used throughout the controllers ----
    public static boolean canManageProducts()   { return hasRole(Role.OWNER); }
    public static boolean canManageUsers()      { return hasRole(Role.OWNER); }
    public static boolean canViewWholesale()    { return hasRole(Role.OWNER) || hasRole(Role.SALESPERSON); }
    public static boolean canRecordSales()      { return hasRole(Role.OWNER) || hasRole(Role.SALESPERSON); }
    public static boolean canAccessReports()    { return hasRole(Role.OWNER); }
    public static boolean canAccessSettings()   { return hasRole(Role.OWNER); }
    public static boolean canViewAuditLog()     { return hasRole(Role.OWNER); }

    private static boolean hasRole(Role role) {
        User u = SessionManager.getInstance().getCurrentUser();
        return u != null && u.getRole() == role;
    }
}
