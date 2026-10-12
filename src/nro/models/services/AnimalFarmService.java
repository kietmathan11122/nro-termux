package nro.models.services;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import nro.models.data.DataGame;
import nro.models.item.Item;
import nro.models.network.Message;
import nro.models.player.Player;

/** Server-authoritative feeding timers and one-item animal harvests. */
public final class AnimalFarmService {
    public static final int MAP_EARTH = 177, MAP_NAMEK = 178, MAP_SAIYAN = 179;
    public static final short FEED = 2250;
    private static final short[] PRODUCT = {2251, 2252, 2253, 2254};
    private static final int[] FARM_ICONS = {19202, 19203, 19204, 19205, 19206};
    private static final int[] SECONDS = {300, 360, 600, 480};
    private static final AnimalFarmService INSTANCE = new AnimalFarmService();
    private final Map<Long, Animal[]> farms = new ConcurrentHashMap<>();

    public static AnimalFarmService gI() { return INSTANCE; }
    public boolean isAnimalFarmMap(int id) { return id >= MAP_EARTH && id <= MAP_SAIYAN; }

    public void handleMessage(Player p, Message msg) throws IOException {
        if (p == null || p.zone == null || !isAnimalFarmMap(p.zone.map.mapId)) return;
        byte action = msg.reader().readByte();
        int id = msg.reader().readInt();
        if (action == 0) feed(p, id);
        else if (action == 1) collect(p, id);
        else if (action == 3) {
            sendFarmIcons(p);
            sendFullData(p);
        }
    }

    public void sendFullData(Player p) {
        if (p == null || p.zone == null || !isAnimalFarmMap(p.zone.map.mapId)) return;
        Animal[] farm = farmOf(p);
        Message out = null;
        try {
            out = new Message(-34);
            out.writer().writeByte(20);
            out.writer().writeByte(farm.length);
            for (Animal a : farm) {
                int remain = remaining(a);
                out.writer().writeInt(a.id);
                out.writer().writeByte(a.type);
                out.writer().writeByte(a.readyAt == 0 ? 0 : (remain == 0 ? 2 : 1));
                out.writer().writeInt(remain);
                out.writer().writeShort(a.x);
                out.writer().writeShort(a.y);
            }
            p.sendMessage(out);
        } catch (Exception e) { e.printStackTrace(); }
        finally { if (out != null) out.cleanup(); }
    }

    private void sendFarmIcons(Player p) {
        if (p == null || p.getSession() == null) return;
        for (int iconId : FARM_ICONS) {
            DataGame.sendIcon(p.getSession(), iconId);
        }
    }

    private void feed(Player p, int id) {
        Animal a = find(p, id);
        if (a == null) {
            Service.gI().sendThongBao(p, "Không tìm thấy vật nuôi, hãy vào lại khu nông trại");
            sendFullData(p);
            return;
        }
        if (a.readyAt != 0) {
            Service.gI().sendThongBao(p, remaining(a) == 0 ? "Vật phẩm đã sẵn sàng, hãy thu thập" : "Động vật vẫn đang tạo vật phẩm");
            return;
        }
        Item food = InventoryService.gI().findItemBag(p, FEED);
        if (food == null || food.quantity < 1) {
            Service.gI().sendThongBao(p, "Cần 1 Thức ăn chăn nuôi. Hãy mua từ Trưởng làng");
            return;
        }
        InventoryService.gI().subQuantityItemsBag(p, food, 1);
        InventoryService.gI().sendItemBags(p);
        a.readyAt = System.currentTimeMillis() + SECONDS[a.type] * 1000L;
        sendFullData(p);
        Service.gI().sendThongBao(p, "Đã cho ăn. Còn " + SECONDS[a.type] / 60 + " phút");
    }

    private void collect(Player p, int id) {
        Animal a = find(p, id);
        if (a == null) {
            Service.gI().sendThongBao(p, "Không tìm thấy vật nuôi, hãy vào lại khu nông trại");
            sendFullData(p);
            return;
        }
        if (a.readyAt == 0 || remaining(a) > 0) {
            Service.gI().sendThongBao(p, "Chưa đến thời gian thu thập"); return;
        }
        Item reward = ItemService.gI().createNewItem(PRODUCT[a.type], 1);
        if (reward == null || reward.template == null) {
            Service.gI().sendThongBao(p, "Vật phẩm thu hoạch chưa được tải, vui lòng báo quản trị viên");
            return;
        }
        if (!InventoryService.gI().addItemBag(p, reward)) {
            Service.gI().sendThongBao(p, "Hành trang đã đầy"); return;
        }
        a.readyAt = 0;
        // Gửi trực tiếp PNG mới trước gói hành trang để không phụ thuộc cache icon cũ của client.
        if (p.getSession() != null) {
            DataGame.sendIcon(p.getSession(), reward.template.iconID);
        }
        InventoryService.gI().sendItemBags(p);
		ProfessionService.gI().onHarvest(p, PRODUCT[a.type], 1);
        sendFullData(p);
        Service.gI().sendThongBao(p, "Thu thập được 1 " + reward.template.name);
    }

    private Animal[] farmOf(Player p) {
        return farms.computeIfAbsent(p.id, k -> new Animal[] {
            new Animal(177000, 0, 170, 300), new Animal(177001, 1, 360, 300),
            new Animal(177002, 2, 550, 300), new Animal(177003, 3, 735, 282)
        });
    }
    private Animal find(Player p, int id) {
        Animal[] farm = farmOf(p);
        for (Animal a : farm) if (a.id == id) return a;
        // Client có thể tạo ID tạm theo map (177xxx/178xxx/179xxx) trước khi nhận gói đồng bộ.
        // Chữ số cuối luôn là vị trí gà, bò, chó, cá nên chuẩn hóa để thao tác không bị mất.
        int slot = Math.floorMod(id, 10);
        return slot < farm.length ? farm[slot] : null;
    }
    private int remaining(Animal a) { return a.readyAt == 0 ? 0 : (int)Math.max(0, (a.readyAt - System.currentTimeMillis() + 999) / 1000); }
    private static final class Animal {
        int id, type, x, y; long readyAt;
        Animal(int id, int type, int x, int y) { this.id=id; this.type=type; this.x=x; this.y=y; }
    }
}
