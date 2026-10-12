package nro.models.server;

import nro.models.services.Service;
import nro.models.utils.Logger;

public class Maintenance extends Thread {

    private static Maintenance instance;
    private int timeInSeconds;
    public static boolean isRunning = false;

    private Maintenance() {
    }

    public static synchronized Maintenance gI() {
        if (instance == null || (!isRunning && instance.getState() != Thread.State.NEW && !instance.isAlive())) {
            instance = new Maintenance();
        }
        return instance;
    }

    public synchronized void startCountdown() {
        startSeconds(60);
    }

    public synchronized void startSeconds(int seconds) {
        if (isRunning) {
            return;
        }
        if (this.getState() != Thread.State.NEW) {
            instance = new Maintenance();
            instance.startSeconds(seconds);
            return;
        }
        isRunning = true;
        this.timeInSeconds = Math.max(0, seconds);
        this.start();
    }

    public void startImmediately() {
        if (!isRunning) {
            isRunning = true;
            Logger.log(Logger.YELLOW, "BAO TRI NGAY\n");
            ServerManager.gI().close();
        }
    }

    @Override
    public void run() {
        Logger.log(Logger.YELLOW, "Bat dau dem nguoc " + timeInSeconds + "s bao tri\n");
        while (timeInSeconds > 0) {
            try {
                if (timeInSeconds % 5 == 0) {
                    sendRemainingTime();
                }
                Thread.sleep(1000);
                timeInSeconds--;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        Logger.log(Logger.YELLOW, "BAO TRI BAT DAU\n");
        ServerManager.gI().close();
    }

    private void sendRemainingTime() {
        String msg = "BAO TRI SAU " + timeInSeconds + " GIAY.\nOUT GAME MAU LEN.";
        Service.gI().sendThongBaoAllPlayer(msg);
        Logger.log(Logger.YELLOW, msg + "\n");
    }
}
