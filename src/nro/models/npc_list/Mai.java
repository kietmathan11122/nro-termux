package nro.models.npc_list;

import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.consts.ConstNpc;
import nro.models.map.service.ChangeMapService;

import nro.models.services.TaskService;

public class Mai extends Npc {

    public Mai(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (!canOpenNpc(player)) {
            return;
        }
        if (TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
            return;
        }
        if (this.mapId == 211 || (player.zone != null && player.zone.map.mapId == 211)) {
            createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Ta có thể đưa ngươi về Hành tinh Bill.",
                    "Hành tinh\nBill",
                    "Đóng");
        } else {
            createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Ta có thể đưa ngươi tới Hành tinh thời gian.",
                    "Hành tinh\nthời gian",
                    "Đóng");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) {
            return;
        }

        if (player.idMark.getIndexMenu() == ConstNpc.BASE_MENU) {
            if (select == 0) {
                if (this.mapId == 211 || (player.zone != null && player.zone.map.mapId == 211)) {
                    ChangeMapService.gI().changeMap(player, 154, -1, 520, 360);
                } else {
                    ChangeMapService.gI().changeMap(player, 211, -1, 200, 792);
                }
            }
        }
    }
}
