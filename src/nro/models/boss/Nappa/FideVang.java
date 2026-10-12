package nro.models.boss.Nappa;

import nro.models.boss.Boss;
import nro.models.boss.BossID;
import nro.models.boss.BossesData;
import nro.models.map.ItemMap;
import nro.models.player.Player;
import nro.models.services.Service;
import nro.models.utils.Util;
import nro.models.boss.BossData;

public class FideVang extends Boss {

    public FideVang(int mapId, int zoneId) throws Exception {
        super(BossID.FIDE_VANG, new BossData(
                "Fide Vàng",
                (byte) 2, // gender
                new short[]{502, 503, 504, -1, -1, -1}, // outfit
                15000, // dame
                new int[]{250000000}, // hp
                new int[]{mapId}, // mapJoin
                new int[][]{{nro.models.skill.Skill.DRAGON, 7, 1000}}, // skillTemp
                new String[]{"|-1|Ngươi dám đến đây sao!"}, // textS
                new String[]{"|-1|Ta sẽ cho các ngươi biết thế nào là địa ngục!"}, // textM
                new String[]{"|-1|Không thể nào..."}, // textE
                60 // secondsRest
        ));
        this.zoneFinal = nro.models.map.service.MapService.gI().getZoneByMapIDAndZoneID(mapId, zoneId);
    }

    @Override
    public void reward(Player plKill) {
        if (this.zone.map.mapId == 203 || this.zone.map.mapId == 225) {
            Service.gI().dropItemMap(this.zone, new ItemMap(this.zone, 2009, 1,
                    this.location.x, this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), plKill.id));
        }
        if (plKill != null && nro.models.services.TaskService.gI().getIdTask(plKill) == nro.models.consts.ConstTask.TASK_32_5) {
            nro.models.services.TaskService.gI().doneTask(plKill, nro.models.consts.ConstTask.TASK_32_5);
        }
        
        boolean isDodoriaAlive = this.zone.getBosses().stream().anyMatch(b -> b.id == nro.models.boss.BossID.DODORIA && !b.isDie());
        boolean isZarbonAlive = this.zone.getBosses().stream().anyMatch(b -> b.id == nro.models.boss.BossID.ZARBON && !b.isDie());
        
        if (!isDodoriaAlive && !isZarbonAlive) {
            Service.gI().sendThongBao(plKill, "Bạn sẽ về nhà sau 10 giây nữa!");
            new Thread(() -> {
                try {
                    Thread.sleep(10000);
                    if (plKill != null && plKill.zone != null && (plKill.zone.map.mapId == 225 || plKill.zone.map.mapId == 203)) {
                        nro.models.map.service.ChangeMapService.gI().changeMapBySpaceShip(plKill, plKill.gender + 21, -1, 250);
                    }
                } catch (Exception e) {}
            }).start();
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
