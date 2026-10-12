package nro.models.boss;

import nro.models.consts.BossStatus;
import nro.models.consts.ConstPlayer;
import nro.models.map.Zone;
import nro.models.map.service.ChangeMapService;
import nro.models.map.service.MapService;
import nro.models.player.Pet;
import nro.models.player.Player;
import nro.models.services.PlayerService;
import nro.models.services.Service;
import nro.models.skill.Skill;
import nro.models.utils.Logger;

/** One native boss in the final map of each anime area. */
public final class AnimeMapBoss extends Boss {
    public static final int RESPAWN_SECONDS = 300;
    public static final int SPAWN_X = 960;
    public static final int SPAWN_Y = 480;
    public final int homeMapId;
    public final int mobTemplateId;

    private static final int[] IDS = {
        BossID.ANIME_AINZ, BossID.ANIME_FATHER,
        BossID.ANIME_ALL_FOR_ONE, BossID.ANIME_KATANA_MAN,
        BossID.ANIME_BOROS, BossID.ANIME_KANEKI,
        BossID.ANIME_IGRIS, BossID.ANIME_DEMON_KING,
        BossID.ANIME_ZODD, BossID.ANIME_YOR
    };
    private static final String[] NAMES = {
        "Ainz Ooal Gown", "Father", "All For One", "Katana Man", "Boros",
        "Kaneki", "Igris", "Ma Thần Vương", "Zodd", "Yor Forger"
    };

    public AnimeMapBoss(int bossId) throws Exception {
        super(bossId, false, false, dataFor(bossId));
        int index = indexFor(bossId);
        this.homeMapId = 276 + index * 3;
        this.mobTemplateId = 153 + index;
    }

    private static int indexFor(int bossId) {
        for (int i = 0; i < IDS.length; i++) {
            if (IDS[i] == bossId) return i;
        }
        throw new IllegalArgumentException("Unknown anime map boss ID: " + bossId);
    }

    private static BossData dataFor(int bossId) {
        int index = indexFor(bossId);
        return new BossData(
                NAMES[index], ConstPlayer.TRAI_DAT,
                // Valid native fallback outfit; the matching client renders the
                // complete mob atlas by this boss's explicit character ID.
                new short[]{39, 40, 41, -1, -1, -1},
                15_000 + index * 2_000,
                new int[]{15_000_000 + index * 5_000_000},
                new int[]{276 + index * 3},
                new int[][]{{Skill.DRAGON, 7, 1_500}, {Skill.KAMEJOKO, 7, 2_000}},
                new String[]{"|-1|Muốn vượt qua đây, hãy đánh bại ta!"},
                new String[]{"|-1|Hãy chuẩn bị đón chiêu tiếp theo!"},
                new String[]{"|-1|Năm phút nữa ta sẽ trở lại!"},
                RESPAWN_SECONDS);
    }

    private boolean acceptsResidents(Zone candidate) {
        for (Player resident : candidate.getBosses()) {
            if (resident != this) return false;
        }
        return true;
    }

    @Override
    public Zone getMapJoin() {
        nro.models.map.Map map = MapService.gI().getMapById(this.homeMapId);
        if (map == null || map.mapId != this.homeMapId || map.zones == null || map.mapWidth < 240) {
            return null;
        }
        for (Zone candidate : map.zones) {
            if (candidate != null && candidate.map == map && candidate.zoneId == 0
                    && candidate.getNumOfPlayers() <= 10 && acceptsResidents(candidate)
                    && BossSpawnPolicy.destination(this, candidate) == candidate) {
                return candidate;
            }
        }
        return null;
    }

    private void restAfterFailedJoin() {
        if (this.zone != null) {
            ChangeMapService.gI().exitMap(this);
        }
        this.zone = null;
        this.lastZone = null;
        this.playerTarger = null;
        this.lastTimeRest = System.currentTimeMillis();
        this.changeStatus(BossStatus.REST);
    }

    @Override
    public void joinMap() {
        synchronized (BossSpawnPolicy.class) {
            Zone destination = getMapJoin();
            if (destination == null) {
                restAfterFailedJoin();
                return;
            }
            if (this.zone == destination && destination.getBosses().contains(this)) {
                // A repeated spawn call must not duplicate membership or notify
                // players again. Advance only an unfinished join state.
                if (this.bossStatus == BossStatus.JOIN_MAP) {
                    this.changeStatus(BossStatus.CHAT_S);
                }
                return;
            }
            int x = Math.max(120, Math.min(SPAWN_X, destination.map.mapWidth - 120));
            int y = destination.map.yPhysicInTop(x, SPAWN_Y - 24);
            // Native changeMap requires a source zone on the first spawn.
            if (this.zone == null) this.zone = destination;
            try {
                ChangeMapService.gI().changeMap(this, destination, x, y);
            } catch (RuntimeException ex) {
                restAfterFailedJoin();
                Logger.warningln("Anime boss " + this.id + " spawn deferred: " + ex.getMessage());
                return;
            }
            if (this.zone != destination || !destination.getBosses().contains(this)
                    || !acceptsResidents(destination)) {
                restAfterFailedJoin();
                return;
            }
            Service.gI().sendFlagBag(this);
            this.notifyJoinMap();
            this.changeStatus(BossStatus.CHAT_S);
            this.changeToTypePK();
            this.wakeupAnotherBossWhenAppear();
        }
    }

    @Override
    public void moveTo(int x, int y) {
        if (this.zone == null || this.zone.map == null) return;
        int targetX = Math.max(24, Math.min(x, this.zone.map.mapWidth - 24));
        int difference = targetX - this.location.x;
        int nextX = this.location.x + Integer.signum(difference) * Math.min(48, Math.abs(difference));
        int nextY = this.zone.map.yPhysicInTop(nextX, Math.max(0, y - 24));
        PlayerService.gI().playerMove(this, nextX, nextY);
    }

    @Override
    public void reward(Player plKill) {
        Player recipient = plKill instanceof Pet ? ((Pet) plKill).master : plKill;
        if (recipient == null || recipient.isBoss || recipient.isBot) return;
        super.reward(recipient);
    }
}
