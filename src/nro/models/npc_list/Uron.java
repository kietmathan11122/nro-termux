package nro.models.npc_list;

import nro.models.consts.ConstItem;
import nro.models.consts.ConstNpc;
import nro.models.item.Item;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.services.HeartsRebirth;
import nro.models.services.InventoryService;
import nro.models.services.Service;
import nro.models.services.SkillService;
import nro.models.shop.ShopService;
import nro.models.skill.Skill;
import nro.models.utils.SkillUtil;

public class Uron extends Npc {

    private static final int MENU_HEART_SKILLS = 21341;
    private static final int HEART_ULTIMATE_BOOKS = 9999;
    private static final long HEART_ULTIMATE_POWER = 40_000_000_000L;
    private static final int HEART_ULTIMATE_GOLD = 10_000_000;
    private static final int HEART_ULTIMATE_GEMS = 99;
    private static final String[] HEART_SKILL_NAMES = {
        "Đấm Hearts", "Chưởng Lập Phương", "Trọng Lực Lập Phương",
        "Tốc Biến Cước", "Hủy Diệt Lập Phương", "Bão Tinh Thể", "Ngục Lập Phương"
    };

    public Uron(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player pl) {
        if (canOpenNpc(pl)) {
            createOtherMenu(pl, ConstNpc.BASE_MENU,
                    "Ta có cửa hàng, đồ Tinh Linh và bộ kỹ năng dành cho Heart.",
                    "Cửa hàng", "Đồ\nTinh Linh", "Bùa\nTinh Linh",
                    "Sách\nHeart", "Đóng");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) return;
        if (player.idMark.isBaseMenu()) {
            if (select == 0) {
                ShopService.gI().opendShop(player, "URON_SHOP", false);
            } else if (select == 1) {
                ShopService.gI().opendShop(player, "URON_SPIRIT", false);
            } else if (select == 2) {
                ShopService.gI().opendShop(player, "URON_CHARM", false);
            } else if (select == 3) {
                if (!HeartsRebirth.isReborn(player)) {
                    Service.gI().sendThongBao(player, "Chỉ nhân vật Heart mới học được sách Heart.");
                } else {
                    ShopService.gI().opendShop(player, "URON_HEART", false);
                }
            }
            return;
        }
    }

    private void showHeartSkillMenu(Player player) {
        if (!HeartsRebirth.isReborn(player)) {
            createOtherMenu(player, ConstNpc.IGNORE_MENU, "Chỉ nhân vật Heart mới học được bộ kỹ năng này.", "Đóng");
            return;
        }
        String[] buttons = new String[8];
        for (int i = 0; i < HEART_SKILL_NAMES.length; i++) {
            Skill skill = SkillUtil.getSkillbyId(player, Skill.HEARTS_PUNCH + i);
            buttons[i] = HEART_SKILL_NAMES[i] + ((skill != null && skill.point > 0) ? "\nĐã học" : "\nChưa học");
        }
        buttons[7] = "Đóng";
        createOtherMenu(player, MENU_HEART_SKILLS,
                "Ta sẽ dạy ngươi bộ kỹ năng Heart.\n"
                + "Kỹ năng 6 cần điều kiện như tuyệt kỹ của Trái Đất, Namek và Xayda.", buttons);
    }

    private void learnHeartSkill(Player player, int select) {
        if (!HeartsRebirth.isReborn(player) || select < 0 || select >= HEART_SKILL_NAMES.length) return;
        int skillId = Skill.HEARTS_PUNCH + select;
        Skill current = SkillUtil.getSkillbyId(player, skillId);
        if (current != null && current.point > 0) {
            Service.gI().sendThongBao(player, "Bạn đã học " + HEART_SKILL_NAMES[select] + ".");
            return;
        }
        // Kỹ năng thứ 6: Bão Tinh Thể, học bằng cùng điều kiện tuyệt kỹ ba hành tinh.
        if (select == 5) {
            Item book = InventoryService.gI().findItemBag(player, ConstItem.BI_KIP_TUYET_KY);
            int quantity = book == null ? 0 : book.quantity;
            if (player.nPoint.power < HEART_ULTIMATE_POWER || player.inventory.gold < HEART_ULTIMATE_GOLD
                    || player.inventory.gem < HEART_ULTIMATE_GEMS || quantity < HEART_ULTIMATE_BOOKS) {
                Service.gI().sendThongBao(player,
                        "Cần 40 tỷ sức mạnh, 10.000.000 vàng, 99 ngọc và "
                        + HEART_ULTIMATE_BOOKS + " Bí kíp tuyệt kỹ. Hiện có " + quantity + ".");
                return;
            }
            InventoryService.gI().subQuantityItemsBag(player, book, HEART_ULTIMATE_BOOKS);
            player.inventory.gold -= HEART_ULTIMATE_GOLD;
            player.inventory.gem -= HEART_ULTIMATE_GEMS;
            InventoryService.gI().sendItemBags(player);
        }
        SkillService.gI().learSkillSpecial(player, (byte) skillId);
        putOnFirstEmptyShortcut(player, skillId);
        player.playerSkill.sendSkillShortCut();
        npcChat(player, "Ngươi đã học được " + HEART_SKILL_NAMES[select] + "!");
        Service.gI().sendThongBao(player, "Học kỹ năng thành công!");
    }

    private void putOnFirstEmptyShortcut(Player player, int skillId) {
        for (byte id : player.playerSkill.skillShortCut) if (id == skillId) return;
        for (int i = 0; i < player.playerSkill.skillShortCut.length; i++) {
            if (player.playerSkill.skillShortCut[i] < 0) {
                player.playerSkill.skillShortCut[i] = (byte) skillId;
                return;
            }
        }
    }
}
