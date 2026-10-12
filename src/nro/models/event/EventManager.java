package nro.models.event;

import nro.models.event_list.TopUp;
import nro.models.event_list.TrungThu;
import nro.models.event_list.HungVuong;
import nro.models.event_list.Christmas;
import nro.models.event_list.Halloween;
import nro.models.event_list.LunarNewYear;
import nro.models.event_list.Default;
import nro.models.event_list.InternationalWomensDay;
import nro.models.event_list.Summer;
import nro.models.boss.BossID;
import nro.models.boss.Boss;
import nro.models.boss.Boss_Manager.BossManager;
import nro.models.map.service.MapService;
import nro.models.consts.BossStatus;
import nro.models.consts.ConstNpc;
import nro.models.boss.event_tet.LanCon;
import nro.models.boss.Boss_Manager.LunarNewYearEventManager;
import nro.models.boss.Boss_Manager.HungVuongEventManager;
import nro.models.map.service.ChangeMapService;
import java.util.ArrayList;
import java.util.List;
import nro.models.player.Player;
import nro.models.map.service.NpcService;
import nro.models.network.Message;

public class EventManager {

    private static EventManager instance;

    private volatile int currentEventId = 9;

    // Chỉ bật khi admin dùng sk1, không tự chạy lúc khởi động server.
    public static boolean LUNNAR_NEW_YEAR = false;

    public static boolean INTERNATIONAL_WOMANS_DAY = true;

    public static boolean CHRISTMAS = true;

    public static boolean HALLOWEEN = true;

    // Chỉ bật khi admin chọn đúng sự kiện Hùng Vương.
    public static boolean HUNG_VUONG = false;

    public static boolean TRUNG_THU = true;

    public static boolean TOP_UP = true;

    public static boolean CO_HON = false;

    public static boolean SUMMER = false;

    public static EventManager gI() {
        if (instance == null) {
            instance = new EventManager();
        }
        return instance;
    }

    public void init() {
        if (LUNNAR_NEW_YEAR) {
           // new LunarNewYear().init();
        }
        if (INTERNATIONAL_WOMANS_DAY) {
           // new InternationalWomensDay().init();
        }
        if (HALLOWEEN) {
          //  new Halloween().init();
        }
        if (CHRISTMAS) {
          //  new Christmas().init();
        }
        if (HUNG_VUONG) {
            new HungVuong().init();
        }
        if (TRUNG_THU) {
           // new TrungThu().init();
        }
    }

    public void changeEvent(int eventId) {
        LUNNAR_NEW_YEAR = false;
        INTERNATIONAL_WOMANS_DAY = false;
        HALLOWEEN = false;
        CHRISTMAS = false;
        HUNG_VUONG = false;
        TRUNG_THU = false;
        TOP_UP = false;
        CO_HON = false;
        SUMMER = false;
        nro.models.services.CoHonEventService.gI().stopEvent();
        nro.models.services.JourneyWestEventService.gI().stopEvent();

        // Lân Con được quản lý bởi LunarNewYearEventManager, không nằm trong
        // BossManager chung. Dọn riêng để đổi/bật lại sk không giữ Lân cũ.
        for (Boss lion : new ArrayList<>(LunarNewYearEventManager.gI().getBosses())) {
            if (!(lion instanceof LanCon)) {
                continue;
            }
            if (lion.zone != null) {
                ChangeMapService.gI().exitMap(lion);
            }
            lion.changeStatus(BossStatus.REST);
            LunarNewYearEventManager.gI().removeBoss(lion);
        }

        // Xóa boss sự kiện cũ
        List<Boss> bossesToRemove = new ArrayList<>();
        int[] eventBossIds = {
            BossID.LAN_CON, BossID.ONG_GIA_NOEL,
            BossID.BIMA, BossID.MATROI, BossID.DOI,
            BossID.CAI_TRANG_MA_TROI, BossID.CAI_TRANG_DOI_NHI,
            BossID.CAI_TRANG_BO_XUONG_TD, BossID.CAI_TRANG_BO_XUONG_NM, BossID.CAI_TRANG_BO_XUONG_XD,
            BossID.THUY_TINH, BossID.SON_TINH,
            BossID.KHIDOT, BossID.NGUYETTHAN, BossID.NHATTHAN
        };
        BossManager[] eventManagers = {BossManager.gI(), HungVuongEventManager.gI()};
        for (BossManager manager : eventManagers) {
            for (Boss boss : new ArrayList<>(manager.getBosses())) {
                if (boss instanceof LanCon) {
                    bossesToRemove.add(boss);
                    continue;
                }
                for (int id : eventBossIds) {
                    if (boss.id == id) {
                        bossesToRemove.add(boss);
                        break;
                    }
                }
            }
        }
        for (Boss boss : bossesToRemove) {
            if (boss.zone != null) {
                ChangeMapService.gI().exitMap(boss);
            }
            boss.changeStatus(BossStatus.REST);
            BossManager.gI().removeBoss(boss);
            HungVuongEventManager.gI().removeBoss(boss);
        }

        // Xóa NPC sự kiện cũ
        nro.models.map.Map map0 = MapService.gI().getMapById(0);
        if (map0 != null) {
            // 49 là Đường Tăng: bản cũ đã dùng nhầm ID này cho Nồi Bánh.
            // Dọn cả NPC sai cũ và Nồi Bánh đúng trước khi đổi sự kiện.
            map0.npcs.removeIf(npc -> npc.tempId == ConstNpc.DUONG_TANG
                    || npc.tempId == ConstNpc.NOI_BANH);
        }
        nro.models.map.Map map14 = MapService.gI().getMapById(14);
        if (map14 != null) {
            map14.npcs.removeIf(npc -> npc.tempId == 18);
        }
        nro.models.map.Map map199 = MapService.gI().getMapById(199);
        if (map199 != null) {
            map199.npcs.removeIf(npc -> npc.tempId == 18);
        }

        // Kích hoạt sự kiện mới
        switch (eventId) {
            case 1 -> {
                LUNNAR_NEW_YEAR = true;
                new LunarNewYear().init();
            }
            case 2 -> {
                SUMMER = true;
                new Summer().init();
            }
            case 3 -> {
                HALLOWEEN = true;
                new Halloween().init();
            }
            case 4 -> {
                CHRISTMAS = true;
                new Christmas().init();
            }
            case 5 -> {
                HUNG_VUONG = true;
                new HungVuong().init();
            }
            case 6 -> {
                TRUNG_THU = true;
                new TrungThu().init();
            }
            case 9 -> {
                // Tắt sự kiện. Broly thường vẫn chạy độc lập ở các map săn đệ.
            }
            case 10 -> {
                CO_HON = true;
                nro.models.services.CoHonEventService.gI().startEvent();
            }
        }
        currentEventId = eventId;
    }

