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

public class Chilled extends Boss {

    public Chilled() throws Exception {
        super(BossID.CHILL_1, BossesData.CHILLED, BossesData.CHILLED_2);
    }

    @Override
    public void reward(Player plKill) {
        // Drop Cuồng Huyết (2100) from Chilled ? Or just normal drop
        if (Util.isTrue(100, 100)) {
            ItemMap it = new ItemMap(this.zone, 2100, 1, this.location.x, this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), plKill.id);
            Service.gI().dropItemMap(this.zone, it);
        }
        // Drop Băng buộc đầu thấm máu
        if (Util.isTrue(10, 100)) {
            ItemMap it = new ItemMap(this.zone, 2009, 1, this.location.x, this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), plKill.id);
            Service.gI().dropItemMap(this.zone, it);
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
