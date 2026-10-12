package nro.models.boss.time_planet;

import nro.models.boss.Boss;
import nro.models.boss.BossData;

/** Independent hunting bosses on Time Planet, using the shared combat/drop lifecycle. */
public class TimePlanetBoss extends Boss {

    public TimePlanetBoss(int id, BossData data) throws Exception {
        super(id, false, true, data);
    }
}
