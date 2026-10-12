package nro.models.player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import nro.models.consts.ConstTaskBadges;
import nro.models.item.Item;
import nro.models.item.Item.ItemOption;
import nro.models.task.BadgesTaskService;

/**
 *
 * @author By Mr Blue
 *
 */
public class Inventory {

    public static final long LIMIT_GOLD = 200_000_000_000L;
    public static final int MAX_ITEMS_BAG = 80;
    public static final int MAX_ITEMS_BOX = 100;
    public Item trainArmor;
    public Item chibi;
    public List<String> giftCode;
    public List<Item> itemsBody;
    public List<Item> itemsBag;
    public List<Item> itemsBox;
    public List<Item> itemsRecycle;

    public List<Item> itemsBoxCrackBall;
    public List<Item> itemsDaBan;
    public List<Item> itemsRuongSuuTam;
    public int sizeRuongSuuTam;
    public List<Item> itemsRuongDanhHieu;
    public int sizeRuongDanhHieu;

    public long gold;
    // Persisted together with currencies in data_inventory[5].
    public org.json.simple.JSONObject gameData = new org.json.simple.JSONObject();
    public boolean gameDataPersistenceBlocked;
    public long lastFishingActionAt;
    public int gem;
    public int ruby;
    public int coupon;
    public int event;
    public Iterable<Item> items;

    public Inventory() {
        itemsBody = new ArrayList<>();
        itemsBag = new ArrayList<>();
        itemsBox = new ArrayList<>();
        itemsRecycle = new ArrayList<>();
        itemsBoxCrackBall = new ArrayList<>();
        itemsDaBan = new ArrayList<>();
        itemsRuongSuuTam = new ArrayList<>();
        sizeRuongSuuTam = 30;
        itemsRuongDanhHieu = new ArrayList<>();
        sizeRuongDanhHieu = 30;
        giftCode = new ArrayList<>();
    }

    public int getGem() {
        return this.gem;
    }

    public int getParam(Item it, int id) {
        for (ItemOption op : it.itemOptions) {
            if (op != null && op.optionTemplate.id == id) {
                return op.param;
            }
        }
        return 0;
    }

    public boolean haveOption(List<Item> l, int index, int id) {
        Item it = l.get(index);
        if (it != null && it.isNotNullItem()) {
            return it.itemOptions.stream().anyMatch(op -> op != null && op.optionTemplate.id == id);
        }
        return false;
    }

    public void subGem(int num) {
        this.gem -= num;
    }

    public void subGold(int num) {
        this.gold -= num;
    }

    public void addGold(int gold) {
        this.gold += gold;
        if (this.gold > LIMIT_GOLD) {
            this.gold = LIMIT_GOLD;
        }
    }

    public void dispose() {
        if (this.trainArmor != null) {
            this.trainArmor.dispose();
        }
        this.trainArmor = null;
        if (this.itemsBody != null) {
            for (Item it : this.itemsBody) {
                it.dispose();
            }
            this.itemsBody.clear();
        }
        if (this.itemsBag != null) {
            for (Item it : this.itemsBag) {
                it.dispose();
            }
            this.itemsBag.clear();
        }
        if (this.itemsBox != null) {
            for (Item it : this.itemsBox) {
                it.dispose();
            }
            this.itemsBox.clear();
        }
        if (this.itemsRecycle != null) {
            for (Item it : this.itemsRecycle) {
                it.dispose();
            }
            this.itemsRecycle.clear();
        }
        if (this.itemsBoxCrackBall != null) {
            for (Item it : this.itemsBoxCrackBall) {
                it.dispose();
            }
            this.itemsBoxCrackBall.clear();
        }
        if (this.itemsDaBan != null) {
            for (Item it : this.itemsDaBan) {
                it.dispose();
            }
            this.itemsDaBan.clear();
        }
        if (this.itemsRuongSuuTam != null) {
            for (Item it : this.itemsRuongSuuTam) {
                it.dispose();
            }
            this.itemsRuongSuuTam.clear();
        }
        if (this.itemsRuongDanhHieu != null) {
            for (Item it : this.itemsRuongDanhHieu) {
                it.dispose();
            }
            this.itemsRuongDanhHieu.clear();
        }
        this.itemsBody = null;
        this.itemsBag = null;
        this.itemsBox = null;
        this.itemsRecycle = null;
        this.itemsBoxCrackBall = null;
        this.itemsDaBan = null;
        this.itemsRuongSuuTam = null;
        this.itemsRuongDanhHieu = null;
    }

    public void checkAndUpdateMeRongBadges(Player player) {
        BadgesTaskService.setCountBagesTask(
                player,
                ConstTaskBadges.ME_RONG,
                getPermanentBabyDragonIds().size()
        );
    }

    /**
     * Trả về các loại Rồng Nhí vĩnh viễn người chơi đang sở hữu. Một loại chỉ
     * được tính một lần, kể cả khi có nhiều vật phẩm trùng ID.
     */
    public Set<Integer> getPermanentBabyDragonIds() {
        Set<Integer> dragonIds = new HashSet<>();
        List<List<Item>> inventories = Arrays.asList(
                this.itemsBag,
                this.itemsBox,
                this.itemsBody,
                this.itemsRuongSuuTam
        );

        for (List<Item> inventory : inventories) {
            if (inventory == null) {
                continue;
            }
            for (Item item : inventory) {
                if (item == null || item.template == null || !item.isNotNullItem() || !isPermanent(item)) {
                    continue;
                }
                int itemId = item.template.id;
                if (itemId >= 1765 && itemId <= 1771) {
                    dragonIds.add(itemId);
                }
            }
        }
        return dragonIds;
    }

    /**
     * Trả về các ID trong bộ sưu tầm mà người chơi đang sở hữu vĩnh viễn.
     * Dùng cho các bộ pet/thú cưỡi để không tính nhầm vật phẩm có hạn sử dụng.
     */
    public Set<Integer> getPermanentItemTemplateIds(int[] templateIds) {
        Set<Integer> requestedIds = new HashSet<>();
        if (templateIds != null) {
            for (int templateId : templateIds) {
                requestedIds.add(templateId);
            }
        }
        Set<Integer> ownedIds = new HashSet<>();
        if (requestedIds.isEmpty()) {
            return ownedIds;
        }
        List<List<Item>> inventories = Arrays.asList(
                this.itemsBag,
                this.itemsBox,
                this.itemsBody,
                this.itemsRuongSuuTam
        );
        for (List<Item> inventory : inventories) {
            if (inventory == null) {
                continue;
            }
            for (Item item : inventory) {
                if (item == null || item.template == null || !item.isNotNullItem() || !isPermanent(item)) {
                    continue;
                }
                if (requestedIds.contains((int) item.template.id)) {
                    ownedIds.add((int) item.template.id);
                }
            }
        }
        return ownedIds;
    }

    /** Số vật phẩm thực tế đang đặt trong Rương Sưu Tầm. */
    public int getCollectionChestItemCount() {
        int count = 0;
        if (this.itemsRuongSuuTam == null) {
            return count;
        }
        for (Item item : this.itemsRuongSuuTam) {
            if (item != null && item.isNotNullItem()) {
                count++;
            }
        }
        return count;
    }

    private boolean isPermanent(Item item) {
        return item != null && item.getOptionById(93) == null;
    }
}
