package nro.models.boss;

import nro.models.boss.Boss_mini.AnTrom;
import nro.models.map.Zone;
import nro.models.player.Player;

/** Shared admission checks for natural spawns, summons and boss travel. */
public final class BossSpawnPolicy {
    private BossSpawnPolicy() {}

    public static boolean isMini(Player player) {
        return player != null && player.isBoss && (player.id == BossID.AN_TROM
                || player.id == BossID.BOT_RATI || player.id == BossID.QUY_SANTA);
    }

    public static Zone destination(Player player, Zone requested) {
        if (requested == null || player == null || !player.isBoss) return requested;
        if (isMini(player)) {
            if (!AnTrom.isAllowedZone(requested, player))
                throw new IllegalArgumentException("Mini boss requires zone >= 2 and an unoccupied map");
            return requested;
        }
        // Anime leaders must be visible in the default zone of their own final map.
        // Keep the zone >= 2 rule for every other boss and every other destination.
        if (player instanceof AnimeMapBoss anime && requested.map != null
                && requested.map.mapId == anime.homeMapId && requested.zoneId == 0) {
            return requested;
        }
        // These are public hunting maps; dedicated dungeon/training maps retain their boss.
        if (AnTrom.isAllowedMap(requested) && requested.zoneId < 2) {
            for (Zone candidate : requested.map.zones) {
                if (candidate.zoneId >= 2) return candidate;
            }
            throw new IllegalArgumentException("No boss zone >= 2 in this map");
        }
        return requested;
    }
}
