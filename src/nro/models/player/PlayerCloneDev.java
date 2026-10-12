package nro.models.player;

import nro.models.item.Item;
import nro.models.mob.Mob;
import nro.models.skill.PlayerSkill;
import nro.models.skill.Skill;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.map.service.MapService;
import nro.models.services.PlayerService;
import nro.models.services.Service;
import nro.models.services.SkillService;
import nro.models.map.service.ChangeMapService;
import nro.models.utils.SkillUtil;
import nro.models.utils.Util;
import java.util.ArrayList;

public class PlayerCloneDev extends Player {

    public Player master;
    private long lastSpawnTime = 0;
    private long lastTimeDie = 0;
    public int percentPhanThan = 10;
    public int cloneIndex = 1;
    public long lifeTime = 30_000;
    private boolean isChatBye = false;
    private long lastTimeAutoRole = 0;
    private boolean hasHealMasterWhenDisappear = false;
    private boolean usedCellCloneTuSat = false;
    private boolean waitingCellCloneTuSatDisappear = false;
    private long timeCellCloneTuSat = 0;
    private long timeSummonChat = 0;
    private String summonChatText = null;
    private static final long CELL_TU_SAT_DELAY = 1_200L;

    private static final int MASTER_CAI_TRANG_ID = 528;
    private static final int CLONE_CAI_TRANG_ID = 549;
    private static final int CAI_TRANG_SLOT = 5;
    private int lastMasterCaiTrangId = -2;
    private long lastTimeSyncCaiTrang = 0;
    private byte lastMasterTransformationLevel = -1;

    public PlayerCloneDev(Player master) {
        this(master, 1, 10);
    }

    public PlayerCloneDev(Player master, int cloneIndex, int percentPhanThan) {
        this(master, cloneIndex, percentPhanThan, 30_000);
    }

    public PlayerCloneDev(Player master, int cloneIndex, int percentPhanThan, long lifeTime) {
        super();
        this.master = master;
        this.cloneIndex = cloneIndex;
        this.percentPhanThan = percentPhanThan;
        this.lifeTime = lifeTime;
        this.isClonePhanThan = true;
        this.id = master.id - 10_000 - cloneIndex;
        this.gender = master.gender;
        updateCloneNameByMasterCaiTrang();
        this.nPoint.hpg = master.nPoint.hpg;
        this.nPoint.mpg = master.nPoint.mpg;
        this.nPoint.dameg = master.nPoint.dameg;
        this.nPoint.defg = master.nPoint.defg;
        this.nPoint.critg = master.nPoint.critg;
        this.nPoint.power = master.nPoint.power;
        this.nPoint.tiemNang = master.nPoint.tiemNang;
        this.nPoint.stamina = master.nPoint.stamina;
        this.nPoint.maxStamina = master.nPoint.maxStamina;
        this.inventory = new Inventory();
        syncCaiTrangFromMaster(false);
        this.playerSkill = new PlayerSkill(this);
        this.cloneSkill();
        syncTransformationFromMaster();
        this.nPoint.calPoint();
        this.nPoint.setFullHpMp();
        this.lastSpawnTime = System.currentTimeMillis();
    }

    private void cloneSkill() {
        for (Skill skill : master.playerSkill.skills) {
            if (skill == null || skill.template == null) {
                continue;
            }
            Skill cloneSkill = new Skill(skill);
            this.playerSkill.skills.add(cloneSkill);
        }
    }

    public void syncCaiTrangFromMaster(boolean notifyMap) {
        if (master == null || master.inventory == null || master.inventory.itemsBody == null) {
            return;
        }
        int masterCaiTrangId = getMasterCaiTrangId();
        if (!notifyMap && masterCaiTrangId == lastMasterCaiTrangId && this.inventory != null
                && this.inventory.itemsBody != null && !this.inventory.itemsBody.isEmpty()) {
            return;
        }

        if (this.inventory == null) {
            this.inventory = new Inventory();
        }

        this.gender = master.gender;
        this.head = master.head;

        this.inventory.itemsBody = InventoryService.gI().copyItemsBody(master);

        if (masterCaiTrangId == MASTER_CAI_TRANG_ID
                && this.inventory.itemsBody != null
                && this.inventory.itemsBody.size() > CAI_TRANG_SLOT) {
            Item cloneCaiTrang = ItemService.gI().createNewItem((short) CLONE_CAI_TRANG_ID);
            this.inventory.itemsBody.set(CAI_TRANG_SLOT, cloneCaiTrang);
        }

        updateCloneNameByMasterCaiTrang(masterCaiTrangId);
        lastMasterCaiTrangId = masterCaiTrangId;
        if (notifyMap && this.zone != null) {
            Service.gI().Send_Caitrang(this);
        }
    }

