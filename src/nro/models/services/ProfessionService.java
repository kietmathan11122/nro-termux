package nro.models.services;

import java.util.ArrayList;
import java.util.List;
import nro.models.consts.ConstMap;
import nro.models.map.Map;
import nro.models.map.service.ChangeMapService;
import nro.models.mob.Mob;
import nro.models.network.Message;
import nro.models.npc.Npc;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.player_system.Template;
import nro.models.server.Manager;
import nro.models.utils.Util;

public class ProfessionService {

    public static final byte COMMAND = 118;
    private static final int OBJECTIVE_KILL = 1;
    private static final int OBJECTIVE_EXPLORE = 2;
    private static final int OBJECTIVE_TALK = 3;
    private static final int OBJECTIVE_DELIVERY = 4;
	private static final int DAILY_QUEST_LIMIT = 10;
	private static final int[] FARM_DELIVERY_ITEMS = {2240, 2241, 2242, 2243, 2251, 2252, 2253, 2254};
    private static final ProfessionService INSTANCE = new ProfessionService();

    public static ProfessionService gI() {
        return INSTANCE;
    }

    public void handle(Player player, Message message) {
        try {
            int action = message.reader().readByte();
            switch (action) {
                case 0 -> sendInfo(player);
                case 1 -> chooseProfession(player, message.reader().readByte());
                case 2 -> receiveQuest(player, message.reader().readByte());
                case 3 -> reportQuest(player);
                case 4 -> upgradeStat(player, message.reader().readByte());
                case 5 -> cancelQuest(player);
                default -> sendInfo(player);
            }
        } catch (Exception e) {
            sendInfo(player);
        }
    }

    private void chooseProfession(Player player, int professionId) {
        if (player.professionId != 0) {
            Service.gI().sendThongBao(player, "Bạn đã chọn nghề " + professionName(player.professionId) + ".");
        } else if (professionId < 1 || professionId > 3) {
            Service.gI().sendThongBao(player, "Nghề phụ không hợp lệ.");
        } else {
            player.professionId = professionId;
            Service.gI().sendThongBao(player, "Đã chọn nghề " + professionName(professionId) + ".");
        }
        sendInfo(player);
    }

    private void receiveQuest(Player player, int difficulty) {
        resetDailyLimit(player);
        if (player.professionId == 0) {
            Service.gI().sendThongBao(player, "Hãy chọn nghề phụ trước.");
        } else if (player.dailyQuestType != 0) {
            Service.gI().sendThongBao(player, "Bạn đang có một nhiệm vụ nghề.");
        } else if (player.dailyQuestDoneCount >= DAILY_QUEST_LIMIT) {
            Service.gI().sendThongBao(player, "Hôm nay bạn đã hoàn thành tối đa 10 nhiệm vụ nghề.");
        } else {
            player.dailyQuestType = Math.max(1, Math.min(3, difficulty));
            player.dailyQuestKillCount = 0;
            player.dailyQuestCompletedAndNotified = false;
            createRandomQuest(player);
            Service.gI().sendThongBao(player, "Đã nhận nhiệm vụ: " + objectiveDescription(player) + ".");
        }
        sendInfo(player);
    }

    private void reportQuest(Player player) {
        if (player.dailyQuestType == 0) {
            Service.gI().sendThongBao(player, "Bạn chưa nhận nhiệm vụ nghề.");
        } else if (isFarmerDeliveryQuest(player)) {
            Service.gI().sendThongBao(player, "Hãy mang " + player.dailyQuestItemTarget + " "
                    + itemName(player.dailyQuestItemId) + " giao cho "
                    + npcName(player.dailyQuestMobType) + " tại " + mapName(player.dailyQuestMapId) + ".");
        } else if (player.dailyQuestKillCount < player.dailyQuestKillTarget) {
            Service.gI().sendThongBao(player, "Nhiệm vụ chưa hoàn thành.");
        } else {
            int pointReward = switch (player.dailyQuestType) {
                case 1 -> Util.nextInt(20, 30);
                case 2 -> Util.nextInt(30, 40);
                default -> Util.nextInt(40, 50);
            };
            player.professionPoints += pointReward;
            player.dailyQuestDoneCount++;
            player.dailyQuestLastTime = System.currentTimeMillis();
            clearQuest(player);
            Service.gI().sendThongBao(player, "Báo cáo thành công, nhận " + pointReward + " điểm nghề.");
        }
        sendInfo(player);
    }

