package nro.models.boss.Boss_Manager;

import nro.models.boss.Android.Android13;
import nro.models.boss.Android.Android14;
import nro.models.boss.Android.Android15;
import nro.models.boss.Android.Android19;
import nro.models.boss.Android.DrKore;
import nro.models.boss.Android.KingKong;
import nro.models.boss.Android.Pic;
import nro.models.boss.Android.Poc;
import nro.models.boss.Black_Goku.BlackGoku;
import nro.models.boss.Boss;
import nro.models.boss.BossID;
import nro.models.boss.Boss_mini.AnTrom;
import nro.models.boss.Boss_mini.BotRati;
import nro.models.boss.Boss_mini.QuySanta;
import nro.models.boss.Boss_mini.Odo;
import nro.models.boss.Boss_mini.RongNhi;
import nro.models.boss.Boss_mini.SoiHecQuyn;
import nro.models.boss.Boss_mini.Virut;
import nro.models.boss.Broly.Broly;
import nro.models.boss.Broly.SuperBrolyV1;
import nro.models.boss.Cell.SieuBoHung;
import nro.models.boss.Cell.XENCON1;
import nro.models.boss.Cell.XENCON2;
import nro.models.boss.Cell.XENCON3;
import nro.models.boss.Cell.XENCON4;
import nro.models.boss.Cell.XENCON5;
import nro.models.boss.Cell.XENCON6;
import nro.models.boss.Cell.XENCON7;
import nro.models.boss.Cell.XenBoHung;
import nro.models.boss.Cold.Cooler;
import nro.models.boss.trai_dat.BIDO;
import nro.models.boss.trai_dat.BOJACK;
import nro.models.boss.trai_dat.BUJIN;
import nro.models.boss.trai_dat.KOGU;
import nro.models.boss.trai_dat.SUPER_BOJACK;
import nro.models.boss.trai_dat.ZANGYA;
import nro.models.boss.Frieza.Fide;
import nro.models.boss.Golden_fireza.DeathBeam1;
import nro.models.boss.Golden_fireza.DeathBeam2;
import nro.models.boss.Golden_fireza.DeathBeam3;
import nro.models.boss.Golden_fireza.DeathBeam4;
import nro.models.boss.Golden_fireza.DeathBeam5;
import nro.models.boss.Golden_fireza.GoldenFrieza;
import nro.models.boss.MajinBuu_12h.BuiBui;
import nro.models.boss.MajinBuu_12h.BuiBui2;
import nro.models.boss.MajinBuu_12h.Cadic;
import nro.models.boss.MajinBuu_12h.Drabura;
import nro.models.boss.MajinBuu_12h.Drabura2;
import nro.models.boss.MajinBuu_12h.Drabura3;
import nro.models.boss.MajinBuu_12h.Goku;
import nro.models.boss.MajinBuu_12h.Mabu;
import nro.models.boss.MajinBuu_12h.Yacon;
import nro.models.boss.MajinBuu_14h.Mabu2H;
import nro.models.boss.MajinBuu_14h.SuperBu;
import nro.models.boss.tieu_doi_sat_thu_namek.SO1_NM;
import nro.models.boss.tieu_doi_sat_thu_namek.SO2_NM;
import nro.models.boss.tieu_doi_sat_thu_namek.SO3_NM;
import nro.models.boss.tieu_doi_sat_thu_namek.SO4_NM;
import nro.models.boss.tieu_doi_sat_thu_namek.TDT_NM;
import nro.models.boss.Nappa.Kuku;
import nro.models.boss.Nappa.Ku;
import nro.models.boss.Nappa.MapDauDinh;
import nro.models.boss.Nappa.Rambo;
import nro.models.boss.tieu_doi_sat_thu.SO1;
import nro.models.boss.tieu_doi_sat_thu.SO2;
import nro.models.boss.tieu_doi_sat_thu.SO3;
import nro.models.boss.tieu_doi_sat_thu.SO4;
import nro.models.boss.tieu_doi_sat_thu.TDT;
import nro.models.boss.Tau_PayPay.TaoPaiPai;
// import nro.models.boss.yardrat.CHIENBINH0;
// import nro.models.boss.yardrat.CHIENBINH1;
// import nro.models.boss.yardrat.CHIENBINH2;
// import nro.models.boss.yardrat.CHIENBINH3;
// import nro.models.boss.yardrat.CHIENBINH4;
// import nro.models.boss.yardrat.CHIENBINH5;
// import nro.models.boss.yardrat.DOITRUONG5;
// import nro.models.boss.yardrat.TANBINH0;
// import nro.models.boss.yardrat.TANBINH1;
// import nro.models.boss.yardrat.TANBINH2;
// import nro.models.boss.yardrat.TANBINH3;
// import nro.models.boss.yardrat.TANBINH4;
// import nro.models.boss.yardrat.TANBINH5;
// import nro.models.boss.yardrat.TAPSU0;
// import nro.models.boss.yardrat.TAPSU1;
// import nro.models.boss.yardrat.TAPSU2;
// import nro.models.boss.yardrat.TAPSU3;
// import nro.models.boss.yardrat.TAPSU4;
import nro.models.boss.event.Halloween.BiMa;
import nro.models.boss.event.Halloween.Doi;
import nro.models.boss.event.Halloween.MaTroi;
import nro.models.boss.event.Halloween.HalloweenCaiTrangBoss;
import nro.models.boss.BossesData;
import nro.models.boss.event_hung_vuong.SonTinh;
import nro.models.boss.event_hung_vuong.ThuyTinh;
import nro.models.boss.event_trung_thu.KhiDot;
import nro.models.boss.event_trung_thu.NguyetThan;
import nro.models.boss.event_trung_thu.NhatThan;
import nro.models.boss.event_tet.LanCon;
import nro.models.boss.event_noel.OngGiaNoel;
import nro.models.player.Player;
import nro.models.network.Message;
import nro.models.map.service.MapService;
import nro.models.map.service.ChangeMapService;
import nro.models.consts.BossStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import nro.models.boss.Baby.Baby;
import nro.models.boss.Baby.B;
import nro.models.boss.Boss_mini.MatTroi;
import nro.models.boss.cumber.Cumber;
import nro.models.boss.cumber.GokuSSJ4;
import nro.models.boss.cumber.VegetaSSJ4;
import nro.models.boss.cumber.GogetaSSJ4;
import nro.models.boss.fu.Fu;
import nro.models.boss.time_planet.TimePlanetBoss;
import nro.models.boss.time_planet.KamiOrenBoss;
import nro.models.map.Zone;
import nro.models.mob_bigboss.GauTuongCuop;
import nro.models.server.Maintenance;
import nro.models.server.ServerManager;
import nro.models.utils.Functions;
import nro.models.utils.Logger;

