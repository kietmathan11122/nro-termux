package nro.models.boss.kimetsu;

import nro.models.boss.Boss;
import nro.models.boss.BossData;
import nro.models.boss.BossID;
import nro.models.consts.ConstPlayer;
import nro.models.skill.Skill;

public class BloodMudDemonBoss extends Boss {

    public BloodMudDemonBoss() throws Exception {
        super(BossID.BLOOD_MUD_DEMON, new BossData(
                "Huyết Nê Quỷ",
                ConstPlayer.TRAI_DAT,
                new short[]{508, 509, 510, -1, -1, -1},
                2_000_000,
                new int[]{150_000_000},
                new int[]{172},
                new int[][]{
                    {Skill.DRAGON, 7, 3_000},
                    {Skill.TAI_TAO_NANG_LUONG, 3, 30_000}
                },
                new String[]{"|-1|Vũng bùn máu sẽ nuốt chửng tất cả!"},
                new String[]{"|-1|Không ai thoát khỏi nắm đấm của ta!"},
                new String[]{"|-1|Bùn máu... đang khô cạn..."},
                60
        ));
    }
}
