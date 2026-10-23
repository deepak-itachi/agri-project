package com.agri.backend.security;

import com.agri.backend.model.User;
import com.agri.backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

/**
 * Runs before every /api/** call: reads "Authorization: Bearer <jwt>", loads the user from the database,
 * checks the role against AccessRules and (for learners) that they only touch their own farm profile.
 * Controllers then read the signed-in user with AuthInterceptor.currentUser(request).
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_USER = "authUser";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthInterceptor(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    public static User currentUser(HttpServletRequest request) {
        Object o = request.getAttribute(ATTR_USER);
        return o instanceof User ? (User) o : null;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true; // CORS pre-flight

        String path = request.getRequestURI();
        AccessRules.Rule rule = AccessRules.forRequest(request.getMethod(), path);
        User user = resolveUser(request);
        if (user != null) request.setAttribute(ATTR_USER, user);

        if (rule.kind() == AccessRules.Kind.PUBLIC) return true;

        if (user == null) return deny(response, 401, "Please sign in to continue.");
        if (!user.isEnabled()) return deny(response, 403, "This account has been disabled. Please contact the administrator.");

        String role = user.getRole().name();
        if (!rule.allows(role)) return deny(response, 403, "Your " + role.toLowerCase() + " account cannot access this area.");

        // Learners may only read/write their own farm profile data.
        if ("FARMER".equals(role)) {
            Long pathFarmerId = AccessRules.farmerIdInPath(path);
            if (pathFarmerId != null && !Objects.equals(pathFarmerId, user.getFarmerId())) {
                return deny(response, 403, "You can only access your own farm data.");
            }
            String paramFarmerId = request.getParameter("farmerId");
            if (paramFarmerId != null && !paramFarmerId.equals(String.valueOf(user.getFarmerId()))) {
                return deny(response, 403, "You can only upload data for your own farm.");
            }
        }
        return true;
    }

    private User resolveUser(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return null;
        Optional<JwtService.Claims> claims = jwtService.parse(header.substring(7).trim());
        if (claims.isEmpty()) return null;
        return userRepository.findById(claims.get().userId()).orElse(null);
    }

    private boolean deny(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"status\":" + status + ",\"message\":\"" + message.replace("\"", "'") + "\"}");
        return false;
    }
}