    private void cancelQuest(Player player) {
        if (player.dailyQuestType == 0) {
            Service.gI().sendThongBao(player, "Bạn chưa có nhiệm vụ nghề để hủy.");
        } else {
            clearQuest(player);
            Service.gI().sendThongBao(player, "Đã hủy nhiệm vụ nghề hiện tại.");
        }
        sendInfo(player);
    }

    private void upgradeStat(Player player, int encodedValue) {
        int stat = encodedValue % 4;
        int amountMode = encodedValue / 4;
        if (player.professionId == 0) {
            Service.gI().sendThongBao(player, "Hãy chọn nghề phụ trước.");
        } else if (player.professionPoints <= 0) {
            Service.gI().sendThongBao(player, "Bạn chưa có điểm nghề.");
        } else if (encodedValue < 0 || stat < 0 || stat > 3 || amountMode < 0 || amountMode > 2) {
            Service.gI().sendThongBao(player, "Chỉ số không hợp lệ.");
        } else {
            int amount = switch (amountMode) {
                case 0 -> 1;
                case 1 -> 10;
                default -> player.professionPoints;
            };
            if (player.professionPoints < amount) {
                Service.gI().sendThongBao(player, "Bạn cần đủ " + amount + " điểm nghề để cộng.");
                sendInfo(player);
                return;
            }
            player.professionPoints -= amount;
            switch (stat) {
                case 0 -> player.professionHpLevel += amount;
                case 1 -> player.professionKiLevel += amount;
                case 2 -> player.professionArmorLevel += amount;
                case 3 -> player.professionDamageLevel += amount;
            }
            player.nPoint.calPoint();
            PlayerService.gI().sendInfoHpMp(player);
            Service.gI().sendThongBao(player, "Đã cộng " + amount + " điểm vào " + statName(stat) + ".");
        }
        sendInfo(player);
    }

    private void createRandomQuest(Player player) {
        if (player.professionId == 1) {
            createDeliveryQuest(player);
            return;
        }
        player.dailyQuestItemId = Util.nextInt(OBJECTIVE_KILL, OBJECTIVE_TALK);
        player.dailyQuestItemTarget = 0;
        boolean created = switch (objectiveType(player)) {
            case OBJECTIVE_EXPLORE -> createExploreTarget(player, true);
            case OBJECTIVE_TALK -> createTalkTarget(player);
            default -> createKillTarget(player);
        };
        if (!created) {
            player.dailyQuestItemId = OBJECTIVE_KILL;
            created = createKillTarget(player);
        }
        if (!created && player.zone != null && player.zone.map != null) {
            player.dailyQuestMapId = player.zone.map.mapId;
            player.dailyQuestMobType = 1;
            player.dailyQuestKillTarget = 1;
        }
    }

    public void createDeliveryQuest(Player player) {
        List<Map> maps = accessibleMaps(player, false, true);
        maps.removeIf(map -> FarmService.gI().isFarmMap(map.mapId));
        removeCurrentMapWhenPossible(player, maps);
        if (maps.isEmpty()) {
            maps = accessibleMaps(player, false, true);
        }
        if (maps.isEmpty()) {
            player.dailyQuestMapId = player.zone != null ? player.zone.map.mapId : 0;
            player.dailyQuestMobType = 0;
        } else {
            Map deliveryMap = maps.get(Util.nextInt(0, maps.size() - 1));
            List<Npc> npcs = questNpcs(deliveryMap);
            Npc npc = npcs.get(Util.nextInt(0, npcs.size() - 1));
            player.dailyQuestMapId = deliveryMap.mapId;
            player.dailyQuestMobType = npc.tempId;
        }
		player.dailyQuestItemId = FARM_DELIVERY_ITEMS[Util.nextInt(0, FARM_DELIVERY_ITEMS.length - 1)];
		player.dailyQuestItemTarget = deliveryTarget(player.dailyQuestItemId, player.dailyQuestType);
        player.dailyQuestKillCount = 0;
        player.dailyQuestKillTarget = player.dailyQuestItemTarget;
    }

