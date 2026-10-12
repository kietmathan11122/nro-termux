package nro.models.boss;

import nro.models.boss.Boss_Manager.BrolyManager;
import nro.models.boss.Boss_Manager.LunarNewYearEventManager;
import nro.models.boss.Boss_Manager.GasDestroyManager;
import nro.models.boss.Boss_Manager.RedRibbonHQManager;
import nro.models.boss.Boss_Manager.SkillSummonedManager;
import nro.models.boss.Boss_Manager.TreasureUnderSeaManager;
import nro.models.boss.Boss_Manager.FinalBossManager;
import nro.models.boss.Boss_Manager.HalloweenEventManager;
import nro.models.boss.Boss_Manager.ChristmasEventManager;
import nro.models.boss.Boss_Manager.TrungThuEventManager;
import nro.models.boss.Boss_Manager.YardartManager;
import nro.models.boss.Boss_Manager.SnakeWayManager;
import nro.models.boss.Boss_Manager.HungVuongEventManager;
import nro.models.boss.Boss_Manager.OtherBossManager;
import nro.models.boss.Boss_Manager.BossManager;
import nro.models.consts.AppearType;
import nro.models.consts.BossStatus;
import nro.models.consts.BossType;
import nro.models.consts.ConstPlayer;
import static nro.models.consts.BossStatus.ACTIVE;
import static nro.models.consts.BossStatus.AFK;
import static nro.models.consts.BossStatus.CHAT_E;
import static nro.models.consts.BossStatus.CHAT_S;
import static nro.models.consts.BossStatus.DIE;
import static nro.models.consts.BossStatus.JOIN_MAP;
import static nro.models.consts.BossStatus.LEAVE_MAP;
import static nro.models.consts.BossStatus.RESPAWN;
import static nro.models.consts.BossStatus.REST;
import static nro.models.consts.BossType.BROLY;
import static nro.models.consts.BossType.CHRISTMAS_EVENT;
import static nro.models.consts.BossType.FINAL;
import static nro.models.consts.BossType.HALLOWEEN_EVENT;
import static nro.models.consts.BossType.HUNGVUONG_EVENT;
import static nro.models.consts.BossType.PHOBAN;
import static nro.models.consts.BossType.PHOBANBDKB;
import static nro.models.consts.BossType.PHOBANCDRD;
import static nro.models.consts.BossType.PHOBANDT;
import static nro.models.consts.BossType.PHOBANKGHD;
import static nro.models.consts.BossType.SKILLSUMMONED;
import static nro.models.consts.BossType.TET_EVENT;
import static nro.models.consts.BossType.TRUNGTHU_EVENT;
import static nro.models.consts.BossType.YARDART;
import nro.models.network.Message;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import nro.models.map.Zone;
import nro.models.mob.Mob;
import nro.models.player.Pet;
import nro.models.player.Player;
import nro.models.skill.Skill;
import nro.models.server.ServerNotify;
import nro.models.map.service.MapService;
import nro.models.services.PlayerService;
import nro.models.services.Service;
import nro.models.services.DaishinkanDropService;
import nro.models.services.NhatNguyetDropService;
import nro.models.services.CustomDropService;
import nro.models.services.SkillService;
import nro.models.services.TaskService;
import nro.models.services_dungeon.MajinBuuService;
import nro.models.map.service.ChangeMapService;
import nro.models.utils.Logger;
import nro.models.utils.SkillUtil;
import nro.models.utils.Util;
import nro.models.interfaces.IBoss;
import java.io.IOException;

public class Boss extends Player implements IBoss {

    private static final int BOSS_SKILL_COOLDOWN_MULTIPLIER = 2;
    private static final int MIN_BOSS_SKILL_COOLDOWN = 200;

    // A template owns one clock even when a legacy boss lists several levels.
    // These clocks belong to this boss, never the shared player templates.
    private Map<Integer, Long> bossSkillReadyAt = new HashMap<>();
    private long lastBossSkillUse;
    private Skill bossChargingSkill;

    public int currentLevel = -1;
    public final BossData[] data;

    public BossStatus bossStatus;

    protected Zone lastZone;

    protected long lastTimeRest;
    protected int secondsRest;

    protected long lastTimeChatS;
    protected int timeChatS;
    protected byte indexChatS;

    protected long lastTimeChatE;
    protected int timeChatE;
    protected byte indexChatE;

    protected long lastTimeChatM;
    protected int timeChatM;

    protected long lastTimeTargetPlayer;
    protected int timeTargetPlayer;
    protected Player playerTarger;

    protected Boss parentBoss;
    public Boss[][] bossAppearTogether;

    public Zone zoneFinal = null;

    public Player playerReward;

    public int lv;

    public int error;

    public boolean prepareBom;
public long lastBomTime; // ← thêm dòng này
public Player bomAttacker; // thêm dòng này
    public boolean isNotifyDisabled;
    public boolean isZone01SpawnDisabled;
    public BossType bossType;
    /** 0 = dùng dame cố định; >0 = dame bằng % HP tối đa của mục tiêu. */
    public volatile int damagePercentOfTargetHp;

