import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import nro.models.boss.*;
import nro.models.boss.Boss_mini.AnTrom;
import nro.models.consts.ConstMap;
import nro.models.map.*;
import nro.models.map.service.ChangeMapService;
import nro.models.player.Player;
import sun.misc.Unsafe;

public class BossSpawnPolicyTest {
    static Unsafe unsafe;
    static {
        try { Field f=Unsafe.class.getDeclaredField("theUnsafe"); f.setAccessible(true); unsafe=(Unsafe)f.get(null); }
        catch(Exception e) { throw new RuntimeException(e); }
    }
    static Map map(int id, byte type, int count) throws Exception {
        Map m=(Map)unsafe.allocateInstance(Map.class);
        m.mapId=id; m.type=type; m.zones=new ArrayList<>();
        for(int i=0;i<count;i++) m.zones.add(new Zone(m,i,20));
        return m;
    }
    static Player boss(int id) throws Exception {
        Player p=(Player)unsafe.allocateInstance(Player.class); p.id=id; p.isBoss=true; return p;
    }
    static void check(boolean value) { if(!value) throw new AssertionError(); }
    static void rejects(Runnable action) {
        try { action.run(); } catch(IllegalArgumentException expected) { return; }
        throw new AssertionError("Admission should fail");
    }
    public static void main(String[] args) throws Exception {
        Map aru=map(0,ConstMap.MAP_NORMAL,5);
        Player normal=boss(-98765), thief=boss(BossID.AN_TROM), rati=boss(BossID.BOT_RATI);
        check(BossSpawnPolicy.destination(normal,aru.zones.get(0)).zoneId==2);
        check(BossSpawnPolicy.destination(normal,aru.zones.get(1)).zoneId==2);
        check(BossSpawnPolicy.destination(normal,aru.zones.get(4)).zoneId==4);
        rejects(()->BossSpawnPolicy.destination(thief,aru.zones.get(0)));
        rejects(()->BossSpawnPolicy.destination(rati,aru.zones.get(1)));
        aru.zones.get(2).getBosses().add(thief);
        check(AnTrom.isAllowedZone(aru.zones.get(2),thief));
        rejects(()->BossSpawnPolicy.destination(rati,aru.zones.get(3)));
        aru.zones.get(2).getBosses().clear();
        check(AnTrom.isAllowedZone(aru.zones.get(3)));
        Map dungeon=map(53,ConstMap.MAP_NORMAL,1);
        check(BossSpawnPolicy.destination(normal,dungeon.zones.get(0))==dungeon.zones.get(0));
        Map tiny=map(3,ConstMap.MAP_NORMAL,2);
        rejects(()->BossSpawnPolicy.destination(normal,tiny.zones.get(0)));
        CountDownLatch gate=new CountDownLatch(1); AtomicInteger accepted=new AtomicInteger();
        Thread[] threads=new Thread[2]; Player[] bosses={thief,rati};
        for(int i=0;i<2;i++) {
            final int n=i;
            threads[i]=new Thread(()-> {
                try { gate.await(); ChangeMapService.gI().goToMap(bosses[n],aru.zones.get(n+2)); accepted.incrementAndGet(); }
                catch(IllegalArgumentException expected) {}
                catch(Exception e) { throw new RuntimeException(e); }
            });
            threads[i].start();
        }
        gate.countDown(); for(Thread t:threads)t.join();
        check(accepted.get()==1);
        check(aru.zones.stream().mapToInt(z->z.getBosses().size()).sum()==1);
        System.out.println("PASS: zones 0/1, dungeon exemption, map uniqueness, release after removal, concurrent admission");
    }
}
