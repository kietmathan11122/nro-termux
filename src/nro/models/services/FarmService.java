package nro.models.services;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import nro.models.data.LocalManager;
import nro.models.data.LocalResultSet;
import nro.models.network.Message;
import nro.models.item.Item;
import nro.models.player.Player;

public class FarmService {

    public static final int MAP_EARTH = 174;
    public static final int MAP_NAMEK = 175;
    public static final int MAP_SAIYAN = 176;
    private static final int MAX_PLOTS = 10;
    private static final int INITIAL_PLOTS = 5;
    private static final int QUESTS_PER_PLOT = 30;
    public static final int FERTILIZER_ITEM_ID = 1094;
    public static final int SEED_ITEM_ID = 1095;
    public static final int GROWTH_POTION_ITEM_ID = 1096;
    private static final int[] GROW_SECONDS = {300, 420, 600, 720};
    private static final FarmService INSTANCE = new FarmService();
    private final Map<Long, Garden> gardens = new ConcurrentHashMap<>();
    private final Map<Long, Integer> selectedPlots = new ConcurrentHashMap<>();

    public static FarmService gI() {
        return INSTANCE;
    }

    public boolean isFarmMap(int mapId) {
        return mapId == MAP_EARTH || mapId == MAP_NAMEK || mapId == MAP_SAIYAN;
    }

    public void handleMessage(Player player, Message message) throws IOException {
        if (player == null || player.zone == null || !isFarmMap(player.zone.map.mapId)) {
            return;
        }
        byte action = message.reader().readByte();
        switch (action) {
            case 0:
                interact(player, message.reader().readInt());
                break;
            case 1:
                plant(player, message.reader().readInt(), message.reader().readShort());
                break;
            case 2:
                harvest(player, message.reader().readInt());
                break;
            case 3:
                water(player, message.reader().readInt());
                break;
            case 4:
                unlock(player, message.reader().readByte() == 1);
                break;
            case 5:
                sendFullGarden(player);
                break;
            default:
                break;
        }
    }

    public void sendFullGarden(Player player) {
        if (player == null || player.zone == null || !isFarmMap(player.zone.map.mapId)) {
            return;
        }
        Garden garden = gardenOf(player);
        Message msg = null;
        try {
            msg = new Message(-34);
            msg.writer().writeByte(10);
            msg.writer().writeByte(1);
            msg.writer().writeInt(garden.unlocked);
            for (int i = 0; i < MAX_PLOTS; i++) {
                Plot plot = garden.plots[i];
                msg.writer().writeInt(i);
                msg.writer().writeBoolean(i < garden.unlocked);
                msg.writer().writeByte(stageOf(plot));
                msg.writer().writeByte(plot.crop);
                msg.writer().writeInt(remaining(plot));
                msg.writer().writeBoolean(plot.watered);
                msg.writer().writeShort(plotX(player.zone.map.mapId, i));
                msg.writer().writeShort(plotY(i));
            }
            player.sendMessage(msg);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (msg != null) msg.cleanup();
        }
    }

    private void interact(Player player, int plotId) {
        Garden garden = gardenOf(player);
        if (validPlot(plotId)) selectedPlots.put(player.id, plotId);
        if (!validPlot(plotId) || plotId >= garden.unlocked) {
            Service.gI().sendThongBao(player, "Ô đất này chưa được mở khóa");
            return;
        }
        Plot plot = garden.plots[plotId];
        if (plot.crop < 0) {
            sendOpenSeedMenu(player, plotId);
        } else if (remaining(plot) <= 0) {
            harvest(player, plotId);
        } else if (!plot.watered) {
            water(player, plotId);
        } else {
            Service.gI().sendThongBao(player, "Cây còn " + remaining(plot) + " giây để trưởng thành");
        }
    }

    private void plant(Player player, int plotId, short seedItemId) {
        Garden garden = gardenOf(player);
        if (!validPlot(plotId) || plotId >= garden.unlocked) return;
        Plot plot = garden.plots[plotId];
        if (plot.crop >= 0) {
            Service.gI().sendThongBao(player, "Ô đất đã có cây");
            return;
        }
        int crop = seedItemId - 1872;
        if (crop < 0 || crop >= GROW_SECONDS.length) {
            Service.gI().sendThongBao(player, "Hạt giống không hợp lệ");
            return;
        }
        Item seed = InventoryService.gI().findItemBag(player, SEED_ITEM_ID);
        if (seed == null || seed.quantity < 1) {
            Service.gI().sendThongBao(player, "Cần 1 Hạt mầm để gieo cây");
            return;
        }
        InventoryService.gI().subQuantityItemsBag(player, seed, 1);
        plot.crop = (byte) crop;
        plot.matureAt = System.currentTimeMillis() + GROW_SECONDS[crop] * 1000L;
        plot.watered = false;
        save(player.id, garden);
        InventoryService.gI().sendItemBags(player);
        sendSingle(player, plotId);
        Service.gI().sendThongBao(player, "Đã gieo hạt. Chạm lần nữa để tưới nước");
    }

