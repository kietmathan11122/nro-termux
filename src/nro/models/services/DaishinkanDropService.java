package nro.models.services;

import java.time.LocalDate;
import nro.models.data.LocalManager;
import nro.models.data.LocalResultSet;
import nro.models.map.ItemMap;
import nro.models.player.Player;
import nro.models.utils.Util;

/** Drops reserved for players wearing the complete Angel set. */
public final class DaishinkanDropService {

    private static final int MAP_TRAINING = 209;
    private static final int DAILY_LIMIT = 50;
    private static final int GLOVE_FRAGMENT = 1898;
    private static final int[] MOB_FRAGMENTS = {1894, 1895, 1896, 1897};

    private DaishinkanDropService() {
    }

    public static void dropBossFragment(Player player, int x, int y) {
        if (canReceive(player)) {
            drop(player, GLOVE_FRAGMENT, x, y);
        }
    }

    public static void dropMobFragment(Player player, int x, int y) {
        if (player == null || player.zone == null || player.zone.map.mapId != MAP_TRAINING
                || !Util.isTrue(10, 100) || !canReceive(player)) {
            return;
        }
        drop(player, MOB_FRAGMENTS[Util.nextInt(MOB_FRAGMENTS.length)], x, y);
    }

    private static boolean canReceive(Player player) {
        return player != null && !player.isBot && player.setClothes != null
                && player.setClothes.checkSetThienSu() && reserveDailyDrop(player.id);
    }

    private static synchronized boolean reserveDailyDrop(long playerId) {
        String today = LocalDate.now().toString();
        try {
            LocalResultSet rs = LocalManager.executeQuery(
                    "SELECT amount FROM daishinkan_daily_drop WHERE player_id = ? AND drop_date = ?", playerId, today);
            int amount = rs.next() ? rs.getInt("amount") : 0;
            if (amount >= DAILY_LIMIT) {
                return false;
            }
            LocalManager.executeUpdate(
                    "INSERT INTO daishinkan_daily_drop(player_id, drop_date, amount) VALUES (?, ?, 1) "
                    + "ON DUPLICATE KEY UPDATE amount = amount + 1", playerId, today);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void drop(Player player, int itemId, int x, int y) {
        int groundY = player.zone.map.yPhysicInTop(x, y);
        Service.gI().dropItemMap(player.zone, new ItemMap(player.zone, itemId, 1, x, groundY, player.id));
    }
}
