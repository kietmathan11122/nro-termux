package nro.models.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import nro.models.map.Zone;
import nro.models.mob.Mob;
import nro.models.network.Message;
import nro.models.player.NewSkill;
import nro.models.player.Player;
import nro.models.skill.Skill;

/** Server-authoritative, PvE-only casts. No client position or hit list is trusted. */
public final class PlanetTechniqueService {
    private static final PlanetTechniqueService INSTANCE = new PlanetTechniqueService();
    // Weak identity-by-entity keys do not retain despawned bosses between casts.
    private final java.util.Map<Object, Long> bossControlUntil = new WeakHashMap<>();
    private java.util.function.LongSupplier clock = System::currentTimeMillis;
    // Released debuffs keep ticking without occupying NewSkill's action/timer state.
    private final java.util.concurrent.ConcurrentMap<Player, List<Cast>> lingering = new java.util.concurrent.ConcurrentHashMap<>();
    private PlanetTechniqueService() { }
    public static PlanetTechniqueService gI() { return INSTANCE; }

    public static final class Cast {
        public final Zone zone;
        public final Skill skill;
        public final int id, level, targetX, direction;
        public int targetY;
        public final long startedAt, attackDamage;
        public int originX, originY, previousX, vineTicks;
        public long releasedAt;
        public boolean exploded, completed;
        private final Set<Object> hit = Collections.newSetFromMap(new IdentityHashMap<>());
        private final List<Control> controls = new ArrayList<>();
        private final List<Object> vineTargets = new ArrayList<>();
        private final List<Bleed> bleeds = new ArrayList<>();
        private final Set<Object> bled = Collections.newSetFromMap(new IdentityHashMap<>());

        private Cast(Player owner, Skill skill, int tx, int ty, int direction) {
            this.zone = owner.zone;
            this.skill = skill;
            this.id = skill.template.id;
            this.level = PlanetTechniqueRules.level(skill.point);
            this.startedAt = INSTANCE.clock.getAsLong();
            this.attackDamage = Math.max(1, owner.nPoint.dame);
            this.originX = owner.location.x;
            this.originY = owner.location.y;
            this.previousX = originX;
            this.targetX = tx;
            this.targetY = ty;
            this.direction = direction;
        }
    }

    private static final class Bleed {
        final Object target;
        final long started;
        int ticks;
        Bleed(Object target, long started) { this.target = target; this.started = started; }
    }

    private static final class Control {
        final Object target;
        final boolean root;
        final long started, until;
        Control(Object target, boolean root, long started, int duration) {
            this.target = target;
            this.root = root;
            this.started = started;
            this.until = started + duration;
        }
    }

    public boolean begin(Player owner, int requestedId, int requestedX, int requestedY, int requestedDir) {
        if (owner == null || owner.newSkill == null || owner.zone == null || owner.isDie()
                || owner.isBoss || owner.isPet || owner.isNewPet || owner.playerSkill == null
                || owner.playerSkill.skillSelect == null || owner.playerSkill.skillSelect.template == null) return false;
        Skill selected = owner.playerSkill.skillSelect;
        if (!PlanetTechniqueRules.handles(requestedId) || selected.template.id != requestedId
                || selected.template.classId != owner.gender || selected.point < 1
                || owner.newSkill.isStartSkillSpecial) return false;
        int ox = owner.location.x, oy = owner.location.y;
        // The legacy client sends (-1,-1) when there is no focused target.
        if (requestedX == -1 && requestedY == -1) {
            requestedX = ox + (requestedDir < 0 ? -1 : 1) * PlanetTechniqueRules.range(requestedId);
            requestedY = oy;
        }
        int direction = requestedX == ox ? (requestedDir < 0 ? -1 : 1) : requestedX < ox ? -1 : 1;
        int distance = PlanetTechniqueRules.range(requestedId);
        int tx = PlanetTechniqueRules.clamp(requestedX, ox - distance, ox + distance);
        tx = PlanetTechniqueRules.clamp(tx, 24, Math.max(24, owner.zone.map.mapWidth - 24));
        int ty = requestedId == Skill.MOC_DANG
                ? PlanetTechniqueRules.clamp(requestedY, oy - 160, oy + 160) : oy;
        ty = PlanetTechniqueRules.clamp(ty, 24, Math.max(24, owner.zone.map.mapHeight - 24));
        Cast cast = new Cast(owner, selected, tx, ty, direction);
        NewSkill state = owner.newSkill;
        state.closeSkillSpecial();
        state.planetTechnique = cast;
        state.skillSelect = selected;
        state.dir = (byte) direction;
        state._xPlayer = (short) ox;
        state._yPlayer = (short) oy;
        state.isStartSkillSpecial = true;
        state.lastTimeSkillSpecial = cast.startedAt;
        if (selected.currLevel < 1000) {
            selected.currLevel++;
            SkillService.gI().sendCurrLevelSpecial(owner, selected);
        }
        send(owner, cast, 20);
        state.start(250);
        return true;
    }

