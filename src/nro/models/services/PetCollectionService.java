package nro.models.services;

import java.util.Set;
import java.util.List;
import nro.models.item.Item;
import nro.models.network.Message;
import nro.models.player.Player;
import nro.models.player_system.Template.ItemTemplate;
import nro.models.server.Manager;
import nro.models.boss.BossID;

/** Dữ liệu và hiệu lực của các bộ sưu tập Pet. */
public class PetCollectionService {

    // Các bộ này bám đúng điều kiện cộng chỉ số đang có trong NPoint.
    // Không đếm item có hạn sử dụng; chỉ item vĩnh viễn trong túi/rương/trang bị/rương sưu tầm.
    public static final int[] BABY_DRAGON_IDS = {1765, 1766, 1767, 1768, 1769, 1770, 1771};
    public static final int BABY_DRAGON_REQUIRED = 7;
    public static final int BABY_DRAGON_BONUS_PERCENT = 7;
    public static final int COLLECTION_CHEST_REQUIRED = 30;

    // Bộ trong data item: Thú cưỡi Rồng Đen, Băng, Thiêng và Vô Cực.
    // Khi đủ bốn thú cưỡi vĩnh viễn sẽ cộng 2% SĐ, HP và KI.
    public static final int[] DRAGON_MOUNT_IDS = {1981, 1982, 1983, 1984};
    public static final int[] CELL_COSTUME_IDS = {526, 527, 528, 549};
    // Bộ 5 cải trang Kimetsu: Tanjiro, Inosuke Hashibira, Inosuke,
    // Zenitsu và Nezuko (đúng các ID item 1087-1091 trong data).
    public static final int[] KIMETSU_COSTUME_IDS = {1087, 1088, 1089, 1090, 1091};
    public static final int KIMETSU_DAMAGE_BONUS_PERCENT = 3;
    public static final int KIMETSU_ARMOR_BONUS_PERCENT = 5;
    public static final int[] DEMON_COSTUME_IDS = {908, 909, 910};
    public static final int DEMON_HP_DRAIN_BONUS_PERCENT = 2;
    public static final int DEMON_HP_BONUS_PERCENT = 5;
    public static final int[] CO_HON_GHOST_PET_IDS = {2274, 2275, 2276, 2277};
    public static final int CO_HON_GHOST_PET_DRAIN_BONUS_PERCENT = 3;
    public static final int[] CHIBI_IDS = {2290, 2291, 2292, 2293, 2294, 2295, 2296, 2298};
    public static final int CHIBI_FOOD_ID = 2297;
    public static final int CHIBI_LEVEL_OPTION = 253;
    public static final int CHIBI_MAX_LEVEL = 7;
    public static final int[] JOURNEY_WEST_IDS = {547, 548, 1932, 920};
    public static final int JOURNEY_WEST_DAMAGE_BONUS_PERCENT = 5;
    public static final int[] ARALE_COSTUME_IDS = {612, 613, 614};
    public static final int ARALE_LUCK_BONUS_PERCENT = 10;
    public static final int[] HALLOWEEN_COSTUME_IDS = {642, 643, 644, 645, 646};
    public static final int HALLOWEEN_HP_BONUS_PERCENT = 5;
    public static final int[] DORAEMON_COSTUME_IDS = {806, 819, 862, 863, 864};
    public static final int DORAEMON_GOLD_BONUS_PERCENT = 30;
    public static final int[] SUPER_HERO_COSTUME_IDS = {989, 990, 991};
    public static final int SUPER_HERO_DAMAGE_BONUS_PERCENT = 3;
    public static final int SUPER_HERO_KI_BONUS_PERCENT = 7;
    public static final int[] PI_LAP_COSTUME_IDS = {635, 636, 637};
    public static final int PI_LAP_GOLD_BONUS_PERCENT = 20;
    public static final int DRAGON_MOUNT_REQUIRED = 4;
    public static final int DRAGON_MOUNT_BONUS_PERCENT = 2;

    private static PetCollectionService instance;
    public static final int[] FOOTSTEP_IDS = {2439, 2440, 2441};

