package nro.models.boss.Boss_mini;

import java.util.ArrayList;
import java.util.List;
import nro.models.boss.Boss;
import nro.models.boss.BossData;
import nro.models.boss.BossID;
import nro.models.boss.Boss_Manager.BossManager;
import nro.models.consts.BossStatus;
import nro.models.consts.ConstPlayer;
import nro.models.map.ItemMap;
import nro.models.map.Zone;
import nro.models.map.service.ChangeMapService;
import nro.models.map.service.MapService;
import nro.models.player.Player;
import nro.models.player.Pet;
import nro.models.server.Client;
import nro.models.services.PlayerService;
import nro.models.services.Service;
import nro.models.services.SkillService;
import nro.models.skill.Skill;
import nro.models.utils.Util;

public class QuySanta extends Boss {

    private static final int[] SAO_PHA_LE_IDS = {441, 442, 443, 444, 445, 446, 447};
    private long lastTimeCheckPlayer;
    private long st;

    public QuySanta() throws Exception {
        super(BossID.QUY_SANTA, new BossData(
                "Quỷ Santa",
                ConstPlayer.NAMEC,
                new short[]{344, 345, 346, -1, -1, -1}, // Head: 344, Body: 345, Leg: 346
                1000,
                new int[]{100},
                new int[]{3, 4, 6, 10, 19, 20, 27, 28, 29, 30, 31, 32, 33, 34},
                new int[][]{
                    {Skill.DEMON, 7, 1000},
                    {Skill.GALICK, 7, 1000},
                    {Skill.ANTOMIC, 7, 2000},
                    {Skill.THAI_DUONG_HA_SAN, 1, 10000}
                },
                new String[]{"|-1|Giáng sinh ma quái đã tới!", "|-1|Ai muốn quà của Quỷ Santa?"},
                new String[]{"|-1|Nhận lấy cơn thịnh nộ!", "|-1|Ngươi không có quà đâu!"},
                new String[]{"|-1|Quỷ Santa sẽ trở lại..."},
                60)); // Respawn sau 60s
    }

    public static Zone getZoneWithPlayer() {
        List<Player> players = Client.gI().getPlayers();
        List<Zone> candidateZones = new ArrayList<>();
        if (players != null) {
            for (Player pl : players) {
                if (pl != null && !pl.isBoss && !pl.isBot && pl.isPl() && pl.zone != null && pl.zone.map != null) {
                    int mapId = pl.zone.map.mapId;
                    if (!MapService.gI().isMapDoanhTrai(mapId) && !MapService.gI().isMapPhoBan(mapId)
                            && !MapService.gI().isMapOffline(mapId) && mapId != 21 && mapId != 22 && mapId != 23
                            && AnTrom.isAllowedZone(pl.zone)) {
                        candidateZones.add(pl.zone);
                    }
                }
            }
        }
        if (!candidateZones.isEmpty()) {
            return candidateZones.get(Util.nextInt(0, candidateZones.size() - 1));
        }
        int[] fallbackMaps = {3, 4, 6, 10, 19, 20, 27, 28, 29, 30, 31, 32, 33, 34};
        int start = Util.nextInt(0, fallbackMaps.length - 1);
        for (int i = 0; i < fallbackMaps.length; i++) {
            nro.models.map.Map map = MapService.gI().getMapById(fallbackMaps[(start + i) % fallbackMaps.length]);
            if (map != null) {
                for (Zone zone : map.zones) {
                    if (AnTrom.isAllowedZone(zone)) return zone;
                }
            }
        }
        return null;
    }

    @Override
    public Zone getMapJoin() {
        // getZoneWithPlayer already checks fallback maps; never bypass its filter.
        return getZoneWithPlayer();
    }

