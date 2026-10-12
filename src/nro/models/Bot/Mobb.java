package nro.models.Bot;

import java.util.ArrayList;
import java.util.List;
import nro.models.map.Zone;
import nro.models.mob.Mob;
import nro.models.services.PlayerService;
import nro.models.services.SkillService;
import nro.models.utils.Util;

public class Mobb {

    private static final long CHANGE_ZONE_INTERVAL = 300000L;
    private Mob mAttack;
    public Bot bot;

    public Mobb(Bot b) {
        this.bot = b;
    }

    public void update() {
        this.changeZoneIfNeeded();
        this.ensureAssignedZone();
        this.attack();
    }

    private void getMobAttack() {
        if (this.bot.zone == null || this.bot.zone.mobs.isEmpty()) {
            this.mAttack = null;
            return;
        }
        if (this.mAttack != null && !this.mAttack.isDie() && this.mAttack.zone == this.bot.zone) {
            return;
        }
        List<Mob> aliveMobs = new ArrayList<>();
        for (Mob mob : this.bot.zone.mobs) {
            if (mob != null && !mob.isDie()) {
                aliveMobs.add(mob);
            }
        }
        this.mAttack = aliveMobs.isEmpty() ? null : aliveMobs.get(Util.nextInt(0, aliveMobs.size() - 1));
    }

    private void attack() {
        this.getMobAttack();
        if (this.bot.playerSkill.skills.size() < 2) {
            return;
        }
        this.bot.playerSkill.skillSelect = Util.isTrue(50, 100)
                ? this.bot.playerSkill.skills.get(0)
                : this.bot.playerSkill.skills.get(1);
        if (this.mAttack != null && this.bot.UseLastTimeSkill()) {
            PlayerService.gI().playerMove(this.bot, this.mAttack.location.x, this.mAttack.location.y);
            SkillService.gI().useSkill(this.bot, null, this.mAttack, -1, null);
        }
    }

    private void ensureAssignedZone() {
        if (this.bot.zone != null
                && this.bot.zone.map != null
                && this.bot.zone.map.mapId == this.bot.getAssignedMapId()
                && this.bot.zone.zoneId == this.bot.getAssignedZoneId()) {
            return;
        }
        Zone assignedZone = NewBot.gI().getAssignedZone(this.bot.getAssignedMapId(), this.bot.getAssignedZoneId());
        if (assignedZone != null) {
            this.bot.joinAssignedZone(assignedZone);
        }
    }

    private void changeZoneIfNeeded() {
        if (System.currentTimeMillis() - this.bot.getLastTimeChangeZone() < CHANGE_ZONE_INTERVAL) {
            return;
        }
        Zone newZone = NewBot.gI().getRandomEligibleZone(this.bot.getAssignedMapId(), this.bot.getAssignedZoneId());
        this.bot.setLastTimeChangeZone(System.currentTimeMillis());
        if (newZone == null) {
            return;
        }
        this.bot.setAssignedZoneId(newZone.zoneId);
        this.mAttack = null;
    }
}