    private void water(Player player, int plotId) {
        Garden garden = gardenOf(player);
        if (!validPlot(plotId) || plotId >= garden.unlocked) return;
        Plot plot = garden.plots[plotId];
        if (plot.crop < 0 || remaining(plot) <= 0 || plot.watered) return;
        long now = System.currentTimeMillis();
        plot.matureAt = now + Math.max(1000L, (plot.matureAt - now) * 9L / 10L);
        plot.watered = true;
        save(player.id, garden);
        sendSingle(player, plotId);
        Service.gI().sendThongBao(player, "Đã tưới cây, thời gian trồng giảm 10%");
    }

    private void harvest(Player player, int plotId) {
        Garden garden = gardenOf(player);
        if (!validPlot(plotId) || plotId >= garden.unlocked) return;
        Plot plot = garden.plots[plotId];
        if (plot.crop < 0 || remaining(plot) > 0) {
            Service.gI().sendThongBao(player, "Cây chưa thể thu hoạch");
            return;
        }
        byte crop = plot.crop;
        int quantity = 1 + crop;
        short harvestItemId = (short) (2240 + crop);
        Item harvestItem = ItemService.gI().createNewItem(harvestItemId, quantity);
        if (!InventoryService.gI().addItemBag(player, harvestItem)) {
            Service.gI().sendThongBao(player, "Hành trang đã đầy, hãy dọn chỗ trước khi thu hoạch");
            return;
        }
        long reward = 10000L * quantity;
        player.inventory.gold = Math.min(player.inventory.gold + reward, 2000000000L);
        plot.crop = -1;
        plot.matureAt = 0;
        plot.watered = false;
        save(player.id, garden);
        sendSingle(player, plotId);
        sendHarvest(player, plotId, crop, quantity);
        InventoryService.gI().sendItemBags(player);
        ProfessionService.gI().onHarvest(player, harvestItemId, quantity);
        Service.gI().sendMoney(player);
        Service.gI().sendThongBao(player, "Thu hoạch " + quantity + " "
                + harvestItem.template.name + " và +" + reward + " vàng");
    }

    private void unlock(Player player, boolean useGem) {
        Garden garden = gardenOf(player);
        if (garden.unlocked >= MAX_PLOTS) return;
        int required = (garden.unlocked - INITIAL_PLOTS + 1) * QUESTS_PER_PLOT;
        if (garden.completedQuests < required) {
            Service.gI().sendThongBao(player, "Cần hoàn thành " + required
                    + " nhiệm vụ nông trại để mở ô này (đã hoàn thành "
                    + garden.completedQuests + ")");
            return;
        }
        garden.unlocked++;
        save(player.id, garden);
        sendFullGarden(player);
        Service.gI().sendThongBao(player, "Đã mở thêm 1 ô đất nông trại");
    }

    public void onFarmQuestCompleted(Player player) {
        if (player == null) return;
        Garden garden = gardenOf(player);
        garden.completedQuests++;
        int allowed = Math.min(MAX_PLOTS, INITIAL_PLOTS + garden.completedQuests / QUESTS_PER_PLOT);
        if (allowed > garden.unlocked) {
            garden.unlocked++;
            Service.gI().sendThongBao(player, "Hoàn thành đủ " + garden.completedQuests
                    + " nhiệm vụ: đã mở thêm 1 ô đất nông trại!");
            if (player.zone != null && isFarmMap(player.zone.map.mapId)) sendFullGarden(player);
        }
        save(player.id, garden);
    }

    public boolean useBooster(Player player, Item item) {
        if (player == null || item == null || player.zone == null || !isFarmMap(player.zone.map.mapId)) {
            Service.gI().sendThongBao(player, "Chỉ dùng được vật phẩm này trong nông trại");
            return false;
        }
        Garden garden = gardenOf(player);
        int plotId = selectedPlots.getOrDefault(player.id, -1);
        if (!validPlot(plotId) || plotId >= garden.unlocked || garden.plots[plotId].crop < 0
                || remaining(garden.plots[plotId]) <= 0) {
            plotId = -1;
            for (int i = 0; i < garden.unlocked; i++) {
                if (garden.plots[i].crop >= 0 && remaining(garden.plots[i]) > 0) {
                    plotId = i;
                    break;
                }
            }
        }
        if (plotId < 0) {
            Service.gI().sendThongBao(player, "Không có cây đang lớn để sử dụng");
            return false;
        }
        Plot plot = garden.plots[plotId];
        long now = System.currentTimeMillis();
        if (item.template.id == GROWTH_POTION_ITEM_ID) {
            plot.matureAt = now;
            Service.gI().sendThongBao(player, "Thuốc tăng trưởng đã làm cây chín ngay");
        } else if (item.template.id == FERTILIZER_ITEM_ID) {
            plot.matureAt = now + Math.max(1000L, (plot.matureAt - now) / 2L);
            Service.gI().sendThongBao(player, "Phân bón đã giảm 50% thời gian còn lại");
        } else {
            return false;
        }
        InventoryService.gI().subQuantityItemsBag(player, item, 1);
        InventoryService.gI().sendItemBags(player);
        save(player.id, garden);
        sendSingle(player, plotId);
        return true;
    }

