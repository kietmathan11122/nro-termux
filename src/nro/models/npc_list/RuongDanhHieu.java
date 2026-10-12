package nro.models.npc_list;

import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.services.Service;
import nro.models.services.InventoryService;
import nro.models.consts.ConstNpc;

public class RuongDanhHieu extends Npc {

    public RuongDanhHieu(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            this.createOtherMenu(player, ConstNpc.BASE_MENU, 
                    "Ngươi đang có " + player.inventory.sizeRuongDanhHieu + " ô Rương Danh Hiệu.\n"
                    + "Mỗi Danh hiệu bỏ vào rương sẽ giúp ngươi tăng 500 Sức đánh!\n"
                    + "Muốn mở rộng thêm 1 ô cần 100 Ngọc.",
                    "Mở rương\nDanh hiệu", "Mở rộng rương\n(100 Ngọc/1 ô)", "Đóng");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (player.idMark.isBaseMenu()) {
                switch (select) {
                    case 0:
                        player.isOpeningRuongDanhHieu = true;
                        InventoryService.gI().sendItemBags(player);
                        InventoryService.gI().sendItemRuongDanhHieu(player);
                        break;
                    case 1:
                        if (player.inventory.gem >= 100) {
                            player.inventory.gem -= 100;
                            player.inventory.sizeRuongDanhHieu += 1;
                            Service.gI().sendMoney(player);
                            Service.gI().sendThongBao(player, "Mở rộng rương thành công, hiện tại bạn có " + player.inventory.sizeRuongDanhHieu + " ô.");
                        } else {
                            Service.gI().sendThongBao(player, "Không đủ 100 Ngọc!");
                        }
                        break;
                }
            }
        }
    }
}
