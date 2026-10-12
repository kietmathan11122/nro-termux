package nro.models.boss;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Local gameplay simulation, approved by the owner; no HP or player changes. */
public final class BossCombatSimulation {
    private static final Set<BossData> DATA = Collections.newSetFromMap(new IdentityHashMap<>());
    static {
        Collections.addAll(DATA,
            BossesData.KUKU,
            BossesData.MAP_DAU_DINH,
            BossesData.RAMBO,
            BossesData.SO_4,
            BossesData.SO_3,
            BossesData.SO_2,
            BossesData.SO_1,
            BossesData.TIEU_DOI_TRUONG,
            BossesData.SO_4_NM,
            BossesData.SO_3_NM,
            BossesData.SO_2_NM,
            BossesData.SO_1_NM,
            BossesData.TIEU_DOI_TRUONG_NM,
            BossesData.FIDE_DAI_CA_1,
            BossesData.FIDE_DAI_CA_2,
            BossesData.FIDE_DAI_CA_3,
            BossesData.DR_KORE,
            BossesData.ANDROID_19,
            BossesData.ANDROID_13,
            BossesData.ANDROID_14,
            BossesData.ANDROID_15,
            BossesData.PIC,
            BossesData.POC,
            BossesData.KING_KONG,
            BossesData.XEN_BO_HUNG_1,
            BossesData.XEN_BO_HUNG_2,
            BossesData.XEN_BO_HUNG_3,
            BossesData.XEN_CON_1,
            BossesData.XEN_CON_2,
            BossesData.XEN_CON_3,
            BossesData.XEN_CON_4,
            BossesData.XEN_CON_5,
            BossesData.XEN_CON_6,
            BossesData.XEN_CON_7,
            BossesData.BLACK_GOKU,
            BossesData.SUPER_BLACK_GOKU,
            BossesData.MABU,
            BossesData.MABU_12H,
            BossesData.SUPER_BROLY_V1,
            BossesData.SUPER_BROLY_V1_2,
            BossesData.COOLER,
            BossesData.COOLER_2,
            BossesData.SIEU_BO_HUNG_1,
            BossesData.SIEU_BO_HUNG_2,
            BossesData.GOLDEN_FRIEZA);
    }
    private BossCombatSimulation() {}

    public static int actionGapMs(Boss boss) {
        if (boss == null) return 0;
        if (boss.id == BossID.BROLY || boss.id == BossID.SUPER_BROLY) return 500;
        if (boss.data == null || boss.currentLevel < 0 || boss.currentLevel >= boss.data.length) return 0;
        return DATA.contains(boss.data[boss.currentLevel]) ? 500 : 0;
    }
}