    @Override
    public int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        Player owner = getRealPlayer(plAtt);
        if (!this.isDie() && owner != null) {
            damage = 1;
            this.nPoint.subHP(damage);
            if (isDie()) {
                this.setDie(owner);
                die(owner);
            }
            return (int) damage;
        } else {
            return 0;
        }
    }

    @Override
    public void attack() {
        if (this.zone == null) {
            return;
        }
        if (Util.canDoWithTime(this.lastTimeAttack, 1200) && this.typePk == ConstPlayer.PK_ALL) {
            this.lastTimeAttack = System.currentTimeMillis();
            try {
                Player pl = getNearestRealPlayer();
                if (pl != null && !pl.isDie()) {
                    if (Util.getDistance(this, pl) > 100) {
                        this.moveToPlayer(pl);
                    } else {
                        // Santa chỉ đấm cận chiến bằng Demon, không chưởng và không choáng.
                        if (!this.playerSkill.skills.isEmpty()) {
                            this.playerSkill.skillSelect = this.playerSkill.skills.get(0);
                            SkillService.gI().useSkill(this, pl, null, -1, null);
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private Player getNearestRealPlayer() {
        Player target = null;
        int minDistance = Integer.MAX_VALUE;
        for (Player candidate : this.zone.getPlayers()) {
            if (candidate != null && candidate.isPl() && !candidate.isDie()) {
                int distance = Util.getDistance(this, candidate);
                if (distance < minDistance) {
                    minDistance = distance;
                    target = candidate;
                }
            }
        }
        return target;
    }

    private Player getRealPlayer(Player attacker) {
        if (attacker instanceof Pet && ((Pet) attacker).master != null) {
            attacker = ((Pet) attacker).master;
        }
        return attacker != null && attacker.isPl() ? attacker : null;
    }

    @Override
    public void reward(Player plKill) {
        if (plKill == null || !plKill.isPl()) {
            return;
        }

        // RƠI CÁC LOẠI SAO PHA LÊ KHI BỊ PEM CHẾT
        int dropCount = Util.nextInt(1, 3);
        for (int i = 0; i < dropCount; i++) {
            int splId = SAO_PHA_LE_IDS[Util.nextInt(0, SAO_PHA_LE_IDS.length - 1)];
            int x = this.location.x + Util.nextInt(-35, 35);
            int y = this.zone.map.yPhysicInTop(x, this.location.y - 24);
            ItemMap item = new ItemMap(this.zone, splId, 1, x, y, plKill.id);
            Service.gI().dropItemMap(this.zone, item);
        }
        Service.gI().sendThongBao(plKill, "Quỷ Santa bị tiêu diệt và rơi " + dropCount + " Sao Pha Lê!");
    }

    @Override
    public void active() {
        if (!AnTrom.isAllowedZone(this.zone, this)) {
            this.leaveMap();
            return;
        }
        if (this.typePk == ConstPlayer.NON_PK) {
            this.changeToTypePK();
        }
        this.attack();

        // Nếu khu không có người chơi thì đổi sang map có nhân vật sau 30s
        if (this.zone != null && this.zone.getNumOfPlayers() <= 0 && Util.canDoWithTime(this.lastTimeCheckPlayer, 30000)) {
            Zone newZone = getZoneWithPlayer();
            if (newZone != null && newZone != this.zone) {
                ChangeMapService.gI().changeMap(this, newZone, -1, -1);
            }
            this.lastTimeCheckPlayer = System.currentTimeMillis();
        }

        if (Util.canDoWithTime(st, 900000)) {
            this.changeStatus(BossStatus.LEAVE_MAP);
        }
    }

    @Override
    public void joinMap() {
        this.name = "Quỷ Santa";
        setFixedHp();
        this.nPoint.dameg = 1000;
        this.joinMap2();
        st = System.currentTimeMillis();
    }

    /** Triệu hồi trực tiếp cạnh người chơi, kể cả khi người chơi đang treo máy. */
    public void summonAt(Zone targetZone, int x, int y) throws Exception {
        try {
            if (!AnTrom.isAllowedZone(targetZone)) {
                throw new IllegalArgumentException("Chỉ gọi boss mini từ khu 2, ở map thường chưa có boss mini");
            }
            this.currentLevel = 0;
            this.initBase();
            this.typePk = ConstPlayer.NON_PK;
            this.name = "Quỷ Santa";
            setFixedHp();
            this.nPoint.dameg = 1000;
            this.lastTimeCheckPlayer = System.currentTimeMillis();
            this.st = System.currentTimeMillis();
            ChangeMapService.gI().goToMap(this, targetZone);
            this.location.x = x;
            int groundY = targetZone.map.yPhysicInTop(x, y);
            this.location.y = groundY > 0 ? groundY : y;
            PlayerService.gI().playerMove(this, this.location.x, this.location.y);
            targetZone.load_Me_To_Another(this);
            this.changeStatus(BossStatus.CHAT_S);
            this.changeToTypePK();
        } catch (Exception e) {
            if (this.zone != null) {
                ChangeMapService.gI().exitMap(this);
            }
            BossManager.gI().removeBoss(this);
            throw e;
        }
    }

    private void setFixedHp() {
        this.nPoint.hpg = 100;
        this.nPoint.hpMax = 100;
        this.nPoint.hp = 100;
    }

    public void joinMap2() {
        this.zone = getMapJoin();
        if (this.zone != null) {
            try {
                ChangeMapService.gI().changeMap(this, this.zone, -1, -1);
                this.changeStatus(BossStatus.CHAT_S);
                this.changeToTypePK();
            } catch (Exception e) {
                this.changeStatus(BossStatus.REST);
            }
        } else {
            this.changeStatus(BossStatus.RESPAWN);
        }
    }

    @Override
    public void leaveMap() {
        ChangeMapService.gI().exitMap(this);
        this.lastZone = null;
        this.lastTimeRest = System.currentTimeMillis();
        this.changeStatus(BossStatus.REST);
    }
}
