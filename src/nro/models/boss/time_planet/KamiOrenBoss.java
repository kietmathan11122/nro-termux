package nro.models.boss.time_planet;

import nro.models.boss.Boss;
import nro.models.boss.BossData;
import nro.models.boss.BossID;
import nro.models.consts.BossStatus;
import nro.models.map.Zone;
import nro.models.map.service.ChangeMapService;
import nro.models.player.Player;
import nro.models.services.Service;
import nro.models.utils.Util;

/**
 * Encounter Hành Tinh Thời Gian:
 * - Kami và Oren cùng xuất hiện và chiến đấu.
 * - Khi cả Kami và Oren còn 50% HP, cả 2 biến mất (thoát map) và Kamioren xuất hiện.
 */
public class KamiOrenBoss extends TimePlanetBoss {

    private boolean waitingForFusion;
    private boolean fused;
    private long lastTimeChatWait;

    public KamiOrenBoss(int id, BossData data) throws Exception {
        super(id, data);
    }

    protected KamiOrenBoss leader() {
        return parentBoss instanceof KamiOrenBoss ? (KamiOrenBoss) parentBoss : this;
    }

    private KamiOrenBoss companion(int bossId) {
        KamiOrenBoss leader = leader();
        if (leader.bossAppearTogether != null && leader.bossAppearTogether[0] != null) {
            for (Boss boss : leader.bossAppearTogether[0]) {
                if (boss != null && boss.id == bossId && boss instanceof KamiOrenBoss) {
                    return (KamiOrenBoss) boss;
                }
            }
        }
        return null;
    }

    private boolean isUnfusedForm() {
        return id == BossID.KAMI || id == BossID.OREN;
    }

    @Override
    public void initBase() {
        waitingForFusion = false;
        fused = false;
        lastTimeChatWait = 0;
        super.initBase();
    }

    @Override
    public int injured(Player attacker, long damage, boolean piercing, boolean mobAttack) {
        synchronized (leader()) {
            if (!isUnfusedForm()) {
                return super.injured(attacker, damage, piercing, mobAttack);
            }
            if (waitingForFusion || leader().fused || isDie() || damage <= 0) {
                return 0;
            }

            int halfHp = Math.max(1, (int) ((long) this.nPoint.hpMax / 2L));
            long allowedDamage = Math.min(damage, Math.max(0L, (long) this.nPoint.hp - halfHp));
            int actualDamage = allowedDamage > 0
                    ? super.injured(attacker, allowedDamage, piercing, mobAttack) : 0;

            if (this.nPoint.hp <= halfHp) {
                this.nPoint.hp = halfHp;
                this.waitingForFusion = true;
                this.changeToTypeNonPK();
                this.changeStatus(BossStatus.AFK);

                leader().tryFusion();

                if (!leader().fused) {
                    if (id == BossID.KAMI) {
                        chat("Mau lên Oren! Ta đợi ngươi hợp thể!");
                    } else {
                        chat("Mau lên Kami! Ta đợi ngươi hợp thể!");
                    }
                }
            }
            return actualDamage;
        }
    }

    @Override
    public void setBom(Player attacker) {
        // Kami và Oren không thể bị nổ bom chết trước khi hợp thể
        if (!isUnfusedForm()) {
            super.setBom(attacker);
        }
    }

    @Override
    public void update() {
        synchronized (leader()) {
            if (id == BossID.KAMI) {
                tryFusion();
            }

            if (isUnfusedForm() && (waitingForFusion || leader().fused)) {
                if (waitingForFusion && !leader().fused) {
                    if (Util.canDoWithTime(lastTimeChatWait, 5000)) {
                        lastTimeChatWait = System.currentTimeMillis();
                        if (id == BossID.KAMI) {
                            chat("Mau lên Oren! Hãy hợp thể cùng ta!");
                        } else {
                            chat("Mau lên Kami! Hãy hợp thể cùng ta!");
                        }
                    }
                }
                return;
            }

            super.update();
        }
    }

    @Override
    public void wakeupAnotherBossWhenAppear() {
        if (id == BossID.KAMI) {
            KamiOrenBoss oren = companion(BossID.OREN);
            if (oren != null) {
                oren.changeStatus(BossStatus.RESPAWN);
            }
        }
    }

    @Override
    public void joinMap() {
        if (id == BossID.KAMI) {
            super.joinMap();
            return;
        }
        KamiOrenBoss kami = leader();
        if (kami == this || kami.zone == null) {
            return;
        }

        this.zone = kami.zone;
        int x = (id == BossID.OREN)
                ? Math.max(0, kami.location.x - 30)
                : kami.location.x;
        int y = (kami.location != null && kami.location.y > 0)
                ? kami.location.y
                : this.zone.map.yPhysicInTop(x, 100);

        ChangeMapService.gI().changeMap(this, this.zone, x, y);
        Service.gI().sendFlagBag(this);
        notifyJoinMap();
        changeToTypePK();
        changeStatus(BossStatus.CHAT_S);
    }

    private void tryFusion() {
        if (fused || !waitingForFusion || zone == null) {
            return;
        }
        KamiOrenBoss oren = companion(BossID.OREN);
        KamiOrenBoss fusion = companion(BossID.KAMI_OREN);
        if (oren == null || fusion == null || !oren.waitingForFusion || oren.zone != zone) {
            return;
        }

        chat("Đến lúc rồi! Hợp thể nào Oren!");
        oren.chat("Hợp thể nào Kami!");

        fusion.respawn();
        fusion.joinMap();
        if (fusion.zone != zone) {
            return;
        }

        fused = true;
        removeFromMap();
        oren.removeFromMap();
        changeStatus(BossStatus.AFK);
        oren.changeStatus(BossStatus.AFK);
    }

    protected void removeFromMap() {
        ChangeMapService.gI().exitMap(this);
        zone = null;
        zoneFinal = null;
        lastZone = null;
    }

    @Override
    public void leaveMap() {
        synchronized (leader()) {
            KamiOrenBoss kami = leader();
            kami.resetEncounterBoss();
            if (kami.bossAppearTogether != null && kami.bossAppearTogether[0] != null) {
                for (Boss boss : kami.bossAppearTogether[0]) {
                    if (boss instanceof KamiOrenBoss) {
                        ((KamiOrenBoss) boss).resetEncounterBoss();
                    }
                }
            }
        }
    }

    private void resetEncounterBoss() {
        removeFromMap();
        waitingForFusion = false;
        fused = false;
        prepareBom = false;
        lastTimeRest = System.currentTimeMillis();
        changeStatus(BossStatus.REST);
    }

    @Override
    public void rest() {
        if (id == BossID.KAMI && Util.canDoWithTime(lastTimeRest, (long) secondsRest * 1000L)) {
            changeStatus(BossStatus.RESPAWN);
        }
    }
}