    public int getCurrentEventId() {
        return currentEventId;
    }

    public void sendCurrentEventInfo(Player player) {
        if (player == null) {
            return;
        }
        Message msg = null;
        try {
            msg = new Message(-76);
            msg.writer().writeByte(-3);
            msg.writer().writeUTF("SỰ KIỆN " + getEventName(currentEventId).toUpperCase());
            msg.writer().writeUTF(getCurrentEventParticipation());
            msg.writer().writeUTF(getCurrentEventRewards());
            player.sendMessage(msg);
        } catch (Exception ignored) {
            NpcService.gI().createTutorial(player, -1, getCurrentEventInfo());
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    private String getCurrentEventParticipation() {
        return switch (currentEventId) {
            case 1 -> "CÁCH THAM GIA\n"
                    + "• Quái Trái Đất rơi Cơm Nếp 10%.\n"
                    + "• Quái Namek rơi Đậu Xanh 10%.\n"
                    + "• Quái Xayda rơi Thịt Tươi 10%.\n"
                    + "• NPC tặng Bột Gạo; Santa bán Muối Tiêu.\n"
                    + "• Boss rơi Chả Lụa.\n"
                    + "• Nấu Bánh Dầy và Bánh Chưng tại Nồi Bánh.\n"
                    + "• Dẫn Đường Tăng bị lạc về NPC Đường Tăng.";
            case 10 -> "CÁCH THAM GIA\n"
                    + "• Mỗi NPC thường tặng 1 Nhang Trầm/ngày.\n"
                    + "• NPC Cô Hồn tặng 5 Nhang Trầm/ngày.\n"
                    + "• Đánh quái có 5% rơi Cháo Trắng.\n"
                    + "• Dùng 5 Nhang Trầm + 20 Cháo Trắng gọi Hồn Ma.\n"
                    + "• Dẫn Quỷ Đói về Cô Hồn tại Vách Aru, Moori hoặc Kakarot.\n"
                    + "• Quỷ Đói xuất hiện lại sau 5 phút.";
            default -> "CÁCH THAM GIA\n" + getCurrentEventInfo().replace("|7|", "").replace("|0|", "");
        };
    }

    private String getCurrentEventRewards() {
        return switch (currentEventId) {
            case 1 -> "PHẦN THƯỞNG\n"
                    + "• Giao Bánh Dầy nhận 15 Hộp Tây Du.\n"
                    + "• Giao Bánh Chưng nhận 10 Hộp Tây Du.\n"
                    + "• Thu phục Lân Con rơi 1-3 Hộp Tây Du.\n"
                    + "• Hộp có thể mở ra đồ Tây Du, vàng, ngọc và Ngọc Rồng.\n"
                    + "• Trang bị ngẫu nhiên HP, KI hoặc sức đánh 10-25%.\n"
                    + "• Tỷ lệ vật phẩm vĩnh viễn: 5%.\n"
                    + "• Đủ bộ Tây Du vĩnh viễn: +5% sức đánh.";
            case 10 -> "PHẦN THƯỞNG\n"
                    + "• Hồn Ma tặng vàng hoặc ngọc; 50% có thể trở mặt.\n"
                    + "• Dẫn Quỷ Đói nhận 1 Hộp Ma Quỷ.\n"
                    + "• Hộp có thể mở cải trang, vàng, ngọc hoặc Ngọc Rồng.\n"
                    + "• Dẫn đủ 100 Quỷ Đói nhận danh hiệu Người Dẫn Hồn.\n"
                    + "• Đủ 3 cải trang vĩnh viễn trong Sổ: +2% hút HP, +5% HP.";
            default -> "PHẦN THƯỞNG\n• Xem thông tin chi tiết tại NPC của sự kiện.";
        };
    }

    public String getCurrentEventInfo() {
        return switch (currentEventId) {
            case 1 -> "|7|SỰ KIỆN TẾT NGUYÊN ĐÁN\n"
                    + "|0|• Quái Trái Đất rơi Cơm Nếp 10%.\n"
                    + "• Quái Namek rơi Đậu Xanh 10%.\n"
                    + "• Quái Xayda rơi Thịt Tươi 10%.\n"
                    + "• NPC tặng Bột Gạo; Santa bán Muối Tiêu; boss rơi Chả Lụa.\n"
                    + "• Nấu Bánh Dầy và Bánh Chưng tại Nồi Bánh.\n"
                    + "• Giao Bánh Dầy nhận 15 Hộp Tây Du, Bánh Chưng nhận 10 hộp.\n"
                    + "• Thu phục Lân Con rơi 1-3 Hộp Tây Du.\n"
                    + "• Dẫn Đường Tăng bị lạc về NPC Đường Tăng nhận Hộp Tây Du.\n"
                    + "• Đủ bộ Tây Du vĩnh viễn được +5% sức đánh.";
            case 2 -> "|7|SỰ KIỆN MÙA HÈ\n"
                    + "|0|• Đánh quái toàn bản đồ có 5% rơi Kem Trái Cây.\n"
                    + "• Dùng 20 Kem Trái Cây để mở một phần quà hè.\n"
                    + "• Quà gồm vàng, ngọc và cải trang Mùa Hè.\n"
                    + "• Xem hướng dẫn và điểm tại NPC ChiChi.";
            case 3 -> "|7|SỰ KIỆN HALLOWEEN\n"
                    + "|0|• Săn quái và boss Halloween để thu thập nguyên liệu.\n"
                    + "• Đổi Túi Mù Halloween tại NPC Karin.\n"
                    + "• Boss sự kiện gồm Bí Ma, Ma Trơi, Dơi và các cải trang Bộ Xương.";
            case 4 -> "|7|SỰ KIỆN GIÁNG SINH\n"
                    + "|0|• Ông Già Noel xuất hiện trong các khu vực sự kiện.\n"
                    + "• Đánh boss và thu thập vật phẩm Noel để nhận quà.";
            case 5 -> "|7|SỰ KIỆN GIỖ TỔ HÙNG VƯƠNG\n"
                    + "|0|• Tham gia hoạt động tại NPC Vua Hùng.\n"
                    + "• Trồng Dưa Hấu, nấu bánh và đổi quà sự kiện.\n"
                    + "• Boss Thủy Tinh xuất hiện trong thời gian sự kiện.";
            case 6 -> "|7|SỰ KIỆN TRUNG THU\n"
                    + "|0|• Tham gia săn boss Khỉ Đột và Nguyệt Thần.\n"
                    + "• Thu thập vật phẩm Trung Thu để đổi phần thưởng.";
            case 10 -> "|7|SỰ KIỆN CÔ HỒN\n"
                    + "|0|• NPC tặng Nhang Trầm mỗi ngày.\n"
                    + "• Quái rơi Cháo Trắng với tỉ lệ 5%.\n"
                    + "• Dùng 5 Nhang Trầm và 20 Cháo Trắng để gọi Hồn Ma.\n"
                    + "• Dẫn Quỷ Đói về NPC Cô Hồn tại map 42, 43 hoặc 44 nhận Hộp Ma Quỷ.\n"
                    + "• Dẫn đủ 100 Quỷ Đói nhận danh hiệu Người Dẫn Hồn.";
            default -> "|7|SỰ KIỆN\n|0|Hiện tại máy chủ chưa bật sự kiện nào.";
        };
    }

    public String getEventName(int eventId) {
        return switch (eventId) {
            case 1 -> "Tết Nguyên Đán";
            case 2 -> "Mùa Hè";
            case 3 -> "Halloween";
            case 4 -> "Giáng sinh";
            case 5 -> "Hùng Vương";
            case 6 -> "Trung Thu";
            case 9 -> "Tắt toàn bộ sự kiện";
            case 10 -> "Cô Hồn";
            default -> "Không xác định";
        };
    }
}
