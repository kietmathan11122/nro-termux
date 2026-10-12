package nro.models.services;

import nro.models.item.Item;
import nro.models.item.Item.ItemOption;
import nro.models.npc.MabuEgg;
import nro.models.player.Inventory;
import nro.models.player.Pet;
import nro.models.player.Player;
import nro.models.network.Message;
import nro.models.services.ItemService;
import nro.models.services.Service;
import nro.models.services_dungeon.NgocRongNamecService;
import nro.models.map.service.ChangeMapService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import nro.models.consts.ConstTaskBadges;
import nro.models.services_dungeon.BlackBallWarService;
import nro.models.map.service.ItemMapService;
import nro.models.task.BadgesTaskService;

/**
 *
 * @author By Mr Blue
 *
 */
public class InventoryService {

    private static InventoryService I;

    public static InventoryService gI() {
        if (InventoryService.I == null) {
            InventoryService.I = new InventoryService();
        }
        return InventoryService.I;
    }

    private void __________________Tìm_kiếm_item_____________________________() {
        //**********************************************************************
    }

    public Item findItem(List<Item> list, int tempId) {
        try {
            for (Item item : list) {
                if (item.isNotNullItem() && item.template.id == tempId) {
                    return item;
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    public Item findItemBody(Player player, int tempId) {
        return this.findItem(player.inventory.itemsBody, tempId);
    }

    public Item findItemBag(Player player, int tempId) {
        return this.findItem(player.inventory.itemsBag, tempId);
    }

    public Item findItemBox(Player player, int tempId) {
        return this.findItem(player.inventory.itemsBox, tempId);
    }

    public boolean isExistItem(List<Item> list, int tempId) {
        try {
            return this.findItem(list, tempId) != null;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isExistItemBody(Player player, int tempId) {
        return this.isExistItem(player.inventory.itemsBody, tempId);
    }

    public boolean isExistItemBag(Player player, int tempId) {
        return this.isExistItem(player.inventory.itemsBag, tempId);
    }

    public boolean isExistItemBox(Player player, int tempId) {
        return this.isExistItem(player.inventory.itemsBox, tempId);
    }

    private void __________________Sao_chép_danh_sách_item__________________() {
        //**********************************************************************
    }

    public List<Item> copyList(List<Item> items) {
        List<Item> list = new ArrayList<>();
        for (Item item : items) {
            list.add(ItemService.gI().copyItem(item));
        }
        return list;
    }

    public List<Item> copyItemsBody(Player player) {
        return copyList(player.inventory.itemsBody);
    }

    public List<Item> copyItemsBag(Player player) {
        return copyList(player.inventory.itemsBag);
    }

    public List<Item> copyItemsBox(Player player) {
        return copyList(player.inventory.itemsBox);
    }

    private void __________________Vứt_bỏ_item______________________________() {
        //**********************************************************************
    }

    public void throwItem(Player player, int where, int index) {
        Item itemThrow = null;
        if (where == 0) {
            itemThrow = player.inventory.itemsBody.get(index);
            removeItemBody(player, index);
            sendItemBody(player);
            Service.gI().Send_Caitrang(player);
        } else if (where == 1) {
            itemThrow = player.inventory.itemsBag.get(index);
            if (itemThrow.template != null && itemThrow.template.id == 570) {
                Service.gI().sendThongBao(player, "Không thể bỏ vật phẩm này.");
                return;
            }
            if (itemThrow.template != null && itemThrow.template.id != 457) {
                removeItemBag(player, index);
                sortItems(player.inventory.itemsBag);
                sendItemBags(player);
            } else {
                Service.gI().sendThongBao(player, "Bạn nên suy nghĩ lại");
                itemThrow = null;
            }
        }
        if (itemThrow == null) {
            return;
        }
        if (itemThrow.isNotNullItem()) {
            player.inventory.itemsRecycle.add(0, ItemService.gI().copyItem(itemThrow));
            while (player.inventory.itemsRecycle.size() > 20) {
                player.inventory.itemsRecycle.remove(player.inventory.itemsRecycle.size() - 1);
            }
        }
    }

    private void __________________Xoá_bỏ_item______________________________() {
        //**********************************************************************
    }

    public void removeItem(List<Item> items, int index) {
        Item item = ItemService.gI().createItemNull();
        items.set(index, item);
    }

    public void removeItem(List<Item> items, Item item) {
        if (item == null) {
            return;
        }
        Item it = ItemService.gI().createItemNull();
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).equals(item)) {
                items.set(i, it);
                item.dispose();
                break;
            }
        }
    }

    public void removeItemBag(Player player, int index) {
        this.removeItem(player.inventory.itemsBag, index);
    }

    public void removeItemBag(Player player, Item item) {
        this.removeItem(player.inventory.itemsBag, item);
    }

    public void removeItemBody(Player player, int index) {
        this.removeItem(player.inventory.itemsBody, index);
    }

    public void removeItemPetBody(Player player, int index) {
        this.removeItemBody(player.pet, index);
    }

    public void removeItemBox(Player player, int index) {
        this.removeItem(player.inventory.itemsBox, index);
    }

    private void __________________Giảm_số_lượng_item_______________________() {
        //**********************************************************************
    }

    public void subQuantityItemsBag(Player player, Item item, int quantity) {
        subQuantityItem(player.inventory.itemsBag, item, quantity);
    }

    public void subQuantityItemsBody(Player player, Item item, int quantity) {
        subQuantityItem(player.inventory.itemsBody, item, quantity);
    }

    public void subQuantityItemsBox(Player player, Item item, int quantity) {
        subQuantityItem(player.inventory.itemsBox, item, quantity);
    }

    public void subQuantityItem(List<Item> items, Item item, int quantity) {
        if (item != null) {
            for (Item it : items) {
                if (item.equals(it)) {
                    it.quantity -= quantity;
                    if (it.quantity <= 0) {
                        this.removeItem(items, item);
                    }
                    break;
                }
            }
        }
    }

    private boolean addItemRuongSuuTam(Player player, Item item) {
        for (int i = 0; i < player.inventory.itemsRuongSuuTam.size(); i++) {
            if (!player.inventory.itemsRuongSuuTam.get(i).isNotNullItem()) {
                player.inventory.itemsRuongSuuTam.set(i, ItemService.gI().copyItem(item));
                item.quantity = 0;
                return true;
            }
        }
        if (player.inventory.itemsRuongSuuTam.size() < player.inventory.sizeRuongSuuTam) {
            player.inventory.itemsRuongSuuTam.add(ItemService.gI().copyItem(item));
            item.quantity = 0;
            return true;
        }
        Service.gI().sendThongBao(player, "Rương sưu tầm đã đầy!");
        return false;
    }

    private boolean addItemRuongDanhHieu(Player player, Item item) {
        for (int i = 0; i < player.inventory.itemsRuongDanhHieu.size(); i++) {
            if (!player.inventory.itemsRuongDanhHieu.get(i).isNotNullItem()) {
                player.inventory.itemsRuongDanhHieu.set(i, ItemService.gI().copyItem(item));
                item.quantity = 0;
                return true;
            }
        }
        if (player.inventory.itemsRuongDanhHieu.size() < player.inventory.sizeRuongDanhHieu) {
            player.inventory.itemsRuongDanhHieu.add(ItemService.gI().copyItem(item));
            item.quantity = 0;
            return true;
        }
        Service.gI().sendThongBao(player, "Rương danh hiệu đã đầy!");
        return false;
    }

    private void __________________Sắp_xếp_danh_sách_item___________________() {
        //**********************************************************************
    }

    public void sortItems(List<Item> list) {
        for (int i = 0; i < list.size() - 1; i++) {
            Item current = list.get(i);
            if (current == null || !current.isNotNullItem()) {
                int indexSwap = -1;
                for (int j = i + 1; j < list.size(); j++) {
                    Item candidate = list.get(j);
                    if (candidate != null && candidate.isNotNullItem()) {
                        indexSwap = j;
                        break;
                    }
                }
                if (indexSwap != -1) {
                    list.set(i, list.get(indexSwap));
                    list.set(indexSwap, ItemService.gI().createItemNull());
                } else {
                    break;
                }
            }
        }
    }

    public Item finditemBongHoa(Player player, int soluong) {
        for (Item item : player.inventory.itemsBag) {
            if (item.isNotNullItem() && (item.template.id == 589) && item.quantity >= soluong) {
                return item;
            }
        }
        return null;
    }

    public void sortItemv2(List<Item> items) {
        int index = 0;
        for (Item item : items) {
            if (item != null && item.quantity > 0) {
                items.set(index, item);
                index++;
            }
        }
        for (int i = index; i < items.size(); i++) {
            items.set(i, null);
        }
    }

    private void __________________Thao_tác_tháo_mặc_item___________________() {
        //**********************************************************************
    }

    private Item putItemBag(Player player, Item item) {
        for (int i = 0; i < player.inventory.itemsBag.size(); i++) {
            if (!player.inventory.itemsBag.get(i).isNotNullItem()) {
                player.inventory.itemsBag.set(i, item);
                Item sItem = ItemService.gI().createItemNull();
                return sItem;
            }
        }
        return item;
    }

    private Item putItemBox(Player player, Item item) {
        for (int i = 0; i < player.inventory.itemsBox.size(); i++) {
            if (!player.inventory.itemsBox.get(i).isNotNullItem()) {
                player.inventory.itemsBox.set(i, item);
                Item sItem = ItemService.gI().createItemNull();
                return sItem;
            }
        }
        return item;
    }

    public Item putItemBody(Player player, Item item) {
        byte type = item.getType();
        Item sItem = item;
        if (!item.isNotNullItem()) {
            return sItem;
        }

        if (ItemService.isSpiritDiscipleEquipment(item.template.id)
                && (!player.isPet || !nro.models.services.SpiritDiscipleService.hasForm((Pet) player))) {
            Service.gI().sendThongBaoOK(player.isPet ? ((Pet) player).master : player,
                    "Trang bị này chỉ dành cho đệ Tinh Linh!");
            return sItem;
        }

        switch (item.template.type) {
            case 0, 1, 2, 3, 4, 5, 32, 23, 24, 11, 27, 25, 36, 37 -> {
            }
            default -> {
                Service.gI().sendThongBaoOK(player.isPet ? ((Pet) player).master : player, "Trang bị không phù hợp!1");
                return sItem;
            }
        }

        // Đệ tử dùng cùng ô áo/quần/cải trang với nhân vật. Không khóa theo giới
        // tính cho riêng Đệ tử, để các bộ cải trang và trang bị hợp lệ có thể mặc
        // được; các kiểm tra loại vật phẩm, sức mạnh và vật phẩm cấm bên dưới vẫn giữ nguyên.
        boolean isPetWearable = player.isPet && item.template.type >= 0 && item.template.type <= 5;
        if (!isPetWearable && item.template.gender < 3 && item.template.gender != player.gender) {
            Service.gI().sendThongBaoOK(player.isPet ? ((Pet) player).master : player, "Trang bị không phù hợp!");
            return sItem;
        }

        // Kiểm tra các vật phẩm có ID đặc biệt
        if (item.getId() == 691 || item.getId() == 692 || item.getId() == 693) {
            List<Item> itemsBody = player.inventory.itemsBody;
            if (itemsBody.get(0).isNotNullItem() && itemsBody.get(5).isNotNullItem()) {
                Service.gI().sendThongBaoOK(player.isPet ? ((Pet) player).master : player, "Vui lòng cởi áo để có thể sử dụng!");
                return sItem;
            }
        }

        // Kiểm tra yêu cầu sức mạnh
        long powerRequire = item.template.strRequire;
        for (Item.ItemOption io : item.itemOptions) {
            if (io.optionTemplate.id == 21) {
                powerRequire = io.param * 1000000000L;
                break;
            }
        }
        if (player.nPoint.power < powerRequire) {
            Service.gI().sendThongBaoOK(player.isPet ? ((Pet) player).master : player, "Sức mạnh không đủ yêu cầu!");
            return sItem;
        }
        //handleOption210(item);
        //checkOption231(item);
        if (player.isPet && ((item.template.id >= 2020 && item.template.id <= 2029) || (item.template.id >= 2305 && item.template.id <= 2311))) {
            Pet pet = (Pet) player;
            Player recipient = pet.master != null ? pet.master : player;
            Service.gI().sendThongBaoOK(recipient, "Đệ tử không thể sử dụng vật phẩm này!");
            return sItem;
        }

        int index = -1;
        if (item.template.id >= 2020 && item.template.id <= 2029) {
            index = 11;
        } else if (item.template.id >= 2305 && item.template.id <= 2311) {
            index = 14;
        } else {
            switch (item.template.type) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                    index = item.template.type;
                    break;
                case 32:
                    index = 6;
                    break;
                case 23:
                case 24:
                    index = 7;
                    break;
                case 11:
                    index = 8;
                    break;
                case 27:
                    if (item.template.id == 2046) {
                        index = 12;
                    } else {
                        index = 9;
                    }
                    break;
                case 25:
                    index = 10;
                    break;
                case 36:
                    index = 13;
                    break;
                case 37:
                    index = 12;
                    break;
            }
        }
        if (!player.isPet && item.getOptionById(218) != null) {
            Service.gI().sendThongBaoOK(player, "Trang bị này chỉ dành cho đệ tử!");
            return sItem;
        }

        if (player.isPet && item.template.type == 11) {
            Player recipient = ((Pet) player).master;
            if (recipient == null) {
                recipient = player;
            }
            if (item.getOptionById(218) == null) {
                Service.gI().sendThongBaoOK(recipient, "Đệ tử chỉ mang được cánh khi có dòng dành riêng cho đệ tử!");
                return sItem;
            }
        }

        if (player.isPet && item.template.type == 25) {
            Pet pet = (Pet) player;
            if (pet.type != 2 && pet.type != 3 && pet.type != 4) {
                Player recipient = pet.master != null ? pet.master : player;
                Service.gI().sendThongBaoOK(recipient, "Chỉ đệ tử vip mới sử dụng được vật phẩm này!");
                return sItem;
            }
        }

        if (player.isPet && (item.template.type == 23 || item.template.type == 24 || item.template.type == 27)) {
            Player recipient = ((Pet) player).master;
            if (recipient == null) {
                recipient = player;
            }
            Service.gI().sendThongBaoOK(recipient, "Đệ tử không thể sử dụng vật phẩm này!");
            return sItem;
        }

        if (player.isPet) {
            Pet pet = (Pet) player;
            while (pet.inventory.itemsBody.size() <= index) {
                pet.inventory.itemsBody.add(ItemService.gI().createItemNull());
            }
        } else {
            while (player.inventory.itemsBody.size() <= index) {
                player.inventory.itemsBody.add(ItemService.gI().createItemNull());
            }
        }

        sItem = player.inventory.itemsBody.get(index);
        player.inventory.itemsBody.set(index, item);
        return sItem;

    }

    public void itemBagToBody(Player player, int index) {
        if (index < 0) {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
            return;
        }
        Item item = player.inventory.itemsBag.get(index);
        if (item.isNotNullItem()) {
            short previousMount = player.getMount();
            player.inventory.itemsBag.set(index, putItemBody(player, item));
            if (!player.isPet && item.template.type == 27
                    && player.inventory.itemsBody.size() > 9
                    && player.inventory.itemsBody.get(9) == item) {
                player.refreshEquippedPet();
            }
            sendItemBags(player);
            sendItemBody(player);
            Service.gI().point(player);
            Service.gI().Send_Caitrang(player);
            if ((item.template.type == 23 || item.template.type == 24)
                    && previousMount != player.getMount() && player.zone != null) {
                player.zone.refreshMount(player);
            }
            updateTitleFromBody(player);
        }
    }

    public void itemBodyToBag(Player player, int index) {
        Item item = player.inventory.itemsBody.get(index);
        if (item.isNotNullItem()) {
            short previousMount = player.getMount();
            if (index == 12) {
                Service.gI().sendPetFollow(player, (short) 0);
            }
            if ((index == 9 || index == 12) && !player.isPet && item.template.type != 25) {
                if (index == 9 && player.newPet != null) {
                    ChangeMapService.gI().exitMap(player.newPet);
                    player.newPet.dispose();
                    player.newPet = null;
                }
                if (index == 12 && player.newPet2 != null) {
                    ChangeMapService.gI().exitMap(player.newPet2);
                    player.newPet2.dispose();
                    player.newPet2 = null;
                }
            }
            player.inventory.itemsBody.set(index, putItemBag(player, item));
            sendItemBags(player);
            sendItemBody(player);
            Service.gI().player(player);
            if (index == 7 && (item.template.type == 23 || item.template.type == 24)
                    && previousMount != player.getMount()) {
                player.zone.refreshMount(player);
            } else {
                player.zone.load_Me_To_Another(player);
            }
            player.zone.load_Another_To_Me(player);
            Service.gI().Send_Caitrang(player);
            Service.gI().sendFlagBag(player);
            Service.gI().point(player);
            updateTitleFromBody(player);
        }
    }

    public void itemBagToPetBody(Player player, int index) {
        itemBagToCompanionBody(player, index, false);
    }

    public void itemBagToSpiritBody(Player player, int index) {
        itemBagToCompanionBody(player, index, true);
    }

    private void itemBagToCompanionBody(Player player, int index, boolean spiritAction) {
        if (player == null || player.inventory == null || index < 0 || index >= player.inventory.itemsBag.size()) return;
        Item requested = player.inventory.itemsBag.get(index);
        if (requested == null || !requested.isNotNullItem()) return;
        boolean spiritEquipment = ItemService.isSpiritDiscipleEquipment(requested.template.id);
        if (spiritEquipment != spiritAction) {
            Service.gI().sendThongBao(player, spiritEquipment
                    ? "Đệ tử không thể mặc trang bị này. Hãy chọn Sử dụng cho tinh linh."
                    : "Đây không phải trang bị tinh linh.");
            return;
        }
        if (spiritAction && !SpiritDiscipleService.hasForm(player.pet)) {
            Service.gI().sendThongBao(player, "Bạn chưa có tinh linh để sử dụng trang bị này.");
            return;
        }
        try {
            if (player.pet != null && (player.pet.nPoint.power >= 1500000
                    || nro.models.services.SpiritDiscipleService.hasForm(player.pet))) {
                Item item = player.inventory.itemsBag.get(index);
                if (item.isNotNullItem()) {
                    Item itemSwap = putItemBody(player.pet, item);
                    player.inventory.itemsBag.set(index, itemSwap);
                    sendItemBags(player);
                    sendItemBody(player);
                    if (!itemSwap.equals(item)) {
                        Service.gI().point(player);
                        Service.gI().showInfoPet(player);
                    }
                    Service.gI().Send_Caitrang(player.pet);
                    Service.gI().Send_Caitrang(player);
                }
            } else {
                Service.gI().sendThongBao(player, "Đệ tử phải đạt 1tr5 sức mạnh mới có thể mặc");
            }
        } catch (Exception E) {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
        }
    }

    public void itemPetBodyToBag(Player player, int index) {
        Item item = player.pet.inventory.itemsBody.get(index);
        if (item.isNotNullItem()) {
            player.pet.inventory.itemsBody.set(index, putItemBag(player, item));
            sendItemBags(player);
            sendItemBody(player);
            Service.gI().point(player);
            Service.gI().Send_Caitrang(player.pet);
            Service.gI().Send_Caitrang(player);
            Service.gI().showInfoPet(player);
        }
    }

    public void itemBoxToBodyOrBag(Player player, int index) {
        if (index < 0) {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
            return;
        }
        if (player.isOpeningRuongSuuTam) {
            if (index >= player.inventory.itemsRuongSuuTam.size()) return;
            Item item = player.inventory.itemsRuongSuuTam.get(index);
            if (item.isNotNullItem()) {
                if (addItemBag(player, item)) {
                    if (item.quantity == 0) {
                        Item sItem = ItemService.gI().createItemNull();
                        player.inventory.itemsRuongSuuTam.set(index, sItem);
                    }
                    sendItemBags(player);
                    sendItemRuongSuuTam(player);
                }
            }
            return;
        }
        if (player.isOpeningRuongDanhHieu) {
            if (index >= player.inventory.itemsRuongDanhHieu.size()) return;
            Item item = player.inventory.itemsRuongDanhHieu.get(index);
            if (item.isNotNullItem()) {
                if (addItemBag(player, item)) {
                    if (item.quantity == 0) {
                        Item sItem = ItemService.gI().createItemNull();
                        player.inventory.itemsRuongDanhHieu.set(index, sItem);
                    }
                    sendItemBags(player);
                    sendItemRuongDanhHieu(player);
                    player.nPoint.calPoint();
                    Service.gI().point(player);
                }
            }
            return;
        }
        Item item = player.inventory.itemsBox.get(index);
        if (item.isNotNullItem()) {
            boolean done = false;
            if (item.template.type >= 0 && item.template.type <= 5 || item.template.type == 32) {
                Item itemBody = player.inventory.itemsBody.get(item.template.type == 32 ? 6 : item.template.type);
                if (!itemBody.isNotNullItem()) {
                    if (item.template.gender == player.gender || item.template.gender == 3) {
                        long powerRequire = item.template.strRequire;
                        for (Item.ItemOption io : item.itemOptions) {
                            if (io.optionTemplate.id == 21) {
                                powerRequire = io.param * 1000000000L;
                                break;
                            }
                        }
                        if (powerRequire <= player.nPoint.power) {
                            player.inventory.itemsBody.set(item.template.type == 32 ? 6 : item.template.type, item);
                            player.inventory.itemsBox.set(index, itemBody);
                            done = true;

                            sendItemBody(player);
                            Service.gI().point(player);
                            Service.gI().Send_Caitrang(player);
                        }
                    }
                }
            }
            if (!done) {
                if (addItemBag(player, item)) {

                    if (item.quantity == 0) {
                        Item sItem = ItemService.gI().createItemNull();
                        player.inventory.itemsBox.set(index, sItem);
                    }
                    sendItemBags(player);
                }
            }
        }
    }

    public void itemBagToBox(Player player, int index) {
        if (index < 0 || index >= player.inventory.itemsBag.size()) {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
            return;
        }
        Item item = player.inventory.itemsBag.get(index);
        if (item != null && item.isNotNullItem()) {
            if (item.template.id == ChibiRingService.ITEM_ID && player.chibiRingActive) {
                ChibiRingService.gI().toggle(player);
            }
            if (player.isOpeningRuongDanhHieu) {
                if (item.template.type != 36) {
                    Service.gI().sendThongBao(player, "Chỉ có thể bỏ vật phẩm Danh hiệu vào rương này!");
                    return;
                }
                boolean added = addItemRuongDanhHieu(player, item);
                if (added) {
                    if (item.quantity == 0) {
                        Item sItem = ItemService.gI().createItemNull();
                        player.inventory.itemsBag.set(index, sItem);
                    }
                    sortItems(player.inventory.itemsBag);
                    sendItemBags(player);
                    sendItemRuongDanhHieu(player);
                    player.nPoint.calPoint();
                    Service.gI().point(player);
                }
                return;
            }
            if (player.isOpeningRuongSuuTam) {
                // Chỉ cho phép Cải trang, Pet, Cờ và Phụ kiện vĩnh viễn (không có HSD)
                boolean isPermanent = true;
                for (Item.ItemOption io : item.itemOptions) {
                    if (io.optionTemplate.id == 93) { // Hạn sử dụng
                        isPermanent = false;
                        break;
                    }
                }
                if (!isPermanent || (item.template.type != 5 && item.template.type != 11 && item.template.type != 23 && item.template.type != 24 && item.template.type != 27 && item.template.type != 32)) {
                    Service.gI().sendThongBao(player, "Chỉ có thể bỏ vật phẩm Cải trang, Pet hoặc Phụ kiện vĩnh viễn vào rương này!");
                    return;
                }
                if (item.template.name.toLowerCase().contains("bông tai") || item.template.id == 454 || item.template.id == 921 || item.template.id == 1819 || item.template.id == 2000) {
                    Service.gI().sendThongBao(player, "Không thể bỏ Bông tai vào rương này!");
                    return;
                }
                boolean added = addItemRuongSuuTam(player, item);
                if (added) {
                    if (item.quantity == 0) {
                        Item sItem = ItemService.gI().createItemNull();
                        player.inventory.itemsBag.set(index, sItem);
                    }
                    sortItems(player.inventory.itemsBag);
                    sendItemBags(player);
                    sendItemRuongSuuTam(player);
                    Service.gI().point(player);
                }
                return;
            }
            boolean added = addItemBox(player, item);

            if (added) {
                if (item.quantity == 0) {
                    Item sItem = ItemService.gI().createItemNull();
                    player.inventory.itemsBag.set(index, sItem);
                }
                sortItems(player.inventory.itemsBag);
                sendItemBags(player);
                sendItemBox(player);
            }
        }
    }

    public void itemBodyToBox(Player player, int index) {
        if (index < 0 || index >= player.inventory.itemsBody.size()) {
            Item item = player.inventory.itemsBody.get(index);
            if (item.isNotNullItem()) {
                player.inventory.itemsBody.set(index, putItemBox(player, item));
                sortItems(player.inventory.itemsBag);
                sendItemBody(player);
                sendItemBox(player);
                Service.gI().point(player);
                Service.gI().Send_Caitrang(player);
            }
        }
    }

    private void __________________Gửi_danh_sách_item_cho_người_chơi________() {
        //**********************************************************************
    }

    public void sendItemBags(Player player) {
        mergeCoHonEventStacks(player.inventory.itemsBag);
        sortItems(player.inventory.itemsBag);
        Message msg;
        try {
            msg = new Message(-36);
            msg.writer().writeByte(0);
            msg.writer().writeByte(player.inventory.itemsBag.size());
            for (int i = 0; i < player.inventory.itemsBag.size(); i++) {
                Item item = player.inventory.itemsBag.get(i);
                if (!item.isNotNullItem()) {
                    continue;
                }
                msg.writer().writeShort(item.template.id);
                msg.writer().writeInt(item.quantity);
                msg.writer().writeUTF(item.getInfo());
                msg.writer().writeUTF(item.getContent());
                msg.writer().writeByte(item.itemOptions.size()); //options
                for (int j = 0; j < item.itemOptions.size(); j++) {
                    if (item.itemOptions.get(j).optionTemplate.id == 213) {
                        int opId = 213;
                        int param = item.itemOptions.get(j).param;
                        if (param > 1_000_000) {
                            opId = 223;
                            param /= 1_000_000;
                        } else if (param > 1000) {
                            opId = 222;
                            param /= 1000;
                        }
                        msg.writer().writeByte(opId);
                        msg.writer().writeShort(param);
                    } else {
                        msg.writer().writeByte(item.itemOptions.get(j).optionTemplate.id);
                        msg.writer().writeShort(item.itemOptions.get(j).param);
                    }
                }
            }

            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    private void mergeCoHonEventStacks(List<Item> items) {
        for (int eventItemId : new int[]{2255, 2256, 2257, 2258}) {
            Item stack = null;
            long total = 0;
            for (int i = 0; i < items.size(); i++) {
                Item current = items.get(i);
                if (current.isNotNullItem() && current.template.id == eventItemId) {
                    total += current.quantity;
                    if (stack == null) {
                        stack = current;
                    } else {
                        items.set(i, ItemService.gI().createItemNull());
                    }
                }
            }
            if (stack != null) {
                stack.quantity = (int) Math.min(total, 99_999);
            }
        }
    }

    public void sendItemBody(Player player) {
        Message msg;
        try {
            msg = new Message(-37);
            msg.writer().writeByte(0);
            int bodySize = Math.min(15, player.inventory.itemsBody.size());
            msg.writer().writeByte(bodySize);
            for (int i = 0; i < bodySize; i++) {
                Item item = player.inventory.itemsBody.get(i);
                if (!item.isNotNullItem()) {
                    msg.writer().writeShort(-1);
                } else {
                    msg.writer().writeShort(item.template.id);
                    msg.writer().writeInt(item.quantity);
                    msg.writer().writeUTF(item.getInfo());
                    msg.writer().writeUTF(item.getContent());
                    List<Item.ItemOption> itemOptions = item.itemOptions;
                    msg.writer().writeByte(itemOptions.size());
                    for (Item.ItemOption itemOption : itemOptions) {
                        if (itemOption.optionTemplate.id == 213) {
                            int opId = 213;
                            int param = itemOption.param;
                            if (param > 1_000_000) {
                                opId = 223;
                                param /= 1_000_000;
                            } else if (param > 1000) {
                                opId = 222;
                                param /= 1000;
                            }
                            msg.writer().writeByte(opId);
                            msg.writer().writeShort(param);
                        } else {
                            msg.writer().writeByte(itemOption.optionTemplate.id);
                            msg.writer().writeShort(SpiritGiftService.clientOptionParam(item, itemOption));
                        }
                    }
                }
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
        }
        Service.gI().Send_Caitrang(player);
    }

    public void sendItemBox(Player player) {
        player.isOpeningRuongSuuTam = false;
        player.isOpeningRuongDanhHieu = false;
        Message msg;
        try {
            msg = new Message(-35);
            msg.writer().writeByte(0);
            msg.writer().writeByte(player.inventory.itemsBox.size());
            for (Item it : player.inventory.itemsBox) {
                msg.writer().writeShort(it.isNotNullItem() ? it.template.id : -1);
                if (it.isNotNullItem()) {
                    msg.writer().writeInt(it.quantity);
                    msg.writer().writeUTF(it.getInfo());
                    msg.writer().writeUTF(it.getContent());
                    msg.writer().writeByte(it.itemOptions.size());
                    for (Item.ItemOption io : it.itemOptions) {
                        if (io.optionTemplate.id == 213) {
                            int opId = 213;
                            int param = io.param;
                            if (param > 1_000_000) {
                                opId = 223;
                                param /= 1_000_000;
                            } else if (param > 1000) {
                                opId = 222;
                                param /= 1000;
                            }
                            msg.writer().writeByte(opId);
                            msg.writer().writeShort(param);
                        } else {
                            msg.writer().writeByte(io.optionTemplate.id);
                            msg.writer().writeShort(SpiritGiftService.clientOptionParam(it, io));
                        }
                    }
                }
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
        }
        this.openBox(player);
    }

    public void sendItemRuongSuuTam(Player player) {
        while (player.inventory.itemsRuongSuuTam.size() < player.inventory.sizeRuongSuuTam) {
            player.inventory.itemsRuongSuuTam.add(ItemService.gI().createItemNull());
        }
        Message msg;
        try {
            msg = new Message(-35);
            msg.writer().writeByte(0);
            msg.writer().writeByte(player.inventory.itemsRuongSuuTam.size());
            for (Item it : player.inventory.itemsRuongSuuTam) {
                msg.writer().writeShort(it.isNotNullItem() ? it.template.id : -1);
                if (it.isNotNullItem()) {
                    msg.writer().writeInt(it.quantity);
                    msg.writer().writeUTF(it.getInfo());
                    msg.writer().writeUTF(it.getContent());
                    msg.writer().writeByte(it.itemOptions.size());
                    for (Item.ItemOption io : it.itemOptions) {
                        if (io.optionTemplate.id == 213) {
                            int opId = 213;
                            int param = io.param;
                            if (param > 1_000_000) {
                                opId = 223;
                                param /= 1_000_000;
                            } else if (param > 1000) {
                                opId = 222;
                                param /= 1000;
                            }
                            msg.writer().writeByte(opId);
                            msg.writer().writeShort(param);
                        } else {
                            msg.writer().writeByte(io.optionTemplate.id);
                            msg.writer().writeShort(SpiritGiftService.clientOptionParam(it, io));
                        }
                    }
                }
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
        }
        this.openBox(player);
    }

    public void openBox(Player player) {
        Message msg;
        try {
            msg = new Message(-35);
            msg.writer().writeByte(1);
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    private void __________________Thêm_vật_phẩm_vào_danh_sách______________() {
        //**********************************************************************
    }

    private boolean addItemSpecial(Player player, Item item) {
        if (item.template.id == 2037) {
            if (player.idMark.getShopOpen() != null && player.idMark.getShopOpen().tagName.equals("DENDE")) {
                if (player.itemTime.isBuaRobot) {
                    long remaining = nro.models.item.ItemTime.TIME_BUA_ROBOT - (System.currentTimeMillis() - player.itemTime.lastTimeBuaRobot);
                    player.itemTime.lastTimeBuaRobot = System.currentTimeMillis() - (nro.models.item.ItemTime.TIME_BUA_ROBOT - (remaining + nro.models.item.ItemTime.TIME_BUA_ROBOT));
                } else {
                    player.itemTime.isBuaRobot = true;
                    player.itemTime.lastTimeBuaRobot = System.currentTimeMillis();
                }
                nro.models.services.ItemTimeService.gI().sendItemTime(player, 11493, (int) ((nro.models.item.ItemTime.TIME_BUA_ROBOT - (System.currentTimeMillis() - player.itemTime.lastTimeBuaRobot)) / 1000));
                return true;
            }
        }

        //bùa
        if (item.template.type == 13) {
            int min = 0;
            try {
                String tagShopBua = player.idMark.getShopOpen().tagName;
                if (tagShopBua.equals("BUA_1H")) {
                    min = 60;
                } else if (tagShopBua.equals("BUA_8H")) {
                    min = 60 * 8;
                } else if (tagShopBua.equals("BUA_1M")) {
                    min = 60 * 24 * 30;
                }
            } catch (Exception e) {
            }
            player.charms.addTimeCharms(item.template.id, min);
            return true;
        }

        switch (item.template.id) {
            case 453: //tàu tennis
                player.haveTennisSpaceShip = true;
                return true;
            case 74: //đùi gà nướng
                player.nPoint.setFullHpMp();
                PlayerService.gI().sendInfoHpMp(player);
                return true;
        }
        return false;
    }

    public boolean addItemBag(Player player, Item item) {
        //ngọc rồng đen
        if (ItemMapService.gI().isBlackBall(item.template.id)) {
            return BlackBallWarService.gI().pickBlackBall(player, item);
        }

        //ngọc rồng namek
        if (ItemMapService.gI().isNamecBall(item.template.id) || ItemMapService.gI().isNamecBallStone(item.template.id)) {
            return NgocRongNamecService.gI().pickNamekBall(player, item);
        }
        if (item.template.id == 2072) {
            long ruby = (long) player.inventory.ruby + (long) item.quantity;
            if (ruby > Integer.MAX_VALUE) {
                ruby = Integer.MAX_VALUE;
            }
            player.inventory.ruby = (int) ruby;
            Service.gI().sendMoney(player);
            return true;
        }
        if (addItemSpecial(player, item)) {
            return true;
        }

        //gold, gem, ruby
        switch (item.template.type) {
            case 9:
                if (player.inventory.gold + item.quantity <= Inventory.LIMIT_GOLD) {
                    if (player.effectSkill.isChibi && player.typeChibi == 0) {
                        player.inventory.gold += item.quantity;
                    }
                    player.inventory.gold += item.quantity;
                    Service.gI().sendMoney(player);
                    return true;
                } else {
                    Service.gI().sendThongBao(player, "Vàng sau khi nhặt quá giới hạn cho phép");
                    return false;
                }
            case 10:
                long gem = (long) player.inventory.gem + (long) item.quantity;
                if (gem > Integer.MAX_VALUE) {
                    gem = Integer.MAX_VALUE;
                }
                player.inventory.gem = (int) gem;
                Service.gI().sendMoney(player);
                return true;
            case 34:
                long ruby = (long) player.inventory.ruby + (long) item.quantity;
                if (ruby > Integer.MAX_VALUE) {
                    ruby = Integer.MAX_VALUE;
                }
                player.inventory.ruby = (int) ruby;
                Service.gI().sendMoney(player);
                return true;
        }

        //mở rộng hành trang - rương đồ
        if (item.template.id == 517) {
            if (player.inventory.itemsBag.size() < Inventory.MAX_ITEMS_BAG) {
                player.inventory.itemsBag.add(ItemService.gI().createItemNull());
                Service.gI().sendThongBaoOK(player, "Hành trang của bạn đã được mở rộng thêm 1 ô");
                return true;
            } else {
                Service.gI().sendThongBaoOK(player, "Hành trang của bạn đã đạt tối đa");
                return false;
            }
        } else if (item.template.id == 518) {
            if (player.inventory.itemsBox.size() < Inventory.MAX_ITEMS_BOX) {
                player.inventory.itemsBox.add(ItemService.gI().createItemNull());
                Service.gI().sendThongBaoOK(player, "Rương đồ của bạn đã được mở rộng thêm 1 ô");
                return true;
            } else {
                Service.gI().sendThongBaoOK(player, "Rương đồ của bạn đã đạt tối đa");
                return false;
            }
        }
        boolean added = addItemList(player.inventory.itemsBag, item);
        if (added && item.template.id >= 1765 && item.template.id <= 1771
                && item.getOptionById(93) == null) {
            player.inventory.checkAndUpdateMeRongBadges(player);
            Service.gI().point(player);
        }
        return added;
    }

    public boolean addItemBox(Player player, Item item) {
        return addItemList(player.inventory.itemsBox, item);
    }

    /**
     * Thêm vật phẩm do admin tạo vào đầu hành trang để người chơi nhìn thấy ngay.
     * Các vật phẩm hiện có chỉ được dịch sang ô trống gần nhất, không bị ghi đè.
     */
    public boolean addAdminItemToFront(Player player, Item item) {
        if (item == null || !item.isNotNullItem() || item.quantity <= 0) {
            return false;
        }
        if (item.itemOptions.isEmpty()) {
            item.itemOptions.add(new Item.ItemOption(73, 0));
        }

        List<Item> bag = player.inventory.itemsBag;
        int emptyIndex = -1;
        for (int i = 0; i < bag.size(); i++) {
            if (!bag.get(i).isNotNullItem()) {
                emptyIndex = i;
                break;
            }
        }
        if (emptyIndex < 0) {
            return false;
        }

        for (int i = emptyIndex; i > 0; i--) {
            bag.set(i, bag.get(i - 1));
        }
        bag.set(0, ItemService.gI().copyItem(item));
        item.quantity = 0;
        return true;
    }

    /** Thêm toàn bộ số lượng item admin vào một ô trống cuối hành trang. */
    public boolean addAdminItemToBottom(Player player, Item item) {
        if (player == null || item == null || !item.isNotNullItem() || item.quantity <= 0) {
            return false;
        }
        if (item.itemOptions.isEmpty()) {
            item.itemOptions.add(new Item.ItemOption(73, 0));
        }
        List<Item> bag = player.inventory.itemsBag;
        for (int i = bag.size() - 1; i >= 0; i--) {
            Item current = bag.get(i);
            if (current == null || !current.isNotNullItem()) {
                bag.set(i, ItemService.gI().copyItem(item));
                item.quantity = 0;
                return true;
            }
        }
        return false;
    }

public int getOptionParamInBody(Player player, int optionId) {
    int total = 0;
    for (Item item : player.inventory.itemsBody) {
        if (item != null && item.isNotNullItem()) {
            for (Item.ItemOption op : item.itemOptions) {
                if (op.optionTemplate.id == optionId) {
                    total += op.param;
                }
            }
        }
    }
    return total;
}

    public boolean addItemList(List<Item> items, Item itemAdd) {
        if (itemAdd.itemOptions.isEmpty()) {
            itemAdd.itemOptions.add(new Item.ItemOption(73, 0));
        }

        // Vật phẩm sự kiện Cô Hồn luôn cộng dồn theo ID. Cháo nhặt từ quái
        // và nhang nhận từ NPC có thể mang danh sách option ẩn khác nhau,
        // khiến cơ chế so sánh option chung tạo ra nhiều ô riêng biệt.
        if (itemAdd.template.id == 2255 || itemAdd.template.id == 2256
                || itemAdd.template.id == 2257 || itemAdd.template.id == 2258) {
            Item stack = null;
            long total = itemAdd.quantity;
            for (int i = 0; i < items.size(); i++) {
                Item current = items.get(i);
                if (current.isNotNullItem() && current.template.id == itemAdd.template.id) {
                    total += current.quantity;
                    if (stack == null) {
                        stack = current;
                    } else {
                        items.set(i, ItemService.gI().createItemNull());
                    }
                }
            }
            if (stack != null) {
                stack.quantity = (int) Math.min(total, 99_999);
                itemAdd.quantity = 0;
                return true;
            }
        }

        int[] idParam = isItemIncrementalOption(itemAdd);
        if (idParam[0] != -1) {
            for (Item it : items) {
                if (it.isNotNullItem() && it.template.id == itemAdd.template.id) {
                    for (Item.ItemOption io : it.itemOptions) {
                        if (io.optionTemplate.id == idParam[0]) {
                            if (idParam[0] == 31 && (itemAdd.template.id == 933
                                    || itemAdd.template.id == 934)) {
                                io.param = (int) Math.min((long) Integer.MAX_VALUE,
                                        (long) io.param + idParam[1]);
                                it.quantity = Math.min(99_999, io.param);
                            } else {
                                io.param += idParam[1];
                            }
                            itemAdd.quantity = 0;
                            return true;
                        }
                    }
                }
            }
        }
        if (itemAdd.template.isUpToUp) {
            for (Item it : items) {
                if (!it.isNotNullItem() || it.template.id != itemAdd.template.id || (!checkListsEqual(it.itemOptions, itemAdd.itemOptions) && itemAdd.template.id != 2047 && itemAdd.template.id != 2074 && itemAdd.template.id != 1239 && itemAdd.template.id != 1240 && itemAdd.template.id != 73 && !itemAdd.isDaNangCap() && !itemAdd.isManhTS()) || it.quantity >= 100_000_000) {
                    continue;
                }

                //========================ITEM TĂNG SỐ LƯỢNG========================
                if ((itemAdd.template.id >= 1066 && itemAdd.template.id <= 1070) || itemAdd.template.id == 457
                        || itemAdd.template.id == 610 || itemAdd.template.type == 14 || itemAdd.template.id == 2048
                        || itemAdd.template.id > 2049 && itemAdd.template.id < 2056 || itemAdd.template.id == 821
                        || itemAdd.template.id == 2075 || itemAdd.template.id == 2047) {
                    it.quantity += itemAdd.quantity;
                    itemAdd.quantity = 0;
                    return true;
                }

                if (it.quantity < 99999) {
                    int add = 99999 - it.quantity;
                    if (itemAdd.quantity <= add) {
                        it.quantity += itemAdd.quantity;
                        itemAdd.quantity = 0;
                        return true;
                    } else {
                        it.quantity = 99999;
                        itemAdd.quantity -= add;
                    }
                }
            }
        }

        //add item vào ô mới
        if (itemAdd.quantity > 0) {
            for (int i = 0; i < items.size(); i++) {
                if (!items.get(i).isNotNullItem()) {
                    items.set(i, ItemService.gI().copyItem(itemAdd));
                    itemAdd.quantity = 0;
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean checkListsEqual(List<ItemOption> list1, List<ItemOption> list2) {
        if (list1.size() != list2.size()) {
            return false;
        }

        for (int i = 0; i < list1.size(); i++) {
            if (list1.get(i).optionTemplate.id != list2.get(i).optionTemplate.id || list1.get(i).param != list2.get(i).param) {
                return false;
            }
        }

        return true;
    }

    private void __________________Kiểm_tra_điều_kiện_vật_phẩm______________() {
        //**********************************************************************
    }

    /**
     * Kiểm tra vật phẩm có phải là vật phẩm tăng chỉ số option hay không
     *
     * @param item
     * @return id option tăng chỉ số - param
     */
    private int[] isItemIncrementalOption(Item item) {
        for (Item.ItemOption io : item.itemOptions) {
            if (io != null && io.optionTemplate != null) {
                switch (io.optionTemplate.id) {
                    case 1:
                        return new int[]{io.optionTemplate.id, io.param};
                    case 31:
                        return new int[]{io.optionTemplate.id, io.param};
                }
            }
        }
        return new int[]{-1, -1};
    }

    private void __________________Kiểm_tra_danh_sách_còn_chỗ_trống_________() {
        //**********************************************************************
    }

    public byte getCountEmptyBag(Player player) {
        return getCountEmptyListItem(player.inventory.itemsBag);
    }

    public byte getCountEmptyListItem(List<Item> list) {
        byte count = 0;
        for (Item item : list) {
            if (!item.isNotNullItem()) {
                count++;
            }
        }
        return count;
    }

    public byte getIndexBag(Player pl, Item it) {
        for (byte i = 0; i < pl.inventory.itemsBag.size(); ++i) {
            Item item = pl.inventory.itemsBag.get(i);
            if (item != null && it.equals(item)) {
                return i;
            }
        }
        return -1;
    }

    public boolean finditemWoodChest(Player player) {
        for (Item item : player.inventory.itemsBag) {
            if (item.isNotNullItem() && item.template.id == 570) {
                return false;
            }
        }
        for (Item item : player.inventory.itemsBox) {
            if (item.isNotNullItem() && item.template.id == 570) {
                return false;
            }
        }
        return true;
    }

    public int getParam(Player player, int idoption, int itemID) {
        if (idoption == 31 && (itemID == 933 || itemID == 934)) {
            long total = 0;
            for (Item item : player.inventory.itemsBag) {
                if (item != null && item.isNotNullItem() && item.template.id == itemID) {
                    ItemOption count = item.getOptionById(31);
                    total += count != null ? Math.max(0, count.param) : Math.max(0, item.quantity);
                }
            }
            return (int) Math.min(total, Integer.MAX_VALUE);
        }
        for (Item it : player.inventory.itemsBag) {
            if (it != null && it.itemOptions != null && it.isNotNullItem() && it.template.id == itemID) {
                for (ItemOption iop : it.itemOptions) {
                    if (iop.optionTemplate.id == idoption) {
                        return iop.param;
                    }
                }
            }
        }
        return 0;
    }

    public void subParamItemsBag(Player player, int itemID, int idoption, int param) {
        if (idoption == 31 && (itemID == 933 || itemID == 934)) {
            int remaining = param;
            for (int index = 0; index < player.inventory.itemsBag.size() && remaining > 0; index++) {
                Item item = player.inventory.itemsBag.get(index);
                if (item == null || !item.isNotNullItem() || item.template.id != itemID) continue;
                ItemOption count = item.getOptionById(31);
                int available = count != null ? Math.max(0, count.param) : Math.max(0, item.quantity);
                int used = Math.min(remaining, available);
                if (used == 0) continue;
                if (count != null) {
                    count.param -= used;
                    if (item.quantity == available) item.quantity -= used;
                    if (count.param == 0) removeItem(player.inventory.itemsBag, index);
                } else {
                    item.quantity -= used;
                    if (item.quantity == 0) removeItem(player.inventory.itemsBag, index);
                }
                remaining -= used;
            }
            return;
        }
        Item itemRemove = null;
        for (Item it : player.inventory.itemsBag) {
            if (it != null && it.template.id == itemID) {
                for (ItemOption op : it.itemOptions) {
                    if (op != null && op.optionTemplate.id == idoption) {
                        op.param -= param;
                        if (op.param <= 0) {
                            itemRemove = it;
                        }
                        break;
                    }
                }
                break;
            }
        }
        if (itemRemove != null) {
            removeItem(player.inventory.itemsBag, itemRemove);
        }
    }

    public boolean findItemBongTaiCap2(Player player) {
        for (Item item : player.inventory.itemsBag) {
            if (item.isNotNullItem() && item.template.id == 921) {
                return true;
            }
        }
        for (Item item : player.inventory.itemsBox) {
            if (item.isNotNullItem() && item.template.id == 921) {
                return true;
            }
        }
        return false;
    }
    public boolean findItemBongTaiCap3(Player player) {
        for (Item item : player.inventory.itemsBag) {
            if (item.isNotNullItem() && item.template.id == 1819) {
                return true;
            }
        }
        for (Item item : player.inventory.itemsBox) {
            if (item.isNotNullItem() && item.template.id == 1819) {
                return true;
            }
        }
        return false;
    }

    public boolean findItemBongTai(Player player) {
        for (Item item : player.inventory.itemsBag) {
            if (item.isNotNullItem() && (item.template.id == 921 || item.template.id == 454)) {
                return true;
            }
        }
        for (Item item : player.inventory.itemsBox) {
            if (item.isNotNullItem() && (item.template.id == 921 || item.template.id == 454)) {
                return true;
            }
        }
        return false;
    }

    public boolean findItemSkinQuyLaoKame(Player player) {
        for (Item item : player.inventory.itemsBody) {
            if (item.isNotNullItem() && item.template.id == 710) {
                return true;
            }
        }
        for (Item item : player.inventory.itemsBag) {
            if (item.isNotNullItem() && item.template.id == 710) {
                return true;
            }
        }
        for (Item item : player.inventory.itemsBox) {
            if (item.isNotNullItem() && item.template.id == 710) {
                return true;
            }
        }
        return false;
    }

    public boolean findItemNTK(Player player) {
        if (player.isPl()) {
            for (Item item : player.inventory.itemsBag) {
                if (item.isNotNullItem() && item.template.id == 992) {
                    return true;
                }
            }
            for (Item item : player.inventory.itemsBox) {
                if (item.isNotNullItem() && item.template.id == 992) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean findItemTVC(Player player) {
        if (player.isPl()) {
            for (Item item : player.inventory.itemsBag) {
                if (item.isNotNullItem() && item.template.id == 2077) {
                    return true;
                }
            }
            for (Item item : player.inventory.itemsBox) {
                if (item.isNotNullItem() && item.template.id == 2077) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean findItemTatVoGiangSinh(Player player) {
        for (Item item : player.inventory.itemsBag) {
            if (item.isNotNullItem() && item.template.id == 649) {
                return true;
            }
        }
        return false;
    }

    public boolean findSenzu(Player player) {
        for (Item item : player.inventory.itemsBag) {
            if (item.isNotNullItem() && item.template.type == 6) {
                return true;
            }
        }
        return false;
    }

    public boolean fullSetThan(Player player) {
        for (int i = 0; i < 5; i++) {
            Item item = player.inventory.itemsBody.get(i);
            if (item == null || item.template == null || item.template.level != 13) {
                return false;
            }
        }
        return true;
    }

    public boolean x99ThucAn(Player player) {
        Item doAn = player.inventory.itemsBag.stream().filter(it -> it != null && it.template != null && (it.template.id == 663 || it.template.id == 664 || it.template.id == 665 || it.template.id == 666 || it.template.id == 667) && it.quantity >= 99).findFirst().orElse(null);
        return doAn != null;
    }

    public boolean canOpenBillShop(Player player) {
        return fullSetThan(player) && x99ThucAn(player);
    }

    public boolean optionCanUpgrade(int id) {
        return id == 0 || id == 22 || id == 23 || id == 14 || id == 27 || id == 28 || id == 47;
    }

    public int getIndexItem(Player player, List<Item> items, Item item) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) == item) {
                return i;
            }
        }
        return -1;
    }

    public int getIndexItemBag(Player player, Item item) {
        return getIndexItem(player, player.inventory.itemsBag, item);
    }

    public int getIndexItemBody(Player player, Item item) {
        return getIndexItem(player, player.inventory.itemsBody, item);
    }

    public int getIndexItemBox(Player player, Item item) {
        return getIndexItem(player, player.inventory.itemsBox, item);
    }

    private void handleOption210(Item item) {
        for (int i = 0; i < item.itemOptions.size(); i++) {
            Item.ItemOption io = item.itemOptions.get(i);
            if (io.optionTemplate.id == 210) {
                int option210Index = i;
                item.itemOptions.remove(i);
                int numberOfOptionsToAdd = io.param;
                int[] allOptions = {8, 14, 108, 94, 108, 16, 80, 81, 97, 100, 101, 104, 106};
                List<Integer> selectedOptions = new ArrayList<>();
                while (selectedOptions.size() < numberOfOptionsToAdd && selectedOptions.size() < allOptions.length) {
                    int randomIndex = (int) (Math.random() * allOptions.length);
                    int selectedOption = allOptions[randomIndex];
                    if (!selectedOptions.contains(selectedOption)) {
                        selectedOptions.add(selectedOption);
                    }
                }
                List<Item.ItemOption> newOptions = new ArrayList<>();
                for (int option : selectedOptions) {
                    int newParam = 0;
                    if (option == 8 || option == 14 || option == 108 || option == 94 || option == 108) {
                        newParam = 3 + (int) (Math.random() * 3);
                    } else if (option == 16 || option == 80 || option == 81 || option == 97 || option == 100 || option == 101 || option == 104) {
                        newParam = 10 + (int) (Math.random() * 16);
                    } else if (option == 106) {
                        newParam = 0;
                    }
                    newOptions.add(new Item.ItemOption(option, newParam));
                }
                item.itemOptions.addAll(option210Index, newOptions);
                break;
            }
        }
    }

    public boolean findItem(Player player, int id) {
        for (Item item : player.inventory.itemsBag) {
            if (item.isNotNullItem() && item.template.id == id) {
                return true;
            }
        }
        for (Item item : player.inventory.itemsBox) {
            if (item.isNotNullItem() && item.template.id == id) {
                return true;
            }
        }
        return false;
    }

    private void checkOption231(Item item) {
        for (int i = 0; i < item.itemOptions.size(); i++) {
            Item.ItemOption io = item.itemOptions.get(i);
            if (io.optionTemplate.id == 231) {
                item.itemOptions.remove(i);
                double randomValue = Math.random();
                if (randomValue <= 0.99) {
                    int[] validParams = {3, 7, 15, 21};
                    int selectedParam = validParams[(int) (Math.random() * validParams.length)];
                    item.itemOptions.add(new Item.ItemOption(93, selectedParam));
                }
                break;
            }
        }
    }

    public void updateTitleFromBody(Player player) {
        if (player.inventory.itemsBody.size() > 13) {
            Item titleItem = player.inventory.itemsBody.get(13);
            int idEffect = -1;
            if (titleItem != null && titleItem.isNotNullItem() && titleItem.template.type == 36) {
                idEffect = nro.models.player_badges.BagesTemplate.fineIdEffectbyIdItem(titleItem.template.id);
            }
            if (player.badges.idBadges != idEffect) {
                if (idEffect != -1) {
                    nro.models.player_badges.BadgesData owned = null;
                    if (player.dataBadges != null) {
                        for (nro.models.player_badges.BadgesData bg : player.dataBadges) {
                            if (bg.idBadGes == idEffect) {
                                owned = bg;
                                break;
                            }
                        }
                    }
                    if (owned == null) {
                        owned = new nro.models.player_badges.BadgesData(player, idEffect, 30);
                    }
                    nro.models.player_badges.BadgesService.turnOnBadges(player, idEffect);
                    player.badges.idBadges = idEffect;
                    player.badges.lastTimeSendBadges = 0;
                    Service.gI().sendBadgesPlayer(player, 12, idEffect);
                } else {
                    nro.models.player_badges.BadgesService.turnOnBadges(player, -1);
                    player.badges.idBadges = -1;
                    player.badges.lastTimeSendBadges = 0;
                    Service.gI().sendBadgesPlayer(player, 12, -1);
                }
                player.nPoint.calPoint();
                Service.gI().point(player);
                Service.gI().Send_Info_NV(player);
            }
        }
    }

    public void sendItemRuongDanhHieu(Player player) {
        while (player.inventory.itemsRuongDanhHieu.size() < player.inventory.sizeRuongDanhHieu) {
            player.inventory.itemsRuongDanhHieu.add(ItemService.gI().createItemNull());
        }
        Message msg;
        try {
            msg = new Message(-35);
            msg.writer().writeByte(0);
            msg.writer().writeByte(player.inventory.itemsRuongDanhHieu.size());
            for (Item it : player.inventory.itemsRuongDanhHieu) {
                msg.writer().writeShort(it.isNotNullItem() ? it.template.id : -1);
                if (it.isNotNullItem()) {
                    msg.writer().writeInt(it.quantity);
                    msg.writer().writeUTF(it.getInfo());
                    msg.writer().writeUTF(it.getContent());
                    msg.writer().writeByte(it.itemOptions.size());
                    for (Item.ItemOption io : it.itemOptions) {
                        if (io.optionTemplate.id == 213) {
                            int opId = 213;
                            int param = io.param;
                            if (param > 1_000_000) {
                                opId = 223;
                                param /= 1_000_000;
                            } else if (param > 1000) {
                                opId = 222;
                                param /= 1000;
                            }
                            msg.writer().writeByte(opId);
                            msg.writer().writeShort(param);
                        } else {
                            msg.writer().writeByte(io.optionTemplate.id);
                            msg.writer().writeShort(SpiritGiftService.clientOptionParam(it, io));
                        }
                    }
                }
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
        }
        this.openBox(player);
    }
}
