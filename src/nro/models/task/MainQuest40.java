package nro.models.task;

import nro.models.boss.BossID;
import nro.models.consts.ConstNpc;

/** Native main-story continuation after the Time Planet campaign. */
public final class MainQuest40 {

    public static final int ID = 40;
    private static final int TIME_PLANET = 211;
    private static final int BILL_PLANET = 154;
    private static final int PRISON_PLANET = 155;
    private static final int TIME_GUARD = 128;
    private static final int BLACK_CROW = 119;
    private static final int TIME_FRAGMENT = 2299;
    private static final int SPACETIME_THREAD = 2300;
    private static final int TIME_CRYSTAL = 2301;

    private record Step(String event, long target, int map, int maximum,
            String name, String instruction, String dialogue) {
    }

    private static final Step[] STEPS = {
        talk(ConstNpc.MAI, TIME_PLANET, "Gặp Mai sau khi nhận Ấn Ký Thời Gian",
                "Gặp Mai tại Hành Tinh Thời Gian để nhận tin về vết nứt còn sót lại.",
                "Trận chiến đã kết thúc, nhưng Ấn Ký Người Bảo Vệ Thời Gian vẫn đang rung lên. "
                + "Thí nghiệm của Fu để lại một vết nứt nối với Hành tinh ngục tù. Hãy gặp Ô Sin ở đây; "
                + "ông ấy biết cách tìm đầu bên kia của vết nứt."),
        talk(ConstNpc.OSIN, TIME_PLANET, "Nhận chỉ dẫn phong ấn từ Ô Sin",
                "Gặp Ô Sin tại Hành Tinh Thời Gian.",
                "Vết nứt đang khiến Hộ Vệ Thời Gian mất kiểm soát. Hãy đánh bại 40 Hộ Vệ, "
                + "rồi thu thập 20 Mảnh Vỡ Thời Gian từ chúng. Mai sẽ dùng các mảnh vỡ để tìm tọa độ nguồn nhiễu."),
        action("mob", TIME_GUARD, TIME_PLANET, 40, "Đánh bại 40 Hộ Vệ Thời Gian",
                "Đánh bại Hộ Vệ Thời Gian tại Hành Tinh Thời Gian."),
        action("item", TIME_FRAGMENT, TIME_PLANET, 20, "Thu thập 20 Mảnh Vỡ Thời Gian",
                "Nhặt Mảnh Vỡ Thời Gian rơi từ Hộ Vệ Thời Gian tại Hành Tinh Thời Gian."),
        talk(ConstNpc.MAI, TIME_PLANET, "Đưa kết quả dò tìm cho Mai",
                "Trở về gặp Mai tại Hành Tinh Thời Gian.",
                "Tọa độ dẫn tới nhà ngục nơi Cumber từng bị giam giữ. Vết nứt quá bất ổn để đi thẳng tới đó. "
                + "Ô Sin cần Tơ Nhện Thời Không để giữ cổng mở; hãy đánh bại 30 Quạ Đen, "
                + "rồi nhặt 20 sợi tơ mà chúng mang theo."),
        action("mob", BLACK_CROW, TIME_PLANET, 30, "Đánh bại 30 Quạ Đen",
                "Đánh bại Quạ Đen tại Hành Tinh Thời Gian."),
        action("item", SPACETIME_THREAD, TIME_PLANET, 20, "Thu thập 20 Tơ Nhện Thời Không",
                "Nhặt Tơ Nhện Thời Không rơi từ Quạ Đen tại Hành Tinh Thời Gian."),
        talk(ConstNpc.OSIN, TIME_PLANET, "Gia cố cổng thời không cùng Ô Sin",
                "Gặp Ô Sin tại Hành Tinh Thời Gian để gia cố cổng.",
                "Tơ đã giữ được mép vết nứt, nhưng chúng ta còn cần năng lượng ổn định. "
                + "Hãy thu thập 30 Tinh Thể Thời Gian từ Hộ Vệ Thời Gian hoặc Quạ Đen, rồi trở về gặp Mai."),
        action("item", TIME_CRYSTAL, TIME_PLANET, 30, "Thu thập 30 Tinh Thể Thời Gian",
                "Nhặt Tinh Thể Thời Gian rơi từ Hộ Vệ Thời Gian hoặc Quạ Đen tại Hành Tinh Thời Gian."),
        talk(ConstNpc.MAI, TIME_PLANET, "Ổn định tọa độ cùng Mai",
                "Gặp Mai tại Hành Tinh Thời Gian trước khi đến Hành tinh Bill.",
                "Tọa độ đã ổn định. Hãy đến Hành tinh Bill và tìm Ô Sin. "
                + "Ông ấy sẽ dẫn bạn sang Hành tinh ngục tù để chặn nguồn sức mạnh đang nuôi vết nứt."),
        action("map", BILL_PLANET, BILL_PLANET, 1, "Đến Hành tinh Bill",
                "Đến Hành tinh Bill, sau đó gặp Ô Sin."),
        talk(ConstNpc.OSIN, BILL_PLANET, "Gặp Ô Sin tại Hành tinh Bill",
                "Gặp Ô Sin tại Hành tinh Bill để đến Hành tinh ngục tù.",
                "Khí tức của Cumber đang tràn qua vết nứt. Ta đã mở đường đến Hành tinh ngục tù. "
                + "Cumber sẽ biến hình khi bị đánh bại; hãy hạ cả dạng Super Cumber, rồi gặp ta trước khi trở về."),
        action("map", PRISON_PLANET, PRISON_PLANET, 1, "Tiến vào Hành tinh ngục tù",
                "Nhờ Ô Sin tại Hành tinh Bill đưa đến Hành tinh ngục tù."),
        action("boss", BossID.CUMBER, PRISON_PLANET, 1, "Đánh bại Super Cumber tại Hành tinh ngục tù",
                "Đánh bại Cumber và dạng Super Cumber tại Hành tinh ngục tù, rồi gặp Ô Sin."),
        talk(ConstNpc.OSIN, PRISON_PLANET, "Báo cáo chiến thắng Cumber với Ô Sin",
                "Gặp Ô Sin tại Hành tinh ngục tù để trở về Hành Tinh Thời Gian.",
                "Sức mạnh của Cumber không còn tiếp sức cho vết nứt. Tuy nhiên, Fu đã lần theo dấu cổng của chúng ta. "
                + "Hãy trở về Hành Tinh Thời Gian, đánh bại 60 Hộ Vệ bị hắn thao túng rồi ngăn hắn mở lại nhà ngục."),
        action("map", TIME_PLANET, TIME_PLANET, 1, "Trở về Hành Tinh Thời Gian",
                "Nhờ Ô Sin đưa về Hành tinh Bill, rồi gặp Mai tại đó để đến Hành Tinh Thời Gian."),
        action("mob", TIME_GUARD, TIME_PLANET, 60, "Phá vòng vây của 60 Hộ Vệ Thời Gian",
                "Đánh bại Hộ Vệ Thời Gian tại Hành Tinh Thời Gian để phá vòng vây của Fu."),
        action("boss", BossID.FU, TIME_PLANET, 1, "Đánh bại Fu tại Hành Tinh Thời Gian",
                "Đánh bại Fu tại Hành Tinh Thời Gian, rồi gặp Ô Sin để phong ấn vết nứt."),
        talk(ConstNpc.OSIN, TIME_PLANET, "Phong ấn vết nứt cùng Ô Sin",
                "Gặp Ô Sin tại Hành Tinh Thời Gian để hoàn tất phong ấn.",
                "Fu đã bị đẩy lui. Ta sẽ dùng tinh thể và tơ thời không để đóng vết nứt, "
                + "còn ấn ký của bạn sẽ giữ dòng thời gian ổn định. Hãy báo tin an toàn cho Mai."),
        talk(ConstNpc.MAI, TIME_PLANET, "Báo tin bình an cho Mai",
                "Gặp Mai tại Hành Tinh Thời Gian để hoàn thành nhiệm vụ 40.",
                "Vết nứt đã được phong ấn và những người còn mắc kẹt đã trở về an toàn. "
                + "Cảm ơn bạn, Người Bảo Vệ Thời Gian. Chúng ta sẽ tiếp tục theo dõi những dấu hiệu bất thường mới.")
    };