    private void updateCloneNameByMasterCaiTrang() {
        updateCloneNameByMasterCaiTrang(getMasterCaiTrangId());
    }

    private void updateCloneNameByMasterCaiTrang(int masterCaiTrangId) {
        if (masterCaiTrangId == MASTER_CAI_TRANG_ID) {
            this.name = "Xên Con " + cloneIndex;
        } else if (master != null) {
            this.name = "[Clone " + cloneIndex + "] " + master.name;
        }
    }

    private int getMasterCaiTrangId() {
        try {
            if (master != null && master.inventory != null && master.inventory.itemsBody != null
                    && master.inventory.itemsBody.size() > CAI_TRANG_SLOT) {
                Item item = master.inventory.itemsBody.get(CAI_TRANG_SLOT);
                if (item != null && item.isNotNullItem() && item.template != null) {
                    return item.template.id;
                }
            }
        } catch (Exception ignored) {
        }
        return -1;
    }

    public void attackWithMaster(Player plAtt, Mob mAtt) {
        if (plAtt != null) {
            if (SkillUtil.isUseSkillDam(this)) {
                PlayerService.gI().playerMove(this, plAtt.location.x + getCloneOffsetX() + Util.nextInt(-15, 15), plAtt.location.y);
            }
            SkillService.gI().useSkillAttack(this, plAtt, null);
        } else if (mAtt != null) {
            if (SkillUtil.isUseSkillDam(this)) {
                PlayerService.gI().playerMove(this, mAtt.location.x + getCloneOffsetX() + Util.nextInt(-15, 15), mAtt.location.y);
            }
            SkillService.gI().useSkillAttack(this, null, mAtt);
        }
    }

    public void assistMasterAttack(Player plTarget, Mob mobTarget) {
        if (master == null || this.zone == null || this.isDie()) {
            return;
        }
        selectNormalAttackSkill();
        if (this.playerSkill == null || this.playerSkill.skillSelect == null
                || this.playerSkill.skillSelect.template == null) {
            followMaster();
            return;
        }

        if (plTarget != null && plTarget.location != null && plTarget.zone == master.zone
                && isTargetNearMaster(plTarget.location.x, plTarget.location.y, 260)) {
            attackWithMaster(plTarget, null);
            return;
        }

        if (mobTarget != null && mobTarget.location != null && !mobTarget.isDie()
                && isTargetNearMaster(mobTarget.location.x, mobTarget.location.y, 260)) {
            attackWithMaster(null, mobTarget);
            return;
        }

        followMaster();
    }

    @Override
    public void update() {
        super.update();
        if (isDie() && canRespawn()) {
            Service.gI().hsChar(this, nPoint.hpMax, nPoint.mpMax);
        }
        if (master != null && (this.zone == null || this.zone != master.zone)) {
            joinMapMaster();
        }

        if (Util.canDoWithTime(lastTimeSyncCaiTrang, 500)) {
            lastTimeSyncCaiTrang = System.currentTimeMillis();
            syncCaiTrangFromMaster(true);
            syncTransformationFromMaster();
        }

        autoRoleAction();

        if (summonChatText != null && Util.canDoWithTime(timeSummonChat, 0)) {
            Service.gI().chat(this, summonChatText);
            summonChatText = null;
        }

        if (waitingCellCloneTuSatDisappear) {
            if (Util.canDoWithTime(timeCellCloneTuSat, CELL_TU_SAT_DELAY)) {
                finishDisposeAfterCellTuSat();
            }
            return;
        }

        if (!isChatBye
                && !isDie()
                && lifeTime > 5_000
                && Util.canDoWithTime(lastSpawnTime, lifeTime - 5_000)) {
            Service.gI().chat(this, getDisappearText());
            isChatBye = true;
        }

        if (Util.canDoWithTime(lastSpawnTime, lifeTime) || (lastTimeDie != 0 && Util.canDoWithTime(lastTimeDie, 3_000))) {
            startCellTuSatThenDisappear();
        }
    }

