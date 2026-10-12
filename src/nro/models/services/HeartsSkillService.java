package nro.models.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import nro.models.map.Zone;
import nro.models.map.service.MapService;
import nro.models.mob.Mob;
import nro.models.network.Message;
import nro.models.player.NewSkill;
import nro.models.player.Player;
import nro.models.skill.Skill;
import nro.models.utils.Logger;
import nro.models.utils.Util;

/** Authoritative Hearts casts: client coordinates aim the attack, never identify a hit. */
public final class HeartsSkillService {
    private static final HeartsSkillService INSTANCE = new HeartsSkillService();
    private static final ThreadLocal<DamageContext> DAMAGE_CONTEXT = new ThreadLocal<>();
    private final ConcurrentMap<Player, List<Cast>> lingering = new ConcurrentHashMap<>();
    private java.util.function.LongSupplier clock = System::currentTimeMillis;
    private HeartsSkillService() { }
    public static HeartsSkillService gI() { return INSTANCE; }

    private static final class DamageContext {
        final Player owner;
        final Skill skill;
        DamageContext(Player owner, Skill skill) { this.owner = owner; this.skill = skill; }
    }

    /** Damage receivers use the captured skill even if the caster has selected another one. */
    public static Skill damageSkill(Player owner) {
        if (owner == null) return null;
        DamageContext context = DAMAGE_CONTEXT.get();
        if (context != null && context.owner == owner) return context.skill;
        return owner.playerSkill == null ? null : owner.playerSkill.skillSelect;
    }

    public static final class Cast {
        public final Zone zone;
        public final Skill skill;
        public final int id, level, targetX, targetY, direction;
        public final long startedAt;
        public int originX, originY, previousX, previousY, gravityTicks;
        public long releasedAt, attackDamage, mobDamage;
        public boolean released, hitDone, completed, critical;
        public Object lockedTarget;
        private final Set<Object> hit = Collections.newSetFromMap(new IdentityHashMap<>());
        private final List<Object> gravityTargets = new ArrayList<>();
        private final List<Control> controls = new ArrayList<>();

        private Cast(Player owner, Skill selected, int tx, int ty, int direction, long now) {
            zone = owner.zone;
            skill = new Skill(selected); // Persist source identity and balance throughout delayed damage.
            id = selected.template.id;
            level = HeartsSkillRules.level(selected.point);
            startedAt = now;
            originX = previousX = owner.location.x;
            originY = previousY = owner.location.y;
            targetX = tx;
            targetY = ty;
            this.direction = direction;
        }
    }

    private static final class Control {
        final Object target;
        final boolean root;
        final long started, until;
        Control(Object target, boolean root, long now, int duration) {
            this.target = target;
            this.root = root;
            started = now;
            until = now + duration;
        }
    }

