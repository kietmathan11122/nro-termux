package nro.models.boss.kimetsu;

import nro.models.boss.Boss;
import nro.models.boss.BossData;
import nro.models.boss.BossID;
import nro.models.consts.ConstPlayer;
import nro.models.services.EffectSkillService;
import nro.models.skill.Skill;
import nro.models.utils.Util;

public class ReptileDemonBoss extends Boss {

    private static final int INVISIBLE_DURATION = 5_000;
    private static final int INVISIBLE_COOLDOWN = 20_000;
    private long lastInvisibleTime;

    public ReptileDemonBoss() throws Exception {
        super(BossID.REPTILE_DEMON, new BossData(
                "Bạo Chúa Thằn Lằn",
                ConstPlayer.TRAI_DAT,
                new short[]{508, 509, 510, -1, -1, -1},
                2_000_000,
                new int[]{50_000_000},
                new int[]{169},
                new int[][]{
                    {Skill.DRAGON, 7, 3_000},
                    {Skill.TAI_TAO_NANG_LUONG, 1, 30_000}
                },
                new String[]{"|-1|Các ngươi không thể nhìn thấy ta!"},
                new String[]{"|-1|Móng vuốt bò sát sẽ nghiền nát tất cả!"},
                new String[]{"|-1|Lớp vảy của ta..."},
                60
        ));
    }

    @Override
    public void initBase() {
        super.initBase();
        lastInvisibleTime = System.currentTimeMillis();
    }

    @Override
    public void active() {
        super.active();
        if (!effectSkill.isTanHinh && Util.canDoWithTime(lastInvisibleTime, INVISIBLE_COOLDOWN)) {
            EffectSkillService.gI().setIsTanHinh(this, INVISIBLE_DURATION);
            lastInvisibleTime = System.currentTimeMillis();
            chat("Ta đã biến mất khỏi tầm mắt của các ngươi!");
        }
    }
}
