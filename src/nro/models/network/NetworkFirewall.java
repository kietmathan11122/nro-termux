package nro.models.network;

import java.util.concurrent.ConcurrentHashMap;

/** Firewall nhẹ ở tầng ứng dụng, bảo vệ accept loop và luồng đọc packet. */
public final class NetworkFirewall {

    public static final int MAX_PACKET_BYTES = 32 * 1024;
    private static final int MAX_CONNECTS_PER_WINDOW = 12;
    private static final long CONNECT_WINDOW_MS = 10_000L;
    private static final int MAX_PACKETS_PER_SECOND = 150;
    private static final long BAN_MS = 120_000L;
    private static final ConcurrentHashMap<String, Bucket> BUCKETS = new ConcurrentHashMap<>();

    private NetworkFirewall() {
    }

    public static boolean allowConnection(String ip) {
        long now = System.currentTimeMillis();
        Bucket bucket = BUCKETS.computeIfAbsent(ip, ignored -> new Bucket(now));
        synchronized (bucket) {
            if (bucket.bannedUntil > now) {
                return false;
            }
            if (now - bucket.connectWindowStart >= CONNECT_WINDOW_MS) {
                bucket.connectWindowStart = now;
                bucket.connectCount = 0;
            }
            if (++bucket.connectCount > MAX_CONNECTS_PER_WINDOW) {
                bucket.bannedUntil = now + BAN_MS;
                return false;
            }
            bucket.lastSeen = now;
            return true;
        }
    }

    public static boolean allowPacket(String ip, int size, PacketLimiter limiter) {
        if (size < 0 || size > MAX_PACKET_BYTES) {
            ban(ip);
            return false;
        }
        long now = System.currentTimeMillis();
        Bucket bucket = BUCKETS.computeIfAbsent(ip, ignored -> new Bucket(now));
        synchronized (bucket) {
            if (bucket.bannedUntil > now) {
                return false;
            }
            bucket.lastSeen = now;
            // A busy connection must not consume another player's allowance
            // behind the same router, or ban every local test client.
            return limiter.allow(now);
        }
    }

    public static final class PacketLimiter {
        private long windowStart;
        private int count;

        synchronized boolean allow(long now) {
            if (now - windowStart >= 1_000L) {
                windowStart = now;
                count = 0;
            }
            return ++count <= MAX_PACKETS_PER_SECOND;
        }
    }

    private static void ban(String ip) {
        long now = System.currentTimeMillis();
        Bucket bucket = BUCKETS.computeIfAbsent(ip, ignored -> new Bucket(now));
        synchronized (bucket) {
            bucket.bannedUntil = now + BAN_MS;
            bucket.lastSeen = now;
        }
    }

    public static void cleanup() {
        long staleBefore = System.currentTimeMillis() - 10 * 60_000L;
        BUCKETS.entrySet().removeIf(entry -> entry.getValue().lastSeen < staleBefore);
    }

    private static final class Bucket {
        long connectWindowStart;
        int connectCount;
        long bannedUntil;
        long lastSeen;

        Bucket(long now) {
            connectWindowStart = now;
            lastSeen = now;
        }
    }
}
