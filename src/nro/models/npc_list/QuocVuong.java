package nro.models.npc_list;

import nro.models.consts.ConstNpc;
import nro.models.item.Item;
import nro.models.npc.Npc;
import nro.models.player.NPoint;
import nro.models.player.Player;
import nro.models.services.OpenPowerService;
import nro.models.services.Service;
import nro.models.services.TaskService;
import nro.models.utils.Util;

public class QuocVuong extends Npc {

    private static final byte MAX_LIMIT_CUSTOM = 13;

    private int getCost(int limitPower) {
        return limitPower >= 9 ? 150 : OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER;
    }

    public QuocVuong(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (player.playerTask.taskMain.id == 34) {
                if (player.playerTask.taskMain.index == 6) {
                    this.createOtherMenu(player, 345, "Bây giờ con hãy nâng sức đánh của mình lên 24,000 để có đủ sức mạnh bảo vệ hành tinh Namek nhé!", "Đồng ý", "Từ chối");
                    return;
                }
                if (player.playerTask.taskMain.index == 7) {
                    if (player.nPoint.dameg >= 24000) {
                        this.createOtherMenu(player, 346, "Sức mạnh của cậu thật đáng kinh ngạc! Hãy báo cáo lại cho ta khi sẵn sàng nhé.", "Báo cáo", "Từ chối");
                    } else {
                        this.createOtherMenu(player, ConstNpc.IGNORE_MENU, "Cậu mới đạt " + nro.models.utils.Util.numberToMoney(player.nPoint.dameg) + " sức đánh gốc. Hãy tập luyện thêm đến 24.000 rồi quay lại đây!", "Đóng");
                    }
                    return;
                }
                if (player.playerTask.taskMain.index == 8) {
                    this.createOtherMenu(player, 347, "Tuyệt vời! Sức mạnh của con đã đủ rồi. Bây giờ con hãy đi gặp Dende để nhận nhiệm vụ tìm 7 viên ngọc rồng Namek nhé!", "Đồng ý", "Từ chối");
                    return;
                }
            }
            if (player.playerTask.taskMain.id == 35) {
                if (player.playerTask.taskMain.index == 0) {
                    this.createOtherMenu(player, 351, "Ôi không! Sau khi điều ước được thực hiện, các viên Ngọc Rồng bỗng nhiên nứt vỡ và tỏa ra luồng khí đen kịch độc! Một cánh cổng hắc ám vừa xuất hiện tại Vùng Đất Hắc Ám (Map 207). Cậu hãy lập tức tiến vào đó để điều tra ngay!", "Đi ngay");
                    return;
                }
            }
            this.createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Con muốn nâng giới hạn sức mạnh cho bản thân hay đệ tử?",
                    "Bản thân", "Đệ tử", "Từ chối");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (player.idMark.getIndexMenu() == 345) {
                if (select == 0) {
                    if (player.playerTask.taskMain.id == 34 && player.playerTask.taskMain.index == 6) {
                        player.playerTask.taskMain.subTasks.get(player.playerTask.taskMain.index).count = (short) (player.playerTask.taskMain.subTasks.get(player.playerTask.taskMain.index).maxCount - 1);
                    }
                    nro.models.services.TaskService.gI().doneTask(player, nro.models.consts.ConstTask.TASK_34_6);
                }
                return;
            }
            if (player.idMark.getIndexMenu() == 346) {
                if (select == 0) {
                    if (player.playerTask.taskMain.id == 34 && player.playerTask.taskMain.index == 7) {
                        player.playerTask.taskMain.subTasks.get(player.playerTask.taskMain.index).count = (short) (player.playerTask.taskMain.subTasks.get(player.playerTask.taskMain.index).maxCount - 1);
                    }
                    nro.models.services.TaskService.gI().doneTask(player, nro.models.consts.ConstTask.TASK_34_7);
                }
                return;
            }
            if (player.idMark.getIndexMenu() == 347) {
                if (select == 0) {
                    if (player.playerTask.taskMain.id == 34 && player.playerTask.taskMain.index == 8) {
                        player.playerTask.taskMain.subTasks.get(player.playerTask.taskMain.index).count = (short) (player.playerTask.taskMain.subTasks.get(player.playerTask.taskMain.index).maxCount - 1);
                    }
                    nro.models.services.TaskService.gI().doneTask(player, nro.models.consts.ConstTask.TASK_34_8);
                }
                return;
            }
            if (player.idMark.getIndexMenu() == 351) {
                if (select == 0) {
                    TaskService.gI().doneTask(player, nro.models.consts.ConstTask.TASK_35_0);
                    nro.models.map.service.ChangeMapService.gI().changeMapBySpaceShip(player, 207, -1, 250);
                }
                return;
            }
            if (player.idMark.isBaseMenu()) {
                switch (select) {

                    case 0 -> {
                        if (player.nPoint.limitPower < MAX_LIMIT_CUSTOM) {
                            if (player.nPoint.limitPower >= 8 && player.zone.map.mapId != 206) {
                                this.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                                        "Sức mạnh của con đã đạt đến giới hạn tại đây. Hãy đến Namek Mới (Map 206) gặp ta để tiếp tục mở thêm giới hạn sức mạnh!",
                                        "Đóng");
                            } else {
                                this.createOtherMenu(player, ConstNpc.OPEN_POWER_MYSEFT,
                                        "Ta sẽ truyền năng lượng giúp con mở giới hạn sức mạnh của bản thân lên "
                                        + Util.numberToMoney(player.nPoint.getPowerNextLimit()),
                                        "Nâng\ngiới hạn\nsức mạnh",
                                        "Nâng ngay\n" + Util.numberToMoney(getCost(player.nPoint.limitPower)) + " ngọc",
                                        "Đóng");
                            }
                        } else {
                            this.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                                     "Sức mạnh của con đã đạt tới giới hạn hiện tại",
                                    "Đóng");
                        }
                    }

