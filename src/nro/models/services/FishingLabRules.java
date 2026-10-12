package nro.models.services;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/** Pure rules; timestamps and random rolls are supplied only by the server. */
public final class FishingLabRules {
    public static final int MAX_LEVEL = 7;
    public static final int MAX_FISH = 10000;
    public static final int CAST_COST = 1000;
    public static final int[] ROD_PRICES = {10000, 25000, 50000, 100000, 200000, 400000, 800000};
    public static final String[] FISH_NAMES = {"Cá rô", "Cá chép", "Cá trê", "Cá hồi", "Cá vàng", "Cá rồng"};
    public static final int[] SELL_PRICES = {1500, 2500, 5000, 12000, 30000, 80000};
    private static final int[] UPGRADE_GOLD = {10000, 25000, 50000, 100000, 200000, 400000};
    private static final int[] UPGRADE_FISH = {3, 6, 10, 15, 20, 30};
    private static final int[] WAIT_SECONDS = {8, 7, 7, 6, 5, 5, 4};
    private static final int[] WEIGHTS = {5000, 2700, 1400, 650, 220, 30};
    private static final int[] LEVEL_WEIGHT = {-450, 150, 150, 90, 45, 15};

    private FishingLabRules() { }

    public static final class State {
        public final int rodLevel;
        public final int ownedRods;
        private final int[] fish;
        public final int pendingType;
        public final long readyAt;
        public final int hookHits;
        public final long lastHookAt;
        public int requiredHits() { return pendingType < 0 ? 0 : 3 + pendingType; }
        public long escapeAt() { return pendingType < 0 ? 0 : readyAt + 15000L; }

        public State(int rodLevel, int[] fish, int pendingType, long readyAt) {
            this(rodLevel, rodLevel == 0 ? 0 : (1 << rodLevel) - 1, fish, pendingType, readyAt);
        }

        public State(int rodLevel, int ownedRods, int[] fish, int pendingType, long readyAt) {
            this(rodLevel, ownedRods, fish, pendingType, readyAt, 0, 0);
        }
        private State(int rodLevel, int ownedRods, int[] fish, int pendingType, long readyAt, int hookHits, long lastHookAt) {
            if(hookHits<0 || hookHits>8 || lastHookAt<0 || (pendingType<0 && (hookHits!=0 || lastHookAt!=0))) throw new IllegalArgumentException("Invalid hook state");
            if (rodLevel < 0 || rodLevel > MAX_LEVEL || ownedRods < 0 || ownedRods > 127
                    || (rodLevel == 0 ? ownedRods != 0 : (ownedRods & (1 << (rodLevel - 1))) == 0)
                    || (rodLevel == 0 && pendingType >= 0) || fish == null || fish.length != FISH_NAMES.length
                    || pendingType < -1 || pendingType >= FISH_NAMES.length
                    || (pendingType == -1 ? readyAt != 0 : readyAt <= 0)) {
                throw new IllegalArgumentException("Invalid fishing state");
            }
            int total = 0;
            for (int amount : fish) {
                if (amount < 0 || amount > MAX_FISH) throw new IllegalArgumentException("Invalid fish count");
                total += amount;
            }
            if (total > MAX_FISH || (pendingType >= 0 && total >= MAX_FISH)) {
                throw new IllegalArgumentException("Fishing basket capacity exceeded");
            }
            this.rodLevel = rodLevel;
            this.ownedRods = ownedRods;
            this.fish = fish.clone();
            this.pendingType = pendingType;
            this.readyAt = readyAt;
            this.hookHits = hookHits; this.lastHookAt = lastHookAt;
        }

