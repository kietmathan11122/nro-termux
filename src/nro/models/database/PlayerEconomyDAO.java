package nro.models.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import nro.models.data.LocalManager;
import nro.models.player.Inventory;
import nro.models.player.Player;
import nro.models.server.Client;
import nro.models.utils.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/** Persists the wallet and minigame progress in one existing player row. */
public final class PlayerEconomyDAO {
    private PlayerEconomyDAO() { }

    public static JSONObject copyGameData(Player player) {
        synchronized (player) {
            return copy(player.inventory.gameData);
        }
    }

    static JSONObject copy(JSONObject data) {
        Object value = JSONValue.parse(data.toJSONString());
        if (!(value instanceof JSONObject)) {
            throw new IllegalArgumentException("Invalid game progress");
        }
        return (JSONObject) value;
    }

    public static void loadGameData(Player player, JSONArray wallet) {
        player.inventory.gameData = decodeGameData(wallet);
        player.inventory.gameDataPersistenceBlocked = false;
    }

    static JSONObject decodeGameData(JSONArray wallet) {
        // Existing accounts have only the original five currency fields.
        if (wallet.size() <= 5) {
            return new JSONObject();
        }
        Object value = wallet.get(5);
        if (!(value instanceof JSONObject)) {
            throw new IllegalArgumentException("Invalid persisted game progress");
        }
        return copy((JSONObject) value);
    }

    /** Null deliberately preserves the database wallet after an uncertain write. */
    public static String serialize(Player player) {
        synchronized (player) {
            Inventory inv = player.inventory;
            if (inv.gameDataPersistenceBlocked) {
                return null;
            }
            return encodeWallet(Math.max(0, Math.min(Inventory.LIMIT_GOLD, inv.gold)),
                    inv.gem, inv.ruby, inv.coupon, inv.event, inv.gameData);
        }
    }

    static String encodeWallet(long gold, int gem, int ruby, int coupon, int event,
            JSONObject data) {
        JSONArray wallet = new JSONArray();
        wallet.add(gold);
        wallet.add(gem);
        wallet.add(ruby);
        wallet.add(coupon);
        wallet.add(event);
        wallet.add(data);
        return wallet.toJSONString();
    }

    /** Caller holds the player monitor across its rule checks and this commit. */
    public static boolean commit(Player player, long newGold, JSONObject data) {
        if (player == null || data == null || newGold < 0 || newGold > Inventory.LIMIT_GOLD) {
            return false;
        }
        synchronized (player) {
            if (player.inventory == null || player.idMark == null
                    || !player.idMark.isLoadedAllDataPlayer()
                    || player.beforeDispose
                    || player.inventory.gameDataPersistenceBlocked) {
                return false;
            }
            Inventory inv = player.inventory;
            JSONObject snapshot = copy(data);
            String wallet = encodeWallet(newGold, inv.gem, inv.ruby, inv.coupon, inv.event, snapshot);
            if (wallet.length() > 16384) {
                return false;
            }
            // Install the debit/credit before JDBC waits. Existing unrelated loot
            // rewards that arrive during the write remain in memory for autosave.
            // Autosave and every minigame commit share this player monitor.
            inv.gold = newGold;
            inv.gameData = snapshot;
            try (Connection connection = LocalManager.getConnection()) {
                persistWallet(connection, player.id, wallet);
                return true;
            } catch (Exception error) {
                // A lost DB acknowledgement may have committed already. Do not
                // roll back locally and later overwrite that committed wallet.
                inv.gameDataPersistenceBlocked = true;
                Logger.logException(PlayerEconomyDAO.class, error, "Wallet persistence failed for " + player.id);
                if (player.getSession() != null) {
                    Client.gI().kickSession(player.getSession());
                }
                return false;
            }
        }
    }

    static void persistWallet(Connection connection, long playerId, String wallet) throws Exception {
        if (!connection.getAutoCommit()) {
            throw new IllegalStateException("Wallet connection must use auto-commit");
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE player SET data_inventory = ? WHERE id = ?")) {
            statement.setString(1, wallet);
            statement.setLong(2, playerId);
            statement.setQueryTimeout(15);
            if (statement.executeUpdate() != 1) {
                throw new IllegalStateException("Player wallet row was not updated");
            }
        }
    }

    /** Wallet, fish materials, stones and modified gear are saved in one SQL update. */
    public static boolean commitBag(Player player, long newGold, JSONObject data) {
        synchronized (player) {
            Inventory inv = player.inventory;
            if (inv.gameDataPersistenceBlocked || newGold < 0 || newGold > Inventory.LIMIT_GOLD) return false;
            JSONObject snapshot = copy(data);
            String wallet = encodeWallet(newGold, inv.gem, inv.ruby, inv.coupon, inv.event, snapshot);
            JSONArray bag = new JSONArray();
            for (nro.models.item.Item item : inv.itemsBag) {
                JSONArray entry = new JSONArray();
                entry.add(item.isNotNullItem() ? item.template.id : -1);
                entry.add(item.isNotNullItem() ? item.quantity : 0);
                JSONArray options = new JSONArray();
                if (item.isNotNullItem()) for (nro.models.item.Item.ItemOption option : item.itemOptions) {
                    JSONArray values = new JSONArray();
                    values.add(option.optionTemplate.id); values.add(option.param);
                    values.add(option.darkInfusion ? 1 : 0); options.add(values.toJSONString());
                }
                entry.add(options.toJSONString()); entry.add(item.createTime);
                bag.add(entry.toJSONString());
            }
            inv.gold = newGold; inv.gameData = snapshot;
            try (Connection connection = LocalManager.getConnection();
                    PreparedStatement statement = connection.prepareStatement(
                            "UPDATE player SET data_inventory=?, items_bag=? WHERE id=?")) {
                if (!connection.getAutoCommit()) throw new IllegalStateException("Auto-commit required");
                statement.setString(1, wallet); statement.setString(2, bag.toJSONString());
                statement.setLong(3, player.id); statement.setQueryTimeout(15);
                if (statement.executeUpdate() != 1) throw new IllegalStateException("Player row missing");
                return true;
            } catch (Exception error) {
                inv.gameDataPersistenceBlocked = true;
                Logger.logException(PlayerEconomyDAO.class, error, "Lab persistence failed for " + player.id);
                if (player.getSession() != null) Client.gI().kickSession(player.getSession());
                return false;
            }
        }
    }
}
