package nro.models.npc_list;

import nro.models.consts.ConstNpc;
import nro.models.map.service.ChangeMapService;
import nro.models.npc.Npc;
import nro.models.player.Player;

/**
 *
 * @author By Mr Blue
 * 
 */

public class DaiThienSu extends Npc {

    public DaiThienSu(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (!canOpenNpc(player)) {
            return;
        }

        if (this.mapId == 145) {
            createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Ta sẽ đưa ngươi trở về hành tinh Bill.",
                    "Về hành tinh\nBill", "Từ chối");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) {
            return;
        }

        if (player.idMark.isBaseMenu() && this.mapId == 145 && select == 0) {
            ChangeMapService.gI().changeMapInYard(player, 154, -1, 725);
        }
    }
}