    private void autoRoleAction() {
        if (master == null || this.zone == null || this.isDie()) {
            return;
        }
        if (!Util.canDoWithTime(lastTimeAutoRole, 900)) {
            return;
        }
        lastTimeAutoRole = System.currentTimeMillis();
        selectNormalAttackSkill();
        if (this.playerSkill == null || this.playerSkill.skillSelect == null || this.playerSkill.skillSelect.template == null) {
            followMaster();
            return;
        }

        switch (cloneIndex) {
            case 1:
                attackNearestMobOrFollow();
                break;
            case 2:
                attackBossOrNearestMob();
                break;
            case 3:
                protectMaster();
                break;
            default:
                if (cloneIndex % 3 == 1) {
                    attackNearestMobOrFollow();
                } else if (cloneIndex % 3 == 2) {
                    attackBossOrNearestMob();
                } else {
                    protectMaster();
                }
                break;
        }
    }

    private void syncTransformationFromMaster() {
        if (master == null || master.effectSkill == null || this.effectSkill == null) {
            return;
        }
        byte masterLevel = master.effectSkill.isTransformation
                ? master.effectSkill.transformationLevel : 0;
        if (masterLevel == lastMasterTransformationLevel) {
            return;
        }
        this.effectSkill.isTransformation = masterLevel > 0;
        this.effectSkill.transformationLevel = masterLevel;
        lastMasterTransformationLevel = masterLevel;
        if (this.nPoint != null && this.nPoint.hpMax > 0) {
            this.nPoint.calPoint();
            Service.gI().point(this);
        }
    }

    private void selectNormalAttackSkill() {
        if (this.playerSkill == null || this.playerSkill.skills == null || this.playerSkill.skills.isEmpty()) {
            return;
        }
        int[] normalSkillIds = {Skill.DRAGON, Skill.DEMON, Skill.GALICK};
        for (int id : normalSkillIds) {
            Skill skill = this.playerSkill.getSkillbyId(id);
            if (skill != null && skill.template != null && skill.template.type == 1) {
                this.playerSkill.skillSelect = skill;
                return;
            }
        }
        for (Skill skill : this.playerSkill.skills) {
            if (skill != null && skill.template != null && skill.template.type == 1
                    && (skill.template.id == Skill.DRAGON
                    || skill.template.id == Skill.DEMON
                    || skill.template.id == Skill.GALICK
                    || skill.template.id == Skill.LIEN_HOAN)) {
                this.playerSkill.skillSelect = skill;
                return;
            }
        }
    }

    private boolean isTargetNearMaster(int x, int y, int range) {
        return master != null && master.location != null && Util.getDistance(master.location.x, master.location.y, x, y) <= range;
    }

    private void attackNearestMobOrFollow() {
        Mob mob = getNearestMob(master.location.x, master.location.y, 180);
        if (mob != null) {
            attackWithMaster(null, mob);
        } else {
            followMaster();
        }
    }

    private void attackBossOrNearestMob() {
        Player boss = getNearestBossToMaster(520);
        if (boss != null) {
            attackWithMaster(boss, null);
            return;
        }
        attackNearestMobOrFollow();
    }

    private void protectMaster() {
        followMaster();
        Player bossNearMaster = getNearestBossToMaster(420);
        if (bossNearMaster != null) {
            attackWithMaster(bossNearMaster, null);
            return;
        }
        Mob mobNearMaster = getNearestMob(master.location.x, master.location.y, 330);
        if (mobNearMaster != null) {
            attackWithMaster(null, mobNearMaster);
        }
    }

    private Mob getNearestMob(int x, int y, int range) {
        if (this.zone == null || this.zone.mobs == null) {
            return null;
        }
        Mob result = null;
        int minDis = range;
        for (Mob mob : this.zone.mobs) {
            if (mob == null || mob.isDie() || mob.location == null) {
                continue;
            }
            int dis = Util.getDistance(x, y, mob.location.x, mob.location.y);
            if (dis < minDis) {
                minDis = dis;
                result = mob;
            }
        }
        return result;
    }

    private Player getNearestBoss(int range) {
        if (this.zone == null || this.zone.getBosses() == null) {
            return null;
        }
        Player result = null;
        int minDis = range;
        for (Player boss : this.zone.getBosses()) {
            if (boss == null || boss.isDie() || boss.location == null) {
                continue;
            }
            int dis = Util.getDistance(this, boss);
            if (dis < minDis) {
                minDis = dis;
                result = boss;
            }
        }
        return result;
    }

    private Player getNearestBossToMaster(int range) {
        if (master == null || this.zone == null || this.zone.getBosses() == null) {
            return null;
        }
        Player result = null;
        int minDis = range;
        for (Player boss : this.zone.getBosses()) {
            if (boss == null || boss.isDie() || boss.location == null) {
                continue;
            }
            int dis = Util.getDistance(master, boss);
            if (dis < minDis) {
                minDis = dis;
                result = boss;
            }
        }
        return result;
    }

