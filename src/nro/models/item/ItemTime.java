package nro.models.item;

import nro.models.player.NPoint;
import nro.models.player.Player;
import nro.models.services.Service;
import nro.models.utils.Util;
import nro.models.services.ItemTimeService;

public class ItemTime {
    public volatile int spiritCharmMultiplier = 1;
    public volatile long spiritCharmUntil;

    public static final byte DOANH_TRAI = 0;
    public static final byte BAN_DO_KHO_BAU = 1;
    public static final byte CON_DUONG_RAN_DOC = 2;
    public static final byte KHI_GAS_HUY_DIET = 3;
    public static final byte TIME_KEO_BUA_BAO = 4;
    public static final byte TEXT_NHAN_BUA_MIEN_PHI = 5;

    public static final int TIME_ITEM = 600000;
    public static final int TIME_OPEN_POWER = 8640000;
    public static final int TIME_MAY_DO = 1800000;
    public static final int TIME_MAY_DO2 = 1800000;
    public static final int TIME_BUA_ROBOT = 1800000;
    public static final long TIME_CO_BON_LA = 30 * 60 * 1000L;
    public static final int TIME_KILIS = 3600000;
    public static final int TIME_NUOC_MIA1 = 600_000;
    public static final int TIME_NUOC_MIA2 = 600_000;
    public static final int TIME_NUOC_MIA3 = 600_000;

    public static final int TIME_BUA_SANTA = 1800000;
    public static final int TIME_EAT_MEAL = 600000;
    public static final int TIME_CMS = 3600000;
    public static final int TIME_DK = 1800000;
    public static final int TIME_NCD = 1800000;
    /** Thời gian hiệu lực của lọ tiềm năng x2/x5/x7/x10: 30 phút. */
    public static final int TIME_LO_TIEM_NANG = 1800000;

    private Player player;

    public boolean isUseBoHuyet;
    public boolean isUseBoKhi;
    public boolean isUseGiapXen;
    public boolean isUseCuongNo;
    public boolean isUseAnDanh;
    public boolean isUseBoHuyet2;
    public boolean isUseBoKhi2;
    public boolean isUseGiapXen2;
    public boolean isUseCuongNo2;
    public boolean isUseAnDanh2;
    public boolean isUseCuongHuyet;

    public long lastTimeBoHuyet;
    public long lastTimeBoKhi;
    public long lastTimeGiapXen;
    public long lastTimeCuongNo;
    public long lastTimeAnDanh;

    public long lastTimeBoHuyet2;
    public long lastTimeBoKhi2;
    public long lastTimeGiapXen2;
    public long lastTimeCuongNo2;
    public long lastTimeAnDanh2;
    public long lastTimeCuongHuyet;

    public boolean isUseMayDo;
    public long lastTimeUseMayDo;
    public boolean isUseMayDo2;
    public long lastTimeUseMayDo2;
    public boolean isUseMayDoLinhHon;
    public long lastTimeUseMayDoLinhHon;
    public boolean isUseKhoBauX2;
    public long lastTimeUseKhoBauX2;
    public boolean isUseBuaSanta;
    public long lastTimeBuaSanta;
    public boolean isBuaRobot;
    public long lastTimeBuaRobot;
    public int typeMayDo2;

    public long lastTimeUseCoBonLa;
    public boolean isUseCoBonLa;

    public boolean isUseKilis;
    public long lastTimeUseKilis;

    public boolean isUseNuocMia1;
    public long lastTimeUseNuocMia1;
    public boolean isUseNuocMia2;
    public long lastTimeUseNuocMia2;
    public boolean isUseNuocMia3;
    public long lastTimeUseNuocMia3;

    public boolean isOpenPower;
    public long lastTimeOpenPower;

    public boolean isUseBuffRongTim;
    public long lastTimeBuffRongTim;
    public boolean isUseMayManRongTim;
    public long lastTimeMayManRongTim;

    public boolean isUseTDLT;
    public long lastTimeUseTDLT;
    public int timeTDLT;

    public boolean isUseRX;
    public long lastTimeUseRX;
    public int timeRX;

    public boolean isUseCMS;
    public long lastTimeUseCMS;

    public boolean isUseNCD;
    public long lastTimeUseNCD;

    public boolean isUseGTPT;
    public long lastTimeUseGTPT;

    public boolean isUseDK;
    public long lastTimeUseDK;

    public boolean isEatMeal;
    public long lastTimeEatMeal;
    public int iconMeal;

    public boolean isEatMeal2;
    public long lastTimeEatMeal2;
    public int iconMeal2;

