package nro.models.Bot;

import java.util.Set;
import nro.models.utils.Util;

public final class BotNameRepository {

    private static final String[] NAMES = {
        "Goku", "Vegeta", "Gohan", "Trunks", "Piccolo", "Krilin", "Yamcha", "TienShinhan",
        "Bulma", "ChiChi", "Bardock", "Broly", "Cell", "Frieza", "Beerus", "Whis",
        "Jiren", "Hit", "Cabba", "Caulifla", "Kale", "Pan", "Uub", "Android17",
        "Android18", "Buu", "Dende", "Karin", "Popo", "Yajirobe", "Raditz", "Nappa",
        "Cooler", "Goten", "Shin", "Kibito", "Toppo", "Dyspo", "Zamasu", "BlackGoku",
        "Mabu", "Kaioken", "Senzu", "Dragon", "Namek", "Saiyan", "Fusion", "Kame"
    };

    private BotNameRepository() {
    }

    public static String randomTrainingBotName(Set<String> usedNames) {
        for (int i = 0; i < 100; i++) {
            String name = NAMES[Util.nextInt(0, NAMES.length - 1)] + Util.nextInt(10, 9999);
            if (usedNames == null || usedNames.add(name)) {
                return name;
            }
        }
        return "mrblue" + System.currentTimeMillis() % 100000;
    }
}
