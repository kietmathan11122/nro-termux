package nro.models.server;

import java.io.IOException;
import java.util.Objects;
import nro.models.consts.ConstNpc;
import nro.models.npc.Npc;
import nro.models.npc_list.Lemo;
import nro.models.map.service.NpcManager;
import nro.models.network.MySession;
import nro.models.player.Player;
import nro.models.services.Service;
import nro.models.services.ProfessionService;
import nro.models.services_func.TransactionService;

/**
 *
 * @author By Mr Blue
 * 
 */

public class MenuController {

    public static final int PROFESSION_MENU_REQUEST = 10000;

    private static MenuController instance;

    public static MenuController gI() {
        if (instance == null) {
            instance = new MenuController();
        }
        return instance;
    }

    public void openMenuNPC(MySession session, int idnpc, Player player) {
        TransactionService.gI().cancelTrade(player);
        Npc npc;
        if (idnpc == PROFESSION_MENU_REQUEST) {
            npc = NpcManager.getNpc(ConstNpc.LEMO);
            if (npc instanceof Lemo) {
                ((Lemo) npc).openProfessionMenu(player);
                return;
            }
        } else if (idnpc == ConstNpc.CALICK && player.zone.map.mapId != 102) {
            npc = NpcManager.getNpc(ConstNpc.CALICK);
        } else if (idnpc == ConstNpc.LY_TIEU_NUONG) {
            npc = NpcManager.getNpc(ConstNpc.LY_TIEU_NUONG);
        } else {
            npc = player.zone.map.getNpc(player, idnpc);
        }
        if (npc != null) {
            nro.models.services.StoryAdventureService.gI().onTalkNpc(player, npc);
            ProfessionService.gI().onTalkNpc(player, idnpc);
            npc.openBaseMenu(player);
        } else {
            Service.gI().hideWaitDialog(player);
        }
    }

    public void doSelectMenu(Player player, int npcId, int select) throws IOException {
        TransactionService.gI().cancelTrade(player);
        if (nro.models.services.StoryAdventureService.gI().handleMenu(player, npcId, select)) return;
        switch (npcId) {
            case ConstNpc.RONG_THIENG, ConstNpc.CON_MEO ->
                Objects.requireNonNull(NpcManager.getNpc((byte) npcId)).confirmMenu(player, select);
            default -> {
                Npc npc = null;
                if (npcId == ConstNpc.LEMO) {
                    npc = NpcManager.getNpc(ConstNpc.LEMO);
                } else if (npcId == ConstNpc.CALICK && player.zone.map.mapId != 102) {
                    npc = NpcManager.getNpc(ConstNpc.CALICK);
                } else if (npcId == ConstNpc.LY_TIEU_NUONG) {
                    npc = NpcManager.getNpc(ConstNpc.LY_TIEU_NUONG);
                } else if (player.zone != null) {
                    npc = player.zone.map.getNpc(player, npcId);
                }
                if (npc != null) {
                    npc.confirmMenu(player, select);
                } else {
                    Service.gI().hideWaitDialog(player);
                }
            }
        }

    }
}