    public boolean isCasting(Player owner) {
        if (owner == null || owner.newSkill == null || owner.newSkill.planetTechnique == null) return false;
        return true; // Release the lock only when our action state has actually been detached.
    }

    public void update(Player owner, Cast cast) {
        if (owner == null || owner.newSkill == null || cast == null || owner.newSkill.planetTechnique != cast) return;
        long now = clock.getAsLong();
        if (!validOwner(owner, cast) || owner.effectSkill == null || owner.effectSkill.isHaveEffectSkill()) {
            owner.newSkill.closeSkillSpecial();
            return;
        }
        cleanupControls(owner, cast, now, false);
        if (cast.releasedAt == 0) {
            // Walking/teleporting away interrupts the charge, without moving the effect origin.
            if (Math.abs(owner.location.x - cast.originX) > 32 || Math.abs(owner.location.y - cast.originY) > 48) {
                owner.newSkill.closeSkillSpecial();
                return;
            }
            if (now - cast.startedAt < PlanetTechniqueRules.CHARGE_MS) return;
            if (cast.id == Skill.THIEN_LONG_QUYEN) {
                dashOnGround(owner, cast);
                cast.targetY = cast.originY;
            }
            cast.releasedAt = now;
            cast.previousX = cast.originX;
            collectPacketTargets(owner, cast);
            send(owner, cast, 21);
            if (cast.id == Skill.MOC_DANG) {
                cast.vineTargets.addAll(owner.newSkill.playersTaget);
                cast.vineTargets.addAll(owner.newSkill.mobsTaget);
                for (Object target : cast.vineTargets)
                    sendHit(owner, cast, target, control(owner, cast, target, true, now), false);
            }
        }
        long elapsed = now - cast.releasedAt;
        if (cast.id == Skill.MOC_DANG) {
            // Tick in the shared post-hit lifecycle below, not as a long channeled cast.
        } else {
            int travel = cast.id == Skill.THIEN_LONG_QUYEN
                    ? PlanetTechniqueRules.DRAGON_TRAVEL_MS : PlanetTechniqueRules.TORNADO_TRAVEL_MS;
            int nextX = PlanetTechniqueRules.travelX(cast.originX, cast.targetX, elapsed, travel);
            // Sweep the remaining segment even after a delayed timer tick.
            if (!cast.exploded) {
                for (Object target : targets(cast)) {
                    if (!validTarget(owner, cast, target) || cast.hit.contains(target)) continue;
                    if (PlanetTechniqueRules.inSweptLane(x(target), y(target), cast.previousX, nextX,
                            cast.originY, PlanetTechniqueRules.radius(cast.id))) {
                        long amount = PlanetTechniqueRules.scaledDamage(cast.attackDamage,
                                PlanetTechniqueRules.damagePercent(cast.id, cast.level));
                        damage(owner, target, cast.id == Skill.THIEN_LONG_QUYEN ? amount * 70 / 100 : amount);
                        cast.hit.add(target);
                        if (cast.id == Skill.THIEN_LONG_QUYEN) applyBleed(owner, cast, target, now);
                        if (cast.id == Skill.CUONG_PHONG) {
                            int duration = control(owner, cast, target, false, now);
                            sendHit(owner, cast, target, duration, true);
                        }
                    }
                }
            }
            cast.previousX = nextX;
            if (elapsed >= travel && !cast.exploded) {
                cast.exploded = true;
                if (cast.id == Skill.THIEN_LONG_QUYEN) {
                    long amount = PlanetTechniqueRules.scaledDamage(cast.attackDamage,
                            PlanetTechniqueRules.damagePercent(cast.id, cast.level));
                    for (Object target : targets(cast)) {
                        if (validTarget(owner, cast, target) && PlanetTechniqueRules.inCircle(x(target), y(target),
                                cast.targetX, cast.targetY, PlanetTechniqueRules.radius(cast.id))) {
                            damage(owner, target, Math.max(1, amount * 30 / 100));
                            applyBleed(owner, cast, target, now);
                        }
                    }
                }
            }
        }
        tickDebuffs(owner, cast, now);
        if (elapsed >= PlanetTechniqueRules.actionDuration(cast.id)) {
            List<Cast> effects = lingering.computeIfAbsent(owner, ignored -> new ArrayList<>());
            synchronized (effects) { effects.add(cast); }
            // Clear only this cast before closing the old action timer. Do not cancel its debuffs.
            owner.newSkill.planetTechnique = null;
            send(owner, cast, 32);
            owner.newSkill.closeSkillSpecial();
        }
    }

