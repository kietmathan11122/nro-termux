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

public class NangCapPetBa {

    private static Npc getNpc(Player player) {
        if (player.idMark.getNpcChose() != null) {
            return player.idMark.getNpcChose();
        }
        return CombineService.gI().baHatMit;
    }

    public static int getMaterialCost(int level) {
        return 1000;
    }

    public static void showInfoCombine(Player player) {
        int size = player.combineNew.itemsCombine.size();
        if (size == 3 || size == 4) {
            Item ba = null;
            Item material = null;
            Item ngocNangCap = null;
            Item cuonSachCo = null;
            boolean hasInvalidItem = false;

            for (Item item : player.combineNew.itemsCombine) {
                if (item.template.id == 2046) {
                    ba = item;
                } else if (item.template.id == 2047) {
                    material = item;
                } else if (item.template.id == 2061) {
                    ngocNangCap = item;
                } else if (item.template.id == 2058) {
                    cuonSachCo = item;
                } else {
                    hasInvalidItem = true;
                }
            }

            if (hasInvalidItem) {
                getNpc(player).createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Hộp kết hợp chứa vật phẩm không hợp lệ!", "Đóng");
                return;
            }

            if (ba != null && material != null && ngocNangCap != null) {
                int currentLevel = ba.getOptionParam(72);

                if (currentLevel >= 7) {
                    getNpc(player).createOtherMenu(player, ConstNpc.IGNORE_MENU,
                            "Pet BA đã đạt cấp tối đa (cấp 7)!", "Đóng");
                    return;
                }

                int requiredMaterial = 1000;
                int requiredNgoc = 1;
                int ratio = 100;
                switch (currentLevel) {
                    case 0 -> ratio = 30;
                    case 1 -> ratio = 15;
                    case 2 -> ratio = 10;
                    case 3 -> ratio = 5;
                    case 4 -> ratio = 3;
                    case 5 -> ratio = 2;
                    case 6 -> ratio = 1;
                }

                if (cuonSachCo != null) {
                    ratio += 3;
                }

                player.combineNew.goldCombine = 0;
                player.combineNew.gemCombine = 0;
                player.combineNew.ratioCombine = ratio;

                String npcSay = "|2|Pet BA cấp " + (currentLevel + 1) + "\n\n";
                npcSay += "|2|Tỉ lệ thành công: " + ratio + "%\n";
                npcSay += "|2|Cần " + requiredMaterial + " Bọ Ma (đang có " + material.quantity + ")\n";
                npcSay += "|2|Cần " + requiredNgoc + " Ngọc Nâng Cấp Ba (đang có " + ngocNangCap.quantity + ")\n";
                if (cuonSachCo != null) {
                    npcSay += "|2|Có Cuốn sách cổ (+3% tỉ lệ, bảo vệ Bọ Ma khi thất bại)\n";
                } else {
                    npcSay += "|7|Thất bại sẽ mất 100 Bọ Ma (Thêm Cuốn sách cổ để bảo vệ và +3% tỉ lệ)\n";
                }

                if (material.quantity < requiredMaterial) {
                    npcSay += "\nCòn thiếu " + (requiredMaterial - material.quantity) + " Bọ Ma";
                    getNpc(player).createOtherMenu(player, ConstNpc.IGNORE_MENU, npcSay, "Đóng");
                } else if (ngocNangCap.quantity < requiredNgoc) {
                    npcSay += "\nCòn thiếu " + (requiredNgoc - ngocNangCap.quantity) + " Ngọc Nâng Cấp Ba";
                    getNpc(player).createOtherMenu(player, ConstNpc.IGNORE_MENU, npcSay, "Đóng");
                } else {
                    String costText = "Nâng cấp";
                    getNpc(player).createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay,
                            costText, "Từ chối");
                }
            } else {
                getNpc(player).createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Cần 1 Pet BA, Bọ Ma và 1 Ngọc Nâng Cấp Ba.", "Đóng");
            }
        } else {
            getNpc(player).createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Cần 1 Pet BA, Bọ Ma và 1 Ngọc Nâng Cấp Ba (Có thể thêm Cuốn sách cổ).", "Đóng");
        }
    }

    public static void nangCapPetBa(Player player) {
        int size = player.combineNew.itemsCombine.size();
        if (size == 3 || size == 4) {
            Item ba = null;
            Item material = null;
            Item ngocNangCap = null;
            Item cuonSachCo = null;
            boolean hasInvalidItem = false;

            for (Item item : player.combineNew.itemsCombine) {
                if (item.template.id == 2046) {
                    ba = item;
                } else if (item.template.id == 2047) {
                    material = item;
                } else if (item.template.id == 2061) {
                    ngocNangCap = item;
                } else if (item.template.id == 2058) {
                    cuonSachCo = item;
                } else {
                    hasInvalidItem = true;
                }
            }

            if (hasInvalidItem || ba == null || material == null || ngocNangCap == null) {
                return;
            }

            int currentLevel = ba.getOptionParam(72);
            if (currentLevel >= 7) {
                return;
            }

            int requiredMaterial = 1000;
            int requiredNgoc = 1;

            if (material.quantity < requiredMaterial) {
                Service.gI().sendThongBao(player, "Không đủ Bọ Ma");
                return;
            }
            if (ngocNangCap.quantity < requiredNgoc) {
                Service.gI().sendThongBao(player, "Không đủ Ngọc Nâng Cấp Ba");
                return;
            }

            int ratio = 100;
            switch (currentLevel) {
                case 0 -> ratio = 30;
                case 1 -> ratio = 15;
                case 2 -> ratio = 10;
                case 3 -> ratio = 5;
                case 4 -> ratio = 3;
                case 5 -> ratio = 2;
                case 6 -> ratio = 1;
            }

            if (cuonSachCo != null) {
                ratio += 3;
            }

            // Tiêu hao Ngọc Nâng Cấp Ba (ID 2061) x1
            InventoryService.gI().subQuantityItemsBag(player, ngocNangCap, requiredNgoc);
            
            // Tiêu hao Cuốn sách cổ (ID 2058) x1 nếu có bỏ vào
            if (cuonSachCo != null) {
                InventoryService.gI().subQuantityItemsBag(player, cuonSachCo, 1);
            }

            if (Util.isTrue(ratio, 100)) {
                // Thành công: mất 1000 Bọ Ma
                InventoryService.gI().subQuantityItemsBag(player, material, requiredMaterial);

                int nextLevel = currentLevel + 1;
                int hpKiPercent = nextLevel * 3;
                int sdPercent = nextLevel * 2;
                int crit = nextLevel * 2;

                ba.itemOptions.clear();
                ba.itemOptions.add(new ItemOption(72, nextLevel)); // Cấp
                ba.itemOptions.add(new ItemOption(30, 1)); // Không thể giao dịch
                ba.itemOptions.add(new ItemOption(77, hpKiPercent)); // HP +%
                ba.itemOptions.add(new ItemOption(103, hpKiPercent)); // KI +%
                ba.itemOptions.add(new ItemOption(50, sdPercent)); // Sức đánh +%
                ba.itemOptions.add(new ItemOption(14, crit)); // Chí mạng +%

                CombineService.gI().sendEffectSuccessCombine(player);
                Service.gI().sendThongBao(player, "Nâng cấp thành công Pet BA cấp " + nextLevel + "!");
                
                player.combineNew.itemsCombine.clear();
            } else {
                // Thất bại
                if (cuonSachCo != null) {
                    // Có cuốn sách cổ -> không mất bọ ma!
                    CombineService.gI().sendEffectFailCombine(player);
                    Service.gI().sendThongBao(player, "Nâng cấp thất bại! Nhờ có Cuốn sách cổ bảo vệ, bạn không bị mất Bọ Ma.");
                } else {
                    // Không có cuốn sách cổ -> mất 100 bọ ma
                    InventoryService.gI().subQuantityItemsBag(player, material, 100);
                    CombineService.gI().sendEffectFailCombine(player);
                    Service.gI().sendThongBao(player, "Nâng cấp thất bại, bạn mất 100 Bọ Ma!");
                }
            }

            InventoryService.gI().sendItemBags(player);
            Service.gI().sendMoney(player);
            CombineService.gI().reOpenItemCombine(player);
        }
    }
}
