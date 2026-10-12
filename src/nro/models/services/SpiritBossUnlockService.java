package nro.models.services;

import nro.models.data.LocalManager;
import nro.models.data.LocalResultSet;
import nro.models.player.Player;
import nro.models.utils.Logger;

/** Ghi nhận đệ tử Tinh Linh gió cố định sau khi hạ boss Tinh Linh. */
public final class SpiritBossUnlockService {

    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS spirit_boss_unlock ("
            + "player_id BIGINT NOT NULL PRIMARY KEY, "
            + "unlocked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)";

    private SpiritBossUnlockService() {
    }

    public static boolean isUnlocked(Player player) {
        if (player == null || player.isPet || player.isBoss || player.isNewPet) return false;
        // Giữ quyền cho những nhân vật đã chọn Tinh Linh gió trước khi có boss.
        if (player.pet != null && player.pet.companionForm == SpiritDiscipleRules.WIND) {
            rememberExistingForm(player);
            return true;
        }
        LocalResultSet result = null;
        try {
            LocalManager.executeUpdate(CREATE_TABLE);
            result = LocalManager.executeQuery(
                    "SELECT player_id FROM spirit_boss_unlock WHERE player_id = ? LIMIT 1", player.id);
            return result.next();
        } catch (Exception e) {
            Logger.logException(SpiritBossUnlockService.class, e);
            return false;
        } finally {
            if (result != null) result.close();
        }
    }

    public static void rememberExistingForm(Player player) {
        if (player == null || player.pet == null
                || player.pet.companionForm != SpiritDiscipleRules.WIND) return;
        try {
            LocalManager.executeUpdate(CREATE_TABLE);
            LocalManager.executeUpdate(
                    "INSERT IGNORE INTO spirit_boss_unlock(player_id) VALUES (?)", player.id);
        } catch (Exception e) {
            Logger.logException(SpiritBossUnlockService.class, e);
        }
    }

    public static void award(Player player) {
        if (player == null || player.isPet || player.isBoss || player.isNewPet || player.isBot) return;
        synchronized (player) {
            rememberExistingForm(player);
            try {
                LocalManager.executeUpdate(CREATE_TABLE);
                int inserted = LocalManager.executeUpdate(
                        "INSERT IGNORE INTO spirit_boss_unlock(player_id) VALUES (?)", player.id);
                if (inserted == 0) {
                    SpiritDiscipleService.grantBossWind(player);
                    Service.gI().sendThongBao(player, "Bạn đã sở hữu đệ tử Tinh Linh gió.");
                    return;
                }
                SpiritDiscipleService.grantBossWind(player);
                Service.gI().sendThongBao(player, "Bạn đã nhận đệ tử Tinh Linh gió!");
            } catch (Exception e) {
                Logger.logException(SpiritBossUnlockService.class, e);
                Service.gI().sendThongBao(player,
                        "Chưa thể trao Tinh Linh gió; hãy thử săn boss lần nữa.");
            }
        }
    }
}