    private void applyBleed(Player owner, Cast cast, Object target, long now) {
        if (!validTarget(owner, cast, target) || !cast.bled.add(target)) return;
        cast.bleeds.add(new Bleed(target, now));
        sendHit(owner, cast, target, control(owner, cast, target, false, now), true);
    }

    private long maxHp(Object target) {
        return target instanceof Mob ? ((Mob) target).point.maxHp : ((Player) target).nPoint.hpMax;
    }

    private void tickDebuffs(Player owner, Cast cast, long now) {
        if (cast.id == Skill.MOC_DANG) {
            long elapsed = now - cast.releasedAt;
            while (cast.vineTicks < PlanetTechniqueRules.vineTicks(cast.level)
                    && elapsed >= (cast.vineTicks + 1) * 1000L) {
                for (Object target : cast.vineTargets) {
                    if (!validTarget(owner, cast, target)) continue;
                    damage(owner, target, PlanetTechniqueRules.vineTickDamage(maxHp(target), cast.level));
                    sendHit(owner, cast, target, 0, true);
                }
                cast.vineTicks++;
            }
        }
        for (Iterator<Bleed> it = cast.bleeds.iterator(); it.hasNext();) {
            Bleed bleed = it.next();
            while (bleed.ticks < PlanetTechniqueRules.BLEED_TICKS && now - bleed.started >= (bleed.ticks + 1) * 1000L
                    && validTarget(owner, cast, bleed.target)) {
                damage(owner, bleed.target, PlanetTechniqueRules.bleedTickDamage(maxHp(bleed.target), cast.level));
                sendHit(owner, cast, bleed.target, 0, true);
                bleed.ticks++;
            }
            if (bleed.ticks == PlanetTechniqueRules.BLEED_TICKS || !validTarget(owner, cast, bleed.target)) it.remove();
        }
    }

    /** Called by Player.update even when no special attack is being cast. */
    public void updateEffects(Player owner) {
        List<Cast> effects = lingering.get(owner);
        if (effects == null) return;
        synchronized (effects) {
            long now = clock.getAsLong();
            for (Iterator<Cast> it = effects.iterator(); it.hasNext();) {
                Cast cast = it.next();
                if (!validOwner(owner, cast)) { cancel(owner, cast); it.remove(); continue; }
                tickDebuffs(owner, cast, now);
                cleanupControls(owner, cast, now, false);
                if (cast.bleeds.isEmpty() && cast.controls.isEmpty()
                        && (cast.id != Skill.MOC_DANG || cast.vineTicks >= PlanetTechniqueRules.vineTicks(cast.level))) {
                    cast.completed = true;
                    cancel(owner, cast);
                    it.remove();
                }
            }
            // Keep an empty bucket until dispose: avoids detach racing a bucket removal.
        }
    }

    public void cancelEffects(Player owner) {
        List<Cast> effects = lingering.remove(owner);
        if (effects != null) synchronized (effects) {
            for (Cast cast : effects) cancel(owner, cast);
            effects.clear();
        }
    }

    private boolean validOwner(Player owner, Cast cast) {
        return owner != null && owner.newSkill != null && owner.zone == cast.zone && !owner.isDie()
                && owner.getSession() != null && owner.getSession().isConnected()
                && cast.zone.getPlayers().contains(owner);
    }
    private boolean validTarget(Player owner, Cast cast, Object target) {
        if (target instanceof Mob) {
            Mob mob = (Mob) target;
            return mob.zone == cast.zone && !mob.isDie() && cast.zone.mobs.contains(mob);
        }
        if (!(target instanceof Player)) return false;
        Player boss = (Player) target;
        return boss != owner && boss.isBoss && !boss.isPet && !boss.isNewPet && !boss.isDie()
                && boss.zone == cast.zone && cast.zone.getBosses().contains(boss)
                && SkillService.gI().canAttackPlayer(owner, boss);
    }
    private List<Object> targets(Cast cast) {
        List<Object> all = new ArrayList<>();
        all.addAll(new ArrayList<>(cast.zone.mobs));
        all.addAll(new ArrayList<>(cast.zone.getBosses()));
        return all;
    }
    private static int x(Object target) { return target instanceof Mob ? ((Mob) target).location.x : ((Player) target).location.x; }
    private static int y(Object target) { return target instanceof Mob ? ((Mob) target).location.y : ((Player) target).location.y; }
    private static boolean isBoss(Object target) { return target instanceof Player || ((Mob) target).isBigBoss(); }

