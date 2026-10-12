package nro.models.boss.Nappa;

import nro.models.boss.Boss;
import nro.models.boss.BossID;
import nro.models.boss.BossesData;
import nro.models.map.ItemMap;
import nro.models.player.Player;
import nro.models.services.Service;
import nro.models.services.TaskService;
import nro.models.utils.Util;
import nro.models.boss.BossData;

public class Dodoria extends Boss {

    public Dodoria() throws Exception {
        super(BossID.DODORIA, BossesData.MAP_DAU_DINH);
    }
    
    public Dodoria(int mapId, int zoneId) throws Exception {
        super(BossID.DODORIA, new BossData(
                "Dodoria",
                (byte) 2, // gender
                new short[]{165, 166, 167, -1, -1, -1}, // outfit
                10000, // dame
                new int[]{150000000}, // hp
                new int[]{mapId}, // mapJoin
                new int[][]{{nro.models.skill.Skill.DRAGON, 7, 1000}}, // skillTemp
                new String[]{"|-1|Chào cưng"}, // textS
                new String[]{"|-1|Cưng sẽ phải hối hận!"}, // textM
                new String[]{"|-1|Gì cơ... sao ta lại thua..."}, // textE
                60 // secondsRest
        ));
        this.zoneFinal = nro.models.map.service.MapService.gI().getZoneByMapIDAndZoneID(mapId, zoneId);
    }

    @Override
    public void reward(Player plKill) {
        if (Util.isTrue(10, 100)) { // 10%
            Service.gI().dropItemMap(this.zone, new ItemMap(this.zone, 2009, 1,
                    this.location.x, this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), plKill.id));
        }
        if (plKill != null && nro.models.services.TaskService.gI().getIdTask(plKill) == nro.models.consts.ConstTask.TASK_32_5) {
            nro.models.services.TaskService.gI().doneTask(plKill, nro.models.consts.ConstTask.TASK_32_5);
        }
        boolean isZarbonAlive = this.zone.getBosses().stream().anyMatch(b -> b.id == nro.models.boss.BossID.ZARBON && !b.isDie());
        if (!isZarbonAlive) {
            if (this.zone.getBosses().stream().noneMatch(b -> b.id == nro.models.boss.BossID.FIDE_VANG)) {
                try {
                    new nro.models.boss.Nappa.FideVang(this.zone.map.mapId, this.zone.zoneId);
                } catch (Exception e) {}
            }
        }
    }

    @Override
    public void active() {
        super.active(); 
    }
    
    @Override
    public void joinMap() {
        super.joinMap();
    }

    @Override
    public void leaveMap() {
        super.leaveMap();
    }
}
