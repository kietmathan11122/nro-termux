package nro.models.services;

/** Deterministic rules for the seven Earth rebirth techniques (template IDs 69-75). */
public final class HeartsSkillRules {
    private HeartsSkillRules() { }
    public static boolean handles(int id) { return id >= 69 && id <= 75; }
    public static int level(int level) { return Math.max(1, Math.min(7, level)); }
    public static int chargeMs(int id) {
        return switch (id) { case 69 -> 130; case 70 -> 440; case 71 -> 0; case 72 -> 150; case 74 -> 3000; case 75 -> 300; default -> 4000; };
    }
    public static int actionMs(int id) {
        return switch (id) { case 69 -> 390; case 70 -> 440; case 71 -> 600; case 72 -> 400; case 74 -> 720; case 75 -> 1600; default -> 1200; };
    }
    public static int gravityTicks(int level) { return level(level) + 2; }
    public static int durationMs(int id, int level) { return id == 71 ? gravityTicks(level) * 1000 : actionMs(id); }
    public static int range(int id) { return id == 69 ? 100 : id == 70 ? 300 : id == 73 ? 500 : id >= 74 ? 420 : 320; }
    public static int radius(int id, int level) { return id == 71 ? 120 : id == 73 ? 350 + level(level) * 30 : id == 74 ? 95 + level(level) * 5 : id == 75 ? 110 : 70; }
    public static int kickStunMs(int level) { return (level(level) + 1) * 500; }
    public static long percent(long value, int percent) {
        long safe = Math.max(0, value);
        long whole = safe / 100;
        long remainder = safe % 100 * Math.max(0L, percent) / 100;
        if (percent <= 0) return 0;
        if (whole > (Long.MAX_VALUE - remainder) / percent) return Long.MAX_VALUE;
        return whole * percent + remainder;
    }
    public static long attackDamage(long attack, int percent) { return Math.max(1, Math.min(Integer.MAX_VALUE, percent(attack, percent))); }
    public static long gravityDamage(long maxHp) { return Math.max(1, percent(maxHp, 3)); }
    public static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    public static boolean inCircle(int x, int y, int cx, int cy, int radius) {
        long dx = (long) x - cx, dy = (long) y - cy;
        return dx * dx + dy * dy <= (long) radius * radius;
    }
    public static boolean inSweptLane(int x, int y, int startX, int endX, int laneY, int radius) {
        return x >= Math.min(startX, endX) - radius && x <= Math.max(startX, endX) + radius
                && Math.abs((long) y - laneY) <= radius;
    }
    /** Earliest contact along a 2-D swept projectile, including vertical shots. */
    public static double sweptContact(int x, int y, int ax, int ay, int bx, int by, int radius) {
        double dx = (double) bx - ax, dy = (double) by - ay;
        double fx = (double) ax - x, fy = (double) ay - y;
        double c = fx * fx + fy * fy - (double) radius * radius;
        if (c <= 0) return 0;
        double a = dx * dx + dy * dy;
        if (a == 0) return Double.POSITIVE_INFINITY;
        double b = 2 * (fx * dx + fy * dy);
        double discriminant = b * b - 4 * a * c;
        if (discriminant < 0) return Double.POSITIVE_INFINITY;
        double t = (-b - Math.sqrt(discriminant)) / (2 * a);
        return t >= 0 && t <= 1 ? t : Double.POSITIVE_INFINITY;
    }
    public static int travelX(int from, int to, long elapsed, int duration) {
        return from + (int) (((long) to - from) * Math.max(0, Math.min(duration, elapsed)) / duration);
    }
}