    private void collectPacketTargets(Player owner, Cast cast) {
        owner.newSkill.playersTaget.clear();
        owner.newSkill.mobsTaget.clear();
        int count = 0;
        for (Object target : targets(cast)) {
            if (!validTarget(owner, cast, target)) continue;
            boolean inside = cast.id == Skill.MOC_DANG
                    ? PlanetTechniqueRules.inCircle(x(target), y(target), cast.targetX, cast.targetY, 120)
                    : PlanetTechniqueRules.inSweptLane(x(target), y(target), cast.originX, cast.targetX,
                            cast.originY, PlanetTechniqueRules.radius(cast.id));
            if (!inside || count >= 120) continue; // Packet count is signed byte on the legacy client.
            if (target instanceof Mob) owner.newSkill.mobsTaget.add((Mob) target);
            else owner.newSkill.playersTaget.add((Player) target);
            count++;
        }
    }

    private void damage(Player owner, Object target, long amount) {
        amount = Math.max(1, amount);
        if (target instanceof Mob) {
            // Explicit damage avoids the old lvMob branch replacing a capped DoT with 10% HP.
            ((Mob) target).injured(owner, amount, true);
        } else {
            Player boss = (Player) target;
            boss.injured(owner, amount, false, false); // Respect boss shields/phases/reward attribution.
            Service.gI().reload_HP_NV(boss);
        }
    }

    private int control(Player owner, Cast cast, Object target, boolean root, long now) {
        if (!validTarget(owner, cast, target)) return 0;
        if (target instanceof Mob ? ((Mob) target).effectSkill == null
                : ((Player) target).effectSkill == null) return 0;
        if (target instanceof Mob ? ((Mob) target).effectSkill.isHaveEffectSkill()
                : ((Player) target).effectSkill.isHaveEffectSkill()) return 0;
        boolean boss = isBoss(target);
        if (boss && cast.id == Skill.CUONG_PHONG) {
            synchronized (bossControlUntil) {
                Long until = bossControlUntil.get(target);
                if (until != null && until > now) return 0;
                bossControlUntil.put(target, now + PlanetTechniqueRules.BOSS_CONTROL_IMMUNITY_MS);
            }
        }
        int duration = root ? PlanetTechniqueRules.duration(68, cast.level)
                : cast.id == Skill.THIEN_LONG_QUYEN ? PlanetTechniqueRules.punchStunMs(cast.level)
                : (boss ? 600 : 1500);
        if (target instanceof Mob) {
            Mob mob = (Mob) target;
            if (root) mob.effectSkill.setTroi(now, duration);
            else {
                mob.effectSkill.startStun(now, duration);
                EffectSkillService.gI().sendEffectMob(owner, mob, EffectSkillService.TURN_ON_EFFECT,
                        EffectSkillService.BLIND_EFFECT);
            }
        } else {
            Player player = (Player) target;
            if (root) EffectSkillService.gI().setAnTroi(player, owner, now, duration);
            else EffectSkillService.gI().startStun(player, now, duration);
        }
        cast.controls.add(new Control(target, root, now, duration));
        return duration;
    }

    private void cleanupControls(Player owner, Cast cast, long now, boolean force) {
        for (Iterator<Control> it = cast.controls.iterator(); it.hasNext();) {
            Control control = it.next();
            if (!force && now < control.until && validTarget(owner, cast, control.target)) continue;
            // Only undo our lease, never a newer effect from a different skill/caster.
            if (control.target instanceof Mob) {
                Mob mob = (Mob) control.target;
                if (mob.effectSkill != null) {
                    if (control.root && mob.effectSkill.isAnTroi && mob.effectSkill.lastTimeAnTroi == control.started)
                        mob.effectSkill.removeAnTroi();
                    else if (!control.root && mob.effectSkill.isStun && mob.effectSkill.lastTimeStun == control.started) {
                        mob.effectSkill.timeStun = 0;
                        mob.effectSkill.update();
                    }
                }
            } else {
                Player boss = (Player) control.target;
                if (boss.effectSkill != null) {
                    if (control.root && boss.effectSkill.anTroi && boss.effectSkill.lastTimeTroi == control.started)
                        EffectSkillService.gI().removeAnTroi(boss);
                    else if (!control.root && boss.effectSkill.isStun && boss.effectSkill.lastTimeStartStun == control.started)
                        EffectSkillService.gI().removeStun(boss);
                }
            }
            it.remove();
        }
    }