        public int fishCount(int type) { return fish[type]; }
        public int totalFish() {
            int total = 0;
            for (int amount : fish) total += amount;
            return total;
        }
        public int pendingStatus(long now) { return pendingType < 0 ? 0 : now < readyAt ? 1 : 2; }
        public int remainingSeconds(long now) {
            return pendingType < 0 || now >= readyAt ? 0
                    : (int) Math.min(Integer.MAX_VALUE, (readyAt - now - 1) / 1000 + 1);
        }
        public int upgradeGold() { return rodLevel == 0 || rodLevel >= MAX_LEVEL ? 0 : UPGRADE_GOLD[rodLevel - 1]; }
        public int upgradeFish() { return rodLevel == 0 || rodLevel >= MAX_LEVEL ? 0 : UPGRADE_FISH[rodLevel - 1]; }
        public boolean ownsRod(int level) { return level >= 1 && level <= MAX_LEVEL && (ownedRods & (1 << (level - 1))) != 0; }
        public long saleValue() {
            long value = 0;
            for (int i = 0; i < fish.length; i++) value += (long) fish[i] * SELL_PRICES[i];
            return value;
        }
        public JSONObject toJson() {
            JSONObject result = new JSONObject();
            JSONArray counts = new JSONArray();
            for (int amount : fish) counts.add(amount);
            result.put("version", 3);
            result.put("rod", rodLevel);
            result.put("ownedRods", ownedRods);
            result.put("fish", counts);
            result.put("pending", pendingType);
            result.put("readyAt", readyAt);
            result.put("hookHits",hookHits); result.put("lastHookAt",lastHookAt);
            return result;
        }
    }

    public static State initial() { return new State(0, 0, new int[FISH_NAMES.length], -1, 0); }

    public static State fromJson(Object value) {
        if (value == null) return initial();
        if (!(value instanceof JSONObject)) throw new IllegalArgumentException("Invalid fishing progress");
        JSONObject object = (JSONObject) value;
        long version = number(object.get("version"));
        if (version != 1 && version != 2 && version != 3) throw new IllegalArgumentException("Unsupported fishing version");
        Object countsObject = object.get("fish");
        if (!(countsObject instanceof JSONArray)) throw new IllegalArgumentException("Invalid fishing basket");
        JSONArray counts = (JSONArray) countsObject;
        if (counts.size() != FISH_NAMES.length) throw new IllegalArgumentException("Invalid fishing basket size");
        int[] fish = new int[FISH_NAMES.length];
        for (int i = 0; i < fish.length; i++) fish[i] = Math.toIntExact(number(counts.get(i)));
        int rod = Math.toIntExact(number(object.get("rod")));
        if (rod < 0 || rod > MAX_LEVEL || (version == 1 && rod == 0)) throw new IllegalArgumentException("Invalid fishing rod");
        int owned = version == 1 ? (1 << rod) - 1 : Math.toIntExact(number(object.get("ownedRods")));
        return new State(rod, owned, fish,
                Math.toIntExact(number(object.get("pending"))), number(object.get("readyAt")),
                version < 3 ? 0 : Math.toIntExact(number(object.get("hookHits"))), version < 3 ? 0 : number(object.get("lastHookAt")));
    }

    private static long number(Object value) {
        if (!(value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long)) {
            throw new IllegalArgumentException("Invalid fishing number");
        }
        return ((Number) value).longValue();
    }

    public static State cast(State state, long now, int roll) {
        if (state.rodLevel == 0) throw new IllegalArgumentException("Hãy mua cần câu trong Cửa hàng trước khi thả câu.");
        if (state.pendingType >= 0) throw new IllegalArgumentException("Hãy thu cá ở lượt câu hiện tại trước.");
        if (state.totalFish() >= MAX_FISH) throw new IllegalArgumentException("Giỏ cá đã đầy. Hãy bán cá trước.");
        if (now <= 0 || roll < 0 || roll >= 10000) throw new IllegalArgumentException("Invalid cast input");
        int type = 0;
        int cumulative = 0;
        for (int i = 0; i < FISH_NAMES.length; i++) {
            cumulative += WEIGHTS[i] + LEVEL_WEIGHT[i] * (state.rodLevel - 1);
            if (roll < cumulative) { type = i; break; }
        }
        return new State(state.rodLevel, state.ownedRods, state.fish, type,
                Math.addExact(now, WAIT_SECONDS[state.rodLevel - 1] * 1000L));
    }

