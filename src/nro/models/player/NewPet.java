package nro.models.player;

import nro.models.map.service.MapService;
import nro.models.mob.Mob;
import nro.models.network.Message;
import nro.models.services.PlayerService;
import nro.models.services.Service;
import nro.models.services.SkillService;
import nro.models.skill.Skill;
import nro.models.map.service.ChangeMapService;
import nro.models.utils.Util;

/**
 *
 * @author By Mr Blue
 * 
 */

public class NewPet extends Player {

    public Player master;
    public short body;
    public short leg;
    public static int idb = -310720020;

    public NewPet(Player master, short h, short b, short l) {
        this.master = master;
        this.isNewPet = true;
        this.isNewPet1 = true;
        this.id = idb;
        idb--;
        this.head = h;
        this.body = b;
        this.leg = l;
    }

    @Override
    public short getHead() {
        return head;
    }

    @Override
    public short getBody() {
        return body;
    }

    @Override
    public short getLeg() {
        return leg;
    }

    public void joinMapMaster() {
        if (master == null || master.zone == null) {
            return;
        }
        this.location.x = master.location.x + Util.nextInt(-10, 10);
        this.location.y = master.location.y;
        if (isPl()) {
            this.dispose();
            return;
        }
        ChangeMapService.gI().goToMap(this, master.zone);
        this.zone.load_Me_To_Another(this);
    }

    private long lastTimeMoveIdle;
    private int timeMoveIdle;
    public boolean idle;
    private byte spiritStatus = Pet.FOLLOW;
    private long lastSpiritAttack;

    /** Match the ordinary disciple's first-attack skill cooldown. */
    private long spiritAttackCooldown() {
        if (master == null || master.pet == null || master.pet.playerSkill == null
                || master.pet.playerSkill.skills == null || master.pet.playerSkill.skills.isEmpty()) return 950;
        Skill first = master.pet.playerSkill.skills.get(0);
        return first == null || first.coolDown <= 0 ? 950 : Math.max(1, first.coolDown - 50);
    }

    private boolean isSpiritFollower() {
        return master != null && master.spiritPet == this && master.pet != null;
    }

    public byte getSpiritStatus() {
        return spiritStatus;
    }

    public void changeSpiritStatus(byte status) {
        if (!isSpiritFollower() || status < Pet.FOLLOW || status > Pet.GOHOME) return;
        spiritStatus = status;
        if (status == Pet.GOHOME) {
            if (zone != null) ChangeMapService.gI().exitMap(this);
        } else if (master.zone != null && zone != master.zone) {
            joinMapMaster();
        }
        Service.gI().showInfoPet(master);
    }

