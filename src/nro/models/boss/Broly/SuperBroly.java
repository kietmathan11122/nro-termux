package nro.models.boss.Broly;

import nro.models.boss.Boss;
import nro.models.boss.Boss_Manager.BrolyManager;
import nro.models.boss.BossData;
import nro.models.boss.BossID;
import nro.models.consts.BossStatus;
import static nro.models.consts.BossType.BROLY;
import nro.models.consts.ConstPlayer;
import nro.models.map.Zone;
import nro.models.player.Player;
import nro.models.services.PetService;
import nro.models.services.SkillService;
import nro.models.map.service.ChangeMapService;
import nro.models.skill.Skill;
import nro.models.services.PlayerService;
import nro.models.utils.SkillUtil;
import nro.models.map.ItemMap;
import nro.models.services.Service;
import nro.models.utils.Util;

public class SuperBroly extends Boss {

    public SuperBroly(Zone zone, int x, int y) throws Exception {

        super(BROLY, BossID.SUPER_BROLY, false, false, new BossData(
                "Super Broly", //name
                ConstPlayer.XAYDA, //gender
                // Client: Spine/Skins/10172/skin - Super Broly tóc xanh.
                new short[]{3101, 0, 0, -1, -1, -1},
                100, //dame
                new int[]{16_000_000},
                new int[]{5, 6, 10, 19, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38},
                new int[][]{
                    {0, 3, 475},
                    {1, 3, 1400},
                    {8, 3, 25000}
                },
                new String[]{}, //text chat 1
                new String[]{"|-1|Haha! ta sẽ giết hết các ngươi",
                    "|-1|Sức mạnh của ta là tuyệt đối",
                    "|-1|Vào hết đây!!!",}, //text chat 2
                new String[]{"|-1|Các ngươi giỏi lắm. Ta sẽ quay lại."}, //text chat 3
                1//type appear
        ));
        this.zone = zone;
        this.location.x = x;
        this.location.y = y;
    }

    @Override
    public void reward(Player plKill) {
        if (plKill instanceof nro.models.player.Pet) {
            plKill = ((nro.models.player.Pet) plKill).master;
        }
        if (plKill != null && plKill.isPl() && plKill.pet == null) {
            PetService.gI().createNormalPet(plKill);
            Service.gI().sendThongBao(plKill, "Bạn đã hạ Super Broly và nhận được đệ tử!");
        }
    }

    @Override
    public void active() {
        super.active();
    }

    @Override
    public void joinMap() {
        this.name = "Super Broly " + Util.nextInt(10, 100);
        this.nPoint.hpMax = Util.nextInt(1_000_000, 16_000_000);
        this.nPoint.hp = this.nPoint.hpMax;
        this.nPoint.dame = this.nPoint.hpMax / 100;
        this.nPoint.crit = Util.nextInt(50);
        if (this.zone != null) {
            ChangeMapService.gI().changeMap(this, this.zone, this.location.x, this.location.y);
            this.changeStatus(BossStatus.CHAT_S);
            this.notifyJoinMap();
        } else {
            super.joinMap();
        }
        st = System.currentTimeMillis();
    }

    private long st;

    @Override
    public void autoLeaveMap() {
        if (Util.canDoWithTime(st, 300_000)) {
            this.leaveMap();
        }
        if (this.zone != null && this.zone.getNumOfPlayers() > 0) {
            st = System.currentTimeMillis();
        }
    }

    @Override
    public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            if (!piercing && Util.isTrue(this.nPoint.tlNeDon, 1000)) {
                this.chat("Xí hụt");
                return 0;
            }
            damage = this.nPoint.subDameInjureWithDeff(damage);
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

    private long lastTimeAttack;

    @Override
    public void attack() {
        super.attack();
    }

    @Override
    public void leaveMap() {
        ChangeMapService.gI().exitMap(this);
        if (this.pet != null) {
            ChangeMapService.gI().exitMap(this.pet);
        }
        this.lastZone = null;
        this.lastTimeRest = System.currentTimeMillis();
        this.changeStatus(BossStatus.REST);
        BrolyManager.gI().removeBoss(this);
        this.dispose();
    }
}
