package nro.models.npc_list;

import nro.models.consts.ConstNpc;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.services.JourneyWestEventService;

public class DuongTang extends Npc {
    public DuongTang(int mapId, int status, int x, int y, int tempId, int avatar) {
        super(mapId, status, x, y, tempId, avatar);
    }
    @Override public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) createOtherMenu(player, ConstNpc.BASE_MENU,
                "Hãy tìm Đường Tăng bị lạc hoặc mang bánh Tết đến đổi Hộp Tây Du.",
                "Giao\nĐường Tăng", "Giao 1\nBánh Dầy", "Giao 1\nBánh Chưng",
                "Hướng dẫn", "Đóng");
    }
    @Override public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) return;
        if (select == 0) JourneyWestEventService.gI().deliver(player, this);
        else if (select == 1) JourneyWestEventService.gI().exchangeCake(player, 1542, 15, "Bánh Dầy");
        else if (select == 2) JourneyWestEventService.gI().exchangeCake(player, 1556, 10, "Bánh Chưng");
        else if (select == 3) nro.models.services.Service.gI().sendThongBaoOK(player,
                "Dẫn Đường Tăng bị lạc về đây nhận 1 Hộp Tây Du. Giao Bánh Dầy nhận 15 hộp, Bánh Chưng nhận 10 hộp. Thu phục Lân Con có thể rơi 1-3 hộp.");
    }
}
