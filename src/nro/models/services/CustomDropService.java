package nro.models.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import nro.models.boss.Boss;
import nro.models.item.Item;
import nro.models.map.ItemMap;
import nro.models.mob.Mob;
import nro.models.player.Player;
import nro.models.utils.Logger;
import nro.models.utils.Util;

public class CustomDropService {

    private static CustomDropService instance;

    public static CustomDropService gI() {
        if (instance == null) {
            instance = new CustomDropService();
        }
        return instance;
    }

    public static class MobDropRule {
        public String id;
        public String targetType; // "all", "mob", "map"
        public int targetId;
        public String targetName;
        public int itemId;
        public String itemName;
        public int minQuantity;
        public int maxQuantity;
        public double rate;
        public String options;
        public String category; // "normal", "event"
        public boolean enabled;
    }

    public static class BossDropRule {
        public String id;
        public String targetType; // "all", "boss_id", "config_key"
        public int targetId;
        public String configKey;
        public String targetName;
        public int itemId;
        public String itemName;
        public int minQuantity;
        public int maxQuantity;
        public double rate;
        public String pickupType; // "killer", "free"
        public String options;
        public String category; // "normal", "event"
        public boolean enabled;
    }

    public static class BlockedBossDrop {
        public String id;
        public String bossKey; // configKey, bossName, or "GLOBAL_BOSS"
        public int bossId;
        public int itemId; // -1 to block all items
        public String category; // "normal", "event"
    }

    public static class BlockedMobDrop {
        public String id;
        public int mobTempId; // 0 for all
        public int itemId;
        public String category; // "normal", "event"
    }

    private final List<MobDropRule> mobDrops = new CopyOnWriteArrayList<>();
    private final List<BossDropRule> bossDrops = new CopyOnWriteArrayList<>();
    private final List<BlockedBossDrop> blockedBossDrops = new CopyOnWriteArrayList<>();
    private final List<BlockedMobDrop> blockedMobDrops = new CopyOnWriteArrayList<>();
    private final Set<String> blockedEvents = new HashSet<>();

    private final ThreadLocal<Boss> currentDyingBoss = new ThreadLocal<>();

    private CustomDropService() {
        load();
    }

    public void setCurrentDyingBoss(Boss boss) {
        currentDyingBoss.set(boss);
    }

    public Boss getCurrentDyingBoss() {
        return currentDyingBoss.get();
    }

