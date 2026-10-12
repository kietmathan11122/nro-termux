package nro.models.boss.kimetsu;

import nro.models.boss.Boss;
import nro.models.boss.BossData;
import nro.models.boss.BossID;
import nro.models.consts.ConstPlayer;
import nro.models.player.Player;
import nro.models.skill.Skill;

public class RestlessSoulBoss extends Boss {

    public RestlessSoulBoss() throws Exception {
        super(BossID.RESTLESS_SOUL, new BossData(
                "Oán Hồn Hyottoko",
                ConstPlayer.TRAI_DAT,
                new short[]{508, 509, 510, -1, -1, -1},
                2_000_000,
                new int[]{60_000_000},
                new int[]{170},
                new int[][]{
                    {Skill.DRAGON, 7, 3_000},
                    {Skill.TAI_TAO_NANG_LUONG, 1, 30_000}
                },
                new String[]{"|-1|Tiếng khóc của oan hồn sẽ ám lấy các ngươi!"},
                new String[]{"|-1|Chưởng lực không thể làm tổn thương ta!"},
                new String[]{"|-1|Oán niệm của ta vẫn chưa tan..."},
                60
        ));
    }

    @Override
    public synchronized int injured(Player attacker, long damage, boolean piercing, boolean isMobAttack) {
        if (attacker != null && attacker.playerSkill != null
                && attacker.playerSkill.skillSelect != null
                && attacker.playerSkill.skillSelect.template != null) {
            int skillId = attacker.playerSkill.skillSelect.template.id;
            if (skillId == Skill.KAMEJOKO || skillId == Skill.MASENKO
                    || skillId == Skill.ANTOMIC || skillId == Skill.GALICK) {
                return 1;
            }
        }
        return super.injured(attacker, damage, piercing, isMobAttack);
    }
}