    private boolean createKillTarget(Player player) {
        List<Map> maps = accessibleMaps(player, true, false);
        if (maps.isEmpty()) {
            return false;
        }
        Map questMap = maps.get(Util.nextInt(0, maps.size() - 1));
        List<Mob> mobs = questMobs(questMap);
        if (mobs.isEmpty()) {
            return false;
        }
        player.dailyQuestMapId = questMap.mapId;
        player.dailyQuestMobType = mobs.get(Util.nextInt(0, mobs.size() - 1)).tempId;
        player.dailyQuestKillTarget = switch (player.dailyQuestType) {
            case 1 -> Util.nextInt(20, 40);
            case 2 -> Util.nextInt(50, 80);
            default -> Util.nextInt(100, 150);
        };
        return true;
    }

    private boolean createExploreTarget(Player player, boolean firstTarget) {
        List<Map> maps = accessibleMaps(player, false, false);
        removeCurrentMapWhenPossible(player, maps);
        if (maps.isEmpty()) {
            return false;
        }
        Map questMap = maps.get(Util.nextInt(0, maps.size() - 1));
        player.dailyQuestMapId = questMap.mapId;
        player.dailyQuestMobType = 0;
        if (firstTarget) {
            player.dailyQuestKillTarget = objectiveStepTarget(player.dailyQuestType);
        }
        return true;
    }

    private boolean createTalkTarget(Player player) {
        List<Map> maps = accessibleMaps(player, false, true);
        if (maps.isEmpty()) {
            return false;
        }
        Map questMap = maps.get(Util.nextInt(0, maps.size() - 1));
        List<Npc> npcs = questNpcs(questMap);
        if (npcs.isEmpty()) {
            return false;
        }
        Npc npc = npcs.get(Util.nextInt(0, npcs.size() - 1));
        player.dailyQuestMapId = questMap.mapId;
        player.dailyQuestMobType = npc.tempId;
        if (player.dailyQuestKillTarget <= 0) {
            player.dailyQuestKillTarget = objectiveStepTarget(player.dailyQuestType);
        }
        return true;
    }

    private int objectiveStepTarget(int difficulty) {
        return switch (difficulty) {
            case 1 -> 2;
            case 2 -> 3;
            default -> 5;
        };
    }

    private List<Map> accessibleMaps(Player player, boolean requireMobs, boolean requireNpcs) {
        List<Map> maps = new ArrayList<>();
        for (Map map : Manager.MAPS) {
            if (map == null || map.type != ConstMap.MAP_NORMAL || map.zones == null || map.zones.isEmpty()
                    || map.zones.get(0) == null
                    || ChangeMapService.gI().checkMapCanJoin(player, map.zones.get(0)) == null) {
                continue;
            }
            if (requireMobs && questMobs(map).isEmpty()) {
                continue;
            }
            if (requireNpcs && questNpcs(map).isEmpty()) {
                continue;
            }
            maps.add(map);
        }
        return maps;
    }

    private List<Mob> questMobs(Map map) {
        List<Mob> mobs = new ArrayList<>();
        if (map == null || map.zones == null || map.zones.isEmpty() || map.zones.get(0).mobs == null) {
            return mobs;
        }
        for (Mob mob : map.zones.get(0).mobs) {
            if (mob != null && mob.tempId > 0 && Manager.getMobTemplateByTemp(mob.tempId) != null) {
                mobs.add(mob);
            }
        }
        return mobs;
    }

    private List<Npc> questNpcs(Map map) {
        List<Npc> npcs = new ArrayList<>();
        if (map == null || map.npcs == null) {
            return npcs;
        }
        for (Npc npc : map.npcs) {
            if (npc != null && npc.status != 15 && npc.tempId >= 0 && npc.tempId <= 21
                    && npc.tempId != 3 && npc.tempId != 4 && npc.tempId != 5 && npc.tempId != 6
                    && npc.tempId < Manager.NPC_TEMPLATES.size()
                    && Manager.NPC_TEMPLATES.get(npc.tempId) != null
                    && Manager.NPC_TEMPLATES.get(npc.tempId).name != null
                    && !Manager.NPC_TEMPLATES.get(npc.tempId).name.isBlank()) {
                npcs.add(npc);
            }
        }
        return npcs;
    }

    private void removeCurrentMapWhenPossible(Player player, List<Map> maps) {
        if (player.zone == null || player.zone.map == null || maps.size() <= 1) {
            return;
        }
        maps.removeIf(map -> map.mapId == player.zone.map.mapId);
    }

