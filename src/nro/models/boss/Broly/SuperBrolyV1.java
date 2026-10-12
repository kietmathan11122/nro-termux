package nro.models.boss.Broly;

import nro.models.boss.Boss;
import nro.models.boss.BossData;
import nro.models.boss.BossID;
import nro.models.boss.BossesData;
import nro.models.map.ItemMap;
import nro.models.player.Player;
import nro.models.services.Service;
import nro.models.utils.Util;
import nro.models.item.Item;

public class SuperBrolyV1 extends Boss {

    public SuperBrolyV1() throws Exception {
        super(BossID.SUPER_BROLY_V1, BossesData.SUPER_BROLY_V1, BossesData.SUPER_BROLY_V1_2);
    }

    @Override
    public void reward(Player plKill) {
        if (plKill != null) {
            int x = this.location.x;
            int y = this.zone.map.yPhysicInTop(x, this.location.y - 24);
            if (Util.isTrue(5, 100)) {
                ItemMap it = new ItemMap(zone, 2066, 1, x, y, plKill.id);
                Service.gI().dropItemMap(zone, it);
            }
            if (Util.isTrue(10, 100)) {
                ItemMap it = new ItemMap(zone, 2072, 5, x, y, plKill.id);
                Service.gI().dropItemMap(zone, it);
            }
            if (Util.isTrue(10, 100)) {
                ItemMap it = new ItemMap(zone, 2058, 1, x, y, plKill.id);
                Service.gI().dropItemMap(zone, it);
            }
            if (Util.isTrue(10, 100)) {
                ItemMap it = new ItemMap(zone, 2061, 1, x, y, plKill.id);
                Service.gI().dropItemMap(zone, it);
            }
            if (this.currentLevel == 0) { // Super Broly V1 (Dạng 1)
                if (Util.isTrue(5, 100)) { // Tỉ lệ 5%
                    ItemMap it = new ItemMap(zone, 2048, 1, x, y, plKill.id);
                    Service.gI().dropItemMap(zone, it);
                }
            } else if (this.currentLevel == 1) { // Super Broly V2 (Dạng 2)
                // Drop 3-star dragon ball as a cool reward
                ItemMap itBall = new ItemMap(zone, 16, 1, x, y, plKill.id);
                Service.gI().dropItemMap(zone, itBall);

                if (Util.isTrue(5, 100)) { // Tỉ lệ 5%
                    ItemMap it = new ItemMap(zone, 2049, 1, x, y, plKill.id);
                    Service.gI().dropItemMap(zone, it);
                }
            }
        }
        super.reward(plKill);
    }
}
