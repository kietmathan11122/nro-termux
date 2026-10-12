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
 * Nâng cấp Vòng Cổ Rồng (1 Sao -> 7 Sao)
 * Mỗi cấp tăng 1% cho cả 2 chỉ số ngẫu nhiên
 */
public class NangCapVongCo {

    private static Npc getNpc(Player player) {
        if (player.idMark.getNpcChose() != null) {
            return player.idMark.getNpcChose();
        }
        Npc zeno = nro.models.map.service.NpcManager.getNpc(ConstNpc.ZENO);
        if (zeno != null) {
            return zeno;
        }
        return CombineService.gI().baHatMit;
    }

    public static int getGoldCost(int star) {
        return star * 10_000_000;
    }

    public static int getGemCost(int star) {
        return star * 10;
    }

    public static int getSuccessRatio(int star) {
        return 80;
    }

    public static void showInfoCombine(Player player) {
        if (player.combineNew.itemsCombine.size() == 1) {
            Item vongCo = null;
            for (Item item : player.combineNew.itemsCombine) {
                if (item != null && ItemService.isVongCo(item.template.id)) {
                    vongCo = item;
                }
            }

            if (vongCo == null) {
                getNpc(player).createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Hộp kết hợp chứa vật phẩm không hợp lệ! Cần 1 Vòng Cổ Rồng.", "Đóng");
                return;
            }

            int star = vongCo.template.id - 2304;
            if (star >= 7) {
                getNpc(player).createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Vòng Cổ Rồng đã đạt cấp tối đa (7 Sao)!", "Đóng");
                return;
            }

            int gold = getGoldCost(star);
            int gem = getGemCost(star);
            int ratio = getSuccessRatio(star);

            player.combineNew.goldCombine = gold;
            player.combineNew.gemCombine = gem;
            player.combineNew.ratioCombine = ratio;

            String nextName = "Vòng Cổ Rồng " + (star + 1) + " Sao";
            String npcSay = "|2|Nâng cấp lên: " + nextName + "\n\n";
            npcSay += "|2|Tỉ lệ thành công: " + ratio + "%\n";
            npcSay += "|2|Cần: " + Util.numberToMoney(gold) + " vàng\n";
            if (gem > 0) {
                npcSay += "|2|Cần: " + gem + " ngọc\n";
            }
            npcSay += "|1|Khi nâng cấp thành công, cả 2 chỉ số hiện tại sẽ tăng thêm +1%!";

            if (player.inventory.gold < gold) {
                getNpc(player).createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        npcSay + "\n\n|7|Ngươi không đủ vàng để thực hiện!", "Đóng");
                return;
            }
            if (player.inventory.gem < gem) {
                getNpc(player).createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        npcSay + "\n\n|7|Ngươi không đủ ngọc để thực hiện!", "Đóng");
                return;
            }

            getNpc(player).createOtherMenu(player, ConstNpc.MENU_START_COMBINE,
                    npcSay, "Nâng cấp", "Từ chối");
        } else {
            getNpc(player).createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Cần đặt đúng 1 Vòng Cổ Rồng (cấp 1 đến 6 Sao) vào ô kết hợp.", "Đóng");
        }
    }

    public static void startCombine(Player player) {
        if (player.combineNew.itemsCombine.size() == 1) {
            Item vongCo = null;
            for (Item item : player.combineNew.itemsCombine) {
                if (item != null && ItemService.isVongCo(item.template.id)) {
                    vongCo = item;
                }
            }

            if (vongCo == null) {
                Service.gI().sendThongBao(player, "Vật phẩm không hợp lệ!");
                return;
            }

            int star = vongCo.template.id - 2304;
            if (star >= 7) {
                Service.gI().sendThongBao(player, "Vòng Cổ Rồng đã đạt cấp tối đa (7 Sao)!");
                return;
            }

            int gold = getGoldCost(star);
            int gem = getGemCost(star);
            int ratio = getSuccessRatio(star);

            if (player.inventory.gold < gold) {
                Service.gI().sendThongBao(player, "Không đủ vàng để nâng cấp!");
                return;
            }
            if (player.inventory.gem < gem) {
                Service.gI().sendThongBao(player, "Không đủ ngọc để nâng cấp!");
                return;
            }

            player.inventory.gold -= gold;
            player.inventory.gem -= gem;
            Service.gI().sendMoney(player);

            if (Util.isTrue(ratio, 100)) {
                ItemService.gI().upgradeVongCo(vongCo);
                CombineService.gI().sendEffectSuccessCombine(player);
                Service.gI().sendThongBao(player, "Nâng cấp thành công " + vongCo.template.name + " (+1% cả 2 chỉ số)!");
            } else {
                CombineService.gI().sendEffectFailCombine(player);
                Service.gI().sendThongBao(player, "Nâng cấp thất bại! Vật phẩm không bị mất hay giảm cấp.");
            }

            InventoryService.gI().sendItemBags(player);
            Service.gI().point(player);
            getNpc(player).createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Nâng cấp hoàn tất!", "Đóng");
        }
    }
}