    public boolean begin(Player owner, int requestedId, int requestedX, int requestedY, int requestedDir) {
        if (owner == null || owner.newSkill == null || owner.playerSkill == null || owner.nPoint == null
                || owner.zone == null || owner.zone.map == null || owner.location == null || owner.isDie()
                || owner.isBoss || owner.isPet || owner.isNewPet || !HeartsRebirth.isReborn(owner)) return false;
        NewSkill state = owner.newSkill;
        synchronized (state) {
            Skill selected = owner.playerSkill.skillSelect;
            long now = clock.getAsLong();
            if (!HeartsSkillRules.handles(requestedId) || selected == null || selected.template == null
                    || selected.template.id != requestedId || selected.template.classId != owner.gender
                    || selected.point < 1 || !owner.playerSkill.skills.contains(selected)
                    || state.isStartSkillSpecial || owner.effectSkill == null || owner.effectSkill.isHaveEffectSkill()
                    || owner.getSession() == null || !owner.getSession().isConnected()
                    || !owner.zone.getPlayers().contains(owner)
                    || now - selected.lastTimeUseThisSkill < Math.max(0, selected.coolDown)) return false;
            long mana = manaCost(owner, selected);
            if (mana < 0 || owner.nPoint.mp < mana) return false;
            int ox = owner.location.x, oy = owner.location.y;
            int range = HeartsSkillRules.range(requestedId);
            if (requestedX == -1 && requestedY == -1) {
                requestedX = ox + (requestedDir < 0 ? -1 : 1) * range;
                requestedY = oy;
            }
            int direction = requestedX == ox ? (requestedDir < 0 ? -1 : 1) : requestedX < ox ? -1 : 1;
            int tx = HeartsSkillRules.clamp(requestedX, ox - range, ox + range);
            tx = HeartsSkillRules.clamp(tx, 24, Math.max(24, owner.zone.map.mapWidth - 24));
            int verticalRange = requestedId == Skill.HEARTS_CUBE || requestedId == Skill.HEARTS_CRYSTAL_BARRAGE
                    || requestedId == Skill.HEARTS_CUBE_PRISON
                    || requestedId == Skill.HEARTS_PUNCH ? range : 160;
            int ty = requestedId == Skill.HEARTS_GRAVITY || requestedId == Skill.HEARTS_ULTIMATE
                    || requestedId == Skill.HEARTS_CUBE || requestedId == Skill.HEARTS_CRYSTAL_BARRAGE
                    || requestedId == Skill.HEARTS_CUBE_PRISON
                    || requestedId == Skill.HEARTS_PUNCH
                    ? HeartsSkillRules.clamp(requestedY, oy - verticalRange, oy + verticalRange) : oy;
            if (requestedId == Skill.HEARTS_CUBE || requestedId == Skill.HEARTS_CRYSTAL_BARRAGE
                    || requestedId == Skill.HEARTS_CUBE_PRISON) {
                // Preserve the existing maximum range in every direction, not a larger diagonal square.
                double dx = (double) tx - ox, dy = (double) ty - oy;
                double distance = Math.hypot(dx, dy);
                if (distance > range) {
                    tx = ox + (int) Math.round(dx * range / distance);
                    ty = oy + (int) Math.round(dy * range / distance);
                }
            }
            ty = HeartsSkillRules.clamp(ty, 24, Math.max(24, owner.zone.map.mapHeight - 24));
            Cast cast = new Cast(owner, selected, tx, ty, direction, now);
            cast.lockedTarget = aimedTarget(owner, cast);
            snapshotDamage(owner, cast);
            state.closeSkillSpecial();
            state.heartsTechnique = cast;
            state.skillSelect = selected;
            state.dir = (byte) direction;
            state._xPlayer = (short) ox;
            state._yPlayer = (short) oy;
            state.isStartSkillSpecial = true;
            state.lastTimeSkillSpecial = now;
            // Debit exactly once after validation, before any instant hit or timer callback.
            selected.lastTimeUseThisSkill = now;
            owner.nPoint.setMp(owner.nPoint.mp - mana);
            PlayerService.gI().sendInfoHpMpMoney(owner);
            if (selected.currLevel < 1000) {
                selected.currLevel++;
                SkillService.gI().sendCurrLevelSpecial(owner, selected);
            }
            send(owner, cast, 20);
            if (HeartsSkillRules.chargeMs(cast.id) == 0) update(owner, cast);
            state.start(25);
            return true;
        }
    }

    private static long manaCost(Player owner, Skill skill) {
        if (skill.manaUse < 0) return -1;
        return switch (skill.template.manaUseType) {
            case 0 -> skill.manaUse;
            case 1 -> HeartsSkillRules.percent(owner.nPoint.mpMax, skill.manaUse);
            case 2 -> owner.nPoint.mp > 0 ? owner.nPoint.mp : -1;
            default -> -1;
        };
    }

