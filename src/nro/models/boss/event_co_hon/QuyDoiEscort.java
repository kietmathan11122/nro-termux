package nro.models.boss.event_co_hon;

import nro.models.boss.Boss;
import nro.models.boss.BossData;
import nro.models.boss.BossID;
import nro.models.consts.BossStatus;
import nro.models.consts.ConstNpc;
import nro.models.map.Zone;
import nro.models.map.service.ChangeMapService;
import nro.models.map.service.MapService;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.services.CoHonEventService;
import nro.models.services.Service;
import nro.models.skill.Skill;
import nro.models.utils.Util;

public class QuyDoiEscort extends Boss {

    private static final long RESPAWN_DELAY = 5 * 60 * 1000L;

    private final int[] routeMaps;
    private final int destinationMap;
    private final int homeMap;
    private Player escort;
    private long lastMove;
    private long respawnAt;
    private boolean waitingRespawn;

    public void cancelEscortForEventStop() {
        CoHonEventService.gI().unregisterHungryGhostEscort(escort, this);
        escort = null;
        waitingRespawn = true;
        respawnAt = Long.MAX_VALUE;
    }

    public QuyDoiEscort(int[] routeMaps, int destinationMap, int homeMap) throws Exception {
        super(BossID.QUY_DOI_CO_HON, new BossData(
                "Quỷ Đói",
                (byte) 1,
                new short[]{3291, 3292, 3293, -1, -1, -1},
                2_000_000_000,
                new int[]{1},
                new int[]{homeMap},
                new int[][]{{Skill.DEMON, 1, 1}},
                new String[]{"|-1|Ta đói quá... ai đưa ta về với Cô Hồn?"},
                new String[]{},
                new String[]{},
                1));
        this.routeMaps = routeMaps;
        this.destinationMap = destinationMap;
        this.homeMap = homeMap;
    }

    @Override
    protected void notifyJoinMap() {
        // Không thông báo toàn server khi Quỷ Đói đổi map theo người dẫn.
    }

    /**
     * Lần xuất hiện đầu tiên cũng chọn khu ngẫu nhiên. Boss.joinMap mặc định
     * thường dồn boss vào khu trống đầu tiên nên không phù hợp với Quỷ Đói.
     */
    @Override
    public void joinMap() {
        Zone target = randomHomeZone();
        if (target == null) {
            changeStatus(BossStatus.REST);
            return;
        }
        int x = randomSpawnX(target);
        int y = target.map.yPhysicInTop(x, 100);
        if (y <= 0) {
            y = Math.max(24, target.map.mapHeight - 24);
        }
        this.zone = target;
        ChangeMapService.gI().changeMap(this, target, x, y);
        notifyJoinMap();
        changeStatus(BossStatus.CHAT_S);
    }

    @Override
    public synchronized int injured(Player attacker, long damage, boolean piercing, boolean isMobAttack) {
        return 0;
    }

    @Override
    public void attack() {
        // Quỷ Đói của nhiệm vụ hộ tống không chiến đấu.
    }

    @Override
    public void active() {
        if (!CoHonEventService.gI().isActive()) {
            return;
        }
        if (waitingRespawn) {
            if (System.currentTimeMillis() >= respawnAt) {
                respawnAtHomeMap();
            }
            return;
        }
        if (zone == null) {
            return;
        }
        if (escort == null || escort.isDie() || escort.zone == null || !isRouteMap(escort.zone.map.mapId)) {
            escort = findNearbyPlayer();
            if (escort != null) {
                if (CoHonEventService.gI().registerHungryGhostEscort(escort, this)) {
                    chat("Xin hãy dắt ta về gặp Cô Hồn ở cuối tuyến này...");
                } else {
                    escort = null;
                }
            }
            return;
        }

        if (escort.zone != zone) {
            if (Util.canDoWithTime(lastMove, 700)) {
                lastMove = System.currentTimeMillis();
                int x = followX(escort);
                int y = safeFollowY(escort);
                ChangeMapService.gI().changeMap(this, escort.zone, x, y);
            }
            return;
        }

        if (tryDeliver()) {
            return;
        }

        if (Util.canDoWithTime(lastMove, 1_000)
                && Util.getDistance(location.x, location.y, escort.location.x, escort.location.y) > 85) {
            lastMove = System.currentTimeMillis();
            // Bám theo bằng một lần cập nhật vị trí thay vì phát liên tục nhiều
            // gói playerMove. Khóa Y theo chân người dẫn để không lọt xuống tile.
            Service.gI().setPos(this, followX(escort), safeFollowY(escort));
        }
    }

