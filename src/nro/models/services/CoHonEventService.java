package nro.models.services;

import nro.models.boss.BossID;
import nro.models.boss.event_co_hon.HonMaCoHon;
import nro.models.boss.event_co_hon.CoHonPetWanderer;
import nro.models.boss.event_co_hon.QuyDoiEscort;
import nro.models.consts.ConstNpc;
import nro.models.item.Item;
import nro.models.map.Map;
import nro.models.npc.Npc;
import nro.models.npc.NpcFactory;
import nro.models.player.Player;
import nro.models.server.Manager;
import nro.models.utils.Util;
import java.time.LocalDate;
import nro.models.data.LocalManager;
import nro.models.data.LocalResultSet;
import nro.models.boss.Boss_Manager.BossManager;
import nro.models.consts.BossStatus;
import nro.models.map.service.ChangeMapService;

public class CoHonEventService {

    public static final int NHANG_TRAM_ITEM_ID = 2255;
    public static final int CHAO_TRANG_ITEM_ID = 2256;
    public static final int HOP_MA_QUY_ITEM_ID = 2257;
    public static final int MAM_CUNG_ITEM_ID = 2278;
    public static final int MAM_CUNG_GEM_PRICE = 20;
    public static final int MAM_CUNG_DAILY_LIMIT = 20;
    public static final int NGUOI_DAN_HON_TITLE_ID = 1925;
    public static final int NGUOI_DAN_HON_EFFECT_ID = 265;
    public static final int NHANG_REQUIRED = 5;
    public static final int CHAO_REQUIRED = 20;
    public static final int CHAO_DROP_PERCENT = 5;
    public static final int ESCORTS_REQUIRED_FOR_TITLE = 100;

    private volatile boolean escortProgressTableReady;
    private volatile boolean mamCungPurchaseTableReady;
    private volatile boolean active;
    private final java.util.List<QuyDoiEscort> eventGhosts
            = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final java.util.List<CoHonPetWanderer> wanderingGhostPets
            = new java.util.concurrent.CopyOnWriteArrayList<>();

    private final java.util.Map<Long, java.util.Set<Integer>> npcIncenseClaims
            = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<Long, LocalDate> npcIncenseClaimDates
            = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<Long, LocalDate> coHonIncenseClaimDates
            = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<Long, QuyDoiEscort> hungryGhostEscorts
            = new java.util.concurrent.ConcurrentHashMap<>();

    private static CoHonEventService instance;

    public static CoHonEventService gI() {
        if (instance == null) {
            instance = new CoHonEventService();
        }
        return instance;
    }

    public synchronized void startEvent() {
        if (active) {
            // Tự sửa lại NPC/quỷ nếu admin bật sk10 lần nữa trong lúc chạy.
            stopEvent();
        }
        active = true;
        removeAllCoHonNpcs();
        // Không đặt Cô Hồn ở ba làng khởi đầu. Đồng thời dọn NPC cũ nếu
        // phương thức này được gọi lại trên một server đang chạy.
        removeNpc(0);
        removeNpc(7);
        removeNpc(14);
        removeNpc(6);
        removeNpc(13);
        removeNpc(20);
        addDestinationNpc(42);
        addDestinationNpc(43);
        addDestinationNpc(44);
        spawnHungryGhosts();
        spawnWanderingGhostPets();
    }

    public synchronized void stopEvent() {
        active = false;
        removeAllCoHonNpcs();
        for (QuyDoiEscort ghost : eventGhosts) {
            if (ghost == null) {
                continue;
            }
            ghost.cancelEscortForEventStop();
            if (ghost.zone != null) {
                ChangeMapService.gI().exitMap(ghost);
            }
            ghost.changeStatus(BossStatus.REST);
            BossManager.gI().removeBoss(ghost);
        }
        eventGhosts.clear();
        for (CoHonPetWanderer pet : wanderingGhostPets) {
            if (pet == null) {
                continue;
            }
            pet.cancelForEventStop();
            pet.changeStatus(BossStatus.REST);
            BossManager.gI().removeBoss(pet);
        }
        wanderingGhostPets.clear();
        hungryGhostEscorts.clear();
    }

