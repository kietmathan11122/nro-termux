package nro.models.boss.event_co_hon;

import nro.models.boss.Boss;
import nro.models.boss.BossData;
import nro.models.boss.BossID;
import nro.models.map.service.ChangeMapService;
import nro.models.player.Player;
import nro.models.services.CoHonEventService;
import nro.models.services.Service;
import nro.models.skill.Skill;
import nro.models.utils.Util;

public class HonMaCoHon extends Boss {

    private final Player summoner;
    private final boolean friendly;
    private boolean resolved;
    private long resolvedAt;

    public HonMaCoHon(Player summoner, boolean friendly) throws Exception {
        super(BossID.HON_MA_CO_HON, new BossData(
                "Hồn Ma",
                (byte) 1,
                new short[]{3288, 3289, 3290, -1, -1, -1},
                2_000_000_000,
                new int[]{10_000_000},
                new int[]{summoner.zone.map.mapId},
                new int[][]{{Skill.DEMON, 7, 1000}},
                new String[]{friendly ? "|-1|Mùi cháo này... thật ấm áp." : "|-1|Ngươi gọi ta đến để nộp mạng sao?"},
                new String[]{},
                new String[]{"|-1|Ta sẽ trở lại..."},
                1));
        this.summoner = summoner;
        this.friendly = friendly;
        this.zoneFinal = summoner.zone;
    }

    @Override
    public void joinMap() {
        super.joinMap();
        if (zone != null && summoner != null && summoner.zone == zone) {
            int x = summoner.location.x + 40;
            int y = zone.map.yPhysicInTop(x, summoner.location.y) - 20;
            ChangeMapService.gI().changeMap(this, zone, x, y);
        }
    }

    @Override
    protected void notifyJoinMap() {
        // Hồn ma triệu hồi riêng không phát thông báo toàn server.
    }

    @Override
    public Player getPlayerAttack() {
        return summoner != null && summoner.zone == zone && !summoner.isDie() ? summoner : null;
    }

    @Override
    public void active() {
        if (!resolved) {
            resolved = true;
            resolvedAt = System.currentTimeMillis();
            if (friendly) {
                chat("Cảm ơn bát cháo. Ta để lại chút lễ vật cho ngươi.");
                CoHonEventService.gI().rewardSummoner(summoner);
            } else if (summoner != null && summoner.zone == zone && !summoner.isDie()) {
                chat("Cháo không đủ làm ta no. Hãy đưa sinh khí của ngươi đây!");
                long lethalDamage = Math.max(summoner.nPoint.hpMax, summoner.nPoint.hp) + 1;
                summoner.injured(this, lethalDamage, true, false);
                Service.gI().Send_Info_NV(summoner);
            }
            return;
        }
        if (Util.canDoWithTime(resolvedAt, 3000)) {
            leaveMapNew();
        }
    }

    @Override
    public void leaveMap() {
        super.leaveMap();
        nro.models.boss.Boss_Manager.BossManager.gI().removeBoss(this);
    }
}
