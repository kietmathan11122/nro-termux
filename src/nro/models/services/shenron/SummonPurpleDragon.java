package nro.models.services.shenron;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import nro.models.item.Item;
import nro.models.item.Item.ItemOption;
import nro.models.consts.ConstNpc;
import nro.models.consts.ConstPlayer;
import nro.models.map.Zone;
import nro.models.map.service.NpcService;
import nro.models.player.Player;
import nro.models.services.Service;
import nro.models.services.ItemService;
import nro.models.services.PlayerService;
import nro.models.services.InventoryService;
import nro.models.services.ItemTimeService;
import nro.models.utils.Util;
import nro.models.utils.Logger;
import nro.models.server.Maintenance;
import nro.models.network.Message;

public class SummonPurpleDragon {

    public static final byte WISHED = 0;
    public static final byte TIME_UP = 1;

    public static final byte DRAGON_PURPLE = 2; // custom type for Purple Dragon

    public static final short NGOC_RONG_1_SAO = 2039;
    public static final short NGOC_RONG_2_SAO = 2040;
    public static final short NGOC_RONG_3_SAO = 2041;
    public static final short NGOC_RONG_4_SAO = 2042;
    public static final short NGOC_RONG_5_SAO = 2043;
    public static final short NGOC_RONG_6_SAO = 2044;
    public static final short NGOC_RONG_7_SAO = 2045;

    public static final String SUMMON_TUTORIAL
            = "Cách gọi Rồng Thần Tím. Gọi từ Ngọc Rồng Tím 1 sao, 2 sao, hoặc 3 sao\n"
            + "Để gọi Rồng 1 sao cần Ngọc Tím từ 1 sao đến 7 sao\n"
            + "Để gọi Rồng 2 sao cần Ngọc Tím từ 2 sao đến 7 sao\n"
            + "Để gọi Rồng 3 sao cần Ngọc Tím từ 3 sao đến 7 sao\n"
            + "Ngọc rồng sẽ mất ngay khi gọi rồng dù bạn có ước hay không\n"
            + "Quá 5 phút nếu không ước rồng thần sẽ bay mất";

    public static final String SHENRON_SAY
            = "Ta là Rồng Thần Tím! Ta sẽ ban cho ngươi 1 điều ước, ngươi có 5 phút, hãy suy nghĩ thật kỹ.";

    public static final String[] WISHES_PAGE_1 = new String[]{
        "1 tỷ\nSức mạnh",
        "Nâng cấp\nRada\n1 Cấp",
        "1000 Ngọc",
        "Tăng 10%\nSĐ, HP, KI\n(30 Phút)",
        "Xem tiếp..."
    };

    public static final String[] WISHES_PAGE_2 = new String[]{
        "Danh hiệu\nngẫu nhiên",
        "Tăng 20%\nMay mắn\n(100 Phút)",
        "Quay lại"
    };

    private static SummonPurpleDragon instance;
    private final Map<Player, Byte> pl_dragonStar;
    private long lastTimeDragonAppeared;
    private long lastTimeDragonWait;
    private final int timeResummonDragon = 600000; // 10 minutes
    private boolean isDragonAppear;
    private final int timeDragonWait = 300000; // 5 minutes

    private final Thread update;
    private boolean active;

    public boolean isPlayerDisconnect;
    public Player playerSummon;
    private int playerSummonId;
    private Zone mapDragonAppear;
    private byte dragonStar;
    private int menuId;
    private byte select;

