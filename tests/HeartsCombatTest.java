import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import nro.models.map.Zone;
import nro.models.mob.Mob;
import nro.models.network.Message;
import nro.models.network.MySession;
import nro.models.player.EffectSkill;
import nro.models.player.EffectSkin;
import nro.models.player.Location;
import nro.models.player.NPoint;
import nro.models.player.NewSkill;
import nro.models.player.Player;
import nro.models.player.SetClothes;
import nro.models.player_system.Template.SkillTemplate;
import nro.models.intrinsic.Intrinsic;
import nro.models.intrinsic.IntrinsicPlayer;
import nro.models.services.HeartsSkillRules;
import nro.models.services.HeartsSkillService;
import nro.models.skill.PlayerSkill;
import nro.models.skill.Skill;
import sun.misc.Unsafe;

/** In-memory policy/lifecycle tests: no database, listener, socket or timer is started. */
public final class HeartsCombatTest {
    private static final Unsafe UNSAFE;
    private static final AtomicLong NOW = new AtomicLong(1000000);
    private static final HeartsSkillService SERVICE = HeartsSkillService.gI();
    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            UNSAFE = (Unsafe) field.get(null);
            Field clock = HeartsSkillService.class.getDeclaredField("clock");
            clock.setAccessible(true);
            clock.set(SERVICE, (java.util.function.LongSupplier) NOW::get);
        } catch (Exception ex) { throw new ExceptionInInitializerError(ex); }
    }

    public static final class MemorySession extends MySession {
        private MemorySession() { super(null); }
        boolean connected = true;
        @Override public boolean isConnected() { return connected; }
        @Override public void sendMessage(Message msg) { }
    }
    public static final class MemoryPlayer extends Player {
        List<byte[]> packets;
        int hits, sourceId;
        long damage;
        boolean pierced, critical;
        @Override public void sendMessage(Message msg) { if (msg.command == -45) packets.add(msg.getData()); }
        @Override public synchronized int injured(Player owner, long amount, boolean piercing, boolean mobAttack) {
            hits++;
            damage += amount;
            pierced = piercing;
            critical = owner.nPoint.isCrit;
            sourceId = HeartsSkillService.damageSkill(owner).template.id;
            return (int) amount;
        }
    }
    public static final class MemoryMob extends Mob {
        int hits;
        long damage;
        @Override public void injured(Player owner, long amount, boolean full) { hits++; damage += amount; }
    }
    public static final class ManualSkill extends NewSkill {
        ManualSkill(Player player) { super(player); }
        @Override public void start(int interval) { }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static Zone zone() throws Exception {
        nro.models.map.Map map = (nro.models.map.Map) UNSAFE.allocateInstance(nro.models.map.Map.class);
        map.mapId = 0;
        map.mapWidth = 2400;
        map.mapHeight = 600;
        return new Zone(map, 0, 20);
    }
    private static MemoryPlayer player(Zone zone, int id, int x) throws Exception {
        MemoryPlayer p = (MemoryPlayer) UNSAFE.allocateInstance(MemoryPlayer.class);
        p.id = id;
        p.zone = zone;
        p.location = new Location();
        p.location.x = (short) x;
        p.location.y = 240;
        p.nPoint = new NPoint(p);
        p.nPoint.hp = p.nPoint.hpMax = 10000;
        p.nPoint.mp = p.nPoint.mpMax = 1000;
        p.nPoint.dame = 1000;
        p.playerSkill = new PlayerSkill(p);
        p.newSkill = new ManualSkill(p);
        p.effectSkill = new EffectSkill(p);
        p.effectSkin = new EffectSkin(p);
        p.setClothes = new SetClothes(p);
        p.playerIntrinsic = new IntrinsicPlayer();
        p.playerIntrinsic.intrinsic = new Intrinsic();
        p.packets = new ArrayList<>();
        p.heartsReborn = true;
        p.idNRNM = -1;
        MemorySession session = (MemorySession) UNSAFE.allocateInstance(MemorySession.class);
        session.connected = true;
        p.session = session;
        zone.getPlayers().add(p);
        zone.getHumanoids().add(p);
        return p;
    }
    private static MemoryMob mob(Zone zone, int id, int x) {
        MemoryMob m = new MemoryMob();
        m.id = id;
        m.zone = zone;
        m.location.x = (short) x;
        m.location.y = 240;
        m.point.hp = m.point.maxHp = 10000;
        zone.mobs.add(m);
        return m;
    }
    private static Skill select(Player p, int id, int level) {
        Skill s = new Skill();
        s.template = new SkillTemplate();
        s.template.id = (byte) id;
        s.template.classId = 0;
        s.template.type = 4;
        s.template.manaUseType = 1;
        s.point = level;
        s.manaUse = 10;
        s.damage = 165;
        s.coolDown = id == 69 ? 650 : 30000;
        p.playerSkill.skills.add(s);
        p.playerSkill.skillSelect = s;
        return s;
    }
    private static HeartsSkillService.Cast begin(Player p, int id, int x) {
        check(SERVICE.begin(p, id, x, 240, 1), "Cast rejected: " + id);
        return p.newSkill.heartsTechnique;
    }
    private static void advance(Player p, HeartsSkillService.Cast cast, int ms) {
        NOW.addAndGet(ms);
        SERVICE.update(p, cast);
        SERVICE.updateEffects(p);
    }
    private static boolean packet(MemoryPlayer p, int status) {
        for (byte[] bytes : p.packets) if ((bytes[0] & 255) == status) return true;
        return false;
    }

    public static void main(String[] args) throws Exception {
        check(HeartsSkillRules.gravityDamage(10001) == 300, "3% rounding");
        check(HeartsSkillRules.gravityDamage(Integer.MAX_VALUE) == 64424509, "maximum int HP overflow");
        check(HeartsSkillRules.percent(Long.MAX_VALUE, 120) == Long.MAX_VALUE, "overflow saturation");
        check(HeartsSkillRules.chargeMs(69) + HeartsSkillRules.actionMs(69) <= 650, "punch action exceeds cooldown");
        check(HeartsSkillRules.travelX(500, 100, 900, 440) == 100, "late tick sweep ends at target");

        Zone zone = zone();
        MemoryPlayer owner = player(zone, 1, 240);
        MemoryPlayer enemy = player(zone, 2, 300);
        MemoryPlayer friend = player(zone, 3, 300);
        owner.cFlag = 1;
        enemy.cFlag = 2;
        friend.cFlag = 1;
        MemoryMob mob = mob(zone, 1, 300);
        Skill gravity = select(owner, 71, 1);
        owner.heartsReborn = false;
        check(!SERVICE.begin(owner, 71, 300, 240, 1), "non-reborn user accepted");
        check(owner.nPoint.mp == 1000 && gravity.lastTimeUseThisSkill == 0, "rejected cast cost mana/cooldown");
        owner.heartsReborn = true;
        owner.nPoint.mp = 99;
        check(!SERVICE.begin(owner, 71, 300, 240, 1), "insufficient mana accepted");
        owner.nPoint.mp = 1000;
        HeartsSkillService.Cast cast = begin(owner, 71, 300);
        check(enemy.effectSkill.anTroi && mob.effectSkill.isAnTroi, "gravity must control on acceptance, no charge delay");
        check(!friend.effectSkill.anTroi && friend.hits == 0, "friendly control/damage");
        check(owner.nPoint.mp == 900 && gravity.lastTimeUseThisSkill == NOW.get(), "mana/cooldown not charged once");
        check(!SERVICE.begin(owner, 71, 300, 240, 1) && owner.nPoint.mp == 900, "duplicate cast debited");
        advance(owner, cast, 600);
        check(!SERVICE.isCasting(owner) && enemy.effectSkill.anTroi && packet(owner, 32), "gravity pose must unlock while root persists");
        select(owner, 69, 1);
        advance(owner, cast, 400);
        check(enemy.damage == 300 && enemy.pierced && enemy.sourceId == 71 && mob.damage == 300, "gravity tick must be fixed maxHP and preserve captured identity");
        check(HeartsSkillService.damageSkill(owner).template.id == 69, "damage context leaked");
        advance(owner, cast, 2000);
        check(enemy.damage == 900 && mob.damage == 900 && !enemy.effectSkill.anTroi, "catch-up ticks/control expiry");
        check(friend.hits == 0, "gravity hurt friend");

        NOW.addAndGet(30000);
        owner.playerSkill.skillSelect = gravity;
        cast = begin(owner, 71, 300);
        advance(owner, cast, 600);
        enemy.effectSkill.lastTimeTroi += 1;
        SERVICE.cancelEffects(owner);
        check(enemy.effectSkill.anTroi && !mob.effectSkill.isAnTroi, "cancel cleared someone else's newer root");
        enemy.effectSkill.anTroi = false;

        Zone ranged = zone();
        MemoryPlayer shooter = player(ranged, 4, 240);
        MemoryMob near = mob(ranged, 2, 390), far = mob(ranged, 3, 510);
        select(shooter, 70, 1);
        shooter.setClothes.songoku = 5;
        shooter.playerIntrinsic.intrinsic.id = 2;
        shooter.playerIntrinsic.intrinsic.param1 = 20;
        cast = begin(shooter, 70, 540);
        check(cast.attackDamage == 3960, "Kame intrinsic/set bonus not preserved");
        shooter.nPoint.dame = 1;
        advance(shooter, cast, 440);
        advance(shooter, cast, 1000);
        check(near.hits == 1 && near.damage == 3960 && far.hits == 0, "projectile must snapshot damage and stop at first swept hit");
        check(!SERVICE.isCasting(shooter), "projectile action remains locked");

        // Real airborne targets: each direction must use the same swept 2-D path as the visual.
        int[][] aims = {{480,100}, {240,40}, {80,100}, {380,450}, {240,450}};
        for (int i = 0; i < aims.length; i++) {
            Zone sky = zone();
            MemoryPlayer archer = player(sky, 100 + i, 240);
            MemoryMob flying = mob(sky, 100 + i, aims[i][0]);
            flying.location.y = (short) aims[i][1];
            MemoryMob offPath = mob(sky, 200 + i, 480);
            offPath.location.y = 330;
            select(archer, 70, 1);
            check(SERVICE.begin(archer, 70, flying.location.x, flying.location.y, 1), "airborne cast accepted");
            HeartsSkillService.Cast airCast = archer.newSkill.heartsTechnique;
            check(airCast.targetY != archer.location.y, "aim height flattened");
            advance(archer, airCast, 440);
            check(flying.hits == 0, "distant flying target hit before travel");
            advance(archer, airCast, 440);
            check(flying.hits == 1 && offPath.hits == 0, "missed aerial target or hit off-path target: " + i);
        }
        Zone vertical = zone();
        MemoryPlayer upward = player(vertical, 120, 240);
        MemoryMob higher = mob(vertical, 120, 240), lower = mob(vertical, 121, 240);
        higher.location.y = 40; lower.location.y = 140;
        select(upward, 70, 1);
        check(SERVICE.begin(upward, 70, 240, 40, 1), "vertical cast accepted");
        HeartsSkillService.Cast verticalCast = upward.newSkill.heartsTechnique;
        advance(upward, verticalCast, 440);
        advance(upward, verticalCast, 440);
        check(lower.hits == 1 && higher.hits == 0, "vertical sweep must stop at first contact, not list order");
        Zone meleeSky = zone();
        MemoryPlayer fist = player(meleeSky, 130, 240);
        MemoryMob flyingFistTarget = mob(meleeSky, 130, 300);
        flyingFistTarget.location.y = 180;
        Skill fistSkill = select(fist, 69, 1);
        check(SERVICE.begin(fist, 69, 300, 180, 1), "punch cast accepted");
        HeartsSkillService.Cast fistCast = fist.newSkill.heartsTechnique;
        advance(fist, fistCast, 130);
        check(flyingFistTarget.hits == 0, "ground punch cannot hit 60 pixels above");
        advance(fist, fistCast, 390);
        NOW.addAndGet(fistSkill.coolDown);
        fist.location.y = 180;
        check(SERVICE.begin(fist, 69, 300, 180, 1), "airborne punch accepted after approach");
        fistCast = fist.newSkill.heartsTechnique;
        advance(fist, fistCast, 130);
        check(flyingFistTarget.hits == 1, "punch at target altitude missed");
        advance(fist, fistCast, 390);
        check(Double.isInfinite(HeartsSkillRules.sweptContact(100, 240, 240, 240, 240, 40, 30)),
                "vertical sweep admitted horizontally distant target");

        Zone duel = zone();
        MemoryPlayer kicker = player(duel, 5, 240), victim = player(duel, 6, 300);
        kicker.cFlag = 1;
        victim.cFlag = 2;
        select(kicker, 72, 1);
        kicker.nPoint.tlSDCM = 10;
        cast = begin(kicker, 72, 300);
        advance(kicker, cast, 150);
        check(victim.damage == 2200 && victim.critical && victim.effectSkill.isStun, "kick must guarantee critical and stun");
        advance(kicker, cast, 400);
        check(!SERVICE.isCasting(kicker) && victim.effectSkill.isStun, "kick stun should outlast its action");
        ((MemorySession) kicker.session).connected = false;
        SERVICE.updateEffects(kicker);
        check(!victim.effectSkill.isStun && packet(kicker, 30), "disconnect must release control");

        Zone bombZone = zone();
        MemoryPlayer bomber = player(bombZone, 7, 240), ally = player(bombZone, 8, 280);
        MemoryMob bombTarget = mob(bombZone, 4, 400);
        select(bomber, 73, 1);
        cast = begin(bomber, 73, 400);
        check(cast.attackDamage == 1320, "ultimate must be 120% of QCKK attack+nearbyHP/200 incl ally energy");
        advance(bomber, cast, 3999);
        check(bombTarget.hits == 0, "ultimate fired before preparation");
        advance(bomber, cast, 1);
        advance(bomber, cast, 1200);
        check(bombTarget.damage == 1320 && ally.hits == 0, "ultimate AoE damages only valid foes");

        Zone cancellation = zone();
        MemoryPlayer caster = player(cancellation, 9, 240);
        select(caster, 73, 1);
        cast = begin(caster, 73, 400);
        caster.zone = zone();
        advance(caster, cast, 50);
        check(!SERVICE.isCasting(caster) && packet(caster, 30), "zone-change charge cancellation");
        System.out.println("PASS: Hearts authorization, mana/cooldown, instant gravity, 3% ticks, PvP/friendly policy, control leases, captured damage, Kame modifiers, single-hit cube, critical kick, QCKK ultimate and cancellation");
    }
}