    private MainQuest40() {
    }

    private static Step talk(int npc, int map, String name, String notify, String dialogue) {
        return new Step("talk", npc, map, 1, name, notify, dialogue);
    }

    private static Step action(String event, long target, int map, int maximum,
            String name, String notify) {
        return new Step(event, target, map, maximum, name, notify, "");
    }

    /** Returns a fresh template; progress is never shared between players. */
    public static TaskMain createTemplate() {
        TaskMain task = new TaskMain();
        task.id = ID;
        task.name = "Phong ấn vết nứt thời gian";
        task.detail = "Sau chiến dịch Hành Tinh Thời Gian, tàn dư thí nghiệm của Fu vẫn nối với nhà ngục của Cumber. "
                + "Phối hợp với Mai và Ô Sin, ổn định cổng thời không, đánh bại Cumber rồi trở về phong ấn vết nứt. "
                + "Thưởng: 150 triệu sức mạnh và tiềm năng, 150 triệu vàng, 1.000 ngọc xanh và 1.000 hồng ngọc.";
        for (Step step : STEPS) {
            SubTaskMain sub = new SubTaskMain();
            sub.name = step.name();
            sub.notify = step.instruction();
            sub.maxCount = (short) step.maximum();
            sub.npcId = "talk".equals(step.event()) ? (byte) step.target() : (byte) -1;
            sub.mapId = (short) step.map();
            task.subTasks.add(sub);
        }
        return task;
    }

    /** Checks only the current step, its exact event, target and destination. */
    public static boolean matches(int step, String event, long target, int map) {
        if (step < 0 || step >= STEPS.length) {
            return false;
        }
        Step expected = STEPS[step];
        return expected.event().equals(event) && expected.target() == target && expected.map() == map;
    }

    /** Identifies the quest drop; the caller applies its normal drop chance. */
    public static int dropItem(int step, int mob, int map) {
        if (map != TIME_PLANET) {
            return -1;
        }
        return switch (step) {
            case 3 -> mob == TIME_GUARD ? TIME_FRAGMENT : -1;
            case 6 -> mob == BLACK_CROW ? SPACETIME_THREAD : -1;
            case 8 -> mob == TIME_GUARD || mob == BLACK_CROW ? TIME_CRYSTAL : -1;
            default -> -1;
        };
    }

    /** Dialogue is present only for the matching main-story conversation step. */
    public static String dialogue(int step) {
        return step >= 0 && step < STEPS.length ? STEPS[step].dialogue() : "";
    }
}