    private SummonPurpleDragon() {
        this.pl_dragonStar = new HashMap<>();
        this.update = new Thread(() -> {
            while (active && !Maintenance.isRunning) {
                try {
                    if (isDragonAppear) {
                        if (isPlayerDisconnect) {
                            List<Player> players = mapDragonAppear.getPlayers();
                            for (Player plMap : players) {
                                if (plMap.isPl() && plMap.id == playerSummonId) {
                                    playerSummon = plMap;
                                    reSummonDragon();
                                    isPlayerDisconnect = false;
                                    break;
                                }
                            }
                        }
                        if (Util.canDoWithTime(lastTimeDragonWait, timeDragonWait)) {
                            purpleDragonLeave(playerSummon, TIME_UP);
                        }
                    }
                    Thread.sleep(1000);
                } catch (Exception e) {
                    Logger.logException(SummonPurpleDragon.class, e);
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

    public static SummonPurpleDragon gI() {
        if (instance == null) {
            instance = new SummonPurpleDragon();
        }
        return instance;
    }

    public boolean isPurpleDragonAppear() {
        return isDragonAppear;
    }

    public Player getPlayerSummon() {
        return playerSummon;
    }

    public void openMenuSummon(Player pl, byte dragonBallStar) {
        this.pl_dragonStar.put(pl, dragonBallStar);
        NpcService.gI().createMenuConMeo(pl, ConstNpc.SUMMON_PURPLE_DRAGON, -1, "Bạn muốn gọi Rồng Thần Tím?",
                "Hướng dẫn", "Gọi Rồng Thần Tím " + dragonBallStar + " Sao");
    }

    public void summonPurpleDragon(Player pl) {
        if (pl.zone.map.mapId == 206 || pl.zone.map.mapId == 207) {
            if (checkPurpleDragonBalls(pl)) {
                if (isDragonAppear) {
                    Service.gI().sendThongBao(pl, "Rồng Thần Tím đang được người khác gọi!");
                    return;
                }

                if (Util.canDoWithTime(lastTimeDragonAppeared, timeResummonDragon)) {
                    playerSummon = pl;
                    playerSummonId = (int) pl.id;
                    mapDragonAppear = pl.zone;
                    byte star = pl_dragonStar.get(playerSummon);
                    this.dragonStar = star;
                    int begin = NGOC_RONG_1_SAO;
                    switch (star) {
                        case 2:
                            begin = NGOC_RONG_2_SAO;
                            break;
                        case 3:
                            begin = NGOC_RONG_3_SAO;
                            break;
                    }
                    for (int i = begin; i <= NGOC_RONG_7_SAO; i++) {
                        try {
                            InventoryService.gI().subQuantityItemsBag(pl, InventoryService.gI().findItemBag(pl, i), 1);
                        } catch (Exception ex) {
                        }
                    }
                    InventoryService.gI().sendItemBags(pl);
                    sendNotifyPurpleDragonAppear();
                    activePurpleDragon(pl, true, DRAGON_PURPLE);
                    sendPurpleDragonMenu(pl);
                } else {
                    int timeLeft = (int) ((timeResummonDragon - (System.currentTimeMillis() - lastTimeDragonAppeared)) / 1000);
                    Service.gI().sendThongBao(pl, "Vui lòng đợi " + (timeLeft < 7200 ? timeLeft + " giây" : timeLeft / 60 + " phút") + " nữa");
                }
            }
        } else {
            Service.gI().sendThongBao(pl, "Chỉ được gọi Rồng Thần Tím ở Hành tinh namek mới hoặc Vùng Đất Hắc Ám!");
        }
    }

    private void reSummonDragon() {
        activePurpleDragon(playerSummon, true, DRAGON_PURPLE);
        sendPurpleDragonMenu(playerSummon);
    }

    private void sendPurpleDragonMenu(Player pl) {
        NpcService.gI().createMenuRongThieng(pl, ConstNpc.PURPLE_DRAGON_WISH_1, SHENRON_SAY, WISHES_PAGE_1);
    }

    private void activePurpleDragon(Player pl, boolean appear, byte type) {
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
                lastTimeDragonWait = System.currentTimeMillis();
                isDragonAppear = true;
                pl.idMark.setShenronType(-1);
            }
            Service.gI().sendMessAllPlayer(msg);
        } catch (Exception e) {
        }
    }

    private boolean checkPurpleDragonBalls(Player pl) {
        byte star = this.pl_dragonStar.get(pl);
        if (star == 1) {
            if (!InventoryService.gI().isExistItemBag(pl, NGOC_RONG_2_SAO)) {
                Service.gI().sendThongBao(pl, "Bạn còn thiếu 1 viên ngọc rồng tím 2 sao");
                return false;
            }
            if (!InventoryService.gI().isExistItemBag(pl, NGOC_RONG_3_SAO)) {
                Service.gI().sendThongBao(pl, "Bạn còn thiếu 1 viên ngọc rồng tím 3 sao");
                return false;
            }
        } else if (star == 2) {
            if (!InventoryService.gI().isExistItemBag(pl, NGOC_RONG_3_SAO)) {
                Service.gI().sendThongBao(pl, "Bạn còn thiếu 1 viên ngọc rồng tím 3 sao");
                return false;
            }
        }
        if (!InventoryService.gI().isExistItemBag(pl, NGOC_RONG_4_SAO)) {
            Service.gI().sendThongBao(pl, "Bạn còn thiếu 1 viên ngọc rồng tím 4 sao");
            return false;
        }
        if (!InventoryService.gI().isExistItemBag(pl, NGOC_RONG_5_SAO)) {
            Service.gI().sendThongBao(pl, "Bạn còn thiếu 1 viên ngọc rồng tím 5 sao");
            return false;
        }
        if (!InventoryService.gI().isExistItemBag(pl, NGOC_RONG_6_SAO)) {
            Service.gI().sendThongBao(pl, "Bạn còn thiếu 1 viên ngọc rồng tím 6 sao");
            return false;
        }
        if (!InventoryService.gI().isExistItemBag(pl, NGOC_RONG_7_SAO)) {
            Service.gI().sendThongBao(pl, "Bạn còn thiếu 1 viên ngọc rồng tím 7 sao");
            return false;
        }
        return true;
    }

    private void sendNotifyPurpleDragonAppear() {
        Message msg = null;
        try {
            msg = new Message(-25);
            msg.writer().writeUTF(playerSummon.name + " vừa gọi Rồng Thần Tím tại "
                    + playerSummon.zone.map.mapName + " khu vực " + playerSummon.zone.zoneId);
            Service.gI().sendMessAllPlayerIgnoreMe(playerSummon, msg);
        } catch (Exception e) {
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    public void showConfirmWish(Player pl, int menuId, byte select) {
        this.menuId = menuId;
        this.select = select;
        String wish = null;
        if (menuId == ConstNpc.PURPLE_DRAGON_WISH_1) {
            wish = WISHES_PAGE_1[select];
        } else if (menuId == ConstNpc.PURPLE_DRAGON_WISH_2) {
            wish = WISHES_PAGE_2[select];
        }
        NpcService.gI().createMenuRongThieng(pl, ConstNpc.PURPLE_DRAGON_CONFIRM, "Ngươi có chắc muốn ước?", wish, "Từ chối");
    }

    public void reOpenPurpleDragonWishes(Player pl) {
        if (this.menuId == ConstNpc.PURPLE_DRAGON_WISH_1) {
            NpcService.gI().createMenuRongThieng(pl, ConstNpc.PURPLE_DRAGON_WISH_1, SHENRON_SAY, WISHES_PAGE_1);
        } else {
            NpcService.gI().createMenuRongThieng(pl, ConstNpc.PURPLE_DRAGON_WISH_2, SHENRON_SAY, WISHES_PAGE_2);
        }
    }

    public void confirmWish() {
        if (menuId == ConstNpc.PURPLE_DRAGON_WISH_1) {
            switch (select) {
                case 0: // 1 tỷ sức mạnh
                    Service.gI().addSMTN(this.playerSummon, (byte) 2, 1000000000L, false);
                    break;
                case 1: // nâng cấp rada lên 1 cấp
                    Item item = this.playerSummon.inventory.itemsBody.get(4);
                    if (item.isNotNullItem()) {
                        int nextRadaId = getNextRadaId(item.template.id);
                        if (nextRadaId != -1) {
                            item.template = ItemService.gI().getTemplate(nextRadaId);
                            boolean foundOption14 = false;
                            for (ItemOption io : item.itemOptions) {
                                if (io.optionTemplate.id == 14) {
                                    io.param++;
                                    foundOption14 = true;
                                    break;
                                }
                            }
                            if (!foundOption14) {
                                item.itemOptions.add(new ItemOption(14, getRadaLevel(nextRadaId)));
                            }
                            InventoryService.gI().sendItemBody(playerSummon);
                            Service.gI().sendThongBao(playerSummon, "Đã nâng cấp thành công Rada lên " + item.template.name + "!");
                        } else {
                            Service.gI().sendThongBao(playerSummon, "Rada của bạn đã đạt cấp tối đa!");
                            reOpenPurpleDragonWishes(playerSummon);
                            return;
                        }
                    } else {
                        Service.gI().sendThongBao(playerSummon, "Bạn phải đeo Rada ở mục trang bị!");
                        reOpenPurpleDragonWishes(playerSummon);
                        return;
                    }
                    break;
                case 2: // 1000 ngọc
                    this.playerSummon.inventory.gem += 1000;
                    PlayerService.gI().sendInfoHpMpMoney(this.playerSummon);
                    Service.gI().sendThongBao(playerSummon, "Bạn nhận được 1000 ngọc xanh!");
                    break;
                case 3: // tăng 10% sức đánh, hp, ki trong 30 phút
                    playerSummon.itemTime.isUseBuffRongTim = true;
                    playerSummon.itemTime.lastTimeBuffRongTim = System.currentTimeMillis();
                    ItemTimeService.gI().sendItemTime(playerSummon, 2039, 1800);
                    playerSummon.nPoint.calPoint();
                    playerSummon.nPoint.setHp((int) playerSummon.nPoint.hpMax);
                    playerSummon.nPoint.setMp((int) playerSummon.nPoint.mpMax);
                    Service.gI().point(playerSummon);
                    Service.gI().Send_Info_NV(playerSummon);
                    Service.gI().sendThongBao(playerSummon, "Tăng 10% sức đánh, HP, KI trong 30 phút!");
                    break;
            }
        } else if (menuId == ConstNpc.PURPLE_DRAGON_WISH_2) {
            switch (select) {
                case 0: // sẽ có được 1 danh hiệu ngẫu nhiên
                    int[] listTitles = {1286, 1287, 1288, 1289, 1290, 1291, 1292, 1293, 1294, 1295, 1296, 1297, 1298, 1299, 1300, 1392, 1393, 1394, 1457, 1514, 1673, 2032};
                    int randomTitleId = listTitles[Util.nextInt(listTitles.length)];
                    if (InventoryService.gI().getCountEmptyBag(playerSummon) > 0) {
                        Item titleItem = ItemService.gI().createNewItem((short) randomTitleId, 1);
                        InventoryService.gI().addItemBag(playerSummon, titleItem);
                        InventoryService.gI().sendItemBags(playerSummon);
                        Service.gI().sendThongBao(playerSummon, "Bạn nhận được Danh hiệu " + titleItem.template.name + "!");
                    } else {
                        Service.gI().sendThongBao(playerSummon, "Hành trang của bạn không đủ chỗ trống!");
                        reOpenPurpleDragonWishes(playerSummon);
                        return;
                    }
                    break;
                case 1: // tăng 20% may mắn trong 100p
                    playerSummon.itemTime.isUseMayManRongTim = true;
                    playerSummon.itemTime.lastTimeMayManRongTim = System.currentTimeMillis();
                    ItemTimeService.gI().sendItemTime(playerSummon, 2040, 6000);
                    Service.gI().sendThongBao(playerSummon, "Tăng 20% may mắn trong 100 phút!");
                    break;
            }
        }
        purpleDragonLeave(playerSummon, WISHED);
    }

    private int getNextRadaId(int currentRadaId) {
        switch (currentRadaId) {
            case 12: return 57;
            case 57: return 58;
            case 58: return 59;
            case 59: return 184;
            case 184: return 185;
            case 185: return 186;
            case 186: return 187;
            case 187: return 278;
            case 278: return 279;
            case 279: return 280;
            case 280: return 281;
            default: return -1;
        }
    }

    private int getRadaLevel(int id) {
        switch (id) {
            case 12: return 1;
            case 57: return 2;
            case 58: return 3;
            case 59: return 4;
            case 184: return 5;
            case 185: return 6;
            case 186: return 7;
            case 187: return 8;
            case 278: return 9;
            case 279: return 10;
            case 280: return 11;
            case 281: return 12;
            default: return 0;
        }
    }

    public void purpleDragonLeave(Player pl, byte type) {
        if (isDragonAppear) {
            isDragonAppear = false;
            activePurpleDragon(pl, false, DRAGON_PURPLE);
            if (pl != null) {
                if (type == WISHED) {
                    NpcService.gI().createTutorial(pl, -1, "Điều ước của ngươi đã được thực hiện... tạm biệt!");
                } else if (type == TIME_UP) {
                    NpcService.gI().createTutorial(pl, -1, "Đã hết thời gian 5 phút, ta đi đây!");
                }
            }
            this.lastTimeDragonAppeared = System.currentTimeMillis();
            this.playerSummon = null;
        }
    }
}
