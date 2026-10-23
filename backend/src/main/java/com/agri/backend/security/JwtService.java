package com.agri.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal standards-compliant JWT (HS256) - no extra library needed.
 * Token = base64url(header).base64url(payload).base64url(HMAC-SHA256(secret, header.payload))
 * Payload carries only: uid (user id), role, iat, exp. Everything else is looked up from the database
 * on each request, so disabling an account or changing a role takes effect immediately.
 */
@Service
public class JwtService {

    public record Claims(long userId, String role, long expiresAtEpochSec) {}

    private static final String HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    private static final Pattern UID = Pattern.compile("\"uid\":(\\d+)");
    private static final Pattern ROLE = Pattern.compile("\"role\":\"([A-Z_]+)\"");
    private static final Pattern EXP = Pattern.compile("\"exp\":(\\d+)");

    private final byte[] secret;
    private final long expirySeconds;
    private final boolean ephemeralSecret;

    public JwtService(@Value("${app.jwt.secret:}") String configuredSecret,
                      @Value("${app.jwt.expiry-hours:12}") long expiryHours) {
        if (configuredSecret == null || configuredSecret.isBlank()) {
            byte[] random = new byte[48];
            new SecureRandom().nextBytes(random);
            this.secret = random;
            this.ephemeralSecret = true;
        } else {
            this.secret = configuredSecret.getBytes(StandardCharsets.UTF_8);
            this.ephemeralSecret = false;
        }
        this.expirySeconds = Math.max(1, expiryHours) * 3600L;
    }

    /** True when no JWT_SECRET was configured: logins then stop working after every server restart. */
    public boolean isEphemeralSecret() {
        return ephemeralSecret;
    }

    public String createToken(long userId, String role) {
        long now = System.currentTimeMillis() / 1000L;
        String payload = "{\"uid\":" + userId + ",\"role\":\"" + role + "\",\"iat\":" + now
                + ",\"exp\":" + (now + expirySeconds) + "}";
        String signingInput = b64(HEADER_JSON.getBytes(StandardCharsets.UTF_8)) + "."
                + b64(payload.getBytes(StandardCharsets.UTF_8));
        return signingInput + "." + b64(hmac(signingInput));
    }

    public Optional<Claims> parse(String token) {
        try {
            if (token == null) return Optional.empty();
            String[] parts = token.split("\\.");
            if (parts.length != 3) return Optional.empty();

            byte[] expected = hmac(parts[0] + "." + parts[1]);
            byte[] actual = Base64.getUrlDecoder().decode(parts[2]);
            if (!MessageDigest.isEqual(expected, actual)) return Optional.empty();

            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            Matcher uid = UID.matcher(payload);
            Matcher role = ROLE.matcher(payload);
            Matcher exp = EXP.matcher(payload);
            if (!uid.find() || !role.find() || !exp.find()) return Optional.empty();

            long expiresAt = Long.parseLong(exp.group(1));
            if (expiresAt < System.currentTimeMillis() / 1000L) return Optional.empty();
            return Optional.of(new Claims(Long.parseLong(uid.group(1)), role.group(1), expiresAt));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private byte[] hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Could not sign token", e);
        }
    }

    private static String b64(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