public class BossManager implements Runnable {

    private static BossManager instance;
    public static byte ratioReward = 10;

    public static BossManager gI() {
        if (instance == null) {
            instance = new BossManager();
        }
        return instance;
    }

    public BossManager() {
        this.bosses = new CopyOnWriteArrayList<>();
    }

    protected final List<Boss> bosses;
    private long lastUpdateErrorLog;

    public List<Boss> getBosses() {
        return this.bosses;
    }

    public void addBoss(Boss boss) {
        this.bosses.add(boss);
    }

    public void removeBoss(Boss boss) {
        this.bosses.remove(boss);
    }

    public synchronized Boss replaceBoss(int bossId) {
        int[] groupIds = getReplacementGroupIds(bossId);
        List<Boss> currentBosses = new ArrayList<>(this.bosses);
        for (Boss boss : new ArrayList<>(BrolyManager.gI().getBosses())) {
            if (!currentBosses.contains(boss)) {
                currentBosses.add(boss);
            }
        }
        for (Boss boss : currentBosses) {
            if (!containsBossId(groupIds, boss.id)) {
                continue;
            }
            if (boss.zone != null) {
                ChangeMapService.gI().exitMap(boss);
            }
            boss.changeStatus(BossStatus.REST);
            this.bosses.remove(boss);
            BrolyManager.gI().removeBoss(boss);
        }
        Boss boss = createBoss(getReplacementSpawnId(bossId));
        if (boss != null) {
            boss.changeStatus(BossStatus.RESPAWN);
            boss.update();
            boss.update();
            for (Boss turnBoss : new ArrayList<>(this.bosses)) {
                if (containsBossId(groupIds, turnBoss.id) && turnBoss.bossStatus == BossStatus.RESPAWN) {
                    turnBoss.update();
                    turnBoss.update();
                }
            }
        }
        return boss;
    }

