package com.agri.backend.security;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The single place that decides who may call which API path. Pure functions (no Spring) so it is easy to read and test.
 *
 *   /api/auth/login, /api/auth/register/**  -> public
 *   /api/admin/**                           -> ADMIN
 *   /api/seller/**                          -> SELLER
 *   /api/buyer/**, /api/orders, /api/payments/** -> BUYER
 *   /api/farmers/**, /api/farm-data/**, /api/growth-logs/** -> FARMER (own data only) or ADMIN
 *   any other GET (crops, crop images, marketplace browse, weather, mandi prices) -> public
 *   any other write -> ADMIN
 */
public final class AccessRules {
    private AccessRules() {}

    public enum Kind { PUBLIC, ANY_AUTHENTICATED, ROLES }

    public record Rule(Kind kind, Set<String> roles) {
        public boolean allows(String role) {
            return kind != Kind.ROLES || roles.contains(role);
        }
    }

    private static final Rule PUBLIC = new Rule(Kind.PUBLIC, Set.of());
    private static final Rule ANY = new Rule(Kind.ANY_AUTHENTICATED, Set.of());
    private static final Rule ADMIN = new Rule(Kind.ROLES, Set.of("ADMIN"));
    private static final Rule SELLER = new Rule(Kind.ROLES, Set.of("SELLER"));
    private static final Rule BUYER = new Rule(Kind.ROLES, Set.of("BUYER"));
    private static final Rule LEARNER = new Rule(Kind.ROLES, Set.of("FARMER", "ADMIN"));

    /** Paths that name a specific farm profile, e.g. /api/farmers/7 or /api/farm-data/soil-report/farmer/7 */
    private static final Pattern FARMER_SCOPED =
            Pattern.compile("^/api/(?:farmers|growth-logs/farmer|farm-data/[a-z-]+/farmer)/(\\d+)$");

    public static Rule forRequest(String method, String path) {
        if (path.startsWith("/api/auth/")) {
            if (path.equals("/api/auth/me") || path.equals("/api/auth/logout")) return ANY;
            return PUBLIC; // login + register
        }
        if (path.startsWith("/api/admin/") || path.equals("/api/admin")) return ADMIN;
        if (path.startsWith("/api/seller/") || path.equals("/api/seller")) return SELLER;
        if (path.startsWith("/api/buyer/") || path.equals("/api/buyer")) return BUYER;
        if (path.equals("/api/payments/config")) return PUBLIC;
        if (path.equals("/api/orders") || path.startsWith("/api/orders/") || path.startsWith("/api/payments/")) return BUYER;
        if (path.equals("/api/farmers") || path.startsWith("/api/farmers/")
                || path.startsWith("/api/farm-data/") || path.startsWith("/api/growth-logs")) return LEARNER;
        if ("GET".equalsIgnoreCase(method)) return PUBLIC;
        if (path.equals("/api/profit/estimate")) return PUBLIC;
        return ADMIN;
    }

    /** If this path is scoped to one farm profile, returns its id; otherwise null. */
    public static Long farmerIdInPath(String path) {
        Matcher m = FARMER_SCOPED.matcher(path);
        return m.matches() ? Long.parseLong(m.group(1)) : null;
    }
}
