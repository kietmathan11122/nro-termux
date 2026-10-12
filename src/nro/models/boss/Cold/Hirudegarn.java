package nro.models.boss.Cold;

import nro.models.boss.Boss;
import nro.models.boss.BossID;
import nro.models.boss.BossData;
import nro.models.player.Player;
import nro.models.utils.Util;

public class Hirudegarn extends Boss {

    public Hirudegarn() throws Exception {
        super(BossID.HIRUDEGARN, new BossData(
                "Hirudegarn", // name
                (byte) 2, // gender
                new short[]{400, 401, 402, -1, -1, -1}, // outfit placeholder (Hirudegarn)
                200000, // dame
                new int[]{500000000}, // hp 500 trieu
                new int[]{205}, // mapJoin
                new int[][]{
                        {nro.models.skill.Skill.DRAGON, 7, 1000},
                        {nro.models.skill.Skill.KAMEJOKO, 7, 3000},
                        {nro.models.skill.Skill.LIEN_HOAN, 7, 2000}
                }, // skillTemp
                new String[]{"|-1|Ngươi dám đánh thức ta sao!"}, // textS
                new String[]{"|-1|Ta sẽ nghiền nát tất cả!"}, // textM
                new String[]{"|-1|Không thể nào... ta là Hirudegarn vô đối..."}, // textE
                900 // secondsRest (15 phut)
        ));
    }

    public Hirudegarn(int mapId, int zoneId) throws Exception {
        super(BossID.HIRUDEGARN, new BossData(
                "Hirudegarn", // name
                (byte) 2, // gender
                new short[]{400, 401, 402, -1, -1, -1}, // outfit placeholder (Hirudegarn)
                200000, // dame
                new int[]{500000000}, // hp 500 trieu
                new int[]{mapId}, // mapJoin
                new int[][]{
                        {nro.models.skill.Skill.DRAGON, 7, 1000},
                        {nro.models.skill.Skill.KAMEJOKO, 7, 3000},
                        {nro.models.skill.Skill.LIEN_HOAN, 7, 2000}
                }, // skillTemp
                new String[]{"|-1|Ngươi dám đánh thức ta sao!"}, // textS
                new String[]{"|-1|Ta sẽ nghiền nát tất cả!"}, // textM
                new String[]{"|-1|Không thể nào... ta là Hirudegarn vô đối..."}, // textE
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
    public void leaveMap() {
        super.leaveMap();
        nro.models.boss.Boss_Manager.BossManager.gI().removeBoss(this);
    }
}
