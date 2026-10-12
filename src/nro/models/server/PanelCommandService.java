package nro.models.server;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import nro.models.Bot.NewBot;
import nro.models.boss.Boss;
import nro.models.boss.BossData;
import nro.models.boss.BossesData;
import nro.models.boss.BossID;
import nro.models.consts.ConstPlayer;
import nro.models.map.Zone;
import nro.models.map.service.ChangeMapService;
import nro.models.boss.Boss_Manager.BrolyManager;
import nro.models.boss.Boss_Manager.BossManager;
import nro.models.boss.Boss_Manager.AnTromManager;
import nro.models.boss.Boss_Manager.ChristmasEventManager;
import nro.models.boss.Boss_Manager.FinalBossManager;
import nro.models.boss.Boss_Manager.GasDestroyManager;
import nro.models.boss.Boss_Manager.HalloweenEventManager;
import nro.models.boss.Boss_Manager.HungVuongEventManager;
import nro.models.boss.Boss_Manager.LunarNewYearEventManager;
import nro.models.boss.Boss_Manager.OtherBossManager;
import nro.models.boss.Boss_Manager.RedRibbonHQManager;
import nro.models.boss.Boss_Manager.SkillSummonedManager;
import nro.models.boss.Boss_Manager.SnakeWayManager;
import nro.models.boss.Boss_Manager.TreasureUnderSeaManager;
import nro.models.boss.Boss_Manager.TrungThuEventManager;
import nro.models.boss.Boss_Manager.YardartManager;
import nro.models.data.LocalManager;
import nro.models.data.LocalResultSet;
import nro.models.database.PlayerDAO;
import nro.models.event.EventManager;
import nro.models.item.Item;
import nro.models.map.service.MapService;
import nro.models.managers.GiftCodeManager;
import nro.models.player.Player;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.services.PlayerService;
import nro.models.services.Service;
import nro.models.skill.Skill;
import nro.models.utils.Logger;
import nro.models.utils.SkillUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class PanelCommandService implements Runnable {

    private static final PanelCommandService INSTANCE = new PanelCommandService();
    private static final Path COMMAND_FILE = Path.of("panel_cmd.txt");
    private static final Path ADMIN_MODE_FILE = Path.of("panel_admin_mode_status.txt");
    private static final Path BOT_PEM_STATUS_FILE = Path.of("panel_xnxx_status.txt");
    private static final Path EXP_STATUS_FILE = Path.of("panel_exp_status.txt");
    private static final Path DROP_STATUS_FILE = Path.of("panel_drop_status.txt");
    private static final Path BOSS_PERSIST_STATUS_FILE = Path.of("panel_boss_persist_status.txt");
    private static final Path BOSS_OVERRIDES_FILE = Path.of("panel_boss_overrides.properties");
    private static final Path BOSS_STATUS_FILE = Path.of("boss_status.json");
    private static final long BOSS_STATUS_INTERVAL_MS = 3000L;
    private static final Set<String> HIDDEN_BOSS_NAMES = Set.of(
    );
    private static final ConcurrentHashMap<Integer, Long> KICK_LOGIN_BLOCKS = new ConcurrentHashMap<>();
    private static volatile boolean adminModeOnly = readFlag(ADMIN_MODE_FILE);
    private static volatile boolean bossPersistMode = readFlag(BOSS_PERSIST_STATUS_FILE);
    private final Set<Boss> appliedBossOverrides = Collections.newSetFromMap(new IdentityHashMap<>());
    private volatile boolean catalogOverridesApplied;
    private long lastBossStatusWrite;

    public static PanelCommandService gI() {
        return INSTANCE;
    }

    public static boolean isAdminModeOnly() {
        return adminModeOnly || readFlag(ADMIN_MODE_FILE);
    }

    public static boolean isBotModeEnabled() {
        return readFlag(BOT_PEM_STATUS_FILE);
    }

    public static boolean isBossPersistMode() {
        return bossPersistMode || readFlag(BOSS_PERSIST_STATUS_FILE);
    }

    /** Execute a command submitted by the embedded localhost admin panel. */
    public synchronized void executeNow(String command) {
        process(command == null ? "" : command.trim());
    }

    public static long getKickBlockRemainingSeconds(int accountId) {
        Long blockedUntil = KICK_LOGIN_BLOCKS.get(accountId);
        if (blockedUntil == null) {
            return 0L;
        }
        long remainingMs = blockedUntil - System.currentTimeMillis();
        if (remainingMs <= 0L) {
            KICK_LOGIN_BLOCKS.remove(accountId, blockedUntil);
            return 0L;
        }
        return (remainingMs + 999L) / 1000L;
    }

    public static void loadStartupState() {
        adminModeOnly = readFlag(ADMIN_MODE_FILE);
        int expRate = readPositiveInt(EXP_STATUS_FILE, 0);
        if (expRate > 0) {
            Manager.RATE_EXP_SERVER = expRate;
        }
        int dropRate = readPositiveInt(DROP_STATUS_FILE, 0);
        if (dropRate > 0) {
            Manager.RATE_DROP_SERVER = dropRate;
        }
        bossPersistMode = readFlag(BOSS_PERSIST_STATUS_FILE);
        if (readFlag(BOT_PEM_STATUS_FILE)) {
            NewBot.gI().spawnTrainingBots();
        }
        nro.models.services.CustomDropService.gI().load();
    }

    private static boolean readFlag(Path file) {
        try {
            return Files.exists(file) && Files.readString(file, StandardCharsets.UTF_8).trim().equals("1");
        } catch (IOException ignored) {
            return false;
        }
    }

    private static int readPositiveInt(Path file, int fallback) {
        try {
            if (!Files.exists(file)) {
                return fallback;
            }
            int value = Integer.parseInt(Files.readString(file, StandardCharsets.UTF_8).trim());
            return value > 0 ? value : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    @Override
    public void run() {
        loadStartupState();
        while (ServerManager.isRunning) {
            try {
                pollCommands();
                applyBossOverridesIfNeeded();
                exportBossStatusIfNeeded();
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                Logger.logException(PanelCommandService.class, e);
            }
        }
    }

    private void pollCommands() throws IOException {
        if (!Files.exists(COMMAND_FILE)) {
            return;
        }
        List<String> lines = Files.readAllLines(COMMAND_FILE, StandardCharsets.UTF_8);
        Files.newBufferedWriter(COMMAND_FILE, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING).close();
        for (String line : lines) {
            process(line == null ? "" : line.trim());
        }
    }

    private void process(String command) {
        if (command.isEmpty()) {
            return;
        }
        String[] parts = command.split("\\|", 3);
        String action = parts[0];
        switch (action) {
            case "BUFF_VND" -> handleBuffVnd(parts);
            case "BUFF_ITEM" -> handleBuffItem(parts);
            case "KICK_PLAYER" -> handleKick(parts);
            case "ADMIN_MODE" -> handleAdminMode(parts);
            case "MAINTENANCE_NOW" -> Maintenance.gI().startSeconds(30);
            case "SET_EXP" -> handleSetExp(parts);
            case "SET_DROP" -> handleSetDrop(parts);
            case "SPAWN_BOSS" -> handleSpawnBoss(command);
            case "EDIT_BOSS" -> handleEditBoss(command);
            case "EDIT_BOSS_DATA" -> handleEditBossData(command);
            case "SET_BOSS_TARGET_HP_PERCENT" -> handleBossTargetHpPercent(parts);
            case "BOSS_PERSIST_MODE" -> handleBossPersistMode(parts);
            case "LOAD_GIFTCODE" -> GiftCodeManager.gI().loadGiftCodeFromDB();
            case "LOAD_POWER_LIMIT" -> Manager.loadPowerLimits();
            case "SET_EVENT" -> handleSetEvent(parts);
            case "RELOAD_CUSTOM_DROPS" -> nro.models.services.CustomDropService.gI().load();
            case "RESET_PANEL" -> handleResetPanel();
            case "BOT_PEM", "XNXX_MODE" -> handleBotPem(parts);
            case "BOTTALL", "SPAWN_BOT_GD" -> nro.models.Bot.BotManager.gI().spawnStoneTradeBots();
            case "REMOVE_BOT_GD", "TAT_BOT_GD" -> nro.models.Bot.BotManager.gI().removeStoneTradeBots();
            default -> Logger.warning("Panel command unknown: " + command + "\n");
        }
    }

    private void handleSetEvent(String[] parts) {
        if (parts.length < 2) {
            return;
        }
        int eventId = parseInt(parts[1], 9);
        EventManager.gI().changeEvent(eventId);
        Service.gI().sendThongBaoAllPlayer("Sự kiện server: " + EventManager.gI().getEventName(eventId));
    }

    private void handleResetPanel() {
        adminModeOnly = false;
        bossPersistMode = false;
        Manager.RATE_EXP_SERVER = readConfiguredExpRate();
        Manager.RATE_DROP_SERVER = 1;
        NewBot.gI().stopTrainingBots();
        appliedBossOverrides.clear();
        catalogOverridesApplied = false;
        try {
            Files.writeString(ADMIN_MODE_FILE, "0", StandardCharsets.UTF_8);
            Files.writeString(BOT_PEM_STATUS_FILE, "0", StandardCharsets.UTF_8);
            Files.writeString(EXP_STATUS_FILE, String.valueOf(Manager.RATE_EXP_SERVER), StandardCharsets.UTF_8);
            Files.writeString(DROP_STATUS_FILE, "1", StandardCharsets.UTF_8);
            Files.writeString(BOSS_PERSIST_STATUS_FILE, "0", StandardCharsets.UTF_8);
            Files.deleteIfExists(BOSS_OVERRIDES_FILE);
        } catch (IOException ignored) {
        }
        Service.gI().sendThongBaoAllPlayer("Thiết lập vận hành đã được đưa về mặc định.");
    }

    private int readConfiguredExpRate() {
        Properties properties = new Properties();
        try (var input = Files.newInputStream(Path.of("Config.properties"))) {
            properties.load(input);
            return Math.max(1, parseInt(properties.getProperty("server.expserver", "1"), 1));
        } catch (Exception ignored) {
            return 1;
        }
    }

    private void handleBotPem(String[] parts) {
        if (parts.length < 2) {
            return;
        }
        boolean enabled = "1".equals(parts[1]);
        if (enabled) {
            NewBot.gI().spawnTrainingBots();
        } else {
            NewBot.gI().stopTrainingBots();
        }
        try {
            Files.writeString(BOT_PEM_STATUS_FILE, enabled ? "1" : "0", StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
        Service.gI().sendThongBaoAllPlayer(enabled
                ? "Bot pem quai da duoc bat."
                : "Bot pem quai da duoc tat.");
    }

    private void handleBuffVnd(String[] parts) {
        if (parts.length < 3) {
            return;
        }
        Player target = Client.gI().getPlayer(parts[1]);
        int amount = parseInt(parts[2], 0);
        if (target == null || amount <= 0 || target.getSession() == null) {
            return;
        }
        PlayerDAO.addVnd(target, amount);
        target.getSession().vnd += amount;
        Service.gI().sendThongBao(target, "Bạn được admin cộng " + amount + " VND!");
    }

    private void handleBuffItem(String[] parts) {
        if (parts.length < 3) {
            return;
        }
        Player target = Client.gI().getPlayer(parts[1]);
        Object parsed = JSONValue.parse(parts[2]);
        if (target == null || !(parsed instanceof JSONArray items)) {
            return;
        }
        List<Item> added = new ArrayList<>();
        for (Object obj : items) {
            if (!(obj instanceof JSONObject itemData)) {
                continue;
            }
            int tempId = parseInt(String.valueOf(itemData.get("temp_id")), -1);
            int quantity = parseInt(String.valueOf(itemData.get("quantity")), 1);
            if (tempId < 0 || quantity <= 0) {
                continue;
            }
            Item item = ItemService.gI().createNewItem((short) tempId, quantity);
            item.itemOptions.clear();
            Object opts = itemData.get("options");
            if (opts instanceof JSONArray optionArray) {
                for (Object optObj : optionArray) {
                    if (optObj instanceof JSONObject opt) {
                        int id = parseInt(String.valueOf(opt.get("id")), -1);
                        int param = parseInt(String.valueOf(opt.get("param")), 0);
                        if (id >= 0) {
                            item.itemOptions.add(new Item.ItemOption(id, param));
                        }
                    }
                }
            }
            if (InventoryService.gI().addItemBag(target, item)) {
                added.add(item);
            }
        }
        if (!added.isEmpty()) {
            InventoryService.gI().sendItemBags(target);
            Service.gI().sendThongBao(target, "Bạn nhận được " + added.size() + " vật phẩm từ admin.");
        }
    }

    private void handleKick(String[] parts) {
        if (parts.length < 2) {
            return;
        }
        String playerName = parts[1] == null ? "" : parts[1].trim();
        int blockSeconds = parts.length >= 3 ? parseInt(parts[2], 300) : 300;
        if (playerName.isEmpty()) {
            return;
        }
        Player target = Client.gI().getPlayer(playerName);
        if (target != null && target.getSession() != null) {
            blockLogin(target.getSession().userId, blockSeconds);
            Client.gI().kickSession(target.getSession());
            return;
        }
        Integer accountId = findAccountIdByPlayerName(playerName);
        if (accountId != null) {
            blockLogin(accountId, blockSeconds);
        }
    }

    private static void blockLogin(int accountId, int seconds) {
        if (accountId <= 0) {
            return;
        }
        int safeSeconds = Math.max(1, seconds);
        KICK_LOGIN_BLOCKS.put(accountId, System.currentTimeMillis() + safeSeconds * 1000L);
    }

    private static Integer findAccountIdByPlayerName(String playerName) {
        LocalResultSet rs = null;
        try {
            rs = LocalManager.executeQuery("select account_id from player where name = ? limit 1", playerName);
            if (rs.first()) {
                return rs.getInt("account_id");
            }
        } catch (Exception e) {
            Logger.logException(PanelCommandService.class, e);
        } finally {
            if (rs != null) {
                rs.dispose();
            }
        }
        return null;
    }

    private void handleAdminMode(String[] parts) {
        if (parts.length < 2) {
            return;
        }
        adminModeOnly = "1".equals(parts[1]);
        try {
            Files.writeString(ADMIN_MODE_FILE, adminModeOnly ? "1" : "0", StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
        Service.gI().sendThongBaoAllPlayer(adminModeOnly
                ? "Server đã bật chế độ admin, tài khoản thường không thể đăng nhập."
                : "Server đã tắt chế độ admin.");
    }

    private void handleSetExp(String[] parts) {
        if (parts.length < 2) {
            return;
        }
        int rate = parseInt(parts[1], 0);
        if (rate <= 0) {
            return;
        }
        Manager.RATE_EXP_SERVER = rate;
        try {
            Files.writeString(EXP_STATUS_FILE, String.valueOf(rate), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
        Service.gI().sendThongBaoAllPlayer("EXP server hiện tại: x" + rate);
    }

    private void handleSetDrop(String[] parts) {
        if (parts.length < 2) {
            return;
        }
        int rate = parseInt(parts[1], 0);
        if (rate <= 0) {
            return;
        }
        Manager.RATE_DROP_SERVER = rate;
        try {
            Files.writeString(DROP_STATUS_FILE, String.valueOf(rate), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
        Service.gI().sendThongBaoAllPlayer("Ti le roi do hien tai: x" + rate);
    }

    private void handleSpawnBoss(String command) {
        String[] parts = command.split("\\|");
        if (parts.length < 2) {
            return;
        }
        int bossId = parseInt(parts[1], 0);
        if (bossId == 0) {
            return;
        }
        Boss boss = BossManager.gI().replaceBoss(bossId);
        if (boss != null && parts.length >= 4) {
            int mapId = parseInt(parts[2], -1);
            int zoneId = parseInt(parts[3], -1);
            if (mapId >= 0 && zoneId >= 0) {
                Zone zone = MapService.gI().getZoneByMapIDAndZoneID(mapId, zoneId);
                if (zone != null && (boss.zone == null || boss.zone != zone)) {
                    ChangeMapService.gI().changeMap(boss, zone, -1, -1);
                }
            }
        }
        lastBossStatusWrite = 0;
        exportBossStatusIfNeeded();
    }

    private void handleEditBoss(String command) {
        // EDIT_BOSS|id|level|hp|dame|armor|url-encoded-name|url-encoded-skills
        String[] parts = command.split("\\|", 8);
        if (parts.length < 8) {
            return;
        }
        long bossId = parseLong(parts[1], 0);
        int level = parseInt(parts[2], 0);
        int hp = parseInt(parts[3], 0);
        int dame = parseInt(parts[4], 0);
        int armor = parseInt(parts[5], 0);
        if (level < 0 || hp <= 0 || dame <= 0 || armor < 0) {
            return;
        }
        String name;
        String skillText;
        try {
            name = URLDecoder.decode(parts[6], StandardCharsets.UTF_8.name()).trim();
            skillText = URLDecoder.decode(parts[7], StandardCharsets.UTF_8.name()).trim();
        } catch (Exception e) {
            return;
        }
        List<int[]> skills = parseBossSkills(skillText);
        if (name.isEmpty() || skills.isEmpty()) {
            return;
        }
        for (Boss b : getPanelBosses()) {
            if (b != null && b.id == bossId) {
                applyBossConfiguration(b, level, name, hp, dame, armor, skills, true);
            }
        }
        if (bossPersistMode || readFlag(BOSS_PERSIST_STATUS_FILE)) {
            saveBossOverride(bossId, level, hp, dame, armor, name, skills);
        }
        lastBossStatusWrite = 0;
        exportBossStatusIfNeeded();
        Logger.warning("Panel edited boss " + name + " (ID=" + bossId + ", level=" + level + ")\n");
    }

    private void handleEditBossData(String command) {
        // EDIT_BOSS_DATA|BossesData field|hp|dame|armor|url-name|url-skills
        String[] parts = command.split("\\|", 7);
        if (parts.length < 7) {
            return;
        }
        String dataKey = parts[1].replaceAll("[^A-Za-z0-9_]", "");
        BossData bossData = getBossDataCatalog().get(dataKey);
        int hp = parseInt(parts[2], 0);
        int dame = parseInt(parts[3], 0);
        int armor = parseInt(parts[4], -1);
        if (bossData == null || hp <= 0 || dame <= 0 || armor < 0) {
            return;
        }
        try {
            String name = URLDecoder.decode(parts[5], StandardCharsets.UTF_8.name()).trim();
            String skillText = URLDecoder.decode(parts[6], StandardCharsets.UTF_8.name()).trim();
            List<int[]> skills = parseBossSkills(skillText);
            if (name.isEmpty() || skills.isEmpty()) {
                return;
            }
            applyBossDataConfiguration(bossData, name, hp, dame, armor, skills, true);
            if (bossPersistMode || readFlag(BOSS_PERSIST_STATUS_FILE)) {
                saveBossDataOverride(dataKey, hp, dame, armor, name, skills);
            }
            lastBossStatusWrite = 0;
            exportBossStatusIfNeeded();
            Logger.warning("Panel edited BossesData." + dataKey + " (" + name + ")\n");
        } catch (Exception ignored) {
        }
    }

    /** SET_BOSS_TARGET_HP_PERCENT|bossId|percent (0 = dame cố định). */
    private void handleBossTargetHpPercent(String[] parts) {
        if (parts.length < 3) {
            return;
        }
        long bossId = parseLong(parts[1], 0);
        int percent = Math.max(0, Math.min(100, parseInt(parts[2], 0)));
        for (Boss boss : getPanelBosses()) {
            if (boss != null && boss.id == bossId) {
                boss.damagePercentOfTargetHp = percent;
            }
        }
        lastBossStatusWrite = 0;
        exportBossStatusIfNeeded();
    }

    private void applyBossDataConfiguration(BossData targetData, String name, int hp, int dame,
            int armor, List<int[]> skills, boolean notify) {
        if (targetData == null) {
            return;
        }
        boolean appliedToInstance = false;
        for (Boss boss : getPanelBosses()) {
            if (boss == null || boss.data == null) {
                continue;
            }
            for (int level = 0; level < boss.data.length; level++) {
                if (boss.data[level] == targetData) {
                    applyBossConfiguration(boss, level, name, hp, dame, armor, skills, notify);
                    appliedToInstance = true;
                }
            }
        }
        if (!appliedToInstance) {
            applyBossDataValues(targetData, name, hp, dame, armor, skills);
        }
    }

    private void applyBossDataValues(BossData data, String name, int hp, int dame,
            int armor, List<int[]> skills) {
        data.setName(name);
        data.setHp(new int[]{hp});
        data.setDame(dame);
        data.setDefense(armor);
        int[][] configuredSkills = new int[skills.size()][3];
        for (int i = 0; i < skills.size(); i++) {
            int[] spec = skills.get(i);
            configuredSkills[i] = new int[]{spec[0], spec[1], Math.max(1, (spec[2] + 1) / 2)};
        }
        data.setSkillTemp(configuredSkills);
    }

    private Boss findPanelBoss(int bossId, int level) {
        Boss fallback = null;
        for (Boss boss : getPanelBosses()) {
            if (boss == null || boss.id != bossId || boss.data == null || level >= boss.data.length) {
                continue;
            }
            if (boss.currentLevel == level) {
                return boss;
            }
            if (fallback == null) {
                fallback = boss;
            }
        }
        return fallback;
    }

    private List<int[]> parseBossSkills(String text) {
        List<int[]> result = new ArrayList<>();
        if (text == null) {
            return result;
        }
        for (String entry : text.split("[,;\\n]+")) {
            String value = entry.trim();
            if (value.isEmpty()) {
                continue;
            }
            String[] fields = value.split(":");
            if (fields.length != 3) {
                return new ArrayList<>();
            }
            int skillId = parseInt(fields[0], -1);
            int skillLevel = parseInt(fields[1], -1);
            int cooldown = parseInt(fields[2], -1);
            if (skillId < 0 || skillLevel < 1 || cooldown < 200
                    || SkillUtil.createSkill(skillId, skillLevel) == null) {
                return new ArrayList<>();
            }
            result.add(new int[]{skillId, skillLevel, cooldown});
        }
        return result;
    }

    private void applyBossConfiguration(Boss boss, int level, String name, int hp, int dame,
            int armor, List<int[]> skills, boolean notify) {
        if (boss == null || boss.data == null || level < 0 || level >= boss.data.length) {
            return;
        }
        BossData data = boss.data[level];
        applyBossDataValues(data, name, hp, dame, armor, skills);

        if (boss.currentLevel != level || boss.nPoint == null) {
            return;
        }
        synchronized (boss) {
            boss.name = name;
            boss.nPoint.dameg = dame;
            boss.nPoint.hpg = hp;
            boss.nPoint.hp = hp;
            boss.nPoint.calPoint();
            boss.nPoint.hpMax = hp;
            boss.nPoint.hp = hp;
            boss.nPoint.defg = armor <= 0 ? 0 : Math.max(1, armor / 4);
            boss.nPoint.def = armor;
            for (Skill oldSkill : boss.playerSkill.skills) {
                if (oldSkill != null) {
                    oldSkill.dispose();
                }
            }
            boss.playerSkill.skills.clear();
            boss.playerSkill.skillSelect = null;
            for (int[] spec : skills) {
                Skill skill = SkillUtil.createSkill(spec[0], spec[1]);
                if (skill != null) {
                    skill.coolDown = spec[2];
                    boss.playerSkill.skills.add(skill);
                }
            }
        }
        if (notify) {
            try {
                Service.gI().Send_Info_NV(boss);
                PlayerService.gI().sendInfoHpMp(boss);
            } catch (Exception ignored) {
            }
        }
    }

    private void handleBossPersistMode(String[] parts) {
        if (parts.length < 2) {
            return;
        }
        bossPersistMode = "1".equals(parts[1]);
        if (!bossPersistMode) {
            appliedBossOverrides.clear();
            catalogOverridesApplied = false;
            try {
                Files.deleteIfExists(BOSS_OVERRIDES_FILE);
            } catch (IOException ignored) {
            }
        }
        try {
            Files.writeString(BOSS_PERSIST_STATUS_FILE, bossPersistMode ? "1" : "0", StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private void applyBossOverridesIfNeeded() {
        bossPersistMode = readFlag(BOSS_PERSIST_STATUS_FILE);
        if (!bossPersistMode) {
            appliedBossOverrides.clear();
            catalogOverridesApplied = false;
            return;
        }
        Properties overrides = readBossOverrides();
        if (overrides.isEmpty()) {
            return;
        }
        if (!catalogOverridesApplied) {
            for (Map.Entry<String, BossData> entry : getBossDataCatalog().entrySet()) {
                String prefix = "data." + entry.getKey() + ".";
                int hp = readPositiveProperty(overrides, prefix + "hp", 0);
                int dame = readPositiveProperty(overrides, prefix + "dame", 0);
                int armor = parseInt(overrides.getProperty(prefix + "armor", "-1"), -1);
                String name = overrides.getProperty(prefix + "name", "").trim();
                List<int[]> skills = parseBossSkills(overrides.getProperty(prefix + "skills", ""));
                if (hp > 0 && dame > 0 && armor >= 0 && !name.isEmpty() && !skills.isEmpty()) {
                    applyBossDataConfiguration(entry.getValue(), name, hp, dame, armor, skills, false);
                }
            }
            catalogOverridesApplied = true;
        }
        for (Boss boss : getPanelBosses()) {
            if (boss == null || boss.data == null || appliedBossOverrides.contains(boss)) {
                continue;
            }
            for (int level = 0; level < boss.data.length; level++) {
                String prefix = boss.id + "." + level + ".";
                int hp = readPositiveProperty(overrides, prefix + "hp", 0);
                int dame = readPositiveProperty(overrides, prefix + "dame", 0);
                int armor = parseInt(overrides.getProperty(prefix + "armor", "-1"), -1);
                String name = overrides.getProperty(prefix + "name", "").trim();
                List<int[]> skills = parseBossSkills(overrides.getProperty(prefix + "skills", ""));
                if (hp > 0 && dame > 0 && armor >= 0 && !name.isEmpty() && !skills.isEmpty()) {
                    applyBossConfiguration(boss, level, name, hp, dame, armor, skills, false);
                }
            }
            appliedBossOverrides.add(boss);
        }
    }

    private Properties readBossOverrides() {
        Properties props = new Properties();
        if (!Files.exists(BOSS_OVERRIDES_FILE)) {
            return props;
        }
        try (var reader = Files.newBufferedReader(BOSS_OVERRIDES_FILE, StandardCharsets.UTF_8)) {
            props.load(reader);
        } catch (Exception ignored) {
        }
        return props;
    }

    private int readPositiveProperty(Properties props, String key, int fallback) {
        return parseInt(props.getProperty(key, ""), fallback);
    }

    private void saveBossOverride(long bossId, int level, int hp, int dame, int armor,
            String name, List<int[]> skills) {
        try {
            Properties props = readBossOverrides();
            String prefix = bossId + "." + level + ".";
            props.setProperty(prefix + "name", name);
            props.setProperty(prefix + "hp", String.valueOf(hp));
            props.setProperty(prefix + "dame", String.valueOf(dame));
            props.setProperty(prefix + "armor", String.valueOf(armor));
            StringBuilder skillText = new StringBuilder();
            for (int[] skill : skills) {
                if (skillText.length() > 0) {
                    skillText.append(',');
                }
                skillText.append(skill[0]).append(':').append(skill[1]).append(':').append(skill[2]);
            }
            props.setProperty(prefix + "skills", skillText.toString());
            try (var writer = Files.newBufferedWriter(BOSS_OVERRIDES_FILE, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                props.store(writer, "Boss overrides edited from localhost admin panel");
            }
        } catch (IOException ignored) {
        }
    }

    private void saveBossDataOverride(String dataKey, int hp, int dame, int armor,
            String name, List<int[]> skills) {
        try {
            Properties props = readBossOverrides();
            String prefix = "data." + dataKey + ".";
            props.setProperty(prefix + "name", name);
            props.setProperty(prefix + "hp", String.valueOf(hp));
            props.setProperty(prefix + "dame", String.valueOf(dame));
            props.setProperty(prefix + "armor", String.valueOf(armor));
            StringBuilder skillText = new StringBuilder();
            for (int[] skill : skills) {
                if (skillText.length() > 0) {
                    skillText.append(',');
                }
                skillText.append(skill[0]).append(':').append(skill[1]).append(':').append(skill[2]);
            }
            props.setProperty(prefix + "skills", skillText.toString());
            try (var writer = Files.newBufferedWriter(BOSS_OVERRIDES_FILE, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                props.store(writer, "Boss overrides edited from localhost admin panel");
            }
        } catch (IOException ignored) {
        }
    }

    private final Map<String, Integer> bossIdMap = new ConcurrentHashMap<>();

    private Map<String, Integer> getBossIdLookup() {
        if (!bossIdMap.isEmpty()) {
            return bossIdMap;
        }
        for (Field f : BossID.class.getFields()) {
            if (Modifier.isStatic(f.getModifiers()) && f.getType() == int.class) {
                try {
                    bossIdMap.put(f.getName(), f.getInt(null));
                } catch (Exception ignored) {
                }
            }
        }
        return bossIdMap;
    }

    private int resolveBossId(String dataKey, BossData bossData, Boss linkedBoss) {
        if (linkedBoss != null) {
            return (int) linkedBoss.id;
        }
        Map<String, Integer> lookup = getBossIdLookup();
        if (lookup.containsKey(dataKey)) {
            return lookup.get(dataKey);
        }
        String clean = dataKey.replaceAll("_\\d+$", "");
        if (lookup.containsKey(clean)) {
            return lookup.get(clean);
        }
        if (clean.startsWith("FIDE_")) {
            return lookup.getOrDefault("FIDE", 0);
        }
        if (clean.startsWith("XEN_BO_HUNG")) {
            return lookup.getOrDefault("XEN_BO_HUNG", 0);
        }
        if (clean.startsWith("SIEU_BO_HUNG")) {
            return lookup.getOrDefault("SIEU_BO_HUNG", 0);
        }
        if (clean.startsWith("CHILL_PAST")) {
            return lookup.getOrDefault("CHILL_PAST_1", 0);
        }
        if (clean.startsWith("CHILL_")) {
            return lookup.getOrDefault("CHILL_1", 0);
        }
        if (clean.startsWith("TANJIRO")) {
            return lookup.getOrDefault("TANJIRO_DEMON", 0);
        }
        if (clean.startsWith("BROLY") || dataKey.equals("BROLY")) {
            return lookup.getOrDefault("BROLY", 0);
        }
        if (clean.startsWith("SUPER_BROLY") || dataKey.equals("SUPER_BROLY")) {
            return lookup.getOrDefault("SUPER_BROLY", 0);
        }
        for (Map.Entry<String, Integer> entry : lookup.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(dataKey) || entry.getKey().equalsIgnoreCase(clean)) {
                return entry.getValue();
            }
        }
        return 0;
    }

    private Map<String, BossData> getBossDataCatalog() {
        Map<String, BossData> catalog = new LinkedHashMap<>();
        Field[] fields = BossesData.class.getFields();
        Arrays.sort(fields, Comparator.comparing(Field::getName));
        for (Field field : fields) {
            if (!Modifier.isStatic(field.getModifiers()) || field.getType() != BossData.class) {
                continue;
            }
            try {
                BossData data = (BossData) field.get(null);
                if (data != null) {
                    catalog.put(field.getName(), data);
                }
            } catch (IllegalAccessException ignored) {
            }
        }
        // Include any active bosses from manager instances
        for (Boss boss : getPanelBosses()) {
            if (boss == null || boss.data == null) {
                continue;
            }
            for (int level = 0; level < boss.data.length; level++) {
                BossData data = boss.data[level];
                if (data != null && !catalog.containsValue(data)) {
                    String extraKey = "BOSS_" + Math.abs(boss.id) + (boss.data.length > 1 ? ("_" + level) : "");
                    catalog.put(extraKey, data);
                }
            }
        }
        return catalog;
    }

    private void exportBossStatusIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastBossStatusWrite < BOSS_STATUS_INTERVAL_MS) {
            return;
        }
        lastBossStatusWrite = now;
        try {
            JSONObject root = new JSONObject();
            JSONArray bosses = new JSONArray();
            root.put("generatedAt", now);
            root.put("bosses", bosses);

            List<Boss> instances = getPanelBosses();
            Map<String, BossData> catalog = getBossDataCatalog();

            for (Map.Entry<String, BossData> catalogEntry : catalog.entrySet()) {
                String dataKey = catalogEntry.getKey();
                BossData bossData = catalogEntry.getValue();
                Boss linkedBoss = null;
                int linkedLevel = -1;
                for (Boss candidate : instances) {
                    if (candidate == null || candidate.data == null) {
                        continue;
                    }
                    for (int level = 0; level < candidate.data.length; level++) {
                        if (candidate.data[level] != bossData) {
                            continue;
                        }
                        boolean candidateAlive = candidate.zone != null && !candidate.isDie()
                                && candidate.currentLevel == level;
                        boolean linkedAlive = linkedBoss != null && linkedBoss.zone != null
                                && !linkedBoss.isDie() && linkedBoss.currentLevel == linkedLevel;
                        if (linkedBoss == null || (candidateAlive && !linkedAlive)) {
                            linkedBoss = candidate;
                            linkedLevel = level;
                        }
                    }
                }
                boolean alive = linkedBoss != null && linkedBoss.zone != null && !linkedBoss.isDie()
                        && linkedBoss.currentLevel == linkedLevel;
                String bossName = bossData.getName() == null ? dataKey : bossData.getName();
                if (alive && linkedBoss.name != null && !linkedBoss.name.isBlank()) {
                    bossName = linkedBoss.name;
                }
                int resolvedBossId = resolveBossId(dataKey, bossData, linkedBoss);
                int maxHp = 0;
                try {
                    int[] hps = bossData.getHp();
                    if (hps != null && hps.length > 0) {
                        maxHp = hps[0];
                    }
                } catch (Exception ignored) {
                }
                int currentHp = alive && linkedBoss.nPoint != null ? Math.max(0, linkedBoss.nPoint.hp) : maxHp;
                if (maxHp <= 0) {
                    maxHp = Math.max(1, currentHp);
                }

                int defaultMapId = (bossData.getMapJoin() != null && bossData.getMapJoin().length > 0)
                        ? bossData.getMapJoin()[0] : -1;
                String mapName = "Chưa xuất hiện";
                if (alive && linkedBoss.zone != null && linkedBoss.zone.map != null) {
                    mapName = linkedBoss.zone.map.mapName + " (khu " + linkedBoss.zone.zoneId + ")";
                } else if (defaultMapId >= 0) {
                    try {
                        nro.models.map.Map m = MapService.gI().getMapById(defaultMapId);
                        if (m != null && m.mapName != null) {
                            mapName = m.mapName;
                        } else {
                            mapName = "Map " + defaultMapId;
                        }
                    } catch (Exception ignored) {
                        mapName = "Map " + defaultMapId;
                    }
                }

                JSONObject row = new JSONObject();
                row.put("configKey", dataKey);
                row.put("id", resolvedBossId);
                row.put("spawnable", resolvedBossId != 0);
                row.put("name", bossName);
                row.put("level", Math.max(0, linkedLevel));
                row.put("status", linkedBoss == null || linkedBoss.bossStatus == null
                        ? "" : linkedBoss.bossStatus.toString());
                row.put("state", alive ? "Sống" : "Chưa xuất hiện");
                row.put("hp", currentHp);
                row.put("maxHp", maxHp);
                row.put("dame", alive && linkedBoss.nPoint != null ? linkedBoss.nPoint.dameg : bossData.getDame());
                row.put("armor", alive && linkedBoss.nPoint != null ? linkedBoss.nPoint.def : bossData.getDefense());
                row.put("targetHpPercent", linkedBoss == null ? 0 : linkedBoss.damagePercentOfTargetHp);
                
                JSONArray skillRows = new JSONArray();
                int[][] skillTemps = bossData.getSkillTemp();
                if (skillTemps != null) {
                    for (int[] spec : skillTemps) {
                        if (spec == null || spec.length < 2) {
                            continue;
                        }
                        int skillId = spec[0];
                        int skillPoint = Math.max(1, spec[1]);
                        int cooldown = spec.length >= 3 ? Math.max(200, spec[2] * 2) : 1000;
                        JSONObject sRow = new JSONObject();
                        sRow.put("id", skillId);
                        sRow.put("name", getSkillName(skillId));
                        sRow.put("level", skillPoint);
                        sRow.put("cooldown", cooldown);
                        skillRows.add(sRow);
                    }
                }
                row.put("skills", skillRows);
                row.put("mapName", mapName);
                row.put("mapId", alive ? linkedBoss.zone.map.mapId : defaultMapId);
                row.put("zoneId", alive ? linkedBoss.zone.zoneId : 0);
                row.put("respawnIn", 0);
                short[] outfit = bossData.getOutfit();
                row.put("outfit0", outfit != null && outfit.length > 0 ? outfit[0] : 0);
                row.put("outfit1", outfit != null && outfit.length > 1 ? outfit[1] : 0);
                row.put("outfit2", outfit != null && outfit.length > 2 ? outfit[2] : 0);
                bosses.add(row);
            }

            Path tmp = BOSS_STATUS_FILE.resolveSibling(BOSS_STATUS_FILE.getFileName() + ".tmp");
            writeBossStatus(tmp, root.toJSONString());
        } catch (Exception e) {
            Logger.logException(PanelCommandService.class, e);
        }
    }

    private static String getSkillName(int skillId) {
        switch (skillId) {
            case 0: return "Chiêu đấm Dragon";
            case 1: return "Chiêu Kamejoko";
            case 2: return "Chiêu đấm Demon";
            case 3: return "Chiêu Masenko";
            case 4: return "Chiêu đấm Galick";
            case 5: return "Chiêu Antomic";
            case 6: return "Thái Dương Hạ San";
            case 7: return "Tự sát";
            case 8: return "Tái tạo năng lượng";
            case 9: return "Biến sôcôla";
            case 10: return "Dịch chuyển tức thời";
            case 11: return "Khiên năng lượng";
            case 12: return "Biến hình";
            case 13: return "Quả cầu kênh khi";
            case 14: return "Thôi miên";
            case 17: return "Liên hoàn";
            case 19: return "Khiên năng lượng";
            case 20: return "Huýt sáo";
            case 22: return "Thôi miên";
            case 23: return "Trói";
            default: return "Chiêu " + skillId;
        }
    }

    @SuppressWarnings("unchecked")
    private void addSkillStatus(JSONArray rows, Skill skill) {
        if (skill == null || skill.template == null) {
            return;
        }
        JSONObject row = new JSONObject();
        row.put("id", skill.template.id);
        row.put("name", skill.template.name == null ? "Skill " + skill.template.id : skill.template.name);
        row.put("level", Math.max(1, skill.point));
        row.put("cooldown", skill.coolDown);
        rows.add(row);
    }

    private void writeBossStatus(Path tmp, String json) throws IOException {
        Files.writeString(tmp, json, StandardCharsets.UTF_8);
        IOException lastError = null;
        for (int i = 0; i < 5; i++) {
            try {
                Files.move(tmp, BOSS_STATUS_FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                lastError = null;
                break;
            } catch (IOException e) {
                lastError = e;
                try {
                    Files.move(tmp, BOSS_STATUS_FILE, StandardCopyOption.REPLACE_EXISTING);
                    lastError = null;
                    break;
                } catch (IOException fallbackError) {
                    lastError = fallbackError;
                }
                try {
                    Thread.sleep(25L);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw lastError;
                }
            }
        }
        if (lastError != null) {
            Files.writeString(BOSS_STATUS_FILE, json, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Files.deleteIfExists(tmp);
        }
        Path[] mirrors = new Path[]{
            Path.of("data", "htdocs", "sv1", "boss_status.json"),
            Path.of("C:", "xampp1", "htdocs", "sv1", "boss_status.json"),
            Path.of("boss_catalog.json"),
            Path.of("data", "htdocs", "sv1", "boss_catalog.json"),
            Path.of("C:", "xampp1", "htdocs", "sv1", "boss_catalog.json")
        };
        for (Path mirror : mirrors) {
            try {
                if (mirror.getParent() == null || Files.exists(mirror.getParent())) {
                    Files.writeString(mirror, json, StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private List<Boss> getPanelBosses() {
        List<Boss> bosses = new ArrayList<>();
        BossManager[] managers = new BossManager[]{
            BossManager.gI(), BrolyManager.gI(), AnTromManager.gI(),
            OtherBossManager.gI(), FinalBossManager.gI(), SkillSummonedManager.gI(),
            RedRibbonHQManager.gI(), TreasureUnderSeaManager.gI(), SnakeWayManager.gI(),
            GasDestroyManager.gI(), YardartManager.gI(), LunarNewYearEventManager.gI(),
            ChristmasEventManager.gI(), HalloweenEventManager.gI(), HungVuongEventManager.gI(),
            TrungThuEventManager.gI()
        };
        for (BossManager manager : managers) {
            for (Boss boss : new ArrayList<>(manager.getBosses())) {
                if (boss != null && !bosses.contains(boss)) {
                    bosses.add(boss);
                }
            }
        }
        return bosses;
    }

    private boolean isHiddenFromPanel(Boss boss) {
        try {
            int mapJoin = boss.data[0].getMapJoin()[0];
            // Public Doraemon/One Piece/anime maps must appear in the boss panel.
            if (mapJoin >= 238 && mapJoin <= 303) {
                return false;
            }
            return MapService.gI().isMapBossFinal(mapJoin)
                    || MapService.gI().isMapHuyDiet(mapJoin)
                    || MapService.gI().isMapCadic(mapJoin)
                    || MapService.gI().isMapYardart(mapJoin)
                    || MapService.gI().isMapMaBu(mapJoin)
                    || MapService.gI().isMapBlackBallWar(mapJoin);
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isHiddenBossName(String bossName) {
        return bossName != null
                && HIDDEN_BOSS_NAMES.contains(bossName.trim().toLowerCase(Locale.ROOT));
    }

    private String safeBossName(Boss boss, int dataIndex) {
        if (boss.currentLevel == dataIndex && boss.name != null && !boss.name.isBlank()) {
            return boss.name;
        }
        try {
            return boss.data[dataIndex].getName();
        } catch (Exception ignored) {
            return "Boss " + boss.id;
        }
    }

    private int getBossDataIndex(Boss boss) {
        try {
            return boss.currentLevel >= 0 && boss.currentLevel < boss.data.length ? boss.currentLevel : 0;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private short safeBossOutfit(Boss boss, int dataIndex, int part) {
        try {
            if (boss.currentLevel >= 0 && boss.currentLevel < boss.data.length) {
                if (part == 0) {
                    return boss.getHead();
                }
                if (part == 1) {
                    return boss.getBody();
                }
                if (part == 2) {
                    return boss.getLeg();
                }
            }
        } catch (Exception ignored) {
        }
        try {
            short[] outfit = boss.data[dataIndex].getOutfit();
            return outfit.length > part ? outfit[part] : 0;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public static void main(String[] args) {
        try {
            PanelCommandService svc = PanelCommandService.gI();
            svc.lastBossStatusWrite = 0;
            svc.exportBossStatusIfNeeded();
            System.out.println("Exported boss_status.json successfully with "
                    + svc.getBossDataCatalog().size() + " catalog entries.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