    public synchronized void load() {
        mobDrops.clear();
        bossDrops.clear();
        blockedBossDrops.clear();
        blockedMobDrops.clear();
        blockedEvents.clear();

        Path filePath = resolveFilePath();
        if (filePath == null || !Files.isRegularFile(filePath)) {
            Logger.warning("CustomDropService: file custom_drops.json chua ton tai. Su dung danh sach trong.\n");
            return;
        }

        try {
            String content = Files.readString(filePath, StandardCharsets.UTF_8);
            if (content == null || content.trim().isEmpty()) {
                return;
            }

            JsonElement rootElem = new JsonParser().parse(content);
            if (!rootElem.isJsonObject()) {
                return;
            }

            JsonObject root = rootElem.getAsJsonObject();
            if (root.has("mob_drops") && root.get("mob_drops").isJsonArray()) {
                JsonArray arr = root.getAsJsonArray("mob_drops");
                for (JsonElement el : arr) {
                    if (el.isJsonObject()) {
                        MobDropRule rule = parseMobRule(el.getAsJsonObject());
                        if (rule != null) {
                            mobDrops.add(rule);
                        }
                    }
                }
            }

            if (root.has("boss_drops") && root.get("boss_drops").isJsonArray()) {
                JsonArray arr = root.getAsJsonArray("boss_drops");
                for (JsonElement el : arr) {
                    if (el.isJsonObject()) {
                        BossDropRule rule = parseBossRule(el.getAsJsonObject());
                        if (rule != null) {
                            bossDrops.add(rule);
                        }
                    }
                }
            }

            if (root.has("blocked_boss_drops") && root.get("blocked_boss_drops").isJsonArray()) {
                JsonArray arr = root.getAsJsonArray("blocked_boss_drops");
                for (JsonElement el : arr) {
                    if (el.isJsonObject()) {
                        JsonObject obj = el.getAsJsonObject();
                        BlockedBossDrop b = new BlockedBossDrop();
                        b.id = obj.has("id") ? obj.get("id").getAsString() : "";
                        b.bossKey = obj.has("boss_key") ? obj.get("boss_key").getAsString() : "";
                        b.bossId = obj.has("boss_id") ? obj.get("boss_id").getAsInt() : 0;
                        b.itemId = obj.has("item_id") ? obj.get("item_id").getAsInt() : -1;
                        b.category = obj.has("category") ? obj.get("category").getAsString() : "normal";
                        blockedBossDrops.add(b);
                    }
                }
            }

            if (root.has("blocked_mob_drops") && root.get("blocked_mob_drops").isJsonArray()) {
                JsonArray arr = root.getAsJsonArray("blocked_mob_drops");
                for (JsonElement el : arr) {
                    if (el.isJsonObject()) {
                        JsonObject obj = el.getAsJsonObject();
                        BlockedMobDrop m = new BlockedMobDrop();
                        m.id = obj.has("id") ? obj.get("id").getAsString() : "";
                        m.mobTempId = obj.has("mob_temp_id") ? obj.get("mob_temp_id").getAsInt() : 0;
                        m.itemId = obj.has("item_id") ? obj.get("item_id").getAsInt() : -1;
                        m.category = obj.has("category") ? obj.get("category").getAsString() : "normal";
                        blockedMobDrops.add(m);
                    }
                }
            }

            if (root.has("blocked_events") && root.get("blocked_events").isJsonArray()) {
                JsonArray arr = root.getAsJsonArray("blocked_events");
                for (JsonElement el : arr) {
                    try {
                        blockedEvents.add(el.getAsString().toUpperCase());
                    } catch (Exception ignored) {}
                }
            }

            Logger.success("CustomDropService: Da nap thanh cong " + mobDrops.size()
                    + " rule quai, " + bossDrops.size() + " rule boss, "
                    + blockedBossDrops.size() + " block boss, " + blockedMobDrops.size() + " block quai tu " + filePath.getFileName() + "\n");
        } catch (Exception e) {
            Logger.error("CustomDropService.load() loi: " + e.getMessage() + "\n");
        }
    }

    private Path resolveFilePath() {
        String[] candidates = {
            "custom_drops.json",
            "data/custom_drops.json",
            "data/htdocs/sv1/custom_drops.json",
            "C:/xampp1/htdocs/sv1/custom_drops.json"
        };
        for (String c : candidates) {
            Path p = Path.of(c);
            if (Files.isRegularFile(p)) {
                return p;
            }
        }
        return Path.of("custom_drops.json");
    }

    private MobDropRule parseMobRule(JsonObject obj) {
        try {
            MobDropRule r = new MobDropRule();
            r.id = obj.has("id") ? obj.get("id").getAsString() : ("mob_" + System.currentTimeMillis());
            r.targetType = obj.has("target_type") ? obj.get("target_type").getAsString() : "all";
            r.targetId = obj.has("target_id") ? obj.get("target_id").getAsInt() : 0;
            r.targetName = obj.has("target_name") ? obj.get("target_name").getAsString() : "";
            r.itemId = obj.has("item_id") ? obj.get("item_id").getAsInt() : -1;
            r.itemName = obj.has("item_name") ? obj.get("item_name").getAsString() : "";
            r.minQuantity = obj.has("min_quantity") ? Math.max(1, obj.get("min_quantity").getAsInt()) : 1;
            r.maxQuantity = obj.has("max_quantity") ? Math.max(r.minQuantity, obj.get("max_quantity").getAsInt()) : r.minQuantity;
            r.rate = obj.has("rate") ? obj.get("rate").getAsDouble() : 100.0;
            r.options = obj.has("options") ? obj.get("options").getAsString() : "";
            r.category = obj.has("category") ? obj.get("category").getAsString() : "normal";
            r.enabled = !obj.has("enabled") || obj.get("enabled").getAsBoolean();
            if (r.itemId <= 0) {
                return null;
            }
            return r;
        } catch (Exception e) {
            return null;
        }
    }