    private int getReplacementSpawnId(int bossId) {
        if (bossId == BossID.KAMI || bossId == BossID.OREN || bossId == BossID.KAMI_OREN) {
            return BossID.KAMI;
        }
        if (bossId == BossID.SO_1 || bossId == BossID.SO_2 || bossId == BossID.SO_3
                || bossId == BossID.SO_4 || bossId == BossID.TIEU_DOI_TRUONG) {
            return BossID.TIEU_DOI_TRUONG;
        }
        if (bossId == BossID.SO_1_NM || bossId == BossID.SO_2_NM || bossId == BossID.SO_3_NM
                || bossId == BossID.SO_4_NM || bossId == BossID.TIEU_DOI_TRUONG_NM) {
            return BossID.TIEU_DOI_TRUONG_NM;
        }
        return bossId;
    }

    private int[] getReplacementGroupIds(int bossId) {
        if (bossId == BossID.KAMI || bossId == BossID.OREN || bossId == BossID.KAMI_OREN) {
            return new int[]{BossID.KAMI, BossID.OREN, BossID.KAMI_OREN};
        }
        if (bossId == BossID.SO_1 || bossId == BossID.SO_2 || bossId == BossID.SO_3
                || bossId == BossID.SO_4 || bossId == BossID.TIEU_DOI_TRUONG) {
            return new int[]{BossID.SO_1, BossID.SO_2, BossID.SO_3, BossID.SO_4, BossID.TIEU_DOI_TRUONG};
        }
        if (bossId == BossID.SO_1_NM || bossId == BossID.SO_2_NM || bossId == BossID.SO_3_NM
                || bossId == BossID.SO_4_NM || bossId == BossID.TIEU_DOI_TRUONG_NM) {
            return new int[]{BossID.SO_1_NM, BossID.SO_2_NM, BossID.SO_3_NM, BossID.SO_4_NM, BossID.TIEU_DOI_TRUONG_NM};
        }
        return new int[]{bossId};
    }

    private boolean containsBossId(int[] bossIds, long bossId) {
        for (int id : bossIds) {
            if (id == bossId) {
                return true;
            }
        }
        return false;
    }

    public void loadBoss() {
        this.createBoss(BossID.ANIME_AINZ);
        this.createBoss(BossID.ANIME_FATHER);
        this.createBoss(BossID.ANIME_ALL_FOR_ONE);
        this.createBoss(BossID.ANIME_KATANA_MAN);
        this.createBoss(BossID.ANIME_BOROS);
        this.createBoss(BossID.ANIME_KANEKI);
        this.createBoss(BossID.ANIME_IGRIS);
        this.createBoss(BossID.ANIME_DEMON_KING);
        this.createBoss(BossID.ANIME_ZODD);
        this.createBoss(BossID.ANIME_YOR);
        this.createBoss(BossID.TIEU_DOI_TRUONG);
        this.createBoss(BossID.TIEU_DOI_TRUONG_NM);
        this.createBoss(BossID.BOJACK);
        this.createBoss(BossID.SUPER_BOJACK);
        this.createBoss(BossID.KING_KONG);
        this.createBoss(BossID.XEN_BO_HUNG, 1);
        this.createBoss(BossID.SIEU_BO_HUNG, 1);
        this.createBoss(BossID.KUKU, 2);
          this.createBoss(BossID.KU, 1);
        this.createBoss(BossID.MAP_DAU_DINH, 1);
        this.createBoss(BossID.RAMBO, 1);
        this.createBoss(BossID.FIDE);
        this.createBoss(BossID.ANDROID_14);
        this.createBoss(BossID.DR_KORE);
        this.createBoss(BossID.CUMBER);
        this.createBoss(BossID.CHILL_1);
        this.createBoss(BossID.CHILL_PAST_1, 2);
        this.createBoss(BossID.COOLER, 1);
        this.createBoss(BossID.SUPER_BROLY_V1, 1);
        this.createBoss(BossID.BLACK_GOKU, 2);
        // this.createBoss(BossID.GOLDEN_FRIEZA, 1);
        this.createBoss(BossID.SOI_HEC_QUYN1, 5);
        this.createBoss(BossID.AN_TROM, 3);
        this.createBoss(BossID.BOT_RATI, 3);
        this.createBoss(BossID.QUY_SANTA, 3);
        this.createBoss(BossID.O_DO1, 1);
        this.createBoss(BossID.BABY, 1);
             this.createBoss(BossID.B, 1);
        this.createBoss(BossID.MAT_TROI, 3);
        // this.createBoss(BossID.HIRUDEGARN, 1);
        this.createBoss(BossID.FROST_1, 1);
        this.createBoss(BossID.TANJIRO_DEMON);
		this.createBoss(BossID.HUYET_QUY_KUROGANE);
		this.createBoss(BossID.REPTILE_DEMON);
		this.createBoss(BossID.RESTLESS_SOUL);
		this.createBoss(BossID.TOXIC_LION);
        this.createBoss(BossID.BLOOD_MUD_DEMON);
		this.createBoss(BossID.BLOOD_MOON_DRUM);
        this.createBoss(BossID.BULMA_PHU_THUY_BAMBOO);
        this.createBoss(BossID.BULMA_PHU_THUY_DUONG_XI);
        this.createBoss(BossID.DORAEMON);
        this.createBoss(BossID.SHIZUKA);
        this.createBoss(BossID.NOBITA);
        this.createBoss(BossID.CHAIEN);
        this.createBoss(BossID.XEKO);
        this.createBoss(BossID.TINH_LINH_DAO_KAME);
        this.createBoss(BossID.TINH_LINH_NAM_KAME);
        this.createBoss(BossID.TINH_LINH_DAO_GURU);
        this.createBoss(BossID.TINH_LINH_NAM_GURU);
        this.createBoss(BossID.TINH_LINH_VACH_NUI_DEN);
        this.createBoss(BossID.TINH_LINH_THUNG_LUNG_DEN);
        this.createBoss(BossID.KAMI);
        this.createBoss(BossID.ULTIMATE_HEARTS);
        this.createBoss(BossID.HEARTS);
        this.createBoss(BossID.LAGSS);
        this.createBoss(BossID.FU);
        // this.createBoss(BossID.BIG_GETE_STAR, 1);
        // this.createBoss(BossID.FROST_2, 1);
        // this.createBoss(BossID.FROST_3, 1);

    }

