package nro.models.boss.cumber;

import nro.models.boss.Boss;
import nro.models.boss.BossesData;
import nro.models.boss.BossID;
import nro.models.consts.ConstPlayer;
import nro.models.player.Player;
import nro.models.services.EffectSkillService;
import nro.models.services.SkillService;
import nro.models.utils.Util;

public class GokuSSJ4 extends Boss {

    private long lastTimeStun;

    public GokuSSJ4() throws Exception {
        super(BossID.GOKU_SSJ4, false, true, BossesData.GOKU_SSJ4);
        this.typePk = ConstPlayer.NON_PK;
    }

    @Override
    public Player getPlayerAttack() {
        if (this.zone == null) {
            return null;
        }
        for (Player pl : this.zone.getBosses()) {
            if (pl != null && !pl.isDie() && pl.id == BossID.CUMBER) {
                return pl;
            }
        }
        return null;
    }

    @Override
    public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (plAtt != null && plAtt.isBoss && plAtt.id == BossID.CUMBER) {
            return super.injured(plAtt, damage, piercing, isMobAttack);
        }
        return 0;
    }

    @Override
    public void attack() {
        if (Util.canDoWithTime(this.lastTimeAttack, 2000)) {
            this.lastTimeAttack = System.currentTimeMillis();
            try {
                Player pl = getPlayerAttack();
                if (pl == null || pl.isDie()) {
                    return;
                }
                int dis = Util.getDistance(this, pl);
                if (dis > 150) {
                    move(pl.location.x + Util.nextInt(-50, 50), pl.location.y);
                } else {
                    // Chỉ choáng 30 giây 1 lần
                    if (Util.canDoWithTime(this.lastTimeStun, 30000)) {
                        this.lastTimeStun = System.currentTimeMillis();
                        
                        // Choáng boss 3 giây
                        EffectSkillService.gI().startStun(pl, System.currentTimeMillis(), 3000);
                        
                        this.chat("Thái Dương Hạ San!");
                    }
                    
                    // Gây 100k sát thương
                    pl.injured(this, 100000, true, false);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
