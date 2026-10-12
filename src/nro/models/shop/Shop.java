package nro.models.shop;

import java.util.ArrayList;
import java.util.List;
import nro.models.player.Player;
import nro.models.services.HeartsRebirth;
import nro.models.server.Manager;
import nro.models.shop.TabShopHocKynang;
import nro.models.shop.TabShopSoHuu;

/**
 *
 * @author By Mr Blue
 * 
 */

public class Shop {

    public int id;

    public byte npcId;

    public List<TabShop> tabShops;

    public String tagName;

    public byte typeShop;

    public Shop() {
        this.tabShops = new ArrayList<>();
    }

    public Shop(Shop shop, Player player) {
        this.tabShops = new ArrayList<>();
        this.id = shop.id;
        this.npcId = shop.npcId;
        this.tagName = shop.tagName;
        this.typeShop = shop.typeShop;
        TabShop heartBooks = null;
        for (TabShop tabShop : shop.tabShops) {
            if ("URON".equals(tagName) && tabShop.id == 70) {
                if (HeartsRebirth.isReborn(player)) heartBooks = tabShop;
                continue;
            } else if (tabShop.id >= 10 && tabShop.id <= 12) {
                this.tabShops.add(new TabShopUron(tabShop, player));
            } else if (tabShop.id == 17) {
                this.tabShops.add(new TabShopSanta(tabShop, player));
            } else if (tabShop.id == 19) {
                    this.tabShops.add(new TabShopMuaAvatar(tabShop, player));
            } else if (tabShop.id == 13) {
               
                this.tabShops.add(new TabShopHangDoc(tabShop, player));
            } else if (tabShop.id == 44) {
                this.tabShops.add(new TabShopDanhHieu(tabShop, player));
            } else if (tabShop.id == 45) {
                this.tabShops.add(new TabShopSoHuu(tabShop, player));
            } else if (tabShop.id >= 41 && tabShop.id <= 43) {
                this.tabShops.add(new TabShopHocKynang(tabShop, player));
            } else {
                this.tabShops.add(new TabShop(tabShop));
            }
        }
        addSpiritProducts(heartBooks);
    }

    public Shop(Shop shop) {
        this.tabShops = new ArrayList<>();
        this.id = shop.id;
        this.npcId = shop.npcId;
        this.tagName = shop.tagName;
        this.typeShop = shop.typeShop;
        TabShop heartBooks = null;
        for (TabShop tabShop : shop.tabShops) {
            if ("URON".equals(tagName) && tabShop.id == 70) {
                heartBooks = tabShop;
                continue;
            }
            this.tabShops.add(new TabShop(tabShop));
        }
        addSpiritProducts(heartBooks);
    }

    private void addSpiritProducts(TabShop heartBooks) {
        if (!"URON".equals(tagName)) return;
        TabShop tab = new TabShop();
        tab.shop = this;
        tab.id = 21342;
        tab.name = "Tinh Linh";
        int[] ids = {2380, 2381, 2382};
        int[] prices = {100_000, 500_000, 10_000};
        for (int i = 0; i < ids.length; i++) {
            if (ids[i] >= Manager.ITEM_TEMPLATES.size()) continue;
            ItemShop item = new ItemShop();
            item.tabShop = tab;
            item.temp = Manager.ITEM_TEMPLATES.get(ids[i]);
            item.id = ids[i];
            item.typeSell = 0;
            item.cost = prices[i];
            tab.itemShops.add(item);
        }
        int[] skillPrices = {100, 200, 300};
        for (int slot = 2; slot <= 4; slot++) {
            int id = 2454 + slot;
            if (id >= Manager.ITEM_TEMPLATES.size()) continue;
            ItemShop offer = new ItemShop();
            offer.tabShop = tab;
            offer.temp = Manager.ITEM_TEMPLATES.get(id);
            offer.id = id;
            offer.typeSell = 1;
            offer.cost = skillPrices[slot - 2];
            tab.itemShops.add(offer);
        }
        if (!tab.itemShops.isEmpty()) tabShops.add(tab);
        TabShop charmTab = new TabShop();
        charmTab.shop = this;
        charmTab.id = 21343;
        charmTab.name = "Bùa/Heart";
        int[][] charmPrices = {{9, 13}, {200, 300}, {6000, 9000}};
        for (int period = 0; period < charmPrices.length; period++) {
            for (int power = 0; power < 2; power++) {
                int id = 2450 + period * 2 + power;
                if (id >= Manager.ITEM_TEMPLATES.size()) continue;
                ItemShop offer = new ItemShop();
                offer.tabShop = charmTab;
                offer.temp = Manager.ITEM_TEMPLATES.get(id);
                offer.id = id;
                offer.typeSell = 1;
                offer.cost = charmPrices[period][power];
                charmTab.itemShops.add(offer);
            }
        }
        if (heartBooks != null) {
            for (ItemShop book : heartBooks.itemShops) {
                ItemShop offer = new ItemShop(book);
                offer.tabShop = charmTab;
                charmTab.itemShops.add(offer);
            }
        }
        if (!charmTab.itemShops.isEmpty()) tabShops.add(charmTab);
    }

    public ItemShop getItemShop(int temp) {
        for (TabShop tab : this.tabShops) {
            for (ItemShop is : tab.itemShops) {
                if (is.temp.id == temp) {
                    return is;
                }
            }
        }
        return null;
    }

    public void dispose() {
        if (this.tabShops != null) {
            for (TabShop ts : this.tabShops) {
                ts.dispose();
            }
            this.tabShops.clear();
        }
        this.tabShops = null;
    }

}
