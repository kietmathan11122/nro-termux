package nro.models.boss.kimetsu;

import nro.models.boss.Boss;
import nro.models.boss.BossData;
import nro.models.boss.BossID;
import nro.models.consts.ConstPlayer;
import nro.models.map.service.ChangeMapService;
import nro.models.player.Player;
import nro.models.services.EffectSkillService;
import nro.models.services.ItemTimeService;
import nro.models.services.Service;
import nro.models.skill.Skill;
import nro.models.utils.Util;

public class BloodMoonDrumBoss extends Boss {

    private static final int CHOCOLATE_COOLDOWN = 30_000;
    private static final int CHOCOLATE_DURATION = 30_000;
    private long lastTimeChocolate;

    public BloodMoonDrumBoss() throws Exception {
        super(BossID.BLOOD_MOON_DRUM, new BossData(
                "Quỷ Trống Huyết Nguyệt",
                ConstPlayer.TRAI_DAT,
                new short[]{508, 509, 510, -1, -1, -1},
                2_000_000,
                new int[]{250_000_000},
                new int[]{173},
                new int[][]{
                    {Skill.DRAGON, 7, 3_000},
                    {Skill.TAI_TAO_NANG_LUONG, 1, 30_000}
                },
                new String[]{"|-1|Tiếng trống huyết nguyệt gọi các ngươi tới đây!"},
                new String[]{"|-1|Hãy hóa thành socola và quên đi sức mạnh của mình!"},
                new String[]{"|-1|Nhịp trống... đã dừng lại..."},
                60
        ));
    }

    private void turnPlayersIntoChocolate() {
        if (this.zone == null) {
            return;
        }
        for (Player player : this.zone.getNotBosses()) {
            if (player != null && !player.isDie() && Util.isTrue(1, 5)) {
                this.chat("Úm ba la xì bùa");
                EffectSkillService.gI().setSocola(player, System.currentTimeMillis(), CHOCOLATE_DURATION);
                Service.gI().Send_Caitrang(player);
                ItemTimeService.gI().sendItemTime(player, 4133, CHOCOLATE_DURATION / 1000);
            }
        }
    }

    @Override
    public void attack() {
        if (this.typePk == ConstPlayer.PK_ALL
                && Util.canDoWithTime(lastTimeChocolate, CHOCOLATE_COOLDOWN)) {
            turnPlayersIntoChocolate();
            lastTimeChocolate = System.currentTimeMillis();
        }
        super.attack();
    }

    @Override
    public void reward(Player killer) {
        super.reward(killer);
        if (killer == null || killer.isBot) {
            return;
        }
        Thread goHomeThread = new Thread(() -> {
            try {
                for (int seconds = 30; seconds > 0; seconds -= 3) {
                    if (killer.zone == null || killer.zone.map == null || killer.zone.map.mapId != 173) {
                        return;
                    }
                    Service.gI().sendThongBao(killer, "Về nhà sau " + seconds + " giây nữa");
                    Thread.sleep(3_000);
                }
                if (killer.zone != null && killer.zone.map != null
                        && killer.zone.map.mapId == 173) {
                    ChangeMapService.gI().changeMapBySpaceShip(
                            killer, killer.gender + 21, -1, 250);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (Exception ignored) {
            }
        }, "blood-moon-boss-go-home-" + killer.id);
        goHomeThread.setDaemon(true);
        goHomeThread.start();
    }
}
