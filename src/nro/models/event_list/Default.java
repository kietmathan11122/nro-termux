package nro.models.event_list;


import nro.models.boss.BossID;
import nro.models.boss.Boss;
import nro.models.boss.Boss_Manager.BossManager;
import nro.models.consts.BossStatus;
import nro.models.event.Event;

public class Default extends Event {

    @Override
    public void boss() {
        // Broly từng chỉ được tạo ở trạng thái REST nên không bao giờ vào map.
        // Ép hai nhịp cập nhật RESPAWN -> JOIN_MAP để xuất hiện ngay.
        for (int i = 0; i < 5; i++) {
            Boss broly = BossManager.gI().createBoss(BossID.BROLY);
            if (broly != null) {
                broly.changeStatus(BossStatus.RESPAWN);
                broly.update();
                broly.update();
            }
        }
    }

}
