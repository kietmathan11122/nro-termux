package nro.models.event_list;

import nro.models.boss.BossID;
import nro.models.boss.event_tet.LanCon;
import nro.models.consts.ConstNpc;
import nro.models.consts.ConstPlayer;
import nro.models.consts.BossStatus;
import nro.models.event.Event;
import nro.models.map.service.MapService;
import nro.models.server.Manager;
import nro.models.utils.Logger;

public class LunarNewYear extends Event {

    @Override
    public void npc() {
        createNpc(0, ConstNpc.NOI_BANH, 850, 432);
        createNpc(0, ConstNpc.DUONG_TANG, 650, 432);
    }

    @Override
    public void boss() {
        // Mỗi map thuộc hành tinh Trái Đất có đúng một Lân Con, ở khu ngẫu nhiên.
        // Lấy theo planetId của dữ liệu map để các map mới thêm sau này cũng tự có Lân.
        Manager.MAPS.stream()
                .filter(map -> map != null && map.planetId == ConstPlayer.TRAI_DAT)
                .filter(map -> map.zones != null && !map.zones.isEmpty())
                .forEach(map -> {
                    try {
                        LanCon lion = new LanCon();
                        lion.zoneFinal = MapService.gI().getMapWithRandZone(map.mapId);
                        // Khởi tạo HP và đưa vào map ngay. Nếu chỉ để trạng thái
                        // REST, danh sách Boss sẽ hiển thị Lân là "Đã chết" cho
                        // tới lượt cập nhật sau của manager sự kiện.
                        if (lion.zoneFinal != null) {
                            lion.changeStatus(BossStatus.RESPAWN);
                            lion.update(); // RESPAWN -> JOIN_MAP, tạo HP/kỹ năng
                            lion.update(); // JOIN_MAP -> xuất hiện tại zoneFinal
                        }
                    } catch (Exception e) {
                        Logger.error("Không thể tạo Lân Con ở map " + map.mapId + ": " + e + "\n");
                    }
                });
        nro.models.services.JourneyWestEventService.gI().startEvent();
    }
}