    private void snapshotDamage(Player owner, Cast cast) {
        long amount = Math.max(1, owner.nPoint.dame);
        if (cast.id == Skill.HEARTS_ULTIMATE) {
            // QCKK actually uses nearby HP / 200 + attack, not its catalogue damage field.
            long nearbyHp = 0;
            for (Object target : targets(cast)) {
                boolean alive = target instanceof Mob ? !((Mob) target).isDie() : target != owner && !((Player) target).isDie();
                if (alive && HeartsSkillRules.inCircle(x(target), y(target),
                        cast.originX, cast.originY, HeartsSkillRules.radius(cast.id, cast.level))) {
                    long hp = target instanceof Mob ? ((Mob) target).point.hp : ((Player) target).nPoint.hp;
                    nearbyHp = Math.min(Long.MAX_VALUE - Math.max(0, hp), nearbyHp) + Math.max(0, hp);
                }
            }
            amount += nearbyHp / 200;
            if (owner.setClothes != null && owner.setClothes.kirin == 5) amount = HeartsSkillRules.percent(amount, 200);
            cast.attackDamage = cast.mobDamage = HeartsSkillRules.attackDamage(amount, 120);
            return;
        }
        if (cast.id == Skill.HEARTS_GRAVITY) return;
        nro.models.intrinsic.Intrinsic intrinsic = owner.playerIntrinsic == null ? null : owner.playerIntrinsic.intrinsic;
        int intrinsicPercent = 0, clothesPercent = 0;
        if (cast.id == Skill.HEARTS_PUNCH) {
            if (intrinsic != null && intrinsic.id == 16) intrinsicPercent = intrinsic.param1;
            if (owner.setClothes != null && owner.setClothes.kakarot == 5) clothesPercent = 100;
        } else if (cast.id == Skill.HEARTS_CUBE) {
            if (intrinsic != null && intrinsic.id == 2) intrinsicPercent = intrinsic.param1;
            if (owner.setClothes != null && owner.setClothes.songoku == 5) clothesPercent = 100;
        }
        if (intrinsic != null && intrinsic.id == 18 && owner.effectSkill.isMonkey) intrinsicPercent = intrinsic.param1;
        if (cast.id == Skill.HEARTS_KICK) {
            // DCTT's damage field is stun milliseconds; its attack is a guaranteed critical.
            cast.critical = true;
            owner.nPoint.isCritTele = true;
        } else {
            amount = HeartsSkillRules.percent(amount, Math.max(1, cast.skill.damage));
            cast.critical = owner.nPoint.isCrit100 || owner.nPoint.isCritTele || Util.isTrue(owner.nPoint.crit, 100)
                    || (intrinsic != null && intrinsic.id == 25 && owner.nPoint.getCurrPercentHP() <= intrinsic.param1);
            owner.nPoint.isCrit100 = false;
            owner.nPoint.isCritTele = false;
        }
        amount += HeartsSkillRules.percent(amount, Math.max(0, intrinsicPercent));
        amount += HeartsSkillRules.percent(amount, Math.max(0, owner.nPoint.dameAfter));
        owner.nPoint.dameAfter = 0;
        if (owner.effectSkill.isDameBuff && owner.nPoint.tlSexyDame == 0)
            amount += HeartsSkillRules.percent(amount, Math.max(0, owner.effectSkill.tileDameBuff));
        long mobAmount = amount;
        if (owner.nPoint.tlDameAttMob != null) for (Integer bonus : owner.nPoint.tlDameAttMob)
            if (bonus != null) mobAmount += HeartsSkillRules.percent(mobAmount, Math.max(0, bonus));
        int finalPercent = cast.critical ? 200 + Math.max(0, owner.nPoint.tlSDCM) * 2 : 100;
        amount = HeartsSkillRules.percent(amount, finalPercent);
        mobAmount = HeartsSkillRules.percent(mobAmount, finalPercent);
        amount += HeartsSkillRules.percent(amount, clothesPercent);
        mobAmount += HeartsSkillRules.percent(mobAmount, clothesPercent);
        if (cast.id == Skill.HEARTS_CUBE && owner.effectSkin != null && owner.effectSkin.isXChuong) {
            amount = HeartsSkillRules.percent(amount, Math.max(1, owner.nPoint.xChuong) * 100);
            mobAmount = HeartsSkillRules.percent(mobAmount, Math.max(1, owner.nPoint.xChuong) * 100);
            owner.effectSkin.isXDame = true;
            owner.effectSkin.isXChuong = false;
            owner.effectSkin.lastTimeXChuong = cast.startedAt;
        }
        if (owner.effectSkill.isSocola) {
            amount = HeartsSkillRules.percent(amount, 30);
            mobAmount = HeartsSkillRules.percent(mobAmount, 30);
        }
        cast.attackDamage = Math.max(1, Math.min(Integer.MAX_VALUE, amount));
        cast.mobDamage = Math.max(1, Math.min(Integer.MAX_VALUE, mobAmount));
    }

    public boolean isCasting(Player owner) {
        return owner != null && owner.newSkill != null && owner.newSkill.heartsTechnique != null;
    }

