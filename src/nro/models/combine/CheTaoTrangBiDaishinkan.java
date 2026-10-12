package nro.models.combine;

import nro.models.consts.ConstNpc;
import nro.models.item.Item;
import nro.models.item.Item.ItemOption;
import java.util.ArrayList;
import java.util.Arrays;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.services.ItemService;
import nro.models.services.Service;
import nro.models.services.InventoryService;
import nro.models.utils.Util;

public class CheTaoTrangBiDaishinkan {

    private static Npc getNpc(Player player) {
        if (player.idMark.getNpcChose() != null) {
            return player.idMark.getNpcChose();
        }
        return CombineService.gI().baHatMit;
    }

    private static boolean hasMonThienSu(Player player) {
        if (player == null || player.setClothes == null) {
            return false;
        }
        return player.setClothes.checkSetThienSu();
    }

    public static void showInfoCombine(Player player) {
        if (!hasMonThienSu(player)) {
            getNpc(player).createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Ngươi cần phải mặc ít nhất 1 món trang bị Thiên Sứ (hoặc Full Set) trên người mới có thể chế tạo Daishinkan!", "Đóng");
            return;
        }

        if (player.combineNew.itemsCombine.size() < 2 || player.combineNew.itemsCombine.size() > 4) {
            getNpc(player).createOtherMenu(player, ConstNpc.IGNORE_MENU, "Thiếu vật phẩm, vui lòng thêm vào", "Đóng");
            return;
        }

        Item mDS = null, daNC = null, daMM = null, CtVip = null;
        for (Item item : player.combineNew.itemsCombine) {
            if (item.isNotNullItem()) {
                if (item.isManhDaishinkan()) {
                    mDS = item;
                } else if (item.isDaNangCapDaishinkan()) {
                    daNC = item;
                } else if (item.isDaMayManDaishinkan()) {
                    daMM = item;
                } else if (item.isCongThucVipDaishinkan()) {
                    CtVip = item;
                }
            }
        }

        if (CtVip == null) {
            getNpc(player).createOtherMenu(player, ConstNpc.IGNORE_MENU, "Thiếu Công Thức Vip Daishinkan", "Đóng");
            return;
        } else if (mDS == null || mDS.quantity < 999) {
            getNpc(player).createOtherMenu(player, ConstNpc.IGNORE_MENU, "Thiếu Mảnh Daishinkan (cần 999 cái)", "Đóng");
            return;
        }

        int expectedCount = 2;
        if (daNC != null) expectedCount++;
        if (daMM != null) expectedCount++;
        if (player.combineNew.itemsCombine.size() != expectedCount) {
            getNpc(player).createOtherMenu(player, ConstNpc.IGNORE_MENU, "Vật phẩm thêm vào không hợp lệ", "Đóng");
            return;
        }

        int tilemacdinh = 20; // Tỷ lệ thành công cơ bản = 20%
        int tileLucky = 0;   // Tỷ lệ ra option cơ bản

        if (daNC != null) {
            tilemacdinh += (daNC.template.id - 1908) * 10; // Cấp 1 +10% (30%), Cấp 2 +20% (40%), Cấp 3 +30% (50%), Cấp 4 +40% (60%), Cấp 5 +50% (70%)
            if (tilemacdinh > 100) tilemacdinh = 100;
        }
        if (daMM != null) {
            tileLucky = 10 + (daMM.template.id - 1913) * 10;
        }

        String npcSay = "|2|Chế tạo Trang bị Daishinkan\n\n";
        npcSay += "|2|Tỉ lệ thành công: " + tilemacdinh + "%\n";
        if (tileLucky > 0) {
            npcSay += "|2|Tỉ lệ nhận thêm Option: " + tileLucky + "%\n";
        } else {
            npcSay += "|7|(Có thể thêm Túi may mắn để tăng tỉ lệ nhận thêm Option)\n";
        }
        if (daNC == null) {
            npcSay += "|7|(Có thể thêm Đá nâng cấp để tăng tỉ lệ thành công)\n";
        }
        npcSay += "|2|Chi phí: 200.000.000 vàng\n";

        if (player.inventory.gold < 200000000L || player.inventory.gem < 100) {
            npcSay += "\n|7|Không đủ vàng (cần 200 triệu vàng)";
            getNpc(player).createOtherMenu(player, ConstNpc.IGNORE_MENU, npcSay, "Đóng");
        } else if (InventoryService.gI().getCountEmptyBag(player) < 1) {
            npcSay += "\n|7|Hành trang không đủ 1 ô trống";
            getNpc(player).createOtherMenu(player, ConstNpc.IGNORE_MENU, npcSay, "Đóng");
        } else {
            getNpc(player).createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay,
                    "Nâng cấp\n200Tr Vàng", "Từ chối");
        }
    }

    public static void CheTaoDS(Player player) {
        if (!hasMonThienSu(player)) {
            Service.gI().sendThongBao(player, "Ngươi cần phải mặc ít nhất 1 món trang bị Thiên Sứ (hoặc Full Set) mới có thể chế tạo Daishinkan!");
            return;
        }

        if (player.combineNew.itemsCombine.size() < 2 || player.combineNew.itemsCombine.size() > 4) {
            return;
        }

        Item mDS = null, daNC = null, daMM = null, CtVip = null;
        for (Item item : player.combineNew.itemsCombine) {
            if (item.isNotNullItem()) {
                if (item.isManhDaishinkan()) {
                    mDS = item;
                } else if (item.isDaNangCapDaishinkan()) {
                    daNC = item;
                } else if (item.isDaMayManDaishinkan()) {
                    daMM = item;
                } else if (item.isCongThucVipDaishinkan()) {
                    CtVip = item;
                }
            }
        }

        if (CtVip == null || mDS == null || mDS.quantity < 999) {
            return;
        }

        if (player.inventory.gold < 200000000L || player.inventory.gem < 100) {
            Service.gI().sendThongBao(player, "Không đủ vàng để thực hiện (cần 200 triệu vàng)");
            return;
        }
        if (InventoryService.gI().getCountEmptyBag(player) < 1) {
            Service.gI().sendThongBao(player, "Bạn phải có ít nhất 1 ô trống hành trang");
            return;
        }

        player.inventory.gold -= 200000000L;
        player.inventory.gem -= 100;

        int tilemacdinh = 20;
        int tileLucky = 0;

        if (daNC != null) {
            tilemacdinh += (daNC.template.id - 1908) * 10;
            if (tilemacdinh > 100) tilemacdinh = 100;
        }
        if (daMM != null) {
            tileLucky = 10 + (daMM.template.id - 1913) * 10;
        }

        if (Util.nextInt(0, 100) < tilemacdinh) {
            Item itemCtVip = CtVip;
            Item itemManh = mDS;

            short[][] itemIds = {
                {1889, 1890, 1891, 1892, 1893},
                {1889, 1890, 1891, 1892, 1893},
                {1889, 1890, 1891, 1892, 1893}
            };

            int genderToUse = itemCtVip.template.gender > 2 ? player.gender : itemCtVip.template.gender;
            Item itemDS = ItemService.gI().DoDaishinkan(itemIds[genderToUse][itemManh.typeIdManhDaishinkan()], itemCtVip.template.gender);

            int rolledOption = Util.nextInt(0, 100);
            if (tileLucky > 0 && rolledOption <= tileLucky) {
                int optCount;
                if (rolledOption >= (tileLucky - 10)) {
                    optCount = 3;
                } else if (rolledOption >= (tileLucky - 25)) {
                    optCount = 2;
                } else {
                    optCount = 1;
                }
                itemDS.itemOptions.add(new ItemOption(15, optCount));
                ArrayList<Integer> listOptionBonus = new ArrayList<>(Arrays.asList(50, 77, 103, 94, 5));
                for (int j = 0; j < optCount; j++) {
                    int indexOpt = Util.nextInt(0, listOptionBonus.size() - 1);
                    itemDS.itemOptions.add(new ItemOption(listOptionBonus.get(indexOpt), Util.nextInt(1, 3)));
                    listOptionBonus.remove(indexOpt);
                }
            }

            InventoryService.gI().addItemBag(player, itemDS);
            CombineService.gI().sendEffectSuccessCombine(player);
            Service.gI().sendThongBao(player, "Chúc mừng bạn chế tạo thành công " + itemDS.template.name);
        } else {
            CombineService.gI().sendEffectFailCombine(player);
            Service.gI().sendThongBao(player, "Chế tạo thất bại!");
        }

        if (CtVip != null) {
            InventoryService.gI().subQuantityItemsBag(player, CtVip, 1);
        }
        if (mDS != null) {
            InventoryService.gI().subQuantityItemsBag(player, mDS, 999);
        }
        if (daNC != null) {
            InventoryService.gI().subQuantityItemsBag(player, daNC, 1);
        }
        if (daMM != null) {
            InventoryService.gI().subQuantityItemsBag(player, daMM, 1);
        }

        InventoryService.gI().sendItemBags(player);
        Service.gI().sendMoney(player);
        player.combineNew.itemsCombine.clear();
        CombineService.gI().reOpenItemCombine(player);
    }
}
