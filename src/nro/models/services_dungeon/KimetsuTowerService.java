package nro.models.services_dungeon;

import nro.models.consts.ConstPlayer;
import nro.models.map.service.ChangeMapService;
import nro.models.map.service.MapService;
import nro.models.player.Player;
import nro.models.services.Service;

public class KimetsuTowerService {

    public static final int AGATSUMA_REQUEST = 30001;
    public static final int DEMON_KING_REQUEST = 30002;
    private static KimetsuTowerService instance;

    public static KimetsuTowerService gI() {
        if (instance == null) {
            instance = new KimetsuTowerService();
        }
        return instance;
    }

    public void interact(Player player, int requestId) {
        if (player == null || player.zone == null || player.zone.map == null
                || !MapService.gI().isMapKimetsuTower(player.zone.map.mapId)) {
            return;
        }

        int desiredFlag = requestId == AGATSUMA_REQUEST ? 9 : 10;
        String factionName = desiredFlag == 9 ? "phe Agatsuma" : "phe Quỷ Vương";
        if (player.cFlag != desiredFlag) {
            Service.gI().changeFlag(player, desiredFlag);
            Service.gI().SendPowerInfo(player);
            Service.gI().sendThongBao(player,
                    "Đã gia nhập " + factionName + ". Hạ boss hoặc người chơi khác phe để nhận điểm.");
            return;
        }

        int mapId = player.zone.map.mapId;
        if (player.fightMabu.pointMabu < player.fightMabu.POINT_MAX) {
            Service.gI().sendThongBao(player, "Bạn đang có " + player.fightMabu.pointMabu
                    + "/" + player.fightMabu.POINT_MAX + " điểm.");
            return;
        }
        if (mapId >= 173) {
            Service.gI().sendThongBao(player, "Đây là tầng cuối cùng.");
            return;
        }

        player.fightMabu.clear();
        Service.gI().SendPowerInfo(player);
        ChangeMapService.gI().changeMap(player, mapId + 1, -1, -1, 100);
    }
}
