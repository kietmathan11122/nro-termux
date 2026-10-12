package nro.models.services;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;
import nro.models.database.PlayerEconomyDAO;
import nro.models.network.Message;
import nro.models.player.Inventory;
import nro.models.player.Player;
import nro.models.utils.Logger;
import org.json.simple.JSONObject;

/** Command -34 / subtype 30. No client-supplied reward, amount, or deadline. */
public final class FishingLabService {
    private static final FishingLabService INSTANCE = new FishingLabService();
    private static final String STATE_KEY = "fishing";

    private FishingLabService() { }
    public static FishingLabService gI() { return INSTANCE; }

    public void handleMessage(Player player, Message message) {
        if (!isLoggedIn(player)) return;
        synchronized (player) {
            if (!isLoggedIn(player)) return;
            FishingLabRules.State state;
            try {
                state = FishingLabRules.fromJson(player.inventory.gameData.get(STATE_KEY));
            } catch (RuntimeException invalidState) {
                Logger.logException(FishingLabService.class, invalidState, "Invalid fishing progress for " + player.id);
                Service.gI().sendThongBao(player, "Dữ liệu câu cá chưa đọc được. Hãy liên hệ quản trị viên.");
                return;
            }
            int action;
            int level = 0;
            try {
                action = message.reader().readUnsignedByte();
                if (action == 5 || action == 6) level = message.reader().readUnsignedByte();
                if (action > 7 || message.reader().available() != 0) {
                    sendState(player, state, "Yêu cầu không hợp lệ.");
                    return;
                }
            } catch (IOException | RuntimeException malformed) {
                sendState(player, state, "Yêu cầu không hợp lệ.");
                return;
            }
            long observedAt=System.currentTimeMillis();
            if(state.pendingType>=0 && observedAt>=state.escapeAt()) {
                FishingLabRules.State expired=FishingLabRules.cancel(state);
                JSONObject expiredData=PlayerEconomyDAO.copyGameData(player); expiredData.put(STATE_KEY,expired.toJson());
                if(!PlayerEconomyDAO.commit(player,player.inventory.gold,expiredData))return;
                sendState(player,expired,"Cá đã tuột! Hãy thả cần lại."); return;
            }
            if (action == 0) {
                sendState(player, state, "");
                return;
            }
            if (player.isDie() || player.zone == null) {
                sendState(player, state, "Hãy hồi sinh và vào bản đồ để sử dụng chức năng này.");
                return;
            }
            long now = System.currentTimeMillis();
            if (now - player.inventory.lastFishingActionAt < 250) {
                sendState(player, state, "Bạn thao tác quá nhanh. Hãy thử lại sau một chút.");
                return;
            }
            player.inventory.lastFishingActionAt = now;
            long gold = player.inventory.gold;
            if (gold < 0 || gold > Inventory.LIMIT_GOLD) {
                sendState(player, state, "Số vàng hiện tại chưa hợp lệ. Hãy đăng nhập lại.");
                return;
            }
            FishingLabRules.State next;
            String notice;
            try {
                switch (action) {
                    case 1:
                        if (gold < FishingLabRules.CAST_COST) {
                            throw new IllegalArgumentException("Cần 1.000 vàng mua mồi cho mỗi lượt câu.");
                        }
                        next = FishingLabRules.cast(state, now, ThreadLocalRandom.current().nextInt(10000));
                        gold -= FishingLabRules.CAST_COST;
                        notice = "Đã thả câu! Chờ cá cắn rồi bấm Giật cần để kéo cá lên.";
                        break;
                    case 2:
                        next = FishingLabRules.hook(state, now);
                        notice = next.pendingType<0 ? "Bạn nhận được 1 " + FishingLabRules.FISH_NAMES[state.pendingType] + "." : "Đang kéo cá: " + next.hookHits + "/" + next.requiredHits() + " nhịp.";
                        break;
                    case 3:
                        long value = state.saleValue();
                        if (value > Inventory.LIMIT_GOLD - gold) {
                            throw new IllegalArgumentException("Túi vàng không đủ chỗ để bán hết cá. Hãy sử dụng bớt vàng.");
                        }
                        next = FishingLabRules.sellAll(state);
                        gold += value;
                        notice = "Đã bán " + state.totalFish() + " cá, nhận " + value + " vàng.";
                        break;
                    case 4:
                        throw new IllegalArgumentException("Mua cần câu tại cửa hàng trong mục Câu cá.");
                    case 5:
                        next = FishingLabRules.buyRod(state, level);
                        int price = FishingLabRules.ROD_PRICES[level - 1];
                        if (gold < price) throw new IllegalArgumentException("Bạn chưa đủ vàng mua cần câu.");
                        gold -= price;
                        notice = "Đã mua và chọn cần cấp " + level + ".";
                        break;
                    case 6:
                        next = FishingLabRules.selectRod(state, level);
                        notice = "Đã chọn cần cấp " + level + ".";
                        break;
                    case 7:
                        if(state.pendingType<0)throw new IllegalArgumentException("Bạn chưa thả câu.");
                        next=FishingLabRules.cancel(state); notice="Đã thu cần, hủy lượt câu."; break;
                    default:
                        throw new IllegalArgumentException("Yêu cầu không hợp lệ.");
                }
            } catch (IllegalArgumentException rejected) {
                sendState(player, state, rejected.getMessage());
                return;
            }
            JSONObject data = PlayerEconomyDAO.copyGameData(player);
            data.put(STATE_KEY, next.toJson());
            if (!PlayerEconomyDAO.commit(player, gold, data)) {
                if (isLoggedIn(player)) sendState(player, state, "Chưa lưu được dữ liệu. Hãy đăng nhập lại.");
                return;
            }
            Service.gI().sendMoney(player);
            sendState(player, next, notice);
        }
    }

