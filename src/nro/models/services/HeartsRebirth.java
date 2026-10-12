package nro.models.services;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import nro.models.consts.ConstPlayer;
import nro.models.network.Message;
import nro.models.player.Player;
import nro.models.player_system.Template.SkillTemplate;
import nro.models.skill.NClass;
import nro.models.skill.Skill;
import nro.models.utils.Logger;
import nro.models.utils.SkillUtil;

/** Dedicated Hearts templates; old planet templates and skill IDs are never overwritten. */
public final class HeartsRebirth {
    private HeartsRebirth() { }
    public static final int FIRST_LEVEL_ID = 23000;
    public static final int FIRST_ICON_ID = 30617;
    private static final int[] BASE_TEMPLATES = {Skill.GALICK, Skill.KAMEJOKO,
        Skill.THAI_DUONG_HA_SAN, Skill.DICH_CHUYEN_TUC_THOI, Skill.QUA_CAU_KENH_KHI, Skill.SUPER_KAME, Skill.KAMEJOKO};
    private static final int[] INHERIT_TEMPLATES = {Skill.DRAGON, Skill.KAMEJOKO,
        Skill.THAI_DUONG_HA_SAN, Skill.DICH_CHUYEN_TUC_THOI, Skill.QUA_CAU_KENH_KHI, Skill.SUPER_KAME, Skill.KAMEJOKO};
    private static final int[] DAMAGE_PERCENT = {150, 110, 100, 100, 120, 110, 120};
    private static final int[] COOLDOWN_PERCENT = {130, 110, 100, 100, 120, 115, 120};
    private static final String[] NAMES = {"Đấm Hearts", "Chưởng Lập Phương", "Trọng Lực Lập Phương",
        "Tốc Biến Cước", "Hủy Diệt Lập Phương", "Bão Tinh Thể", "Ngục Lập Phương"};
    private static final String[] DESCRIPTIONS = {
        "Sát thương hơn Galick cùng cấp 50%; đấm và hồi chiêu lâu hơn 30%",
        "Bắn khối lập phương; sát thương hơn Kame cùng cấp 10%, thi triển và hồi chiêu lâu hơn 10%",
        "Khối lập phương trên trời giữ địch ngay lập tức; mất 3% HP tối đa mỗi giây, thời gian bằng Thái Dương cùng cấp",
        "Dịch chuyển nhanh tới địch rồi đá hất lên; sát thương chí mạng và thời gian choáng bằng Dịch Chuyển Tức Thời cùng cấp",
        "Tụ các khối lập phương rồi ném xuống vùng mục tiêu; mạnh hơn Quả Cầu Kênh Khí 20%, hồi chiêu lâu hơn 20%",
        "Gồng aura 60 trong 3 giây, phóng 6 hạt năng lượng rồi phát nổ; mạnh hơn Super Kame 10%, hồi chiêu lâu hơn 15%",
        "Các khối lập phương trồi lên quanh mục tiêu, khóa di chuyển rồi đồng loạt phát nổ"
    };

    public static boolean handles(int id) { return id >= Skill.HEARTS_PUNCH && id <= Skill.HEARTS_CUBE_PRISON; }

    public static boolean isReborn(Player player) {
        return player != null && player.gender == ConstPlayer.TRAI_DAT && !player.isBoss
                && !player.isPet && !player.isNewPet && (player.heartsReborn || player.head == 3310 || player.head == 2139);
    }

