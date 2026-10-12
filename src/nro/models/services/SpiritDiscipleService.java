package nro.models.services;

import nro.models.consts.ConstPlayer;
import nro.models.player.Pet;
import nro.models.player.Player;
import nro.models.player.NewPet;
import nro.models.skill.Skill;
import nro.models.utils.SkillUtil;
import nro.models.utils.Util;

/** Changes the existing Pet in place: identity, equipment, stats and learned skills survive. */
public final class SpiritDiscipleService {
    private SpiritDiscipleService() { }

    public static int ownerForm(Player player) {
        return player == null || player.isPet || player.isNewPet || player.isBoss || player.pet == null
                ? 0 : SpiritDiscipleRules.sanitize(player.pet.companionForm);
    }

    public static boolean hasForm(Pet pet) {
        return pet != null && SpiritDiscipleRules.active(pet.companionForm);
    }

    /** Rolls every unlocked slot once; changing forms never rerolls a known skill. */
    public static void applyForm(Pet pet, int form) {
        if (pet == null || SpiritDiscipleRules.sanitize(form) != form)
            throw new IllegalArgumentException("Invalid disciple form: " + form);
        if (SpiritDiscipleRules.active(form) && pet.playerSkill != null) {
            while (pet.playerSkill.skills.size() < 5) pet.playerSkill.skills.add(SkillUtil.createEmptySkill());
            if (isUnlearned(pet, 0))
                pet.playerSkill.skills.set(0, SkillUtil.createSkill(Util.nextInt(0, 2) * 2, 1));
            long power = pet.nPoint == null ? 0 : pet.nPoint.power;
            if (power >= 150_000_000L && isUnlearned(pet, 1)) pet.openSkill2();
            if (power >= 1_500_000_000L && isUnlearned(pet, 2)) pet.openSkill3();
            if (power >= 20_000_000_000L && isUnlearned(pet, 3)) pet.openSkill4();
        }
        pet.companionForm = (byte) form;
        syncFollower(pet);
    }

    /** Keeps the normal disciple and the spirit visible as two separate map entities. */
    public static void syncFollower(Pet pet) {
        if (pet == null || pet.master == null) return;
        Player owner = pet.master;
        int form = SpiritDiscipleRules.sanitize(pet.companionForm);
        // Vòng Chibi ẩn Tinh Linh; chặn cả luồng tự tạo lại ở mỗi tick.
        if (!SpiritDiscipleRules.active(form) || owner.chibiRingActive) {
            if (owner.spiritPet != null) {
                owner.spiritPet.dispose();
                owner.spiritPet = null;
            }
            return;
        }
        short head = SpiritDiscipleRules.head(form);
        if (owner.spiritPet != null && owner.spiritPet.getHead() == head) return;
        if (owner.spiritPet != null) owner.spiritPet.dispose();
        NewPet follower = new NewPet(owner, head, (short) -1, (short) -1);
        follower.name = "$S$" + SpiritDiscipleRules.name(form);
        follower.gender = owner.gender;
        follower.nPoint.hpg = follower.nPoint.mpg = 1;
        follower.nPoint.dameg = follower.nPoint.defg = 1;
        follower.nPoint.critg = 1;
        follower.nPoint.stamina = follower.nPoint.maxStamina = 1000;
        follower.nPoint.setBasePoint();
        follower.nPoint.setFullHpMp();
        owner.spiritPet = follower;
        if (owner.zone != null) follower.joinMapMaster();
    }

    private static boolean isUnlearned(Pet pet, int slot) {
        if (pet.playerSkill.skills.size() <= slot) return true;
        Skill skill = pet.playerSkill.skills.get(slot);
        return skill == null || skill.skillId == -1 || skill.point < 1;
    }

    /** Trao dạng Tinh Linh gió cố định từ boss; Uron không gọi luồng này. */
    public static void grantBossWind(Player owner) {
        if (owner == null || owner.isPet || owner.isNewPet || owner.isBoss) return;
        synchronized (owner) {
            if (owner.pet != null && owner.pet.companionForm == SpiritDiscipleRules.WIND) return;
            if (owner.pet != null && owner.fusion != null
                    && owner.fusion.typeFusion != ConstPlayer.NON_FUSION) {
                owner.pet.unFusion();
            }
            boolean created = owner.pet == null;
            if (created) PetService.gI().createCompanionPet(owner);
            applyForm(owner.pet, SpiritDiscipleRules.WIND);
            Service.gI().sendHavePet(owner);
            if (created && owner.zone != null) owner.pet.joinMapMaster();
            Service.gI().Send_Caitrang(owner.pet);
            Service.gI().point(owner);
            Service.gI().showInfoPet(owner);
        }
    }

    public static void choose(Player owner, int form) {
        if (owner == null || owner.isPet || owner.isNewPet || owner.isBoss
                || SpiritDiscipleRules.sanitize(form) != form) return;
        synchronized (owner) {
            SpiritBossUnlockService.rememberExistingForm(owner);
            if (form == SpiritDiscipleRules.WIND) {
                Service.gI().sendThongBao(owner,
                        "Tinh Linh gió chỉ nhận được khi hạ boss Tinh Linh.");
                return;
            }
            if (owner.pet != null && owner.pet.companionForm == SpiritDiscipleRules.WIND) {
                Service.gI().sendThongBao(owner,
                        "Đệ tử Tinh Linh gió không thể đổi hình dạng.");
                return;
            }
            if (owner.fusion != null && owner.fusion.typeFusion != ConstPlayer.NON_FUSION) {
                Service.gI().sendThongBao(owner, "Hãy tách hợp thể trước khi đổi hình dạng đệ tử.");
                return;
            }
            if (owner.pet == null) {
                Service.gI().sendThongBao(owner, "Đệ tử thường chỉ nhận khi hạ Super Broly.");
                return;
            }
            if (owner.pet != null && owner.pet.newSkill != null && owner.pet.newSkill.isStartSkillSpecial) {
                Service.gI().sendThongBao(owner, "Đợi đệ tử thi triển xong rồi đổi hình dạng.");
                return;
            }
            applyForm(owner.pet, form);
            // The client enables disciple/equipment controls only after -107 subtype 1.
            Service.gI().sendHavePet(owner);
            Service.gI().Send_Caitrang(owner.pet);
            Service.gI().point(owner);
            Service.gI().showInfoPet(owner);
            Service.gI().sendThongBao(owner, "Đã chọn " + SpiritDiscipleRules.name(form)
                    + ". " + SpiritDiscipleRules.description(form) + ".");
        }
    }
}
