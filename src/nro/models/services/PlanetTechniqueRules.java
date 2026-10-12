package nro.models.services;

/** Pure, deterministic rules shared by the three additional PvE techniques. */
public final class PlanetTechniqueRules {
    private PlanetTechniqueRules() { }
    public static final int CHARGE_MS = 1000;
    public static final int DRAGON_TRAVEL_MS = 900;
    public static final int TORNADO_TRAVEL_MS = 1800;
    public static final int BLEED_TICKS = 5;
    public static final int BOSS_CONTROL_IMMUNITY_MS = 5000;
    public static final int MAX_DASH = 120;

    public static boolean handles(int id) { return id >= 66 && id <= 68; }
    public static int level(int level) { return Math.max(1, Math.min(7, level)); }
    public static int duration(int id) { return duration(id, 1); }
    public static int duration(int id, int level) { return id == 66 ? 1500 : id == 67 ? 1800 : vineTicks(level) * 1000; }
    public static int actionDuration(int id) { return id == 68 ? 600 : duration(id); }
    public static int punchStunMs(int level) { return 500 + (level(level) - 1) * 200; }
    public static int bleedBasisPoints(int level) { return 200 + (level(level) - 1) * 150; }
    public static int vineTicks(int level) { return 4 + level(level); }
    public static long percentMaxHp(long maxHp, int basisPoints) {
        long hp = Math.max(1, maxHp);
        int bp = clamp(basisPoints, 1, 10000);
        // Split the product to preserve percentages without overflowing long HP pools.
        return Math.max(1, hp / 10000 * bp + hp % 10000 * bp / 10000);
    }
    public static long bleedTickDamage(long maxHp, int level) { return percentMaxHp(maxHp, bleedBasisPoints(level)); }
    public static long vineTickDamage(long maxHp, int level) { return percentMaxHp(maxHp, level(level) * 100); }
    public static int range(int id) { return id == 68 ? 320 : 420; }
    public static int radius(int id) { return id == 66 ? 80 : id == 67 ? 70 : 120; }
    public static int damagePercent(int id, int level) {
        return (id == 66 ? 220 : id == 67 ? 160 : 50)
                + (level(level) - 1) * (id == 66 ? 30 : id == 67 ? 20 : 10);
    }
    public static int cooldown(int id, int level) {
        return (id == 66 ? 18000 : id == 67 ? 22000 : 25000) - (level(level) - 1) * 500;
    }
    public static int manaPercent(int id) { return id == 66 ? 15 : id == 67 ? 18 : 20; }
    public static long scaledDamage(long attack, int percent) {
        long safeAttack = Math.min(Integer.MAX_VALUE, Math.max(1, attack));
        return Math.max(1, Math.min(Integer.MAX_VALUE, safeAttack * Math.max(1, percent) / 100));
    }
    public static long vineTickDamage(long maxHp, long attack, int level, boolean boss) {
        return vineTickDamage(maxHp, level);
    }
    public static boolean inCircle(int x, int y, int cx, int cy, int radius) {
        long dx = (long) x - cx, dy = (long) y - cy;
        return dx * dx + dy * dy <= (long) radius * radius;
    }
    public static boolean inSweptLane(int x, int y, int startX, int endX, int laneY, int radius) {
        return x >= Math.min(startX, endX) - radius && x <= Math.max(startX, endX) + radius
                && Math.abs((long) y - laneY) <= radius;
    }
    public static int travelX(int start, int end, long elapsed, int duration) {
        return start + (int) ((long) (end - start) * Math.max(0, Math.min(duration, elapsed)) / duration);
    }
    public static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
