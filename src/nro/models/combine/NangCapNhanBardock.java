package nro.models.combine;

import nro.models.consts.ConstNpc;
import nro.models.item.Item;
import nro.models.item.Item.ItemOption;
import nro.models.player.Player;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.services.Service;
import nro.models.utils.Util;
import nro.models.npc.Npc;

/**
 *
 * @author Antigravity
 */
public class NangCapNhanBardock {
    
    private static Npc getNpc(Player player) {
        if (player.idMark.getNpcChose() != null) {
            return player.idMark.getNpcChose();
        }
        return CombineService.gI().baHatMit;
    }
    
    public static int getGoldCost(int level) {
        return 50_000_000;
    }
    
    public static int getGemCost(int level) {
        return 50;
    }
    
    public static int getMaterialCost(int level) {
        return (level + 1) * 100;
    }

    public static int getHeadbandCost(int level) {
        return level + 1;
    }
    
    public static int getSuccessRatio(int level) {
        if (level == 8) {
            return 1;
        }
        return (int) CombineSystem.getTileNangCapDo(level);
    }
    
    public static void showInfoCombine(Player player) {
        if (player.combineNew.itemsCombine.size() >= 2 && player.combineNew.itemsCombine.size() <= 4) {
            Item nhan = null;
            Item material = null;
            Item headband = null;
            Item ngocNangCap = null;
            boolean hasInvalidItem = false;
            
            for (Item item : player.combineNew.itemsCombine) {
                if (item.template.id >= 2020 && item.template.id <= 2029) {
                    nhan = item;
                } else if (item.template.id == 2007) {
                    material = item;
                } else if (item.template.id == 2009) {
                    headband = item;
                } else if (item.template.id == 2030) {
                    ngocNangCap = item;
                } else {
                    hasInvalidItem = true;
                }
            }
            
            if (hasInvalidItem) {
                getNpc(player).createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Hộp kết hợp chứa vật phẩm không hợp lệ!", "Đóng");
                return;
            }
            
            if (nhan != null && material != null) {
                int currentLevel = nhan.template.id - 2020;
                
                if (currentLevel >= 9) {
                    getNpc(player).createOtherMenu(player, ConstNpc.IGNORE_MENU,
                            "Nhẫn Bardock đã đạt cấp tối đa (cấp 9)!", "Đóng");
                    return;
                }
                
                int gold = getGoldCost(currentLevel);
                int gem = getGemCost(currentLevel);
                int ratio = (ngocNangCap != null) ? 100 : getSuccessRatio(currentLevel);
                int requiredMaterial = getMaterialCost(currentLevel);
                int requiredHeadbands = getHeadbandCost(currentLevel);
                
                player.combineNew.goldCombine = gold;
                player.combineNew.gemCombine = gem;
                player.combineNew.ratioCombine = ratio;
                
                String npcSay = "|2|Nhẫn Bardock cấp " + (currentLevel + 1) + "\n\n";
                if (ngocNangCap != null) {
                    npcSay += "|2|Tỉ lệ thành công: 100% (Nhờ Ngọc Nâng Cấp)\n";
                } else {
                    npcSay += "|2|Tỉ lệ thành công: " + ratio + "%\n";
                }
                npcSay += "|2|Cần " + requiredMaterial + " " + material.template.name + " (đang có " + material.quantity + ")\n";
                if (headband != null) {
                    npcSay += "|2|Cần " + requiredHeadbands + " " + headband.template.name + " để chống xịt (đang có " + headband.quantity + ")\n";
                } else if (ngocNangCap == null) {
                    npcSay += "|7|Không dùng Băng buộc đầu: Thất bại ở cấp 2, 4, 6 sẽ bị giảm cấp!\n";
                }
                if (ngocNangCap != null) {
                    npcSay += "|2|Cần 1 Ngọc Nâng Cấp (đang có " + ngocNangCap.quantity + ")\n";
                }
                npcSay += "|2|Cần: " + Util.numberToMoney(gold) + " vàng\n";
                npcSay += "|2|Cần: " + gem + " ngọc\n";
                
                if (material.quantity < requiredMaterial) {
                    npcSay += "\nCòn thiếu " + (requiredMaterial - material.quantity) + " " + material.template.name;
                    getNpc(player).createOtherMenu(player, ConstNpc.IGNORE_MENU, npcSay, "Đóng");
                } else if (headband != null && headband.quantity < requiredHeadbands) {
                    npcSay += "\nCòn thiếu " + (requiredHeadbands - headband.quantity) + " Băng buộc đầu thắm máu";
                    getNpc(player).createOtherMenu(player, ConstNpc.IGNORE_MENU, npcSay, "Đóng");
                } else if (ngocNangCap != null && ngocNangCap.quantity < 1) {
                    npcSay += "\nCòn thiếu 1 Ngọc Nâng Cấp";
                    getNpc(player).createOtherMenu(player, ConstNpc.IGNORE_MENU, npcSay, "Đóng");
                } else if (player.inventory.gem < gem) {
                    npcSay += "\nCòn thiếu " + (gem - player.inventory.gem) + " ngọc";
                    getNpc(player).createOtherMenu(player, ConstNpc.IGNORE_MENU, npcSay, "Đóng");
                } else if (player.inventory.gold < gold) {
                    npcSay += "\nCòn thiếu " + Util.numberToMoney(gold - player.inventory.gold) + " vàng";
                    getNpc(player).createOtherMenu(player, ConstNpc.IGNORE_MENU, npcSay, "Đóng");
                } else {
                    String costText = "Nâng cấp\n" + Util.numberToMoney(gold) + " vàng\n" + gem + " ngọc";
                    if (headband != null) {
                        costText += "\nDùng " + requiredHeadbands + " băng buộc đầu";
                    }
                    if (ngocNangCap != null) {
                        costText += "\nDùng 1 Ngọc Nâng Cấp";
                    }
                    getNpc(player).createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay,
                            costText, "Từ chối");
                }
            } else {
                getNpc(player).createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Cần 1 Nhẫn Bardock (cấp 0 đến 8), Linh hồn đồng đội, và có thể dùng thêm Băng buộc đầu hoặc Ngọc Nâng Cấp.", "Đóng");
            }
        } else {
            getNpc(player).createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Cần 1 Nhẫn Bardock (cấp 0 đến 8), Linh hồn đồng đội, và có thể dùng thêm Băng buộc đầu hoặc Ngọc Nâng Cấp.", "Đóng");
        }
    }
    
    public static void nangCapNhanBardock(Player player) {
        if (player.combineNew.itemsCombine.size() >= 2 && player.combineNew.itemsCombine.size() <= 4) {
            Item nhan = null;
            Item material = null;
            Item headband = null;
            Item ngocNangCap = null;
            boolean hasInvalidItem = false;
            
            for (Item item : player.combineNew.itemsCombine) {
                if (item.template.id >= 2020 && item.template.id <= 2029) {
                    nhan = item;
                } else if (item.template.id == 2007) {
                    material = item;
                } else if (item.template.id == 2009) {
                    headband = item;
                } else if (item.template.id == 2030) {
                    ngocNangCap = item;
                } else {
                    hasInvalidItem = true;
                }
            }
            
            if (hasInvalidItem) {
                return;
            }
            
            if (nhan != null && material != null) {
                int currentLevel = nhan.template.id - 2020;
                if (currentLevel >= 9) {
                    return;
                }
                
                int gold = getGoldCost(currentLevel);
                int gem = getGemCost(currentLevel);
                int ratio = (ngocNangCap != null) ? 100 : getSuccessRatio(currentLevel);
                int requiredMaterial = getMaterialCost(currentLevel);
                int requiredHeadbands = getHeadbandCost(currentLevel);
                
                if (player.inventory.gold < gold) {
                    Service.gI().sendThongBao(player, "Không đủ vàng để thực hiện");
                    return;
                }
                if (player.inventory.gem < gem) {
                    Service.gI().sendThongBao(player, "Không đủ ngọc để thực hiện");
                    return;
                }
                if (material.quantity < requiredMaterial) {
                    Service.gI().sendThongBao(player, "Không đủ " + material.template.name);
                    return;
                }
                if (headband != null && headband.quantity < requiredHeadbands) {
                    Service.gI().sendThongBao(player, "Không đủ " + headband.template.name);
                    return;
                }
                if (ngocNangCap != null && ngocNangCap.quantity < 1) {
                    Service.gI().sendThongBao(player, "Không đủ Ngọc Nâng Cấp");
                    return;
                }
                
                player.inventory.gold -= gold;
                player.inventory.gem -= gem;
                
                InventoryService.gI().subQuantityItemsBag(player, material, requiredMaterial);
                if (headband != null) {
                    InventoryService.gI().subQuantityItemsBag(player, headband, requiredHeadbands);
                }
                if (ngocNangCap != null) {
                    InventoryService.gI().subQuantityItemsBag(player, ngocNangCap, 1);
                }
                
                if (Util.isTrue(ratio, 100)) {
                    int nextId = nhan.template.id + 1;
                    nhan.template = ItemService.gI().getTemplate(nextId);
                    
                    int nextLevel = currentLevel + 1;
                    int percent = 5 + nextLevel;
                    int cm = 1 + nextLevel;
                    
                    nhan.itemOptions.clear();
                    nhan.itemOptions.add(new ItemOption(30, 1));
                    nhan.itemOptions.add(new ItemOption(77, percent));
                    nhan.itemOptions.add(new ItemOption(103, percent));
                    nhan.itemOptions.add(new ItemOption(50, percent));
                    nhan.itemOptions.add(new ItemOption(14, cm));
                    
                    CombineService.gI().sendEffectSuccessCombine(player);
                    Service.gI().sendThongBao(player, "Nâng cấp thành công Nhẫn Bardock cấp " + nextLevel + "!");
                } else {
                    CombineService.gI().sendEffectFailCombine(player);
                    
                    if ((currentLevel == 2 || currentLevel == 4 || currentLevel == 6) && headband == null) {
                        int prevLevel = currentLevel - 1;
                        int prevId = nhan.template.id - 1;
                        nhan.template = ItemService.gI().getTemplate(prevId);
                        
                        int percent = 5 + prevLevel;
                        int cm = 1 + prevLevel;
                        
                        nhan.itemOptions.clear();
                        nhan.itemOptions.add(new ItemOption(30, 1));
                        nhan.itemOptions.add(new ItemOption(77, percent));
                        nhan.itemOptions.add(new ItemOption(103, percent));
                        nhan.itemOptions.add(new ItemOption(50, percent));
                        nhan.itemOptions.add(new ItemOption(14, cm));
                        
                        Service.gI().sendThongBao(player, "Nâng cấp thất bại! Nhẫn Bardock bị giảm xuống cấp " + prevLevel + ".");
                    } else {
                        if (headband != null) {
                            Service.gI().sendThongBao(player, "Nâng cấp thất bại! Nhờ có Băng buộc đầu thẫm máu nên Nhẫn Bardock không bị giảm cấp.");
                        } else {
                            Service.gI().sendThongBao(player, "Nâng cấp thất bại!");
                        }
                    }
                }
                
                InventoryService.gI().sendItemBags(player);
                Service.gI().sendMoney(player);
                CombineService.gI().reOpenItemCombine(player);
                player.combineNew.itemsCombine.clear();
            }
        }
    }
}