    public Boss(int id, boolean isNotifyDisabled, boolean isZone01SpawnDisabled, BossData... data) throws Exception {
        this(id, data);
        this.isNotifyDisabled = isNotifyDisabled;
        this.isZone01SpawnDisabled = isZone01SpawnDisabled;
    }

    public Boss(BossType bossType, int id, boolean isNotifyDisabled, boolean isZone01SpawnDisabled, BossData... data) throws Exception {
        this(bossType, id, data);
        this.isNotifyDisabled = isNotifyDisabled;
        this.isZone01SpawnDisabled = isZone01SpawnDisabled;
    }

    public Boss(int id, BossData... data) throws Exception {
        this.id = id;
        this.isBoss = true;
        if (data == null || data.length == 0) {
            throw new Exception("Dữ liệu boss không hợp lệ");
        }
        this.data = data;
        this.secondsRest = this.data[0].getSecondsRest();
        this.bossStatus = BossStatus.REST;
        BossManager.gI().addBoss(this);

        this.bossAppearTogether = new Boss[this.data.length][];
        for (int i = 0; i < this.bossAppearTogether.length; i++) {
            if (this.data[i].getBossesAppearTogether() != null) {
                this.bossAppearTogether[i] = new Boss[this.data[i].getBossesAppearTogether().length];
                for (int j = 0; j < this.data[i].getBossesAppearTogether().length; j++) {
                    Boss boss = BossManager.gI().createBoss(this.data[i].getBossesAppearTogether()[j]);
                    if (boss != null) {
                        boss.parentBoss = this;
                        boss.lv = j;
                        this.bossAppearTogether[i][j] = boss;
                    }
                }
            }
        }
    }

    public Boss(BossType bossType, int id, BossData... data) throws Exception {
        this.bossType = bossType;
        this.id = id;
        this.isBoss = true;
        if (data == null || data.length == 0) {
            throw new Exception("Dữ liệu boss không hợp lệ");
        }
        this.data = data;
        this.secondsRest = this.data[0].getSecondsRest();
        this.bossStatus = BossStatus.REST;
        switch (bossType) {
            case YARDART ->
                YardartManager.gI().addBoss(this);
            case FINAL ->
                FinalBossManager.gI().addBoss(this);
            case SKILLSUMMONED ->
                SkillSummonedManager.gI().addBoss(this);
            case BROLY ->
                BrolyManager.gI().addBoss(this);
            case PHOBAN ->
                OtherBossManager.gI().addBoss(this);
            case PHOBANDT ->
                RedRibbonHQManager.gI().addBoss(this);
            case PHOBANBDKB ->
                TreasureUnderSeaManager.gI().addBoss(this);
            case PHOBANCDRD ->
                SnakeWayManager.gI().addBoss(this);
            case PHOBANKGHD ->
                GasDestroyManager.gI().addBoss(this);
            case TRUNGTHU_EVENT ->
                TrungThuEventManager.gI().addBoss(this);
            case HALLOWEEN_EVENT ->
                HalloweenEventManager.gI().addBoss(this);
            case CHRISTMAS_EVENT ->
                ChristmasEventManager.gI().addBoss(this);
            case HUNGVUONG_EVENT ->
                HungVuongEventManager.gI().addBoss(this);
            case TET_EVENT ->
                LunarNewYearEventManager.gI().addBoss(this);
        }

        this.bossAppearTogether = new Boss[this.data.length][];
        for (int i = 0; i < this.bossAppearTogether.length; i++) {
            if (this.data[i].getBossesAppearTogether() != null) {
                this.bossAppearTogether[i] = new Boss[this.data[i].getBossesAppearTogether().length];
                for (int j = 0; j < this.data[i].getBossesAppearTogether().length; j++) {
                    Boss boss = BossManager.gI().createBoss(this.data[i].getBossesAppearTogether()[j]);
                    if (boss != null) {
                        boss.parentBoss = this;
                        this.bossAppearTogether[i][j] = boss;
                    }
                }
            }
        }
    }

    @Override
    public void initBase() {
        BossData data = this.data[this.currentLevel];
        this.name = String.format(data.getName(), Util.nextInt(0, 100));
        this.gender = data.getGender();
        this.nPoint.mpg = 31_07_2002;
        this.nPoint.dameg = data.getDame();
        this.nPoint.hpg = data.getHp()[Util.nextInt(0, data.getHp().length - 1)];
        this.nPoint.hp = nPoint.hpg;
        this.nPoint.calPoint();
        if (data.getDefense() > 0) {
            this.nPoint.defg = Math.max(1, data.getDefense() / 4);
            this.nPoint.def = data.getDefense();
        }
        this.initSkill();
        this.resetBase();
    }

    protected void initSkill() {
        resetBossSkillClock();
        for (Skill skill : this.playerSkill.skills) {
            skill.dispose();
        }
        this.playerSkill.skills.clear();
        this.playerSkill.skillSelect = null;
        int[][] skillTemps = data[this.currentLevel].getSkillTemp();
        for (int[] skillTemp : skillTemps) {
            Skill skill = SkillUtil.createSkill(skillTemp[0], skillTemp[1]);
            if (skillTemp.length == 3) {
                skill.coolDown = skillTemp[2];
            }
            applyBossSkillCooldown(skill);
            this.playerSkill.skills.add(skill);
        }
    }