    @Override
    public void setDie(Player plAtt) {
        if (isCellCloneMaster528() && !usedCellCloneTuSat) {
            useCellCloneTuSatIfNeed();
        }
        super.setDie(plAtt);
        lastTimeDie = System.currentTimeMillis();
        if (isCellCloneMaster528()) {
            waitingCellCloneTuSatDisappear = true;
            timeCellCloneTuSat = System.currentTimeMillis();
        }
    }

    private boolean canRespawn() {
        return lastTimeDie == 0;
    }

    public String getRoleText() {
        switch (cloneIndex) {
            case 1:
                return "Tôi sẽ farm quái gần nhất!";
            case 2:
                return "Boss để tôi lo!";
            case 3:
                return "Tôi sẽ bảo vệ cậu!";
            default:
                if (cloneIndex % 3 == 1) {
                    return "Tôi hỗ trợ farm quái!";
                } else if (cloneIndex % 3 == 2) {
                    return "Tôi ưu tiên săn boss!";
                }
                return "Tôi đứng hộ vệ!";
        }
    }

    public String getSummonXenChildText() {
        String[] texts = {
            "Tuân lệnh phụ thân!",
            "Con sẽ xử lý chúng!",
            "Không ai thoát được đâu!",
            "Vì Cell đại nhân!",
            "Mục tiêu đã xác định!",
            "Hãy để con ra tay!",
            "Kẻ địch sẽ bị nghiền nát!",
            "Con đã chờ lệnh từ lâu!",
            "Đã rõ, phụ thân!",
            "Hãy chứng kiến sức mạnh của Cell!"
        };
        return texts[Util.nextInt(0, texts.length - 1)];
    }

    public void scheduleSummonChat(long delayMillis) {
        this.summonChatText = getMasterCaiTrangId() == MASTER_CAI_TRANG_ID ? getSummonXenChildText() : getRoleText();
        this.timeSummonChat = System.currentTimeMillis() + Math.max(0, delayMillis);
    }

    public String getDisappearText() {
        if (getMasterCaiTrangId() == MASTER_CAI_TRANG_ID) {
            String[] texts = {
                "Con xin cáo lui!",
                "Hẹn gặp lại phụ thân!",
                "Nhiệm vụ hoàn thành!",
                "Cell đại nhân vạn tuế!"
            };
            return texts[Util.nextInt(0, texts.length - 1)];
        }
        return "Tôi đi đây phần còn lại bạn lo nhé!";
    }

    public void joinMapMaster() {
        this.location.x = master.location.x + getCloneOffsetX();
        this.location.y = master.location.y;
        MapService.gI().goToMap(this, master.zone);
        this.zone.load_Me_To_Another(this);
    }

    private int getCloneOffsetX() {
        int distance = 35 + ((cloneIndex - 1) / 2) * 35;
        return cloneIndex % 2 == 1 ? -distance : distance;
    }

    public void followMaster() {
        int targetX = master.location.x + getCloneOffsetX();
        int targetY = master.location.y;
        if (Math.sqrt(Math.pow(targetX - this.location.x, 2) + Math.pow(targetY - this.location.y, 2)) >= 45) {
            this.location.x = targetX + Util.nextInt(-8, 8);
            this.location.y = targetY;
            PlayerService.gI().playerMove(this, this.location.x, this.location.y);
        }
    }

    @Override
    public short getHead() {
        syncCaiTrangFromMaster(false);
        return super.getHead();
    }

    @Override
    public short getBody() {
        syncCaiTrangFromMaster(false);
        return super.getBody();
    }

    @Override
    public short getLeg() {
        syncCaiTrangFromMaster(false);
        return super.getLeg();
    }

    @Override
    public short getAuraId() {
        return master.getAuraId();
    }

    @Override
    public byte getAura() {
        return master.getAura();
    }

    @Override
    public short getMount() {
        return master.getMount();
    }

    private boolean isCellCloneMaster528() {
        return getMasterCaiTrangId() == MASTER_CAI_TRANG_ID;
    }

    private void startCellTuSatThenDisappear() {
        if (isCellCloneMaster528() && !waitingCellCloneTuSatDisappear) {
            if (!isChatBye && this.master != null) {
                Service.gI().chat(this, getDisappearText());
                isChatBye = true;
            }
            useCellCloneTuSatIfNeed();
            waitingCellCloneTuSatDisappear = true;
            timeCellCloneTuSat = System.currentTimeMillis();
            return;
        }
        finishDisposeAfterCellTuSat();
    }