    public void createBoss(int bossID, int total) {
        for (int i = 0; i < total; i++) {
            createBoss(bossID);
        }
    }

    public Boss createBoss(int bossID) {
        try {
            return switch (bossID) {               
                case BossID.BROLY ->
                    new Broly();
                case BossID.KAMI ->
                    new KamiOrenBoss(BossID.KAMI, BossesData.KAMI);
                case BossID.OREN ->
                    new KamiOrenBoss(BossID.OREN, BossesData.OREN);
                case BossID.KAMI_OREN ->
                    new KamiOrenBoss(BossID.KAMI_OREN, BossesData.KAMI_OREN);
                case BossID.ULTIMATE_HEARTS ->
                    new TimePlanetBoss(BossID.ULTIMATE_HEARTS, BossesData.ULTIMATE_HEARTS);
                case BossID.HEARTS ->
                    new TimePlanetBoss(BossID.HEARTS, BossesData.HEARTS);
                case BossID.LAGSS ->
                    new TimePlanetBoss(BossID.LAGSS, BossesData.LAGSS);
                case BossID.TANJIRO_DEMON ->
                    new nro.models.boss.kimetsu.TanjiroDemonBoss();
				case BossID.HUYET_QUY_KUROGANE ->
					new nro.models.boss.kimetsu.KimetsuMap168Boss();
				case BossID.XICH_QUY_ENMA ->
					new nro.models.boss.kimetsu.KimetsuMap168Boss();
				case BossID.REPTILE_DEMON ->
					new nro.models.boss.kimetsu.ReptileDemonBoss();
				case BossID.RESTLESS_SOUL ->
					new nro.models.boss.kimetsu.RestlessSoulBoss();
				case BossID.TOXIC_LION ->
					new nro.models.boss.kimetsu.ToxicLionBoss();
				case BossID.BLOOD_MUD_DEMON ->
					new nro.models.boss.kimetsu.BloodMudDemonBoss();
				case BossID.BLOOD_MOON_DRUM ->
					new nro.models.boss.kimetsu.BloodMoonDrumBoss();
                case BossID.HIRUDEGARN ->
                    new nro.models.boss.Cold.Hirudegarn();
                case BossID.FROST_1 ->
                    new nro.models.boss.Cold.Frost1();
                case BossID.BIG_GETE_STAR ->
                    new nro.models.boss.Cold.BigGeteStar();
                case BossID.FROST_2 ->
                    new nro.models.boss.Cold.Frost2();
                case BossID.FROST_3 ->
                    new nro.models.boss.Cold.Frost3();
                case BossID.CHILL_1 ->
                    new nro.models.boss.Cold.Chilled();
                case BossID.CHILL_2 ->
                    new nro.models.boss.Cold.Chilled2();
                case BossID.CHILL_PAST_1 ->
                    new nro.models.boss.Cold.ChilledPast();
                case BossID.KU ->
                    new Ku();
                // case BossID.TAP_SU_0 ->
                //     new TAPSU0();
                // case BossID.TAP_SU_1 ->
                //     new TAPSU1();
                // case BossID.TAP_SU_2 ->
                //     new TAPSU2();
                // case BossID.TAP_SU_3 ->
                //     new TAPSU3();
                // case BossID.TAP_SU_4 ->
                //     new TAPSU4();
                // case BossID.TAN_BINH_5 ->
                //     new TANBINH5();
                // case BossID.TAN_BINH_0 ->
                //     new TANBINH0();
                // case BossID.TAN_BINH_1 ->
                //     new TANBINH1();
                // case BossID.TAN_BINH_2 ->
                //     new TANBINH2();
                // case BossID.TAN_BINH_3 ->
                //     new TANBINH3();
                // case BossID.TAN_BINH_4 ->
                //     new TANBINH4();
                // case BossID.CHIEN_BINH_5 ->
                //     new CHIENBINH5();
                // case BossID.CHIEN_BINH_0 ->
                //     new CHIENBINH0();
                // case BossID.CHIEN_BINH_1 ->
                //     new CHIENBINH1();
                // case BossID.CHIEN_BINH_2 ->
                //     new CHIENBINH2();
                // case BossID.CHIEN_BINH_3 ->
                //     new CHIENBINH3();
                // case BossID.CHIEN_BINH_4 ->
                //     new CHIENBINH4();
                // case BossID.DOI_TRUONG_5 ->
                //     new DOITRUONG5();
                case BossID.SO_4 ->
                    new SO4();
                case BossID.SO_3 ->
                    new SO3();
                case BossID.SO_2 ->
                    new SO2();
                case BossID.SO_1 ->
                    new SO1();
                case BossID.TIEU_DOI_TRUONG ->
                    new TDT();
                case BossID.SO_4_NM ->
                    new SO4_NM();
                case BossID.SO_3_NM ->
                    new SO3_NM();
                case BossID.SO_2_NM ->
                    new SO2_NM();
                case BossID.SO_1_NM ->
                    new SO1_NM();
                case BossID.TIEU_DOI_TRUONG_NM ->
                    new TDT_NM();
                case BossID.BUJIN ->
                    new BUJIN();
                case BossID.KOGU ->
                    new KOGU();
                case BossID.ZANGYA ->
                    new ZANGYA();
                case BossID.BIDO ->
                    new BIDO();
                case BossID.BOJACK ->
                    new BOJACK();
                case BossID.SUPER_BOJACK ->
                    new SUPER_BOJACK();
                case BossID.KUKU ->
                    new Kuku();
                case BossID.MAP_DAU_DINH ->
                    new MapDauDinh();
                case BossID.RAMBO ->
                    new Rambo();
                case BossID.TAU_PAY_PAY_DONG_NAM_KARIN ->
                    new TaoPaiPai();
                case BossID.DRABURA ->
                    new Drabura();
                case BossID.BUI_BUI ->
                    new BuiBui();
                case BossID.BUI_BUI_2 ->
                    new BuiBui2();
                case BossID.YA_CON ->
                    new Yacon();
                case BossID.DRABURA_2 ->
                    new Drabura2();
                case BossID.GOKU ->
                    new Goku();
                case BossID.CADIC ->
                    new Cadic();
                case BossID.MABU_12H ->
                    new Mabu();
                case BossID.DRABURA_3 ->
                    new Drabura3();
                case BossID.MABU ->
                    new Mabu2H();
                case BossID.SUPERBU ->
                    new SuperBu();
                case BossID.FIDE ->
                    new Fide();
                case BossID.DR_KORE ->
                    new DrKore();
                case BossID.ANDROID_19 ->
                    new Android19();
                case BossID.ANDROID_13 ->
                    new Android13();
                case BossID.ANDROID_14 ->
                    new Android14();
                case BossID.ANDROID_15 ->
                    new Android15();
                case BossID.PIC ->
                    new Pic();
                case BossID.POC ->
                    new Poc();
                case BossID.KING_KONG ->
                    new KingKong();
                case BossID.XEN_BO_HUNG ->
                    new XenBoHung();
                case BossID.SIEU_BO_HUNG ->
                    new SieuBoHung();
                case BossID.XEN_CON_1 ->
                    new XENCON1();
                case BossID.XEN_CON_2 ->
                    new XENCON2();
                case BossID.XEN_CON_3 ->
                    new XENCON3();
                case BossID.XEN_CON_4 ->
                    new XENCON4();
                case BossID.XEN_CON_5 ->
                    new XENCON5();
                case BossID.XEN_CON_6 ->
                    new XENCON6();
                case BossID.XEN_CON_7 ->
                    new XENCON7();
                case BossID.COOLER ->
                    new Cooler();
                case BossID.SUPER_BROLY_V1 ->
                    new SuperBrolyV1();
                case BossID.KHIDOT ->
                    new KhiDot();
                case BossID.NGUYETTHAN ->
                    new NguyetThan();
                case BossID.NHATTHAN ->
                    new NhatThan();
                // case BossID.GOLDEN_FRIEZA ->
                //     new GoldenFrieza();
                // case BossID.DEATH_BEAM_1 ->
                //     new DeathBeam1();
                // case BossID.DEATH_BEAM_2 ->
                //     new DeathBeam2();
                // case BossID.DEATH_BEAM_3 ->
                //     new DeathBeam3();
                // case BossID.DEATH_BEAM_4 ->
                //     new DeathBeam4();
                // case BossID.DEATH_BEAM_5 ->
                //     new DeathBeam5();
                case BossID.BIMA ->
                    new BiMa();
                case BossID.MATROI ->
                    new MaTroi();
                case BossID.DOI ->
                    new Doi();
                case BossID.CAI_TRANG_MA_TROI ->
                    new HalloweenCaiTrangBoss(BossID.CAI_TRANG_MA_TROI, BossesData.CAI_TRANG_MA_TROI);
                case BossID.CAI_TRANG_DOI_NHI ->
                    new HalloweenCaiTrangBoss(BossID.CAI_TRANG_DOI_NHI, BossesData.CAI_TRANG_DOI_NHI);
                case BossID.CAI_TRANG_BO_XUONG_TD ->
                    new HalloweenCaiTrangBoss(BossID.CAI_TRANG_BO_XUONG_TD, BossesData.CAI_TRANG_BO_XUONG_TD);
                case BossID.CAI_TRANG_BO_XUONG_NM ->
                    new HalloweenCaiTrangBoss(BossID.CAI_TRANG_BO_XUONG_NM, BossesData.CAI_TRANG_BO_XUONG_NM);
                case BossID.CAI_TRANG_BO_XUONG_XD ->
                    new HalloweenCaiTrangBoss(BossID.CAI_TRANG_BO_XUONG_XD, BossesData.CAI_TRANG_BO_XUONG_XD);
                case BossID.ONG_GIA_NOEL ->
                    new OngGiaNoel();
                case BossID.SON_TINH ->
                    new SonTinh();
                case BossID.THUY_TINH ->
                    new ThuyTinh();
                case BossID.LAN_CON ->
                    new LanCon();
                case BossID.SOI_HEC_QUYN1 ->
                    new SoiHecQuyn();
                case BossID.O_DO1 ->
                    new Odo();
                case BossID.Virut ->
                    new Virut();
                case BossID.MAT_TROI ->
                    new MatTroi();
                case BossID.BLACK_GOKU ->
                    new BlackGoku();
                case BossID.CUMBER ->
                    new Cumber();
                case BossID.GOKU_SSJ4 ->
                    new GokuSSJ4();
                case BossID.VEGETA_SSJ4 ->
                    new VegetaSSJ4();
                case BossID.GOGETA_SSJ4 ->
                    new GogetaSSJ4();
                case BossID.FU ->
                    new Fu();
                case BossID.AN_TROM ->
                    new AnTrom();
                case BossID.BOT_RATI ->
                    new BotRati();
                case BossID.QUY_SANTA ->
                    new QuySanta();
                case BossID.RONG_NHI ->
                    new RongNhi();
                case BossID.BABY ->
                    new Baby();
                case BossID.B ->
                    new B();
                case BossID.BULMA_PHU_THUY_BAMBOO ->
                    new nro.models.boss.BulmaPhuThuy(BossID.BULMA_PHU_THUY_BAMBOO,
                            nro.models.consts.ConstMap.RUNG_BAMBOO);
                case BossID.BULMA_PHU_THUY_DUONG_XI ->
                    new nro.models.boss.BulmaPhuThuy(BossID.BULMA_PHU_THUY_DUONG_XI,
                            nro.models.consts.ConstMap.RUNG_DUONG_XI);
                case BossID.DORAEMON ->
                    new nro.models.boss.BossDoraemon();
                case BossID.SHIZUKA ->
                    new nro.models.boss.BossDoraemonFriend(bossID);
                case BossID.NOBITA ->
                    new nro.models.boss.BossDoraemonFriend(bossID);
                case BossID.CHAIEN ->
                    new nro.models.boss.BossDoraemonFriend(bossID);
                case BossID.XEKO ->
                    new nro.models.boss.BossDoraemonFriend(bossID);
                case BossID.ANIME_AINZ ->
                    new nro.models.boss.AnimeMapBoss(bossID);
                case BossID.ANIME_FATHER ->
                    new nro.models.boss.AnimeMapBoss(bossID);
                case BossID.ANIME_ALL_FOR_ONE ->
                    new nro.models.boss.AnimeMapBoss(bossID);
                case BossID.ANIME_KATANA_MAN ->
                    new nro.models.boss.AnimeMapBoss(bossID);
                case BossID.ANIME_BOROS ->
                    new nro.models.boss.AnimeMapBoss(bossID);
                case BossID.ANIME_KANEKI ->
                    new nro.models.boss.AnimeMapBoss(bossID);
                case BossID.ANIME_IGRIS ->
                    new nro.models.boss.AnimeMapBoss(bossID);
                case BossID.ANIME_DEMON_KING ->
                    new nro.models.boss.AnimeMapBoss(bossID);
                case BossID.ANIME_ZODD ->
                    new nro.models.boss.AnimeMapBoss(bossID);
                case BossID.ANIME_YOR ->
                    new nro.models.boss.AnimeMapBoss(bossID);
                case BossID.TINH_LINH_DAO_KAME ->
                    new nro.models.boss.TinhLinhBoss(bossID, 5, 20_000_000);
                case BossID.TINH_LINH_NAM_KAME ->
                    new nro.models.boss.TinhLinhBoss(bossID, 29, 35_000_000);
                case BossID.TINH_LINH_DAO_GURU ->
                    new nro.models.boss.TinhLinhBoss(bossID, 13, 50_000_000);
                case BossID.TINH_LINH_NAM_GURU ->
                    new nro.models.boss.TinhLinhBoss(bossID, 33, 65_000_000);
                case BossID.TINH_LINH_VACH_NUI_DEN ->
                    new nro.models.boss.TinhLinhBoss(bossID, 20, 80_000_000);
                case BossID.TINH_LINH_THUNG_LUNG_DEN ->
                    new nro.models.boss.TinhLinhBoss(bossID, 37, 100_000_000);
                default ->
                    null;
            };
        } catch (Exception e) {
            Logger.error(e + "\n");
            return null;
        }
    }

