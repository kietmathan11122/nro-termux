package nro.models.services;

import nro.models.item.Item;
import nro.models.player.Player;

/** Cosmetic followers kept in body slot 12 and broadcast with command 31. */
public final class FollowPetService {
    public static final int HUNGRY_GHOST_ITEM = 2274;
    public static final int LONG_TONGUE_GHOST_ITEM = 2275;
    public static final int WILL_O_WISP_ITEM = 2276;
    public static final int WRONGED_GHOST_ITEM = 2277;

    private FollowPetService() { }

    // Pet Cô Hồn nay dùng NewPet (part head/body/leg), không dùng sprite follower ở client.
    public static boolean isFollowPetItem(int itemId) {
        return false;
    }

    public static short smallIdForItem(int itemId) {
        return (short) (20825 + (itemId - HUNGRY_GHOST_ITEM) * 8);
    }

    public static boolean usesSeparateFrameImages(short smallId) {
        return smallId >= 20825 && smallId <= 20849;
    }

    /** Xóa sprite follower cũ đã tạo ở client để chỉ còn NewPet chuẩn của server. */
    public static void sync(Player player) {
        if (player != null) {
            if (PetCollectionService.gI().hasAnyChibi(player)) PetCollectionService.gI().syncChibiFollower(player); else Service.gI().sendPetFollow(player, (short) 0);
        }
    }

    public static void syncTo(Player observer, Player owner) {
        if (observer != null && owner != null) {
            if (PetCollectionService.gI().hasAnyChibi(owner)) Service.gI().sendChibiFollowToMe(observer, owner); else Service.gI().sendPetFollowTo(observer, owner, (short) 0);
        }
    }

}
