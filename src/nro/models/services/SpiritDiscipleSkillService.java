package nro.models.services;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import nro.models.mob.Mob;
import nro.models.network.Message;
import nro.models.player.Pet;
import nro.models.player.Player;
import nro.models.skill.Skill;
import nro.models.utils.SkillUtil;
import nro.models.utils.Util;

/** Server-authoritative PvE abilities for the three spirit disciple forms. */
public final class SpiritDiscipleSkillService {
    public static final short WIND_TORNADO = 3201, ICE_RAIN = 3202, DEMON_LAVA = 3203;
    public static final short WIND_CARNIVOROUS_FLOWER = 3204, WITCH_CLONES = 3205, DEMON_FORM = 3206;
    private static final long COOLDOWN = 12_000L;
    private static final long WIND_DEBUFF_DURATION = 30_000L;
    private static final long CLONE_DURATION = 30_000L, CLONE_ATTACK_INTERVAL = 1_500L;
    private static final long FLOWER_DURATION = 30_000L, FLOWER_ATTACK_INTERVAL = 1_500L;
    private static final int DEMON_DAMAGE_BONUS = 40, DEMON_CRIT_BONUS = 35;
    private static final SpiritDiscipleSkillService INSTANCE = new SpiritDiscipleSkillService();
    private final Map<Pet, State> states = new WeakHashMap<>();
    private final Map<Mob, Debuff> debuffs = new WeakHashMap<>();

    private static final class Burn {
        final Mob mob;
        final long started;
        final long damage;
        int ticks;
        Burn(Mob mob, long started, long damage) { this.mob = mob; this.started = started; this.damage = damage; }
    }
    private static final class Debuff {
        long windUntil;
        int armorReduction;
        int damageReduction;
    }
    private static final class State {
        long lastCast;
        long lastSkillThree;
        long lastSkillFour;
        long lastDemonForm;
        long demonFormUntil;
        long cloneUntil;
        long nextCloneAttack;
        long flowerUntil;
        long nextFlowerAttack;
        boolean castSkillFour;
        final List<Burn> burns = new ArrayList<>();
    }
    private SpiritDiscipleSkillService() { }
    public static SpiritDiscipleSkillService gI() { return INSTANCE; }

    /** Returns true only on the update that starts a special cast. */
    public boolean update(Pet pet) {
        if (pet == null) return false;
        State state;
        synchronized (states) { state = states.computeIfAbsent(pet, ignored -> new State()); }
        long now = System.currentTimeMillis();
        tickBurns(pet, state, now);
        tickClones(pet, state, now);
        tickFlower(pet, state, now);
        if (!SpiritDiscipleService.hasForm(pet) || pet.zone == null || pet.isDie()
                || pet.master == null || pet.master.zone != pet.zone
                || pet.master.spiritPet == null || pet.master.spiritPet.zone != pet.zone
                || (pet.master.spiritPet.getSpiritStatus() != Pet.ATTACK
                    && pet.master.spiritPet.getSpiritStatus() != Pet.PROTECT)
                || now - state.lastCast < COOLDOWN) return false;
        Mob focus = nearestMob(pet, pet.master.spiritPet.getSpiritStatus() == Pet.PROTECT ? 170 : 260);
        if (focus == null) return false;
        int form = SpiritDiscipleRules.sanitize(pet.companionForm);
        if (!skillUnlocked(pet, 3)) return false;
        boolean requestedSkillFour = state.castSkillFour && skillUnlocked(pet, 4);
        boolean skillFour = requestedSkillFour && skillFourReady(pet, state, form, now);
        // Offensive effects always lock to the selected monster.
        int cx = focus.location.x;
        int cy = focus.location.y;
        int radius = form == SpiritDiscipleRules.WIND ? 145 : form == SpiritDiscipleRules.WITCH ? 170 : 185;
        List<Mob> targets = targets(pet, cx, cy, radius);
        if (targets.isEmpty()) return false;
        if (!skillFour && !skillThreeReady(pet, state, now)) return false;
        state.lastCast = now;
        // While Hóa Quỷ is cooling down, keep using lava and retry the transformation later.
        state.castSkillFour = requestedSkillFour && !skillFour ? true : !state.castSkillFour;
        long base = Math.max(1, pet.nPoint.dame);
        if (skillFour) {
            castSkillFour(pet, state, form, now, cx, cy, radius, targets, base);
            state.lastSkillFour = now;
            return true;
        }
        state.lastSkillThree = now;
        int skillLevel = skillThreeLevel(pet);
        for (Mob mob : targets) {
            if (form == SpiritDiscipleRules.WIND) {
                int percent = 10 + (skillLevel - 1) * 2;
                damage(pet, mob, base * (100L + percent) / 100L);
                if (!mob.isDie() && mob.effectSkill != null && !mob.effectSkill.isHaveEffectSkill()) {
                    int stun = SkillUtil.getTimeStun(skillLevel) * 80 / 100;
                    mob.effectSkill.startStun(now, mob.isBigBoss() ? Math.max(500, stun / 3) : stun);
                    EffectSkillService.gI().sendEffectMob(pet, mob, EffectSkillService.TURN_ON_EFFECT,
                            EffectSkillService.BLIND_EFFECT);
                }
                applyWindDebuff(mob, now, percent);
            } else if (form == SpiritDiscipleRules.WITCH) {
                damage(pet, mob, base * kameDamagePercent(skillLevel) / 100L);
                if (!mob.isDie() && mob.effectSkill != null && !mob.effectSkill.isHaveEffectSkill())
                    mob.effectSkill.setTroi(now, mob.isBigBoss()
                            ? Math.max(800, freezeDuration(skillLevel) / 4) : freezeDuration(skillLevel));
            } else {
                damage(pet, mob, base * 80 / 100);
                if (!mob.isDie())
                    state.burns.add(new Burn(mob, now,
                            Math.max(1, pet.nPoint.hpMax * (long) skillLevel / 100L)));
            }
        }
        send(pet, form, cx, cy, radius, targets, false);
        return true;
    }

