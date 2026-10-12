package nro.models.boss.nhan_ban;

import nro.models.boss.Boss;
import nro.models.boss.BossData;
import nro.models.boss.BossID;
import nro.models.consts.BossStatus;
import nro.models.boss.BossesData;
import static nro.models.consts.BossType.PHOBAN;
import nro.models.boss.Boss_Manager.OtherBossManager;
import nro.models.consts.ConstPlayer;
import nro.models.map.ItemMap;
import nro.models.player.Player;
import nro.models.services.EffectSkillService;
import nro.models.services.PlayerService;
import nro.models.services.Service;
import nro.models.services.SkillService;
import nro.models.utils.SkillUtil;
import nro.models.utils.Util;

public class BongMaTaAc extends Boss {

    private int timeLive;
    private long lastUpdate = System.currentTimeMillis();
    private Player playerAtt;

    public BongMaTaAc(Player player) throws Exception {
        super(PHOBAN, Util.createIdBossClone((int) player.id), BossesData.BONG_MA_TA_AC);
        this.playerAtt = player;
        
        this.currentLevel = 0;
        this.initBase();
        this.gender = player.gender;

        this.zone = player.zone;
        this.location.x = player.location.x + 50;
        this.location.y = player.location.y;
        
        this.timeLive = 300; // 5 minutes (300 seconds)
        this.typePk = ConstPlayer.PK_ALL;
        
        this.changeStatus(BossStatus.JOIN_MAP);
    }

    @Override
    protected void initSkill() {
        int skillId = nro.models.skill.Skill.GALICK;
        if (this.playerAtt != null) {
            if (this.playerAtt.gender == 0) {
                skillId = nro.models.skill.Skill.KAMEJOKO;
            } else if (this.playerAtt.gender == 1) {
                skillId = nro.models.skill.Skill.MASENKO;
            }
        }
        nro.models.skill.Skill skill = nro.models.utils.SkillUtil.createSkill(skillId, 7);
        skill.coolDown = 1000;
        applyBossSkillCooldown(skill);
        this.playerSkill.skills.clear();
        this.playerSkill.skills.add(skill);
    }
    
    @Override
    protected void resetBase() {
        super.resetBase();
        if (this.playerAtt != null) {
            long maxHp = this.playerAtt.nPoint.hpMax;
            if (maxHp > 2000000000) maxHp = 2000000000;
            
            this.nPoint.hpMax = (int) maxHp;
            this.nPoint.hpg = (int) maxHp;
            this.nPoint.hp = (int) maxHp;
            
            this.nPoint.mpMax = this.playerAtt.nPoint.mpMax;
            this.nPoint.mpg = this.playerAtt.nPoint.mpMax;
            this.nPoint.mp = this.playerAtt.nPoint.mpMax;
            
            this.nPoint.dame = this.playerAtt.nPoint.dame;
            this.nPoint.dameg = this.playerAtt.nPoint.dame;
            
            this.nPoint.def = this.playerAtt.nPoint.def;
            this.nPoint.defg = this.playerAtt.nPoint.def;
            this.nPoint.crit = this.playerAtt.nPoint.crit;
            this.nPoint.critg = this.playerAtt.nPoint.crit;
            
            this.nPoint.calPoint();
            this.nPoint.hp = this.nPoint.hpMax;
        }
    }

    @Override
    public short getHead() {
        if (playerAtt != null) {
            return playerAtt.getHead();
        }
        return super.getHead();
    }

    @Override
    public short getBody() {
        if (playerAtt != null) {
            return playerAtt.getBody();
        }
        return super.getBody();
    }

    @Override
    public short getLeg() {
        if (playerAtt != null) {
            return playerAtt.getLeg();
        }
        return super.getLeg();
    }

    @Override
    public short getAuraId() {
        return -1;
    }

    @Override
    public byte getAura() {
        return -1;
    }
    
    @Override
    public byte getEffFront() {
        return -1;
    }

    @Override
    public void joinMap() {
        if (this.zone != null) {
            nro.models.map.service.ChangeMapService.gI().changeMap(this, this.zone, this.location.x, this.location.y);
            nro.models.services.Service.gI().sendFlagBag(this);
            this.notifyJoinMap();
            this.changeStatus(nro.models.consts.BossStatus.CHAT_S);
        }
    }
    
    @Override
    public void leaveMap() {
        try {
            super.leaveMap();
            OtherBossManager.gI().removeBoss(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void attack() {
        try {
            if (playerAtt != null && playerAtt.location != null && playerAtt.zone != null && this.zone != null && this.zone.equals(playerAtt.zone)) {
                if (this.isDie()) {
                    return;
                }
                this.playerSkill.skillSelect = this.playerSkill.skills.get(Util.nextInt(0, this.playerSkill.skills.size() - 1));
                if (Util.getDistance(this, playerAtt) <= this.getRangeCanAttackWithSkillSelect()) {
                    if (Util.isTrue(15, 100) && SkillUtil.isUseSkillChuong(this)) {
                        moveTo(playerAtt.location.x + (Util.getOne(-1, 1) * Util.nextInt(20, 80)), Util.nextInt(10) % 2 == 0 ? playerAtt.location.y : playerAtt.location.y - Util.nextInt(0, 50));
                    }
                    SkillService.gI().useSkill(this, playerAtt, null, -1, null);
                    checkPlayerDie(playerAtt);
                } else {
                    moveToPlayer(playerAtt);
                }
            } else {
                this.leaveMap();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void update() {
        if (this.playerAtt == null) {
            return;
        }
        try {
            super.update();
            if (Util.canDoWithTime(lastUpdate, 1000)) {
                lastUpdate = System.currentTimeMillis();
                if (timeLive > 0) {
                    timeLive--;
                } else {
                    leaveMap();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void die(Player plKill) {
        if (plKill != null && plKill.id == this.playerAtt.id) {
            if (plKill.playerTask != null && plKill.playerTask.taskMain != null && plKill.playerTask.taskMain.id == 35) {
                if (plKill.playerTask.taskMain.index == 3) {
                    plKill.playerTask.taskMain.subTasks.get(plKill.playerTask.taskMain.index).count = 1;
                    nro.models.services.TaskService.gI().sendUpdateCountSubTask(plKill);
                    nro.models.services.TaskService.gI().doneTask(plKill, nro.models.consts.ConstTask.TASK_35_3);
                }
                if (plKill.playerTask.taskMain.index >= 3 && plKill.playerTask.taskMain.index <= 6) {
                    if (nro.models.utils.Util.isTrue(100, 100)) {
                        nro.models.map.ItemMap it = new nro.models.map.ItemMap(this.zone, 2018, 1, this.location.x, this.zone.map.yPhysicInTop(this.location.x, this.location.y), plKill.id);
                        nro.models.services.Service.gI().dropItemMap(this.zone, it);
                        nro.models.services.Service.gI().sendThongBao(plKill, "Bóng Ma Tà Ác đã làm rơi Tinh Khói Đậm Đặc!");
                    } else {
                        nro.models.services.Service.gI().sendThongBao(plKill, "Bóng Ma Tà Ác đã bị tiêu diệt nhưng không rơi ra gì cả...");
                    }
                }
            }
        }
        super.die(plKill);
    }
}
