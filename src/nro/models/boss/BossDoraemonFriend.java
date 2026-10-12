package nro.models.boss;

import nro.models.consts.BossStatus;
import nro.models.item.Item;
import nro.models.map.ItemMap;
import nro.models.map.Zone;
import nro.models.map.service.ChangeMapService;
import nro.models.map.service.MapService;
import nro.models.player.Pet;
import nro.models.player.Player;
import nro.models.player_system.Template.ItemTemplate;
import nro.models.services.ItemService;
import nro.models.services.PlayerService;
import nro.models.services.Service;
import nro.models.utils.Util;

/** Native three-part friends; each city admits its own pair in the boss zone. */
public final class BossDoraemonFriend extends Boss {
    public static final int SPAWN_Y = 480;
    public static final int COSTUME_DROP_PERCENT = 10;
    public final int homeMapId;
    public final int spawnX;
    public final int costumeItemId;

    public BossDoraemonFriend(int bossId) throws Exception {
        super(bossId, false, true, dataFor(bossId));
        this.homeMapId = this.data[0].getMapJoin()[0];
        this.spawnX = bossId == BossID.SHIZUKA || bossId == BossID.CHAIEN ? 600 : 1320;
        this.costumeItemId = switch (bossId) {
            case BossID.SHIZUKA -> 2449;
            case BossID.NOBITA -> 2499;
            case BossID.CHAIEN -> 2500;
            case BossID.XEKO -> 2501;
            default -> throw new IllegalArgumentException("Unknown Doraemon friend");
        };
    }

    private static BossData dataFor(int bossId) {
        return switch (bossId) {
            case BossID.SHIZUKA -> BossesData.SHIZUKA;
            case BossID.NOBITA -> BossesData.NOBITA;
            case BossID.CHAIEN -> BossesData.CHAIEN;
            case BossID.XEKO -> BossesData.XEKO;
            default -> throw new IllegalArgumentException("Unknown Doraemon friend");
        };
    }

    private boolean acceptsResidents(Zone candidate) {
        for (Player resident : candidate.getBosses()) {
            if (!(resident instanceof BossDoraemonFriend) || resident.id == this.id
                    || ((BossDoraemonFriend) resident).homeMapId != this.homeMapId) {
                return false;
            }
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
            if (candidate != null && candidate.map == map && candidate.zoneId >= 2
                    && candidate.getNumOfPlayers() <= 10 && acceptsResidents(candidate)
                    && BossSpawnPolicy.destination(this, candidate) == candidate) {
                return candidate;
            }
        }
        return null;
    }

    @Override
    public void joinMap() {
        // Both friend admission and the shared mini-boss path use this lock.
        // A city's two distinct friends may coexist; duplicates and outsiders wait.
        synchronized (BossSpawnPolicy.class) {
            Zone destination = getMapJoin();
            if (destination == null) {
                this.zone = null;
                this.lastZone = null;
                this.lastTimeRest = System.currentTimeMillis();
                this.changeStatus(BossStatus.REST);
                return;
            }
            int x = Math.max(120, Math.min(this.spawnX, destination.map.mapWidth - 120));
            int y = destination.map.yPhysicInTop(x, SPAWN_Y - 24);
            // Native changeMap requires a source zone even for a first spawn.
            // Keep an existing source so it can remove the old membership.
            if (this.zone == null) {
                this.zone = destination;
            }
            ChangeMapService.gI().changeMap(this, destination, x, y);
            if (this.zone != destination || !destination.getBosses().contains(this)) {
                if (this.zone != null) {
                    ChangeMapService.gI().exitMap(this);
                }
                this.zone = null;
                this.lastZone = null;
                this.lastTimeRest = System.currentTimeMillis();
                this.changeStatus(BossStatus.REST);
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
        if (this.zone == null || this.zone.map == null) {
            return;
        }
        int targetX = Math.max(24, Math.min(x, this.zone.map.mapWidth - 24));
        int difference = targetX - this.location.x;
        int nextX = this.location.x + Integer.signum(difference) * Math.min(48, Math.abs(difference));
        int nextY = this.zone.map.yPhysicInTop(nextX, Math.max(0, y - 24));
        PlayerService.gI().playerMove(this, nextX, nextY);
    }

    @Override
    public void reward(Player plKill) {
        Player recipient = plKill instanceof Pet ? ((Pet) plKill).master : plKill;
        if (recipient == null || recipient.isBoss || recipient.isBot) {
            return;
        }
        super.reward(recipient);
        if (this.zone == null || this.zone.map == null || this.zone.map.mapId != this.homeMapId) {
            return;
        }
        ItemTemplate template = ItemService.gI().getTemplate(this.costumeItemId);
        if (template == null || template.id != this.costumeItemId || !Util.isTrue(COSTUME_DROP_PERCENT, 100)) {
            return;
        }
        int x = this.location.x;
        int y = this.zone.map.yPhysicInTop(x, this.location.y - 24);
        Item reward = ItemService.gI().createNewItem((short) this.costumeItemId);
        ItemMap costume = new ItemMap(this.zone, template, 1, x, y, recipient.id);
        costume.options.addAll(reward.itemOptions);
        Service.gI().dropItemMap(this.zone, costume);
    }
}
