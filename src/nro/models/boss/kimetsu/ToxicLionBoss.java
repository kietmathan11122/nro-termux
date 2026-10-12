package nro.models.boss.kimetsu;

import nro.models.boss.Boss;
import nro.models.boss.BossData;
import nro.models.boss.BossID;
import nro.models.consts.ConstPlayer;
import nro.models.skill.Skill;

public class ToxicLionBoss extends Boss {

    public ToxicLionBoss() throws Exception {
        super(BossID.TOXIC_LION, new BossData(
                "Độc Sư Tử Vương",
                ConstPlayer.TRAI_DAT,
                new short[]{508, 509, 510, -1, -1, -1},
                2_000_000,
                new int[]{80_000_000},
                new int[]{171},
                new int[][]{
                    {Skill.DRAGON, 7, 3_000},
                    {Skill.TAI_TAO_NANG_LUONG, 3, 30_000}
                },
                new String[]{"|-1|Nọc độc của ta sẽ bao phủ vùng đất này!"},
                new String[]{"|-1|Móng vuốt sư tử sẽ xé nát các ngươi!"},
                new String[]{"|-1|Độc khí của ta... chưa thể tan biến..."},
                60
        ));
    }
}
