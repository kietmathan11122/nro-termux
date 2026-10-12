package nro.models.Bot;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import nro.models.item.Item;
import nro.models.map.Zone;
import nro.models.map.service.ChangeMapService;
import nro.models.map.service.MapService;
import nro.models.player.Player;
import nro.models.services.PlayerService;
import nro.models.services.ChatGlobalService;
import nro.models.services.ItemService;
import nro.models.services.InventoryService;
import nro.models.services.Service;
import nro.models.services_func.Trade;
import nro.models.services_func.TransactionService;
import nro.models.utils.Util;

public class BotGiaoDich {
    public static final long GOLD_BAR_VALUE = 37_000_000L; // Bán thỏi vàng được 37 triệu vàng
    private static final long GLOBAL_CHAT_INTERVAL = 5 * 60 * 1000L;
    private static final long LOCAL_CHAT_GROUP_DELAY = 3 * 1000L;
    private static final long LOCAL_CHAT_CYCLE = 9 * 1000L;

    public int idItem;
    public int idItTd;
    public int slot;
    public int priceGold; // Giá bán tính bằng vàng

    // Bot 6: Thu mua đồ Thần Linh
    public boolean isBuyer = false;
    public long buyPricePerItem = 75_000_000L;

    // Bot 7: Mua và Bán Ngọc Rồng 3 sao
    public boolean isNR3Trader = false;

    // Bot 8, 9, 10: Multi-item map (Item ID -> Giá vàng)
    public Map<Integer, Integer> multiPrices = new LinkedHashMap<>();
    public String customChat = null;

    public int targetMapId = 44; // Vách núi Kakarot
    public int targetZoneId = 0; // Khu 0
    public int targetX = 200;
    public int targetY = 408;

    private long nextGlobalChatTime;
    private long nextLocalChatTime;
    private Trade trade;

    public Bot bot;
    private Player pl;

    public BotGiaoDich(int item, int traodoi, int slot) {
        this.idItem = item;
        this.idItTd = traodoi;
        this.slot = slot;
        this.priceGold = 0;
        this.isBuyer = false;
    }

    public BotGiaoDich(int item, int priceGold) {
        this.idItem = item;
        this.idItTd = -1;
        this.slot = 1;
        this.priceGold = priceGold;
        this.isBuyer = false;
    }

    // Factory Bot 6: Thu mua đồ Thần Linh
    public static BotGiaoDich createBuyerBot(long pricePerItem) {
        BotGiaoDich shop = new BotGiaoDich(-1, 0);
        shop.isBuyer = true;
        shop.buyPricePerItem = pricePerItem;
        return shop;
    }

    // Factory Bot 7: Mua & Bán Ngọc Rồng 3 sao
    public static BotGiaoDich createNR3Trader() {
        BotGiaoDich shop = new BotGiaoDich(16, 120_000_000); // Bán 120tr
        shop.isNR3Trader = true;
        shop.buyPricePerItem = 100_000_000L; // Mua 100tr
        return shop;
    }

    // Factory Bot bán bổ trợ (Cuồng nộ, Bổ khí, Bổ huyết, Giáp xên)
    public static BotGiaoDich createBoTroShop(int primaryItem, int primaryPrice, String chat) {
        BotGiaoDich shop = new BotGiaoDich(primaryItem, primaryPrice);
        shop.customChat = chat;
        shop.multiPrices.put(primaryItem, primaryPrice);
        return shop;
    }

    public BotGiaoDich(BotGiaoDich shop) {
        this.idItem = shop.idItem;
        this.idItTd = shop.idItTd;
        this.slot = shop.slot;
        this.priceGold = shop.priceGold;
        this.isBuyer = shop.isBuyer;
        this.buyPricePerItem = shop.buyPricePerItem;
        this.isNR3Trader = shop.isNR3Trader;
        this.customChat = shop.customChat;
        this.multiPrices.putAll(shop.multiPrices);
        this.targetMapId = shop.targetMapId;
        this.targetZoneId = shop.targetZoneId;
        this.targetX = shop.targetX;
        this.targetY = shop.targetY;
    }