    private void useCellCloneTuSatIfNeed() {
        if (usedCellCloneTuSat || this.zone == null || this.nPoint == null) {
            return;
        }
        if (getMasterCaiTrangId() != MASTER_CAI_TRANG_ID) {
            return;
        }
        usedCellCloneTuSat = true;

        Skill tuSatSkill = getTuSatSkillForClone();
        if (tuSatSkill != null) {
            this.playerSkill.skillSelect = tuSatSkill;
        }

        int tuSatPoint = getTuSatPointForClone();
        int rangeBom = SkillUtil.getRangeBom(tuSatPoint);
        long dame = Math.max(1, this.nPoint.hpMax);

        try {
            Service.gI().chat(this, "TU_SAT!");
            SkillService.gI().sendPlayerPrepareBom(this, 500);
        } catch (Exception ignored) {
        }

        try {
            if (this.zone.mobs != null) {
                for (Mob mob : this.zone.mobs) {
                    if (mob != null && !mob.isDie() && mob.location != null
                            && Util.getDistance(this.location.x, this.location.y, mob.location.x, mob.location.y) <= rangeBom) {
                        mob.injured(this, dame, true);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        try {
            if (this.zone.getBosses() != null) {
                for (Player boss : this.zone.getBosses()) {
                    if (boss != null && !boss.isDie() && boss.location != null
                            && Util.getDistance(this, boss) <= rangeBom) {
                        boss.injured(this, dame / 3, false, false);
                        PlayerService.gI().sendInfoHpMpMoney(boss);
                        Service.gI().Send_Info_NV(boss);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private Skill getTuSatSkillForClone() {
        try {
            if (this.playerSkill != null) {
                Skill skill = this.playerSkill.getSkillbyId(Skill.TU_SAT);
                if (skill != null && skill.template != null) {
                    return skill;
                }
            }
            if (master != null && master.playerSkill != null) {
                Skill skill = master.playerSkill.getSkillbyId(Skill.TU_SAT);
                if (skill != null && skill.template != null) {
                    return skill;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private int getTuSatPointForClone() {
        Skill skill = getTuSatSkillForClone();
        if (skill != null && skill.point > 0) {
            return skill.point;
        }
        return Math.max(1, Math.min(7, cloneIndex));
    }

    private void healMasterWhenDisappear() {
        if (hasHealMasterWhenDisappear || master == null || master.nPoint == null || master.isDie()) {
            return;
        }
        hasHealMasterWhenDisappear = true;

        long hpLimit = master.nPoint.hpMax * 30 / 100;
        if (master.nPoint.hp > hpLimit) {
            return;
        }

        int healPercent = Math.max(5, Math.min(35, cloneIndex * 5));
        long hpLost = Math.max(0, master.nPoint.hpMax - master.nPoint.hp);
        long mpLost = Math.max(0, master.nPoint.mpMax - master.nPoint.mp);
        long hpHeal = hpLost * healPercent / 100;
        long mpHeal = mpLost * healPercent / 100;

        if (hpHeal <= 0 && mpHeal <= 0) {
            return;
        }

        master.nPoint.addHp(hpHeal);
        master.nPoint.addMp(mpHeal);
        Service.gI().point(master);
        PlayerService.gI().sendInfoHpMpMoney(master);
        Service.gI().chat(this, "Tôi truyền lại " + healPercent + "% sinh lực cho bạn!");
    }

    @Override
    public void dispose() {
        if (isCellCloneMaster528() && !waitingCellCloneTuSatDisappear) {
            startCellTuSatThenDisappear();
            return;
        }
        finishDisposeAfterCellTuSat();
    }

    private void finishDisposeAfterCellTuSat() {
        if (!isChatBye && this.master != null && !this.isDie()) {
            Service.gI().chat(this, getDisappearText());
            isChatBye = true;
        }
        healMasterWhenDisappear();
        ChangeMapService.gI().exitMap(this);
        if (this.master != null) {
            this.master.ClonePhanThanList.remove(this);
            if (this.master.ClonePhanThan1234 == this) {
                this.master.ClonePhanThan1234 = this.master.ClonePhanThanList.isEmpty() ? null : this.master.ClonePhanThanList.get(0);
            }
        }
        this.master = null;
        super.dispose();
    }
}