    public static State collect(State state, long now) {
        if (state.pendingType < 0) throw new IllegalArgumentException("Bạn chưa thả câu.");
        if (now < state.readyAt) throw new IllegalArgumentException("Cá chưa cắn câu. Hãy chờ thêm một chút.");
        int[] fish = state.fish.clone();
        fish[state.pendingType]++;
        return new State(state.rodLevel, state.ownedRods, fish, -1, 0);
    }

    public static State cancel(State state) {
        return new State(state.rodLevel,state.ownedRods,state.fish,-1,0);
    }
    public static State hook(State state, long now) {
        if(state.pendingType<0) throw new IllegalArgumentException("Bạn chưa thả câu.");
        if(now<state.readyAt) throw new IllegalArgumentException("Cá chưa cắn câu.");
        if(now>=state.escapeAt()) throw new IllegalArgumentException("Cá đã tuột khỏi lưỡi câu.");
        if(state.lastHookAt>0 && now-state.lastHookAt<300) throw new IllegalArgumentException("Hãy chờ nhịp giật tiếp theo.");
        if(state.hookHits+1>=state.requiredHits()) return collect(state,now);
        return new State(state.rodLevel,state.ownedRods,state.fish,state.pendingType,state.readyAt,state.hookHits+1,now);
    }

    public static State sellAll(State state) {
        if (state.totalFish() == 0) throw new IllegalArgumentException("Giỏ cá đang trống.");
        return new State(state.rodLevel, state.ownedRods, new int[FISH_NAMES.length], state.pendingType, state.readyAt, state.hookHits, state.lastHookAt);
    }

    public static State buyRod(State state, int level) {
        if (level < 1 || level > MAX_LEVEL) throw new IllegalArgumentException("Cần câu không hợp lệ.");
        if (state.ownsRod(level)) throw new IllegalArgumentException("Bạn đã sở hữu cần câu này.");
        if (state.pendingType >= 0) throw new IllegalArgumentException("Hãy thu cá trước khi mua cần câu.");
        return new State(level, state.ownedRods | (1 << (level - 1)), state.fish, -1, 0);
    }

    public static State selectRod(State state, int level) {
        if (!state.ownsRod(level)) throw new IllegalArgumentException("Bạn chưa sở hữu cần câu này.");
        if (state.pendingType >= 0) throw new IllegalArgumentException("Hãy thu cá trước khi đổi cần câu.");
        return new State(level, state.ownedRods, state.fish, -1, 0);
    }

    public static State consumeFish(State state, int amount) {
        if (amount < 0 || state.totalFish() < amount) throw new IllegalArgumentException("Bạn chưa đủ cá làm nguyên liệu.");
        int[] fish = state.fish.clone();
        for (int i = 0; i < fish.length && amount > 0; i++) {
            int used = Math.min(amount, fish[i]);
            fish[i] -= used;
            amount -= used;
        }
        return new State(state.rodLevel, state.ownedRods, fish, state.pendingType, state.readyAt, state.hookHits, state.lastHookAt);
    }

    public static State upgrade(State state) {
        if (state.rodLevel == 0) throw new IllegalArgumentException("Bạn chưa sở hữu cần câu.");
        if (state.rodLevel == MAX_LEVEL) throw new IllegalArgumentException("Cần câu đã đạt cấp tối đa.");
        if (state.pendingType >= 0) throw new IllegalArgumentException("Hãy thu cá trước khi nâng cấp cần câu.");
        int required = state.upgradeFish();
        if (state.totalFish() < required) throw new IllegalArgumentException("Bạn chưa có đủ cá để nâng cấp.");
        int[] fish = state.fish.clone();
        // Preserve valuable catches when inexpensive fish cover the materials.
        for (int i = 0; i < fish.length && required > 0; i++) {
            int consumed = Math.min(required, fish[i]);
            fish[i] -= consumed;
            required -= consumed;
        }
        return new State(state.rodLevel + 1, fish, -1, 0);
    }
}