    private boolean isLoggedIn(Player player) {
        return player != null && player.isPl() && player.inventory != null
                && !player.beforeDispose
                && !player.inventory.gameDataPersistenceBlocked && player.idMark != null
                && player.idMark.isLoadedAllDataPlayer() && player.getSession() != null
                && player.getSession().player == player && player.getSession().isConnected();
    }

    private void sendState(Player player, FishingLabRules.State state, String notice) {
        Message response = null;
        try {
            long now = System.currentTimeMillis();
            response = new Message(-34);
            response.writer().writeByte(30);
            response.writer().writeByte(3);
            response.writer().writeByte(state.rodLevel);
            response.writer().writeByte(state.pendingStatus(now));
            response.writer().writeInt(state.remainingSeconds(now));
            response.writer().writeInt(FishingLabRules.CAST_COST);
            response.writer().writeInt(0);
            response.writer().writeInt(0);
            response.writer().writeLong(player.inventory.gold);
            response.writer().writeByte(FishingLabRules.FISH_NAMES.length);
            for (int i = 0; i < FishingLabRules.FISH_NAMES.length; i++) {
                response.writer().writeUTF(FishingLabRules.FISH_NAMES[i]);
                response.writer().writeInt(state.fishCount(i));
                response.writer().writeInt(FishingLabRules.SELL_PRICES[i]);
            }
            response.writer().writeUTF(notice == null ? "" : notice);
            response.writer().writeByte(state.ownedRods);
            response.writer().writeByte(7);
            for (int price : FishingLabRules.ROD_PRICES) response.writer().writeInt(price);
            response.writer().writeByte(state.hookHits); response.writer().writeByte(state.requiredHits());
            response.writer().writeInt(state.pendingType<0?0:(int)Math.max(0,(state.escapeAt()-now+999)/1000));
            player.sendMessage(response);
        } catch (IOException error) {
            Logger.logException(FishingLabService.class, error);
        } finally {
            if (response != null) response.cleanup();
        }
    }
}
