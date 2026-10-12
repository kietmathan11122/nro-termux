package nro.models.boss.trai_dat;

import nro.models.boss.Boss;
import nro.models.boss.BossData;
import nro.models.boss.BossID;
import nro.models.boss.BossesData;
import nro.models.map.ItemMap;
import nro.models.player.Player;
import nro.models.services.Service;
import nro.models.consts.BossStatus;
import static nro.models.consts.BossType.PHOBAN;
import nro.models.boss.Boss_Manager.OtherBossManager;

public class RongTaAc extends Boss {

    private Player playerAtt;
    private int timeLive;
    private long lastUpdate = System.currentTimeMillis();

    public RongTaAc(Player player) throws Exception {
        super(PHOBAN, BossID.RONG_TA_AC, BossesData.RONG_TA_AC);
        this.playerAtt = player;
        
        this.currentLevel = 0;
        this.initBase();
        
        this.zone = player.zone;
        this.location.x = player.location.x + 50;
        this.location.y = player.location.y;
        
        this.timeLive = 300;
        this.typePk = nro.models.consts.ConstPlayer.PK_ALL;
        
        this.changeStatus(BossStatus.JOIN_MAP);
    }

    @Override
    public void active() {
        super.active();
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
        super.leaveMap();
        OtherBossManager.gI().removeBoss(this);
    }
    
    @Override
    public void update() {
        super.update();
        if (System.currentTimeMillis() - lastUpdate > 1000) {
            lastUpdate = System.currentTimeMillis();
            if (timeLive > 0) {
                timeLive--;
            } else {
                super.leaveMap();
                OtherBossManager.gI().removeBoss(this);
            }
        }
    }
    
    @Override
    public void die(nro.models.player.Player plKill) {
        if (plKill != null) {
            // Rớt ngẫu nhiên 1 trong các Ngọc Rồng Tím (ID 2039 - 2045) với tỷ lệ 10%
            if (nro.models.utils.Util.isTrue(10, 100)) {
                int randomBallId = nro.models.utils.Util.nextInt(2039, 2045);
                nro.models.map.ItemMap itBall = new nro.models.map.ItemMap(this.zone, randomBallId, 1, this.location.x, this.zone.map.yPhysicInTop(this.location.x, this.location.y), plKill.id);
                nro.models.services.Service.gI().dropItemMap(this.zone, itBall);
                nro.models.services.Service.gI().sendThongBao(plKill, "Rồng Tà Ác đã rơi ra " + itBall.itemTemplate.name + "!");
            }
            if (plKill.playerTask != null && plKill.playerTask.taskMain != null && plKill.playerTask.taskMain.id == 35) {
                if (plKill.playerTask.taskMain.index == 5) {
                    nro.models.services.TaskService.gI().doneTask(plKill, nro.models.consts.ConstTask.TASK_35_5);
                }
                if (plKill.playerTask.taskMain.index == 6) {
                    if (nro.models.utils.Util.isTrue(30, 100)) {
                        // Rớt Lõi Ngọc Rồng Đen (ID: 2019) với tỷ lệ 30%
                        nro.models.map.ItemMap it = new nro.models.map.ItemMap(this.zone, 2019, 1, this.location.x, this.zone.map.yPhysicInTop(this.location.x, this.location.y), plKill.id);
                        nro.models.services.Service.gI().dropItemMap(this.zone, it);
                        nro.models.services.Service.gI().sendThongBao(plKill, "Rồng Tà Ác đã rơi ra Lõi Ngọc Rồng Đen!");
                    } else {
                        nro.models.services.Service.gI().sendThongBao(plKill, "Rất tiếc! Rồng Tà Ác không rớt Lõi Ngọc Rồng Đen lần này.");
                    }
                }
            }
        }
        super.die(plKill);
    }
}
