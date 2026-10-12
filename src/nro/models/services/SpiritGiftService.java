package nro.models.services;

import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.player.Pet;
import nro.models.skill.Skill;
import nro.models.utils.Util;

/** Uron's spirit supplies; all rolls and inventory changes happen on the server. */
public final class SpiritGiftService {
    public static final int NORMAL_BOX = 2380;
    public static final int ADVANCED_BOX = 2381;
    public static final int BEAN = 2382;
    public static final int CHARM_X2 = 2383;
    public static final int CHARM_X3 = 2384;
    public static final int TIMED_CHARM_FIRST = 2450;
    public static final int TIMED_CHARM_LAST = 2455;
    public static final int REROLL_SKILL_FIRST = 2456;
    public static final int REROLL_SKILL_LAST = 2458;
    private static final long HOUR_MS = 60L * 60_000L;

    private SpiritGiftService() { }

    public static void use(Player player, Item item) {
        if (player == null || item == null || !item.isNotNullItem()) return;
        int id = item.template.id;
        if (id == NORMAL_BOX || id == ADVANCED_BOX) {
            openBox(player, item);
        } else if (id == BEAN) {
            if (player.pet == null || !SpiritDiscipleService.hasForm(player.pet)) {
                Service.gI().sendThongBao(player, "Bạn chưa có Tinh Linh.");
                return;
            }
            player.pet.nPoint.setFullHpMp();
            if (player.spiritPet != null) player.spiritPet.nPoint.setFullHpMp();
            InventoryService.gI().subQuantityItemsBag(player, item, 1);
            InventoryService.gI().sendItemBags(player);
            Service.gI().showInfoPet(player);
            Service.gI().sendThongBao(player, "Tinh Linh đã hồi đầy HP và KI.");
        } else if (id == CHARM_X2 || id == CHARM_X3) {
            int multiplier = id == CHARM_X2 ? 2 : 3;
            if (!canActivateCharm(player, multiplier)) return;
            InventoryService.gI().subQuantityItemsBag(player, item, 1);
            InventoryService.gI().sendItemBags(player);
            activateCharm(player, multiplier, HOUR_MS);
        } else if (id >= REROLL_SKILL_FIRST && id <= REROLL_SKILL_LAST) {
            rerollSkill(player, item);
        }
    }

    public static boolean isTimedCharm(int id) {
        return id >= TIMED_CHARM_FIRST && id <= TIMED_CHARM_LAST;
    }

    public static int charmMultiplier(int id) {
        return 2 + (id - TIMED_CHARM_FIRST) % 2;
    }

    public static long charmDuration(int id) {
        int period = (id - TIMED_CHARM_FIRST) / 2;
        return period == 0 ? HOUR_MS : period == 1 ? HOUR_MS * 24 : HOUR_MS * 24 * 30;
    }

    public static boolean canActivateCharm(Player player, int multiplier) {
        if (player.pet == null || !SpiritDiscipleService.hasForm(player.pet)) {
            Service.gI().sendThongBao(player, "Bạn chưa có Tinh Linh.");
            return false;
        }
        if (player.itemTime.spiritCharmUntil > System.currentTimeMillis()
                && player.itemTime.spiritCharmMultiplier != multiplier) {
            Service.gI().sendThongBao(player, "Bùa x" + player.itemTime.spiritCharmMultiplier
                    + " còn hiệu lực. Hãy dùng hết trước khi đổi loại bùa.");
            return false;
        }
        return true;
    }

    public static void activateCharm(Player player, int multiplier, long duration) {
        long now = System.currentTimeMillis();
        player.itemTime.spiritCharmMultiplier = multiplier;
        player.itemTime.spiritCharmUntil = Math.max(now, player.itemTime.spiritCharmUntil) + duration;
        long seconds = (player.itemTime.spiritCharmUntil - now) / 1000;
        ItemTimeService.gI().sendItemTime(player, multiplier == 2 ? 24013 : 24014,
                (int) Math.min(Integer.MAX_VALUE, seconds));
        Service.gI().sendThongBao(player, "Bùa Tinh Linh x" + multiplier
                + " đã kích hoạt, còn " + (seconds / 3600) + " giờ.");
    }

