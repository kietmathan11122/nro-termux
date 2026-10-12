package nro.models.npc_list;

import nro.models.consts.ConstNpc;
import nro.models.map.service.ChangeMapService;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.services.TaskService;

public class Daishinkan extends Npc {

    private static final String GUIDE = "|2|H\u01b0\u1edbng d\u1eabn ch\u1ebf t\u1ea1o trang b\u1ecb Daishinkan\n\n"
            + "|7|1. M\u1eb7c \u0111\u1ee7 Full Set Thi\u00ean S\u1ee9.\n"
            + "|7|2. Ch\u1ecdn 999 m\u1ea3nh c\u00f9ng lo\u1ea1i v\u00e0 C\u00f4ng th\u1ee9c VIP.\n"
            + "|7|3. Th\u00eam \u0110\u00e1 n\u00e2ng c\u1ea5p \u0111\u1ec3 t\u0103ng t\u1ec9 l\u1ec7 th\u00e0nh c\u00f4ng.\n"
            + "|7|4. Th\u00eam T\u00fai may m\u1eafn \u0111\u1ec3 t\u0103ng c\u01a1 h\u1ed9i c\u00f3 th\u00eam ch\u1ec9 s\u1ed1.\n"
            + "|7|5. Chi ph\u00ed: 200 t\u1ef7 v\u00e0ng v\u00e0 100 ng\u1ecdc.\n\n"
            + "|2|Sau \u0111\u00f3 ch\u1ecdn Ch\u1ebf t\u1ea1o \u0111\u1ec3 b\u1eaft \u0111\u1ea7u.";

    public Daishinkan(int mapId, int status, int cx, int cy, int tempId, int avatar) {
        super(mapId, status, cx, cy, tempId, avatar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (!canOpenNpc(player)) {
            return;
        }
        if (TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
            return;
        }
        if (this.mapId == 209) {
            createOtherMenu(player, ConstNpc.BASE_MENU, "Ta c\u00f3 th\u1ec3 gi\u00fap g\u00ec cho ng\u01b0\u01a1i?", "V\u00f5 \u0111\u00e0i si\u00eau c\u1ea5p", "T\u1eeb ch\u1ed1i");
        } else {
            createOtherMenu(player, ConstNpc.BASE_MENU, "Ta c\u00f3 th\u1ec3 gi\u00fap g\u00ec cho ng\u01b0\u01a1i?", "Khu luy\u1ec7n t\u1eadp", "Ch\u1ebf t\u1ea1o trang b\u1ecb Daishinkan", "H\u01b0\u1edbng d\u1eabn", "T\u1eeb ch\u1ed1i");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) {
            return;
        }
        if (!player.idMark.isBaseMenu()) {
            if ((player.idMark.getIndexMenu() == nro.models.combine.CombineService.CHE_TAO_TRANG_BI_DAISHINKAN
                    || player.idMark.getIndexMenu() == ConstNpc.MENU_START_COMBINE) && select == 0) {
                nro.models.combine.CombineService.gI().startCombine(player);
            }
            return;
        }
        if (this.mapId == 209) {
            if (select == 0) {
                ChangeMapService.gI().changeMapInYard(player, 154, -1, 725);
            }
            return;
        }
        if (select == 0) {
            ChangeMapService.gI().changeMapInYard(player, 209, -1, 350);
        } else if (select == 1) {
            nro.models.combine.CombineService.gI().openTabCombine(player, nro.models.combine.CombineService.CHE_TAO_TRANG_BI_DAISHINKAN);
        } else if (select == 2) {
            createOtherMenu(player, ConstNpc.IGNORE_MENU, GUIDE, "\u0110\u00f3ng");
        }
    }
}