    private void sendOpenSeedMenu(Player player, int plotId) {
        Message msg = null;
        try {
            msg = new Message(-34);
            msg.writer().writeByte(10);
            msg.writer().writeByte(2);
            msg.writer().writeInt(plotId);
            player.sendMessage(msg);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (msg != null) msg.cleanup();
        }
    }

    private void sendSingle(Player player, int plotId) {
        Plot plot = gardenOf(player).plots[plotId];
        Message msg = null;
        try {
            msg = new Message(-34);
            msg.writer().writeByte(10);
            msg.writer().writeByte(0);
            msg.writer().writeInt(plotId);
            msg.writer().writeByte(stageOf(plot));
            msg.writer().writeByte(plot.crop);
            msg.writer().writeInt(remaining(plot));
            msg.writer().writeBoolean(plot.watered);
            player.sendMessage(msg);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (msg != null) msg.cleanup();
        }
    }

    private void sendHarvest(Player player, int plotId, byte crop, int quantity) {
        Message msg = null;
        try {
            msg = new Message(-34);
            msg.writer().writeByte(10);
            msg.writer().writeByte(4);
            msg.writer().writeInt(plotId);
            msg.writer().writeByte(crop);
            msg.writer().writeInt(quantity);
            player.sendMessage(msg);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (msg != null) msg.cleanup();
        }
    }

    private Garden gardenOf(Player player) {
        return gardens.computeIfAbsent(player.id, this::load);
    }

    private Garden load(long playerId) {
        Garden garden = new Garden();
        LocalResultSet rs = null;
        try {
            rs = LocalManager.executeQuery("SELECT data FROM farm_garden WHERE player_id = ? LIMIT 1", playerId);
            if (rs.first()) {
                garden.decode(rs.getString("data"));
            } else {
                save(playerId, garden);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (rs != null) rs.close();
        }
        return garden;
    }

    private void save(long playerId, Garden garden) {
        try {
            LocalManager.executeUpdate("INSERT INTO farm_garden(player_id, data) VALUES(?, ?) ON DUPLICATE KEY UPDATE data = VALUES(data)", playerId, garden.encode());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean validPlot(int plotId) {
        return plotId >= 0 && plotId < MAX_PLOTS;
    }

    private int remaining(Plot plot) {
        if (plot.crop < 0) return 0;
        return (int) Math.max(0L, (plot.matureAt - System.currentTimeMillis() + 999L) / 1000L);
    }

    private byte stageOf(Plot plot) {
        if (plot.crop < 0) return 0;
        int remaining = remaining(plot);
        if (remaining <= 0) return 5;
        int total = GROW_SECONDS[plot.crop];
        double elapsed = 1.0 - (double) remaining / total;
        if (elapsed < 0.10) return 1;
        if (elapsed < 0.30) return 2;
        if (elapsed < 0.50) return 3;
        return 4;
    }

    private short plotX(int mapId, int plotId) {
        int col = plotId % 5;
        // Bố cục cố định cho cả ba nông trại: ô đầu bắt đầu tại x=492.
        return (short) (492 + col * 130);
    }

    private short plotY(int plotId) {
        // Tọa độ mới theo map tool: hàng trên y=360, hàng dưới cách 60px.
        return (short) (plotId < 5 ? 360 : 420);
    }

    private static class Plot {
        byte crop = -1;
        long matureAt;
        boolean watered;
    }

    private static class Garden {
        int unlocked = INITIAL_PLOTS;
        final Plot[] plots = new Plot[MAX_PLOTS];

        Garden() {
            for (int i = 0; i < plots.length; i++) plots[i] = new Plot();
        }

        String encode() {
            StringBuilder data = new StringBuilder();
            data.append(unlocked).append(',').append(completedQuests);
            for (Plot plot : plots) {
                data.append(';').append(plot.crop).append(',').append(plot.matureAt).append(',').append(plot.watered ? 1 : 0);
            }
            return data.toString();
        }

        void decode(String data) {
            if (data == null || data.isEmpty()) return;
            try {
                String[] parts = data.split(";");
                String[] header = parts[0].split(",");
                unlocked = Math.max(INITIAL_PLOTS, Math.min(MAX_PLOTS, Integer.parseInt(header[0])));
                completedQuests = header.length > 1 ? Math.max(0, Integer.parseInt(header[1])) : 0;
                for (int i = 0; i < plots.length && i + 1 < parts.length; i++) {
                    String[] fields = parts[i + 1].split(",");
                    plots[i].crop = Byte.parseByte(fields[0]);
                    plots[i].matureAt = Long.parseLong(fields[1]);
                    plots[i].watered = "1".equals(fields[2]);
                }
            } catch (Exception ignored) {
            }
        }

        int completedQuests;
    }
}
