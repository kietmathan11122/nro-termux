package nro.models.services;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import nro.models.boss.Boss;
import nro.models.item.Item;
import nro.models.map.Zone;
import nro.models.map.service.ChangeMapService;
import nro.models.map.service.MapService;
import nro.models.boss.Boss_Manager.AnTromManager;
import nro.models.boss.Boss_Manager.BossManager;
import nro.models.boss.Boss_Manager.BrolyManager;
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
import nro.models.network.Message;
import nro.models.player.Player;
import nro.models.server.Manager;
import nro.models.utils.Logger;

/** Gửi toàn bộ Boss cùng trạng thái sống/chết và bảng rơi đọc trực tiếp từ mã nguồn Boss. */
public final class BossBrowserService {

    public static final int BOSS_RADAR_ITEM_ID = 2374;
    private static final BossBrowserService INSTANCE = new BossBrowserService();
    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern LINE_COMMENT = Pattern.compile("//.*?(?:\\R|$)");
    private static final Pattern ITEM_MAP_CALL = Pattern.compile(
            "new\\s+(?:[\\w$.]+\\.)?ItemMap\\s*\\(\\s*[^,]+,\\s*(?:\\([^)]*\\)\\s*)?([A-Za-z_$][\\w$]*|\\d+)",
            Pattern.MULTILINE);
    private static final Pattern NUMBER = Pattern.compile("(?<![\\w])\\d{1,5}(?![\\w])");
    private final Map<Class<?>, short[]> dropCache = new ConcurrentHashMap<>();
    private final Map<Long, Long> lastRadarTravel = new ConcurrentHashMap<>();
    private volatile Path sourceRoot;

    public static BossBrowserService gI() {
        return INSTANCE;
    }

    public void openRadar(Player player) {
        if (player == null) return;
        Item radar = InventoryService.gI().findItemBag(player, BOSS_RADAR_ITEM_ID);
        if (radar == null || radar.quantity <= 0) return;
        Message msg = null;
        try {
            msg = new Message(-76);
            msg.writer().writeByte(8);
            player.sendMessage(msg);
        } catch (Exception e) {
            Logger.logException(BossBrowserService.class, e);
        } finally {
            if (msg != null) msg.cleanup();
        }
        sendBosses(player);
    }

    public synchronized void teleportToBoss(Player player, long bossId, int mapId, int zoneId) {
        if (player == null || player.zone == null || player.isDie()) return;
        Item radar = InventoryService.gI().findItemBag(player, BOSS_RADAR_ITEM_ID);
        if (radar == null || radar.quantity <= 0) {
            Service.gI().sendThongBao(player, "Cần Radar Boss trong hành trang để dịch chuyển");
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastRadarTravel.getOrDefault(player.id, 0L) < 1500L) return;
        Boss target = null;
        for (Boss boss : getBosses()) {
            if (boss.id == bossId && isAlive(boss)
                    && boss.zone.map.mapId == mapId && boss.zone.zoneId == zoneId) {
                target = boss;
                break;
            }
        }
        if (target == null) {
            Service.gI().sendThongBao(player, "Boss đã rời đi hoặc bị tiêu diệt");
            sendBosses(player);
            return;
        }
        Zone destination = target.zone;
        if (destination != player.zone && (MapService.gI().isMapPhoBan(destination.map.mapId)
                || MapService.gI().isMapOffline(destination.map.mapId))) {
            Service.gI().sendThongBao(player, "Không thể dùng radar vào bản đồ riêng");
            return;
        }
        if (destination != player.zone && destination.getNumOfPlayers() >= destination.maxPlayer) {
            Service.gI().sendThongBao(player, "Khu Boss đã đầy");
            return;
        }
        if (ChangeMapService.gI().checkMapCanJoin(player, destination) != destination
                || ChangeMapService.gI().checkMapCanJoinByYardart(player, destination) != destination) {
            Service.gI().sendThongBao(player, "Bạn chưa đủ điều kiện vào bản đồ Boss");
            return;
        }
        int x = Math.max(24, Math.min(destination.map.mapWidth - 24, target.location.x + 35));
        ChangeMapService.gI().changeMapYardrat(player, destination, x, target.location.y);
        if (player.zone != destination || player.location.x != x) {
            Service.gI().sendThongBao(player, "Dịch chuyển thất bại, Radar Boss không bị trừ");
            return;
        }
        InventoryService.gI().subQuantityItemsBag(player, radar, 1);
        InventoryService.gI().sendItemBags(player);
        lastRadarTravel.put(player.id, now);
        Message msg = null;
        try {
            msg = new Message(-76);
            msg.writer().writeByte(9);
            player.sendMessage(msg);
        } catch (Exception e) {
            Logger.logException(BossBrowserService.class, e);
        } finally {
            if (msg != null) msg.cleanup();
        }
        Service.gI().sendThongBao(player, "Đã dịch chuyển đến " + safeName(target) + " (-1 Radar Boss)");
    }