    private void castSkillFour(Pet pet, State state, int form, long now, int x, int y,
            int radius, List<Mob> targets, long base) {
        if (form == SpiritDiscipleRules.WIND) {
            state.flowerUntil = now + FLOWER_DURATION;
            state.nextFlowerAttack = now + FLOWER_ATTACK_INTERVAL;
            for (Mob mob : targets) {
                damage(pet, mob, SkillUtil.getHPMobMe(base, skillFourLevel(pet)));
                if (!mob.isDie() && mob.effectSkill != null && !mob.effectSkill.isHaveEffectSkill())
                    mob.effectSkill.setTroi(now, mob.isBigBoss() ? 500 : 1400);
            }
        } else if (form == SpiritDiscipleRules.WITCH) {
            state.cloneUntil = now + CLONE_DURATION;
            state.nextCloneAttack = now + CLONE_ATTACK_INTERVAL;
            // Three 70%-stat clones make their first strike immediately.
            for (Mob mob : targets)
                for (int hit = 0; hit < 3 && valid(pet, mob); hit++) damage(pet, mob, base * 70 / 100);
        } else {
            state.demonFormUntil = now + demonFormDuration(pet);
            state.lastDemonForm = now;
            // Transformation is attached to the disciple, not the monster.
            x = pet.location.x;
            y = pet.location.y;
        }
        send(pet, form, x, y, radius, targets, true);
    }

    public int demonDamageBonus(Pet pet) {
        State state;
        synchronized (states) { state = states.get(pet); }
        return state != null && System.currentTimeMillis() < state.demonFormUntil ? DEMON_DAMAGE_BONUS : 0;
    }

    public int demonCritBonus(Pet pet) {
        State state;
        synchronized (states) { state = states.get(pet); }
        return state != null && System.currentTimeMillis() < state.demonFormUntil ? DEMON_CRIT_BONUS : 0;
    }

    public long amplifyDamage(Mob mob, long damage) {
        Debuff debuff;
        synchronized (debuffs) { debuff = debuffs.get(mob); }
        return debuff != null && System.currentTimeMillis() < debuff.windUntil
                ? Math.min(Integer.MAX_VALUE, damage + damage * debuff.armorReduction / 100L) : damage;
    }

    public int reduceMobDamage(Mob mob, int damage) {
        Debuff debuff;
        synchronized (debuffs) { debuff = debuffs.get(mob); }
        return debuff != null && System.currentTimeMillis() < debuff.windUntil
                ? Math.max(1, damage - damage * debuff.damageReduction / 100) : damage;
    }

    private void applyWindDebuff(Mob mob, long now, int percent) {
        synchronized (debuffs) {
            Debuff debuff = debuffs.computeIfAbsent(mob, ignored -> new Debuff());
            debuff.windUntil = Math.max(debuff.windUntil, now + WIND_DEBUFF_DURATION);
            debuff.armorReduction = Math.max(debuff.armorReduction, percent);
            debuff.damageReduction = Math.max(debuff.damageReduction, percent);
        }
    }