    protected final void applyBossSkillCooldown(Skill skill) {
        if (skill == null) {
            return;
        }
        long slowedCooldown = (long) skill.coolDown * BOSS_SKILL_COOLDOWN_MULTIPLIER;
        skill.coolDown = (int) Math.min(Integer.MAX_VALUE,
                Math.max(MIN_BOSS_SKILL_COOLDOWN, slowedCooldown));
    }

    protected void resetBase() {
        this.lastTimeChatS = 0;
        this.lastTimeChatE = 0;
        this.timeChatS = 0;
        this.timeChatE = 0;
        this.indexChatS = 0;
        this.indexChatE = 0;
    }

    //.outfit.
    @Override
    public short getHead() {
        if (effectSkill != null && effectSkill.isBinh) {
            return idOutfitMafuba[effectSkill.typeBinh][0];
        }
        if (effectSkill != null && effectSkill.isMonkey) {
            return (short) ConstPlayer.HEADMONKEY[effectSkill.levelMonkey - 1];
        }
        return this.data[this.currentLevel].getOutfit()[0];
    }

    @Override
    public short getBody() {
        if (effectSkill != null && effectSkill.isBinh) {
            return idOutfitMafuba[effectSkill.typeBinh][1];
        }
        if (effectSkill != null && effectSkill.isMonkey) {
            return 193;
        }
        return this.data[this.currentLevel].getOutfit()[1];
    }

    @Override
    public short getLeg() {
        if (effectSkill != null && effectSkill.isBinh) {
            return idOutfitMafuba[effectSkill.typeBinh][2];
        }
        if (effectSkill != null && effectSkill.isMonkey) {
            return 194;
        }
        return this.data[this.currentLevel].getOutfit()[2];

    }

    @Override
    public short getFlagBag() {
        return this.data[this.currentLevel].getOutfit()[3];
    }

    @Override
    public short getAuraId() {
        if (this.isHideAura) return -1;
        if (this.adminAuraId >= 0) return this.adminAuraId;
        return this.data[this.currentLevel].getOutfit()[4];
    }

    @Override
    public byte getAura() {
        return (byte) this.data[this.currentLevel].getOutfit()[4];
    }

    @Override
    public byte getEffFront() {
        return (byte) this.data[this.currentLevel].getOutfit()[5];
    }

    public Zone getMapJoin() {
        if (this.bossType == BossType.TET_EVENT ||
            this.bossType == BossType.TRUNGTHU_EVENT ||
            this.bossType == BossType.HALLOWEEN_EVENT ||
            this.bossType == BossType.CHRISTMAS_EVENT ||
            this.bossType == BossType.HUNGVUONG_EVENT) {
            java.util.List<nro.models.map.Map> maps = nro.models.server.Manager.MAPS;
            nro.models.map.Map randomMap = null;
            for (int i = 0; i < 100; i++) {
                nro.models.map.Map m = maps.get(Util.nextInt(0, maps.size() - 1));
                if (m != null && m.zones != null && !m.zones.isEmpty() && m.mapId != 46) {
                    randomMap = m;
                    break;
                }
            }
            if (randomMap != null) {
                return MapService.gI().getMapWithRandZone(randomMap.mapId);
            }
        }
        int mapId = this.data[this.currentLevel].getMapJoin()[Util.nextInt(0, this.data[this.currentLevel].getMapJoin().length - 1)];
        Zone map = MapService.gI().getMapWithRandZone(mapId);
        return map;
    }

    @Override
    public void changeStatus(BossStatus status) {
        this.bossStatus = status;
    }

    @Override
    public Player getPlayerAttack() {
        if (this.zone == null) {
            return null;
        }
        if (this.playerTarger != null && (this.playerTarger.isDie() || !this.zone.equals(this.playerTarger.zone))) {
            this.playerTarger = null;
        }
        if (this.playerTarger == null || Util.canDoWithTime(this.lastTimeTargetPlayer, this.timeTargetPlayer)) {
            this.playerTarger = this.zone.getRandomPlayerInMap();
            this.lastTimeTargetPlayer = System.currentTimeMillis();
            this.timeTargetPlayer = Util.nextInt(5000, 7000);
        }
        if (this.playerTarger != null && this.playerTarger.isPet && ((Pet) this.playerTarger).master != null && ((Pet) this.playerTarger).master.equals(this)) {
            this.playerTarger = null;
        }
        return this.playerTarger;
    }

    @Override
    public void changeToTypePK() {
        PlayerService.gI().changeAndSendTypePK(this, ConstPlayer.PK_ALL);
    }

    @Override
    public void changeToTypeNonPK() {
        PlayerService.gI().changeAndSendTypePK(this, ConstPlayer.NON_PK);
    }