    private BossDropRule parseBossRule(JsonObject obj) {
        try {
            BossDropRule r = new BossDropRule();
            r.id = obj.has("id") ? obj.get("id").getAsString() : ("boss_" + System.currentTimeMillis());
            r.targetType = obj.has("target_type") ? obj.get("target_type").getAsString() : "all";
            r.targetId = obj.has("target_id") ? obj.get("target_id").getAsInt() : 0;
            r.configKey = obj.has("config_key") ? obj.get("config_key").getAsString() : "";
            r.targetName = obj.has("target_name") ? obj.get("target_name").getAsString() : "";
            r.itemId = obj.has("item_id") ? obj.get("item_id").getAsInt() : -1;
            r.itemName = obj.has("item_name") ? obj.get("item_name").getAsString() : "";
            r.minQuantity = obj.has("min_quantity") ? Math.max(1, obj.get("min_quantity").getAsInt()) : 1;
            r.maxQuantity = obj.has("max_quantity") ? Math.max(r.minQuantity, obj.get("max_quantity").getAsInt()) : r.minQuantity;
            r.rate = obj.has("rate") ? obj.get("rate").getAsDouble() : 100.0;
            r.pickupType = obj.has("pickup_type") ? obj.get("pickup_type").getAsString() : "killer";
            r.options = obj.has("options") ? obj.get("options").getAsString() : "";
            r.category = obj.has("category") ? obj.get("category").getAsString() : "normal";
            r.enabled = !obj.has("enabled") || obj.get("enabled").getAsBoolean();
            if (r.itemId <= 0) {
                return null;
            }
            return r;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isDropBlocked(ItemMap item) {
        if (item == null || item.itemTemplate == null) {
            return false;
        }
        int itemId = item.itemTemplate.id;
        Boss boss = currentDyingBoss.get();
        if (boss != null) {
            int bossId = (int) boss.id;
            String bossName = boss.name != null ? boss.name.trim() : "";
            for (BlockedBossDrop b : blockedBossDrops) {
                if (b.itemId == -1 || b.itemId == itemId) {
                    if ("GLOBAL_BOSS".equalsIgnoreCase(b.bossKey)) {
                        return true;
                    }
                    if (b.bossId != 0 && b.bossId == bossId) {
                        return true;
                    }
                    if (b.bossKey != null && !b.bossKey.isEmpty()) {
                        String normKey = b.bossKey.replace("_", "").toLowerCase();
                        String normName = bossName.replace(" ", "").replace("_", "").toLowerCase();
                        if (normName.contains(normKey) || normKey.contains(normName)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean isMobDropBlocked(Mob mob, int itemId) {
        int mobTempId = mob != null ? mob.tempId : 0;
        for (BlockedMobDrop m : blockedMobDrops) {
            if (m.itemId == itemId) {
                if (m.mobTempId == 0 || m.mobTempId == mobTempId) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isEventBlocked(String eventName) {
        if (eventName == null) {
            return false;
        }
        return blockedEvents.contains(eventName.toUpperCase());
    }

    public List<ItemMap> getMobDrops(Mob mob, Player player, int x, int yEnd) {
        List<ItemMap> list = new ArrayList<>();
        if (mob == null || player == null || mob.zone == null) {
            return list;
        }

        int mobTempId = mob.tempId;
        int mapId = mob.zone.map.mapId;

        for (MobDropRule rule : mobDrops) {
            if (!rule.enabled) {
                continue;
            }

            boolean match = false;
            if ("all".equalsIgnoreCase(rule.targetType)) {
                match = true;
            } else if ("mob".equalsIgnoreCase(rule.targetType)) {
                match = (mobTempId == rule.targetId);
            } else if ("map".equalsIgnoreCase(rule.targetType)) {
                match = (mapId == rule.targetId);
            }

            if (!match) {
                continue;
            }

            if (!checkRate(rule.rate)) {
                continue;
            }

            int qty = rule.minQuantity >= rule.maxQuantity
                    ? rule.minQuantity
                    : Util.nextInt(rule.minQuantity, rule.maxQuantity);
            if (qty <= 0) {
                qty = 1;
            }

            ItemMap itemMap = new ItemMap(mob.zone, rule.itemId, qty, x, yEnd, player.id);
            applyOptions(itemMap, rule.options);
            list.add(itemMap);
        }

        return list;
    }

    public void dropBossReward(Boss boss, Player plKill) {
        if (boss == null || boss.zone == null) {
            return;
        }

        int bossId = (int) boss.id;
        String bossName = boss.name != null ? boss.name.trim() : "";

        for (BossDropRule rule : bossDrops) {
            if (!rule.enabled) {
                continue;
            }

            boolean match = false;
            if ("all".equalsIgnoreCase(rule.targetType)) {
                match = true;
            } else if ("boss_id".equalsIgnoreCase(rule.targetType)) {
                match = (bossId == rule.targetId);
            } else if ("config_key".equalsIgnoreCase(rule.targetType)) {
                if (rule.configKey != null && !rule.configKey.isEmpty()) {
                    String normConfig = rule.configKey.replace("_", "").toLowerCase();
                    String normName = bossName.replace(" ", "").replace("_", "").toLowerCase();
                    match = normName.contains(normConfig) || normConfig.contains(normName);
                }
            }

            if (!match) {
                continue;
            }

            if (!checkRate(rule.rate)) {
                continue;
            }

            int qty = rule.minQuantity >= rule.maxQuantity
                    ? rule.minQuantity
                    : Util.nextInt(rule.minQuantity, rule.maxQuantity);
            if (qty <= 0) {
                qty = 1;
            }

            long playerId = "free".equalsIgnoreCase(rule.pickupType)
                    ? -1
                    : (plKill != null ? plKill.id : -1);

            int count = Math.min(qty, 10);
            int qtyPerItem = qty / count;
            int remain = qty % count;

            for (int i = 0; i < count; i++) {
                int thisQty = qtyPerItem + (i == 0 ? remain : 0);
                if (thisQty <= 0) {
                    continue;
                }
                int dropX = boss.location.x + (count > 1 ? Util.nextInt(-60, 60) : 0);
                int dropY = boss.zone.map.yPhysicInTop(dropX, boss.location.y - 24);
                ItemMap itemMap = new ItemMap(boss.zone, rule.itemId, thisQty, dropX, dropY, playerId);
                applyOptions(itemMap, rule.options);
                Service.gI().dropItemMap(boss.zone, itemMap);
            }
        }
    }

    private boolean checkRate(double rate) {
        if (rate >= 100.0) {
            return true;
        }
        if (rate <= 0.0) {
            return false;
        }
        long numerator = Math.round(rate * 100.0);
        long denominator = 10000L;
        return Util.isTrue(numerator, denominator);
    }

    private void applyOptions(ItemMap itemMap, String optionsStr) {
        if (optionsStr == null || optionsStr.trim().isEmpty() || itemMap == null) {
            return;
        }
        try {
            String[] parts = optionsStr.split("[,;\\s]+");
            for (String part : parts) {
                part = part.trim();
                if (part.isEmpty()) {
                    continue;
                }
                String[] kv = part.split("[:=-]");
                if (kv.length >= 2) {
                    int optId = Integer.parseInt(kv[0].trim());
                    int optParam = Integer.parseInt(kv[1].trim());
                    itemMap.options.add(new Item.ItemOption(optId, optParam));
                }
            }
        } catch (Exception ignored) {
        }
    }

    public List<MobDropRule> getMobDrops() {
        return mobDrops;
    }

    public List<BossDropRule> getBossDrops() {
        return bossDrops;
    }

    public List<BlockedBossDrop> getBlockedBossDrops() {
        return blockedBossDrops;
    }

    public List<BlockedMobDrop> getBlockedMobDrops() {
        return blockedMobDrops;
    }

    public Set<String> getBlockedEvents() {
        return blockedEvents;
    }
}
