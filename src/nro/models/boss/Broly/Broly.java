package nro.models.boss.Broly;

import nro.models.boss.Boss;
import nro.models.boss.BossData;
import nro.models.boss.BossID;
import nro.models.consts.BossStatus;
import static nro.models.consts.BossType.BROLY;
import nro.models.consts.ConstPlayer;
import nro.models.map.Zone;
import nro.models.player.Player;
import nro.models.services.SkillService;
import nro.models.map.service.ChangeMapService;
import nro.models.skill.Skill;
import nro.models.utils.SkillUtil;
import nro.models.map.ItemMap;
import nro.models.services.Service;
import nro.models.utils.Util;

public class Broly extends Boss {
    private boolean summonSuperOnLeave;

    public Broly() throws Exception {

        super(BROLY, BossID.BROLY, new BossData(
                "Broly", //name
                ConstPlayer.XAYDA, //gender
                // Client: Spine/Skins/10143/skin - Broly dạng thường.
                new short[]{3072, 0, 0, -1, -1, -1},
                100, //dame
                new int[]{1_000_000},
                new int[]{5, 6, 10, 19, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38},
                new int[][]{
                    {0, 3, 550},
                    {1, 3, 1600},
                    {8, 3, 27500}
                },
                new String[]{}, //text chat 1
                new String[]{"|-1|Haha! ta sẽ giết hết các ngươi",
                    "|-1|Sức mạnh của ta là tuyệt đối",
                    "|-1|Vào hết đây!!!",}, //text chat 2
                new String[]{"|-1|Các ngươi giỏi lắm. Ta sẽ quay lại."}, //text chat 3
                600//type appear
        ));
    }

    @Override
    public void active() {
        super.active();
    }

    @Override
    public void joinMap() {
        this.name = "Broly " + Util.nextInt(10, 100);
        this.nPoint.hpMax = Util.nextInt(50_000, 1_000_000);
        this.nPoint.hp = this.nPoint.hpMax;
        this.nPoint.dame = this.nPoint.hpMax / 100;
        this.nPoint.crit = Util.nextInt(50);
        this.joinMap2();
        st = System.currentTimeMillis();
    }

    public void joinMap2() {
        if (this.zone == null) {
            if (this.parentBoss != null) {
                this.zone = parentBoss.zone;
            } else if (this.lastZone == null) {
                this.zone = getMapJoin();
            } else {
                this.zone = this.lastZone;
            }
        }
        if (this.zone != null) {
            try {

                int zoneid = Util.nextInt(2, this.zone.map.zones.size() - 1);

                while (zoneid < this.zone.map.zones.size() && this.zone.map.zones.get(zoneid).getBosses().size() > 0) {
                    zoneid++;
                }

                if (zoneid < this.zone.map.zones.size()) {
                    this.zone = this.zone.map.zones.get(zoneid);
                } else {
                    if (this.id == BossID.BROLY) {
                        this.changeStatus(BossStatus.DIE);
                        return;
                    }
                    this.zone = this.zone.map.zones.get(Util.nextInt(2, this.zone.map.zones.size() - 1));
                }

                if (this.zone.zoneId < 2) {
                    this.leaveMap();
                }

                ChangeMapService.gI().changeMap(this, this.zone, -1, -1);
                this.changeStatus(BossStatus.CHAT_S);
            } catch (Exception e) {
                this.changeStatus(BossStatus.REST);
            }
        } else {
            this.changeStatus(BossStatus.RESPAWN);
        }
    }

    private long st;

    @Override
    public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            if (!piercing && Util.isTrue(this.nPoint.tlNeDon, 1000)) {
                this.chat("Xí hụt");
                return 0;
            }
            damage = this.nPoint.subDameInjureWithDeff(damage);
            // Mỗi đòn của người chơi chỉ lấy tối đa 1% HP boss.
            long percentDamage = Math.max(1L, this.nPoint.hpMax / 100L);
            if (!piercing && damage > percentDamage) {
                damage = percentDamage;
            }
            this.nPoint.subHP(damage);
            if (isDie()) {
                this.setDie(plAtt);
                die(plAtt);
            }
            return (int) damage;
        } else {
            return 0;
        }
    }

    @Override
    public void attack() {
        super.attack();
    }

    @Override
    public void die(Player plKill) {
        // Broly thường có 30% cơ hội hóa thành Super Broly sau khi bị hạ.
        this.summonSuperOnLeave = Util.isTrue(30, 100);
        this.changeStatus(BossStatus.DIE);
    }

    @Override
    public void leaveMap() {
        Zone zone = this.zone;
        int x = this.location.x;
        int y = this.location.y;
        ChangeMapService.gI().exitMap(this);
        if (this.summonSuperOnLeave && zone != null) {
            try {
                new SuperBroly(zone, x, y);
            } catch (Exception ex) {
            }
        }
        this.summonSuperOnLeave = false;
        this.lastZone = null;
        this.lastTimeRest = System.currentTimeMillis();
        this.changeStatus(BossStatus.REST);
    }
}
