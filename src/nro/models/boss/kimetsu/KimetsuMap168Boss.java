package nro.models.boss.kimetsu;

import nro.models.boss.Boss;
import nro.models.boss.BossData;
import nro.models.boss.BossID;
import nro.models.consts.AppearType;
import nro.models.consts.ConstPlayer;
import nro.models.skill.Skill;

/** Hai boss Spine 102/104 xuất hiện độc lập tại map 168. */
public class KimetsuMap168Boss extends Boss {

    public KimetsuMap168Boss() throws Exception {
        super(BossID.HUYET_QUY_KUROGANE,
            new BossData(
                "Hắc Quỷ Kurogane",
                ConstPlayer.TRAI_DAT,
                new short[]{508, 509, 510, -1, -1, -1},
                2_000_000,
                new int[]{30_000_000},
                new int[]{168},
                new int[][]{
                    {Skill.DRAGON, 7, 3_000}
                },
                new String[]{"|-1|Kẻ xâm nhập phải chết!"},
                new String[]{"|-1|Không ai rời khỏi nơi này!"},
                new String[]{"|-1|Ta chưa thể biến mất..."},
                60
            ),
            new BossData(
                "Xích Quỷ Enma",
                ConstPlayer.TRAI_DAT,
                new short[]{508, 509, 510, -1, -1, -1},
                2_000_000,
                new int[]{40_000_000},
                new int[]{168},
                new int[][]{
                    {Skill.DRAGON, 7, 3_000}
                },
                new String[]{"|-1|Xích Quỷ Enma đã thức tỉnh!"},
                new String[]{"|-1|Ngọn lửa quỷ sẽ thiêu rụi tất cả!"},
                new String[]{"|-1|Không thể nào..."},
                AppearType.ANOTHER_LEVEL
            ));
    }
}
