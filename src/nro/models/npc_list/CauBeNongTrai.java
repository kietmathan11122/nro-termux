package nro.models.npc_list;

import nro.models.consts.ConstNpc;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.shop.ShopService;
import nro.models.item.Item;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.services.Service;

public class CauBeNongTrai extends Npc {

    public CauBeNongTrai(int mapId, int status, int cx, int cy, int tempId, int avatar) {
        super(mapId, status, cx, cy, tempId, avatar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (player.zone != null && player.zone.map.mapId >= 177 && player.zone.map.mapId <= 179) {
                createOtherMenu(player, ConstNpc.BASE_MENU,
                        "Ta là Trưởng làng. Hãy mua thức ăn rồi chạm vào vật nuôi để cho ăn hoặc thu thập.",
                        "Mua thức ăn", "Hướng dẫn", "Đóng");
                return;
            }
            createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Tớ có hạt giống và đồ giúp cây lớn nhanh. Cậu cần gì?",
                    "Cửa hàng\nnông trại", "Đóng");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player) && player.idMark.isBaseMenu() && player.zone != null
                && player.zone.map.mapId >= 177 && player.zone.map.mapId <= 179) {
            if (select == 0) {
                if (player.inventory.gold < 100000) {
                    Service.gI().sendThongBao(player, "Cần 100.000 vàng để mua 10 thức ăn");
                } else {
                    Item food = ItemService.gI().createNewItem((short)2250, 10);
                    if (!InventoryService.gI().addItemBag(player, food)) {
                        Service.gI().sendThongBao(player, "Hành trang đã đầy");
                    } else {
                        player.inventory.gold -= 100000;
                        InventoryService.gI().sendItemBags(player);
                        Service.gI().sendMoney(player);
                        Service.gI().sendThongBao(player, "Đã mua 10 Thức ăn chăn nuôi");
                    }
                }
            }
            else if (select == 1) nro.models.services.Service.gI().sendThongBao(player,
                    "Gà 5 phút: Trứng | Bò 6 phút: Sữa | Chó 10 phút: Chó con | Cừu 8 phút: Bộ lông. Mỗi lần nhận 1 vật phẩm.");
            return;
        }
        if (canOpenNpc(player) && player.idMark.isBaseMenu() && select == 0) {
            ShopService.gI().opendShop(player, "FARM_BOY", true);
        }
    }
}
