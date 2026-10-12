import java.lang.reflect.Field;
import java.util.Arrays;
import nro.models.boss.Boss;
import nro.models.boss.BossData;
import nro.models.boss.BossID;
import nro.models.boss.Boss_Manager.BossManager;
import nro.models.boss.time_planet.TimePlanetBoss;
import nro.models.consts.AppearType;
import nro.models.consts.BossStatus;

/** Factory and respawn checks without starting the server or connecting to a database. */
public class TimePlanetBossTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        int[] ids = {BossID.KAMI, BossID.OREN, BossID.KAMI_OREN,
            BossID.ULTIMATE_HEARTS, BossID.HEARTS, BossID.LAGSS};
        String[] names = {"Kami", "Oren", "Kami và Oren", "Ultimate Hearts", "Hearts", "Lagss"};
        BossManager manager = BossManager.gI();
        int before = manager.getBosses().size();
        Field restTime = Boss.class.getDeclaredField("lastTimeRest");
        restTime.setAccessible(true);
        BossData previous = null;
        for (int i = 0; i < ids.length; i++) {
            int occurrences = 0;
            for (Field field : BossID.class.getFields()) {
                if (field.getType() == int.class && field.getInt(null) == ids[i]) occurrences++;
            }
            check(occurrences == 1, "Boss ID collision: " + ids[i]);
            Boss boss = manager.createBoss(ids[i]);
            check(boss instanceof TimePlanetBoss, "Factory missing for " + names[i]);
            check(boss.id == ids[i], "Factory returned wrong ID");
            check(boss.data.length == 1, "Boss should spawn independently");
            BossData data = boss.data[0];
            check(data.getName().equals(names[i]), "Wrong name");
            check(Arrays.equals(data.getMapJoin(), new int[]{211}), "Wrong map");
            short head = (short) (2127 + i * 3);
            check(Arrays.equals(data.getOutfit(), new short[]{head, (short)(head + 1),
                (short)(head + 2), -1, -1, -1}), "Outfit differs from reference image");
            check(data.getTypeAppear() == AppearType.DEFAULT_APPEAR
                && data.getBossesAppearTogether() == null, "Unexpected linked boss");
            check(boss.isZone01SpawnDisabled && !boss.isNotifyDisabled, "Wrong spawn flags");
            check(data.getSecondsRest() == 300, "Wrong respawn delay");
            if (previous != null) {
                check(previous.getHp() != data.getHp()
                    && previous.getSkillTemp() != data.getSkillTemp(), "Boss settings share mutable arrays");
            }
            previous = data;
            restTime.setLong(boss, System.currentTimeMillis());
            boss.rest();
            check(boss.bossStatus == BossStatus.REST, "Boss respawned before delay");
            restTime.setLong(boss, System.currentTimeMillis() - 301_000);
            boss.rest();
            check(boss.bossStatus == BossStatus.RESPAWN, "Boss did not respawn after delay");
        }
        check(manager.getBosses().size() == before + 6, "Factory registered duplicate or linked bosses");
        System.out.println("PASS: 6 unique bosses, factory registration, map 211, outfits, independent settings and 5-minute respawn");
    }
}