    public static PetCollectionService gI() {
        if (instance == null) {
            instance = new PetCollectionService();
        }
        return instance;
    }

    public void openCollectionUI(Player player) {
        if (player == null || player.inventory == null) {
            return;
        }

        sendChibiState(player);
        player.inventory.checkAndUpdateMeRongBadges(player);
        Service.gI().point(player);

        Message msg = null;
        try {
            msg = new Message(-76);
            msg.writer().writeByte(3); // Sưu tầm nhiều bộ pet
            msg.writer().writeByte(14);
            writeCollection(msg, player, "Bộ Rồng Nhí",
                    "Sở hữu đủ 7 loại Rồng Nhí vĩnh viễn", "+7% sức đánh",
                    BABY_DRAGON_IDS);
            writeCollection(msg, player, "Bộ Thú cưỡi Rồng",
                    "Sở hữu đủ 4 Thú cưỡi Rồng vĩnh viễn", "+2% sức đánh, HP và KI",
                    DRAGON_MOUNT_IDS);
            writeCollection(msg, player, "Bộ Xên Bọ Hung",
                    "Sở hữu đủ 4 cải trang Xên Bọ Hung", "+2% sức đánh, HP và KI",
                    CELL_COSTUME_IDS);
            writeCollection(msg, player, "Bộ Cải trang Kimetsu",
                    "Sở hữu đủ 5 cải trang Kimetsu vĩnh viễn", "+3% sức đánh, +5% giáp",
                    KIMETSU_COSTUME_IDS);
            writeCollection(msg, player, "Bộ Ma Quỷ",
                    "Sở hữu đủ Ma phong ba, Thần chết cute và Bí ngô hí nhảnh vĩnh viễn",
                    "+2% hút HP và +5% HP", DEMON_COSTUME_IDS);
            writeCollection(msg, player, "Bộ Pet Cô Hồn",
                    "Sở hữu đủ 4 Pet Cô Hồn vĩnh viễn", "+3% hút HP và +3% hút KI",
                    CO_HON_GHOST_PET_IDS);
            writeCollection(msg, player, "Chibi",
                    "Chọn Chibi đã sở hữu để nâng bằng Thức ăn Chibi",
                    "Cấp 1–7: KI, giáp, dame, may mắn, vàng rơi hoặc chí mạng",
                    CHIBI_IDS);
            writeCollection(msg, player, "Bộ Tây Du",
                    "Sở hữu vĩnh viễn Tôn Ngộ Không, Bát Giới, Đường Tăng và Gậy Như Ý",
                    "+5% sức đánh", JOURNEY_WEST_IDS);
            writeCollection(msg, player, "Bộ Arale",
                    "Sở hữu đủ Cải trang Arale, Gatchan và Obotchaman vĩnh viễn",
                    "+10% may mắn", ARALE_COSTUME_IDS);
            writeCollection(msg, player, "Bộ Halloween",
                    "Sở hữu đủ 5 Cải trang Ma Trơi, Đội Nhí và Bộ Xương vĩnh viễn",
                    "+5% HP", HALLOWEEN_COSTUME_IDS);
            writeCollection(msg, player, "Bộ Doraemon",
                    "Sở hữu đủ Doraemon, Xuka, Nobita, Xêkô và Chaien vĩnh viễn",
                    "+30% vàng rơi từ quái", DORAEMON_COSTUME_IDS);
            writeCollection(msg, player, "Bộ Siêu Nhân",
                    "Sở hữu đủ Gohan, Biden và Bản Cô Nương Siêu Nhân vĩnh viễn",
                    "+3% sức đánh, +7% KI", SUPER_HERO_COSTUME_IDS);
            writeCollection(msg, player, "Bộ Pí Lập",
                    "Sở hữu đủ Cải trang Pí Lập, Mai và Su vĩnh viễn",
                    "+20% vàng rơi từ quái", PI_LAP_COSTUME_IDS);
            writeCollection(msg, player, "Bộ Bước chân",
                    "Sở hữu hoa hồng, hoa xanh và nước vĩnh viễn",
                    "+5.000 SĐ, +10.000 HP, +11.000 KI", FOOTSTEP_IDS);
            player.sendMessage(msg);
        } catch (Exception ignored) {
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    private void writeCollection(Message msg, Player player, String name, String requirement,
            String bonus, int[] itemIds) throws Exception {
        Set<Integer> ownedIds = player.inventory.getPermanentItemTemplateIds(itemIds);
        msg.writer().writeUTF(name);
        msg.writer().writeUTF(requirement);
        msg.writer().writeUTF(bonus);
        msg.writer().writeByte(ownedIds.size());
        msg.writer().writeByte(itemIds.length);
        msg.writer().writeBoolean(ownedIds.size() >= itemIds.length);
        msg.writer().writeByte(itemIds.length);
        for (int itemId : itemIds) {
            msg.writer().writeShort(itemId);
            msg.writer().writeBoolean(ownedIds.contains(itemId));
            // Gửi đúng icon/part/head/body/leg đang được nạp từ SQL. Client chỉ
            // dựng hình theo dữ liệu này, không tự gán ID thay thế.
            ItemTemplate template = itemId >= 0 && itemId < Manager.ITEM_TEMPLATES.size()
                    ? Manager.ITEM_TEMPLATES.get(itemId) : null;
            msg.writer().writeShort(template == null ? -1 : template.iconID);
            msg.writer().writeShort(template == null ? -1 : template.part);
            msg.writer().writeShort(template == null ? -1 : template.head);
            msg.writer().writeShort(template == null ? -1 : template.body);
            msg.writer().writeShort(template == null ? -1 : template.leg);
        }
    }

    public int getCollectionChestDamagePercent(Player player) {
        if (player == null || player.inventory == null) {
            return 0;
        }
        int countItems = player.inventory.getCollectionChestItemCount();
        return countItems >= COLLECTION_CHEST_REQUIRED ? 5 + (countItems - COLLECTION_CHEST_REQUIRED) / 10 : 0;
    }

    public boolean hasCompleteBabyDragonCollection(Player player) {
        return player != null && player.inventory != null
                && player.inventory.getPermanentItemTemplateIds(BABY_DRAGON_IDS).size() >= BABY_DRAGON_REQUIRED;
    }

    public boolean hasCompleteDragonMountCollection(Player player) {
        return player != null && player.inventory != null
                && player.inventory.getPermanentItemTemplateIds(DRAGON_MOUNT_IDS).size() >= DRAGON_MOUNT_REQUIRED;
    }

    public boolean hasCompleteCellCostumeCollection(Player player) {
        return player != null && player.inventory != null
                && player.inventory.getPermanentItemTemplateIds(CELL_COSTUME_IDS).size() >= CELL_COSTUME_IDS.length;
    }

    public boolean hasCompleteKimetsuCostumeCollection(Player player) {
        return player != null && player.inventory != null
                && player.inventory.getPermanentItemTemplateIds(KIMETSU_COSTUME_IDS).size() >= KIMETSU_COSTUME_IDS.length;
    }

    public boolean hasCompleteDemonCostumeCollection(Player player) {
        return player != null && player.inventory != null
                && player.inventory.getPermanentItemTemplateIds(DEMON_COSTUME_IDS).size() >= DEMON_COSTUME_IDS.length;
    }

    public boolean hasCompleteCoHonGhostPetCollection(Player player) {
        return player != null && player.inventory != null
                && player.inventory.getPermanentItemTemplateIds(CO_HON_GHOST_PET_IDS).size() >= CO_HON_GHOST_PET_IDS.length;
    }

    public boolean hasCompleteJourneyWestCollection(Player player) {
        return player != null && player.inventory != null
                && player.inventory.getPermanentItemTemplateIds(JOURNEY_WEST_IDS).size() >= JOURNEY_WEST_IDS.length;
    }

    public boolean hasCompleteAraleCostumeCollection(Player player) {
        return player != null && player.inventory != null
                && player.inventory.getPermanentItemTemplateIds(ARALE_COSTUME_IDS).size() >= ARALE_COSTUME_IDS.length;
    }

    public int getLuckBonusPercent(Player player) {
        int percent = hasCompleteAraleCostumeCollection(player) ? ARALE_LUCK_BONUS_PERCENT : 0;
        percent += getChibiBonus(player, 2294);
        if (player != null && player.zone != null) {
            for (Player boss : player.zone.getBosses()) {
                if (boss != null && boss.id == BossID.QUY_SANTA && !boss.isDie()) {
                    percent -= 30;
                    break;
                }
            }
        }
        return percent;
    }

    public boolean hasCompleteHalloweenCostumeCollection(Player player) {
        return player != null && player.inventory != null
                && player.inventory.getPermanentItemTemplateIds(HALLOWEEN_COSTUME_IDS).size() >= HALLOWEEN_COSTUME_IDS.length;
    }

    public boolean hasCompleteDoraemonCostumeCollection(Player player) {
        return player != null && player.inventory != null
                && player.inventory.getPermanentItemTemplateIds(DORAEMON_COSTUME_IDS).size() >= DORAEMON_COSTUME_IDS.length;
    }

    public int getMobGoldBonusPercent(Player player) {
        int percent = hasCompleteDoraemonCostumeCollection(player) ? DORAEMON_GOLD_BONUS_PERCENT : 0;
        percent += getChibiBonus(player, 2295);
        if (hasCompletePiLapCostumeCollection(player)) {
            percent += PI_LAP_GOLD_BONUS_PERCENT;
        }
        return percent;
    }

    public boolean hasCompleteSuperHeroCostumeCollection(Player player) {
        return player != null && player.inventory != null
                && player.inventory.getPermanentItemTemplateIds(SUPER_HERO_COSTUME_IDS).size() >= SUPER_HERO_COSTUME_IDS.length;
    }

    public boolean hasCompletePiLapCostumeCollection(Player player) {
        return player != null && player.inventory != null
                && player.inventory.getPermanentItemTemplateIds(PI_LAP_COSTUME_IDS).size() >= PI_LAP_COSTUME_IDS.length;
    }
    public void syncChibiFollower(Player player) {
        if (player == null) return;
        sendChibiState(player);
        for (int i = 0; i < CHIBI_IDS.length; i++) {
            if (findChibi(player, CHIBI_IDS[i]) != null) {
                Service.gI().sendChibiPet(player, (short) (30000 + i));
                return;
            }
        }
    }

    public boolean hasAnyChibi(Player player) {
        if (player == null) return false;
        for (int id : CHIBI_IDS) if (findChibi(player, id) != null) return true;
        return false;
    }

    public synchronized void discardChibi(Player player, int itemId) {
        if (!isChibiId(itemId)) return;
        if (player == null || player.inventory == null) return;
        Item chibi = findChibi(player, itemId);
        if (chibi == null) { Service.gI().sendThongBao(player, "Bạn không có Chibi này."); return; }
        String discardedName = chibi.template.name;
        player.inventory.chibi = null;
        InventoryService.gI().sendItemBags(player);
        Service.gI().sendPetFollow(player, (short) 0);
        Service.gI().sendThongBao(player, "Đã vứt " + discardedName + ".");
        openCollectionUI(player);
    }

    public int getChibiLevel(Player player, int itemId) {
        if (player == null || player.inventory == null) return 0;
        Item item = findChibi(player, itemId);
        if (item == null) return 0;
        Item.ItemOption level = item.getOptionById(CHIBI_LEVEL_OPTION);
        return Math.max(1, Math.min(CHIBI_MAX_LEVEL, level == null ? 1 : level.param));
    }

    public int getChibiBonus(Player player, int itemId) { return getChibiLevel(player, itemId) * ((itemId == 2294 || itemId == 2295 || itemId == 2298) ? 2 : 1); }

    public static boolean isChibiId(int id) {
        for (int candidate : CHIBI_IDS) if (candidate == id) return true;
        return false;
    }

    public void loadChibi(Player player, int id, int level) {
        if (isChibiId(id)) {
            player.inventory.chibi = ItemService.gI().createNewItem((short) id);
            player.inventory.chibi.itemOptions.add(new Item.ItemOption(CHIBI_LEVEL_OPTION, Math.max(1, Math.min(7, level))));
            return;
        }
        // Move an existing companion into its dedicated slot without changing its level.
        for (List<Item> items : new List[]{player.inventory.itemsBag, player.inventory.itemsBox, player.inventory.itemsBody}) {
            for (int i = 0; i < items.size(); i++) {
                Item item = items.get(i);
                if (item != null && item.isNotNullItem() && isChibiId(item.template.id) && item.getOptionById(93) == null) {
                    player.inventory.chibi = item;
                    items.set(i, ItemService.gI().createItemNull());
                    return;
                }
            }
        }
    }

    public void sendChibiState(Player player) {
        if (player == null || player.inventory == null) return;
        Message msg = null;
        try {
            msg = new Message(-76);
            msg.writer().writeByte(6);
            Item item = player.inventory.chibi;
            msg.writer().writeShort(item == null ? -1 : item.template.id);
            msg.writer().writeByte(item == null ? 0 : getChibiLevel(player, item.template.id));
            msg.writer().writeUTF(item == null ? "" : item.template.name);
            msg.writer().writeShort(item == null ? -1 : item.template.iconID);
            player.sendMessage(msg);
        } catch (Exception e) { e.printStackTrace(); }
        finally { if (msg != null) msg.cleanup(); }
    }

    private Item findChibi(Player player, int itemId) {
        if (player == null || player.inventory == null) return null;
        Item item = player.inventory.chibi;
        return item != null && item.isNotNullItem() && item.template.id == itemId ? item : null;
    }

    /** Current level N costs N*100 food to reach N+1. */
    public synchronized void upgradeChibi(Player player, int itemId) {
        boolean valid = false; for (int id : CHIBI_IDS) if (id == itemId) valid = true;
        if (!valid || player == null || player.inventory == null) return;
        Item chibi = findChibi(player, itemId);
        if (chibi == null) { Service.gI().sendThongBao(player, "Bạn chưa sở hữu Chibi này."); return; }
        int level = getChibiLevel(player, itemId);
        if (level >= CHIBI_MAX_LEVEL) { Service.gI().sendThongBao(player, "Chibi đã đạt cấp 7."); return; }
        long available = 0;
        for (Item food : player.inventory.itemsBag) {
            if (food != null && food.isNotNullItem() && food.template.id == CHIBI_FOOD_ID)
                available += Math.max(0, food.quantity);
        }
        int targetLevel = level, cost = 0;
        while (targetLevel < CHIBI_MAX_LEVEL && available - cost >= targetLevel * 100) {
            cost += targetLevel * 100;
            targetLevel++;
        }
        if (cost == 0) {
            Service.gI().sendThongBao(player, "Cần " + (level * 100) + " Thức ăn Chibi để lên cấp " + (level + 1) + ".");
            return;
        }
        int remaining = cost;
        for (Item food : new java.util.ArrayList<>(player.inventory.itemsBag)) {
            if (remaining == 0) break;
            if (food == null || !food.isNotNullItem() || food.template.id != CHIBI_FOOD_ID) continue;
            int used = Math.min(remaining, Math.max(0, food.quantity));
            InventoryService.gI().subQuantityItemsBag(player, food, used);
            remaining -= used;
        }
        Item.ItemOption option = chibi.getOptionById(CHIBI_LEVEL_OPTION);
        if (option == null) chibi.itemOptions.add(new Item.ItemOption(CHIBI_LEVEL_OPTION, targetLevel)); else option.param = targetLevel;
        InventoryService.gI().sendItemBags(player);
        Service.gI().point(player);
        Service.gI().sendThongBao(player, chibi.template.name + " đã lên cấp " + targetLevel + ". Đã dùng " + cost + " thức ăn, còn " + (available - cost) + ".");
        openCollectionUI(player);
    }
}