    public Boss getBoss(int id) {
        try {
            List<Boss> visibleBosses = getVisibleBosses();
            Boss boss = visibleBosses.get(id);
            if (boss != null) {
                return boss;
            }
        } catch (Exception e) {
        }
        return null;
    }

    public List<Boss> getVisibleBosses() {
        List<Boss> visibleBosses = new ArrayList<>(this.bosses);
        if (this == BossManager.gI()) {
            for (Boss boss : new ArrayList<>(BrolyManager.gI().getBosses())) {
                if ((boss.id == BossID.BROLY || boss.id == BossID.SUPER_BROLY)
                        && !visibleBosses.contains(boss)) {
                    visibleBosses.add(boss);
                }
            }
        }
        visibleBosses.removeIf(boss ->
                boss.id == BossID.SO_1 || boss.id == BossID.SO_2 || boss.id == BossID.SO_3
                || boss.id == BossID.SO_1_NM || boss.id == BossID.SO_2_NM || boss.id == BossID.SO_3_NM
                || boss.id == BossID.XEN_CON_2 || boss.id == BossID.XEN_CON_3
                || boss.id == BossID.XEN_CON_4 || boss.id == BossID.XEN_CON_5
                || boss.id == BossID.XEN_CON_6);
        return visibleBosses;
    }