    private void updateSpirit() {
        if (spiritStatus == Pet.GOHOME || zone == null || master.pet.isDie()) return;
        if (spiritStatus == Pet.FOLLOW) {
            followMaster(70);
            return;
        }
        Mob target = null;
        int best = Integer.MAX_VALUE;
        for (Mob mob : zone.mobs) {
            if (mob == null || mob.isDie() || mob.zone != zone) continue;
            if (Util.getDistance(master, mob) > (spiritStatus == Pet.PROTECT ? 170 : 550)) continue;
            int distance = Util.getDistance(this, mob);
            if (distance < best) { best = distance; target = mob; }
        }
        Player bossTarget = null;
        for (Player boss : zone.getBosses()) {
            if (boss == null || boss.isDie() || boss.zone != zone
                    || !SkillService.gI().canAttackPlayer(master, boss)) continue;
            if (Util.getDistance(master, boss) > (spiritStatus == Pet.PROTECT ? 170 : 550)) continue;
            int distance = Util.getDistance(this, boss);
            if (distance < best) { best = distance; target = null; bossTarget = boss; }
        }
        if (target == null && bossTarget == null) {
            followMaster(70);
            return;
        }
        int targetX = target != null ? target.location.x : bossTarget.location.x;
        int targetY = target != null ? target.location.y : bossTarget.location.y;
        if (best > 45) {
            // Một lần cập nhật vị trí tới ngay cạnh mục tiêu, rồi mới phát đòn đánh.
            PlayerService.gI().playerMove(this, targetX + (this.location.x < targetX ? -30 : 30), targetY);
        }
        long now = System.currentTimeMillis();
        if (master.pet.nPoint == null || now - lastSpiritAttack < spiritAttackCooldown()) return;
        lastSpiritAttack = now;
        Message attack = null;
        try {
            if (target != null) {
                attack = new Message(54);
                attack.writer().writeInt((int) id);
                attack.writer().writeByte(0);
                attack.writer().writeByte(target.id);
                Service.gI().sendMessAllPlayerInMap(zone, attack);
                target.injured(master, Math.max(1, master.pet.nPoint.dame / 2L), true);
            } else {
                int hit = bossTarget.injured(master, Math.max(1, master.pet.nPoint.dame / 2L), false, false);
                attack = new Message(-60);
                attack.writer().writeInt((int) id);
                attack.writer().writeByte(0);
                attack.writer().writeByte(1);
                attack.writer().writeInt((int) bossTarget.id);
                attack.writer().writeByte(1);
                attack.writer().writeByte(0);
                attack.writer().writeInt(hit);
                attack.writer().writeBoolean(bossTarget.isDie());
                attack.writer().writeBoolean(false);
                Service.gI().sendMessAllPlayerInMap(zone, attack);
            }
        } catch (Exception ex) {
            nro.models.utils.Logger.logException(NewPet.class, ex);
        } finally {
            if (attack != null) attack.cleanup();
        }
    }

    private void moveIdle() {
        if (idle && Util.canDoWithTime(lastTimeMoveIdle, timeMoveIdle)) {
            int dir = this.location.x - master.location.x <= 0 ? -1 : 1;
            PlayerService.gI().playerMove(this, master.location.x
                    + Util.nextInt(dir == -1 ? 30 : -50, dir == -1 ? 50 : 30), master.location.y);
            lastTimeMoveIdle = System.currentTimeMillis();
            timeMoveIdle = Util.nextInt(5000, 8000);
        }
    }

    @Override
    public void update() {
        if (isSpiritFollower() && spiritStatus == Pet.GOHOME) return;
        super.update();
        if (this.isDie()) {
            Service.gI().hsChar(this, nPoint.hpMax, nPoint.mpMax);
        }
        if (master != null && master.zone != null && (this.zone == null || this.zone != master.zone) && !MapService.gI().isMapOffline(master.zone.map.mapId)) {
            joinMapMaster();
        }
        if (master != null && master.isDie()) {
            return;
        }
        if (isSpiritFollower()) {
            updateSpirit();
            return;
        }
        moveIdle();
    }

    public void followMaster() {
        if (isSpiritFollower()) {
            if (spiritStatus != Pet.GOHOME && (spiritStatus != Pet.ATTACK || Util.getDistance(this, master) > 400))
                followMaster(70);
            return;
        }
        followMaster(50);
    }

    private void followMaster(int dis) {
        int mX = master.location.x;
        int mY = master.location.y;
        int disX = this.location.x - mX;
        if (Math.sqrt(Math.pow(mX - this.location.x, 2) + Math.pow(mY - this.location.y, 2)) >= dis) {
            if (disX < 0) {
                this.location.x = mX - Util.nextInt(0, dis);
            } else {
                this.location.x = mX + Util.nextInt(0, dis);
            }
            this.location.y = mY;
            PlayerService.gI().playerMove(this, this.location.x, this.location.y);
        }
    }

    @Override
    public void dispose() {
        if (zone !=null) {
            nro.models.services.AnimePetAuraService.remove(this);
            ChangeMapService.gI().exitMap(this);
        }
        this.master = null;
        super.dispose();
    }
}
