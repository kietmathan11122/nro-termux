package nro.models.boss.Boss_mini;

import java.util.ArrayList;
import java.util.List;
import nro.models.boss.Boss;
import nro.models.boss.BossData;
import nro.models.boss.BossID;
import nro.models.boss.Boss_Manager.BossManager;
import nro.models.consts.BossStatus;
import nro.models.consts.ConstPlayer;
import nro.models.player.Inventory;
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

public class AnTrom extends Boss {

    private long goldAnTrom = 0;
    private long lastTimeAnTrom;
    private long lastTimeStun;
    private long lastTimeCheckPlayer;
    private long st;
    private Player fleeFrom;
    private long fleeUntil;
    private long lastFleeMove;

    public AnTrom() throws Exception {
        super(BossID.AN_TROM, new BossData(
                "Ăn Trộm",
                ConstPlayer.TRAI_DAT,
                new short[]{618, 619, 620, -1, -1, -1}, // Head: 618, Body: 619, Leg: 620
                1,
                new int[]{100},
                new int[]{3, 4, 6, 10, 19, 20, 27, 28, 29, 30, 31, 32, 33, 34},
                new int[][]{
                    // Boss nhân đôi hồi chiêu: 15 giây cấu hình = 30 giây thực tế.
                    {Skill.THAI_DUONG_HA_SAN, 3, 15000}
                },
                new String[]{"|-1|Ai có tiền không cho ta xin ít!", "|-1|Hehe ví dày thế này..."},
                new String[]{"|-1|Chạy đâu cho thoát!"},
                new String[]{"|-1|Cứu ta với..."},
                600)); // Respawn sau 10 phút
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
                            && isAllowedZone(pl.zone)) {
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
                    if (isAllowedZone(zone)) return zone;
                }
            }
        }
        return null;
    }

    /** Chỉ áp dụng cho Ăn Trộm, Rati và Santa; không ảnh hưởng boss riêng của map. */
    public static boolean isAllowedMap(Zone zone) {
        if (zone == null || zone.map == null
                || zone.map.type != nro.models.consts.ConstMap.MAP_NORMAL) {
            return false;
        }
        int id = zone.map.mapId;
        MapService maps = MapService.gI();
        // Một số map đặc biệt trong dữ liệu vẫn mang type MAP_NORMAL.
        return id != 44 && id != 47 && id != 100
                && id != 51 && id != 52 && id != 112 && id != 113
                && id != 129 && id != 145 && id != 153 && id != 185
                && id != 201 && id != 209
                && !maps.isHome(id) && !maps.isMapPhoBan(id)
                && !maps.isMapBlackBallWar(id) && !maps.isMapMaBu(id)
                && !maps.isMapMabu2H(id) && !maps.isMapLuyenTap(id)
                && !maps.isMapSieuThanhThuy(id) && !maps.isMapRiengTu(id)
                && !maps.isMapNgucTu(id) && !maps.isMapBossFinal(id)
                && !maps.isMapNguHanhSon(id) && !maps.isMapKimetsuTower(id);
    }

    /** Một mini boss trên toàn map, chỉ xuất hiện từ khu 2. */
    public static boolean isAllowedZone(Zone zone) {
        return isAllowedZone(zone, null);
    }

    public static boolean isAllowedZone(Zone zone, Player self) {
        if (!isAllowedMap(zone) || zone.zoneId < 2) return false;
        for (Zone candidate : zone.map.zones) {
            for (Player boss : candidate.getBosses()) {
                if (boss != null && boss != self && (boss.id == BossID.AN_TROM
                        || boss.id == BossID.BOT_RATI || boss.id == BossID.QUY_SANTA)) {
                    return false;
                }
            }
        }
        return true;
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
                return (int) damage;
            }
            fleeFrom = owner;
            fleeUntil = System.currentTimeMillis() + 5000;
            fleeIfNeeded();
            // Chỉ phản đòn khi bị đánh và không quá một lần mỗi 30 giây.
            if (!this.playerSkill.skills.isEmpty()
                    && Util.canDoWithTime(this.lastTimeStun, 30000)) {
                this.playerSkill.skillSelect = this.playerSkill.skills.get(Util.nextInt(0, this.playerSkill.skills.size() - 1));
                SkillService.gI().useSkill(this, owner, null, -1, null);
                this.lastTimeStun = System.currentTimeMillis();
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

        // CỨ MỖI 20 GIÂY: ĂN TRỘM 1% VÀNG CỦA NHÂN VẬT TRONG MAP
        if (Util.canDoWithTime(this.lastTimeAnTrom, 20000)) {
            for (Player pl : this.zone.getPlayers()) {
                if (pl != null && !pl.isDie() && !pl.isBoss && !pl.isBot && pl.isPl()) {
                    if (pl.inventory.gold > 0) {
                        long stolen = (long) (pl.inventory.gold * 0.01);
                        if (stolen < 1) {
                            stolen = 1;
                        }
                        if (stolen > pl.inventory.gold) {
                            stolen = pl.inventory.gold;
                        }
                        pl.inventory.gold -= stolen;
                        this.goldAnTrom += stolen;
                        Service.gI().sendMoney(pl);
                        Service.gI().sendThongBao(pl, "Bạn bị Boss Ăn Trộm cuỗm mất " + Util.numberToMoney(stolen) + " vàng!");
                        this.chat("Haha! Ta vừa thó được 1% vàng của " + pl.name + " (" + Util.numberToMoney(stolen) + " vàng)!");
                    }
                }
            }
            this.lastTimeAnTrom = System.currentTimeMillis();
        }

        if (fleeIfNeeded()) return;

        // Chỉ đuổi theo người chơi. Chiêu choáng được phản đòn trong injured()
        // khi người chơi thực sự đánh trúng boss.
        if (Util.canDoWithTime(this.lastTimeAttack, 1000) && this.typePk == ConstPlayer.PK_ALL) {
            this.lastTimeAttack = System.currentTimeMillis();
            try {
                Player pl = getNearestRealPlayer();
                if (pl != null && !pl.isDie()) {
                    if (Util.getDistance(this, pl) > 100) {
                        this.moveToPlayer(pl);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private boolean fleeIfNeeded() {
        long now = System.currentTimeMillis();
        if (isDie() || zone == null || fleeFrom == null || fleeFrom.zone != zone
                || fleeFrom.isDie() || now >= fleeUntil) {
            fleeFrom = null;
            return false;
        }
        if (now - lastFleeMove >= 400) {
            lastFleeMove = now;
            int direction = location.x >= fleeFrom.location.x ? 1 : -1;
            int minX = 40;
            int maxX = zone.map.mapWidth - 40;
            if (maxX <= minX) return true;
            int x = Math.max(minX, Math.min(maxX, location.x + direction * 60));
            // Turn back at the edge rather than leaving the map or getting stuck.
            if (x == location.x) {
                x = Math.max(minX, Math.min(maxX, location.x - direction * 60));
            }
            int y = zone.map.yPhysicInTop(x, location.y);
            PlayerService.gI().playerMove(this, x, y > 0 ? y : location.y);
        }
        return true;
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

        // KHI AI PEM CHẾT: TOÀN BỘ VÀNG TRỘM ĐƯỢC SẼ VÀO HÀNH TRANG CỦA NGƯỜI ĐÓ
        long rewardGold = this.goldAnTrom;
        if (rewardGold <= 0) {
            rewardGold = Util.nextInt(1_000_000, 5_000_000); // Tối thiểu 1-5tr vàng khởi điểm
        }

        long canReceive = Math.min(rewardGold, Inventory.LIMIT_GOLD - plKill.inventory.gold);
        if (canReceive > 0) {
            plKill.inventory.gold += canReceive;
            Service.gI().sendMoney(plKill);
            Service.gI().sendThongBao(plKill, "Bạn đã tiêu diệt Boss Ăn Trộm và nhận " + Util.numberToMoney(canReceive) + " vàng vào hành trang!");
        }
        this.goldAnTrom = 0;
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
        this.name = "Ăn Trộm";
        setFixedHp();
        this.nPoint.dameg = 100;
        this.goldAnTrom = 0;
        this.lastTimeStun = 0;
        this.lastTimeAnTrom = System.currentTimeMillis();
        this.joinMap2();
        st = System.currentTimeMillis();
    }

    /** Triệu hồi trực tiếp cạnh người chơi, kể cả khi người chơi đang treo máy. */
    public void summonAt(Zone targetZone, int x, int y) throws Exception {
        try {
            if (!isAllowedZone(targetZone)) {
                throw new IllegalArgumentException("Chỉ gọi boss mini từ khu 2, ở map thường chưa có boss mini");
            }
            this.currentLevel = 0;
            this.initBase();
            this.typePk = ConstPlayer.NON_PK;
            this.name = "Ăn Trộm";
            setFixedHp();
            this.nPoint.dameg = 100;
            this.goldAnTrom = 0;
            this.lastTimeStun = 0;
            this.lastTimeAnTrom = System.currentTimeMillis();
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
        fleeFrom = null;
        fleeUntil = 0;
        lastFleeMove = 0;
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