    public void update(Player owner, Cast cast) {
        if (owner == null || owner.newSkill == null || cast == null || owner.newSkill.heartsTechnique != cast) return;
        long now = clock.getAsLong();
        if (!validOwner(owner, cast) || owner.effectSkill == null || owner.effectSkill.isHaveEffectSkill()) {
            owner.newSkill.closeSkillSpecial();
            return;
        }
        cleanupControls(owner, cast, now, false);
        if (!cast.released) {
            if (Math.abs(owner.location.x - cast.originX) > 32 || Math.abs(owner.location.y - cast.originY) > 48) {
                owner.newSkill.closeSkillSpecial();
                return;
            }
            if (now - cast.startedAt < HeartsSkillRules.chargeMs(cast.id)) return;
            if (cast.id == Skill.HEARTS_KICK) dashOnGround(owner, cast);
            cast.released = true;
            cast.releasedAt = now;
            cast.previousX = cast.originX;
            cast.previousY = cast.originY;
            send(owner, cast, 21);
            if (cast.id == Skill.HEARTS_GRAVITY) {
                for (Object target : targets(cast)) {
                    if (!validTarget(owner, cast, target) || !inArea(cast, target)) continue;
                    int duration = control(owner, cast, target, true, now);
                    cast.gravityTargets.add(target);
                    sendHit(owner, cast, target, duration, false);
                }
            } else if (cast.id == Skill.HEARTS_CUBE_PRISON) {
                Object target = cast.lockedTarget;
                if (target != null && validTarget(owner, cast, target)) {
                    int duration = control(owner, cast, target, true, now);
                    sendHit(owner, cast, target, duration, false);
                }
            } else if (cast.id == Skill.HEARTS_PUNCH || cast.id == Skill.HEARTS_KICK) {
                Object target = nearestMeleeTarget(owner, cast);
                if (target != null) {
                    damage(owner, cast, target, target instanceof Mob ? cast.mobDamage : cast.attackDamage);
                    int duration = cast.id == Skill.HEARTS_KICK ? control(owner, cast, target, false, now) : 0;
                    sendHit(owner, cast, target, duration, true);
                }
                cast.hitDone = true;
            }
        }
        long elapsed = now - cast.releasedAt;
        if (cast.id == Skill.HEARTS_CUBE && !cast.hitDone) {
            int nextX = HeartsSkillRules.travelX(cast.originX, cast.targetX, elapsed, HeartsSkillRules.actionMs(cast.id));
            int nextY = HeartsSkillRules.travelX(cast.originY, cast.targetY, elapsed, HeartsSkillRules.actionMs(cast.id));
            Object first = null;
            double nearest = Double.POSITIVE_INFINITY;
            for (Object target : targets(cast)) {
                // A focused shot belongs to the selected target; a mob crossing the line cannot steal it.
                if (cast.lockedTarget != null && target != cast.lockedTarget) continue;
                if (!validTarget(owner, cast, target) || cast.hit.contains(target)) continue;
                double contact = HeartsSkillRules.sweptContact(x(target), y(target),
                        cast.previousX, cast.previousY, nextX, nextY, 30);
                if (contact < nearest) { nearest = contact; first = target; }
            }
            if (first != null) {
                cast.hitDone = true;
                cast.hit.add(first);
                damage(owner, cast, first, first instanceof Mob ? cast.mobDamage : cast.attackDamage);
                sendHit(owner, cast, first, 0, true);
            }
            cast.previousX = nextX;
            cast.previousY = nextY;
        } else if ((cast.id == Skill.HEARTS_ULTIMATE || cast.id == Skill.HEARTS_CRYSTAL_BARRAGE
                || cast.id == Skill.HEARTS_CUBE_PRISON)
                && !cast.hitDone && elapsed >= HeartsSkillRules.actionMs(cast.id)) {
            cast.hitDone = true;
            for (Object target : targets(cast)) if (validTarget(owner, cast, target) && inArea(cast, target)) {
                damage(owner, cast, target, target instanceof Mob ? cast.mobDamage : cast.attackDamage);
                sendHit(owner, cast, target, 0, true);
            }
        }
        tickGravity(owner, cast, now);
        if (elapsed >= HeartsSkillRules.actionMs(cast.id)) {
            if (!cast.controls.isEmpty() || !cast.gravityTargets.isEmpty()) {
                List<Cast> effects = lingering.computeIfAbsent(owner, ignored -> new ArrayList<>());
                synchronized (effects) { effects.add(cast); }
            } else cast.completed = true;
            owner.newSkill.heartsTechnique = null;
            send(owner, cast, 32);
            owner.newSkill.closeSkillSpecial();
        }
    }

    /** Resolves focus near the exact aim coordinate; coordinates alone never authorize damage. */
    private Object aimedTarget(Player owner, Cast cast) {
        Object nearest = null;
        long best = 56L * 56L;
        for (Object target : targets(cast)) {
            if (!validTarget(owner, cast, target)) continue;
            long dx = (long) x(target) - cast.targetX, dy = (long) y(target) - cast.targetY;
            long distance = dx * dx + dy * dy;
            if (distance <= best) { best = distance; nearest = target; }
        }
        return nearest;
    }