    public void onKillMob(Player player, Mob mob) {
        if (player == null || mob == null || player.dailyQuestType == 0
                || objectiveType(player) != OBJECTIVE_KILL
                || player.zone == null || player.zone.map.mapId != player.dailyQuestMapId
                || mob.tempId != player.dailyQuestMobType
                || player.dailyQuestKillCount >= player.dailyQuestKillTarget) {
            return;
        }
        player.dailyQuestKillCount++;
        Service.gI().sendThongBao(player, "Tiến độ nhiệm vụ: " + mobName(mob.tempId)
                + " (" + player.dailyQuestKillCount + "/" + player.dailyQuestKillTarget + ")");
        notifyCompleted(player);
    }

    public void onEnterMap(Player player, int mapId) {
        if (player == null || player.dailyQuestType == 0
                || objectiveType(player) != OBJECTIVE_EXPLORE
                || mapId != player.dailyQuestMapId
                || player.dailyQuestKillCount >= player.dailyQuestKillTarget) {
            return;
        }
        player.dailyQuestKillCount++;
        if (player.dailyQuestKillCount >= player.dailyQuestKillTarget) {
            notifyCompleted(player);
        } else {
            createExploreTarget(player, false);
            Service.gI().sendThongBao(player, "Đã khám phá " + player.dailyQuestKillCount + "/"
                    + player.dailyQuestKillTarget + ". Điểm đến tiếp theo: " + mapName(player.dailyQuestMapId) + ".");
        }
        sendInfo(player);
    }

    public void onTalkNpc(Player player, int npcId) {
        if (player != null && isFarmerDeliveryQuest(player)) {
            deliverFarmerOrder(player, npcId);
            return;
        }
        if (player == null || player.dailyQuestType == 0
                || objectiveType(player) != OBJECTIVE_TALK
                || player.zone == null || player.zone.map.mapId != player.dailyQuestMapId
                || npcId != player.dailyQuestMobType
                || player.dailyQuestKillCount >= player.dailyQuestKillTarget) {
            return;
        }
        player.dailyQuestKillCount++;
        if (player.dailyQuestKillCount >= player.dailyQuestKillTarget) {
            notifyCompleted(player);
        } else {
            createTalkTarget(player);
            Service.gI().sendThongBao(player, "Đã gặp NPC " + player.dailyQuestKillCount + "/"
                    + player.dailyQuestKillTarget + ". Tiếp theo hãy gặp " + npcName(player.dailyQuestMobType)
                    + " tại " + mapName(player.dailyQuestMapId) + ".");
        }
        sendInfo(player);
    }

    public void onHarvest(Player player, int harvestedItemId, int quantity) {
        if (player == null || player.professionId != 1 || player.dailyQuestType == 0) {
            return;
        }
        if (!isFarmerDeliveryQuest(player)) {
            // Tương thích nhiệm vụ đã nhận từ NPC hoặc dữ liệu cũ.
            player.dailyQuestKillCount = 0;
            player.dailyQuestCompletedAndNotified = false;
            createDeliveryQuest(player);
        }
        if (harvestedItemId != player.dailyQuestItemId
                || player.dailyQuestKillCount >= player.dailyQuestKillTarget) {
            return;
        }
        player.dailyQuestKillCount = Math.min(player.dailyQuestKillTarget,
                player.dailyQuestKillCount + Math.max(1, quantity));
        Service.gI().sendThongBao(player, "Đã thu hoạch " + itemName(harvestedItemId) + ": "
                + player.dailyQuestKillCount + "/" + player.dailyQuestKillTarget);
        if (player.dailyQuestKillCount >= player.dailyQuestKillTarget) {
            Service.gI().sendThongBao(player, "Đã đủ hàng. Hãy giao cho "
                    + npcName(player.dailyQuestMobType) + " tại " + mapName(player.dailyQuestMapId) + ".");
        }
        sendInfo(player);
    }

    private void deliverFarmerOrder(Player player, int npcId) {
        if (player.zone == null || player.zone.map.mapId != player.dailyQuestMapId
                || npcId != player.dailyQuestMobType) {
            return;
        }
        int bagQuantity = countBagItem(player, player.dailyQuestItemId);
        if (bagQuantity < player.dailyQuestItemTarget) {
            Service.gI().sendThongBao(player, "Chưa đủ " + player.dailyQuestItemTarget + " "
                    + itemName(player.dailyQuestItemId) + " để giao (đang có " + bagQuantity + ").");
            sendInfo(player);
            return;
        }
        int pointReward = switch (player.dailyQuestType) {
            case 1 -> Util.nextInt(20, 30);
            case 2 -> Util.nextInt(30, 40);
            default -> Util.nextInt(40, 50);
        };
        consumeBagItem(player, player.dailyQuestItemId, player.dailyQuestItemTarget);
        InventoryService.gI().sendItemBags(player);
        player.professionPoints += pointReward;
        player.dailyQuestDoneCount++;
        FarmService.gI().onFarmQuestCompleted(player);
        player.dailyQuestLastTime = System.currentTimeMillis();
        clearQuest(player);
        Service.gI().sendThongBao(player, "Ta cảm ơn con! Con nhận được "
                + pointReward + " điểm kỹ năng nghề.");
        sendInfo(player);
    }

