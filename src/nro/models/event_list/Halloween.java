package nro.models.event_list;

import nro.models.boss.BossID;
import nro.models.event.Event;

public class Halloween extends Event {

    @Override
    public void npc() {
        createNpc(14, 18, 800, 408);
        createNpc(199, 18, 572, 168);
    }

    @Override
    public void boss() {
        createBoss(BossID.BIMA, 10);
        createBoss(BossID.MATROI, 10);
        createBoss(BossID.DOI, 10);
        createBoss(BossID.CAI_TRANG_MA_TROI, 10);
        createBoss(BossID.CAI_TRANG_DOI_NHI, 10);
        createBoss(BossID.CAI_TRANG_BO_XUONG_TD, 10);
        createBoss(BossID.CAI_TRANG_BO_XUONG_NM, 10);
        createBoss(BossID.CAI_TRANG_BO_XUONG_XD, 10);
    }
}
