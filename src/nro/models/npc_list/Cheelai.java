package nro.models.npc_list;

import nro.models.consts.ConstNpc;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.combine.CombineService;

public class Cheelai extends Npc {

    public Cheelai(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            this.createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Ta có thể giúp gì cho ngươi?", "Nâng cấp\nPet Ba", "Từ chối");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (player.idMark.isBaseMenu()) {
                switch (select) {
                    case 0 -> {
                        CombineService.gI().openTabCombine(player, CombineService.NANG_CAP_BA);
                    }
                }
            } else if (player.idMark.getIndexMenu() == ConstNpc.MENU_START_COMBINE) {
                if (player.combineNew.typeCombine == CombineService.NANG_CAP_BA) {
                    if (select == 0) {
                        CombineService.gI().startCombine(player);
                    }
                }
            }
        }
    }
}
