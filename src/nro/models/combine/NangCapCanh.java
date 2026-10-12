package nro.models.combine;

import nro.models.consts.ConstNpc;
import nro.models.item.Item;
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
public class NangCapCanh {
    
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
        // c1: 24 viên, cấp sau nhân đôi
        return 24 * (1 << (level - 1));
    }
    
    public static int getSuccessRatio(int level) {
        // tỉ lệ c1 50%, c2 30%, c3 15%, c4 5%, c5 3%, c6 1%
        int[] successRatios = {50, 30, 15, 5, 3, 1};
        if (level >= 1 && level <= 6) {
            return successRatios[level - 1];
        }
        return 100;
    }
    
    public static void showInfoCombine(Player player) {
        System.out.println("--- showInfoCombine called ---");
        System.out.println("itemsCombine size: " + player.combineNew.itemsCombine.size());
        if (player.combineNew.itemsCombine.size() == 2) {
            Item wing = null;
            Item material = null;
            boolean hasInvalidItem = false;
            
            for (Item item : player.combineNew.itemsCombine) {
                if (item == null) {
                    System.out.println("Warning: item in itemsCombine is null!");
                    continue;
                }
                System.out.println("Item template ID: " + item.template.id + ", Name: " + item.template.name);
                if ((item.template.id == 1110 || item.template.id == 1814 || item.template.id == 1148 || item.template.id == 1146 || item.template.id == 1147) && item.getOptionById(218) != null) {
                    wing = item;
                } else if (item.template.id == 2035) {
                    material = item;
                } else {
                    hasInvalidItem = true;
                }
            }
            
            System.out.println("wing is null: " + (wing == null) + ", material is null: " + (material == null) + ", hasInvalidItem: " + hasInvalidItem);
            
            if (hasInvalidItem) {
                System.out.println("hasInvalidItem is true, exiting");
                getNpc(player).createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Hộp kết hợp chứa vật phẩm không hợp lệ! Cần 1 Cánh Thiên Thần đệ tử và Tinh thể robot.", "Đóng");
                return;
            }
            
            if (wing != null && material != null) {
                int currentLevel = wing.getOptionParam(72);
                System.out.println("currentLevel of wing: " + currentLevel);
                
                if (currentLevel >= 7) {
                    System.out.println("currentLevel is >= 7, exiting");
                    getNpc(player).createOtherMenu(player, ConstNpc.IGNORE_MENU,
                            "Cánh Thiên Thần đã đạt cấp tối đa (cấp 7)!", "Đóng");
                    return;
                }
                
                int gold = getGoldCost(currentLevel);
                int gem = getGemCost(currentLevel);
                int ratio = getSuccessRatio(currentLevel);
                int requiredMaterial = getMaterialCost(currentLevel);
                
                player.combineNew.goldCombine = gold;
                player.combineNew.gemCombine = gem;
                player.combineNew.ratioCombine = ratio;
                
                String npcSay = "|2|Cánh Thiên Thần cấp " + currentLevel + " -> cấp " + (currentLevel + 1) + "\n\n";
                npcSay += "|2|Tỉ lệ thành công: " + ratio + "%\n";
                npcSay += "|2|Cần " + requiredMaterial + " Tinh thể robot (đang có " + material.quantity + ")\n";
                npcSay += "|2|Cần: " + Util.numberToMoney(gold) + " vàng\n";
                npcSay += "|2|Cần: " + gem + " ngọc\n";
                
                if (material.quantity < requiredMaterial) {
                    npcSay += "\nCòn thiếu " + (requiredMaterial - material.quantity) + " Tinh thể robot";
                    getNpc(player).createOtherMenu(player, ConstNpc.IGNORE_MENU, npcSay, "Đóng");
                } else if (player.inventory.gem < gem) {
                    npcSay += "\nCòn thiếu " + (gem - player.inventory.gem) + " ngọc";
                    getNpc(player).createOtherMenu(player, ConstNpc.IGNORE_MENU, npcSay, "Đóng");
                } else if (player.inventory.gold < gold) {
                    npcSay += "\nCòn thiếu " + Util.numberToMoney(gold - player.inventory.gold) + " vàng";
                    getNpc(player).createOtherMenu(player, ConstNpc.IGNORE_MENU, npcSay, "Đóng");
                } else {
                    String costText = "Nâng cấp\n" + Util.numberToMoney(gold) + " vàng\n" + gem + " ngọc";
                    getNpc(player).createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay, costText, "Từ chối");
                }
            } else {
                getNpc(player).createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Cần 1 Cánh Thiên Thần đệ tử và Tinh thể robot để nâng cấp.", "Đóng");
            }
        } else {
            getNpc(player).createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Cần 1 Cánh Thiên Thần đệ tử và Tinh thể robot để nâng cấp.", "Đóng");
        }
    }
    
    public static void nangCapCanh(Player player) {
        System.out.println("--- nangCapCanh called ---");
        System.out.println("itemsCombine size: " + player.combineNew.itemsCombine.size());
        if (player.combineNew.itemsCombine.size() == 2) {
            Item wing = null;
            Item material = null;
            boolean hasInvalidItem = false;
            
            for (Item item : player.combineNew.itemsCombine) {
                if (item == null) {
                    System.out.println("Warning: item in itemsCombine (nangCapCanh) is null!");
                    continue;
                }
                System.out.println("Item template ID (nangCapCanh): " + item.template.id + ", Name: " + item.template.name);
                if ((item.template.id == 1110 || item.template.id == 1814 || item.template.id == 1148 || item.template.id == 1146 || item.template.id == 1147) && item.getOptionById(218) != null) {
                    wing = item;
                } else if (item.template.id == 2035) {
                    material = item;
                } else {
                    hasInvalidItem = true;
                }
            }
            
            System.out.println("nangCapCanh - wing is null: " + (wing == null) + ", material is null: " + (material == null) + ", hasInvalidItem: " + hasInvalidItem);
            
            if (hasInvalidItem || wing == null || material == null) {
                System.out.println("Exiting nangCapCanh due to invalid items");
                return;
            }
            
            int currentLevel = wing.getOptionParam(72);
            System.out.println("nangCapCanh - currentLevel of wing: " + currentLevel);
            
            if (currentLevel >= 7) {
                return;
            }
            
            int gold = getGoldCost(currentLevel);
            int gem = getGemCost(currentLevel);
            int ratio = getSuccessRatio(currentLevel);
            int requiredMaterial = getMaterialCost(currentLevel);
            
            if (player.inventory.gold < gold) {
                Service.gI().sendThongBao(player, "Không đủ vàng để thực hiện");
                return;
            }
            if (player.inventory.gem < gem) {
                Service.gI().sendThongBao(player, "Không đủ ngọc để thực hiện");
                return;
            }
            if (material.quantity < requiredMaterial) {
                Service.gI().sendThongBao(player, "Không đủ Tinh thể robot");
                return;
            }
            
            player.inventory.gold -= gold;
            player.inventory.gem -= gem;
            
            InventoryService.gI().subQuantityItemsBag(player, material, requiredMaterial);
            
            if (Util.isTrue(ratio, 100)) {
                int nextLevel = currentLevel + 1;
                wing.addOptionParam(72, 1); // Cập nhật Option 72 lên cấp tiếp theo
                wing.addOptionParam(6, 3000); // HP +3k
                wing.addOptionParam(7, 3000); // KI +3k
                wing.addOptionParam(0, 500);  // SD +500
                if (nextLevel >= 6) {
                    wing.template = ItemService.gI().getTemplate(1814);
                }
                
                CombineService.gI().sendEffectSuccessCombine(player);
                Service.gI().sendThongBao(player, "Nâng cấp thành công Cánh Thiên Thần cấp " + nextLevel + "!");
            } else {
                CombineService.gI().sendEffectFailCombine(player);
                Service.gI().sendThongBao(player, "Nâng cấp thất bại! Nguyên liệu và tiền vàng đã bị tiêu hao.");
            }
            
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendMoney(player);
            CombineService.gI().reOpenItemCombine(player);
            player.combineNew.itemsCombine.clear();
        }
    }
}
