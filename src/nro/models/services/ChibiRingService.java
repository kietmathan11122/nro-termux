package nro.models.services;

import nro.models.data.LocalManager;
import nro.models.item.Item;
import nro.models.network.Message;
import nro.models.player.Player;
import nro.models.player_system.Template;
import nro.models.utils.Logger;

/** Vòng Chibi được nhận một lần ở Bò Mộng, rồi dùng từ hành trang như Bông tai. */
public final class ChibiRingService {
    public static final short ITEM_ID = 2378;
    private static final ChibiRingService INSTANCE = new ChibiRingService();

    public static ChibiRingService gI() {
        return INSTANCE;
    }

    private ChibiRingService() {
    }

    public void toggle(Player player) {
        if (player == null || player.inventory == null) {
            return;
        }
        if (player.chibiRingActive) {
            player.chibiRingActive = false;
            SpiritDiscipleService.syncFollower(player.pet);
            sendFusionEffect(player, (byte) 0);
            player.nPoint.calPoint();
            Service.gI().point(player);
            Service.gI().Send_Info_NV(player);
            Service.gI().sendThongBao(player, "Đã tắt Vòng Chibi");
            return;
        }
        if (InventoryService.gI().findItemBag(player, ITEM_ID) == null) {
            Service.gI().sendThongBao(player, "Vòng Chibi phải ở trong hành trang");
            return;
        }
        player.chibiRingActive = true;
        SpiritDiscipleService.syncFollower(player.pet);
        sendFusionEffect(player, (byte) 6);
        player.nPoint.calPoint();
        player.nPoint.setFullHpMp();
        Service.gI().point(player);
        Service.gI().Send_Info_NV(player);
        Service.gI().sendThongBao(player, "Đã bật Vòng Chibi: tăng HP, KI và sức đánh");
    }

    private void sendFusionEffect(Player player, byte effect) {
        try {
            Message msg = new Message(125);
            msg.writer().writeByte(effect);
            msg.writer().writeInt((int) player.id);
            Service.gI().sendMessAllPlayerInMap(player, msg);
            msg.cleanup();
        } catch (Exception e) {
            Logger.logException(ChibiRingService.class, e);
        }
    }

    public void claim(Player player) {
        if (player == null || player.getSession() == null || player.inventory == null) {
            return;
        }
        synchronized (player) {
            Template.ItemTemplate template = ItemService.gI().getTemplate(ITEM_ID);
            if (template == null || template.id != ITEM_ID) {
                Service.gI().sendThongBao(player, "Vòng Chibi chưa được tải, vui lòng thử lại sau");
                return;
            }
            if (InventoryService.gI().findItemBag(player, ITEM_ID) != null
                    || InventoryService.gI().findItemBody(player, ITEM_ID) != null
                    || InventoryService.gI().findItemBox(player, ITEM_ID) != null) {
                Service.gI().sendThongBao(player, "Bạn đã có Vòng Chibi");
                return;
            }
            if (InventoryService.gI().getCountEmptyBag(player) < 1) {
                Service.gI().sendThongBao(player, "Hành trang cần một ô trống để nhận Vòng Chibi");
                return;
            }

            boolean reserved = false;
            boolean delivered = false;
            try {
                LocalManager.executeUpdate("CREATE TABLE IF NOT EXISTS chibi_ring_claim "
                        + "(account_id INT NOT NULL PRIMARY KEY, "
                        + "claimed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
                reserved = LocalManager.executeUpdate(
                        "INSERT IGNORE INTO chibi_ring_claim(account_id) VALUES (?)",
                        player.getSession().userId) == 1;
                if (!reserved) {
                    Service.gI().sendThongBao(player, "Tài khoản đã nhận Vòng Chibi");
                    return;
                }
                Item ring = ItemService.gI().createNewItem(ITEM_ID);
                if (!InventoryService.gI().addItemBag(player, ring)) {
                    LocalManager.executeUpdate("DELETE FROM chibi_ring_claim WHERE account_id = ?",
                            player.getSession().userId);
                    Service.gI().sendThongBao(player, "Không thể nhận Vòng Chibi; hãy kiểm tra hành trang");
                    return;
                }
                delivered = true;
                InventoryService.gI().sendItemBags(player);
                Service.gI().sendThongBao(player,
                        "Đã nhận Vòng Chibi. Bấm Sử dụng để bật hoặc tắt");
            } catch (Exception e) {
                Logger.logException(ChibiRingService.class, e,
                        "Không thể phát Vòng Chibi cho " + player.name);
                if (reserved && !delivered) {
                    try {
                        LocalManager.executeUpdate("DELETE FROM chibi_ring_claim WHERE account_id = ?",
                                player.getSession().userId);
                    } catch (Exception ignored) {
                    }
                }
                Service.gI().sendThongBao(player, "Không thể nhận Vòng Chibi, vui lòng thử lại");
            }
        }
    }
}