    /** Called only after Manager has loaded the original planet skills. */
    public static void installTemplates(List<NClass> classes) {
        Set<Integer> levelIds = new HashSet<>();
        SkillTemplate[] sources = new SkillTemplate[7];
        for (NClass nclass : classes) {
            for (SkillTemplate template : nclass.skillTemplatess) {
                if (handles(template.id)) {
                    throw new IllegalStateException("Hearts template ID already occupied: " + template.id);
                }
                for (Skill skill : template.skillss) levelIds.add((int) skill.skillId);
                for (int i = 0; i < sources.length; i++)
                    if (template.id == BASE_TEMPLATES[i]) sources[i] = template;
            }
        }
        // Validate the whole reservation before adding any template.
        for (int id = FIRST_LEVEL_ID; id < FIRST_LEVEL_ID + 49; id++) {
            if (levelIds.contains(id)) throw new IllegalStateException("Hearts skill level ID occupied: " + id);
        }
        for (int i = 0; i < sources.length; i++) {
            int requiredLevels = i == 5 ? 1 : 7;
            if (sources[i] == null || sources[i].skillss.size() < requiredLevels)
                throw new IllegalStateException("Missing " + requiredLevels + " base levels for Hearts: " + BASE_TEMPLATES[i]);
        }
        for (int i = 0; i < sources.length; i++) {
            SkillTemplate source = sources[i];
            SkillTemplate template = new SkillTemplate();
            template.id = (byte) (Skill.HEARTS_PUNCH + i);
            template.classId = ConstPlayer.TRAI_DAT;
            template.name = NAMES[i];
            template.maxPoint = 7;
            template.type = 4;
            template.manaUseType = source.manaUseType;
            template.iconId = (short) (i == 6 ? FIRST_ICON_ID + 1 : FIRST_ICON_ID + i);
            template.damInfo = DESCRIPTIONS[i];
            for (int rank = 0; rank < 7; rank++) {
                // Super Kame is an ultimate with one catalogue level on this server.
                // Reuse that authoritative level for all seven upgrade ranks instead of refusing startup.
                Skill skill = new Skill(source.skillss.get(Math.min(rank, source.skillss.size() - 1)));
                skill.template = template;
                skill.point = rank + 1;
                skill.skillId = (short) (FIRST_LEVEL_ID + i * 7 + rank);
                skill.coolDown = scale(skill.coolDown, COOLDOWN_PERCENT[i]);
                skill.damage = (short) Math.min(Short.MAX_VALUE, scale(skill.damage, DAMAGE_PERCENT[i]));
                // TDHS/DCTT store milliseconds in damage; Hearts uses explicit server rules.
                if (i == 2) skill.damage = 3;
                if (i == 3) skill.damage = 200;
                skill.dx = HeartsSkillRules.range(template.id);
                skill.dy = HeartsSkillRules.radius(template.id, rank + 1);
                skill.maxFight = i >= 2 && i != 3 ? 20 : 1;
                skill.moreInfo = NAMES[i] + " cấp " + skill.point + ": " + DESCRIPTIONS[i]
                        + "; hồi " + (skill.coolDown / 1000.0) + " giây";
                if (i == 2) skill.moreInfo += "; giữ " + (rank + 3) + " giây";
                if (i == 3) skill.moreInfo += "; choáng " + ((rank + 2) * 0.5) + " giây";
                template.skillss.add(skill);
            }
            classes.get(ConstPlayer.TRAI_DAT).skillTemplatess.add(template);
        }
    }

    static int scale(int value, int percent) {
        return (int) Math.min(Integer.MAX_VALUE, ((long) value * percent + 50L) / 100L);
    }

    /** Đồng bộ bộ kỹ năng Heart; chỉ chiêu đấm được mở sẵn, các chiêu còn lại học tại Urôn. */
    public static void ensureSkills(Player player, boolean notifyClient) {
        if (!isReborn(player) || player.playerSkill == null) return;
        player.heartsReborn = true;
        for (int i = 0; i < 7; i++) {
            int id = Skill.HEARTS_PUNCH + i;
            Skill owned = player.playerSkill.getSkillbyId(id);
            if (owned == null) {
                owned = (i == 0) ? SkillUtil.createSkill(id, 1) : SkillUtil.createSkillLevel0(id);
                if (owned == null) throw new IllegalStateException("Hearts catalogue not loaded: " + id);
                player.playerSkill.skills.add(owned);
            } else if (i == 0 && (owned.skillId == -1 || owned.point < 1)) {
                player.playerSkill.skills.remove(owned);
                owned = SkillUtil.createSkill(id, 1);
                player.playerSkill.skills.add(owned);
            }
            if (notifyClient && owned.point > 0) sendLearned(player, owned);
        }
        boolean hasHeartShortcut = false;
        for (byte id : player.playerSkill.skillShortCut)
            if (handles(id)) { hasHeartShortcut = true; break; }
        if (!hasHeartShortcut && player.playerSkill.skillShortCut.length > 0)
            player.playerSkill.skillShortCut[0] = Skill.HEARTS_PUNCH;
        if (player.playerSkill.skillSelect == null || !handles(player.playerSkill.skillSelect.template.id))
            player.playerSkill.skillSelect = player.playerSkill.getSkillbyId(Skill.HEARTS_PUNCH);
        if (notifyClient) player.playerSkill.sendSkillShortCut();
    }
    private static void sendLearned(Player player, Skill skill) {
        Message msg = null;
        try {
            msg = Service.gI().messageSubCommand((byte) 23);
            msg.writer().writeShort(skill.skillId);
            player.sendMessage(msg);
        } catch (Exception e) {
            Logger.logException(HeartsRebirth.class, e);
        } finally {
            if (msg != null) msg.cleanup();
        }
    }
}
