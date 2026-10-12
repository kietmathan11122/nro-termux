package nro.models.boss.event_tet;

import nro.models.boss.Boss;
import nro.models.boss.BossData;
import nro.models.consts.BossStatus;
import nro.models.consts.ConstNpc;
import nro.models.map.Zone;
import nro.models.map.service.ChangeMapService;
import nro.models.map.service.MapService;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.services.JourneyWestEventService;
import nro.models.services.Service;
import nro.models.skill.Skill;
import nro.models.utils.Util;

public class DuongTangEscort extends Boss {
    private static final long RESPAWN_DELAY = 5 * 60 * 1000L;
    private final int homeMap;
    private Player escort;
    private long lastMove;
    private long respawnAt;
    private boolean waiting;

    public DuongTangEscort(int homeMap) throws Exception {
        super(-9_100_000 - homeMap, new BossData("Đường Tăng bị lạc", (byte) 0,
                new short[]{467, 468, 469, -1, -1, -1}, 1, new int[]{1},
                new int[]{homeMap}, new int[][]{{Skill.DEMON, 1, 1}},
                new String[]{"|-1|Xin hãy dẫn ta về gặp sư phụ ở Làng Aru."},
                new String[]{}, new String[]{}, 1));
        this.homeMap = homeMap;
    }

    @Override public synchronized int injured(Player p, long d, boolean a, boolean b) { return 0; }
    @Override public void attack() { }
    @Override protected void notifyJoinMap() { }

    @Override
    public void joinMap() {
        Zone target = randomHomeZone();
        if (target == null) { changeStatus(BossStatus.REST); return; }
        int x = randomX(target);
        int y = target.map.yPhysicInTop(x, 100);
        zone = target;
        ChangeMapService.gI().changeMap(this, target, x, y > 0 ? y : target.map.mapHeight - 24);
        changeStatus(BossStatus.CHAT_S);
    }

    @Override
    public void active() {
        if (waiting) {
            if (System.currentTimeMillis() >= respawnAt) respawnAtHome();
            return;
        }
        if (zone == null) return;
        if (escort == null || escort.isDie() || escort.zone == null || escort.zone.map.planetId != 0) {
            escort = nearbyPlayer();
            if (escort != null && JourneyWestEventService.gI().register(escort, this))
                chat("Xin hãy dẫn ta về NPC Đường Tăng ở Làng Aru...");
            else escort = null;
            return;
        }
        if (escort.zone != zone) {
            if (Util.canDoWithTime(lastMove, 700)) followTo(escort);
            return;
        }
        if (tryDeliver()) return;
        if (Util.canDoWithTime(lastMove, 1000)
                && Util.getDistance(location.x, location.y, escort.location.x, escort.location.y) > 85) {
            lastMove = System.currentTimeMillis();
            Service.gI().setPos(this, followX(escort), escort.location.y);
        }
    }

    private Player nearbyPlayer() {
        Player best = null; int distance = 101;
        for (Player p : zone.getPlayers()) {
            if (p == null || p.isBoss || p.isDie()) continue;
            int d = Util.getDistance(location.x, location.y, p.location.x, p.location.y);
            if (d < distance) { best = p; distance = d; }
        }
        return best;
    }

    public void followTo(Player player) {
        if (player != escort || player.zone == null || player.zone.map.planetId != 0) return;
        lastMove = System.currentTimeMillis();
        ChangeMapService.gI().changeMap(this, player.zone, followX(player), player.location.y);
    }

    private boolean tryDeliver() {
        if (zone.map.mapId != 0) return false;
        for (Npc npc : zone.map.npcs) {
            if (npc.tempId == ConstNpc.DUONG_TANG
                    && Util.getDistance(escort.location.x, escort.location.y, npc.cx, npc.cy) <= 120) {
                deliverAtNpc(escort, npc); return true;
            }
        }
        return false;
    }

    public void deliverAtNpc(Player player, Npc npc) {
        if (player != escort || npc == null || npc.tempId != ConstNpc.DUONG_TANG
                || player.zone == null || player.zone.map.mapId != 0 || zone != player.zone) return;
        if (JourneyWestEventService.gI().rewardEscort(player)) scheduleRespawn();
    }

    private void scheduleRespawn() {
        JourneyWestEventService.gI().unregister(escort, this);
        escort = null; waiting = true; respawnAt = System.currentTimeMillis() + RESPAWN_DELAY;
        if (zone != null) ChangeMapService.gI().exitMap(this);
    }

    private void respawnAtHome() {
        Zone target = randomHomeZone();
        if (target == null) return;
        waiting = false; respawnAt = 0; joinMapByZone(target);
        int x = randomX(target);
        int y = target.map.yPhysicInTop(x, 100);
        Service.gI().setPos(this, x, y > 0 ? y : Math.max(24, target.map.mapHeight - 24));
    }

    public void cancelEscort() {
        JourneyWestEventService.gI().unregister(escort, this); escort = null; waiting = true;
    }
    private Zone randomHomeZone() {
        nro.models.map.Map map = MapService.gI().getMapById(homeMap);
        return map == null || map.zones.isEmpty() ? null : map.zones.get(Util.nextInt(map.zones.size()));
    }
    private int randomX(Zone z) { return z.map.mapWidth > 100 ? Util.nextInt(50, z.map.mapWidth - 50) : 24; }
    private int followX(Player p) { return Math.max(24, Math.min(p.location.x - 45, p.zone.map.mapWidth - 24)); }
}