                    case 1 -> {
                        if (player.pet != null) {
                            if (player.pet.nPoint.limitPower < MAX_LIMIT_CUSTOM) {
                                if (player.pet.nPoint.limitPower >= 8 && player.zone.map.mapId != 206) {
                                    this.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                                             "Sức mạnh đệ tử của con đã đạt đến giới hạn tại đây. Hãy đến Namek Mới (Map 206) gặp ta để tiếp tục mở thêm giới hạn sức mạnh cho nó!",
                                            "Đóng");
                                } else {
                                    this.createOtherMenu(player, ConstNpc.OPEN_POWER_PET,
                                            "Ta sẽ truyền năng lượng giúp con mở giới hạn sức mạnh của đệ tử lên "
                                            + Util.numberToMoney(player.pet.nPoint.getPowerNextLimit()),
                                            "Nâng ngay\n" + Util.numberToMoney(getCost(player.pet.nPoint.limitPower)) + " ngọc", "Đóng");
                                }
                            } else {
                                this.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                                        "Sức mạnh của đệ con đã đạt tới giới hạn hiện tại",
                                        "Đóng");
                            }
                        } else {
                            Service.gI().sendThongBao(player, "Không thể thực hiện");
                        }
                    }
                }
            } else if (player.idMark.getIndexMenu() == ConstNpc.OPEN_POWER_MYSEFT) {
                if (player.nPoint.limitPower >= 8 && player.zone.map.mapId != 206) {
                    Service.gI().sendThongBao(player, "Hãy đến Namek Mới (Map 206) để mở thêm giới hạn sức mạnh!");
                    return;
                }
                switch (select) {
                    case 0 -> {
                        if (player.nPoint.limitPower < MAX_LIMIT_CUSTOM) {
                            OpenPowerService.gI().openPowerBasic(player);
                        } else {
                            Service.gI().sendThongBao(player, "Giới hạn sức mạnh của bạn đã đạt tới mức tối đa hiện tại");
                        }
                    }
                    case 1 -> {
                        if (player.nPoint.limitPower < MAX_LIMIT_CUSTOM) {
                            int cost = getCost(player.nPoint.limitPower);
                            if (player.inventory.gem >= cost) {
                                if (OpenPowerService.gI().openPowerSpeed(player)) {
                                    player.inventory.gem -= cost;
                                    Service.gI().sendMoney(player);
                                }
                            } else {
                                Service.gI().sendThongBao(player,
                                        "Bạn không đủ ngọc để mở, còn thiếu "
                                        + Util.numberToMoney((cost - player.inventory.gem)) + " ngọc");
                            }
                        } else {
                            Service.gI().sendThongBao(player, "Giới hạn sức mạnh của bạn đã đạt tới mức tối đa hiện tại");
                        }
                    }
                }
            } else if (player.idMark.getIndexMenu() == ConstNpc.OPEN_POWER_PET) {
                if (player.pet != null && player.pet.nPoint.limitPower >= 8 && player.zone.map.mapId != 206) {
                    Service.gI().sendThongBao(player, "Hãy đến Namek Mới (Map 206) để mở thêm giới hạn sức mạnh cho đệ tử!");
                    return;
                }
                if (select == 0) {
                    if (player.pet.nPoint.limitPower < MAX_LIMIT_CUSTOM) {
                        int cost = getCost(player.pet.nPoint.limitPower);
                        if (player.inventory.gem >= cost) {
                            if (OpenPowerService.gI().openPowerSpeed(player.pet)) {
                                player.inventory.gem -= cost;
                                Service.gI().sendMoney(player);
                            }
                        } else {
                            Service.gI().sendThongBao(player,
                                    "Bạn không đủ ngọc để mở, còn thiếu "
                                    + Util.numberToMoney((cost - player.inventory.gem)) + " ngọc");
                        }
                    } else {
                        Service.gI().sendThongBao(player, "Giới hạn sức mạnh của đệ tử đã đạt tới mức tối đa hiện tại");
                    }
                }
            }
        }
    }
}
