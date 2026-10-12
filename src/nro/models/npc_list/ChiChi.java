package nro.models.npc_list;

import nro.models.consts.ConstNpc;
import nro.models.item.Item;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.services.InventoryService;
import nro.models.services.Service;
import nro.models.shop.ShopService;

public class ChiChi extends Npc {

    public ChiChi(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (nro.models.event.EventManager.SUMMER) {
                createOtherMenu(
                        player,
                        ConstNpc.BASE_MENU,
                        "SỰ KIỆN MÙA HÈ\nĐánh quái toàn bản đồ có 5% rơi Kem Trái Cây.\nDùng 20 Kem Trái Cây để nhận ngẫu nhiên vàng, ngọc hoặc cải trang Mùa Hè.",
                        "Thông tin\nsự kiện Hè",
                        "Xem điểm\nKem Trái Cây",
                        "SHOP\nVIP",
                        "Cửa hàng\nĐá",
                        "Đóng"
                );
                return;
            }
            createOtherMenu(
                    player,
                    ConstNpc.BASE_MENU,
                    "Chi sẽ giúp bạn loại bỏ những trang bị có HSD ở hành trang\n(trang bị đang mặc không bị ảnh hưởng)\nHãy đọc kỹ rồi chọn nhé",
                    "Hủy bỏ\ntrang bị\ncó hsd",
                    "Thông tin\nsự kiện mới",
                    "SHOP\nVIP",
                        "Cửa hàng\nĐá",
                    "Đóng"
            );
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) {
            return;
        }

        if (player.idMark.isBaseMenu()) {
            if (nro.models.event.EventManager.SUMMER) {
                switch (select) {
                    case 0 -> Service.gI().sendThongBaoOK(player,
                            "Đánh quái có 5% rơi Kem Trái Cây. Gom đủ 20 rồi sử dụng Kem trong hành trang để mở quà hè. Tỉ lệ: 55% vàng, 40% ngọc, 5% cải trang.");
                    case 1 -> Service.gI().sendThongBao(player,
                            "Bạn đã mở " + player.point_sukien2 + " phần quà Mùa Hè.");
                    case 2 -> ShopService.gI().opendShop(player, "SHOP_VIP", false);
                    case 3 -> ShopService.gI().opendShop(player, "CHI_CHI_STONES", false);
                }
                return;
            }
            switch (select) {
                case 0:
                    vutTrangBiHSD(player);
                    break;

                case 1:
                    Service.gI().sendThongBaoOK(player,
                            "Sự kiện mới sẽ update sau 7 ngày Open Server.");
                    break;

                case 2:
                    ShopService.gI().opendShop(player, "SHOP_VIP", false);
                    break;

                case 3:
                    ShopService.gI().opendShop(player,"CHI_CHI_STONES",false);
                    break;
                case 4:
                    // Đóng
                    break;
            }
        }
    }

    /**
     * Vứt (xóa hẳn) toàn bộ trang bị trong hành trang có option id = 93
     */
    private void vutTrangBiHSD(Player player) {
        boolean daXoa = false;

        for (int i = 0; i < player.inventory.itemsBag.size(); i++) {
            Item item = player.inventory.itemsBag.get(i);

            if (item == null || !item.isNotNullItem()) {
                continue;
            }

            for (int j = 0; j < item.itemOptions.size(); j++) {
                if (item.itemOptions.get(j).optionTemplate.id == 93) {

                    // Tạo ô trống
                    Item empty = new Item();
                    empty.template = null;

                    player.inventory.itemsBag.set(i, empty);
                    daXoa = true;
                    break;
                }
            }
        }

        if (!daXoa) {
            Service.gI().sendThongBao(player,
                    "Hành trang không có trang bị nào có hạn sử dụng.");
            return;
        }

        InventoryService.gI().sendItemBags(player);
        Service.gI().sendThongBao(player,
                "Đã vứt bỏ toàn bộ trang bị có hạn sử dụng.");
    }
}
