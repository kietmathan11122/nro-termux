package nro.models.boss.Cold;

import nro.models.boss.Boss;
import nro.models.boss.BossID;
import nro.models.boss.BossData;

public class BigGeteStar extends Boss {

    public BigGeteStar() throws Exception {
        super(BossID.BIG_GETE_STAR, new BossData(
                "Big Gete Star", // name
                (byte) 2, // gender
                new short[]{514, 515, 516, -1, -1, -1}, // outfit
                100000, // dame
                new int[]{100000000}, // hp 100 trieu
                new int[]{206}, // mapJoin
                new int[][]{
                        {nro.models.skill.Skill.DRAGON, 7, 1000},
                        {nro.models.skill.Skill.KAMEJOKO, 7, 3000},
                        {nro.models.skill.Skill.LIEN_HOAN, 7, 2000}
                }, // skillTemp
                new String[]{"|-1|Ta là Big Gete Star!"}, // textS
                new String[]{"|-1|Các ngươi không thể thắng ta!"}, // textM
                new String[]{"|-1|Chết tiệt..."}, // textE
                900 // secondsRest (15 phut)
        ));
    }

    public BigGeteStar(int mapId, int zoneId) throws Exception {
        super(BossID.BIG_GETE_STAR, new BossData(
                "Big Gete Star", // name
                (byte) 2, // gender
                new short[]{514, 515, 516, -1, -1, -1}, // outfit
                100000, // dame
                new int[]{100000000}, // hp 100 trieu
                new int[]{mapId}, // mapJoin
                new int[][]{
                        {nro.models.skill.Skill.DRAGON, 7, 1000},
                        {nro.models.skill.Skill.KAMEJOKO, 7, 3000},
                        {nro.models.skill.Skill.LIEN_HOAN, 7, 2000}
                }, // skillTemp
                new String[]{"|-1|Ta là Big Gete Star!"}, // textS
                new String[]{"|-1|Các ngươi không thể thắng ta!"}, // textM
                new String[]{"|-1|Chết tiệt..."}, // textE
                900 // secondsRest (15 phut)
        ));
        this.zoneFinal = nro.models.map.service.MapService.gI().getZoneByMapIDAndZoneID(mapId, zoneId);
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
    public void reward(nro.models.player.Player plKill) {
        if (nro.models.services.TaskService.gI().getIdTask(plKill) == nro.models.consts.ConstTask.TASK_34_3) {
            nro.models.services.TaskService.gI().doneTask(plKill, nro.models.consts.ConstTask.TASK_34_3);
        }
        if (nro.models.utils.Util.isTrue(30, 100)) { // 30%
            nro.models.map.ItemMap item = new nro.models.map.ItemMap(this.zone, 2015, 1, this.location.x, this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), plKill.id);
            nro.models.services.Service.gI().dropItemMap(this.zone, item);
        }
        
        // Rớt 20 tinh thể robot (ID 2035)
        for (int i = 0; i < 20; i++) {
            int randomX = this.location.x + nro.models.utils.Util.nextInt(-70, 70);
            int y = this.zone.map.yPhysicInTop(randomX, this.location.y - 24);
            nro.models.map.ItemMap crystal = new nro.models.map.ItemMap(this.zone, 2035, 1, randomX, y, plKill.id);
            nro.models.services.Service.gI().dropItemMap(this.zone, crystal);
        }
    }

    @Override
    public void leaveMap() {
        super.leaveMap();
        nro.models.boss.Boss_Manager.BossManager.gI().removeBoss(this);
    }
}
