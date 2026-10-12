package nro.models.map.service;

import nro.models.consts.ConstNpc;
import nro.models.consts.ConstTask;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.server.Manager;
import nro.models.services.TaskService;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NpcManager {

    public static Npc getByIdAndMap(int id, int mapId) {
        for (Npc npc : Manager.NPCS) {
            if (npc.tempId == id && npc.mapId == mapId) {
                return npc;
            }
        }
        return null;
    }

    public static Npc getNpc(byte tempId) {
        for (Npc npc : Manager.NPCS) {
            if (npc.tempId == tempId) {
                return npc;
            }
        }
        return null;
    }

    public static List<Npc> getNpcsByMapPlayer(Player player) {
        List<Npc> list = new ArrayList<>();
        if (player.zone != null) {
            for (Npc npc : player.zone.map.npcs) {
                list.add(npc);
            }
        }
        return list;
    }

    public static byte getNpcStatusForPlayer(Player player, Npc npc) {
        if (player == null || npc == null) {
            return 15;
        }
        // Điều kiện ẩn NPC QUẢ TRỨNG
        if (npc.tempId == ConstNpc.QUA_TRUNG && player.mabuEgg == null && player.zone.map.mapId == (21 + player.gender)) {
            return 15;
        }
        // Điều kiện ẩn NPC dưa hấu
        if (npc.tempId == ConstNpc.DUA_HAU && player.DuaHauEgg == null && player.zone.map.mapId == (21 + player.gender)) {
            return 15;
        }
        // Điều kiện ẩn NPC Quốc Vương nếu chưa đạt nhiệm vụ 34 và sức mạnh dưới 17 tỷ
        if (npc.tempId == ConstNpc.QUOC_VUONG) {
            if (TaskService.gI().getIdTask(player) < ConstTask.TASK_34_0 && player.nPoint.power < 17000000000L) {
                return 15;
            }
        }
        // Điều kiện ẩn NPC CALICK dựa vào nhiệm vụ
        if (npc.tempId == ConstNpc.CALICK && TaskService.gI().getIdTask(player) < ConstTask.TASK_21_0) {
            return 15;
        }
        // Điều kiện ẩn NPC BARDOCK và BERRY ở map Đại hội võ thuật (52) nếu chưa hoàn thành nhiệm vụ 31
        if ((npc.tempId == ConstNpc.BARDOCK || npc.tempId == ConstNpc.BERRY) && player.zone.map.mapId == 52) {
            if (player.playerTask != null && player.playerTask.taskMain != null && player.playerTask.taskMain.id <= 31) {
                return 15;
            }
        }
        // Điều kiện ẩn Rương Danh Hiệu trên map 207 nếu chưa hoàn thành nhiệm vụ 35
        if (npc.tempId == 113 && player.zone.map.mapId == 207) {
            if (player.playerTask != null && player.playerTask.taskMain != null && player.playerTask.taskMain.id <= 35) {
                return 15;
            }
        }
        // Điều kiện ẩn NPC Vua Vegeta ở Làng Kakalot (map 14) nếu chưa hoàn thành nhiệm vụ 35
        if (npc.tempId == ConstNpc.VUA_VEGETA && player.zone.map.mapId == 14) {
            if (player.playerTask != null && player.playerTask.taskMain != null && player.playerTask.taskMain.id <= 35) {
                return 15;
            }
        }
        return (byte) npc.status;
    }

}