    private static void rerollSkill(Player player, Item ticket) {
        Pet pet = player.pet;
        if (!SpiritDiscipleService.hasForm(pet) || pet.playerSkill == null) {
            Service.gI().sendThongBao(player, "Bạn chưa có Tinh Linh.");
            return;
        }
        int slot = ticket.template.id - 2454;
        int index = slot - 1;
        if (pet.playerSkill.skills.size() <= index) {
            Service.gI().sendThongBao(player, "Tinh Linh chưa mở kỹ năng " + slot + ".");
            return;
        }
        Skill before = pet.playerSkill.skills.get(index);
        if (before == null || before.skillId < 0) {
            Service.gI().sendThongBao(player, "Tinh Linh chưa mở kỹ năng " + slot + ".");
            return;
        }
        for (int attempt = 0; attempt < 20; attempt++) {
            switch (slot) {
                case 2 -> pet.openSkill2();
                case 3 -> pet.openSkill3();
                case 4 -> pet.openSkill4();
                default -> { return; }
            }
            Skill next = pet.playerSkill.skills.get(index);
            if (next != null && next.template != null && before.template != null
                    && next.template.id != before.template.id) {
                InventoryService.gI().subQuantityItemsBag(player, ticket, 1);
                InventoryService.gI().sendItemBags(player);
                Service.gI().showInfoPet(player);
                Service.gI().sendThongBao(player, "Kỹ năng " + slot + " Tinh Linh đã đổi thành " + next.template.name + ".");
                return;
            }
        }
        pet.playerSkill.skills.set(index, before);
        Service.gI().sendThongBao(player, "Chưa đổi được kỹ năng khác, ấn chưa bị tiêu hao.");
    }

    public static int experienceMultiplier(Player disciple) {
        if (disciple == null || !disciple.isPet) return 1;
        nro.models.player.Pet pet = (nro.models.player.Pet) disciple;
        Player owner = pet.master;
        if (owner == null || owner.itemTime == null || !SpiritDiscipleService.hasForm(pet)
                || owner.itemTime.spiritCharmUntil <= System.currentTimeMillis()) return 1;
        return Math.max(1, Math.min(3, owner.itemTime.spiritCharmMultiplier));
    }

    /** Legacy client item packets carry option values in signed shorts. */
    public static int clientOptionParam(Item item, Item.ItemOption option) {
        if (item != null && item.template != null && option != null
                && option.optionTemplate != null && item.template.id >= 2360
                && item.template.id <= 2369 && option.optionTemplate.id == 0) {
            return Math.min(Short.MAX_VALUE, Math.max(0, option.param));
        }
        return option.param;
    }

    private static void openBox(Player player, Item box) {
        if (InventoryService.gI().getCountEmptyBag(player) < 1) {
            Service.gI().sendThongBao(player, "Cần 1 ô trống trong hành trang để mở hộp Tinh Linh.");
            return;
        }
        int tier = Util.nextInt(0, SpiritDiscipleRules.EQUIPMENT_SET_COUNT - 1);
        int first = SpiritDiscipleRules.FIRST_EQUIPMENT + tier * 4;
        int[] ids = {first, first + 1, SpiritDiscipleRules.FIRST_GLOVE + tier,
                first + 2, first + 3};
        int rewardId = ids[Util.nextInt(0, ids.length - 1)];
        Item reward = ItemService.gI().createNewItem((short) rewardId);
        ItemService.gI().normalizeSpiritDiscipleEquipment(reward);
        if (reward.template == null || reward.template.id != rewardId || reward.template.name.isBlank()) {
            Service.gI().sendThongBao(player, "Dữ liệu bộ Tinh Linh chưa sẵn sàng.");
            return;
        }
        if (!InventoryService.gI().addItemBag(player, reward)) {
            Service.gI().sendThongBao(player, "Không thể thêm đồ Tinh Linh; hộp chưa bị trừ.");
            return;
        }
        InventoryService.gI().subQuantityItemsBag(player, box, 1);
        InventoryService.gI().sendItemBags(player);
        Service.gI().sendThongBao(player, "Đã nhận " + reward.template.name + ".");
    }
}
