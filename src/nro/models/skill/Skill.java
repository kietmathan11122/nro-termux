package nro.models.skill;

import nro.models.player_system.Template.SkillTemplate;

/**
 *
 * @author By Mr Blue
 * 
 */

public class Skill {

    public static final int RANGE_ATTACK_CHIEU_DAM = 100;
    public static final int RANGE_ATTACK_CHIEU_CHUONG = 300;

    public static final byte DEMON = 2;
    public static final byte MASENKO = 3;
    public static final byte TRI_THUONG = 7;
    public static final byte MAKANKOSAPPO = 11;
    public static final byte DE_TRUNG = 12;
    public static final byte LIEN_HOAN = 17;
    public static final byte SOCOLA = 18;

    public static final byte GALICK = 4;
    public static final byte ANTOMIC = 5;
    public static final byte TAI_TAO_NANG_LUONG = 8;
    public static final byte BIEN_KHI = 13;
    public static final byte TU_SAT = 14;
    public static final byte HUYT_SAO = 21;
    public static final byte TROI = 23;

    public static final byte DRAGON = 0;
    public static final byte KAMEJOKO = 1;
    public static final byte THAI_DUONG_HA_SAN = 6;
    public static final byte KAIOKEN = 9;
    public static final byte QUA_CAU_KENH_KHI = 10;
    public static final byte DICH_CHUYEN_TUC_THOI = 20;
    public static final byte THOI_MIEN = 22;

    public static final byte KHIEN_NANG_LUONG = 19;

    public static final byte SUPER_KAME = 24;
    public static final byte LIEN_HOAN_CHUONG = 25;
    public static final byte MA_PHONG_BA = 26;
    public static final byte NHAT_CHEM_NANG_LUONG = 27;
    public static final byte BIEN_HINH = 30;
    public static final byte TIEN_HOA_SUC_MANH = 31;
    public static final byte PHAN_THAN = 32;

    public static final byte THIEN_HA_LONG_TRAM = 35;
    public static final byte MA_PHONG_BA_VU_TRU = 36;
    public static final byte MA_SUONG_SINH_HOI = 37;
    public static final byte XUNG_XOAY_MA_NHI = 38;
    public static final byte CUONG_NO_SIEU_SAIYAN = 39;
    public static final byte TIA_CHOP_NO_VU_TRU = 40;
    public static final byte THIEN_MA_THAN_CHUONG = 41;
    public static final byte KAMEJOKO_THAN_VU_TRU = 42;
    public static final byte QUA_CAU_BANG_GIA_SUPERNOVA = 43;
    public static final byte KIEM_KHI_TRUNKS = 44;
    public static final byte CAPSULE_HEN_GIO = 45;
    public static final byte DAI_KIEM_HY_VONG = 46;
    public static final byte LOI_KIEM_PHA_KHONG = 47;
    public static final byte LUC_QUANG_PHAN_RA = 48;
    public static final byte DICH_ANH_LIEN_TRAM = 49;
    public static final byte DUOI_DOC_HU_KHONG = 50;

    // Bộ kỹ năng độc lập của hai lớp Spine. Các ID này không trùng kỹ năng
    // hành tinh cũ, nhờ vậy chọn phím tắt và xử lý server không còn đi nhầm nhánh.
    public static final byte QUYEN_KICH_TUONG_LAI = 51;
    public static final byte BO_PHAP_TAN_ANH = 52;
    public static final byte XUNG_KICH_CAPSULE = 53;
    public static final byte PHAO_SANG_HY_VONG = 54;
    public static final byte KHIEN_THOI_GIAN = 55;
    public static final byte QUA_CAU_HY_VONG = 56;
    public static final byte BOC_PHA_TUONG_LAI = 57;

    public static final byte LIEN_QUYEN_SINH_HOC = 58;
    public static final byte TIA_LUC_QUANG = 59;
    public static final byte KEN_TAI_SINH = 60;
    public static final byte VONG_XOAY_HAP_THU = 61;
    public static final byte NHA_TU_TE_BAO = 62;
    public static final byte LASER_HOAN_MY = 63;
    public static final byte NO_LOI_SINH_HOC = 64;
    public static final byte CHUONG_XUYEN_PHA = 65;
    public static final byte THIEN_LONG_QUYEN = 66;
    public static final byte CUONG_PHONG = 67;
    public static final byte MOC_DANG = 68;
    public static final byte HEARTS_PUNCH = 69;
    public static final byte HEARTS_CUBE = 70;
    public static final byte HEARTS_GRAVITY = 71;
    public static final byte HEARTS_KICK = 72;
    public static final byte HEARTS_ULTIMATE = 73;
    public static final byte HEARTS_CRYSTAL_BARRAGE = 74;
    public static final byte HEARTS_CUBE_PRISON = 75;

    public SkillTemplate template;

    public short skillId;

    public int point;

    public long powRequire;

    public int coolDown;

    public long lastTimeUseThisSkill;

    public int dx;

    public int dy;

    public int maxFight;

    public int manaUse;

    public short damage;

    public short currLevel;

    public String moreInfo;

    public short price;
    
    public long lastTimeUseThisSkillbot;

    public Skill() {

    }

    public Skill(Skill skill) {
        this.skillId = skill.skillId;
        this.point = skill.point;
        this.powRequire = skill.powRequire;
        this.coolDown = skill.coolDown;
        this.lastTimeUseThisSkill = skill.lastTimeUseThisSkill;
        this.dx = skill.dx;
        this.dy = skill.dy;
        this.maxFight = skill.maxFight;
        this.manaUse = skill.manaUse;
        this.damage = skill.damage;
        this.moreInfo = skill.moreInfo;
        this.price = skill.price;
        this.template = skill.template;
    }

    public void dispose() {
        this.template = null;
    }
}