    @Override
    public void updateInfo() {
        super.update();
    }

   
  @Override
public void update() {
    // Đang chờ bom nổ
    if (prepareBom && !Util.canDoWithTime(lastBomTime, 2500)) {
        return;
    }
if (prepareBom && Util.canDoWithTime(lastBomTime, 2500)) {
    prepareBom = false;
    setDie(this);
    die(bomAttacker);
    if (this.zone != null) {  // ← thêm check null
        long dame = this.nPoint.hpMax;
        for (Mob mob : this.zone.mobs) {
            mob.injured(this, dame, true);
        }
        List<Player> playersMap = this.zone.getNotBosses();
        if (!MapService.gI().isMapOffline(this.zone.map.mapId)) {
            for (int i = playersMap.size() - 1; i >= 0; i--) {
                Player pl = playersMap.get(i);
                if (!this.equals(pl)) {
                    pl.injured(this, dame, false, false);
                    PlayerService.gI().sendInfoHpMpMoney(pl);
                    Service.gI().Send_Info_NV(pl);
                }
            }
        }
    }
    bomAttacker = null;
    return;
}
    // phần còn lại giữ nguyên từ đây
    super.update();
        this.nPoint.mp = this.nPoint.mpg;
        if (this.effectSkill == null || this.effectSkill.isHaveEffectSkill() || (this.newSkill != null && this.newSkill.isStartSkillSpecial)) {
            return;
        }
        switch (this.bossStatus) {
            case CHAT_S, AFK, ACTIVE ->
                this.autoLeaveMap();
        }
        switch (this.bossStatus) {
            case REST ->
                this.rest();
            case RESPAWN -> {
                this.respawn();
                this.changeStatus(BossStatus.JOIN_MAP);
            }
            case JOIN_MAP ->
                this.joinMap();
            case CHAT_S -> {
                if (chatS()) {
                    this.doneChatS();
                    this.lastTimeChatM = System.currentTimeMillis();
                    this.timeChatM = 5000;
                    if (this.bossStatus != BossStatus.AFK) {
                        this.changeStatus(BossStatus.ACTIVE);
                    }
                }
            }
            case AFK ->
                this.afk();
            case ACTIVE -> {
                this.chatM();
                if (this.effectSkill.isCharging
                        && (BossCombatSimulation.actionGapMs(this) > 0 || !Util.isTrue(1, 20))
                        || this.effectSkill.useTroi) {
                    return;
                }
                this.active();
            }
            case DIE ->
                this.changeStatus(BossStatus.CHAT_E);
            case CHAT_E -> {
                if (chatE()) {
                    this.doneChatE();
                    this.changeStatus(BossStatus.LEAVE_MAP);
                }
            }
            case LEAVE_MAP ->
                this.leaveMap();
        }
    }

    @Override
    public void rest() {
        int nextLevel = this.currentLevel + 1;
        if (nextLevel >= this.data.length) {
            nextLevel = 0;
        }
        if (this.data[nextLevel].getTypeAppear() == AppearType.DEFAULT_APPEAR
                && Util.canDoWithTime(lastTimeRest, secondsRest * 1000)) {
            this.changeStatus(BossStatus.RESPAWN);
        }
    }

    @Override
    public void afk() {

    }

    @Override
    public void respawn() {
        this.currentLevel++;
        if (this.currentLevel >= this.data.length) {
            this.currentLevel = 0;
        }
        this.initBase();
        this.changeToTypeNonPK();
    }