    private Object nearestMeleeTarget(Player owner, Cast cast) {
        Object nearest = null;
        long best = Long.MAX_VALUE;
        for (Object target : targets(cast)) {
            if (!validTarget(owner, cast, target) || !HeartsSkillRules.inCircle(x(target), y(target),
                    cast.originX, cast.originY, 100)) continue;
            if (cast.id == Skill.HEARTS_PUNCH && Math.abs((long) y(target) - cast.originY) > 24) continue;
            if ((x(target) - cast.originX) * cast.direction < -20) continue;
            long dx = (long) x(target) - cast.targetX, dy = (long) y(target) - cast.targetY;
            long distance = dx * dx + dy * dy;
            if (distance < best) { best = distance; nearest = target; }
        }
        return nearest;
    }

    private boolean inArea(Cast cast, Object target) {
        return HeartsSkillRules.inCircle(x(target), y(target), cast.targetX, cast.targetY,
                HeartsSkillRules.radius(cast.id, cast.level));
    }

    private void tickGravity(Player owner, Cast cast, long now) {
        if (cast.id != Skill.HEARTS_GRAVITY || !cast.released) return;
        while (cast.gravityTicks < HeartsSkillRules.gravityTicks(cast.level)
                && now - cast.releasedAt >= (cast.gravityTicks + 1) * 1000L) {
            for (Iterator<Object> it = cast.gravityTargets.iterator(); it.hasNext();) {
                Object target = it.next();
                if (!validTarget(owner, cast, target)) { it.remove(); continue; }
                long hp = target instanceof Mob ? ((Mob) target).point.maxHp : ((Player) target).nPoint.hpMax;
                damage(owner, cast, target, HeartsSkillRules.gravityDamage(hp));
                sendHit(owner, cast, target, 0, true);
            }
            cast.gravityTicks++;
        }
        if (cast.gravityTicks == HeartsSkillRules.gravityTicks(cast.level)) cast.gravityTargets.clear();
    }

    /** Player's normal update continues released effects without a long action lock. */
    public void updateEffects(Player owner) {
        List<Cast> effects = lingering.get(owner);
        if (effects == null) return;
        synchronized (effects) {
            long now = clock.getAsLong();
            for (Iterator<Cast> it = effects.iterator(); it.hasNext();) {
                Cast cast = it.next();
                if (!validOwner(owner, cast)) { cancel(owner, cast); it.remove(); continue; }
                tickGravity(owner, cast, now);
                cleanupControls(owner, cast, now, false);
                if (cast.controls.isEmpty() && cast.gravityTargets.isEmpty()) {
                    cast.completed = true;
                    it.remove();
                }
            }
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
                && owner.getSession() != null && owner.getSession().isConnected() && cast.zone.getPlayers().contains(owner);
    }

    private boolean validTarget(Player owner, Cast cast, Object target) {
        if (target instanceof Mob) {
            Mob mob = (Mob) target;
            return mob.zone == cast.zone && !mob.isDie() && cast.zone.mobs.contains(mob);
        }
        if (!(target instanceof Player)) return false;
        Player player = (Player) target;
        if (player == owner || player.isPet || player.isNewPet || player.isDie() || player.zone != cast.zone
                || !cast.zone.getHumanoids().contains(player) || !SkillService.gI().canAttackPlayer(owner, player)
                || !Util.canDoWithTime(player.lastTimeRevived, 1500)) return false;
        return owner.clan == null || player.clan != owner.clan
                || (!MapService.gI().isMapBlackBallWar(cast.zone.map.mapId) && owner.idNRNM == -1 && player.idNRNM == -1);
    }

    private List<Object> targets(Cast cast) {
        List<Object> all = new ArrayList<>(cast.zone.mobs);
        all.addAll(new ArrayList<>(cast.zone.getHumanoids()));
        return all;
    }
    private static int x(Object target) { return target instanceof Mob ? ((Mob) target).location.x : ((Player) target).location.x; }
    private static int y(Object target) { return target instanceof Mob ? ((Mob) target).location.y : ((Player) target).location.y; }

    private void damage(Player owner, Cast cast, Object target, long amount) {
        if (!validOwner(owner, cast) || !validTarget(owner, cast, target)) return;
        DamageContext previous = DAMAGE_CONTEXT.get();
        boolean previousCrit = owner.nPoint.isCrit;
        DAMAGE_CONTEXT.set(new DamageContext(owner, cast.skill));
        try {
            owner.nPoint.isCrit = cast.critical;
            amount = Math.max(1, Math.min(Integer.MAX_VALUE, amount));
            if (target instanceof Mob) ((Mob) target).injured(owner, amount, true);
            else {
                Player player = (Player) target;
                player.injured(owner, amount, cast.id == Skill.HEARTS_GRAVITY, false);
                Service.gI().reload_HP_NV(player);
            }
        } finally {
            owner.nPoint.isCrit = previousCrit;
            if (previous == null) DAMAGE_CONTEXT.remove(); else DAMAGE_CONTEXT.set(previous);
        }
    }

    private int control(Player owner, Cast cast, Object target, boolean root, long now) {
        if (!validTarget(owner, cast, target)) return 0;
        int duration = root ? HeartsSkillRules.durationMs(cast.id, cast.level) : HeartsSkillRules.kickStunMs(cast.level);
        if (target instanceof Mob) {
            Mob mob = (Mob) target;
            if (mob.effectSkill == null || mob.effectSkill.isHaveEffectSkill()) return 0;
            if (root) mob.effectSkill.setTroi(now, duration);
            else {
                mob.effectSkill.startStun(now, duration);
                EffectSkillService.gI().sendEffectMob(owner, mob, EffectSkillService.TURN_ON_EFFECT, EffectSkillService.BLIND_EFFECT);
            }
        } else {
            Player player = (Player) target;
            if (player.effectSkill == null || player.effectSkill.isHaveEffectSkill()) return 0;
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
            // Timestamp ownership prevents one caster clearing a newer control from another.
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
                Player player = (Player) control.target;
                if (player.effectSkill != null) {
                    if (control.root && player.effectSkill.anTroi && player.effectSkill.lastTimeTroi == control.started)
                        EffectSkillService.gI().removeAnTroi(player);
                    else if (!control.root && player.effectSkill.isStun && player.effectSkill.lastTimeStartStun == control.started)
                        EffectSkillService.gI().removeStun(player);
                }
            }
            it.remove();
        }
    }

