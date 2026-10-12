package nro.models.Bot;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import nro.models.item.Item;
import nro.models.server.ServerManager;
import nro.models.services.ChatGlobalService;
import nro.models.services.ItemService;
import nro.models.services.InventoryService;
import nro.models.utils.Logger;
import nro.models.utils.Util;

public class BotManager implements Runnable {

    private static final long GLOBAL_CHAT_INTERVAL = 240000L;
    private static final String[] GLOBAL_CHAT_TEXTS = {
        "Mua vang so luong lon qua ATM",
        "Xin slot up tnsm ke",
        "Can pt san boss xuyen dem",
        "Mua do than linh qua ATM",
        "Ai up quai chung khong",
        "Can mua 3 sao so luong lon",
        "BDKB k2 ttv up tnsm",
        "Rung da k5 ttv up tnsm"
    };

    public static BotManager i;

    public List<Bot> bot = new CopyOnWriteArrayList<>();
    private long lastGlobalChatTime;
    private long lastGlobalChatBotId = -1;

    public static BotManager gI() {
        if (i == null) {
            i = new BotManager();
        }
        return i;
    }

    @Override
    public void run() {
        while (ServerManager.isRunning) {
            try {
                long st = System.currentTimeMillis();

                for (Bot bot : this.bot) {
                    if (bot != null) {
                        bot.update();
                    }
                }
                this.chatGlobalForTrainingBot();

                long timeLeft = 150 - (System.currentTimeMillis() - st);
                if (timeLeft > 0) {
                    Thread.sleep(timeLeft);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void removeStoneTradeBots() {
        for (Bot b : this.bot) {
            if (b != null && b.shop != null && b.shop.targetMapId == 44) {
                if (b.zone != null) {
                    b.zone.removePlayer(b);
                }
            }
        }
        this.bot.removeIf(b -> b != null && b.shop != null && b.shop.targetMapId == 44);
        Logger.warning("Đã gỡ/tắt toàn bộ Bot Giao Dịch tại Vách Kakarot.\n");
    }

    public synchronized void spawnStoneTradeBots() {
        int[][] stoneData = {
            {220, 860},  // Đá lục bảo
            {221, 930},  // Đá Saphia
            {222, 1000}, // Đá Ruby
            {223, 1070}, // Đá Titan
            {224, 1140}  // Đá thạch anh tím
        };

        String[] botNames = {
            "bot_muaban_1",
            "bot_muaban_2",
            "bot_muaban_3",
            "bot_muaban_4",
            "bot_muaban_5"
        };

        short[][] botParts = {
            {77, 78, 79},    // Bộ cải trang Trái Đất hoàn chỉnh
            {86, 87, 88},    // Bộ cải trang Namek hoàn chỉnh
            {159, 160, 161}, // Bộ cải trang Xayda hoàn chỉnh
            {77, 78, 79},
            {86, 87, 88}
        };

        // Gỡ các bot bán đá / thu mua cũ tại map 44 nếu có để tránh trùng lặp
        this.bot.removeIf(b -> b != null && b.shop != null && b.shop.targetMapId == 44);

        for (int idx = 0; idx < 5; idx++) {
            int itemId = stoneData[idx][0];
            int posX = stoneData[idx][1];
            String name = botNames[idx];
            short head = botParts[idx][0];
            short body = botParts[idx][1];
            short leg = botParts[idx][2];

            BotGiaoDich shop = new BotGiaoDich(itemId, 1_000_000); // 1 triệu vàng / 1 viên
            shop.targetMapId = 44; // Vách núi Kakarot
            shop.targetZoneId = 0; // Khu 0
            shop.targetX = posX;
            shop.targetY = 408;

            Bot b = new Bot(head, body, leg, 1, name, shop, (short) 0);
            b.isBot = true;
            b.gender = (byte) (idx % 3);
            shop.bot = b;

            b.nPoint.limitPower = 10;
            b.nPoint.power = 2_000_000_000L;
            b.nPoint.tiemNang = 2_000_000_000L;
            b.nPoint.hpMax = 500_000;
            b.nPoint.hp = 500_000;
            b.nPoint.mpMax = 500_000;
            b.nPoint.mp = 500_000;
            b.nPoint.dameg = 20_000;

            // Buff 1.000.000 viên đá ban đầu vào túi
            Item stones = ItemService.gI().createNewItem((short) itemId, 1_000_000);
            InventoryService.gI().addItemBag(b, stones);

            this.bot.add(b);
            shop.mapL();
        }

        // ==========================================
        // BOT SỐ 6: THU MUA ĐỒ THẦN LINH (GIÁ 75TR VÀNG/MÓN)
        // ==========================================
        BotGiaoDich buyerShop = BotGiaoDich.createBuyerBot(75_000_000L);
        buyerShop.targetMapId = 44; // Vách núi Kakarot
        buyerShop.targetZoneId = 0; // Khu 0
        buyerShop.targetX = 1210;
        buyerShop.targetY = 408;

        Bot bot6 = new Bot((short) 159, (short) 160, (short) 161, 1, "bot_muaban_6", buyerShop, (short) 0);
        bot6.isBot = true;
        bot6.gender = 2; // Xayda
        buyerShop.bot = bot6;

        bot6.nPoint.limitPower = 10;
        bot6.nPoint.power = 2_000_000_000L;
        bot6.nPoint.tiemNang = 2_000_000_000L;
        bot6.nPoint.hpMax = 500_000;
        bot6.nPoint.hp = 500_000;
        bot6.nPoint.mpMax = 500_000;
        bot6.nPoint.mp = 500_000;
        bot6.nPoint.dameg = 20_000;

        // Vốn ban đầu: 500 triệu vàng + 100 thỏi vàng trong túi
        bot6.inventory.gold = 500_000_000L;
        Item goldBars = ItemService.gI().createNewItem((short) 457, 100);
        InventoryService.gI().addItemBag(bot6, goldBars);

        this.bot.add(bot6);
        buyerShop.mapL();

        // ==========================================
        // BOT SỐ 7: MUA (100TR) & BÁN (120TR) NGỌC RỒNG 3 SAO
        // ==========================================
        BotGiaoDich bot7Shop = BotGiaoDich.createNR3Trader();
        bot7Shop.targetMapId = 44;
        bot7Shop.targetZoneId = 0;
        bot7Shop.targetX = 1280;
        bot7Shop.targetY = 408;

        Bot bot7 = new Bot((short) 77, (short) 78, (short) 79, 1, "bot_muaban_7", bot7Shop, (short) 0);
        bot7.isBot = true;
        bot7.gender = 0; // Trái Đất
        bot7Shop.bot = bot7;

        bot7.nPoint.limitPower = 10;
        bot7.nPoint.power = 2_000_000_000L;
        bot7.nPoint.tiemNang = 2_000_000_000L;
        bot7.nPoint.hpMax = 500_000;
        bot7.nPoint.hp = 500_000;
        bot7.nPoint.mpMax = 500_000;
        bot7.nPoint.mp = 500_000;
        bot7.nPoint.dameg = 20_000;

        bot7.inventory.gold = 500_000_000L;
        Item bot7Bars = ItemService.gI().createNewItem((short) 457, 100);
        InventoryService.gI().addItemBag(bot7, bot7Bars);
        Item bot7NR3 = ItemService.gI().createNewItem((short) 16, 1_000_000);
        InventoryService.gI().addItemBag(bot7, bot7NR3);

        this.bot.add(bot7);
        bot7Shop.mapL();

        // ==========================================
        // BOT SỐ 8: BÁN CUỒNG NỘ (GIÁ 5TR VÀNG)
        // ==========================================
        BotGiaoDich bot8Shop = BotGiaoDich.createBoTroShop(381, 5_000_000, "Bán Cuồng nộ giá 5tr vàng / 1 cái");
        bot8Shop.targetMapId = 44;
        bot8Shop.targetZoneId = 0;
        bot8Shop.targetX = 1350;
        bot8Shop.targetY = 408;

        Bot bot8 = new Bot((short) 86, (short) 87, (short) 88, 1, "bot_muaban_8", bot8Shop, (short) 0);
        bot8.isBot = true;
        bot8.gender = 1; // Namek
        bot8Shop.bot = bot8;

        bot8.nPoint.limitPower = 10;
        bot8.nPoint.power = 2_000_000_000L;
        bot8.nPoint.tiemNang = 2_000_000_000L;
        bot8.nPoint.hpMax = 500_000;
        bot8.nPoint.hp = 500_000;
        bot8.nPoint.mpMax = 500_000;
        bot8.nPoint.mp = 500_000;
        bot8.nPoint.dameg = 20_000;

        Item bot8CN = ItemService.gI().createNewItem((short) 381, 1_000_000);
        InventoryService.gI().addItemBag(bot8, bot8CN);

        this.bot.add(bot8);
        bot8Shop.mapL();

        // ==========================================
        // BOT SỐ 9: BÁN BỔ KHÍ (GIÁ 3TR VÀNG) & BỔ HUYẾT (4TR)
        // ==========================================
        BotGiaoDich bot9Shop = BotGiaoDich.createBoTroShop(383, 3_000_000, "Bán Bổ khí giá 3tr vàng / 1 cái");
        bot9Shop.multiPrices.put(382, 4_000_000);
        bot9Shop.targetMapId = 44;
        bot9Shop.targetZoneId = 0;
        bot9Shop.targetX = 1420;
        bot9Shop.targetY = 408;

        Bot bot9 = new Bot((short) 159, (short) 160, (short) 161, 1, "bot_muaban_9", bot9Shop, (short) 0);
        bot9.isBot = true;
        bot9.gender = 2; // Xayda
        bot9Shop.bot = bot9;

        bot9.nPoint.limitPower = 10;
        bot9.nPoint.power = 2_000_000_000L;
        bot9.nPoint.tiemNang = 2_000_000_000L;
        bot9.nPoint.hpMax = 500_000;
        bot9.nPoint.hp = 500_000;
        bot9.nPoint.mpMax = 500_000;
        bot9.nPoint.mp = 500_000;
        bot9.nPoint.dameg = 20_000;

        Item bot9BK = ItemService.gI().createNewItem((short) 383, 1_000_000);
        InventoryService.gI().addItemBag(bot9, bot9BK);
        Item bot9BH = ItemService.gI().createNewItem((short) 382, 1_000_000);
        InventoryService.gI().addItemBag(bot9, bot9BH);

        this.bot.add(bot9);
        bot9Shop.mapL();

        // ==========================================
        // BOT SỐ 10: BÁN GIÁP XÊN (2TR) & BỔ HUYẾT (4TR)
        // ==========================================
        BotGiaoDich bot10Shop = BotGiaoDich.createBoTroShop(384, 2_000_000, "Bán Giáp xên 2tr, Bổ huyết 4tr");
        bot10Shop.multiPrices.put(382, 4_000_000);
        bot10Shop.targetMapId = 44;
        bot10Shop.targetZoneId = 0;
        bot10Shop.targetX = 1490;
        bot10Shop.targetY = 408;

        Bot bot10 = new Bot((short) 77, (short) 78, (short) 79, 1, "bot_muaban_10", bot10Shop, (short) 0);
        bot10.isBot = true;
        bot10.gender = 0; // Trái Đất
        bot10Shop.bot = bot10;

        bot10.nPoint.limitPower = 10;
        bot10.nPoint.power = 2_000_000_000L;
        bot10.nPoint.tiemNang = 2_000_000_000L;
        bot10.nPoint.hpMax = 500_000;
        bot10.nPoint.hp = 500_000;
        bot10.nPoint.mpMax = 500_000;
        bot10.nPoint.mp = 500_000;
        bot10.nPoint.dameg = 20_000;

        Item bot10GX = ItemService.gI().createNewItem((short) 384, 1_000_000);
        InventoryService.gI().addItemBag(bot10, bot10GX);
        Item bot10BH = ItemService.gI().createNewItem((short) 382, 1_000_000);
        InventoryService.gI().addItemBag(bot10, bot10BH);

        this.bot.add(bot10);
        bot10Shop.mapL();

        Logger.success("Đã khởi tạo đủ 10 Bot mua bán tại Vách Kakarot khu 0 (Đá 1-5, Đồ Thần, NR 3 sao, Bổ trợ)!\n");
    }

    private void chatGlobalForTrainingBot() {
        if (this.bot.isEmpty() || !Util.canDoWithTime(this.lastGlobalChatTime, GLOBAL_CHAT_INTERVAL)) {
            return;
        }
        Bot chosenBot = null;
        if (this.bot.size() == 1) {
            chosenBot = this.bot.get(0);
        } else {
            for (int i = 0; i < 10; i++) {
                Bot randomBot = this.bot.get(Util.nextInt(0, this.bot.size() - 1));
                if (randomBot != null && randomBot.trainingMob != null && randomBot.id != this.lastGlobalChatBotId) {
                    chosenBot = randomBot;
                    break;
                }
            }
        }
        if (chosenBot == null || chosenBot.trainingMob == null) {
            return;
        }
        String text = GLOBAL_CHAT_TEXTS[Util.nextInt(0, GLOBAL_CHAT_TEXTS.length - 1)];
        ChatGlobalService.gI().chat1(chosenBot, text);
        this.lastGlobalChatBotId = chosenBot.id;
        this.lastGlobalChatTime = System.currentTimeMillis();
    }

}