    private boolean isHiddenFromBossList(Boss boss) {
        int mapJoin = boss.data[0].getMapJoin()[0];
        // Doraemon, One Piece and anime areas are public hunting maps.
        // The legacy Cadic test matches every map >= 165; do not hide these bosses.
        if (mapJoin >= 238 && mapJoin <= 303) {
            return false;
        }
        return MapService.gI().isMapBossFinal(mapJoin)
                || MapService.gI().isMapHuyDiet(mapJoin)
                || MapService.gI().isMapCadic(mapJoin)
                || MapService.gI().isMapYardart(mapJoin)
                || MapService.gI().isMapMaBu(mapJoin)
                || MapService.gI().isMapBlackBallWar(mapJoin);
    }

    public void showListBoss(Player player) {
        if (!player.isAdmin()) {
            return;
        }
        player.idMark.setMenuType(3);
        Message msg;
        try {
            List<Boss> visibleBosses = getVisibleBosses();
            msg = new Message(-96);
            msg.writer().writeByte(0);
            msg.writer().writeUTF("Boss");
            msg.writer().writeByte((int) visibleBosses.stream().filter(boss -> !isHiddenFromBossList(boss)).count());
            for (int i = 0; i < visibleBosses.size(); i++) {
                Boss boss = visibleBosses.get(i);
                if (isHiddenFromBossList(boss)) {
                    continue;
                }
                msg.writer().writeInt(i);
                msg.writer().writeInt(i);
                msg.writer().writeShort(boss.data[0].getOutfit()[0]);
                if (player.getSession().version >= 214) {
                    msg.writer().writeShort(-1);
                }
                msg.writer().writeShort(boss.data[0].getOutfit()[1]);
                msg.writer().writeShort(boss.data[0].getOutfit()[2]);
                msg.writer().writeUTF(boss.data[0].getName());
                if (boss.zone != null) {
                    msg.writer().writeUTF(boss.bossStatus.toString());
                    msg.writer().writeUTF(boss.zone.map.mapName + "(" + boss.zone.map.mapId + ") khu " + boss.zone.zoneId + "");
                } else {
                    msg.writer().writeUTF(boss.bossStatus.toString());
                    msg.writer().writeUTF("=))");
                }
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    public Boss getBossById(int bossId) {
        return this.bosses.stream().filter(boss -> boss.id == bossId && !boss.isDie()).findFirst().orElse(null);
    }

    public boolean checkBosses(Zone zone, int BossID) {
        return this.bosses.stream().filter(boss -> boss.id == BossID && boss.zone != null && boss.zone.equals(zone) && !boss.isDie()).findFirst().orElse(null) != null;
    }

    public Player findBossClone(Player player) {
        return player.zone.getBosses().stream().filter(boss -> boss.id < -100_000_000 && !boss.isDie()).findFirst().orElse(null);
    }

    public Boss getBossById(int bossId, int mapId, int zoneId) {
        return getVisibleBosses().stream().filter(boss -> boss.id == bossId && boss.zone != null && boss.zone.map.mapId == mapId && boss.zone.zoneId == zoneId && !boss.isDie()).findFirst().orElse(null);
    }

    @Override
    public void run() {
        while (ServerManager.isRunning) {
            try {
                long st = System.currentTimeMillis();
                for (Boss boss : this.bosses) {
                    try {
                        boss.update();
                    } catch (Exception e) {
                        long now = System.currentTimeMillis();
                        if (now - lastUpdateErrorLog >= 30_000L) {
                            lastUpdateErrorLog = now;
                            Logger.logException(BossManager.class, e,
                                    "Lỗi update boss " + boss.id);
                        }
                    }
                }
                long sleepTime = 150 - (System.currentTimeMillis() - st);
                Thread.sleep(Math.max(sleepTime, 10));
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ignored) {
            }
        }
    }
}
