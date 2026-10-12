package nro.models.boss.event.Halloween;

import nro.models.boss.Boss;
import nro.models.boss.BossData;
import nro.models.boss.BossesData;
import nro.models.boss.BossID;
import static nro.models.consts.BossType.HALLOWEEN_EVENT;
import nro.models.player.Player;
import nro.models.services.Service;
import nro.models.map.ItemMap;
import nro.models.consts.ConstPlayer;
import nro.models.services.EffectSkillService;
import nro.models.utils.Util;

public class HalloweenCaiTrangBoss extends Boss {

    public HalloweenCaiTrangBoss(int bossId, BossData bossData) throws Exception {
        super(HALLOWEEN_EVENT, bossId, true, true, bossData);
    }

    @Override
    public nro.models.map.Zone getMapJoin() {
        java.util.List<nro.models.map.Map> maps = nro.models.server.Manager.MAPS;
        nro.models.map.Map randomMap = null;
        for (int i = 0; i < 100; i++) {
            nro.models.map.Map m = maps.get(nro.models.utils.Util.nextInt(0, maps.size() - 1));
            if (m != null && m.zones != null && !m.zones.isEmpty() && m.mapId != 46) {
                randomMap = m;
                break;
            }
        }
        if (randomMap != null) {
            return nro.models.map.service.MapService.gI().getMapWithRandZone(randomMap.mapId);
        }
        return super.getMapJoin();
    }

    @Override
    public void attack() {
        if (Util.canDoWithTime(this.lastTimeAttack, 500) && this.typePk == ConstPlayer.PK_ALL) {
            this.lastTimeAttack = System.currentTimeMillis();
            try {
                Player pl = getPlayerAttack();
                if (pl == null || pl.isDie()) {
                    return;
                }
                int distance = Util.getDistance(this, pl);
                if (distance <= 100) {
                    if (pl.effectSkill != null && (!pl.effectSkill.isHalloween || pl.effectSkill.idOutfitHalloween != 0)) {
                        EffectSkillService.gI().setIsHalloween(pl, 0, 1800000);
                        this.chat("Hà hà! Đứng lại đó để ta hóa xương ngươi!");
                    }
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

    @Override
    public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            if (!piercing && Util.isTrue(10, 1000)) {
                this.chat("Xí hụt");
                return 0;
            }
            damage = this.nPoint.subDameInjureWithDeff(damage);
            if (damage > 1000) {
                damage = 1000;
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
    public void reward(Player plKill) {
        int[] itemIds = {1350, 1353, 1355, 1356, 1357};
        int firstIndex = Util.nextInt(0, itemIds.length - 1);
        int secondIndex = Util.nextInt(0, itemIds.length - 1);
        while (secondIndex == firstIndex) {
            secondIndex = Util.nextInt(0, itemIds.length - 1);
        }
        int[] dropIds = {itemIds[firstIndex], itemIds[secondIndex]};
        for (int itemId : dropIds) {
            ItemMap it = new ItemMap(this.zone, itemId, 1, this.location.x + Util.nextInt(-30, 30), this.zone.map.yPhysicInTop(this.location.x,
                    this.location.y - 24), plKill.id);
            Service.gI().dropItemMap(this.zone, it);
        }
    }
}
