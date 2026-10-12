package nro.models.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import nro.models.data.LocalManager;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.utils.Logger;

/**
 * Đổi số dư Cá/VND tại Bò Mộng. Một điểm là một đơn vị account.vnd.
 */
public final class PointExchangeService {
    private static final PointExchangeService INSTANCE = new PointExchangeService();
    private static final int MAX_POINTS_PER_TRADE = 2_000_000;
    private static final int GOLD_BARS_PER_POINT = 10;
    private static final int GEMS_PER_POINT = 1_000;

    public static PointExchangeService gI() {
        return INSTANCE;
    }

    private PointExchangeService() {
    }

    public int refreshBalance(Player player) {
        try (Connection con = LocalManager.getConnection()) {
            int balance = readBalance(con, player);
            player.getSession().vnd = balance;
            return balance;
        } catch (Exception e) {
            Logger.logException(PointExchangeService.class, e, "Không thể đọc số điểm của " + player.name);
            Service.gI().sendThongBao(player, "Không thể đọc số điểm, vui lòng thử lại");
            return -1;
        }
    }

    public void exchangeGoldBars(Player player, String[] input) {
        exchange(player, input, false);
    }

    public void exchangeGems(Player player, String[] input) {
        exchange(player, input, true);
    }

    private void exchange(Player player, String[] input, boolean gems) {
        if (player == null || player.getSession() == null || player.zone == null
                || !isHomeMap(player.zone.map.mapId)) {
            return;
        }
        int points;
        try {
            points = Integer.parseInt(input[0].trim());
        } catch (Exception e) {
            Service.gI().sendThongBao(player, "Hãy nhập số điểm hợp lệ");
            return;
        }
        if (points <= 0 || points > MAX_POINTS_PER_TRADE) {
            Service.gI().sendThongBao(player, "Mỗi lần đổi từ 1 đến 2.000.000 điểm");
            return;
        }
        int reward = points * (gems ? GEMS_PER_POINT : GOLD_BARS_PER_POINT);
        synchronized (player) {
            Item bars = null;
            if (gems) {
                if (player.inventory.gem < 0 || (long) player.inventory.gem + reward > 2_000_000_000L) {
                    Service.gI().sendThongBao(player, "Ngọc đã gần giới hạn, hãy tiêu bớt trước khi đổi");
                    return;
                }
            } else {
                bars = ItemService.gI().createNewItem((short) 457, reward);
                if (bars.template == null) {
                    Service.gI().sendThongBao(player, "Thỏi vàng chưa được tải, vui lòng báo quản trị viên");
                    return;
                }
                bars.itemOptions.add(new Item.ItemOption(73, 0));
                boolean canStack = false;
                for (Item existing : player.inventory.itemsBag) {
                    if (existing.isNotNullItem() && existing.template.id == 457
                            && existing.quantity < 100_000_000
                            && InventoryService.checkListsEqual(existing.itemOptions, bars.itemOptions)) {
                        canStack = true;
                        break;
                    }
                }
                if (!canStack && InventoryService.gI().getCountEmptyBag(player) == 0) {
                    Service.gI().sendThongBao(player, "Hành trang đã đầy, không thể nhận thỏi vàng");
                    return;
                }
            }

            boolean debited = false;
            boolean rewarded = false;
            try (Connection con = LocalManager.getConnection()) {
                int balance = readBalance(con, player);
                player.getSession().vnd = balance;
                if (balance < points) {
                    Service.gI().sendThongBao(player, "Bạn không đủ điểm để đổi");
                    return;
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE account SET vnd = vnd - ? WHERE id = ? AND vnd >= ?")) {
                    ps.setInt(1, points);
                    ps.setInt(2, player.getSession().userId);
                    ps.setInt(3, points);
                    if (ps.executeUpdate() != 1) {
                        player.getSession().vnd = readBalance(con, player);
                        Service.gI().sendThongBao(player, "Số điểm đã thay đổi, vui lòng thử lại");
                        return;
                    }
                    debited = true;
                }
                player.getSession().vnd = balance - points;
                if (gems) {
                    player.inventory.gem += reward;
                    rewarded = true;
                    Service.gI().sendMoney(player);
                } else if (!InventoryService.gI().addItemBag(player, bars)) {
                    throw new IllegalStateException("Không thể thêm thỏi vàng vào hành trang");
                } else {
                    rewarded = true;
                    InventoryService.gI().sendItemBags(player);
                }
                Service.gI().sendThongBao(player, "Đã đổi " + points + " điểm lấy "
                        + reward + (gems ? " ngọc" : " thỏi vàng"));
            } catch (Exception e) {
                Logger.logException(PointExchangeService.class, e, "Lỗi đổi điểm của " + player.name);
                if (debited && !rewarded) {
                    try (Connection refundConnection = LocalManager.getConnection()) {
                        refund(refundConnection, player, points);
                    } catch (Exception refundError) {
                        Logger.logException(PointExchangeService.class, refundError,
                                "CẦN HOÀN ĐIỂM CHO TÀI KHOẢN " + player.getSession().userId
                                + ": " + points);
                        Service.gI().sendThongBao(player, "Giao dịch lỗi, hãy báo quản trị viên để kiểm tra điểm");
                        return;
                    }
                }
                Service.gI().sendThongBao(player, "Đổi điểm thất bại, điểm đã được hoàn lại nếu đã trừ");
            }
        }
    }

    private int readBalance(Connection con, Player player) throws Exception {
        try (PreparedStatement ps = con.prepareStatement("SELECT vnd FROM account WHERE id = ?")) {
            ps.setInt(1, player.getSession().userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Math.max(0, rs.getInt(1));
                }
            }
        }
        throw new IllegalStateException("Không tìm thấy tài khoản " + player.getSession().userId);
    }

    private void refund(Connection con, Player player, int points) throws Exception {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE account SET vnd = vnd + ? WHERE id = ?")) {
            ps.setInt(1, points);
            ps.setInt(2, player.getSession().userId);
            ps.executeUpdate();
        }
        player.getSession().vnd = readBalance(con, player);
    }

    private boolean isHomeMap(int mapId) {
        return mapId == 47 || mapId == 84 || mapId == 21 || mapId == 22 || mapId == 23;
    }
}
