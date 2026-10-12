package nro.models.npc_list;

import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.services.Service;
import nro.models.services.InventoryService;
import nro.models.map.service.NpcService;
import nro.models.services_func.Input;
import nro.models.player.Inventory;
import nro.models.item.Item;
import nro.models.consts.ConstNpc;

public class RuongSuuTam extends Npc {

    public RuongSuuTam(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            this.createOtherMenu(player, ConstNpc.BASE_MENU, "Ngươi đang có " + player.inventory.sizeRuongSuuTam + " ô Rương Sưu Tầm.\nMuốn mở rộng thêm 1 ô cần 100 Ngọc.",
                    "Mở rương\nSưu Tầm", "Mở rộng rương\n(100 Ngọc/1 ô)", "Đóng");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (player.idMark.isBaseMenu()) {
                switch (select) {
                    case 0:
                        player.isOpeningRuongSuuTam = true;
                        InventoryService.gI().sendItemBags(player);
                        InventoryService.gI().sendItemRuongSuuTam(player);
                        break;
                    case 1:
                        if (player.inventory.gem >= 100) {
                            player.inventory.gem -= 100;
                            player.inventory.sizeRuongSuuTam += 1;
                            nro.models.services.Service.gI().sendMoney(player);
                            nro.models.services.Service.gI().sendThongBao(player, "Mở rộng rương thành công, hiện tại bạn có " + player.inventory.sizeRuongSuuTam + " ô.");
                        } else {
                            nro.models.services.Service.gI().sendThongBao(player, "Không đủ 100 Ngọc!");
                        }
                        break;
                }
            }
        }
    }
}