    /** Dọn mọi NPC Cô Hồn cũ và ẩn ngay trên client đang đứng trong map. */
    private void removeAllCoHonNpcs() {
        for (Map map : Manager.MAPS) {
            if (map == null || map.npcs == null) {
                continue;
            }
            boolean hasCoHon = map.npcs.stream().anyMatch(npc -> npc.tempId == ConstNpc.CO_HON);
            if (!hasCoHon) {
                continue;
            }
            if (map.zones != null) {
                for (nro.models.map.Zone zone : map.zones) {
                    if (zone == null) {
                        continue;
                    }
                    for (Player player : new java.util.ArrayList<>(zone.getPlayers())) {
                        if (player != null) {
                            Service.gI().sendHideNpc(player, ConstNpc.CO_HON, true);
                        }
                    }
                }
            }
            map.npcs.removeIf(npc -> npc.tempId == ConstNpc.CO_HON);
        }
        Manager.NPCS.removeIf(npc -> npc.tempId == ConstNpc.CO_HON);
    }

    public boolean isActive() {
        return active;
    }

    private void removeNpc(int mapId) {
        Map map = Manager.MAPS.stream().filter(m -> m.mapId == mapId).findFirst().orElse(null);
        if (map != null) {
            map.npcs.removeIf(npc -> npc.tempId == ConstNpc.CO_HON);
        }
        // Npc constructor đăng ký thêm vào Manager.NPCS; dọn cả danh sách này
        // để bật/tắt nhiều lần không giữ đối tượng NPC Cô Hồn cũ.
        Manager.NPCS.removeIf(npc -> npc.mapId == mapId && npc.tempId == ConstNpc.CO_HON);
    }

    private void addDestinationNpc(int mapId) {
        Map map = Manager.MAPS.stream().filter(m -> m.mapId == mapId).findFirst().orElse(null);
        if (map == null || map.npcs.stream().anyMatch(npc -> npc.tempId == ConstNpc.CO_HON)) {
            return;
        }
        int[] position = findSafeNpcPosition(map);
        int x = position[0];
        int y = position[1];
        Npc npc = NpcFactory.createNPC(mapId, 1, x, y, ConstNpc.CO_HON);
        if (npc != null) {
            map.npcs.add(npc);
        }
    }

