package nro.models.combine;

import nro.models.consts.ConstNpc;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.services.InventoryService;
import nro.models.services.Service;
import nro.models.utils.Util;

/**
 *
 * @author MinhDu
 */
public class NangChiSoBongTai4 {
    // Chi phí & tỉ lệ
    private static final int GOLD_NANG_BT = 200_000_000;
    private static final int GEM_NANG_BT = 500;
    private static final int RATIO_NANG_CAP = 30;
    private static final int ITEM_PARAM_INDEX = 31;
    private static final int BONG_TAI_C4_ID = 2000;
    private static final int HON_BONG_TAI_ID = 934;
    private static final int DA_XANH_LAM_ID = 935;
    private static final int REQUIRED_HON_BONG_TAI = 99;
    private static final byte[] UPGRADE_OPTIONS = {77, 101, 103, 50, 94, 5};
    private static final byte PARAM_MIN = 5;
    private static final byte PARAM_MAX = 20;

    public static void showInfoCombine(Player player) {
        if (player.combineNew.itemsCombine.size() == 3) {
            Item bongTai = null, honBongTai = null, daXanhLam = null;

            for (Item item : player.combineNew.itemsCombine) {
                if (item != null && item.isNotNullItem()) {
                    int id = item.template.id;
                    if (id == BONG_TAI_C4_ID) bongTai = item;
                    else if (id == HON_BONG_TAI_ID) honBongTai = item;
                    else if (id == DA_XANH_LAM_ID) daXanhLam = item;
                }
            }

            if (bongTai != null && honBongTai != null && daXanhLam != null) {
                player.combineNew.goldCombine = GOLD_NANG_BT;
                player.combineNew.gemCombine = GEM_NANG_BT;
                player.combineNew.ratioCombine = RATIO_NANG_CAP;

                int currentHon = honBongTai.quantity;

                String npcSay = "|2|Mở chỉ số Bông tai Porata [+4]\n\n";
                npcSay += "|2|Tỉ lệ thành công: " + player.combineNew.ratioCombine + "%\n";

                if (daXanhLam.quantity < 1) {
                    npcSay += "|2|Cần " + REQUIRED_HON_BONG_TAI + " " + honBongTai.template.name + "\n";
                    npcSay += "|7|Cần 1 " + daXanhLam.template.name + "\n";
                    npcSay += "|2|Cần: " + player.combineNew.gemCombine + " ngọc\n";
                    npcSay += "|2|Cần: " + Util.numberToMoney(player.combineNew.goldCombine) + " vàng\n";
                    npcSay += "|1|Kết quả: +3 dòng chỉ số ngẫu nhiên (có thể trùng nhau)\n";
                    npcSay += "|2|Còn thiếu " + (1 - daXanhLam.quantity) + " " + daXanhLam.template.name;
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, npcSay, "Đóng");
                } else if (currentHon < REQUIRED_HON_BONG_TAI) {
                    npcSay += "|7|Cần " + REQUIRED_HON_BONG_TAI + " " + honBongTai.template.name + "\n";
                    npcSay += "|2|Cần 1 " + daXanhLam.template.name + "\n";
                    npcSay += "|2|Cần: " + player.combineNew.gemCombine + " ngọc\n";
                    npcSay += "|2|Cần: " + Util.numberToMoney(player.combineNew.goldCombine) + " vàng\n";
                    npcSay += "|1|Kết quả: +3 dòng chỉ số ngẫu nhiên (có thể trùng nhau)\n";
                    npcSay += "|2|Còn thiếu " + (REQUIRED_HON_BONG_TAI - currentHon) + " " + honBongTai.template.name;
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, npcSay, "Đóng");
                } else if (player.inventory.gem < player.combineNew.gemCombine || player.inventory.gold < player.combineNew.goldCombine) {
                    npcSay += "|2|Cần " + REQUIRED_HON_BONG_TAI + " " + honBongTai.template.name + "\n";
                    npcSay += "|2|Cần 1 " + daXanhLam.template.name + "\n";
                    npcSay += (player.inventory.gem < player.combineNew.gemCombine ? "|7|" : "|2|") + "Cần: " + player.combineNew.gemCombine + " ngọc\n";
                    npcSay += (player.inventory.gold < player.combineNew.goldCombine ? "|7|" : "|2|") + "Cần: " + Util.numberToMoney(player.combineNew.goldCombine) + " vàng\n";
                    npcSay += "|1|Kết quả: +3 dòng chỉ số ngẫu nhiên (có thể trùng nhau)\n";
                    npcSay += "|2|Còn thiếu " + (player.inventory.gem < player.combineNew.gemCombine ? (player.combineNew.gemCombine - player.inventory.gem) + " ngọc" : Util.powerToString(player.combineNew.goldCombine - player.inventory.gold) + " vàng");
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, npcSay, "Đóng");
                } else {
                    npcSay += "|2|Cần " + REQUIRED_HON_BONG_TAI + " " + honBongTai.template.name + "\n";
                    npcSay += "|2|Cần 1 " + daXanhLam.template.name + "\n";
                    npcSay += "|2|Cần: " + player.combineNew.gemCombine + " ngọc\n";
                    npcSay += "|2|Cần: " + Util.numberToMoney(player.combineNew.goldCombine) + " vàng\n";
                    npcSay += "|1|Kết quả: +3 dòng chỉ số ngẫu nhiên (có thể trùng nhau)";
                    CombineService.gI().baHatMit.createOtherMenu(
                            player,
                            ConstNpc.MENU_START_COMBINE,
                            npcSay,
                            "Nâng cấp\n" + player.combineNew.gemCombine + " ngọc\n" + Util.numberToMoney(player.combineNew.goldCombine) + " vàng",
                            "Từ chối"
                    );
                }
            } else {
                CombineService.gI().baHatMit.createOtherMenu(
                        player,
                        ConstNpc.IGNORE_MENU,
                        "Cần 1 Bông tai Porata cấp 4, x" + REQUIRED_HON_BONG_TAI + " Hồn bông tai và 1 Đá xanh lam",
                        "Đóng"
                );
            }
        } else {
            CombineService.gI().baHatMit.createOtherMenu(
                    player,
                    ConstNpc.IGNORE_MENU,
                    "Cần 1 Bông tai Porata cấp 4, x" + REQUIRED_HON_BONG_TAI + " Hồn bông tai và 1 Đá xanh lam",
                    "Đóng"
            );
        }
    }
    public static void nangChiSoBongTai(Player player) {
        try {
            Item honBongTai = InventoryService.gI().findItemBag(player, HON_BONG_TAI_ID);
            int currentHon = honBongTai != null ? honBongTai.quantity : 0;
            Item daXanhLam = InventoryService.gI().findItemBag(player, DA_XANH_LAM_ID);

            if (currentHon < REQUIRED_HON_BONG_TAI || daXanhLam == null || daXanhLam.quantity < 1) {
                Service.gI().sendThongBao(player, "Không đủ vật phẩm để thực hiện.");
                return;
            }
            if (player.inventory.gold < player.combineNew.goldCombine) {
                Service.gI().sendThongBao(player, "Bạn không đủ vàng, còn thiếu " + Util.powerToString(player.combineNew.goldCombine - player.inventory.gold) + " vàng nữa!");
                return;
            }
            if (player.inventory.gem < player.combineNew.gemCombine) {
                Service.gI().sendThongBao(player, "Bạn không đủ ngọc, còn thiếu " + (player.combineNew.gemCombine - player.inventory.gem) + " ngọc nữa!");
                return;
            }
            player.inventory.gold -= player.combineNew.goldCombine;
            player.inventory.gem -= player.combineNew.gemCombine;
            Item bongTai = player.combineNew.itemsCombine.stream()
                    .filter(it -> it != null && it.isNotNullItem() && it.template != null && it.template.id == BONG_TAI_C4_ID)
                    .findFirst()
                    .orElse(null);

            if (bongTai == null) {
                Service.gI().sendThongBao(player, "Thiếu Bông tai Porata cấp 4.");
                return;
            }

            boolean success = Util.isTrue(player.combineNew.ratioCombine, 100);
            if (success) {
                byte opt1 = randomOpt();
                byte opt2 = randomOpt();
                byte opt3 = randomOpt();
                byte p1 = (byte) Util.nextInt(PARAM_MIN, PARAM_MAX);
                byte p2 = (byte) Util.nextInt(PARAM_MIN, PARAM_MAX);
                byte p3 = (byte) Util.nextInt(PARAM_MIN, PARAM_MAX);
                bongTai.itemOptions.clear();
                bongTai.itemOptions.add(new Item.ItemOption(opt1, p1));
                bongTai.itemOptions.add(new Item.ItemOption(opt2, p2));
                bongTai.itemOptions.add(new Item.ItemOption(opt3, p3));
                bongTai.itemOptions.add(new Item.ItemOption((short) 72, 4));

                CombineService.gI().sendEffectSuccessCombine(player);
            } else {
                CombineService.gI().sendEffectFailCombine(player);
            }
            InventoryService.gI().subQuantityItemsBag(player, honBongTai, REQUIRED_HON_BONG_TAI);
            InventoryService.gI().subQuantityItemsBag(player, daXanhLam, 1);

            Service.gI().sendMoney(player);
            InventoryService.gI().sendItemBags(player);
            CombineService.gI().reOpenItemCombine(player);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static byte randomOpt() {
        return UPGRADE_OPTIONS[Util.nextInt(0, UPGRADE_OPTIONS.length - 1)];
    }
}
