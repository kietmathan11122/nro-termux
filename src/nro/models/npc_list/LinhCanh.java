package nro.models.npc_list;

import nro.models.clan.ClanMember;
import nro.models.consts.ConstNpc;
import nro.models.consts.ConstMap;
import nro.models.map.phoban.RedRibbonHQ;
import nro.models.map.service.ChangeMapService;
import nro.models.services_dungeon.RedRibbonHQService;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.map.service.NpcService;
import nro.models.item.Item;
import nro.models.services.InventoryService;
import nro.models.services.Service;
import nro.models.services.SkillService;
import nro.models.skill.Skill;
import nro.models.utils.SkillUtil;
import nro.models.utils.TimeUtil;
import nro.models.utils.Util;

public class LinhCanh extends Npc {

    private static final int MENU_SKILL_DAO_KHI_DOC = 21325;
    private static final int MENU_SKILL_HANH_TINH_V3 = 21326;
    private static final int SKILL_BOOK_ID = 2273;

    public LinhCanh(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (this.mapId == 213) {
                Item books = InventoryService.gI().findItemBag(player, SKILL_BOOK_ID);
                int bookCount = books == null ? 0 : books.quantity;
                createOtherMenu(player, ConstNpc.BASE_MENU,
                        "Ta canh giữ Đảo khí độc và truyền dạy tuyệt kỹ.\n"
                        + "Ngươi đang có " + bookCount + " Sách thành thạo kỹ năng.",
                        "Kỹ năng cũ", "Kỹ năng mới", "Đóng");
                return;
            }
            if (player.clan == null) {
                NpcService.gI().createTutorial(player, tempId, this.avartar,
                        "Chỉ tiếp các bang hội, miễn tiếp khách vãng lai");
                return;
            }
            if (!player.getSession().actived) {
                Service.gI().sendThongBao(player, "Vui lòng mở thành viên trước");
                return;
            }
            if (player.clan.getMembers().size() < RedRibbonHQ.N_PLAYER_CLAN) {
                NpcService.gI().createTutorial(player, tempId, this.avartar,
                        "Bang hội phải có ít nhất 3 thành viên mới có thể tham gia");
                return;
            }
            if (player.clanMember.getNumDateFromJoinTimeToToday() < 1) {
                NpcService.gI().createTutorial(player, tempId, this.avartar,
                        "Gia nhập bang hội trên 1 ngày mới được tham gia");
                return;
            }
         if (player.clan.doanhTrai != null && player.clan.doanhTrai.isOpened) {
    createOtherMenu(player, ConstNpc.MENU_JOIN_DOANH_TRAI,
        "Bang hội của ngươi đang đánh trại độc nhãn\nThời gian còn lại là "
        + TimeUtil.getTimeLeft(
            player.clan.doanhTrai.getLastTimeOpen(),
            RedRibbonHQ.TIME_DOANH_TRAI / 1000
        )
        + ". Ngươi có muốn tham gia không?",
        "Tham gia", "Không", "Hướng\ndẫn\nthêm");
    return;
}
            int nPlSameClan = 0;
            for (Player pl : player.zone.getPlayers()) {
                if (!pl.equals(player) && pl.clan != null
                        && pl.clan.equals(player.clan) && pl.location.x >= 1285
                        && pl.location.x <= 1645) {
                    nPlSameClan++;
                }
            }
            if (nPlSameClan < RedRibbonHQ.N_PLAYER_MAP) {
                createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Ngươi phải có ít nhất " + RedRibbonHQ.N_PLAYER_MAP + " đồng đội cùng bang đứng gần mới có thể vào\n"
                        + "tuy nhiên ta khuyên ngươi nên đi cùng với 3-4 người để khỏi chết. "
                        + "Hahaha.", "OK", "Hướng\ndẫn\nthêm");
                return;
            }
          if (player.clan.haveGoneDoanhTrai && !Util.isAfterMidnight(player.clan.lastTimeOpenDoanhTrai)) {
    if (!Util.isAfterMidnight(player.lastTimeJoinDT)) {

        String name = player.clan.playerOpenDoanhTrai != null
                ? player.clan.playerOpenDoanhTrai.name
                : "một thành viên trong bang";

        NpcService.gI().createTutorial(player, tempId, this.avartar,
                "Hôm nay là 1 ngày tuyệt vời\n"
                + "Thằng chó: " + name + " đã đi Doanh Trại trộm rồi\n"
                + "Thời gian nó cắn trộm là: "
                + TimeUtil.formatTime(player.clan.lastTimeOpenDoanhTrai, "HH:mm")
                + "\nHẹn gặp bạn vào ngày mai");
        return;
    }

                createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Bang hội của ngươi ngày hôm nay đã vào 1 lần rồi (thành viên " + player.clan.playerOpenDoanhTrai.name + ") lúc " + TimeUtil.formatTime(player.clan.lastTimeOpenDoanhTrai, "HH:mm") + "\n"
                        + "Nên ngươi không thể vào được nữa.\n"
                        + "Hãy chờ đến ngày mai để có thể vào miễn phí", "OK", "Hướng\ndẫn\nthêm");
                return;
            }
            createOtherMenu(player, ConstNpc.MENU_JOIN_DOANH_TRAI,
                    "Hôm nay bang hội của ngươi chưa vào trại lần nào. Ngươi có muốn vào\nkhông?\nĐể vào, ta khuyên ngươi nên có 3-4 người cùng bang đi cùng.",
                    "Vào\n(miễn phí)", "Không", "Hướng\ndẫn\nthêm");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (this.mapId == 213 && player.idMark.isBaseMenu()) {
                if (select == 0) {
                    showPlanetSkillMenu(player, false);
                } else if (select == 1) {
                    showPlanetSkillMenu(player, true);
                }
                return;
            }
            if (this.mapId == 213 && player.idMark.getIndexMenu() == MENU_SKILL_DAO_KHI_DOC) {
                if (select == 0) {
                    learnOrUpgradePlanetSkill(player, false);
                }
                return;
            }
            if (this.mapId == 213 && player.idMark.getIndexMenu() == MENU_SKILL_HANH_TINH_V3) {
                if (select == 0) learnOrUpgradePlanetSkill(player, true);
                return;
            }
            switch (player.idMark.getIndexMenu()) {
                case ConstNpc.MENU_JOIN_DOANH_TRAI -> {
                    if (select == 0) {
                        RedRibbonHQService.gI().joinDoanhTrai(player);
                    } else if (select == 2) {
                        NpcService.gI().createTutorial(player, tempId, this.avartar, ConstNpc.HUONG_DAN_DOANH_TRAI);
                    }
                }
                case ConstNpc.IGNORE_MENU -> {
                    if (select == 1) {
                        NpcService.gI().createTutorial(player, tempId, this.avartar, ConstNpc.HUONG_DAN_DOANH_TRAI);
                    }
                }
            }
        }
    }

    private void showPlanetSkillMenu(Player player, boolean newTechnique) {
        int skillId = getPlanetSkillId(player, newTechnique);
        Skill skill = SkillUtil.getSkillbyId(player, skillId);
        Item books = InventoryService.gI().findItemBag(player, SKILL_BOOK_ID);
        int bookCount = books == null ? 0 : books.quantity;
        String skillName = getPlanetSkillName(player, newTechnique);

        if (skill != null && skill.point >= 7) {
            createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    skillName + " đã đạt cấp 7 tối đa.", "Đóng");
            return;
        }

        int nextLevel = skill == null || skill.point <= 0 ? 1 : skill.point + 1;
        int requiredBooks = requiredBooks(nextLevel);
        int requiredUses = requiredUses(nextLevel);
        int currentUses = skill == null ? 0 : skill.currLevel;
        StringBuilder text = new StringBuilder();
        text.append("Học ").append(skillName).append(" cấp ").append(nextLevel).append("\n")
                .append("Sách thành thạo: ").append(bookCount).append("/").append(requiredBooks);
        if (requiredUses > 0) {
            text.append("\nĐộ thành thạo: ").append(currentUses).append("/").append(requiredUses)
                    .append(" lần sử dụng");
        }
        createOtherMenu(player, newTechnique ? MENU_SKILL_HANH_TINH_V3 : MENU_SKILL_DAO_KHI_DOC, text.toString(),
                nextLevel == 1 ? "Học ngay" : "Nâng cấp", "Đóng");
    }

    private void learnOrUpgradePlanetSkill(Player player, boolean newTechnique) {
        synchronized (player) {
            learnOrUpgradePlanetSkillLocked(player, newTechnique);
        }
    }

    private void learnOrUpgradePlanetSkillLocked(Player player, boolean newTechnique) {
        int skillId = getPlanetSkillId(player, newTechnique);
        Skill current = SkillUtil.getSkillbyId(player, skillId);
        int nextLevel = current == null || current.point <= 0 ? 1 : current.point + 1;
        if (nextLevel > 7) {
            Service.gI().sendThongBao(player, "Kỹ năng đã đạt cấp tối đa!");
            return;
        }

        Item books = InventoryService.gI().findItemBag(player, SKILL_BOOK_ID);
        int bookCount = books == null ? 0 : books.quantity;
        int booksNeeded = requiredBooks(nextLevel);
        int usesNeeded = requiredUses(nextLevel);
        int currentUses = current == null ? 0 : current.currLevel;
        if (bookCount < booksNeeded) {
            Service.gI().sendThongBao(player,
                    "Còn thiếu " + (booksNeeded - bookCount) + " Sách thành thạo kỹ năng.");
            return;
        }
        if (currentUses < usesNeeded) {
            Service.gI().sendThongBao(player,
                    "Cần sử dụng kỹ năng thêm " + (usesNeeded - currentUses) + " lần để đủ thành thạo.");
            return;
        }

        Skill nextSkill = SkillUtil.createSkill(skillId, nextLevel);
        if (nextSkill == null) {
            Service.gI().sendThongBao(player, "Không tìm thấy dữ liệu cấp kỹ năng này.");
            return;
        }
        nextSkill.currLevel = 0;
        SkillUtil.setSkill(player, nextSkill);
        InventoryService.gI().subQuantityItemsBag(player, books, booksNeeded);
        InventoryService.gI().sendItemBags(player);
        SkillService.gI().sendSkillLevelUpdate(player, nextSkill);
        player.playerSkill.sendSkillShortCut();
        Service.gI().sendThongBao(player,
                "Đã " + (nextLevel == 1 ? "học " : "nâng ") + getPlanetSkillName(player, newTechnique)
                + " lên cấp " + nextLevel + "!");
    }

    private int getPlanetSkillId(Player player, boolean newTechnique) {
        if (newTechnique) return Skill.THIEN_LONG_QUYEN + player.gender;
        return switch (player.gender) {
            case 0 -> Skill.QUA_CAU_HY_VONG;
            case 1 -> Skill.CHUONG_XUYEN_PHA;
            default -> Skill.NO_LOI_SINH_HOC;
        };
    }

    private String getPlanetSkillName(Player player, boolean newTechnique) {
        if (newTechnique) return switch (player.gender) {
            case 0 -> "Thiên Long Quyền";
            case 1 -> "Cuồng Phong";
            default -> "Mộc Đằng";
        };
        return switch (player.gender) {
            case 0 -> "Mưa Thiên Thạch";
            case 1 -> "Chưởng Xuyên Phá";
            default -> "Xên Bùng Nổ";
        };
    }

    private int requiredBooks(int nextLevel) {
        return nextLevel == 1 ? 5000 : 1000 + (nextLevel - 2) * 100;
    }

    private int requiredUses(int nextLevel) {
        return nextLevel == 1 ? 0 : 200 + (nextLevel - 2) * 50;
    }
}
// package nro.models.npc_list;

// import nro.models.consts.ConstNpc;
// import nro.models.npc.Npc;
// import nro.models.player.Player;
// import nro.models.map.service.NpcService;

// public class LinhCanh extends Npc {

//     public LinhCanh(int mapId, int status, int cx, int cy, int tempId, int avartar) {
//         super(mapId, status, cx, cy, tempId, avartar);
//     }

//     @Override
//     public void openBaseMenu(Player player) {
//         if (canOpenNpc(player)) {
//             NpcService.gI().createTutorial(player, tempId, this.avartar,
//                     "Doanh trại đã đầy, quay lại sau 30 phút");
//         }
//     }

//     @Override
//     public void confirmMenu(Player player, int select) {

//     }
// }
