package nro.models.boss.fu;

import nro.models.boss.Boss;
import nro.models.boss.BossID;
import nro.models.boss.BossesData;
import nro.models.consts.BossStatus;
import nro.models.consts.ConstPlayer;
import nro.models.item.Item;
import nro.models.map.ItemMap;
import nro.models.player.Player;
import nro.models.services.EffectSkillService;
import nro.models.services.ItemService;
import nro.models.services.Service;
import nro.models.services.SkillService;
import nro.models.services.TaskService;
import nro.models.utils.Util;

public class Fu extends Boss {

    private long st;
    private int timeLeaveMap;

    public Fu() throws Exception {
        super(BossID.FU, false, true, BossesData.FU);
    }

    @Override
    public void joinMap() {
        super.joinMap();
        st = System.currentTimeMillis();
        timeLeaveMap = Util.nextInt(600000, 900000);
    }

    @Override
    public void reward(Player plKill) {
        int x = this.location.x;
        int y = this.zone.map.yPhysicInTop(x, this.location.y - 24);

        TaskService.gI().checkDoneTaskKillBoss(plKill, this);

        // 30% drop Godly Item (randDoTLBoss)
        if (Util.isTrue(30, 100)) {
            ItemMap it = ItemService.gI().randDoTLBoss(this.zone, 1, x, y, plKill.id);
            if (it != null) {
                Service.gI().dropItemMap(zone, it);
            }
        } else {
            // 70% drop Custom item (like Cumber)
            int[] dropItems = {
                241, 253, 265, 277,
                233, 245, 257, 269,
                237, 249, 261, 273,
                281
            };

            int itemId = dropItems[Util.nextInt(dropItems.length)];
            ItemMap it = new ItemMap(zone, itemId, 1, x, y, plKill.id);

            int rd = Util.nextInt(100);
            int sao;
            if (rd < 70) {
                sao = 0;
            } else if (rd < 95) {
                sao = 1;
            } else {
                sao = 2;
            }
            it.options.add(new Item.ItemOption(107, sao));

            switch (itemId) {
                case 241:
                case 233:
                case 237:
                    it.options.add(new Item.ItemOption(47, Util.nextInt(400, 550)));
                    break;
                case 253:
                case 245:
                case 249:
                    it.options.add(new Item.ItemOption(6, Util.nextInt(22000, 27000)));
                    it.options.add(new Item.ItemOption(27, Util.nextInt(3000, 5000)));
                    break;
                case 265:
                case 261:
                case 257:
                    it.options.add(new Item.ItemOption(0, Util.nextInt(2100, 2400)));
                    break;
                case 277:
                case 269:
                case 273:
                    it.options.add(new Item.ItemOption(7, Util.nextInt(22000, 26000)));
                    it.options.add(new Item.ItemOption(28, Util.nextInt(4000, 6000)));
                    break;
                case 281:
                    it.options.add(new Item.ItemOption(14, Util.nextInt(11, 13)));
                    break;
            }
            Service.gI().dropItemMap(zone, it);
        }
    }

    @Override
    public void die(Player plKill) {
        this.chat("Ta sẽ đưa Cumber đi hahaha");
        super.die(plKill);
    }

    @Override
    public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            if (!piercing && Util.isTrue(this.nPoint.tlNeDon, 1000)) {
                this.chat("Xí hụt");
                return 0;
            }
            damage = this.nPoint.subDameInjureWithDeff(damage - Util.nextInt(100000));
            if (!piercing && effectSkill.isShielding) {
                if (damage > nPoint.hpMax) {
                    EffectSkillService.gI().breakShield(this);
                }
                damage = 1;
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
    public void autoLeaveMap() {
        if (Util.canDoWithTime(st, timeLeaveMap)) {
            if (Util.isTrue(1, 2)) {
                this.leaveMap();
            } else {
                this.leaveMapNew();
            }
        }
        if (this.zone != null && this.zone.getNumOfPlayers() > 0) {
            st = System.currentTimeMillis();
            timeLeaveMap = Util.nextInt(300000, 900000);
        }
    }

    @Override
    public void attack() {
        if (Util.canDoWithTime(this.lastTimeAttack, 100) && this.typePk == ConstPlayer.PK_ALL) {
            this.lastTimeAttack = System.currentTimeMillis();
            try {
                Player pl = getPlayerAttack();
                if (pl == null || pl.isDie()) {
                    return;
                }
                this.playerSkill.skillSelect = this.playerSkill.skills.get(Util.nextInt(0, this.playerSkill.skills.size() - 1));
                int dis = Util.getDistance(this, pl);
                if (dis > 450) {
                    move(pl.location.x - 24, pl.location.y);
                } else if (dis > 100) {
                    int dir = (this.location.x - pl.location.x < 0 ? 1 : -1);
                    int move = Util.nextInt(50, 100);
                    move(this.location.x + (dir == 1 ? move : -move), pl.location.y);
                } else {
                    if (Util.isTrue(30, 100)) {
                        int move = Util.nextInt(50);
                        move(pl.location.x + (Util.nextInt(0, 1) == 1 ? move : -move), this.location.y);
                    }
                    SkillService.gI().useSkill(this, pl, null, -1, null);
                    checkPlayerDie(pl);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
