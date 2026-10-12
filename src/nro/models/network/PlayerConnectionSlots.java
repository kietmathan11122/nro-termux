package nro.models.network;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Counts game logins, not auxiliary resource sockets. */
public final class PlayerConnectionSlots {
    private static final Map<String, Set<Object>> SLOTS = new HashMap<>();
    private PlayerConnectionSlots() { }

    public static synchronized boolean acquire(String ip, Object session) {
        Set<Object> sessions = SLOTS.computeIfAbsent(ip, ignored -> new HashSet<>());
        if (sessions.contains(session)) return true;
        if (sessions.size() >= 2) return false;
        sessions.add(session);
        return true;
    }

    public static synchronized void release(String ip, Object session) {
        Set<Object> sessions = SLOTS.get(ip);
        if (sessions == null) return;
        sessions.remove(session);
        if (sessions.isEmpty()) SLOTS.remove(ip);
    }
}
