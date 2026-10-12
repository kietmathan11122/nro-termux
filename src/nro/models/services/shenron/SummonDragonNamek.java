package nro.models.services.shenron;

import nro.models.services.shenron.SummonDragon;
import nro.models.network.Message;
import nro.models.consts.ConstNpc;
import nro.models.database.MrBlue;
import nro.models.database.PlayerDAO;
import nro.models.item.Item;
import nro.models.item.Item.ItemOption;

import java.util.List;
import nro.models.map.Zone;
import nro.models.player.Player;
import nro.models.server.Client;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.map.service.NpcService;
import nro.models.services.Service;
import nro.models.utils.Util;

/**
 *
 * @author By Mr Blue
 *
 */
public class SummonDragonNamek {

    public static final byte DRAGON_PORUNGA = 1;
    private static SummonDragonNamek instance;

    public static final byte WISHED = 0;
    public static final byte TIME_UP = 1;
    private boolean isShenronAppear;
    public Player playerSummonShenron;
    private int playerSummonShenronId;
    private Zone mapShenronAppear;
    private int menuShenron;
    private byte select;
    private final Thread update;
    private boolean active;
    public boolean isPlayerDisconnect;
    private long lastTimeShenronWait;
    private final int timeShenronWait = 300000;
    private boolean isTask34Summon;

    public static SummonDragonNamek gI() {
        if (instance == null) {
            instance = new SummonDragonNamek();
        }
        return instance;
    }