    public boolean isUseTuiGiuNhiet;
    public long lastTimeUseTuiGiuNhiet;

    public long lastTimeKhauTrang;
    public boolean isUseKhauTrang;
    public long timeLengthKilis;
    public long totalCoBonLaTime;
    public long timeLengthCoBonLa;
    
    public boolean isThuocMo;
    public long lastTimeThuocMo;
    public int timeThuocMo;

    public boolean isUseLoMau;
    public long lastTimeLoMau;
    public long lastTimeHealLoMau;

    public boolean isUseLoNangLuong;
    public long lastTimeLoNangLuong;
    public long lastTimeHealLoNangLuong;

    public boolean isUseLoHacAm;
    public long lastTimeLoHacAm;
    public long lastTimeHealLoHacAm;

    public boolean isUseLoTiemNang;
    public long lastTimeLoTiemNang;
    public int loTiemNangMultiplier = 1;
    public int iconLoTiemNang = -1;

    public boolean isUseMayDoVatPham;
    public long lastTimeUseMayDoVatPham;

    public ItemTime(Player player) {
        this.player = player;
    }

    public void update() {
        if (isEatMeal) {
            if (Util.canDoWithTime(lastTimeEatMeal, TIME_EAT_MEAL)) {
                isEatMeal = false;
                Service.gI().point(player);
            }
        }
        if (isEatMeal2) {
            if (Util.canDoWithTime(lastTimeEatMeal2, TIME_EAT_MEAL)) {
                isEatMeal2 = false;
                Service.gI().point(player);
            }
        }
        if (isUseBoHuyet) {
            if (Util.canDoWithTime(lastTimeBoHuyet, TIME_ITEM)) {
                isUseBoHuyet = false;
                Service.gI().point(player);
            }
        }

        if (isUseBoKhi) {
            if (Util.canDoWithTime(lastTimeBoKhi, TIME_ITEM)) {
                isUseBoKhi = false;
                Service.gI().point(player);
            }
        }

        if (isUseGiapXen) {
            if (Util.canDoWithTime(lastTimeGiapXen, TIME_ITEM)) {
                isUseGiapXen = false;
            }
        }
        if (isUseCuongNo) {
            if (Util.canDoWithTime(lastTimeCuongNo, TIME_ITEM)) {
                isUseCuongNo = false;
                Service.gI().point(player);
            }
        }
        if (isUseAnDanh) {
            if (Util.canDoWithTime(lastTimeAnDanh, TIME_ITEM)) {
                isUseAnDanh = false;
            }
        }
        if (isUseCuongHuyet) {
            if (Util.canDoWithTime(lastTimeCuongHuyet, TIME_ITEM)) {
                isUseCuongHuyet = false;
                Service.gI().point(player);
            }
        }

        if (isUseBoHuyet2) {
            if (Util.canDoWithTime(lastTimeBoHuyet2, TIME_ITEM)) {
                isUseBoHuyet2 = false;
                Service.gI().point(player);
            }
        }

        if (isUseBoKhi2) {
            if (Util.canDoWithTime(lastTimeBoKhi2, TIME_ITEM)) {
                isUseBoKhi2 = false;
                Service.gI().point(player);
            }
        }
        if (isUseGiapXen2) {
            if (Util.canDoWithTime(lastTimeGiapXen2, TIME_ITEM)) {
                isUseGiapXen2 = false;
            }
        }
        if (isUseCuongNo2) {
            if (Util.canDoWithTime(lastTimeCuongNo2, TIME_ITEM)) {
                isUseCuongNo2 = false;
                Service.gI().point(player);
            }
        }
        if (isUseAnDanh2) {
            if (Util.canDoWithTime(lastTimeAnDanh2, TIME_ITEM)) {
                isUseAnDanh2 = false;
            }
        }
        if (isUseCMS) {
            if (Util.canDoWithTime(lastTimeUseCMS, TIME_CMS)) {
                isUseCMS = false;
            }
        }
        if (isUseGTPT) {
            if (Util.canDoWithTime(lastTimeUseGTPT, TIME_ITEM)) {
                isUseGTPT = false;
            }
        }
        if (isUseDK) {
            if (Util.canDoWithTime(lastTimeUseDK, TIME_DK)) {
                isUseDK = false;
            }
        }
        if (isUseLoTiemNang && Util.canDoWithTime(lastTimeLoTiemNang, TIME_LO_TIEM_NANG)) {
            isUseLoTiemNang = false;
            loTiemNangMultiplier = 1;
            if (iconLoTiemNang >= 0) {
                ItemTimeService.gI().removeItemTime(player, iconLoTiemNang);
                iconLoTiemNang = -1;
            }
            Service.gI().sendThongBao(player, "Hiệu lực Lọ tiềm năng đã kết thúc.");
        }
        if (isOpenPower) {
            if (Util.canDoWithTime(lastTimeOpenPower, TIME_OPEN_POWER)) {
                player.nPoint.limitPower++;
                if (player.nPoint.limitPower > NPoint.MAX_LIMIT) {
                    player.nPoint.limitPower = NPoint.MAX_LIMIT;
                }
                Service.gI().sendThongBao(player, "Giới hạn sức mạnh của bạn đã được tăng lên 1 bậc");
                isOpenPower = false;
            }
        }
        if (isUseMayDo) {
            if (Util.canDoWithTime(lastTimeUseMayDo, TIME_MAY_DO)) {
                isUseMayDo = false;
            }
        }
        if (isUseMayDo2) {
            if (Util.canDoWithTime(lastTimeUseMayDo2, TIME_MAY_DO2)) {
                isUseMayDo2 = false;
            }
        }
        if (isBuaRobot) {
            if (Util.canDoWithTime(lastTimeBuaRobot, TIME_BUA_ROBOT)) {
                isBuaRobot = false;
            }
        }
        if (isUseCoBonLa) {
            long now = System.currentTimeMillis();
            long timeLeft = (lastTimeUseCoBonLa + timeLengthCoBonLa) - now;
            if (timeLeft <= 0) {
                isUseCoBonLa = false;
                timeLengthCoBonLa = 0;
            }
        }
        if (isUseKilis) {
            if (Util.canDoWithTime(lastTimeUseKilis, TIME_KILIS)) {
                isUseKilis = false;
            }
        }
        if (isUseNuocMia1) {
            if (Util.canDoWithTime(lastTimeUseNuocMia1, TIME_NUOC_MIA1)) {
                isUseNuocMia1 = false;
                Service.gI().point(player);
            }
        }
        if (isUseNuocMia2) {
            if (Util.canDoWithTime(lastTimeUseNuocMia1, TIME_NUOC_MIA2)) {
                isUseNuocMia2 = false;
                Service.gI().point(player);
            }
        }
        if (isUseNuocMia3) {
            if (Util.canDoWithTime(lastTimeUseNuocMia1, TIME_NUOC_MIA3)) {
                isUseNuocMia3 = false;
                Service.gI().point(player);
            }
        }
        if (isUseBuaSanta) {
            if (Util.canDoWithTime(lastTimeBuaSanta, TIME_BUA_SANTA)) {
                isUseBuaSanta = false;
            }
        }
        if (isUseKhoBauX2) {
            if (Util.canDoWithTime(lastTimeUseKhoBauX2, TIME_MAY_DO2)) {
                isUseKhoBauX2 = false;
            }
        }
        if (isUseKhauTrang) {
            if (Util.canDoWithTime(lastTimeKhauTrang, timeLengthKilis)) {
                isUseKhauTrang = false;
                nro.models.services.ItemTimeService.gI().removeItemTime(player, 10711);
                Service.gI().point(player);
            }
        }
        if (isUseTuiGiuNhiet) {
            if (Util.canDoWithTime(lastTimeUseTuiGiuNhiet, 1800000)) { // 30 mins
                isUseTuiGiuNhiet = false;
                nro.models.services.ItemTimeService.gI().removeItemTime(player, 13748);
                Service.gI().sendThongBao(player, "Túi giữ nhiệt đã hết tác dụng");
            }
        }
        if (isUseMayDoLinhHon) {
            if (Util.canDoWithTime(lastTimeUseMayDoLinhHon, 1800000)) {
                isUseMayDoLinhHon = false;
                nro.models.services.ItemTimeService.gI().removeItemTime(player, 1022);
                Service.gI().sendThongBao(player, "Máy dò linh hồn đã hết tác dụng");
            }
        }
        if (isUseTDLT) {
            if (Util.canDoWithTime(lastTimeUseTDLT, timeTDLT)) {
                this.isUseTDLT = false;
                ItemTimeService.gI().sendCanAutoPlay(this.player);
            }
        }
        if (isUseRX) {
            if (Util.canDoWithTime(lastTimeUseRX, timeRX)) {
                isUseRX = false;
            }
        }
        if (isThuocMo) {
            if (Util.canDoWithTime(lastTimeThuocMo, timeThuocMo)) {
                ItemTimeService.gI().turnOffThuocMo(this.player);
            }
        }
        if (isUseBuffRongTim) {
            if (Util.canDoWithTime(lastTimeBuffRongTim, 1800000)) {
                isUseBuffRongTim = false;
                nro.models.services.ItemTimeService.gI().removeItemTime(player, 2039);
                Service.gI().point(player);
            }
        }
        if (isUseMayManRongTim) {
            if (Util.canDoWithTime(lastTimeMayManRongTim, 6000000)) {
                isUseMayManRongTim = false;
                nro.models.services.ItemTimeService.gI().removeItemTime(player, 2040);
            }
        }
        if (isUseLoMau) {
            if (Util.canDoWithTime(lastTimeLoMau, 1800000)) {
                isUseLoMau = false;
                nro.models.services.ItemTimeService.gI().removeItemTime(player, 31922);
                Service.gI().sendThongBao(player, "Lọ Máu đã hết tác dụng!");
            } else {
                if (System.currentTimeMillis() - lastTimeHealLoMau >= 30000) {
                    lastTimeHealLoMau = System.currentTimeMillis();
                    int hpHeal = player.nPoint.hpMax / 10;
                    int kiHeal = player.nPoint.mpMax / 10;
                    player.nPoint.hp = Math.min(player.nPoint.hp + hpHeal, player.nPoint.hpMax);
                    player.nPoint.mp = Math.min(player.nPoint.mp + kiHeal, player.nPoint.mpMax);
                    Service.gI().point(player);
                    Service.gI().Send_Info_NV(player);
                }
            }
        }
        if (isUseLoNangLuong) {
            if (Util.canDoWithTime(lastTimeLoNangLuong, 1800000)) {
                isUseLoNangLuong = false;
                nro.models.services.ItemTimeService.gI().removeItemTime(player, 31924);
                Service.gI().sendThongBao(player, "Lọ Năng Lượng đã hết tác dụng!");
            } else {
                if (System.currentTimeMillis() - lastTimeHealLoNangLuong >= 30000) {
                    lastTimeHealLoNangLuong = System.currentTimeMillis();
                    int hpHeal = player.nPoint.hpMax / 10;
                    int kiHeal = player.nPoint.mpMax / 10;
                    player.nPoint.hp = Math.min(player.nPoint.hp + hpHeal, player.nPoint.hpMax);
                    player.nPoint.mp = Math.min(player.nPoint.mp + kiHeal, player.nPoint.mpMax);
                    Service.gI().point(player);
                    Service.gI().Send_Info_NV(player);
                }
            }
        }
        if (isUseLoHacAm) {
            if (Util.canDoWithTime(lastTimeLoHacAm, 1800000)) {
                isUseLoHacAm = false;
                nro.models.services.ItemTimeService.gI().removeItemTime(player, 31926);
                Service.gI().sendThongBao(player, "Lọ Hắc ám đã hết tác dụng!");
            } else {
                if (System.currentTimeMillis() - lastTimeHealLoHacAm >= 30000) {
                    lastTimeHealLoHacAm = System.currentTimeMillis();
                    int hpHeal = 0;
                    if (player.zone != null && player.zone.mobs != null) {
                        for (nro.models.mob.Mob mob : player.zone.mobs) {
                            if (mob != null && !mob.isDie()) {
                                int hpDrain = mob.point.hp * 5 / 100;
                                if (hpDrain > 0) {
                                    mob.hoi_hp(-hpDrain);
                                    hpHeal += hpDrain;
                                }
                            }
                        }
                    }
                    if (hpHeal > 0) {
                        player.nPoint.hp = Math.min(player.nPoint.hp + hpHeal, player.nPoint.hpMax);
                        player.nPoint.mp = Math.min(player.nPoint.mp + hpHeal, player.nPoint.mpMax);
                        Service.gI().point(player);
                        Service.gI().Send_Info_NV(player);
                        Service.gI().sendThongBao(player, "Hiệu ứng Lọ Hắc ám hút HP/KI của quái vật xung quanh!");
                    }
                }
            }
        }
        if (isUseMayDoVatPham) {
            if (Util.canDoWithTime(lastTimeUseMayDoVatPham, 1800000)) {
                isUseMayDoVatPham = false;
                nro.models.services.ItemTimeService.gI().removeItemTime(player, 31940);
                Service.gI().sendThongBao(player, "Máy dò vật phẩm đã hết tác dụng!");
            }
        }
    }

    public void dispose() {
        this.player = null;
    }
}