    public void cancel(Player owner, Cast cast) {
        if (owner != null && !cast.completed) send(owner, cast, 30);
        cleanupControls(owner, cast, clock.getAsLong(), true);
        cast.hit.clear();
        cast.vineTargets.clear();
        cast.bleeds.clear();
        cast.bled.clear();
    }

    /** Confirm actual hits/CC; clients must not animate stun from a predicted hit list. */
    private void sendHit(Player owner, Cast cast, Object target, int controlMs, boolean pulse) {
        Message message = null;
        try {
            message = new Message(-45);
            message.writer().writeByte(31);
            message.writer().writeInt((int) owner.id);
            message.writer().writeShort(cast.id);
            message.writer().writeByte(target instanceof Mob ? 0 : 1);
            message.writer().writeInt(target instanceof Mob ? ((Mob) target).id : (int) ((Player) target).id);
            message.writer().writeShort(controlMs);
            message.writer().writeByte(pulse ? 1 : 0);
            Service.gI().sendMessAllPlayerInMap(cast.zone, message);
        } catch (Exception ex) {
            nro.models.utils.Logger.logException(PlanetTechniqueService.class, ex);
        } finally {
            if (message != null) message.cleanup();
        }
    }

    private void dashOnGround(Player owner, Cast cast) {
        nro.models.map.Map map = cast.zone.map;
        if (map.tileMap == null || map.types == null || map.tmw <= 0) return;
        int wanted = Math.min(PlanetTechniqueRules.MAX_DASH, Math.max(0, Math.abs(cast.targetX - cast.originX) - 48));
        int lastX = cast.originX, lastY = cast.originY;
        int originalGround = map.yPhysicInTop(lastX, lastY - 24);
        if (Math.abs(originalGround - lastY) > 24) return; // Do not teleport an airborne caster to the floor.
        for (int distance = 6; distance <= wanted; distance += 6) {
            int nx = cast.originX + distance * cast.direction;
            if (nx < 24 || nx >= map.mapWidth - 24) break;
            int ny = map.yPhysicInTop(nx, lastY - 24);
            if (ny < 24 || ny >= map.mapHeight || Math.abs(ny - lastY) > 24) break;
            if (solid(map, nx, ny - 12) || solid(map, nx, ny - 36)) break;
            lastX = nx;
            lastY = ny;
        }
        if (lastX != cast.originX) Service.gI().setPos(owner, lastX, lastY);
        cast.originX = owner.location.x;
        cast.originY = owner.location.y;
    }
    private static boolean solid(nro.models.map.Map map, int x, int y) {
        int index = y / 24 * map.tmw + x / 24;
        return y < 0 || index < 0 || index >= map.types.length || (map.types[index] & 2) != 0;
    }

    public void send(Player owner, Cast cast, int status) {
        Message message = null;
        try {
            message = new Message(-45);
            message.writer().writeByte(status);
            message.writer().writeInt((int) owner.id);
            message.writer().writeShort(cast.id);
            if (status == 20) {
                message.writer().writeByte(cast.id == 66 ? 1 : cast.id == 67 ? 3 : 2);
                message.writer().writeByte(cast.direction);
                message.writer().writeShort(PlanetTechniqueRules.CHARGE_MS);
                message.writer().writeByte(0);
                message.writer().writeByte(0);
                message.writer().writeByte(0);
            } else if (status == 21) {
                message.writer().writeShort(cast.targetX);
                message.writer().writeShort(cast.targetY);
                message.writer().writeShort(PlanetTechniqueRules.duration(cast.id, cast.level));
                message.writer().writeShort(PlanetTechniqueRules.radius(cast.id));
                message.writer().writeByte(0);
                message.writer().writeByte(owner.newSkill.playersTaget.size() + owner.newSkill.mobsTaget.size());
                for (Player boss : owner.newSkill.playersTaget) {
                    message.writer().writeByte(1);
                    message.writer().writeInt((int) boss.id);
                }
                for (Mob mob : owner.newSkill.mobsTaget) {
                    message.writer().writeByte(0);
                    message.writer().writeByte(mob.id);
                }
                message.writer().writeByte(0);
            }
            Service.gI().sendMessAllPlayerInMap(cast.zone, message);
        } catch (Exception ex) {
            nro.models.utils.Logger.logException(PlanetTechniqueService.class, ex);
        } finally {
            if (message != null) message.cleanup();
        }
    }
}
