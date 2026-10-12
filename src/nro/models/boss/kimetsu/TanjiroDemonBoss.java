package nro.models.boss.kimetsu;

import java.util.ArrayList;
import java.util.List;
import nro.models.boss.Boss;
import nro.models.boss.BossData;
import nro.models.boss.BossID;
import nro.models.consts.AppearType;
import nro.models.consts.ConstPlayer;
import nro.models.player.Player;
import nro.models.services.EffectSkillService;
import nro.models.skill.Skill;
import nro.models.utils.Util;

public class TanjiroDemonBoss extends Boss {

    private static final int FIRST_FORM_HP = 10_000_000;
    private static final int SECOND_FORM_HP = 15_000_000;
    private static final int FIRST_FORM_DAMAGE_LIMIT = 300_000;
    private static final int SECOND_FORM_DAMAGE_LIMIT = 200_000;

    private long lastStoneTime;
    private int nextStoneDelay;

    public TanjiroDemonBoss() throws Exception {
        super(BossID.TANJIRO_DEMON,
                new BossData(
                        "Ác Quỷ Tanjiro",
                        ConstPlayer.TRAI_DAT,
                        new short[]{508, 509, 510, -1, -1, -1},
                        2_000_000,
                        new int[]{FIRST_FORM_HP},
                        new int[]{167},
                        new int[][]{
							{Skill.DRAGON, 7, 3_000},
							{Skill.TAI_TAO_NANG_LUONG, 3, 30_000}
                        },
                        new String[]{"|-1|Các ngươi sẽ hóa thành đá!"},
                        new String[]{"|-1|Không ai thoát khỏi vùng đất quỷ."},
                        new String[]{"|-1|Ta vẫn chưa kết thúc..."},
                        60
                ),
                new BossData(
                        "Quỷ Vương Tanjiro",
                        ConstPlayer.TRAI_DAT,
                        new short[]{508, 509, 510, -1, -1, -1},
                        2_000_000,
                        new int[]{SECOND_FORM_HP},
                        new int[]{167},
                        new int[][]{
							{Skill.DRAGON, 7, 3_000},
							{Skill.TAI_TAO_NANG_LUONG, 3, 30_000}
                        },
                        new String[]{"|-1|Quỷ Vương Tanjiro đã thức tỉnh!"},
                        new String[]{"|-1|Quỳ xuống trước Quỷ Vương!"},
                        new String[]{"|-1|Không thể nào..."},
                        AppearType.ANOTHER_LEVEL
                ));
    }

    @Override
    public void initBase() {
        super.initBase();
		int formHp = currentLevel == 0 ? FIRST_FORM_HP : SECOND_FORM_HP;
		nPoint.hpg = formHp;
		nPoint.hpMax = formHp;
		nPoint.hp = formHp;
        lastStoneTime = System.currentTimeMillis();
		nextStoneDelay = Util.nextInt(30_000, 45_000);
    }

    @Override
    public void active() {
        super.active();
        petrifyRandomPlayer();
    }

    private void petrifyRandomPlayer() {
        if (zone == null || !Util.canDoWithTime(lastStoneTime, nextStoneDelay)) {
            return;
        }

        List<Player> candidates = new ArrayList<>();
        for (Player player : zone.getNotBosses()) {
            if (player != null && !player.isDie() && !player.isPet && !player.isBot
                    && player.effectSkill != null && !player.effectSkill.isStone) {
                candidates.add(player);
            }
        }

        if (!candidates.isEmpty()) {
            Player target = candidates.get(Util.nextInt(0, candidates.size() - 1));
            EffectSkillService.gI().setIsStone(target, 10_000);
            chat("Hóa đá " + target.name + " trong 10 giây!");
        }

        lastStoneTime = System.currentTimeMillis();
		nextStoneDelay = Util.nextInt(30_000, 45_000);
    }

    @Override
    public synchronized int injured(Player attacker, long damage, boolean piercing, boolean isMobAttack) {
        if (isDie()) {
            return 0;
        }
        if (attacker != null && attacker.idNRNM != -1) {
            return 1;
        }

        long damageLimit = currentLevel == 0 ? FIRST_FORM_DAMAGE_LIMIT : SECOND_FORM_DAMAGE_LIMIT;
        long appliedDamage = Math.max(1L, Math.min(damage, damageLimit));
        long hpAfterDamage = (long) nPoint.hp - appliedDamage;
        if (hpAfterDamage <= 0) {
            nPoint.hp = 0;
            setDie(attacker);
            die(attacker);
        } else {
			nPoint.hp = (int) hpAfterDamage;
        }
        return (int) appliedDamage;
    }
}
