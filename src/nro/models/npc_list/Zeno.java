package nro.models.npc_list;

import nro.models.combine.CombineService;
import nro.models.consts.ConstNpc;
import nro.models.npc.Npc;
import nro.models.player.Player;

public class Zeno extends Npc {

    public Zeno(int mapId, int status, int cx, int cy, int tempId, int avatar) {
        super(mapId, status, cx, cy, tempId, avatar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (!canOpenNpc(player)) {
            return;
        }
        createOtherMenu(player, ConstNpc.BASE_MENU,
                "Chào ngươi! Ta là Đấng toàn năng Zeno.\nNgươi muốn nâng cấp Vòng Cổ Rồng à?",
                "Nâng cấp\nVòng Cổ Rồng", "Từ chối");
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) {
            return;
        }
        if (!player.idMark.isBaseMenu()) {
            if ((player.idMark.getIndexMenu() == CombineService.NANG_CAP_VONG_CO
                    || player.idMark.getIndexMenu() == ConstNpc.MENU_START_COMBINE) && select == 0) {
                CombineService.gI().startCombine(player);
            }
            return;
        }
        if (select == 0) {
            CombineService.gI().openTabCombine(player, CombineService.NANG_CAP_VONG_CO);
        }
    }
}
