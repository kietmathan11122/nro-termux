package nro.models.minigame;

/** Integer-only rules shared by round settlement and offline validation. */
public final class TaiXiuRules {
    public static final long MIN_BET = 10_000_000L, MAX_BET = 100_000_000_000L;
    private TaiXiuRules() { }
    public static boolean validBet(int type, long amount) {
        return type >= 0 && type <= 3 && amount >= MIN_BET && amount <= MAX_BET;
    }
    public static long payout(int type, long amount, int a, int b, int c) {
        if (!validBet(type, amount) || a < 1 || a > 6 || b < 1 || b > 6 || c < 1 || c > 6)
            throw new IllegalArgumentException("Invalid bet or dice");
        if (a == b && b == c) return (amount + 1) / 2;
        int sum = a + b + c;
        boolean won = type == 0 ? sum >= 11 : type == 1 ? sum <= 10
                : type == 2 ? sum % 2 == 0 : sum % 2 != 0;
        return won ? amount * 18 / 10 : 0;
    }
}