    private SummonDragonNamek() {
        this.update = new Thread(() -> {
            while (active) {
                try {
                    if (isShenronAppear) {
                        if (isPlayerDisconnect) {
                            List<Player> players = mapShenronAppear.getPlayers();
                            for (Player plMap : players) {
                                if (plMap.isPl() && plMap.id == playerSummonShenronId) {
                                    playerSummonShenron = plMap;
                                    reSummonShenron();
                                    isPlayerDisconnect = false;
                                    break;
                                }
                            }

                        }
                        if (Util.canDoWithTime(lastTimeShenronWait, timeShenronWait)) {
                            shenronLeave(playerSummonShenron, TIME_UP);
                        }
                    }
                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        this.active();
    }

    private void active() {
        if (!active) {
            active = true;
            this.update.start();
        }
    }

    public void summonNamec(Player pl) {
        if (pl.zone.map.mapId == 7) {
            playerSummonShenron = pl;
            playerSummonShenronId = (int) pl.id;
            mapShenronAppear = pl.zone;
            lastTimeShenronWait = System.currentTimeMillis();
            sendNotifyShenronNamekAppear();
            activeShenron(pl, true, DRAGON_PORUNGA);
            sendBlackGokuhesNamec(pl);
        } else {
            Service.gI().sendThongBao(pl, "Không thể thực hiện");
        }
    }

    private void reSummonShenron() {
        activeShenron(playerSummonShenron, true, DRAGON_PORUNGA);
        if (isTask34Summon) {
            sendShenronTask34Wish(playerSummonShenron);
        } else {
            sendBlackGokuhesNamec(playerSummonShenron);
        }
    }

    public void summonNamecTask(Player pl) {
        if (pl.zone.map.mapId == 7) {
            playerSummonShenron = pl;
            playerSummonShenronId = (int) pl.id;
            mapShenronAppear = pl.zone;
            lastTimeShenronWait = System.currentTimeMillis();
            isTask34Summon = true;
            sendNotifyShenronNamekAppear();
            activeShenron(pl, true, DRAGON_PORUNGA);
            sendShenronTask34Wish(pl);
        } else {
            Service.gI().sendThongBao(pl, "Không thể thực hiện");
        }
    }

    private void activeShenron(Player pl, boolean appear, byte type) {
        Message msg;
        try {
            msg = new Message(-83);
            msg.writer().writeByte(appear ? 0 : (byte) 1);
            if (appear) {
                msg.writer().writeShort(pl.zone.map.mapId);
                msg.writer().writeShort(pl.zone.map.bgId);
                msg.writer().writeByte(pl.zone.zoneId);
                msg.writer().writeInt((int) pl.id);
                msg.writer().writeUTF("null");
                msg.writer().writeShort(pl.location.x);
                msg.writer().writeShort(pl.location.y);
                msg.writer().writeByte(type);
                isShenronAppear = true;
            }
            Service.gI().sendMessAllPlayer(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendNotifyShenronNamekAppear() {
        Message msg = null;
        try {
            msg = new Message(-25);
            msg.writer().writeUTF(playerSummonShenron.name + " vừa gọi rồng thần namek tại "
                    + playerSummonShenron.zone.map.mapName + " khu vực " + playerSummonShenron.zone.zoneId);
            Service.gI().sendMessAllPlayerIgnoreMe(playerSummonShenron, msg);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    public void confirmWish() {
        switch (this.menuShenron) {
            case ConstNpc.SHOW_SHENRON_TASK_34_CONFIRM:
            case ConstNpc.SHENRON_TASK_34_CONFIRM:
                try {
                    nro.models.services_dungeon.NgocRongNamecService.gI().removeStoneNrNamec();
                    nro.models.services_dungeon.NgocRongNamecService.gI().initNgocRongNamec((byte) 0);
                    nro.models.services_dungeon.NgocRongNamecService.gI().isReinit = false;
                    Service.gI().sendThongBaoAllPlayer("Hành tinh Namek đã được hồi sinh! Ngọc Rồng Namek đã xuất hiện trở lại!");
                    
                    if (playerSummonShenron.playerTask.taskMain.id == 34 && playerSummonShenron.playerTask.taskMain.index == 11) {
                        playerSummonShenron.playerTask.taskMain.index = (byte) (playerSummonShenron.playerTask.taskMain.subTasks.size() - 1);
                        playerSummonShenron.playerTask.taskMain.subTasks.get(playerSummonShenron.playerTask.taskMain.index).count = 
                                (short) (playerSummonShenron.playerTask.taskMain.subTasks.get(playerSummonShenron.playerTask.taskMain.index).maxCount - 1);
                        nro.models.services.TaskService.gI().doneTask(playerSummonShenron, nro.models.services.TaskService.gI().getIdTask(playerSummonShenron));
                        
                        playerSummonShenron.zone.map.wayPoints.stream().filter(w -> w.goMap == 227).findFirst().ifPresent(w -> w.isEnter = true);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case ConstNpc.SHOW_SHENRON_NAMEK_CONFIRM:
                try {
                    nro.models.services_dungeon.NgocRongNamecService.gI().removeStoneNrNamec();
                    nro.models.services_dungeon.NgocRongNamecService.gI().initNgocRongNamec((byte) 0);
                    nro.models.services_dungeon.NgocRongNamecService.gI().isReinit = false;
                    Service.gI().sendThongBaoAllPlayer("Hành tinh Namek đã được hồi sinh! Ngọc Rồng Namek đã xuất hiện trở lại!");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
        shenronLeave(this.playerSummonShenron, WISHED);
    }

    public void showConfirmShenron(Player pl, int menu, byte select) {
        this.menuShenron = menu;
        this.select = select;
        String wish = null;
        switch (menu) {
           case ConstNpc.SHOW_SHENRON_NAMEK_CONFIRM:
                wish = "Hồi sinh\nNamek";
                break;
            case ConstNpc.SHOW_SHENRON_TASK_34_CONFIRM:
                wish = "Hồi sinh\nhành tinh\nNamek";
                break;
        }
        if (menu == ConstNpc.SHOW_SHENRON_TASK_34_CONFIRM) {
            NpcService.gI().createMenuRongThieng(pl, ConstNpc.SHENRON_TASK_34_CONFIRM, "Ngươi có chắc muốn chọn điều ước này?", wish, "Từ chối");
        } else {
            NpcService.gI().createMenuRongThieng(pl, ConstNpc.SHENRON_NAMEK_CONFIRM, "Ngươi có chắc muốn ước?", wish, "Từ chối");
        }
    }

    public void sendBlackGokuhesNamec(Player pl) {
        NpcService.gI().createMenuRongThieng(
            pl,
            ConstNpc.SHOW_SHENRON_NAMEK_CONFIRM,
            "Ta sẽ ban cho cả bang hội ngươi 1 điều ước, ngươi có 5 phút, hãy suy nghĩ thật kỹ trước khi quyết định",
            "Hồi sinh\nNamek"
        );
    }

    public void sendShenronTask34Wish(Player pl) {
        NpcService.gI().createMenuRongThieng(
            pl,
            ConstNpc.SHOW_SHENRON_TASK_34_CONFIRM,
            "Ngươi đã giải cứu hành tinh Namek thành công! Ta sẽ ban cho ngươi 1 điều ước, hãy chọn đi!",
            "Hồi sinh\nhành tinh\nNamek"
        );
    }

    public void shenronLeave(Player pl, byte type) {
        if (type == WISHED) {
            //Điều ước Bùa mạnh mẽ cho tất cả trong 7 ngày của các con đã được thực hiện...tạm biệt
            NpcService.gI().createTutorial(pl, 0, "Điều ước của ngươi đã được thực hiện...tạm biệt");
        } else {
            NpcService.gI().createMenuRongThieng(pl, ConstNpc.IGNORE_MENU, "Ta buồn ngủ quá rồi\nHẹn gặp ngươi lần sau, ta đi đây, bái bai");
        }
        activeShenron(pl, false, SummonDragon.DRAGON_SHENRON);
        this.isShenronAppear = false;
        this.menuShenron = -1;
        this.select = -1;
        this.playerSummonShenron = null;
        this.playerSummonShenronId = -1;
        this.mapShenronAppear = null;
        this.isTask34Summon = false;
    }
}
