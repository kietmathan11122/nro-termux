package nro.models.boss.Cold;

import nro.models.boss.Boss;
import nro.models.boss.BossID;
import nro.models.boss.BossData;

public class Frost1 extends Boss {

    public Frost1() throws Exception {
        super(BossID.FROST_1, new BossData(
                "Frost 1", // name
                (byte) 2, // gender
                new short[]{183, 184, 185, -1, -1, -1}, // outfit placeholder (Fide 1)
                100000, // dame
                new int[]{100000000}, // hp 100 trieu
                new int[]{205}, // mapJoin
                new int[][]{
                        {nro.models.skill.Skill.DRAGON, 7, 1000},
                        {nro.models.skill.Skill.KAMEJOKO, 7, 3000},
                        {nro.models.skill.Skill.LIEN_HOAN, 7, 2000}
                }, // skillTemp
                new String[]{"|-1|Ta là Frost hình thái 1!"}, // textS
                new String[]{"|-1|Các ngươi không thể thắng ta!"}, // textM
                new String[]{"|-1|Chết tiệt..."}, // textE
                900 // secondsRest (15 phut)
        ),
        new BossData(
                "Frost 2", // name
                (byte) 2, // gender
                new short[]{186, 187, 188, -1, -1, -1}, // outfit placeholder (Fide 2)
                150000, // dame
                new int[]{150000000}, // hp 150 trieu
                new int[]{205}, // mapJoin
                new int[][]{
                        {nro.models.skill.Skill.DRAGON, 7, 1000},
                        {nro.models.skill.Skill.KAMEJOKO, 7, 3000},
                        {nro.models.skill.Skill.LIEN_HOAN, 7, 2000}
                }, // skillTemp
                new String[]{"|-1|Ta là Frost hình thái 2!"}, // textS
                new String[]{"|-1|Sức mạnh của ta đã tăng lên gấp bội!"}, // textM
                new String[]{"|-1|Không thể tin được..."}, // textE
                900 // secondsRest (15 phut)
        ),
        new BossData(
                "Frost 3", // name
                (byte) 2, // gender
                new short[]{189, 190, 191, -1, -1, -1}, // outfit placeholder (Fide 3)
                200000, // dame
                new int[]{200000000}, // hp 200 trieu
                new int[]{205}, // mapJoin
                new int[][]{
                        {nro.models.skill.Skill.DRAGON, 7, 1000},
                        {nro.models.skill.Skill.KAMEJOKO, 7, 3000},
                        {nro.models.skill.Skill.LIEN_HOAN, 7, 2000}
                }, // skillTemp
                new String[]{"|-1|Ta là Frost hình thái 3!"}, // textS
                new String[]{"|-1|Các ngươi sẽ phải trả giá!"}, // textM
                new String[]{"|-1|Ta... ta thua rồi sao..."}, // textE
                900 // secondsRest (15 phut)
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
}