    public void update() {
        this.mapL();
        this.chat();
        this.buyGoldBarsIfGoldFull();
    }

    public String getChat() {
        String mapName = (this.bot != null && this.bot.zone != null && this.bot.zone.map != null)
                ? this.bot.zone.map.mapName : "Vách Kakarot";
        int zoneId = (this.bot != null && this.bot.zone != null) ? this.bot.zone.zoneId : this.targetZoneId;

        if (this.customChat != null) {
            return String.format("%s tại %s k%d", this.customChat, mapName, zoneId);
        }

        if (this.isNR3Trader) {
            return String.format("Mua NR 3 sao 100tr, Bán NR 3 sao 120tr tại %s k%d", mapName, zoneId);
        }

        if (this.isBuyer) {
            return String.format("Thu mua đồ Thần Linh giá %s vàng / 1 món tại %s k%d",
                    Util.numberToMoney(this.buyPricePerItem), mapName, zoneId);
        }

        Item it = ItemService.gI().createNewItem((short) this.idItem);
        String itemName = (it != null && it.template != null) ? it.template.name : ("Item " + this.idItem);

        if (this.priceGold > 0) {
            return String.format("Bán %s giá %s vàng / 1 viên tại %s k%d",
                    itemName, Util.numberToMoney(this.priceGold), mapName, zoneId);
        } else {
            Item it1 = ItemService.gI().createNewItem((short) this.idItTd);
            String it1Name = (it1 != null && it1.template != null) ? it1.template.name : ("Item " + this.idItTd);
            return String.format("Bán %s giá x%d %s / 1 viên tại %s k%d",
                    itemName, this.slot, it1Name, mapName, zoneId);
        }
    }

    public void chat() {
        long now = System.currentTimeMillis();
        if (this.nextGlobalChatTime == 0) {
            this.nextGlobalChatTime = now + GLOBAL_CHAT_INTERVAL;
        }
        if (this.nextLocalChatTime == 0) {
            // Chia 10 bot thành 3 lượt, cách nhau 3 giây để không bật bong bóng cùng lúc.
            this.nextLocalChatTime = now + getLocalChatGroup() * LOCAL_CHAT_GROUP_DELAY;
        }
        if (now >= this.nextGlobalChatTime) {
            ChatGlobalService.gI().chat1(this.bot, this.getChat());
            this.nextGlobalChatTime = now + GLOBAL_CHAT_INTERVAL;
        }
        if (now >= this.nextLocalChatTime) {
            Service.gI().chat(this.bot, getChat());
            this.nextLocalChatTime = now + LOCAL_CHAT_CYCLE;
        }
    }