    private int countBagItem(Player player, int itemId) {
        int quantity = 0;
        if (player == null || player.inventory == null || player.inventory.itemsBag == null) {
            return quantity;
        }
        for (Item item : player.inventory.itemsBag) {
            if (item != null && item.template != null && item.template.id == itemId && item.quantity > 0) {
                quantity += item.quantity;
            }
        }
        return quantity;
    }

    private void consumeBagItem(Player player, int itemId, int quantity) {
        int remaining = quantity;
        while (remaining > 0) {
            Item item = InventoryService.gI().findItemBag(player, itemId);
            if (item == null) {
                break;
            }
            int amount = Math.min(remaining, item.quantity);
            InventoryService.gI().subQuantityItemsBag(player, item, amount);
            remaining -= amount;
        }
    }

    private void notifyCompleted(Player player) {
        if (!player.dailyQuestCompletedAndNotified
                && player.dailyQuestKillCount >= player.dailyQuestKillTarget) {
            player.dailyQuestCompletedAndNotified = true;
            Service.gI().sendThongBao(player, "Đã hoàn thành nhiệm vụ nghề! Hãy mở tab Nghề phụ và nhấn Báo cáo để nhận điểm.");
        }
    }

    public void sendInfo(Player player) {
        resetDailyLimit(player);
        if (player.dailyQuestType != 0 && player.professionId == 1
                && !isFarmerDeliveryQuest(player)) {
            // Chuyển nhiệm vụ Nông dân cũ sang cơ chế trồng và giao nông sản mới.
            player.dailyQuestKillCount = 0;
            player.dailyQuestCompletedAndNotified = false;
            createDeliveryQuest(player);
        } else if (player.dailyQuestType != 0 && player.dailyQuestItemId == 0) {
            // Nhiệm vụ cũ trước bản cập nhật được giữ dưới dạng đánh quái.
            player.dailyQuestItemId = OBJECTIVE_KILL;
        }
        Message message = null;
        try {
            message = new Message(COMMAND);
            message.writer().writeByte(player.professionId);
            message.writer().writeUTF(professionName(player.professionId));
            message.writer().writeInt(player.professionPoints);
            message.writer().writeInt(player.professionHpLevel);
            message.writer().writeInt(player.professionKiLevel);
            message.writer().writeInt(player.professionArmorLevel);
            message.writer().writeInt(player.professionDamageLevel);
            message.writer().writeByte(player.dailyQuestType);
            message.writer().writeUTF(player.dailyQuestType == 0 ? "" : mapName(player.dailyQuestMapId));
            message.writer().writeUTF(player.dailyQuestType == 0 ? "" : objectiveTarget(player));
            message.writer().writeInt(player.dailyQuestKillCount);
            message.writer().writeInt(player.dailyQuestKillTarget);
            message.writer().writeBoolean(player.dailyQuestType != 0
                    && !isFarmerDeliveryQuest(player)
                    && player.dailyQuestKillCount >= player.dailyQuestKillTarget);
            message.writer().writeInt(player.dailyQuestDoneCount);
            message.writer().writeShort(player.dailyQuestType == 0 ? -1 : player.dailyQuestMapId);
            message.writer().writeShort(isFarmerDeliveryQuest(player) ? player.dailyQuestMobType : -1);
            player.sendMessage(message);
        } catch (Exception ignored) {
        } finally {
            if (message != null) {
                message.cleanup();
            }
        }
    }

    private void resetDailyLimit(Player player) {
        if (Util.isAfterMidnight(player.dailyQuestLastTime)) {
            player.dailyQuestDoneCount = 0;
        }
    }

    private void clearQuest(Player player) {
        player.dailyQuestType = 0;
        player.dailyQuestMapId = 0;
        player.dailyQuestMobType = 0;
        player.dailyQuestKillCount = 0;
        player.dailyQuestKillTarget = 0;
        player.dailyQuestItemTarget = 0;
        player.dailyQuestItemId = 0;
        player.dailyQuestCompletedAndNotified = false;
    }

