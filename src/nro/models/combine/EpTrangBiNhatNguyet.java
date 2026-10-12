package nro.models.combine;

import java.util.Iterator;
import nro.models.consts.ConstNpc;
import nro.models.item.Item;
import nro.models.item.Item.ItemOption;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.services.InventoryService;
import nro.models.services.Service;

/** Khắc dòng set Nhật Nguyệt lên trang bị bằng 99 nguyên liệu. */
public final class EpTrangBiNhatNguyet {

    private static final int MATERIAL_NGUYET_THUC = 1939;
    private static final int MATERIAL_NHAT_THUC = 1940;
    private static final int MATERIAL_BAN_NHAT_THUC = 1941;
    private static final int REQUIRED_MATERIAL = 99;

    public static final int OPTION_NGUYET_THUC = 251;
    public static final int OPTION_NHAT_THUC = 252;
    public static final int OPTION_BAN_NHAT_THUC = 253;

    private EpTrangBiNhatNguyet() {
    }

    private static Npc getNpc(Player player) {
        return player.idMark.getNpcChose() != null
                ? player.idMark.getNpcChose() : CombineService.gI().baHatMit;
    }

    public static void showInfoCombine(Player player) {
        Ingredients ingredients = findIngredients(player);
        if (ingredients == null) {
            getNpc(player).createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Cần chọn đúng 1 trang bị và 99 nguyên liệu Nhật Nguyệt cùng loại.", "Đóng");
            return;
        }

        SetInfo set = getSetInfo(ingredients.material.template.id);
        String npcSay = "|2|Khắc dòng " + set.name + "\n\n"
                + "|2|Nguyên liệu: 99 " + ingredients.material.template.name + "\n"
                + "|2|Tỉ lệ thành công: 100%\n"
                + "|7|Dòng Nhật Nguyệt cũ trên món đồ sẽ bị thay thế.\n"
                + "|7|" + set.fullSetBonus;
        getNpc(player).createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay,
                "Khắc dòng", "Từ chối");
    }

    public static void epTrangBi(Player player) {
        Ingredients ingredients = findIngredients(player);
        if (ingredients == null) {
            Service.gI().sendThongBao(player, "Không đủ trang bị hoặc 99 nguyên liệu Nhật Nguyệt.");
            return;
        }

        SetInfo set = getSetInfo(ingredients.material.template.id);
        removeOldSetOption(ingredients.equipment);
        // Đặt dòng hệ lên đầu để SetClothes ưu tiên nhận diện trước các dòng set cũ.
        ingredients.equipment.itemOptions.add(0, new ItemOption(set.optionId, 0));
        InventoryService.gI().subQuantityItemsBag(player, ingredients.material, REQUIRED_MATERIAL);

        player.combineNew.itemsCombine.clear();
        CombineService.gI().sendEffectSuccessCombine(player);
        InventoryService.gI().sendItemBags(player);
        Service.gI().sendThongBao(player,
                "Khắc thành công dòng " + set.name + " lên " + ingredients.equipment.template.name + "!");
        CombineService.gI().reOpenItemCombine(player);
    }

    private static Ingredients findIngredients(Player player) {
        if (player.combineNew.itemsCombine.size() != 2) {
            return null;
        }

        Item equipment = null;
        Item material = null;
        for (Item item : player.combineNew.itemsCombine) {
            if (item == null || !item.isNotNullItem()) {
                return null;
            }
            if (isMaterial(item.template.id)) {
                if (material != null || item.quantity < REQUIRED_MATERIAL) {
                    return null;
                }
                material = item;
            } else if (item.template.type >= 0 && item.template.type <= 4) {
                if (equipment != null) {
                    return null;
                }
                equipment = item;
            } else {
                return null;
            }
        }
        return equipment != null && material != null ? new Ingredients(equipment, material) : null;
    }

    private static void removeOldSetOption(Item equipment) {
        Iterator<ItemOption> iterator = equipment.itemOptions.iterator();
        while (iterator.hasNext()) {
            int optionId = iterator.next().optionTemplate.id;
            if (optionId == OPTION_NGUYET_THUC
                    || optionId == OPTION_NHAT_THUC
                    || optionId == OPTION_BAN_NHAT_THUC
                    || (optionId >= 256 && optionId <= 258)) {
                iterator.remove();
            }
        }
    }

    private static boolean isMaterial(int itemId) {
        return itemId >= MATERIAL_NGUYET_THUC && itemId <= MATERIAL_BAN_NHAT_THUC;
    }

    private static SetInfo getSetInfo(int materialId) {
        return switch (materialId) {
            case MATERIAL_NGUYET_THUC ->
                new SetInfo(OPTION_NGUYET_THUC, "Nguyệt Thực", "Đủ 5 món: tăng 20% sức đánh.");
            case MATERIAL_NHAT_THUC ->
                new SetInfo(OPTION_NHAT_THUC, "Nhật Thực", "Đủ 5 món: tăng 20% HP.");
            default ->
                new SetInfo(OPTION_BAN_NHAT_THUC, "Bán Nhật Thực", "Đủ 5 món: tăng 20% KI.");
        };
    }

    private record Ingredients(Item equipment, Item material) {
    }

    private record SetInfo(int optionId, String name, String fullSetBonus) {
    }
}