    private int getLocalChatGroup() {
        if (this.bot == null || this.bot.name == null) {
            return 0;
        }
        String name = this.bot.name;
        int separator = name.lastIndexOf('_');
        if (separator >= 0 && separator + 1 < name.length()) {
            try {
                int number = Integer.parseInt(name.substring(separator + 1));
                if (number == 1 || number == 3 || number == 7 || number == 10) {
                    return 0;
                }
                if (number == 2 || number == 4 || number == 6 || number == 8) {
                    return 1;
                }
                return 2; // bot 5 và bot 9
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    public synchronized void activeTraDe(Player pl) {
        if (pl == null || this.bot == null) {
            return;
        }
        if (TransactionService.gI().check(pl)) {
            Service.gI().sendThongBao(pl, "Bạn đang có một phiên giao dịch khác.");
            return;
        }
        if (TransactionService.gI().check(this.bot)) {
            Service.gI().sendThongBao(pl, "Bot đang giao dịch với người khác, vui lòng thử lại sau.");
            return;
        }
        trade = new Trade(pl, bot);
        this.pl = pl;
        this.trade.openTabTrade();
        Service.gI().sendThongBao(pl, "Đặt vàng hoặc vật phẩm vào ô giao dịch rồi bấm Khóa. Bot sẽ tự phản hồi.");
    }

    public synchronized void CheckTraDe(List<Item> item) {
        if (this.trade == null) {
            if (this.pl != null) {
                Service.gI().sendThongBao(this.pl, "Phiên giao dịch đã hết hạn, vui lòng giao dịch lại với bot.");
            }
            return;
        }

        // 1. BOT SỐ 6: THU MUA ĐỒ THẦN LINH
        if (this.isBuyer) {
            int countThanLinh = 0;
            if (item != null) {
                for (Item it : item) {
                    if (it != null && it.isNotNullItem() && it.template != null) {
                        int tId = it.template.id;
                        if (tId >= 555 && tId <= 567) {
                            countThanLinh += (it.quantity > 0 ? it.quantity : 1);
                        }
                    }
                }
            }

            if (countThanLinh <= 0) {
                this.trade.cancelTrade();
                if (this.pl != null) {
                    Service.gI().sendThongBao(this.pl, "Bot chỉ thu mua Đồ Thần Linh (giá 75tr vàng/món)!");
                }
                return;
            }

            long totalGold = (long) countThanLinh * this.buyPricePerItem;
            if (totalGold > Trade.MAX_GOLD_TRADE_PER_TIME) {
                this.trade.cancelTrade();
                if (this.pl != null) {
                    Service.gI().sendThongBao(this.pl, "Chỉ được giao dịch tối đa " + (Trade.MAX_GOLD_TRADE_PER_TIME / this.buyPricePerItem) + " món đồ Thần một lần!");
                }
                return;
            }

            sellGoldBarsIfNeeded(totalGold);
            this.trade.setGoldTrade2((int) totalGold);
            this.trade.lockTran(this.bot);
            this.trade.acceptTrade();
            return;
        }

        // 2. BOT SỐ 7: MUA & BÁN NGỌC RỒNG 3 SAO
        if (this.isNR3Trader) {
            int nr3Count = 0;
            if (item != null) {
                for (Item it : item) {
                    if (it != null && it.isNotNullItem() && it.template != null && it.template.id == 16) {
                        nr3Count += (it.quantity > 0 ? it.quantity : 1);
                    }
                }
            }

            // Người chơi BÁN NR 3 sao cho Bot
            if (nr3Count > 0) {
                long totalGold = (long) nr3Count * this.buyPricePerItem; // 100tr / viên
                if (totalGold > Trade.MAX_GOLD_TRADE_PER_TIME) {
                    this.trade.cancelTrade();
                    if (this.pl != null) {
                        Service.gI().sendThongBao(this.pl, "Chỉ được bán tối đa " + (Trade.MAX_GOLD_TRADE_PER_TIME / this.buyPricePerItem) + " viên NR 3 sao một lần!");
                    }
                    return;
                }
                sellGoldBarsIfNeeded(totalGold);
                this.trade.setGoldTrade2((int) totalGold);
                this.trade.lockTran(this.bot);
                this.trade.acceptTrade();
                return;
            }

            // Người chơi MUA NR 3 sao từ Bot (120tr / viên)
            int goldInTrade = this.trade.getGoldTrade1();
            if (goldInTrade >= this.priceGold) {
                int slNR3 = goldInTrade / this.priceGold;
                activeItem(16, slNR3);
            } else {
                this.trade.cancelTrade();
                if (this.pl != null) {
                    Service.gI().sendThongBao(this.pl, "Mua NR 3 sao: đặt NR 3 sao vào ô trade (giá 100tr). Bán NR 3 sao: đặt tối thiểu 120tr vàng để mua!");
                }
            }
            return;
        }

        // 3. BOT 8, 9, 10: BÁN BỔ TRỢ (CUỒNG NỘ, BỔ KHÍ, BỔ HUYẾT, GIÁP XÊN)
        if (!this.multiPrices.isEmpty()) {
            int goldInTrade = this.trade.getGoldTrade1();
            if (goldInTrade <= 0) {
                this.trade.cancelTrade();
                if (this.pl != null) {
                    Service.gI().sendThongBao(this.pl, "Hãy đặt vàng vào ô giao dịch rồi bấm Khóa.");
                }
                return;
            }

            int targetItem = this.idItem;
            int itemPrice = this.priceGold;

            // Kiểm tra xem người chơi có đặt mẫu vật phẩm vào ô trade để chọn loại không
            if (item != null) {
                for (Item it : item) {
                    if (it != null && it.isNotNullItem() && it.template != null && this.multiPrices.containsKey(it.template.id)) {
                        targetItem = it.template.id;
                        itemPrice = this.multiPrices.get(targetItem);
                        break;
                    }
                }
            }

            if (goldInTrade >= itemPrice) {
                int qty = goldInTrade / itemPrice;
                activeItem(targetItem, qty);
            } else {
                this.trade.cancelTrade();
                if (this.pl != null) {
                    Service.gI().sendThongBao(this.pl, "Cần tối thiểu " + Util.numberToMoney(itemPrice) + " vàng để mua!");
                }
            }
            return;
        }

        // 4. BOT BÁN ĐÁ BẰNG VÀNG (BOT 1 - 5)
        if (this.priceGold > 0) {
            int goldInTrade = this.trade.getGoldTrade1();
            if (goldInTrade >= this.priceGold) {
                int slStone = goldInTrade / this.priceGold;
                activeItem(this.idItem, slStone);
            } else {
                this.trade.cancelTrade();
                if (this.pl != null) {
                    Item it = ItemService.gI().createNewItem((short) this.idItem);
                    String itemName = (it != null && it.template != null) ? it.template.name : "Đá";
                    Service.gI().sendThongBao(this.pl, "Giá 1 viên " + itemName + " là " + Util.numberToMoney(this.priceGold) + " vàng!");
                }
            }
            return;
        }

        // 5. BÁN BẰNG VẬT PHẨM TRAO ĐỔI
        int slot1 = item.stream()
                .filter(it -> it != null && it.template != null && it.template.id == this.idItTd && it.quantity >= this.slot)
                .mapToInt(it -> it.quantity)
                .findFirst()
                .orElse(0);

        if (slot1 > 0) {
            int sl1 = (int) Math.round((double) slot1 / this.slot);
            activeItem(this.idItem, sl1);
        } else {
            this.trade.cancelTrade();
            if (this.pl != null) {
                Service.gI().sendThongBao(this.pl, "Vật phẩm đặt vào không đúng yêu cầu của bot.");
            }
        }
    }

    public void activeItem(int itemId, int sl) {
        if (this.trade == null || itemId <= 0 || sl <= 0) {
            if (this.trade != null) {
                this.trade.cancelTrade();
            }
            return;
        }
        // Không cần tạo/cập nhật kho ảo ở mỗi giao dịch; việc đó vừa nặng vừa không
        // tham gia vào itemsTrade2. Món bán được đưa trực tiếp vào phiên bên dưới.
        int remaining = sl;
        while (remaining > 0) {
            int batch = Math.min(remaining, 99);
            Item it = ItemService.gI().createNewItem((short) itemId, batch);
            if (it == null || !it.isNotNullItem()) {
                this.trade.cancelTrade();
                if (this.pl != null) {
                    Service.gI().sendThongBao(this.pl, "Vật phẩm của bot không hợp lệ (ID " + itemId + ").");
                }
                return;
            }
            it.quantityGD = batch;
            this.trade.addItemBot(it);
            remaining -= batch;
        }
        this.trade.lockTran(this.bot);
        this.trade.acceptTrade();
    }

    public void onTradeSuccess() {
        this.buyGoldBarsIfGoldFull();
        this.buffStockIfNeeded(this.idItem);
    }

    // Khi vàng đầy (>= 37tr), bot tự mua thỏi vàng cất túi (37tr/thỏi) để không bao giờ bị tràn vàng
    public void buyGoldBarsIfGoldFull() {
        if (this.bot == null || this.bot.inventory == null) {
            return;
        }
        if (this.bot.inventory.gold >= GOLD_BAR_VALUE) {
            int numBars = (int) (this.bot.inventory.gold / GOLD_BAR_VALUE);
            long goldSpent = (long) numBars * GOLD_BAR_VALUE;
            this.bot.inventory.gold -= goldSpent;
            Item goldBars = ItemService.gI().createNewItem((short) 457, numBars);
            InventoryService.gI().addItemBag(this.bot, goldBars);
        }
    }

    // Khi hết vàng, bot tự bán thỏi vàng (37tr vàng/thỏi) để có tiền mặt trả người chơi
    public void sellGoldBarsIfNeeded(long goldNeeded) {
        if (this.bot == null || this.bot.inventory == null) {
            return;
        }
        while (this.bot.inventory.gold < goldNeeded) {
            Item goldBar = null;
            if (this.bot.inventory.itemsBag != null) {
                for (Item it : this.bot.inventory.itemsBag) {
                    if (it != null && it.isNotNullItem() && it.template != null && it.template.id == 457 && it.quantity > 0) {
                        goldBar = it;
                        break;
                    }
                }
            }
            if (goldBar != null) {
                InventoryService.gI().subQuantityItemsBag(this.bot, goldBar, 1);
                this.bot.inventory.gold += GOLD_BAR_VALUE;
            } else {
                // Tự động buff thêm 100 thỏi vàng nếu hết sạch để bán tiếp
                Item newBars = ItemService.gI().createNewItem((short) 457, 100);
                InventoryService.gI().addItemBag(this.bot, newBars);
                InventoryService.gI().subQuantityItemsBag(this.bot, newBars, 1);
                this.bot.inventory.gold += GOLD_BAR_VALUE;
            }
        }
    }

    public void buffStockIfNeeded(int itemId) {
        if (this.isBuyer || itemId <= 0) {
            return;
        }
        if (this.bot == null || this.bot.inventory == null || this.bot.inventory.itemsBag == null) {
            return;
        }
        boolean hasStock = false;
        for (Item it : this.bot.inventory.itemsBag) {
            if (it != null && it.isNotNullItem() && it.template != null && it.template.id == itemId) {
                if (it.quantity < 1000) {
                    it.quantity = 1_000_000;
                }
                hasStock = true;
                break;
            }
        }
        if (!hasStock) {
            Item newStock = ItemService.gI().createNewItem((short) itemId, 1_000_000);
            InventoryService.gI().addItemBag(this.bot, newStock);
        }
    }

    public void mapL() {
        if (this.bot == null) {
            return;
        }
        if (this.bot.zone == null || this.bot.zone.map.mapId != this.targetMapId || this.bot.zone.zoneId != this.targetZoneId) {
            nro.models.map.Map map = MapService.gI().getMapById(this.targetMapId);
            if (map != null && this.targetZoneId >= 0 && this.targetZoneId < map.zones.size()) {
                Zone zone = map.zones.get(this.targetZoneId);
                if (zone != null) {
                    ChangeMapService.gI().goToMap(this.bot, zone);
                    int groundY = zone.map.yPhysicInTop(this.targetX, 100);
                    if (groundY <= 0) {
                        groundY = this.targetY;
                    }
                    this.bot.location.x = this.targetX;
                    this.bot.location.y = groundY;
                    PlayerService.gI().playerMove(this.bot, this.targetX, groundY);
                    this.bot.zone.load_Me_To_Another(this.bot);
                }
            }
        }
    }
}
