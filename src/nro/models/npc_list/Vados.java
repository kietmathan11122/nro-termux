package nro.models.npc_list;
import nro.models.consts.ConstNpc;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.services.TaskService;

public class Vados extends Npc {

    public Vados(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (this.mapId == 121) {
                if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
                    this.createOtherMenu(player, ConstNpc.BASE_MENU,
                            "Con cần ta giúp gì?", "Đóng");
                }
            } else {
                this.createOtherMenu(player, ConstNpc.BASE_MENU,
                        "Con cần ta giúp gì?", "Đóng");
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
    }
}
