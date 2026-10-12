package nro.models.services;

import nro.models.item.Item;
import nro.models.player.Pet;
import nro.models.player.Player;

/** Persistent forms and passive bonuses for real disciples; independent of cosmetic Chibi. */
public final class SpiritDiscipleRules {
    public static final byte ORIGINAL = 0, WIND = 1, WITCH = 2, DEMON = 3;
    public static final int FIRST_HEAD = 3120;
    public static final int FIRST_EQUIPMENT = 2320;
    public static final int FIRST_GLOVE = 2360;
    public static final int EQUIPMENT_SET_COUNT = 5;
    public static final int FIRST_SET_OPTION = 42;
    public static final int SET_POTENTIAL = 0;
    public static final int SET_DAMAGE_X2 = 1;
    public static final int SET_HP_X2 = 2;
    public static final int SET_KI_X2 = 3;
    public static final int SET_BALANCED_X15 = 4;
    private SpiritDiscipleRules() { }

    public static byte sanitize(int form) { return form >= ORIGINAL && form <= DEMON ? (byte) form : ORIGINAL; }
    public static byte readForm(Object value) {
        if (value == null) return ORIGINAL;
        try { return sanitize(Integer.parseInt(String.valueOf(value))); }
        catch (NumberFormatException ignored) { return ORIGINAL; }
    }
    public static byte readSavedInfo(java.util.List<?> info) {
        return readForm(info != null && info.size() > 6 ? info.get(6) : null);
    }
    public static boolean active(int form) { return form >= WIND && form <= DEMON; }
    public static short head(int form) { return active(form) ? (short) (FIRST_HEAD + form - 1) : -1; }
    public static String name(int form) {
        return switch (sanitize(form)) {
            case WIND -> "Tinh linh gió";
            case WITCH -> "Phù thủy";
            case DEMON -> "Quỷ vương";
            default -> "Hình dạng gốc";
        };
    }
    public static String description(int form) {
        return switch (sanitize(form)) {
            case WIND -> "Sư phụ: +10% KI, +3% sức đánh";
            case WITCH -> "Sư phụ: +5% HP, +5% KI";
            case DEMON -> "Sư phụ: +10% HP, +5% giáp";
            default -> "Khôi phục hình dạng cũ; bỏ cộng thêm của linh thú";
        };
    }
    public static int hpPercent(int form) { return form == WITCH ? 5 : form == DEMON ? 10 : 0; }
    public static int mpPercent(int form) { return form == WIND ? 10 : form == WITCH ? 5 : 0; }
    public static int damagePercent(int form) { return form == WIND ? 3 : 0; }
    public static int armorPercent(int form) { return form == DEMON ? 5 : 0; }

    /** A complete set is five items of the same tier: armor, pants, gloves, boots and radar. */
    public static int completeEquipmentSet(Pet pet) {
        if (pet == null || pet.inventory == null || pet.inventory.itemsBody == null
                || pet.inventory.itemsBody.size() < 5 || !active(pet.companionForm)) return -1;
        for (int tier = 0; tier < EQUIPMENT_SET_COUNT; tier++) {
            int first = FIRST_EQUIPMENT + tier * 4;
            if (hasItem(pet, 0, first) && hasItem(pet, 1, first + 1)
                    && hasItem(pet, 2, FIRST_GLOVE + tier)
                    && hasItem(pet, 3, first + 2) && hasItem(pet, 4, first + 3)) return tier;
        }
        return -1;
    }

    public static boolean hasCompleteEquipmentSet(Pet pet) { return completeEquipmentSet(pet) >= 0; }

    private static boolean hasItem(Pet pet, int slot, int templateId) {
        Item item = pet.inventory.itemsBody.get(slot);
        if (item == null || !item.isNotNullItem() || item.template.id != templateId) return false;
        int set = templateId >= FIRST_GLOVE ? templateId - FIRST_GLOVE
                : (templateId - FIRST_EQUIPMENT) / 4;
        for (Item.ItemOption option : item.itemOptions) {
            if (option != null && option.optionTemplate != null
                    && option.optionTemplate.id == FIRST_SET_OPTION + set && option.param == 1) return true;
        }
        return false;
    }

    public static boolean ownerHasCompleteEquipmentSet(Player owner) {
        return owner != null && !owner.isPet && owner.pet != null && hasCompleteEquipmentSet(owner.pet);
    }

    public static long doubleWithLimit(long value) {
        return Math.min(Integer.MAX_VALUE, Math.max(1, value) * 2L);
    }

    private static long multiply(long value, int percent) {
        return Math.min(Integer.MAX_VALUE, Math.max(1, value) * percent / 100L);
    }

    public static long applyHpSet(Pet pet, long value) {
        int set = completeEquipmentSet(pet);
        return set == SET_HP_X2 ? multiply(value, 200)
                : set == SET_BALANCED_X15 ? multiply(value, 150) : value;
    }

    public static long applyKiSet(Pet pet, long value) {
        int set = completeEquipmentSet(pet);
        return set == SET_KI_X2 ? multiply(value, 200)
                : set == SET_BALANCED_X15 ? multiply(value, 150) : value;
    }

    public static long applyDamageSet(Pet pet, long value) {
        int set = completeEquipmentSet(pet);
        return set == SET_DAMAGE_X2 ? multiply(value, 200)
                : set == SET_BALANCED_X15 ? multiply(value, 150) : value;
    }

    public static long applyPotentialSet(Pet pet, long value) {
        return completeEquipmentSet(pet) == SET_POTENTIAL ? multiply(value, 250) : value;
    }

    /** Works on a freshly recalculated statistic, never on a persisted base attribute. */
    public static int addPercent(long value, int percent) {
        long base = Math.max(0, Math.min(Integer.MAX_VALUE, value));
        return (int) Math.min(Integer.MAX_VALUE, base + base * Math.max(0, percent) / 100L);
    }
}
