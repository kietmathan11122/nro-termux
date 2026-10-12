package nro.models.boss.Cold;

import nro.models.boss.Boss;
import nro.models.boss.BossData;
import nro.models.boss.BossID;
import nro.models.boss.Boss_Manager.BossManager;
import nro.models.boss.BossesData;
import nro.models.map.ItemMap;
import nro.models.player.Player;
import nro.models.services.Service;
import nro.models.services.TaskService;
import nro.models.utils.Util;

public class ChilledPast extends Boss {

    public ChilledPast() throws Exception {
        super(BossID.CHILL_PAST_1, BossesData.CHILLED_PAST, BossesData.CHILLED_PAST_2);
    }

    @Override
    public void reward(Player plKill) {
        // Drop Cuồng Huyết (2100) from Chilled ? Or just normal drop
        if (Util.isTrue(100, 100)) {
            ItemMap it = new ItemMap(this.zone, 2100, 1, this.location.x, this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), plKill.id);
            Service.gI().dropItemMap(this.zone, it);
        }
        // Drop Băng buộc đầu thắm máu 100% on maps 202/198
        ItemMap it = new ItemMap(this.zone, 2009, 1, this.location.x, this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), plKill.id);
        Service.gI().dropItemMap(this.zone, it);
        
        if (plKill != null) {
            if (this.currentLevel == 0) {
                if (nro.models.services.TaskService.gI().getIdTask(plKill) == nro.models.consts.ConstTask.TASK_32_6) {
                    nro.models.services.TaskService.gI().doneTask(plKill, nro.models.consts.ConstTask.TASK_32_6);
                    Service.gI().sendThongBao(plKill, "Bạn đã tiêu diệt Chilled 1! Hãy tiếp tục tiêu diệt Chilled 2.");
                }
            } else if (this.currentLevel == 1) {
                if (nro.models.services.TaskService.gI().getIdTask(plKill) == nro.models.consts.ConstTask.TASK_32_7) {
                    nro.models.services.TaskService.gI().doneTask(plKill, nro.models.consts.ConstTask.TASK_32_7);
                    Service.gI().sendThongBao(plKill, "Nhiệm vụ hoàn thành! Hãy về báo cáo cho Bardock để nhận thưởng.");
                }
            }
        }
        TaskService.gI().checkDoneTaskKillBoss(plKill, this);
    }

    @Override
    public void active() {
        super.active();
    }

    @Override
    public void joinMap() {
        super.joinMap();
    }


}