    public void sendBosses(Player player) {
        if (player == null) {
            return;
        }
        Message msg = null;
        try {
            List<Boss> bosses = getBosses();
            msg = new Message(-76);
            msg.writer().writeByte(4);
            msg.writer().writeShort(Math.min(65535, bosses.size()));
            for (Boss boss : bosses) {
                boolean alive = isAlive(boss);
                String mapName = alive && boss.zone.map.mapName != null ? boss.zone.map.mapName : "Đã chết";
                int mapId = alive ? boss.zone.map.mapId : -1;
                int zoneId = alive ? boss.zone.zoneId : -1;
                long hp = alive ? Math.max(0L, boss.nPoint.hp) : 0L;
                long hpMax = safeHpMax(boss);
                short[] outfit = safeOutfit(boss);
                msg.writer().writeLong(boss.id);
                msg.writer().writeUTF(safeName(boss));
                msg.writer().writeUTF(mapName);
                msg.writer().writeShort(mapId);
                msg.writer().writeShort(zoneId);
                msg.writer().writeBoolean(alive);
                msg.writer().writeLong(hp);
                msg.writer().writeLong(hpMax);
                msg.writer().writeShort(outfit[0]);
                msg.writer().writeShort(outfit[1]);
                msg.writer().writeShort(outfit[2]);
                short[] drops = resolveDropItemIds(boss);
                msg.writer().writeByte(Math.min(255, drops.length));
                for (int i = 0; i < drops.length && i < 255; i++) {
                    msg.writer().writeShort(drops[i]);
                }
            }
            player.sendMessage(msg);
        } catch (Exception e) {
            Logger.logException(BossBrowserService.class, e);
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    private List<Boss> getBosses() {
        BossManager[] managers = {
            BossManager.gI(), BrolyManager.gI(), FinalBossManager.gI(), OtherBossManager.gI(),
            YardartManager.gI(), SkillSummonedManager.gI(), RedRibbonHQManager.gI(),
            TreasureUnderSeaManager.gI(), SnakeWayManager.gI(), GasDestroyManager.gI(),
            TrungThuEventManager.gI(), HalloweenEventManager.gI(), ChristmasEventManager.gI(),
            HungVuongEventManager.gI(), LunarNewYearEventManager.gI(), AnTromManager.gI()
        };
        IdentityHashMap<Boss, Boolean> seen = new IdentityHashMap<>();
        List<Boss> result = new ArrayList<>();
        for (BossManager manager : managers) {
            for (Boss boss : new ArrayList<>(manager.getBosses())) {
                if (boss == null || seen.put(boss, Boolean.TRUE) != null) {
                    continue;
                }
                result.add(boss);
            }
        }
        result.sort(Comparator.comparing((Boss boss) -> !isAlive(boss))
                .thenComparing(this::safeName, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    private boolean isAlive(Boss boss) {
        return boss != null && boss.zone != null && boss.zone.map != null && boss.nPoint != null
                && !boss.isDie() && boss.nPoint.hp > 0;
    }

    private int safeLevel(Boss boss) {
        if (boss != null && boss.data != null && boss.data.length > 0) {
            if (boss.currentLevel >= 0 && boss.currentLevel < boss.data.length) {
                return boss.currentLevel;
            }
            return 0;
        }
        return -1;
    }

    private long safeHpMax(Boss boss) {
        if (boss != null && boss.nPoint != null && boss.nPoint.hpMax > 0) {
            return boss.nPoint.hpMax;
        }
        try {
            int level = safeLevel(boss);
            int[] hp = boss.data[level].getHp();
            if (hp != null && hp.length > 0) {
                return Math.max(1L, hp[0]);
            }
        } catch (Exception ignored) {
        }
        return 1L;
    }

    private short[] safeOutfit(Boss boss) {
        short[] result = {0, 0, 0};
        try {
            if (safeLevel(boss) == boss.currentLevel) {
                result[0] = boss.getHead();
                result[1] = boss.getBody();
                result[2] = boss.getLeg();
                return result;
            }
        } catch (Exception ignored) {
        }
        try {
            int level = safeLevel(boss);
            short[] outfit = boss.data[level].getOutfit();
            for (int i = 0; i < result.length && i < outfit.length; i++) {
                result[i] = outfit[i];
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    private String safeName(Boss boss) {
        if (boss.name != null && !boss.name.isBlank()) {
            return boss.name;
        }
        try {
            int level = safeLevel(boss);
            return boss.data[level].getName();
        } catch (Exception ignored) {
            return "Boss " + boss.id;
        }
    }

    private short[] resolveDropItemIds(Boss boss) {
        return dropCache.computeIfAbsent(boss.getClass(), this::readDropItemIds);
    }

    /**
     * Đọc các ID truyền vào ItemMap ngay trong lớp reward của Boss. Cách này giữ
     * bảng hiển thị đồng bộ với source thay vì hard-code một danh sách giả ở client.
     */
    private short[] readDropItemIds(Class<?> bossClass) {
        LinkedHashSet<Integer> ids = new LinkedHashSet<>();
        for (Class<?> type = bossClass; type != null && Boss.class.isAssignableFrom(type); type = type.getSuperclass()) {
            String source = readSource(type);
            if (source == null) {
                continue;
            }
            source = LINE_COMMENT.matcher(BLOCK_COMMENT.matcher(source).replaceAll(" ")).replaceAll(" ");
            Matcher calls = ITEM_MAP_CALL.matcher(source);
            while (calls.find()) {
                String itemExpression = calls.group(1);
                if (itemExpression.matches("\\d+")) {
                    addValidItem(ids, Integer.parseInt(itemExpression));
                } else {
                    collectVariableValues(source, itemExpression, ids);
                }
            }
            if (!ids.isEmpty()) {
                break;
            }
        }
        short[] result = new short[Math.min(24, ids.size())];
        int index = 0;
        for (int id : ids) {
            if (index >= result.length) {
                break;
            }
            result[index++] = (short) id;
        }
        return result;
    }

    private void collectVariableValues(String source, String variable, Set<Integer> ids) {
        Pattern assignment = Pattern.compile("(?:\\b(?:int|short|byte)\\s+)?\\b" + Pattern.quote(variable)
                + "\\s*=\\s*([^;]+);");
        Matcher values = assignment.matcher(source);
        while (values.find()) {
            String rhs = values.group(1);
            Matcher arrayUse = Pattern.compile("\\b([A-Za-z_$][\\w$]*)\\s*\\[").matcher(rhs);
            boolean usedArray = false;
            while (arrayUse.find()) {
                usedArray = true;
                collectArrayValues(source, arrayUse.group(1), ids);
            }
            if (!usedArray) {
                collectNumbers(rhs, ids);
            }
        }
    }

    private void collectArrayValues(String source, String arrayName, Set<Integer> ids) {
        Pattern array = Pattern.compile("\\b" + Pattern.quote(arrayName) + "\\s*(?:\\[\\s*\\])?\\s*=\\s*\\{([^}]+)\\}");
        Matcher matcher = array.matcher(source);
        while (matcher.find()) {
            collectNumbers(matcher.group(1), ids);
        }
    }

    private void collectNumbers(String text, Set<Integer> ids) {
        Matcher numbers = NUMBER.matcher(text);
        while (numbers.find()) {
            addValidItem(ids, Integer.parseInt(numbers.group()));
        }
    }

    private void addValidItem(Set<Integer> ids, int id) {
        if (id < 0 || id >= Manager.ITEM_TEMPLATES.size()) {
            return;
        }
        try {
            if (Manager.ITEM_TEMPLATES.get(id) != null && Manager.ITEM_TEMPLATES.get(id).name != null
                    && !Manager.ITEM_TEMPLATES.get(id).name.isBlank()) {
                ids.add(id);
            }
        } catch (Exception ignored) {
        }
    }

    private String readSource(Class<?> type) {
        Path root = findSourceRoot();
        if (root == null) {
            return null;
        }
        Path file = root.resolve(type.getName().replace('.', '/') + ".java");
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            return null;
        }
    }

    private Path findSourceRoot() {
        if (sourceRoot != null) {
            return sourceRoot;
        }
        Path cursor = Path.of("").toAbsolutePath();
        for (int i = 0; i < 8 && cursor != null; i++, cursor = cursor.getParent()) {
            Path candidate = cursor.resolve("src");
            if (Files.isDirectory(candidate.resolve("nro/models/boss"))) {
                sourceRoot = candidate;
                return candidate;
            }
        }
        return null;
    }
}