    /** Hóa Quỷ follows the disciple's level-1..7 Hóa Khỉ duration: 60..120 seconds. */
    private int demonFormDuration(Pet pet) {
        return SkillUtil.getTimeMonkey(demonFormLevel(pet));
    }

    /** Reuses the real Hóa Khỉ cooldown configured by the server for the same level. */
    private long demonFormCooldown(Pet pet) {
        Skill monkey = SkillUtil.createSkill(Skill.BIEN_KHI, demonFormLevel(pet));
        return monkey == null || monkey.coolDown <= 0 ? 300_000L : monkey.coolDown;
    }

    private boolean demonFormReady(Pet pet, State state, long now) {
        return state.lastDemonForm == 0 || now - state.lastDemonForm >= demonFormCooldown(pet);
    }

    private boolean skillFourReady(Pet pet, State state, int form, long now) {
        if (form == SpiritDiscipleRules.DEMON) return demonFormReady(pet, state, now);
        return state.lastSkillFour == 0 || now - state.lastSkillFour >= skillFourCooldown(pet, form);
    }

    private long skillFourCooldown(Pet pet, int form) {
        int level = skillFourLevel(pet);
        int skillId = form == SpiritDiscipleRules.WIND ? Skill.DE_TRUNG : Skill.KHIEN_NANG_LUONG;
        Skill reference = SkillUtil.createSkill(skillId, level);
        long cooldown = reference == null || reference.coolDown <= 0 ? 60_000L : reference.coolDown;
        // Hoa Ăn Thịt deliberately recovers 25% slower than Đẻ Trứng.
        return form == SpiritDiscipleRules.WIND ? cooldown + cooldown / 4 : cooldown;
    }

    private int skillFourLevel(Pet pet) {
        return demonFormLevel(pet);
    }

    private boolean skillUnlocked(Pet pet, int number) {
        if (pet == null || pet.playerSkill == null || pet.playerSkill.skills == null
                || number < 1 || pet.playerSkill.skills.size() < number) return false;
        Skill skill = pet.playerSkill.skills.get(number - 1);
        return skill != null && skill.template != null && skill.skillId >= 0;
    }

    private int demonFormLevel(Pet pet) {
        int level = 1;
        if (pet != null && pet.playerSkill != null && pet.playerSkill.skills != null
                && pet.playerSkill.skills.size() > 3) {
            Skill skillFour = pet.playerSkill.skills.get(3);
            if (skillFour != null) level = Math.max(1, Math.min(7, skillFour.point));
        }
        return level;
    }

    private int kameDamagePercent(int level) {
        Skill kame = SkillUtil.createSkill(Skill.KAMEJOKO, Math.max(1, Math.min(7, level)));
        return kame == null || kame.damage <= 0 ? 100 + level * 10 : kame.damage;
    }

    /** Ice lasts 4 seconds at level 1 and gains one second each level, up to 10 seconds. */
    private int freezeDuration(int level) {
        return (Math.max(1, Math.min(7, level)) + 3) * 1000;
    }

    /** Skill 3 follows the real Thái Dương Hạ San cooldown at the same level. */
    private long skillThreeCooldown(Pet pet) {
        Skill solarFlare = SkillUtil.createSkill(Skill.THAI_DUONG_HA_SAN, skillThreeLevel(pet));
        return solarFlare == null || solarFlare.coolDown <= 0 ? 60_000L : solarFlare.coolDown;
    }

    private boolean skillThreeReady(Pet pet, State state, long now) {
        return state.lastSkillThree == 0 || now - state.lastSkillThree >= skillThreeCooldown(pet);
    }

    private int skillThreeLevel(Pet pet) {
        int level = 1;
        if (pet != null && pet.playerSkill != null && pet.playerSkill.skills != null
                && pet.playerSkill.skills.size() > 2) {
            Skill skillThree = pet.playerSkill.skills.get(2);
            if (skillThree != null) level = Math.max(1, Math.min(7, skillThree.point));
        }
        return level;
    }