    private int objectiveType(Player player) {
        if (isFarmerDeliveryQuest(player)) {
            return OBJECTIVE_DELIVERY;
        }
        int objective = player.dailyQuestItemId;
        return objective >= OBJECTIVE_KILL && objective <= OBJECTIVE_TALK ? objective : OBJECTIVE_KILL;
    }

    private boolean isFarmerDeliveryQuest(Player player) {
        return player != null && player.professionId == 1 && player.dailyQuestType != 0
				&& ((player.dailyQuestItemId >= 2240 && player.dailyQuestItemId <= 2243)
					|| (player.dailyQuestItemId >= 2251 && player.dailyQuestItemId <= 2254))
                && player.dailyQuestItemTarget > 0;
    }

	private int deliveryTarget(int itemId, int difficulty) {
		int index = Math.max(1, Math.min(3, difficulty)) - 1;
		return switch (itemId) {
			case 2251 -> new int[]{5, 10, 15}[index];  // Trứng gà
			case 2252 -> new int[]{6, 11, 16}[index];  // Sữa bò
			case 2253 -> new int[]{5, 7, 10}[index];   // Chó con
			case 2254 -> new int[]{8, 13, 17}[index];  // Bộ lông cừu
			default -> new int[]{7, 15, 25}[index];    // Nông sản trồng trọt
		};
	}

    private String objectiveTarget(Player player) {
        return switch (objectiveType(player)) {
            case OBJECTIVE_DELIVERY -> "Giao " + player.dailyQuestItemTarget + " "
                    + itemName(player.dailyQuestItemId) + " cho " + npcName(player.dailyQuestMobType);
            case OBJECTIVE_EXPLORE -> "Khám phá: " + mapName(player.dailyQuestMapId);
            case OBJECTIVE_TALK -> "Gặp " + npcName(player.dailyQuestMobType);
            default -> "Tiêu diệt " + mobName(player.dailyQuestMobType);
        };
    }

    private String objectiveDescription(Player player) {
        return switch (objectiveType(player)) {
			case OBJECTIVE_DELIVERY -> "thu thập " + player.dailyQuestItemTarget + " "
                    + itemName(player.dailyQuestItemId) + " rồi giao cho "
                    + npcName(player.dailyQuestMobType) + " tại " + mapName(player.dailyQuestMapId);
            case OBJECTIVE_EXPLORE -> "đi đến " + mapName(player.dailyQuestMapId)
                    + " (" + player.dailyQuestKillTarget + " điểm đến)";
            case OBJECTIVE_TALK -> "gặp " + npcName(player.dailyQuestMobType) + " tại "
                    + mapName(player.dailyQuestMapId) + " (" + player.dailyQuestKillTarget + " NPC)";
            default -> "đến " + mapName(player.dailyQuestMapId) + " tiêu diệt "
                    + player.dailyQuestKillTarget + " " + mobName(player.dailyQuestMobType);
        };
    }

    private String statName(int stat) {
        return switch (stat) {
            case 0 -> "HP";
            case 1 -> "KI";
            case 2 -> "Giáp";
            default -> "Sức đánh";
        };
    }

    private String itemName(int id) {
        try {
            return ItemService.gI().getTemplate((short) id).name;
        } catch (Exception ignored) {
            return "nông sản";
        }
    }

    private String professionName(int id) {
        return switch (id) {
            case 1 -> "Nông trại";
            case 2 -> "Thợ rèn";
            case 3 -> "Nhà thám hiểm";
            default -> "Chưa chọn";
        };
    }

    private String mobName(int id) {
        Template.MobTemplate template = Manager.getMobTemplateByTemp(id);
        return template != null ? template.name : "Quái vật";
    }

    private String npcName(int id) {
        if (id >= 0 && id < Manager.NPC_TEMPLATES.size()
                && Manager.NPC_TEMPLATES.get(id) != null
                && Manager.NPC_TEMPLATES.get(id).name != null
                && !Manager.NPC_TEMPLATES.get(id).name.isBlank()) {
            return Manager.NPC_TEMPLATES.get(id).name;
        }
        return "NPC";
    }

    private String mapName(int id) {
        for (Map map : Manager.MAPS) {
            if (map != null && map.mapId == id) {
                return map.mapName;
            }
        }
        return "Bản đồ " + id;
    }
}