    private int[] findSafeNpcPosition(Map map) {
        int center = Math.max(48, map.mapWidth / 2);
        int bestX = center;
        int bestY = map.yPhysicInTop(center, 24);
        int bestDistance = Integer.MAX_VALUE;
        // Không phải cột tile nào cũng có mặt đất. Quét toàn chiều ngang và
        // lấy điểm đứng hợp lệ gần giữa map nhất.
        for (int x = 48; x < map.mapWidth - 24; x += 24) {
            int groundY = map.yPhysicInTop(x, 24);
            if (groundY > 24 && groundY < map.mapHeight) {
                int distance = Math.abs(x - center);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestX = x;
                    bestY = groundY;
                }
            }
        }
        if (bestY <= 0) {
            bestY = Math.max(24, map.mapHeight - 24);
        }
        return new int[]{bestX, bestY};
    }

    private void spawnHungryGhosts() {
        try {
            int[] earthMaps = {0, 1, 2, 3, 4, 5, 6, 24, 27, 28, 29, 30, 39, 42, 47};
            int[] namekMaps = {7, 8, 9, 10, 11, 12, 13, 25, 31, 32, 33, 34, 43};
            int[] xaydaMaps = {14, 15, 16, 17, 18, 19, 20, 26, 35, 36, 37, 38, 44};
            spawnHungryGhostOnEveryMap(earthMaps, 42);
            spawnHungryGhostOnEveryMap(namekMaps, 43);
            spawnHungryGhostOnEveryMap(xaydaMaps, 44);
        } catch (Exception e) {
            Service.gI().sendThongBaoAllPlayer("Không thể khởi tạo Quỷ Đói sự kiện Cô Hồn.");
        }
    }


    private void spawnHungryGhostOnEveryMap(int[] routeMaps, int destinationMap) throws Exception {
        for (int spawnMap : routeMaps) {
            eventGhosts.add(new QuyDoiEscort(routeMaps, destinationMap, spawnMap));
        }
    }

    /** Mỗi vách có pet đi chậm để người chơi tìm và dâng Mâm Cúng. */
    private void spawnWanderingGhostPets() {
        try {
            addWanderingPet(BossID.PET_MA_DOI_CO_HON, 2274, 42, "Pet Ma Đói");
            addWanderingPet(BossID.PET_MA_LUOI_DAI_CO_HON, 2275, 43, "Pet Ma Nhà Giàu");
            addWanderingPet(BossID.PET_MA_TROI_CO_HON, 2276, 44, "Pet Ma Trơi");
            int[] cliffs = {42, 43, 44};
            addWanderingPet(BossID.PET_MA_CHET_OAN_CO_HON, 2277,
                    cliffs[Util.nextInt(cliffs.length)], "Pet Ma Chết Oan");
        } catch (Exception e) {
            Service.gI().sendThongBaoAllPlayer("Không thể tạo Pet Cô Hồn lang thang.");
        }
    }

    private void addWanderingPet(int bossId, int itemId, int mapId, String name) throws Exception {
        var template = ItemService.gI().getTemplate(itemId);
        short[] outfit = {(short) template.head, (short) template.body, (short) template.leg, -1, -1, -1};
        CoHonPetWanderer pet = new CoHonPetWanderer(bossId, itemId, mapId, name, outfit);
        wanderingGhostPets.add(pet);
        pet.changeStatus(BossStatus.RESPAWN);
    }

    public boolean rewardHungryGhostEscort(Player player) {
        if (!active) {
            Service.gI().sendThongBao(player, "Sự kiện Cô Hồn hiện chưa được bật.");
            return false;
        }
        Item gift = ItemService.gI().createNewItem((short) HOP_MA_QUY_ITEM_ID);
        gift.quantity = 1;
        if (!InventoryService.gI().addItemBag(player, gift)) {
            Service.gI().sendThongBao(player, "Hành trang đã đầy, chưa thể nhận Hộp Quà.");
            return false;
        }
        int escortCount = incrementEscortCount(player.id);
        boolean ownsTitle = InventoryService.gI().findItemBag(player, NGUOI_DAN_HON_TITLE_ID) != null
                || InventoryService.gI().findItemBody(player, NGUOI_DAN_HON_TITLE_ID) != null
                || player.dataBadges != null && player.dataBadges.stream()
                        .anyMatch(badge -> badge.idBadGes == NGUOI_DAN_HON_EFFECT_ID);
        if (escortCount >= ESCORTS_REQUIRED_FOR_TITLE && !ownsTitle) {
            Item title = ItemService.gI().createNewItem((short) NGUOI_DAN_HON_TITLE_ID);
            title.quantity = 1;
            if (InventoryService.gI().addItemBag(player, title)) {
                Service.gI().sendThongBao(player,
                        "Bạn đã dẫn đủ 100 Quỷ Đói và nhận danh hiệu Người Dẫn Hồn: +5% HP và +5% KI!");
            }
        } else if (escortCount < ESCORTS_REQUIRED_FOR_TITLE) {
            Service.gI().sendThongBao(player, "Tiến độ danh hiệu Người Dẫn Hồn: "
                    + escortCount + "/" + ESCORTS_REQUIRED_FOR_TITLE + ".");
        }
        InventoryService.gI().sendItemBags(player);
        Service.gI().sendThongBao(player, "Bạn đã dắt Quỷ Đói về với Cô Hồn và nhận 1 Hộp Ma Quỷ!");
        return true;
    }

    private synchronized int incrementEscortCount(long playerId) {
        try {
            ensureEscortProgressTable();
            LocalManager.executeUpdate(
                    "INSERT INTO co_hon_escort_progress(player_id, escort_count) VALUES (?, 1) "
                    + "ON DUPLICATE KEY UPDATE escort_count = LEAST(escort_count + 1, ?)",
                    playerId, ESCORTS_REQUIRED_FOR_TITLE);
            LocalResultSet rs = LocalManager.executeQuery(
                    "SELECT escort_count FROM co_hon_escort_progress WHERE player_id = ?", playerId);
            return rs.next() ? rs.getInt("escort_count") : 0;
        } catch (Exception e) {
            Service.gI().sendThongBaoAllPlayer("Không thể lưu tiến độ danh hiệu Người Dẫn Hồn.");
            return 0;
        }
    }

    private void ensureEscortProgressTable() throws Exception {
        if (escortProgressTableReady) {
            return;
        }
        LocalManager.executeUpdate(
                "CREATE TABLE IF NOT EXISTS co_hon_escort_progress ("
                + "player_id BIGINT NOT NULL PRIMARY KEY, "
                + "escort_count INT NOT NULL DEFAULT 0, "
                + "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP) "
                + "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        escortProgressTableReady = true;
    }

    public void openDemonBox(Player player) {
        Item box = InventoryService.gI().findItemBag(player, HOP_MA_QUY_ITEM_ID);
        if (box == null || box.quantity < 1) {
            Service.gI().sendThongBao(player, "Bạn không có Hộp Ma Quỷ.");
            return;
        }

        int reward = Util.nextInt(100);
        String message;
        if (reward < 40) {
            if (InventoryService.gI().getCountEmptyBag(player) < 1) {
                Service.gI().sendThongBao(player, "Hành trang đã đầy, chưa thể mở Hộp Ma Quỷ.");
                return;
            }
            int[] demonItems = {908, 909, 910};
            Item costume = ItemService.gI().createNewItem((short) demonItems[Util.nextInt(demonItems.length)]);
            costume.itemOptions.clear();
            // Mỗi món chỉ nhận đúng một trong ba dòng chỉ số.
            switch (Util.nextInt(3)) {
                case 0 -> costume.itemOptions.add(new Item.ItemOption(50, Util.nextInt(5, 15)));
                case 1 -> costume.itemOptions.add(new Item.ItemOption(77, Util.nextInt(5, 20)));
                default -> costume.itemOptions.add(new Item.ItemOption(103, Util.nextInt(5, 20)));
            }
            costume.itemOptions.add(new Item.ItemOption(30, 0));
            if (Util.isTrue(5, 100)) {
                costume.itemOptions.add(new Item.ItemOption(73, 0));
                message = "Bạn nhận được " + costume.template.name + " vĩnh viễn!";
            } else {
                int days = Util.nextInt(7, 30);
                costume.itemOptions.add(new Item.ItemOption(93, days));
                message = "Bạn nhận được " + costume.template.name + " hạn " + days + " ngày.";
            }
            InventoryService.gI().addItemBag(player, costume);
        } else if (reward < 60) {
            player.inventory.addGold(20_000_000);
            Service.gI().sendMoney(player);
            message = "Bạn nhận được 20 triệu vàng.";
        } else if (reward < 80) {
            player.inventory.gem += 50;
            Service.gI().sendMoney(player);
            message = "Bạn nhận được 50 ngọc.";
        } else {
            if (InventoryService.gI().getCountEmptyBag(player) < 1) {
                Service.gI().sendThongBao(player, "Hành trang đã đầy, chưa thể mở Hộp Ma Quỷ.");
                return;
            }
            Item dragonBall = ItemService.gI().createNewItem((short) Util.nextInt(14, 20));
            dragonBall.quantity = 1;
            InventoryService.gI().addItemBag(player, dragonBall);
            message = "Bạn nhận được 1 " + dragonBall.template.name + ".";
        }
        InventoryService.gI().subQuantityItemsBag(player, box, 1);
        InventoryService.gI().sendItemBags(player);
        Service.gI().sendThongBao(player, message);
    }

    public void buyMamCung(Player player) {
        buyMamCung(player, 1);
    }

    public synchronized void buyMamCung(Player player, int quantity) {
        if (quantity != 1 && quantity != 20) {
            return;
        }
        if (!active) {
            Service.gI().sendThongBao(player, "Sự kiện Cô Hồn hiện chưa được bật.");
            return;
        }
        int boughtToday = getMamCungBoughtToday(player.id);
        int remaining = MAM_CUNG_DAILY_LIMIT - boughtToday;
        if (remaining < quantity) {
            Service.gI().sendThongBao(player, "Hôm nay bạn chỉ còn mua được " + Math.max(0, remaining) + " Mâm Cúng.");
            return;
        }
        int price = MAM_CUNG_GEM_PRICE * quantity;
        if (player.inventory.gem < price) {
            Service.gI().sendThongBao(player, "Cần " + price + " ngọc để mua " + quantity + " Mâm Cúng.");
            return;
        }
        if (InventoryService.gI().getCountEmptyBag(player) < 1) {
            Service.gI().sendThongBao(player, "Hành trang đã đầy.");
            return;
        }
        Item tray = ItemService.gI().createNewItem((short) MAM_CUNG_ITEM_ID);
        tray.quantity = quantity;
        if (!InventoryService.gI().addItemBag(player, tray)) {
            Service.gI().sendThongBao(player, "Không thể thêm Mâm Cúng vào hành trang.");
            return;
        }
        player.inventory.gem -= price;
        recordMamCungPurchase(player.id, quantity);
        InventoryService.gI().sendItemBags(player);
        Service.gI().sendMoney(player);
        Service.gI().sendThongBao(player, "Bạn nhận được " + quantity + " Mâm Cúng (" + (boughtToday + quantity) + "/" + MAM_CUNG_DAILY_LIMIT
                + "). Có thể giao dịch; hãy dùng tại Vách núi Aru, Moori hoặc Kakarot.");
    }

    private int getMamCungBoughtToday(long playerId) {
        try {
            ensureMamCungPurchaseTable();
            LocalDate today = LocalDate.now();
            LocalResultSet rs = LocalManager.executeQuery(
                    "SELECT amount FROM co_hon_mam_cung_purchase WHERE player_id = ? AND purchase_date = ?", playerId, today);
            int amount = rs.next() ? rs.getInt("amount") : 0;
            return amount;
        } catch (Exception e) {
            // Không trừ ngọc nếu cơ sở dữ liệu không ghi được giới hạn ngày.
            return MAM_CUNG_DAILY_LIMIT;
        }
    }

    private void recordMamCungPurchase(long playerId, int quantity) {
        try {
            ensureMamCungPurchaseTable();
            LocalManager.executeUpdate(
                    "INSERT INTO co_hon_mam_cung_purchase(player_id, purchase_date, amount) VALUES (?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE amount = amount + VALUES(amount)", playerId, LocalDate.now(), quantity);
        } catch (Exception ignored) {
            // Sau khi đã thêm item, chỉ ghi nhận thất bại; không trừ thêm tài sản.
        }
    }

    private void ensureMamCungPurchaseTable() throws Exception {
        if (mamCungPurchaseTableReady) {
            return;
        }
        LocalManager.executeUpdate(
                "CREATE TABLE IF NOT EXISTS co_hon_mam_cung_purchase ("
                + "player_id BIGINT NOT NULL, purchase_date DATE NOT NULL, amount INT NOT NULL DEFAULT 0, "
                + "PRIMARY KEY(player_id, purchase_date)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        mamCungPurchaseTableReady = true;
    }

    public void useMamCung(Player player, Item tray) {
        if (!active) {
            Service.gI().sendThongBao(player, "Sự kiện Cô Hồn hiện chưa được bật.");
            return;
        }
        if (player.zone == null || (player.zone.map.mapId != 42 && player.zone.map.mapId != 43 && player.zone.map.mapId != 44)) {
            Service.gI().sendThongBao(player, "Chỉ dùng Mâm Cúng tại Vách núi Aru, Moori hoặc Kakarot.");
            return;
        }
        if (tray == null || tray.quantity < 1 || InventoryService.gI().getCountEmptyBag(player) < 1) {
            Service.gI().sendThongBao(player, "Hành trang đã đầy hoặc không có Mâm Cúng.");
            return;
        }
        CoHonPetWanderer wanderingPet = findNearbyWanderingGhostPet(player);
        if (wanderingPet == null) {
            Service.gI().sendThongBao(player,
                    "Hãy tìm Pet Cô Hồn đang đi lại gần bạn rồi mới dâng Mâm Cúng.");
            return;
        }
        Item pet = ItemService.gI().createNewItem((short) wanderingPet.getPetItemId());
        pet.quantity = 1;
        pet.itemOptions.clear();
        pet.itemOptions.add(new Item.ItemOption(95, 5));
        pet.itemOptions.add(new Item.ItemOption(96, 5));
        switch (Util.nextInt(3)) {
            case 0 -> pet.itemOptions.add(new Item.ItemOption(50, Util.nextInt(5, 10)));
            case 1 -> pet.itemOptions.add(new Item.ItemOption(77, Util.nextInt(5, 10)));
            default -> pet.itemOptions.add(new Item.ItemOption(103, Util.nextInt(5, 10)));
        }
        String message;
        if (Util.isTrue(7, 100)) {
            pet.itemOptions.add(new Item.ItemOption(73, 0));
            message = "Bạn nhận được " + pet.template.name + " vĩnh viễn!";
        } else {
            int days = Util.nextInt(7, 30);
            pet.itemOptions.add(new Item.ItemOption(93, days));
            message = "Bạn nhận được " + pet.template.name + " hạn " + days + " ngày.";
        }
        InventoryService.gI().subQuantityItemsBag(player, tray, 1);
        InventoryService.gI().addItemBag(player, pet);
        InventoryService.gI().sendItemBags(player);
        wanderingPet.acceptOffering();
        Service.gI().sendThongBao(player, message);
    }

    private CoHonPetWanderer findNearbyWanderingGhostPet(Player player) {
        for (CoHonPetWanderer pet : wanderingGhostPets) {
            if (pet != null && pet.isNear(player)) {
                return pet;
            }
        }
        return null;
    }

    public boolean registerHungryGhostEscort(Player player, QuyDoiEscort ghost) {
        QuyDoiEscort current = hungryGhostEscorts.putIfAbsent(player.id, ghost);
        if (current != null && current != ghost) {
            return false;
        }
        return true;
    }

    public void unregisterHungryGhostEscort(Player player, QuyDoiEscort ghost) {
        if (player != null) {
            hungryGhostEscorts.remove(player.id, ghost);
        }
    }

    public void followEscortAfterMapChange(Player player) {
        QuyDoiEscort ghost = hungryGhostEscorts.get(player.id);
        if (ghost != null) {
            ghost.followTo(player);
        }
    }

    public void deliverHungryGhost(Player player, Npc npc) {
        QuyDoiEscort ghost = hungryGhostEscorts.get(player.id);
        if (ghost == null) {
            Service.gI().sendThongBao(player, "Bạn chưa dắt theo Quỷ Đói nào.");
            return;
        }
        ghost.deliverAtNpc(player, npc);
    }

    public void giveIncense(Player player) {
        if (!active) {
            Service.gI().sendThongBao(player, "Sự kiện Cô Hồn hiện chưa được bật.");
            return;
        }
        LocalDate today = LocalDate.now();
        if (today.equals(coHonIncenseClaimDates.get(player.id))) {
            Service.gI().sendThongBao(player,
                    "Hôm nay bạn đã nhận 5 Nhang Trầm từ Cô Hồn rồi.");
            return;
        }
        Item incense = ItemService.gI().createNewItem((short) NHANG_TRAM_ITEM_ID);
        incense.quantity = 5;
        if (!InventoryService.gI().addItemBag(player, incense)) {
            Service.gI().sendThongBao(player, "Hành trang đã đầy, chưa thể nhận Nhang Trầm.");
            return;
        }
        coHonIncenseClaimDates.put(player.id, today);
        InventoryService.gI().sendItemBags(player);
        Service.gI().sendThongBao(player,
                "Cô Hồn tặng bạn 5 Nhang Trầm. Mỗi ngày chỉ nhận được một lần.");
    }

    public void giveIncenseFromNpc(Player player, int npcTemplateId) {
        if (!active) {
            return;
        }
        LocalDate today = LocalDate.now();
        LocalDate claimDate = npcIncenseClaimDates.put(player.id, today);
        if (!today.equals(claimDate)) {
            npcIncenseClaims.remove(player.id);
        }
        java.util.Set<Integer> claimed = npcIncenseClaims.computeIfAbsent(
                player.id, ignored -> java.util.concurrent.ConcurrentHashMap.newKeySet());
        if (!claimed.add(npcTemplateId)) {
            Service.gI().sendThongBao(player,
                    "Hôm nay bạn đã nhận Nhang Trầm từ NPC này rồi.");
            return;
        }
        Item incense = ItemService.gI().createNewItem((short) NHANG_TRAM_ITEM_ID);
        incense.quantity = 1;
        if (InventoryService.gI().addItemBag(player, incense)) {
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player,
                    "NPC tặng bạn 1 Nhang Trầm. Mỗi ngày chỉ nhận một lần từ NPC này.");
        } else {
            claimed.remove(npcTemplateId);
            Service.gI().sendThongBao(player, "Hành trang đã đầy, chưa thể nhận Nhang Trầm.");
        }
    }

    public void useChaoTrang(Player player, Item chao) {
        if (!active) {
            Service.gI().sendThongBao(player, "Sự kiện Cô Hồn hiện chưa được bật.");
            return;
        }
        Item incense = InventoryService.gI().findItemBag(player, NHANG_TRAM_ITEM_ID);
        if (incense == null || incense.quantity < NHANG_REQUIRED) {
            Service.gI().sendThongBao(player,
                    "Cần đủ " + NHANG_REQUIRED + " Nhang Trầm để triệu hồi Hồn Ma.");
            return;
        }
        if (chao.quantity < CHAO_REQUIRED) {
            Service.gI().sendThongBao(player,
                    "Cần đủ " + CHAO_REQUIRED + " Cháo Trắng để triệu hồi Hồn Ma.");
            return;
        }
        if (player.zone == null || player.isDie()) {
            Service.gI().sendThongBao(player, "Không thể triệu hồi Hồn Ma lúc này.");
            return;
        }
        boolean ghostAlreadyHere = player.zone.getBosses().stream()
                .anyMatch(boss -> boss.id == BossID.HON_MA_CO_HON && !boss.isDie());
        if (ghostAlreadyHere) {
            Service.gI().sendThongBao(player, "Trong khu vực đã có một Hồn Ma được triệu hồi.");
            return;
        }
        boolean friendly = Util.isTrue(50, 100);
        try {
            new HonMaCoHon(player, friendly);
            InventoryService.gI().subQuantityItemsBag(player, incense, NHANG_REQUIRED);
            InventoryService.gI().subQuantityItemsBag(player, chao, CHAO_REQUIRED);
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player,
                    "Bạn đã dùng 5 Nhang Trầm và 20 Cháo Trắng. Một Hồn Ma đang tìm đến...");
        } catch (Exception e) {
            Service.gI().sendThongBao(player, "Triệu hồi Hồn Ma thất bại, Cháo Trắng chưa bị trừ.");
        }
    }

    public void rewardSummoner(Player player) {
        int reward = Util.nextInt(2);
        switch (reward) {
            case 0 -> {
                int gold = Util.nextInt(10_000_000, 100_000_000);
                player.inventory.addGold(gold);
                Service.gI().sendMoney(player);
                Service.gI().sendThongBao(player, "Hồn Ma tặng bạn " + Util.numberToMoney(gold) + " vàng.");
            }
            case 1 -> {
                int gem = Util.nextInt(100, 1_000);
                player.inventory.gem += gem;
                Service.gI().sendMoney(player);
                Service.gI().sendThongBao(player, "Hồn Ma tặng bạn " + gem + " ngọc.");
            }
        }
    }
}
