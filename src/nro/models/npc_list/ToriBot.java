package nro.models.npc_list;

import nro.models.consts.ConstNpc;
import nro.models.map.service.ChangeMapService;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.shop.ShopService;

public class ToriBot extends Npc {

    public ToriBot(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (!canOpenNpc(player)) return;
        if (this.mapId == 123) {
            createOtherMenu(player, ConstNpc.BASE_MENU, "Ta c\u00f3 th\u1ec3 gi\u00fap g\u00ec cho ng\u01b0\u01a1i?", "V\u1ec1 L\u00e0ng\nAru", "Shop \u0110\u00e1 &\nT\u00fai may m\u1eafn", "\u0110\u00f3ng");
        } else {
            createOtherMenu(player, ConstNpc.BASE_MENU, "Ta c\u00f3 th\u1ec3 gi\u00fap g\u00ec cho ng\u01b0\u01a1i?", "Ng\u0169 H\u00e0nh\nS\u01a1n", "Shop \u0110\u00e1 &\nT\u00fai may m\u1eafn", "\u0110\u00f3ng");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player) || !player.idMark.isBaseMenu()) return;
        if (select == 0) {
            if (this.mapId == 123) ChangeMapService.gI().changeMapBySpaceShip(player, 0, -1, 432);
            else ChangeMapService.gI().changeMapBySpaceShip(player, 123, -1, 500);
        } else if (select == 1) {
            ShopService.gI().opendShop(player, "TORIBOT", true);
        }
    }
}