    private void tickBurns(Pet pet, State state, long now) {
        for (Iterator<Burn> it = state.burns.iterator(); it.hasNext();) {
            Burn burn = it.next();
            while (burn.ticks < 5 && now - burn.started >= (burn.ticks + 1) * 1000L) {
                if (!valid(pet, burn.mob)) break;
                damage(pet, burn.mob, burn.damage);
                burn.ticks++;
            }
            if (burn.ticks >= 5 || !valid(pet, burn.mob)) it.remove();
        }
    }

    private void tickClones(Pet pet, State state, long now) {
        if (now >= state.cloneUntil || now < state.nextCloneAttack || pet == null
                || pet.zone == null || pet.isDie()) return;
        state.nextCloneAttack = now + CLONE_ATTACK_INTERVAL;
        long cloneDamage = Math.max(1, pet.nPoint.dame * 70L / 100L);
        // Three independent copies attack nearby monsters for the full 30 seconds.
        for (int clone = 0; clone < 3; clone++) {
            Mob target = nearestMob(pet, 300);
            if (target == null) break;
            damage(pet, target, cloneDamage);
        }
    }

    private void tickFlower(Pet pet, State state, long now) {
        if (now >= state.flowerUntil || now < state.nextFlowerAttack || pet == null
                || pet.zone == null || pet.isDie()) return;
        state.nextFlowerAttack = now + FLOWER_ATTACK_INTERVAL;
        Mob target = nearestMob(pet, 300);
        if (target != null)
            damage(pet, target, SkillUtil.getHPMobMe(Math.max(1, pet.nPoint.dame), skillFourLevel(pet)));
    }

    private Mob nearestMob(Pet pet, int range) {
        Mob best = null; int distance = Integer.MAX_VALUE;
        Player source = pet.master != null && pet.master.spiritPet != null
                ? pet.master.spiritPet : pet;
        for (Mob mob : new ArrayList<>(pet.zone.mobs)) {
            if (!valid(pet, mob)) continue;
            int next = Util.getDistance(source, mob);
            if (next <= range && next < distance) { best = mob; distance = next; }
        }
        return best;
    }
    private List<Mob> targets(Pet pet, int x, int y, int radius) {
        List<Mob> result = new ArrayList<>();
        long squared = (long) radius * radius;
        for (Mob mob : new ArrayList<>(pet.zone.mobs))
            if (valid(pet, mob) && (long) (mob.location.x - x) * (mob.location.x - x)
                    + (long) (mob.location.y - y) * (mob.location.y - y) <= squared) result.add(mob);
        return result;
    }
    private boolean valid(Pet pet, Mob mob) {
        return pet != null && pet.zone != null && mob != null && mob.zone == pet.zone
                && pet.zone.mobs.contains(mob) && !mob.isDie();
    }
    private void damage(Pet pet, Mob mob, long amount) {
        if (valid(pet, mob)) {
            amount = SpiritDiscipleRules.applyDamageSet(pet, amount);
            mob.injured(pet, Math.max(1, amount), true);
        }
    }
    private void send(Pet pet, int form, int x, int y, int radius, List<Mob> targets, boolean skillFour) {
        Message message = null;
        try {
            message = new Message(-45);
            message.writer().writeByte(40);
            message.writer().writeInt((int) pet.master.spiritPet.id);
            message.writer().writeShort(skillFour
                    ? (form == SpiritDiscipleRules.WIND ? WIND_CARNIVOROUS_FLOWER
                            : form == SpiritDiscipleRules.WITCH ? WITCH_CLONES : DEMON_FORM)
                    : (form == SpiritDiscipleRules.WIND ? WIND_TORNADO
                            : form == SpiritDiscipleRules.WITCH ? ICE_RAIN : DEMON_LAVA));
            message.writer().writeShort(x);
            message.writer().writeShort(y);
            message.writer().writeInt(visualDuration(pet, form, skillFour));
            message.writer().writeShort(radius);
            message.writer().writeByte(Math.min(120, targets.size()));
            for (int i = 0; i < targets.size() && i < 120; i++) message.writer().writeByte(targets.get(i).id);
            Service.gI().sendMessAllPlayerInMap(pet.zone, message);
        } catch (Exception ex) {
            nro.models.utils.Logger.logException(SpiritDiscipleSkillService.class, ex);
        } finally { if (message != null) message.cleanup(); }
    }

    private int visualDuration(Pet pet, int form, boolean skillFour) {
        if (!skillFour) return 6_000;
        if (form == SpiritDiscipleRules.WIND) return 30_000;
        if (form == SpiritDiscipleRules.WITCH) return (int) CLONE_DURATION;
        return demonFormDuration(pet);
    }
}
