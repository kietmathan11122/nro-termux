package nro.models.npc_list;

import nro.models.consts.ConstNpc;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.services.CoHonEventService;
import nro.models.services.Service;

public class CoHon extends Npc {

    private static final int CONFIRM_BUY_MAM_CUNG = 12201;

    public CoHon(int mapId, int status, int cx, int cy, int tempId, int avatar) {
        super(mapId, status, cx, cy, tempId, avatar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Hãy đưa Quỷ Đói về đây để nhận Hộp Quà sự kiện.",
                    "Giao\nQuỷ Đói", "Nhận\nNhang Trầm", "Mua Mâm Cúng\n20 ngọc", "Hướng dẫn", "Từ chối");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) {
            return;
        }
        if (player.idMark.getIndexMenu() == CONFIRM_BUY_MAM_CUNG) {
            if (select == 0) CoHonEventService.gI().buyMamCung(player, 1);
            else if (select == 1) CoHonEventService.gI().buyMamCung(player, 20);
            return;
        }
        if (player.idMark.getIndexMenu() != ConstNpc.BASE_MENU) return;
        switch (select) {
            case 0 -> CoHonEventService.gI().deliverHungryGhost(player, this);
            case 1 -> CoHonEventService.gI().giveIncense(player);
            case 2 -> createOtherMenu(player, CONFIRM_BUY_MAM_CUNG,
                    "Chọn số lượng Mâm Cúng cần mua.\nGiới hạn 20 cái mỗi ngày.",
                    "Mua 1\n20 ngọc", "Mua 20\n400 ngọc", "Từ chối");
            case 3 -> Service.gI().sendThongBaoOK(player,
                    "Đánh quái ở các bản đồ để nhặt Cháo Trắng.\n"
                    + "Dùng 5 Nhang Trầm và 20 Cháo Trắng để triệu hồi Hồn Ma.\n"
                    + "Đến gần Quỷ Đói để nó theo bạn, rồi dẫn về Cô Hồn cuối tuyến.\n"
                    + "Mâm Cúng giá 20 ngọc, mua tối đa 20 cái/ngày và có thể giao dịch.\n"
                    + "Mâm Cúng dùng tại Vách núi Aru, Moori hoặc Kakarot sẽ gọi ngẫu nhiên Pet Cô Hồn.\n"
                    + "Hồn Ma có thể tặng quà hoặc trở mặt lấy mạng ngươi.");
            default -> {
            }
        }
    }
}