    private Player findNearbyPlayer() {
        Player nearest = null;
        int nearestDistance = 101;
        for (Player player : zone.getPlayers()) {
            if (player == null || player.isBoss || player.isDie()) {
                continue;
            }
            int distance = Util.getDistance(location.x, location.y, player.location.x, player.location.y);
            if (distance < nearestDistance) {
                nearest = player;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private boolean tryDeliver() {
        if (zone.map.mapId != destinationMap) {
            return false;
        }
        for (Npc npc : zone.map.npcs) {
            if (npc.tempId == ConstNpc.CO_HON
                    && Util.getDistance(escort.location.x, escort.location.y, npc.cx, npc.cy) <= 120) {
                deliverAtNpc(escort, npc);
                return true;
            }
        }
        return false;
    }

    private void scheduleRespawn() {
        CoHonEventService.gI().unregisterHungryGhostEscort(escort, this);
        escort = null;
        waitingRespawn = true;
        respawnAt = System.currentTimeMillis() + RESPAWN_DELAY;
        if (zone != null) {
            ChangeMapService.gI().exitMap(this);
        }
    }

    private void respawnAtHomeMap() {
        // Sau mỗi lần được giao, 5 phút sau Quỷ Đói trở lại ở một khu mới
        // ngẫu nhiên của map nhà, không cố định khu 0.
        Zone target = randomHomeZone();
        if (target != null) {
            int x = randomSpawnX(target);
            int y = target.map.yPhysicInTop(x, 100);
            if (y <= 0) {
                y = Math.max(24, target.map.mapHeight - 24);
            }
            waitingRespawn = false;
            respawnAt = 0;
            joinMapByZone(target);
            Service.gI().setPos(this, x, y);
        }
    }

    private Zone randomHomeZone() {
        nro.models.map.Map map = MapService.gI().getMapById(homeMap);
        if (map == null || map.zones == null || map.zones.isEmpty()) {
            return null;
        }
        return map.zones.get(Util.nextInt(0, map.zones.size() - 1));
    }

    private int randomSpawnX(Zone target) {
        int minX = 50;
        int maxX = target.map.mapWidth - 50;
        return maxX > minX ? Util.nextInt(minX, maxX) : Math.max(24, target.map.mapWidth / 2);
    }

    public void followTo(Player player) {
        if (player == null || player != escort || player.zone == null || !isRouteMap(player.zone.map.mapId)) {
            return;
        }
        lastMove = System.currentTimeMillis();
        int x = followX(player);
        int y = safeFollowY(player);
        ChangeMapService.gI().changeMap(this, player.zone, x, y);
    }

    private int followX(Player player) {
        int x = player.location.x - 45;
        if (x < 24) {
            x = player.location.x + 45;
        }
        return Math.max(24, Math.min(x, player.zone.map.mapWidth - 24));
    }

    private int safeFollowY(Player player) {
        // Tọa độ Y của Player là điểm chân đã được client/server xác thực.
        // Dùng thẳng điểm này ổn định hơn yPhysicInTop tại X lệch, vì hàm đó
        // có thể trả 0 khi cột tile không tìm thấy mặt đất.
        return Math.max(0, Math.min(player.location.y, player.zone.map.mapHeight - 24));
    }

    public void deliverAtNpc(Player player, Npc npc) {
        if (player != escort || npc == null || npc.tempId != ConstNpc.CO_HON
                || player.zone == null || player.zone.map.mapId != destinationMap
                || zone != player.zone) {
            Service.gI().sendThongBao(player,
                    "Hãy đưa Quỷ Đói đến đúng Cô Hồn ở cuối tuyến.");
            return;
        }
        if (CoHonEventService.gI().rewardHungryGhostEscort(player)) {
            chat("Cảm ơn... cuối cùng ta đã được trở về.");
            scheduleRespawn();
        }
    }

    private boolean isRouteMap(int mapId) {
        for (int allowed : routeMaps) {
            if (allowed == mapId) {
                return true;
            }
        }
        return false;
    }
}
