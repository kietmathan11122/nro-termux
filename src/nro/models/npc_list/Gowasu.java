package nro.models.npc_list;

import nro.models.combine.CombineService;
import nro.models.consts.ConstNpc;
import nro.models.consts.ConstPlayer;
import nro.models.database.PlayerDAO;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.services.PlayerService;
import nro.models.services.Service;
import nro.models.utils.Util;

/** Trái Đất đạt 100 tỷ có thể chuyển sinh thành Heart. */
public class Gowasu extends Npc {
    private static final long REQUIRED_POWER = 100_000_000_000L;
    private static final long RESET_POWER = 2_000L;
    private static final int STAT_REWARD = 500;
    private static final int MENU_CONFIRM_REBIRTH = 119_001;

    public Gowasu(int mapId, int status, int cx, int cy, int tempId, int avatar) {
        super(mapId, status, cx, cy, tempId, avatar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (!canOpenNpc(player)) return;
        String heart = player.gender == ConstPlayer.TRAI_DAT
                ? "Trái Đất sẽ chuyển sinh thành Heart và nhận bộ kỹ năng Lập Phương.\n" : "";
        createOtherMenu(player, ConstNpc.BASE_MENU,
                "Ta là Tổ sư Gowasu.\nKhi đạt 100 tỷ sức mạnh, con có thể chuyển sinh.\n"
                + "Mỗi lần chuyển sinh nhận vĩnh viễn +500 HP, +500 KI và +500 sức đánh gốc.\n"
                + heart + "Sức mạnh hiện tại: " + Util.numberToMoney(player.nPoint.power),
                "Chuyển sinh", "Khắc dòng\nNhật Nguyệt", "Từ chối");
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) return;
        if (player.idMark.isBaseMenu()) {
            if (select == 0) openConfirmMenu(player);
            else if (select == 1)
                CombineService.gI().openTabCombine(player, CombineService.EP_TRANG_BI_NHAT_NGUYET);
            return;
        }
        if (player.idMark.getIndexMenu() == MENU_CONFIRM_REBIRTH && select == 0) rebirth(player);
        else if (player.idMark.getIndexMenu() == ConstNpc.MENU_START_COMBINE && select == 0
                && player.combineNew.typeCombine == CombineService.EP_TRANG_BI_NHAT_NGUYET)
            CombineService.gI().startCombine(player);
    }

    private void openConfirmMenu(Player player) {
        if (player.nPoint.power < REQUIRED_POWER) {
            createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Con chưa đủ 100 tỷ sức mạnh.\nCòn thiếu: "
                    + Util.numberToMoney(REQUIRED_POWER - player.nPoint.power) + " sức mạnh.", "Đóng");
            return;
        }
        createOtherMenu(player, MENU_CONFIRM_REBIRTH,
                "Sau khi chuyển sinh, sức mạnh và tiềm năng sẽ trở về 2.000.\n"
                + "Con nhận vĩnh viễn +500 HP, +500 KI và +500 sức đánh gốc.\n"
                + (player.gender == ConstPlayer.TRAI_DAT
                    ? "Nhân vật sẽ trở thành Heart và nhận bộ kỹ năng Lập Phương.\n" : "")
                + "Con chắc chắn muốn chuyển sinh?", "Đồng ý", "Từ chối");
    }

    private void rebirth(Player player) {
        synchronized (player) {
            if (player.nPoint.power < REQUIRED_POWER) {
                Service.gI().sendThongBao(player, "Không đủ 100 tỷ sức mạnh để chuyển sinh.");
                return;
            }
            if (player.nPoint.hpg > Integer.MAX_VALUE - STAT_REWARD
                    || player.nPoint.mpg > Integer.MAX_VALUE - STAT_REWARD
                    || player.nPoint.dameg > Integer.MAX_VALUE - STAT_REWARD) {
                Service.gI().sendThongBao(player, "Chỉ số gốc đã đạt giới hạn, không thể chuyển sinh thêm.");
                return;
            }
            player.nPoint.hpg += STAT_REWARD;
            player.nPoint.mpg += STAT_REWARD;
            player.nPoint.dameg += STAT_REWARD;
            player.nPoint.power = RESET_POWER;
            player.nPoint.tiemNang = RESET_POWER;
            if (player.gender == ConstPlayer.TRAI_DAT) {
                player.adminSpineSkinIndex = -1;
                player.head = 2139;
                player.heartsReborn = true;
                nro.models.services.HeartsRebirth.ensureSkills(player, true);
            }
            player.nPoint.calPoint();
            player.nPoint.hp = player.nPoint.hpMax;
            player.nPoint.mp = player.nPoint.mpMax;
            Service.gI().point(player);
            PlayerService.gI().sendInfoHpMpMoney(player);
            Service.gI().Send_Caitrang(player);
            PlayerDAO.updatePlayer(player);
        }
        Service.gI().sendThongBao(player,
                "Chuyển sinh thành công! +500 HP, +500 KI và +500 sức đánh gốc."
                + (player.gender == ConstPlayer.TRAI_DAT
                    ? " Nhân vật đã trở thành Heart và nhận bộ kỹ năng Lập Phương." : ""));
    }
}
