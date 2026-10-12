package nro.models.services;

import nro.models.map.ItemMap;
import nro.models.player.Player;
import nro.models.utils.Util;

/** Nguồn rơi nguyên liệu khắc dòng Nhật Nguyệt từ boss. */
public final class NhatNguyetDropService {

    private static final int[] MATERIAL_IDS = {1939, 1940, 1941};

    private NhatNguyetDropService() {
    }

    public static void dropFromBoss(Player player, int x, int y) {
        if (player == null || player.isBot || player.zone == null) {
            return;
        }
        int itemId = MATERIAL_IDS[Util.nextInt(MATERIAL_IDS.length)];
        int quantity = Util.nextInt(1, 3);
        int groundY = player.zone.map.yPhysicInTop(x, y);
        Service.gI().dropItemMap(player.zone,
                new ItemMap(player.zone, itemId, quantity, x, groundY, player.id));
    }
}
