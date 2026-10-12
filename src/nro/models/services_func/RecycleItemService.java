package nro.models.services_func;

import nro.models.item.Item;
import nro.models.network.Message;
import nro.models.player.Player;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.services.Service;

public class RecycleItemService {

    private static RecycleItemService instance;

    public static RecycleItemService gI() {
        if (instance == null) {
            instance = new RecycleItemService();
        }
        return instance;
    }

    public void handle(Player player, Message msg) {
        try {
            int action = msg.reader().readByte();
            if (action == 1) {
                restore(player, msg.reader().readUnsignedByte());
            } else {
                sendList(player);
            }
        } catch (Exception e) {
            sendList(player);
        }
    }

    private void restore(Player player, int index) {
        if (index < 0 || index >= player.inventory.itemsRecycle.size()) {
            sendList(player);
            return;
        }
        if (InventoryService.gI().getCountEmptyBag(player) <= 0) {
            Service.gI().sendThongBao(player, "Hành trang đã đầy.");
            return;
        }
        Item item = player.inventory.itemsRecycle.get(index);
        if (!InventoryService.gI().addItemBag(player, ItemService.gI().copyItem(item))) {
            Service.gI().sendThongBao(player, "Không thể khôi phục vật phẩm.");
            return;
        }
        player.inventory.itemsRecycle.remove(index);
        InventoryService.gI().sendItemBags(player);
        Service.gI().sendThongBao(player, "Đã khôi phục " + item.template.name + ".");
        sendList(player);
    }

    public void sendList(Player player) {
        Message msg = null;
        try {
            msg = new Message(-58);
            int count = Math.min(20, player.inventory.itemsRecycle.size());
            msg.writer().writeByte(count);
            for (int i = 0; i < count; i++) {
                Item item = player.inventory.itemsRecycle.get(i);
                msg.writer().writeShort(item.template.id);
                msg.writer().writeInt(item.quantity);
                msg.writer().writeUTF(item.getInfo());
                msg.writer().writeUTF(item.getContent());
                msg.writer().writeByte(item.itemOptions.size());
                for (Item.ItemOption option : item.itemOptions) {
                    msg.writer().writeByte(option.optionTemplate.id);
                    msg.writer().writeShort(option.param);
                }
            }
            player.sendMessage(msg);
        } catch (Exception ignored) {
        } finally {
            if (msg != null) msg.cleanup();
        }
    }
}