    @Override
    public void joinMap() {
        if (zoneFinal != null) {
            joinMapByZone(zoneFinal);
            this.notifyJoinMap();
            this.changeStatus(BossStatus.CHAT_S);
            this.changeToTypePK();
            this.wakeupAnotherBossWhenAppear();
            return;
        }
        if (this.zone == null) {
            if (this.parentBoss != null) {
                this.zone = parentBoss.zone;
            } else if (this.lastZone == null) {
                this.zone = getMapJoin();
            } else {
                this.zone = this.lastZone;
            }
        }
        if (this.zone == null) {
            this.zone = getMapJoin();
        }
        if (this.zone != null) {
            try {
                if (this.currentLevel == 0) {
                    if (this.parentBoss == null) {
                        int zoneid = 0;
                        //this.zone.map.mapId == 80 || this.zone.map.mapId == 103 || this.zone.map.mapId == 97 || this.zone.map.mapId == 102
                        // Chỉ cho boss xuất hiện từ khu 2 trở lên ở map thường
                        if (this.isZone01SpawnDisabled && this.zone.map.zones.size() > 2) {
                            zoneid = Util.nextInt(2, this.zone.map.zones.size() - 1);
                            while (zoneid < this.zone.map.zones.size() && !this.zone.map.zones.get(zoneid).getBosses().isEmpty()) {
                                zoneid++;
                            }

                            if (zoneid < this.zone.map.zones.size()) {
                                this.zone = this.zone.map.zones.get(zoneid);
                            } else {
                                this.changeStatus(BossStatus.REST);
                                this.zone = null;
                                this.lastZone = null;
                                return;
                            }
                        } else {
                            // Check trong khu lớn hơn 10 người chuyển sang khu n + 1
                            while (zoneid < this.zone.map.zones.size() && this.zone.map.zones.get(zoneid).getNumOfPlayers() > 10) {
                                zoneid++;
                            }
                            // Check trong khu có boss sẽ chuyển sang khu n + 1
                            while (zoneid < this.zone.map.zones.size() && !this.zone.map.zones.get(zoneid).getBosses().isEmpty()) {
                                zoneid++;
                            }
                            if (zoneid < this.zone.map.zones.size()) {
                                this.zone = this.zone.map.zones.get(zoneid);
                            } else {
                                this.zone = this.zone.map.zones.get(0);
                            }
                        }
                        int x = this.zone.map.mapWidth > 100 ? Util.nextInt(100, this.zone.map.mapWidth - 100) : Util.nextInt(100);
                        int y = this.zone.map.yPhysicInTop(x, 100);
                        ChangeMapService.gI().changeMap(this, this.zone, x, y);
                    } else {
                        int x = this.parentBoss.location.x - (this.lv + 1) * 30;
                        int y = this.zone.map.yPhysicInTop(x, 100);
                        ChangeMapService.gI().changeMap(this, this.zone, x, y);
                    }
                    this.wakeupAnotherBossWhenAppear();
                } else {
                    ChangeMapService.gI().changeMap(this, this.zone, this.location.x, this.location.y);
                }
                Service.gI().sendFlagBag(this);
                this.notifyJoinMap();
                this.changeStatus(BossStatus.CHAT_S);
                this.changeToTypePK();
            } catch (Exception e) {
                this.changeStatus(BossStatus.REST);
                if (error < 5) {
                    Logger.error("Lỗi : " + e + "\n");
                    error++;
                }
            }
        } else {
            this.changeStatus(BossStatus.RESPAWN);
        }
    }

    public void joinMapByZone(Zone zone) {
        if (zone != null) {
            this.zone = zone;
            int x = this.zone.map.mapWidth > 100 ? Util.nextInt(100, this.zone.map.mapWidth - 100) : Util.nextInt(100);
            int y = this.zone.map.yPhysicInTop(x, 100);
            ChangeMapService.gI().changeMap(this, this.zone, x, y);
        }
    }

    protected void notifyJoinMap() {
        if (canSendNotify()) {
            ServerNotify.gI().notify("BOSS " + this.name + " vừa xuất hiện tại " + this.zone.map.mapName);
        }
    }

    private boolean canSendNotify() {
        return true;
    }

    @Override
    public boolean chatS() {
        if (Util.canDoWithTime(lastTimeChatS, timeChatS)) {
            if (this.indexChatS == this.data[this.currentLevel].getTextS().length) {
                return true;
            }
            String textChat = this.data[this.currentLevel].getTextS()[this.indexChatS];
            int prefix = Integer.parseInt(textChat.substring(1, textChat.lastIndexOf("|")));
            textChat = textChat.substring(textChat.lastIndexOf("|") + 1);
            if (!this.chat(prefix, textChat)) {
                return false;
            }
            this.lastTimeChatS = System.currentTimeMillis();
            this.timeChatS = textChat.length() * 100;
            if (this.timeChatS > 2000) {
                this.timeChatS = 2000;
            }
            this.indexChatS++;
        }
        return false;
    }

    @Override
    public void doneChatS() {

    }

    @Override
    public void chatM() {
        if (this.typePk == ConstPlayer.NON_PK) {
            return;
        }
        if (this.data[this.currentLevel].getTextM().length == 0) {
            return;
        }
        if (!Util.canDoWithTime(this.lastTimeChatM, this.timeChatM)) {
            return;
        }
        String textChat = this.data[this.currentLevel].getTextM()[Util.nextInt(0, this.data[this.currentLevel].getTextM().length - 1)];
        int prefix = Integer.parseInt(textChat.substring(1, textChat.lastIndexOf("|")));
        textChat = textChat.substring(textChat.lastIndexOf("|") + 1);
        this.chat(prefix, textChat);
        this.lastTimeChatM = System.currentTimeMillis();
        this.timeChatM = Util.nextInt(3000, 20000);
    }

    @Override
    public void active() {
        if (this.typePk == ConstPlayer.NON_PK) {
            this.changeToTypePK();
        }
        this.attack();
    }

    protected long lastTimeAttack;

