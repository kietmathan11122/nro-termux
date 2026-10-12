package nro.models.boss.Cold;

import nro.models.boss.Boss;
import nro.models.boss.BossID;
import nro.models.boss.BossData;

public class Frost3 extends Boss {

    public Frost3() throws Exception {
        super(BossID.FROST_3, new BossData(
                "Frost 3", // name
                (byte) 2, // gender
                new short[]{189, 190, 191, -1, -1, -1}, // outfit placeholder (Fide 3)
                200000, // dame
                new int[]{300000000}, // hp 300 trieu
                new int[]{188, 189}, // mapJoin
                new int[][]{
                        {nro.models.skill.Skill.DRAGON, 7, 1000},
                        {nro.models.skill.Skill.KAMEJOKO, 7, 3000},
                        {nro.models.skill.Skill.LIEN_HOAN, 7, 2000}
                }, // skillTemp
                new String[]{"|-1|Ta là Frost hình thái 3!"}, // textS
                new String[]{"|-1|Các ngươi không thể thắng ta!"}, // textM
                new String[]{"|-1|Chết tiệt..."}, // textE
                10 // secondsRest (15 phut)
        ));
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
