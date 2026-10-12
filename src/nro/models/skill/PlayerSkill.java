package nro.models.skill;

import java.util.ArrayList;
import java.util.List;
import nro.models.player.Player;
import nro.models.services.Service;
import nro.models.network.Message;

/**
 *
 * @author By Mr Blue
 * 
 */

public class PlayerSkill {

    private Player player;
    public List<Skill> skills;
    public Skill skillSelect;

    public PlayerSkill(Player player) {
        this.player = player;
        skills = new ArrayList<>();
    }

    public Skill getSkillbyId(int id) {
        for (Skill skill : skills) {
            if (skill.template.id == id) {
                return skill;
            }
        }
        return null;
    }

    public byte[] skillShortCut = new byte[10];

    public void sendSkillShortCut() {
        Message msg;
        try {
            msg = Service.gI().messageSubCommand((byte) 61);
            msg.writer().writeUTF("KSkill");
            msg.writer().writeInt(skillShortCut.length);
            msg.writer().write(skillShortCut);
            player.sendMessage(msg);
            msg.cleanup();
            msg = Service.gI().messageSubCommand((byte) 61);
            msg.writer().writeUTF("OSkill");
            msg.writer().writeInt(skillShortCut.length);
            msg.writer().write(skillShortCut);
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean prepareQCKK;
    public boolean prepareTuSat;
    public boolean prepareLaze;

    public long lastTimePrepareQCKK;
    public long lastTimePrepareTuSat;
    public long lastTimePrepareLaze;

    public byte getIndexSkillSelect() {
        switch (skillSelect.template.id) {
            case Skill.DRAGON:
            case Skill.DEMON:
            case Skill.GALICK:
            case Skill.KAIOKEN:
            case Skill.LIEN_HOAN:
            case Skill.KIEM_KHI_TRUNKS:
            case Skill.DAI_KIEM_HY_VONG:
            case Skill.LOI_KIEM_PHA_KHONG:
            case Skill.DICH_ANH_LIEN_TRAM:
            case Skill.DUOI_DOC_HU_KHONG:
                return 1;
            case Skill.KAMEJOKO:
            case Skill.ANTOMIC:
            case Skill.MASENKO:
            case Skill.CAPSULE_HEN_GIO:
            case Skill.LUC_QUANG_PHAN_RA:
                return 2;
            case Skill.LIEN_HOAN_CHUONG:
            case Skill.SUPER_KAME:
            case Skill.MA_PHONG_BA:
                return 4;
            default:
                return 3;
        }
    }

    public byte getSizeSkill() {
        byte size = 0;
        for (Skill skill : skills) {
            // Dữ liệu pet/boss cũ có thể còn một ô kỹ năng rỗng sau khi
            // chuyển class 3/4 về Trái Đất/Xayda. Bỏ qua ô rỗng thay vì
            // làm dừng vòng cập nhật của boss.
            if (skill != null && skill.skillId != -1) {
                size++;
            }
        }
        return size;
    }

    public void dispose() {
        if (this.skillSelect != null) {
            this.skillSelect.dispose();
        }
        if (this.skills != null) {
            for (Skill skill : this.skills) {
                skill.dispose();
            }
            this.skills.clear();
        }
        this.player = null;
        this.skillSelect = null;
        this.skills = null;
    }
}
