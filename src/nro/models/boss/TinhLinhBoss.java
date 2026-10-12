package nro.models.boss;

import nro.models.consts.ConstPlayer;
import nro.models.map.Zone;
import nro.models.map.service.MapService;
import nro.models.player.Player;
import nro.models.player.Pet;
import nro.models.services.SpiritBossUnlockService;
import nro.models.skill.Skill;

/** Boss dùng skin Spine 10163; sáu cá thể cố định ở sáu map săn Tinh Linh. */
public final class TinhLinhBoss extends Boss {

    private final int homeMapId;

    public TinhLinhBoss(int bossId, int homeMapId, int hp) throws Exception {
        super(bossId, false, false, new BossData(
                "Hộ Vệ Tinh Linh",
                ConstPlayer.TRAI_DAT,
                // 10163 - 10070 = 93; trigger Spine là 3000 + 93 - 1 = 3092.
                new short[]{3092, 0, 0, -1, -1, -1},
                Math.max(2_000, hp / 10_000),
                new int[]{hp},
                new int[]{homeMapId},
                new int[][]{
                    {Skill.KAMEJOKO, 5, 3_000},
                    {Skill.MASENKO, 5, 3_500},
                    {Skill.DRAGON, 5, 2_500}
                },
                new String[]{"|-1|Muốn có Tinh Linh gió, hãy vượt qua ta!"},
                new String[]{"|-1|Linh lực của ta chưa cạn!"},
                new String[]{"|-1|Hẹn gặp lại lần sau!"},
                15 * 60));
        this.homeMapId = homeMapId;
    }

    @Override
    public Zone getMapJoin() {
        nro.models.map.Map map = MapService.gI().getMapById(homeMapId);
        return map == null || map.zones == null || map.zones.isEmpty()
                ? null : map.zones.get(0);
    }

    @Override
    public void reward(Player plKill) {
        super.reward(plKill);
        Player recipient = plKill != null && plKill.isPet ? ((Pet) plKill).master : plKill;
        SpiritBossUnlockService.award(recipient);
    }
}
