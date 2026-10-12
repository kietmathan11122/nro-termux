package nro.models.services;

import java.io.IOException;
import nro.models.network.Message;
import nro.models.player.NewPet;
import nro.models.player.Player;
import nro.models.utils.Logger;

/** One cosmetic native effect attached to each of the forty anime companions. */
public final class AnimePetAuraService {
    public static final int CYAN = 1243;
    public static final int PURPLE = 1244;
    public static final int GOLD = 1245;
    private static final int FIRST_HEAD = 3343;
    private static final int PET_COUNT = 40;
    private static final int[] FAMILIES = {
        GOLD, GOLD, CYAN, CYAN, PURPLE, GOLD, CYAN, GOLD, CYAN, PURPLE,
        PURPLE, GOLD, CYAN, GOLD, GOLD, PURPLE, PURPLE, PURPLE, PURPLE, GOLD,
        GOLD, GOLD, CYAN, CYAN, CYAN, GOLD, PURPLE, GOLD, CYAN, CYAN,
        GOLD, GOLD, PURPLE, CYAN, GOLD, PURPLE, GOLD, PURPLE, PURPLE, CYAN
    };

    private AnimePetAuraService() { }

    public static int effectForParts(int head, int body, int leg) {
        int difference = head - FIRST_HEAD;
        if (difference < 0 || difference >= PET_COUNT * 3 || difference % 3 != 0
                || body != head + 1 || leg != head + 2) return -1;
        return FAMILIES[difference / 3];
    }

    /** Native -128 subtype zero; isStand=1 keeps the aura visible while moving. */
    public static Message createAddPacket(int petId, int effectId) throws IOException {
        if (effectId < CYAN || effectId > GOLD) throw new IllegalArgumentException("Unknown anime pet aura");
        Message message = new Message(-128);
        message.writer().writeByte(0);
        message.writer().writeInt(petId);
        message.writer().writeShort(effectId);
        message.writer().writeByte(0); // behind the companion
        message.writer().writeByte(-1); // infinite native loop
        message.writer().writeShort(1);
        message.writer().writeByte(1); // both idle and moving
        return message;
    }

    public static Message createRemovePacket(int petId, int effectId) throws IOException {
        if (effectId < CYAN || effectId > GOLD) throw new IllegalArgumentException("Unknown anime pet aura");
        Message message = new Message(-128);
        message.writer().writeByte(1);
        message.writer().writeInt(petId);
        message.writer().writeShort(effectId);
        return message;
    }

    /** Called after the ordinary character-spawn packet, including late arrivals. */
    public static void sendTo(Player subject, Player receiver) {
        if (!(subject instanceof NewPet) || receiver == null || !receiver.isPl()
                || subject.zone == null || receiver.zone != subject.zone) return;
        NewPet pet = (NewPet) subject;
        if (pet.master == null || pet.master.newPet != pet) return;
        int effectId = effectForParts(pet.getHead(), pet.getBody(), pet.getLeg());
        if (effectId < 0) return;
        Message message = null;
        try {
            message = createAddPacket((int) pet.id, effectId);
            receiver.sendMessage(message);
        } catch (IOException error) {
            Logger.logException(AnimePetAuraService.class, error);
        } finally {
            if (message != null) message.cleanup();
        }
    }

    /** Clear the specific cosmetic effect before the native character despawns. */
    public static void remove(NewPet pet) {
        if (pet == null || pet.zone == null) return;
        int effectId = effectForParts(pet.getHead(), pet.getBody(), pet.getLeg());
        if (effectId < 0) return;
        Message message = null;
        try {
            message = createRemovePacket((int) pet.id, effectId);
            Service.gI().sendMessAllPlayerInMap(pet.zone, message);
        } catch (IOException error) {
            Logger.logException(AnimePetAuraService.class, error);
        } finally {
            if (message != null) message.cleanup();
        }
    }
}
