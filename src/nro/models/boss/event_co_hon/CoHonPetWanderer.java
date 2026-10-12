package nro.models.boss.event_co_hon;

import nro.models.boss.Boss;
import nro.models.boss.BossData;
import nro.models.consts.BossStatus;
import nro.models.map.Zone;
import nro.models.map.service.ChangeMapService;
import nro.models.map.service.MapService;
import nro.models.player.Player;
import nro.models.services.CoHonEventService;
import nro.models.services.Service;
import nro.models.skill.Skill;
import nro.models.utils.Util;

/**
 * Pet Cô Hồn xuất hiện trực tiếp trong map. Pet không đánh người chơi, chỉ đi
 * chậm quanh vách núi và biến mất một lúc sau khi nhận Mâm Cúng.
 */
public class CoHonPetWanderer extends Boss {

    private static final long RESPAWN_DELAY = 2 * 60 * 1000L;
    private final int petItemId;
    private final int homeMapId;
    private long lastMove;
    private long respawnAt;
    private boolean waitingRespawn;

    public CoHonPetWanderer(int bossId, int petItemId, int homeMapId,
            String name, short[] outfit) throws Exception {
        super(bossId, new BossData(
                name,
                (byte) 1,
                outfit,
                1,
                new int[]{1},
                new int[]{homeMapId},
                new int[][]{{Skill.DEMON, 1, 1}},
                new String[]{}, new String[]{}, new String[]{}, 1));
        this.petItemId = petItemId;
        this.homeMapId = homeMapId;
    }

    @Override
    protected void notifyJoinMap() {
        // Pet sự kiện không phát thông báo boss toàn server.
    }

    @Override
    public void joinMap() {
        Zone target = randomHomeZone();
        if (target == null) {
            changeStatus(BossStatus.REST);
            return;
        }
        int x = randomX(target);
        int y = safeGroundY(target, x);
        zone = target;
        ChangeMapService.gI().changeMap(this, target, x, y);
        changeStatus(BossStatus.ACTIVE);
    }

    @Override
    public synchronized int injured(Player attacker, long damage, boolean piercing, boolean isMobAttack) {
        return 0;
    }

    @Override
    public void attack() {
        // Đây là pet lang thang, không có hành vi tấn công.
    }

    @Override
    public void active() {
        if (!CoHonEventService.gI().isActive()) {
            return;
        }
        if (waitingRespawn) {
            if (System.currentTimeMillis() >= respawnAt) {
                waitingRespawn = false;
                respawnAt = 0;
                joinMap();
            }
            return;
        }
        if (zone == null || !Util.canDoWithTime(lastMove, 1800)) {
            return;
        }
        lastMove = System.currentTimeMillis();
        int step = Util.isTrue(50, 100) ? Util.nextInt(26, 52) : -Util.nextInt(26, 52);
        int x = Math.max(30, Math.min(zone.map.mapWidth - 30, location.x + step));
        Service.gI().setPos(this, x, safeGroundY(zone, x));
    }

    public boolean isNear(Player player) {
        return !waitingRespawn && player != null && player.zone == zone && zone != null
                && Util.getDistance(location.x, location.y, player.location.x, player.location.y) <= 120;
    }

    public int getPetItemId() {
        return petItemId;
    }

    public void acceptOffering() {
        if (waitingRespawn) {
            return;
        }
        chat("Cảm ơn Mâm Cúng... ta để lại linh thú cho ngươi.");
        waitingRespawn = true;
        respawnAt = System.currentTimeMillis() + RESPAWN_DELAY;
        if (zone != null) {
            ChangeMapService.gI().exitMap(this);
        }
    }

    public void cancelForEventStop() {
        waitingRespawn = true;
        respawnAt = Long.MAX_VALUE;
        if (zone != null) {
            ChangeMapService.gI().exitMap(this);
        }
    }

    private Zone randomHomeZone() {
        nro.models.map.Map map = MapService.gI().getMapById(homeMapId);
        if (map == null || map.zones == null || map.zones.isEmpty()) {
            return null;
        }
        return map.zones.get(Util.nextInt(0, map.zones.size() - 1));
    }

    private int randomX(Zone target) {
        int min = 50;
        int max = target.map.mapWidth - 50;
        return max > min ? Util.nextInt(min, max) : Math.max(24, target.map.mapWidth / 2);
    }

    private int safeGroundY(Zone target, int x) {
        int y = target.map.yPhysicInTop(x, 100);
        return y > 0 ? y : Math.max(24, target.map.mapHeight - 24);
    }
}