    public void cancel(Player owner, Cast cast) {
        if (cast == null) return;
        if (owner != null && !cast.completed) send(owner, cast, 30);
        cleanupControls(owner, cast, clock.getAsLong(), true);
        cast.gravityTargets.clear();
        cast.hit.clear();
        cast.completed = true;
    }

    private void dashOnGround(Player owner, Cast cast) {
        nro.models.map.Map map = cast.zone.map;
        if (map.tileMap == null || map.types == null || map.tmw <= 0) return;
        int wanted = Math.min(HeartsSkillRules.range(cast.id), Math.max(0, Math.abs(cast.targetX - cast.originX) - 48));
        int lastX = cast.originX, lastY = cast.originY;
        if (Math.abs(map.yPhysicInTop(lastX, lastY - 24) - lastY) > 24) return;
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
        } catch (Exception ex) { Logger.logException(HeartsSkillService.class, ex); }
        finally { if (message != null) message.cleanup(); }
    }

    private void send(Player owner, Cast cast, int status) {
        Message message = null;
        try {
            message = new Message(-45);
            message.writer().writeByte(status);
            message.writer().writeInt((int) owner.id);
            message.writer().writeShort(cast.id);
            if (status == 20) {
                message.writer().writeByte(cast.id == Skill.HEARTS_PUNCH ? 4 : cast.id == Skill.HEARTS_KICK ? 3 : 1);
                message.writer().writeByte(cast.direction);
                message.writer().writeShort(HeartsSkillRules.chargeMs(cast.id));
                message.writer().writeByte(0);
                message.writer().writeByte(0);
                message.writer().writeByte(0);
            } else if (status == 21) {
                message.writer().writeShort(cast.targetX);
                message.writer().writeShort(cast.targetY);
                message.writer().writeShort(HeartsSkillRules.durationMs(cast.id, cast.level));
                message.writer().writeShort(HeartsSkillRules.radius(cast.id, cast.level));
                message.writer().writeByte(0);
                message.writer().writeByte(0); // Hits are confirmed individually by status 31.
                message.writer().writeByte(0);
            }
            Service.gI().sendMessAllPlayerInMap(cast.zone, message);
        } catch (Exception ex) { Logger.logException(HeartsSkillService.class, ex); }
        finally { if (message != null) message.cleanup(); }
    }
}