    @Override
    public void attack() {
        if (isSimulationCharging()) {
            return;
        }
        if (Util.canDoWithTime(this.lastTimeAttack, 100) && this.typePk == ConstPlayer.PK_ALL) {
            this.lastTimeAttack = System.currentTimeMillis();
            try {
                Player pl = getPlayerAttack();
                if (pl == null || pl.isDie()) {
                    return;
                }
                if (!this.selectReadySkill()) {
                    return;
                }
                if (Util.getDistance(this, pl) <= this.getRangeCanAttackWithSkillSelect()) {
                    if (Util.isTrue(5, 20)) {
                        if (SkillUtil.isUseSkillChuong(this)) {
                            this.moveTo(pl.location.x + (Util.getOne(-1, 1) * Util.nextInt(20, 200)),
                                    Util.nextInt(10) % 2 == 0 ? pl.location.y : pl.location.y - Util.nextInt(0, 70));
                        } else {
                            this.moveTo(pl.location.x + (Util.getOne(-1, 1) * Util.nextInt(10, 40)),
                                    Util.nextInt(10) % 2 == 0 ? pl.location.y : pl.location.y - Util.nextInt(0, 50));
                        }
                    }
                    int originalDame = this.nPoint.dame;
                    if (this.damagePercentOfTargetHp > 0) {
                        long percentDame = (long) pl.nPoint.hpMax * this.damagePercentOfTargetHp / 100L;
                        this.nPoint.dame = (int) Math.max(1L, Math.min(Integer.MAX_VALUE, percentDame));
                    }
                    try {
                        SkillService.gI().useSkill(this, pl, null, -1, null);
                    } finally {
                        this.nPoint.dame = originalDame;
                    }
                    checkPlayerDie(pl);
                } else {
                    if (Util.isTrue(1, 2)) {
                        this.moveToPlayer(pl);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    protected boolean selectReadySkill() {
        if (isSimulationCharging()) {
            return false;
        }
        if (this.playerSkill == null || this.playerSkill.skills == null || this.playerSkill.skills.isEmpty()) {
            return false;
        }
        int start = Util.nextInt(0, this.playerSkill.skills.size() - 1);
        for (int i = 0; i < this.playerSkill.skills.size(); i++) {
            Skill skill = this.playerSkill.skills.get((start + i) % this.playerSkill.skills.size());
            if (canUseBossSkill(skill)) {
                this.playerSkill.skillSelect = skill;
                return true;
            }
        }
        return false;
    }

    public final boolean canUseBossSkill(Skill skill) {
        return canUseBossSkillAt(skill, System.currentTimeMillis());
    }

    /** Deterministic clock boundary, also used by the ordinary skill service. */
    protected synchronized boolean canUseBossSkillAt(Skill skill, long now) {
        if (skill == null || skill.template == null || this.nPoint == null) {
            return false;
        }
        if (isSimulationCharging()) {
            if (bossChargingSkill != null && this.playerSkill != null) {
                this.playerSkill.skillSelect = bossChargingSkill;
            }
            return false;
        }
        bossChargingSkill = null;
        int actionGap = BossCombatSimulation.actionGapMs(this);
        if (lastBossSkillUse > 0 && now - lastBossSkillUse < actionGap) {
            return false;
        }
        int cooldown = getBossTemplateCooldown(skill);
        if (skill.lastTimeUseThisSkill > 0 && now - skill.lastTimeUseThisSkill < cooldown) {
            return false;
        }
        if (bossSkillReadyAt != null && now < bossSkillReadyAt.getOrDefault((int) skill.template.id, 0L)) {
            return false;
        }
        if (skill.template.id == Skill.KAIOKEN && this.nPoint.hp <= this.nPoint.hpMax / 100 * 10) {
            return false;
        }
        return switch (skill.template.manaUseType) {
            case 0 -> this.nPoint.mp >= skill.manaUse;
            case 1 -> this.nPoint.mp >= (long) this.nPoint.mpMax * skill.manaUse / 100;
            case 2 -> this.nPoint.mp > 0;
            default -> false;
        };
    }

    private boolean isSimulationCharging() {
        return BossCombatSimulation.actionGapMs(this) > 0
                && this.effectSkill != null && this.effectSkill.isCharging;
    }

    private int getBossTemplateCooldown(Skill selected) {
        int cooldown = Math.max(0, selected.coolDown);
        if (this.playerSkill != null && this.playerSkill.skills != null) {
            for (Skill sibling : this.playerSkill.skills) {
                if (sibling != null && sibling.template != null
                        && sibling.template.id == selected.template.id) {
                    cooldown = Math.max(cooldown, sibling.coolDown);
                }
            }
        }
        return cooldown;
    }

    /** Called by after-use only: failed readiness/target checks never consume a clock. */
    public final synchronized void recordBossSkillUse(Skill used, long now) {
        if (used == null || used.template == null) {
            return;
        }
        if (bossSkillReadyAt == null) {
            bossSkillReadyAt = new HashMap<>();
        }
        int cooldown = getBossTemplateCooldown(used);
        long readyAt = now > Long.MAX_VALUE - cooldown ? Long.MAX_VALUE : now + cooldown;
        bossSkillReadyAt.put((int) used.template.id,
                Math.max(readyAt, bossSkillReadyAt.getOrDefault((int) used.template.id, 0L)));
        lastBossSkillUse = now;
        used.lastTimeUseThisSkill = now;
        if (this.playerSkill != null && this.playerSkill.skills != null) {
            for (Skill sibling : this.playerSkill.skills) {
                if (sibling != null && sibling.template != null
                        && sibling.template.id == used.template.id) {
                    sibling.lastTimeUseThisSkill = now;
                }
            }
        }
        if (isSimulationCharging()) {
            bossChargingSkill = used;
        }
    }

    protected final synchronized void resetBossSkillClock() {
        if (bossSkillReadyAt != null) {
            bossSkillReadyAt.clear();
        }
        lastBossSkillUse = 0;
        bossChargingSkill = null;
    }

    @Override
    public void checkPlayerDie(Player player) {
        if (player.isDie()) {

        }
    }

    protected int getRangeCanAttackWithSkillSelect() {
        int skillId = this.playerSkill.skillSelect.template.id;
        if (skillId == Skill.KAMEJOKO || skillId == Skill.MASENKO || skillId == Skill.ANTOMIC) {
            return Skill.RANGE_ATTACK_CHIEU_CHUONG;
        } else if (skillId == Skill.DRAGON || skillId == Skill.DEMON || skillId == Skill.GALICK || skillId == Skill.LIEN_HOAN || skillId == Skill.KAIOKEN) {
            return Skill.RANGE_ATTACK_CHIEU_DAM;
        }
        return 500;
    }

    @Override
    public void die(Player plKill) {
        CustomDropService.gI().setCurrentDyingBoss(this);
        try {
            if (plKill != null
                    && (this.zone.map.mapId != 140 || !MapService.gI().isMapMaBu(this.zone.map.mapId)
                    || !MapService.gI().isMapDoanhTrai(this.zone.map.mapId)
                    || !MapService.gI().isMapBanDoKhoBau(this.zone.map.mapId))) {
                if (!plKill.isBot) {
                    reward(plKill);
                    if (!CustomDropService.gI().isEventBlocked("TET")) {
                        dropTetSausage(plKill);
                    }
                    if (!CustomDropService.gI().isEventBlocked("DAISHINKAN")) {
                        DaishinkanDropService.dropBossFragment(plKill, this.location.x, this.location.y - 24);
                    }
                    if (!CustomDropService.gI().isEventBlocked("NHAT_NGUYET")) {
                        NhatNguyetDropService.dropFromBoss(plKill, this.location.x, this.location.y - 24);
                    }
                    CustomDropService.gI().dropBossReward(this, plKill);
                    if (MapService.gI().isMapMaBu(this.zone.map.mapId) && Util.isTrue(100, 100)) {
                        nro.models.map.ItemMap item1900 = new nro.models.map.ItemMap(this.zone, 1900, 1, this.location.x, this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), plKill.id);
                        Service.gI().dropItemMap(this.zone, item1900);
                    }
                }
                ServerNotify.gI().notify(plKill.name + ": Đã tiêu diệt được " + this.name + " mọi người đều ngưỡng mộ.");
                this.changeStatus(BossStatus.DIE);
            } else {
                if (plKill != null && !plKill.isBot) {
                    reward(plKill);
                    if (!CustomDropService.gI().isEventBlocked("TET")) {
                        dropTetSausage(plKill);
                    }
                    if (!CustomDropService.gI().isEventBlocked("DAISHINKAN")) {
                        DaishinkanDropService.dropBossFragment(plKill, this.location.x, this.location.y - 24);
                    }
                    if (!CustomDropService.gI().isEventBlocked("NHAT_NGUYET")) {
                        NhatNguyetDropService.dropFromBoss(plKill, this.location.x, this.location.y - 24);
                    }
                    CustomDropService.gI().dropBossReward(this, plKill);
                    if (MapService.gI().isMapMaBu(this.zone.map.mapId) && Util.isTrue(100, 100)) {
                        nro.models.map.ItemMap item1900 = new nro.models.map.ItemMap(this.zone, 1900, 1, this.location.x, this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), plKill.id);
                        Service.gI().dropItemMap(this.zone, item1900);
                    }
                }
                this.changeStatus(BossStatus.DIE);
            }
        } finally {
            CustomDropService.gI().setCurrentDyingBoss(null);
        }
    }

    @Override
    public void reward(Player plKill) {
        if (plKill != null && plKill.zone != null && plKill.zone.map != null
                && MapService.gI().isMapKimetsuTower(plKill.zone.map.mapId)) {
            plKill.fightMabu.changePoint((byte) 10);
        }
        TaskService.gI().checkDoneTaskKillBoss(plKill, this);
    }

    private void dropTetSausage(Player plKill) {
        if (!nro.models.event.EventManager.LUNNAR_NEW_YEAR || plKill == null
                || plKill.zone == null || this.zone == null) {
            return;
        }
        int y = this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24);
        nro.models.map.ItemMap sausage = new nro.models.map.ItemMap(this.zone,
                nro.models.services.LunarNewYearEventService.CHA_LUA,
                1, this.location.x, y, plKill.id);
        Service.gI().dropItemMap(this.zone, sausage);
    }

    @Override
    public boolean chatE() {
        if (Util.canDoWithTime(lastTimeChatE, timeChatE)) {
            if (this.indexChatE == this.data[this.currentLevel].getTextE().length) {
                return true;
            }
            String textChat = this.data[this.currentLevel].getTextE()[this.indexChatE];
            int prefix = Integer.parseInt(textChat.substring(1, textChat.lastIndexOf("|")));
            textChat = textChat.substring(textChat.lastIndexOf("|") + 1);
            if (!this.chat(prefix, textChat)) {
                return false;
            }
            this.lastTimeChatE = System.currentTimeMillis();
            this.timeChatE = textChat.length() * 100;
            if (this.timeChatE > 2000) {
                this.timeChatE = 2000;
            }
            this.indexChatE++;
        }
        return false;
    }

    @Override
    public void doneChatE() {

    }

    @Override
    public void leaveMap() {
        if (this.currentLevel < this.data.length - 1) {
            this.lastZone = this.zone;
            this.changeStatus(BossStatus.RESPAWN);
        } else {
            ChangeMapService.gI().exitMap(this);
            this.lastZone = null;
            this.lastTimeRest = System.currentTimeMillis();
            this.changeStatus(BossStatus.REST);
        }
        this.wakeupAnotherBossWhenDisappear();
    }

    @Override
    public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            if (!piercing && Util.isTrue(this.nPoint.tlNeDon, 1000)) {
                this.chat("Xí hụt");
                return 0;
            }

            if (plAtt != null && plAtt.idNRNM != -1) {
                return 1;
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
    public void moveToPlayer(Player pl) {
        if (pl.location != null) {
            moveTo(pl.location.x, pl.location.y);
        }
    }

    @Override
    public void moveTo(int x, int y) {
        byte dir = (byte) (this.location.x - x < 0 ? 1 : -1);
        byte move = (byte) Util.nextInt(40, 60);
        PlayerService.gI().playerMove(this, this.location.x + (dir == 1 ? move : -move), y + (Util.isTrue(3, 10) ? -50 : 0));
    }

    public void chat(String text) {
        Service.gI().chat(this, text);
    }

    protected boolean chat(int prefix, String textChat) {
        if (prefix == -1) {
            this.chat(textChat);
        } else if (prefix == -2) {
            if (this.zone != null) {
                Player plMap = this.zone.getRandomPlayerInMap();
                if (plMap != null && !plMap.isDie() && Util.getDistance(this, plMap) <= 600) {
                    Service.gI().chat(plMap, textChat);
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else if (prefix == -3) {
            if (this.parentBoss != null && !this.parentBoss.isDie()) {
                this.parentBoss.chat(textChat);
            }
        } else if (prefix >= 0) {
            if (this.bossAppearTogether != null && this.bossAppearTogether[this.currentLevel] != null) {
                Boss boss = this.bossAppearTogether[this.currentLevel][prefix];
                if (!boss.isDie()) {
                    boss.chat(textChat);
                }
            } else if (this.parentBoss != null && this.parentBoss.bossAppearTogether != null
                    && this.parentBoss.bossAppearTogether[this.parentBoss.currentLevel] != null) {
                Boss boss = this.parentBoss.bossAppearTogether[this.parentBoss.currentLevel][prefix];
                if (!boss.isDie()) {
                    boss.chat(textChat);
                }
            }
        }
        return true;
    }

    @Override
    public void wakeupAnotherBossWhenAppear() {
        if (this.bossAppearTogether == null || this.bossAppearTogether[this.currentLevel] == null) {
            return;
        }
        for (Boss boss : this.bossAppearTogether[this.currentLevel]) {
            int nextLevelBoss = boss.currentLevel + 1;
            if (nextLevelBoss >= boss.data.length) {
                nextLevelBoss = 0;
            }
            if (boss.data[nextLevelBoss].getTypeAppear() == AppearType.CALL_BY_ANOTHER) {
                if (boss.zone != null) {
                    boss.leaveMap();
                }
            }
            if (boss.data[nextLevelBoss].getTypeAppear() == AppearType.APPEAR_WITH_ANOTHER) {
                if (boss.zone != null) {
                    boss.leaveMap();
                }
                boss.changeStatus(BossStatus.RESPAWN);
            }
        }
    }

    @Override
    public void wakeupAnotherBossWhenDisappear() {
    }

    @Override
    public void autoLeaveMap() {

    }

    public void leaveMapNew() {
        if (this.data != null) {
            this.currentLevel = this.data.length;
        }
        this.changeStatus(BossStatus.LEAVE_MAP);
    }

  @Override
public void setBom(Player plAtt) {
    try {
        if (!prepareBom) {
            prepareBom = true;
            this.nPoint.hp = 1;
            this.lastBomTime = System.currentTimeMillis();
            this.bomAttacker = plAtt; // lưu lại để dùng trong update()
            Service.gI().chat(this, "Rồi, rồi, mày xong rồi!");
            Message msg;
            try {
                msg = new Message(-45);
                msg.writer().writeByte(7);
                msg.writer().writeInt((int) this.id);
                msg.writer().writeShort(104);
                msg.writer().writeShort(2000);
                Service.gI().sendMessAllPlayerInMap(this, msg);
                msg.cleanup();
            } catch (IOException e) {
                e.printStackTrace();
            }
            // KHÔNG CÓ while loop ở đây nữa
        }
    } catch (Exception e) {
        prepareBom = false;
        bomAttacker = null;
        setDie(this);
        die(plAtt);
    }
}

}
