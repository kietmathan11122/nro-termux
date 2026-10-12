package nro.models.npc_list;

import nro.models.consts.ConstNpc;
import nro.models.consts.ConstTask;
import nro.models.consts.ConstMap;
import nro.models.npc.Npc;
import nro.models.map.Map;
import nro.models.mob.Mob;
import nro.models.player.Player;
import nro.models.server.Manager;
import nro.models.shop.ShopService;
import nro.models.services.TaskService;
import nro.models.services.Service;
import nro.models.services.PlayerService;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.services.FarmService;
import nro.models.services.ProfessionService;
import nro.models.item.Item;
import nro.models.map.service.ChangeMapService;
import nro.models.utils.Util;
import java.util.ArrayList;
import java.util.List;

public class Lemo extends Npc {

    public Lemo(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
                openProfessionMenu(player);
            }
        }
    }

    public void openProfessionMenu(Player player) {
        this.createOtherMenu(player, ConstNpc.BASE_MENU,
                "Cậu cần gì ở cửa hàng Lemo?",
                "Cửa hàng Lemo",
                "Bán vật phẩm\nBa/Bọ Ma (Lemo)",
                "Về nhà",
                "Đóng");
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (player.idMark.isBaseMenu()) {
                switch (select) {
                    case 0 -> {
                        ShopService.gI().opendShop(player, "LEMO", true);
                    }
                    case 1 -> {
                        // Bán vật phẩm
                        int[] sellableIds = {2056, 2057, 2059, 2060, 2062, 2063, 2064, 2065, 2067, 2068, 2069, 2070};
                        java.util.List<String> options = new java.util.ArrayList<>();
                        for (int id : sellableIds) {
                            Item item = InventoryService.gI().findItemBag(player, id);
                            if (item != null && item.quantity >= 99) {
                                options.add("Bán 99 " + item.template.name);
                            }
                        }
                        
                        Item iron = InventoryService.gI().findItemBag(player, 2066);
                        if (iron != null && iron.quantity > 0) {
                            options.add("Bán Thỏi Sắt (20 Đ.Xu/cái)");
                        }
                        
                        options.add("Đóng");
                        String[] optionArr = options.toArray(new String[0]);
                        
                        this.createOtherMenu(player, 364, 
                                "Cậu muốn bán vật phẩm nào để đổi lấy Đồng Xu?", 
                                optionArr);
                    }
                    case 2 -> {
                        nro.models.map.service.ChangeMapService.gI().changeMapBySpaceShip(player, player.gender + 21, 0, -1);
                    }
                }
            } else {
                int menuIndex = player.idMark.getIndexMenu();
                switch (menuIndex) {
                    case 365 -> {
                        if (player.professionId == 0 && select >= 0 && select <= 2) {
                            int professionId = select + 1;
                            String description = switch (professionId) {
                                case 1 -> "Nông dân: trồng và thu hoạch nông sản tại farm.";
                                case 2 -> "Thợ rèn: nhận nhiệm vụ ngẫu nhiên trên các bản đồ.";
                                default -> "Nhà thám hiểm: nhận nhiệm vụ ngẫu nhiên trên các bản đồ.";
                            };
                            this.createOtherMenu(player, 369 + professionId,
                                    description + "\nBạn chỉ được chọn nghề một lần. Xác nhận?",
                                    "Xác nhận", "Hủy");
                        }
                    }
                    case 366 -> {
                        if (select >= 0 && select <= 3) {
                            upgradeProfessionStat(player, select);
                        }
                    }
                    case 370, 371, 372 -> {
                        if (select == 0 && player.professionId == 0) {
                            player.professionId = menuIndex - 369;
                            Service.gI().sendThongBao(player, "Đã chọn nghề "
                                    + getProfessionName(player.professionId) + ".");
                            openProfessionMenu(player);
                        }
                    }
                    case 360 -> {
                        // Chọn độ khó
                        if (select >= 0 && select <= 2) {
                            player.dailyQuestType = select + 1;
                            player.dailyQuestKillCount = 0;
                            player.dailyQuestCompletedAndNotified = false;
                            setupProfessionQuest(player);
                            sendProfessionQuestNotice(player);
                        }
                    }
                    case 362 -> {
                        // Nhận thưởng
                        if (select == 0) {
                            int targetMob = player.dailyQuestMobType;
                            int itemId = player.dailyQuestItemId;
                            
                            boolean isKillDone = player.dailyQuestKillCount >= player.dailyQuestKillTarget;
                            boolean isItemDone = true;
                            Item questItem = null;
                            if (player.dailyQuestItemTarget > 0) {
                                questItem = InventoryService.gI().findItemBag(player, itemId);
                                int currentItemCount = (questItem != null) ? questItem.quantity : 0;
                                isItemDone = currentItemCount >= player.dailyQuestItemTarget;
                            }
                            
                            if (isKillDone && isItemDone) {
                                // Thu hồi vật phẩm nếu có yêu cầu
                                if (player.dailyQuestItemTarget > 0 && questItem != null) {
                                    InventoryService.gI().subQuantityItemsBag(player, questItem, player.dailyQuestItemTarget);
                                    InventoryService.gI().sendItemBags(player);
                                }
                                
                                // Phát thưởng Đồng Xu
                                int coinReward = 0;
                                switch (player.dailyQuestType) {
                                    case 1 -> coinReward = Util.nextInt(1, 5);
                                    case 2 -> coinReward = Util.nextInt(3, 7);
                                    case 3 -> coinReward = 10;
                                    case 4 -> coinReward = 15;
                                }
                                
                                player.inventory.ruby += coinReward;
                                int pointReward = switch (player.dailyQuestType) {
                                    case 1 -> Util.nextInt(20, 30);
                                    case 2 -> Util.nextInt(30, 40);
                                    default -> Util.nextInt(40, 50);
                                };
                                player.professionPoints += pointReward;
                                Service.gI().sendMoney(player);
                                Service.gI().sendThongBao(player, "Hoàn thành nhiệm vụ "
                                        + getProfessionName(player.professionId) + "! Nhận "
                                        + coinReward + " Đồng Xu và " + pointReward + " điểm nghề.");
                                
                                // Tăng số lần hoàn thành và lưu thời gian
                                player.dailyQuestDoneCount++;
                                player.dailyQuestLastTime = System.currentTimeMillis();
                                
                                if (TaskService.gI().getIdTask(player) == ConstTask.TASK_36_3) {
                                    TaskService.gI().doneTask(player, ConstTask.TASK_36_3);
                                }
                                
                                // Reset quest state
                                player.dailyQuestType = 0;
                                player.dailyQuestMobType = 0;
                                player.dailyQuestMapId = 0;
                                player.dailyQuestKillCount = 0;
                                player.dailyQuestKillTarget = 0;
                                player.dailyQuestItemTarget = 0;
                                player.dailyQuestItemId = 0;
                                player.dailyQuestCompletedAndNotified = false;
                            } else {
                                Service.gI().sendThongBao(player, "Chưa hoàn thành đủ điều kiện!");
                            }
                        } else if (select == 1) {
                            // Hủy nhiệm vụ
                            player.dailyQuestType = 0;
                            player.dailyQuestMobType = 0;
                            player.dailyQuestMapId = 0;
                            player.dailyQuestKillCount = 0;
                            player.dailyQuestKillTarget = 0;
                            player.dailyQuestItemTarget = 0;
                            player.dailyQuestItemId = 0;
                            player.dailyQuestCompletedAndNotified = false;
                            Service.gI().sendThongBao(player, "Đã hủy nhiệm vụ hiện tại.");
                        }
                    }
                    case 363 -> {
                        // Hủy nhiệm vụ khi chưa xong
                        if (select == 0) {
                            player.dailyQuestType = 0;
                            player.dailyQuestMobType = 0;
                            player.dailyQuestMapId = 0;
                            player.dailyQuestKillCount = 0;
                            player.dailyQuestKillTarget = 0;
                            player.dailyQuestItemTarget = 0;
                            player.dailyQuestItemId = 0;
                            player.dailyQuestCompletedAndNotified = false;
                            Service.gI().sendThongBao(player, "Đã hủy nhiệm vụ hiện tại.");
                        }
                    }
                    case 364 -> {
                        // Bán vật phẩm
                        int[] sellableIds = {2056, 2057, 2059, 2060, 2062, 2063, 2064, 2065, 2067, 2068, 2069, 2070};
                        java.util.List<Integer> validIds = new java.util.ArrayList<>();
                        for (int id : sellableIds) {
                            Item item = InventoryService.gI().findItemBag(player, id);
                            if (item != null && item.quantity >= 99) {
                                validIds.add(id);
                            }
                        }
                        
                        Item iron = InventoryService.gI().findItemBag(player, 2066);
                        boolean hasIron = (iron != null && iron.quantity > 0);
                        int ironOptionIndex = validIds.size();
                        
                        if (hasIron && select == ironOptionIndex) {
                            int qty = iron.quantity;
                            int reward = qty * 20;
                            InventoryService.gI().subQuantityItemsBag(player, iron, qty);
                            InventoryService.gI().sendItemBags(player);
                            player.inventory.ruby += reward;
                            Service.gI().sendMoney(player);
                            Service.gI().sendThongBao(player, "Bán thành công " + qty + " Thỏi Sắt! Nhận được " + reward + " Đồng Xu.");
                        } else if (select >= 0 && select < validIds.size()) {
                            int idToSell = validIds.get(select);
                            Item itemToSell = InventoryService.gI().findItemBag(player, idToSell);
                            if (itemToSell != null && itemToSell.quantity >= 99) {
                                InventoryService.gI().subQuantityItemsBag(player, itemToSell, 99);
                                InventoryService.gI().sendItemBags(player);
                                player.inventory.ruby += 5;
                                Service.gI().sendMoney(player);
                                Service.gI().sendThongBao(player, "Bán thành công 99 " + itemToSell.template.name + "! Nhận 5 Đồng Xu.");
                            } else {
                                Service.gI().sendThongBao(player, "Không đủ 99 vật phẩm!");
                            }
                        }
                    }
                }
            }
        }
    }

    private void openChooseProfessionMenu(Player player) {
        this.createOtherMenu(player, 365,
                "Hãy chọn một nghề phụ. Nghề đã chọn không thể thay đổi.",
                "Nông dân", "Thợ rèn", "Nhà thám hiểm", "Đóng");
    }

    private void openUpgradeMenu(Player player) {
        this.createOtherMenu(player, 366,
                "Nghề: " + getProfessionName(player.professionId)
                        + "\nĐiểm nghề còn lại: " + player.professionPoints,
                "Tăng HP +1\nĐã cộng " + player.professionHpLevel,
                "Tăng KI +1\nĐã cộng " + player.professionKiLevel,
                "Tăng giáp +1\nĐã cộng " + player.professionArmorLevel,
                "Tăng sức đánh +1\nĐã cộng " + player.professionDamageLevel,
                "Đóng");
    }

    private void upgradeProfessionStat(Player player, int stat) {
        if (player.professionId == 0) {
            openChooseProfessionMenu(player);
            return;
        }
        if (player.professionPoints <= 0) {
            Service.gI().sendThongBao(player, "Bạn chưa có điểm nghề.");
            return;
        }
        player.professionPoints--;
        switch (stat) {
            case 0 -> player.professionHpLevel++;
            case 1 -> player.professionKiLevel++;
            case 2 -> player.professionArmorLevel++;
            default -> player.professionDamageLevel++;
        }
        player.nPoint.calPoint();
        PlayerService.gI().sendInfoHpMp(player);
        Service.gI().sendThongBao(player, "Nâng chỉ số nghề thành công.");
        openUpgradeMenu(player);
    }

    private void setupProfessionQuest(Player player) {
        if (player.professionId == 1) {
            ProfessionService.gI().createDeliveryQuest(player);
            return;
        }
        List<Map> maps = new ArrayList<>();
        for (Map map : Manager.MAPS) {
            if (map == null || map.type != ConstMap.MAP_NORMAL || map.zones == null || map.zones.isEmpty()
                    || map.zones.get(0) == null || map.zones.get(0).mobs == null || map.zones.get(0).mobs.isEmpty()) {
                continue;
            }
            if (ChangeMapService.gI().checkMapCanJoin(player, map.zones.get(0)) != null) {
                maps.add(map);
            }
        }

        Map questMap = maps.isEmpty() ? player.zone.map : maps.get(Util.nextInt(0, maps.size() - 1));
        List<Mob> mobs = new ArrayList<>();
        if (questMap.zones != null && !questMap.zones.isEmpty()) {
            for (Mob mob : questMap.zones.get(0).mobs) {
                if (mob != null && mob.tempId > 0 && Manager.getMobTemplateByTemp(mob.tempId) != null) {
                    mobs.add(mob);
                }
            }
        }

        player.dailyQuestMapId = questMap.mapId;
        player.dailyQuestMobType = mobs.isEmpty() ? 1 : mobs.get(Util.nextInt(0, mobs.size() - 1)).tempId;
        player.dailyQuestItemId = 0;
        player.dailyQuestItemTarget = 0;
        switch (player.dailyQuestType) {
            case 1 -> player.dailyQuestKillTarget = Util.nextInt(20, 40);
            case 2 -> player.dailyQuestKillTarget = Util.nextInt(50, 80);
            case 3 -> player.dailyQuestKillTarget = Util.nextInt(100, 150);
            default -> player.dailyQuestKillTarget = Util.nextInt(200, 300);
        }
    }

    private void sendProfessionQuestNotice(Player player) {
        if (player.professionId == 1) {
            String itemName = ItemService.gI().getTemplate((short) player.dailyQuestItemId).name;
            Service.gI().sendThongBao(player, "Nhận nhiệm vụ Nông dân thành công! Hãy trồng "
                    + player.dailyQuestItemTarget + " " + itemName + " rồi giao cho "
                    + Manager.NPC_TEMPLATES.get(player.dailyQuestMobType).name + " tại "
                    + getQuestMapName(player) + ".");
            return;
        }
        String targetName = getQuestMobName(player);
        String message = "Nhận nhiệm vụ " + getProfessionName(player.professionId)
                + " thành công! Hãy đến " + getQuestMapName(player)
                + " và tiêu diệt " + player.dailyQuestKillTarget + " " + targetName;
        if (player.dailyQuestItemTarget > 0) {
            String itemName = ItemService.gI().getTemplate((short) player.dailyQuestItemId).name;
            message += " và thu thập " + player.dailyQuestItemTarget + " " + itemName;
        }
        Service.gI().sendThongBao(player, message + ".");
    }

    private String getQuestMobName(Player player) {
        nro.models.player_system.Template.MobTemplate template
                = Manager.getMobTemplateByTemp(player.dailyQuestMobType);
        return template != null ? template.name : "quái vật";
    }

    private String getQuestMapName(Player player) {
        for (Map map : Manager.MAPS) {
            if (map != null && map.mapId == player.dailyQuestMapId) {
                return map.mapName;
            }
        }
        return "bản đồ " + player.dailyQuestMapId;
    }

    private String getProfessionName(int professionId) {
        return switch (professionId) {
            case 1 -> "Nông dân";
            case 2 -> "Thợ rèn";
            case 3 -> "Nhà thám hiểm";
            default -> "Chưa chọn";
        };
    }
}
