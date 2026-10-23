package com.agri.backend.security;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/** Blocks password guessing: 5 failed sign-ins for the same email+role within 10 minutes -> temporary lock. */
@Component
public class LoginThrottle {
    private static final int MAX_FAILURES = 5;
    private static final long WINDOW_MS = 10 * 60 * 1000L;

    private final ConcurrentHashMap<String, Deque<Long>> failures = new ConcurrentHashMap<>();

    public boolean isLocked(String key) {
        Deque<Long> q = failures.get(key);
        if (q == null) return false;
        synchronized (q) {
            prune(q);
            return q.size() >= MAX_FAILURES;
        }
    }

    public void recordFailure(String key) {
        Deque<Long> q = failures.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (q) {
            prune(q);
            q.addLast(System.currentTimeMillis());
        }
    }

    public void clear(String key) {
        failures.remove(key);
    }

    private void prune(Deque<Long> q) {
        long cutoff = System.currentTimeMillis() - WINDOW_MS;
        while (!q.isEmpty() && q.peekFirst() < cutoff) q.removeFirst();
    }
}
