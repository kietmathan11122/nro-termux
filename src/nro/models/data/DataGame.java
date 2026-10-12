package nro.models.data;

import nro.models.player_system.Template.HeadAvatar;
import nro.models.player_system.Template.MapTemplate;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import nro.models.utils.FileIO;
import nro.models.services.Service;
import nro.models.skill.NClass;
import nro.models.skill.Skill;
import nro.models.player_system.Template.MobTemplate;
import nro.models.player_system.Template.NpcTemplate;
import nro.models.player_system.Template.SkillTemplate;
import nro.models.interfaces.ISession;
import java.io.ByteArrayOutputStream;
import nro.models.network.Message;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import nro.models.server.Manager;
import nro.models.network.MySession;
import nro.models.utils.Logger;

import nro.models.player_system.Template.BgItem;

public class DataGame {

    // Bumped after adding the Trunks 3 part set (3279/3280/3281), so clients
    // discard an older cached NR_part file before opening character creation.
    public static byte vsData = -118;
    public static byte vsMap = 47;
    // Buộc client bỏ cache NRskill cũ và tải lại bảng kỹ năng hiện tại.
    public static byte vsSkill = 28;
    public static byte vsItem = -115;
    public static int vsRes = 58;
    public static short maxSmallVersion = 32767;

    private static final byte[][] SMALL_IMAGE_VERSIONS = new byte[5][];

    public static String LINK_IP_PORT = "NRO SYLAS LOCAL:127.0.0.1:14445:0,NRO SYLAS VPS:103.238.235.161:14445:0";
    public static Map<Object, Object> MAP_MOUNT_NUM = new HashMap<>();

    public static void sendVersionGame(MySession session) {
        Message msg;
        try {
            msg = Service.gI().messageNotMap((byte) 4);
            msg.writer().writeByte(vsData);
            msg.writer().writeByte(vsMap);
            msg.writer().writeByte(vsSkill);
            msg.writer().writeByte(vsItem);
            msg.writer().writeByte(0);

            long[] smtieuchuan = {1000L, 3000L, 15000L, 40000L, 90000L, 170000L, 340000L, 700000L,
                1500000L, 15000000L, 150000000L, 1500000000L, 5000000000L, 10000000000L, 40000000000L,
                50010000000L, 60010000000L, 70010000000L, 80010000000L, 100010000000L, 1000010000000L, 10000010000000L};
            msg.writer().writeByte(smtieuchuan.length);
            for (int i = 0; i < smtieuchuan.length; i++) {
                msg.writer().writeLong(smtieuchuan[i]);
            }
            session.sendMessage(msg);
            msg.cleanup();
        } catch (IOException e) {
        }
    }

    //vData
    public static void updateData(MySession session) {
        final byte[] dart = FileIO.readFile("data/update_data/dart");
        final byte[] arrow = FileIO.readFile("data/update_data/arrow");
        final byte[] effect = FileIO.readFile("data/update_data/effect");
        final byte[] image = FileIO.readFile("data/update_data/image");
        final byte[] part = FileIO.readFile("data/update_data/part");
        final byte[] skill = FileIO.readFile("data/update_data/skill");

        Message msg;
        try {
            msg = new Message(-87);
            msg.writer().writeByte(vsData);
            msg.writer().writeInt(dart.length);
            msg.writer().write(dart);
            msg.writer().writeInt(arrow.length);
            msg.writer().write(arrow);
            msg.writer().writeInt(effect.length);
            msg.writer().write(effect);
            msg.writer().writeInt(image.length);
            msg.writer().write(image);
            msg.writer().writeInt(part.length);
            msg.writer().write(part);
            msg.writer().writeInt(skill.length);
            msg.writer().write(skill);

            session.doSendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //vMap
    // Preserve one-byte values below255; marker255 introduces a wider map ID/count.
    public static void writeMapValue(java.io.DataOutputStream writer, int value) throws java.io.IOException {
        if (value < 0 || value > 32767) {
            throw new IllegalArgumentException("Map value outside supported range: " + value);
        }
        if (value < 255) {
            writer.writeByte(value);
        } else {
            writer.writeByte(255);
            writer.writeShort(value);
        }
    }

    public static int readMapValue(java.io.DataInputStream reader) throws java.io.IOException {
        int value = reader.readUnsignedByte();
        if (value != 255) return value;
        value = reader.readUnsignedShort();
        if (value < 255 || value > 32767) {
            throw new java.io.IOException("Invalid extended map value: " + value);
        }
        return value;
    }

    public static void updateMap(MySession session) {
        Message msg;
        try {
            msg = Service.gI().messageNotMap((byte) 6);
            msg.writer().writeByte(vsMap);
            int mapNameCount = 0;
            for (MapTemplate temp : Manager.MAP_TEMPLATES) mapNameCount = Math.max(mapNameCount, temp.id + 1);
            if (mapNameCount > 32767) throw new IllegalStateException("Map IDs exceed supported map-name limit");
            String[] names = new String[mapNameCount];
            java.util.Arrays.fill(names, "");
            for (MapTemplate temp : Manager.MAP_TEMPLATES) names[temp.id] = temp.name;
            writeMapValue(msg.writer(), mapNameCount);
            for (String name : names) msg.writer().writeUTF(name);
            msg.writer().writeByte(Manager.NPC_TEMPLATES.size());
            for (NpcTemplate temp : Manager.NPC_TEMPLATES) {
                msg.writer().writeUTF(temp.name);
                msg.writer().writeShort(temp.head);
                msg.writer().writeShort(temp.body);
                msg.writer().writeShort(temp.leg);
                msg.writer().writeByte(0);
            }
            msg.writer().writeShort(Manager.MOB_TEMPLATES.size());
            for (MobTemplate temp : Manager.MOB_TEMPLATES) {
                msg.writer().writeByte(temp.type);
                msg.writer().writeUTF(temp.name);
                msg.writer().writeInt(temp.hp);
                msg.writer().writeByte(temp.rangeMove);
                msg.writer().writeByte(temp.speed);
                msg.writer().writeByte(temp.dartType);
            }
            session.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            Logger.logException(DataGame.class, e);
        }
    }

    //vSkill
    public static void updateSkill(MySession session) {
        Message msg;
        try {
            msg = new Message(-28);

            msg.writer().writeByte(7);
            msg.writer().writeByte(vsSkill);
            msg.writer().writeByte(0); //count skill option

            msg.writer().writeByte(Manager.NCLASS.size());
            for (NClass nClass : Manager.NCLASS) {
                msg.writer().writeUTF(nClass.name);

                msg.writer().writeByte(nClass.skillTemplatess.size());
                for (SkillTemplate skillTemp : nClass.skillTemplatess) {
                    msg.writer().writeByte(skillTemp.id);
                    msg.writer().writeUTF(skillTemp.name);
                    msg.writer().writeByte(skillTemp.maxPoint);
                    msg.writer().writeByte(skillTemp.manaUseType);
                    msg.writer().writeByte(skillTemp.type);
                    msg.writer().writeShort(skillTemp.iconId);
                    msg.writer().writeUTF(skillTemp.damInfo);
                    msg.writer().writeUTF("null");
                    if (skillTemp.id != 0) {
                        msg.writer().writeByte(skillTemp.skillss.size());
                        for (Skill skill : skillTemp.skillss) {
                            msg.writer().writeShort(skill.skillId);
                            msg.writer().writeByte(skill.point);
                            msg.writer().writeLong(skill.powRequire);
                            msg.writer().writeShort(skill.manaUse);
                            msg.writer().writeInt(skill.coolDown);
                            msg.writer().writeShort(skill.dx);
                            msg.writer().writeShort(skill.dy);
                            msg.writer().writeByte(skill.maxFight);
                            msg.writer().writeShort(skill.damage);
                            msg.writer().writeShort(skill.price);
                            msg.writer().writeUTF(skill.moreInfo);
                        }
                    } else {
                        //Thêm 2 skill trống 105, 106
                        msg.writer().writeByte(skillTemp.skillss.size() + 2);
                        for (Skill skill : skillTemp.skillss) {
                            msg.writer().writeShort(skill.skillId);
                            msg.writer().writeByte(skill.point);
                            msg.writer().writeLong(skill.powRequire);
                            msg.writer().writeShort(skill.manaUse);
                            msg.writer().writeInt(skill.coolDown);
                            msg.writer().writeShort(skill.dx);
                            msg.writer().writeShort(skill.dy);
                            msg.writer().writeByte(skill.maxFight);
                            msg.writer().writeShort(skill.damage);
                            msg.writer().writeShort(skill.price);
                            msg.writer().writeUTF(skill.moreInfo);
                        }
                        for (int i = 105; i <= 106; i++) {
                            msg.writer().writeShort(i);
                            msg.writer().writeByte(0);
                            msg.writer().writeLong(0);
                            msg.writer().writeShort(0);
                            msg.writer().writeInt(0);
                            msg.writer().writeShort(0);
                            msg.writer().writeShort(0);
                            msg.writer().writeByte(0);
                            msg.writer().writeShort(0);
                            msg.writer().writeShort(0);
                            msg.writer().writeUTF("");
                        }
                    }
                }
            }
            session.doSendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            Logger.logException(DataGame.class, e);
        }
    }

    public static void sendDataImageVersion(MySession session) {
        Message msg;
        try {
        } catch (Exception e) {
            Logger.logException(DataGame.class, e);
        }
    }

    public static void sendEffectTemplate(MySession session, int id, int... idtemp) {
        int idT = id;
        if (idtemp.length > 0 && idtemp[0] != 0) {
            idT = idtemp[0];
        }
        Message msg = null;
        try {
            if (session.zoomLevel < 1 || session.zoomLevel > 4) {
                session.zoomLevel = 2;
            }
            final byte[] effData = FileIO.readFile("data/effdata/DataEffect_" + idT);
            final byte[] effImg = FileIO.readFile("data/effect/x" + session.zoomLevel + "/ImgEffect_" + idT + ".png");
            if (effData == null || effImg == null) {
                return;
            }
            msg = new Message(-66);
            msg.writer().writeShort(id);
            msg.writer().writeInt(effData.length);
            msg.writer().write(effData);
            if (session.version > 216) {
                msg.writer().write(idT == 60 ? 2 : 0);
            }
            msg.writer().writeInt(effImg.length);
            msg.writer().write(effImg);
            session.sendMessage(msg);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    public static void sendBgItemVersion(MySession session) {
        Message msg;
        try {
            if (session.zoomLevel < 1 || session.zoomLevel > 4) {
                session.zoomLevel = 2;
            }
            int maxImageId = 0;
            for (BgItem bgItem : Manager.BG_ITEMS) {
                if (bgItem.idImage > maxImageId) {
                    maxImageId = bgItem.idImage;
                }
            }
            byte[] imageVersions = new byte[maxImageId + 1];
            for (BgItem bgItem : Manager.BG_ITEMS) {
                int imageId = bgItem.idImage;
                if (imageId < 0 || imageId >= imageVersions.length) {
                    continue;
                }
                File imageFile = new File(
                        "data/item_bg_temp/x" + session.zoomLevel + "/" + imageId + ".png");
                imageVersions[imageId] = (byte) (imageFile.isFile() ? imageFile.length() % 127 : 0);
            }
            msg = new Message(-93);
            msg.writer().writeShort(imageVersions.length);
            for (byte imageVersion : imageVersions) {
                msg.writer().writeByte(imageVersion);
            }
            session.sendMessage(msg);
        } catch (Exception e) {
            Logger.logException(DataGame.class, e);
        }
    }

    public static void sendItemBGTemplate(MySession session, int id) {
        Message msg;
        try {
            if (session.zoomLevel < 1 || session.zoomLevel > 4) {
                session.zoomLevel = 2;
            }
            final byte[] bg_temp = FileIO.readFile("data/item_bg_temp/x" + session.zoomLevel + "/" + id + ((id >= 9800 && id <= 9802) ? ".jpg" : ".png"));
            if (bg_temp == null) {
                return;
            }
            msg = new Message(-32);
            msg.writer().writeShort(id);
            msg.writer().writeInt(bg_temp.length);
            msg.writer().write(bg_temp);
            session.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            Logger.logException(DataGame.class, e);
        }
    }

    public static void sendDataItemBG(MySession session) {
        Message msg;
        try {
            // The active client reads an explicit template ID for every entry.
            // IDs in bg_item_template are sparse, so entry positions cannot be IDs.
            msg = new Message(-31);
            msg.writer().writeShort(ServerMapScenery.templatesFor(session, Manager.BG_ITEMS).size());
            for (BgItem bgItem : ServerMapScenery.templatesFor(session, Manager.BG_ITEMS)) {
                msg.writer().writeShort(bgItem.id);
                msg.writer().writeShort(bgItem.idImage);
                msg.writer().writeByte(bgItem.layer);
                msg.writer().writeShort(bgItem.dx);
                msg.writer().writeShort(bgItem.dy);
                msg.writer().writeByte(0);
            }
            session.sendMessage(msg);
            ServerMapScenery.templatesSent(session);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendIcon(MySession session, int id) {
        Message msg;
        try {
            if (session.zoomLevel < 1 || session.zoomLevel > 4) {
                session.zoomLevel = 2;
            }
            final byte[] icon = FileIO.readFile("data/icon/x" + session.zoomLevel + "/" + id + ".png");

            if (icon == null) {
                return;
            }

            msg = new Message(-67);
            msg.writer().writeInt(id);
            msg.writer().writeInt(icon.length);
            msg.writer().write(icon);
            session.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendSmallVersion(MySession session) {
        Message msg;
        try {
            if (session.zoomLevel < 1 || session.zoomLevel > 4) {
                session.zoomLevel = 2;
            }
            byte[] versions = getSmallImageVersions(session.zoomLevel);
            msg = new Message(-77);
            msg.writer().writeShort(maxSmallVersion);
            for (int i = 0; i < maxSmallVersion; i++) {
                msg.writer().writeByte(versions[i]);
            }
            session.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static byte[] getSmallImageVersions(int zoomLevel) {
        byte[] versions = SMALL_IMAGE_VERSIONS[zoomLevel];
        if (versions != null) {
            return versions;
        }
        synchronized (SMALL_IMAGE_VERSIONS) {
            versions = SMALL_IMAGE_VERSIONS[zoomLevel];
            if (versions == null) {
                versions = new byte[maxSmallVersion];
                File iconDirectory = new File("data/icon/x" + zoomLevel);
                for (int id = 0; id < maxSmallVersion; id++) {
                    File iconFile = new File(iconDirectory, id + ".png");
                    if (iconFile.isFile()) {
                        versions[id] = (byte) (iconFile.length() % 127);
                    }
                }
                SMALL_IMAGE_VERSIONS[zoomLevel] = versions;
            }
        }
        return versions;
    }

    public static void requestMobTemplate(MySession session, int id) {
        Message msg;
        try {
            if (session.zoomLevel < 1 || session.zoomLevel > 4) {
                session.zoomLevel = 2;
            }
//            if (!session.check && id > 106) {
//                byte[] mob = FileIO.readFile("data/mob/x" + session.zoomLevel + "/" + 0);
//                msg = new Message(11);
//                msg.writer().writeByte(id);
//                msg.writer().write(mob);
//                session.sendMessage(msg);
//                msg.cleanup();
//                return;
//            }
            final byte[] mob = FileIO.readFile("data/mob/x" + session.zoomLevel + "/" + id);
            msg = new Message(11);
            msg.writer().writeShort(id);
            msg.writer().write(mob);
            session.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    public static void sendTileSetInfo(MySession session) {
        Message msg;
        try {
            final byte[] data = FileIO.readFile("data/map/tile_set_info");
            msg = new Message(-82);
            msg.writer().write(data);
            session.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //data vẽ map
    public static boolean supportsNative95(MySession session) {
        return session != null && (session.typeClient == 4 || session.typeClient == 6)
                && session.zoomLevel >= 2 && session.zoomLevel <= 4;
    }

    public static void sendMapTemp(MySession session, int id) {
        Message msg;
        try {
            String gridFile = String.valueOf(id);
            if ((id == 3 || id == 0 || id == 42 || id == 1) && Manager.MAP_TEMPLATES != null) {
                for (MapTemplate template : Manager.MAP_TEMPLATES) {
                    if (template != null && id == 3 && template.id == 3 && template.tileId == 90) {
                        gridFile = "3_native90";
                        break;
                    }
                    if (template != null && id == 0 && template.id == 0 && template.tileId == 91) {
                        gridFile = "0_aru91";
                        break;
                    }
                    if (template != null && id == 0 && template.id == 0 && template.tileId == 92) {
                        gridFile = "0_aru92_bankfix";
                        break;
                    }
                    if (template != null && id == 42 && template.id == 42 && template.tileId == 93) {
                        gridFile = "42_aru_cliff_hd";
                        break;
                    }
                    if (template != null && id == 1 && template.id == 1 && template.tileId == 94) {
                        gridFile = "1_flower_hill_hd";
                        break;
                    }
                    if (template != null && id == 1 && template.id == 1 && template.tileId == 95) {
                        gridFile = supportsNative95(session)
                                ? "1_flower_hill_decor95" : "1_flower_hill_native_compat";
                        break;
                    }
                }
            }
            gridFile = paintedTerrainGridFile(session, id, gridFile);
            final byte[] data = FileIO.readFile("data/map/tile_map_data/" + gridFile);
            if (data == null) {
                return;
            }
            msg = new Message(-28);
            msg.writer().writeByte(10);
            msg.writer().write(data);
            session.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            Logger.logException(DataGame.class, e);
        }
    }

    //head-avatar
    public static void sendHeadAvatar(Message msg) {
        try {
            msg.writer().writeShort(Manager.HEAD_AVATARS.size());
            for (HeadAvatar ha : Manager.HEAD_AVATARS) {
                msg.writer().writeShort(ha.headId);
                msg.writer().writeShort(ha.avatarId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendImageByName(MySession session, String imgName) {
        if (imgName != null && imgName.startsWith(MapBackgroundStore.PREFIX)) {
            sendMapBackgroundChunk(session, imgName);
            return;
        }
        Message msg;
        try {
            if (session.zoomLevel < 1 || session.zoomLevel > 4) {
                session.zoomLevel = 2;
            }
            msg = new Message(66);
            msg.writer().writeUTF(imgName);
            msg.writer().writeByte(Manager.getNFrameImageByName(imgName));

            final byte[] data = FileIO.readFile("data/img_by_name/x" + session.zoomLevel + "/" + imgName + ".png");

            if (data == null) {
                msg.writer().writeInt(0);
            } else {
                msg.writer().writeInt(data.length);
                msg.writer().write(data);
            }

            session.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void sendMapBackgroundChunk(MySession session, String name) {
        int[] request = MapBackgroundStore.parseRequest(name);
        if (request == null || session.player == null || session.player.zone == null
                || session.player.zone.map.mapId != request[0]) return;
        long now = System.nanoTime();
        if (session.lastBackgroundChunkNanos != 0 && now - session.lastBackgroundChunkNanos < 20_000_000L) return;
        session.lastBackgroundChunkNanos = now;
        Message response = null;
        try {
            byte[] chunk = MapBackgroundStore.readChunk(java.nio.file.Paths.get("data", "map_backgrounds"), request[0], request[1]);
            response = new Message(66);
            response.writer().writeUTF(name);
            response.writer().writeByte(1);
            response.writer().writeInt(chunk == null ? 0 : chunk.length);
            if (chunk != null) response.writer().write(chunk);
            session.sendMessage(response);
        } catch (Exception error) {
            Logger.logException(DataGame.class, error);
        } finally {
            if (response != null) response.cleanup();
        }
    }

    public static void sendVersionRes(ISession session) {
        Message msg;
        try {
            msg = new Message(-74);
            msg.writer().writeByte(0);
            msg.writer().writeInt(vsRes);
            session.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendSizeRes(MySession session) {
        Message msg;
        try {
            if (session.zoomLevel < 1 || session.zoomLevel > 4) {
                session.zoomLevel = 2;
            }
            msg = new Message(-74);
            msg.writer().writeByte(1);
            final File[] files = new File("data/res/x" + session.zoomLevel).listFiles();
            if (files != null) {
                msg.writer().writeShort(files.length);
            } else {
                msg.writer().writeShort(0);
            }
            session.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendRes(MySession session) {
        Message msg;
        try {
            if (session.zoomLevel < 1 || session.zoomLevel > 4) {
                session.zoomLevel = 2;
            }
            File dir = new File("data/res/x" + session.zoomLevel);
            File[] files = dir.listFiles();
            if (files != null) {
                for (final File fileEntry : files) {
                    if (!fileEntry.isFile()) {
                        continue;
                    }
                    // Resource packs contain thousands of files. Keep only a small
                    // number of unsent packets in memory for a slow client.
                    long waitStarted = System.currentTimeMillis();
                    while (session.isConnected() && session.getNumMessages() > 16) {
                        if (System.currentTimeMillis() - waitStarted > 10 * 60_000L) {
                            session.disconnect();
                            return;
                        }
                        try {
                            Thread.sleep(25L);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                    if (!session.isConnected()) {
                        return;
                    }
                    String original = fileEntry.getName();
                    try {
                        byte[] res = Files.readAllBytes(fileEntry.toPath());
                        msg = new Message(-74);
                        msg.writer().writeByte(2);
                        msg.writer().writeUTF(original);
                        msg.writer().writeInt(res.length);
                        msg.writer().write(res);
                        session.sendMessage(msg);
                        msg.cleanup();
                    } catch (IOException e) {
                        if (!session.isConnected() || Thread.currentThread().isInterrupted()) {
                            return;
                        }
                        Logger.logException(DataGame.class, e);
                    }
                }
            }
            if (!session.isConnected()) {
                return;
            }
            msg = new Message(-74);
            msg.writer().writeByte(3);
            msg.writer().writeInt(vsRes);
            session.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            Logger.logException(DataGame.class, e);
        }
    }

    public static void sendLinkIP(MySession session) {
        Message msg;
        try {
            msg = new Message(-29);
            msg.writer().writeByte(2);
            msg.writer().writeUTF(LINK_IP_PORT + ",0,0");
            msg.writer().writeByte(1);
            session.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Painted foreground trial: preserve canonical terrain for fallback clients.
    public static String paintedTerrainBgFile(MySession session, int mapId, int tileId, String fallback) {
        String scenery = ServerMapScenery.bgFile(session, mapId, tileId);
        if (scenery != null) return scenery;
        if (supportsNative95(session) && ((mapId == 4 && tileId == 2) || (mapId == 5 && tileId == 3) || (mapId == 7 && tileId == 5) || (mapId == 9 && tileId == 5) || (mapId == 10 && tileId == 6) || (mapId == 11 && tileId == 7) || (mapId == 12 && tileId == 7) || (mapId == 13 && tileId == 8) || (mapId == 15 && tileId == 9) || (mapId == 16 && tileId == 9) || (mapId == 17 && tileId == 10) || (mapId == 18 && tileId == 10) || (mapId == 19 && tileId == 11) || (mapId == 20 && tileId == 12) || (mapId == 21 && tileId == 1) || (mapId == 22 && tileId == 5) || (mapId == 23 && tileId == 9) || (mapId == 24 && tileId == 1) || (mapId == 25 && tileId == 5) || (mapId == 26 && tileId == 9) || (mapId == 29 && tileId == 3) || (mapId == 30 && tileId == 3) || (mapId == 31 && tileId == 7) || (mapId == 32 && tileId == 7) || (mapId == 33 && tileId == 8) || (mapId == 34 && tileId == 8) || (mapId == 35 && tileId == 10) || (mapId == 36 && tileId == 10) || (mapId == 37 && tileId == 12) || (mapId == 38 && tileId == 12) || (mapId == 40 && tileId == 5) || (mapId == 41 && tileId == 9) || (mapId == 43 && tileId == 5) || (mapId == 45 && tileId == 13) || (mapId == 46 && tileId == 13) || (mapId == 48 && tileId == 13) || (mapId == 49 && tileId == 14) || (mapId == 50 && tileId == 10) || (mapId == 51 && tileId == 15) || (mapId == 52 && tileId == 9) || (mapId == 53 && tileId == 16) || (mapId == 54 && tileId == 17) || (mapId == 55 && tileId == 17) || (mapId == 56 && tileId == 17) || (mapId == 57 && tileId == 17) || (mapId == 58 && tileId == 16) || (mapId == 59 && tileId == 16) || (mapId == 60 && tileId == 18) || (mapId == 61 && tileId == 18) || (mapId == 62 && tileId == 18) || (mapId == 63 && tileId == 19) || (mapId == 64 && tileId == 19) || (mapId == 65 && tileId == 19) || (mapId == 66 && tileId == 19) || (mapId == 67 && tileId == 19) || (mapId == 68 && tileId == 20) || (mapId == 69 && tileId == 20) || (mapId == 70 && tileId == 20) || (mapId == 71 && tileId == 20) || (mapId == 72 && tileId == 20) || (mapId == 73 && tileId == 21) || (mapId == 74 && tileId == 21) || (mapId == 75 && tileId == 21) || (mapId == 76 && tileId == 21) || (mapId == 77 && tileId == 21) || (mapId == 78 && tileId == 1) || (mapId == 79 && tileId == 19) || (mapId == 80 && tileId == 19) || (mapId == 81 && tileId == 20) || (mapId == 82 && tileId == 20) || (mapId == 83 && tileId == 21) || (mapId == 84 && tileId == 11) || (mapId == 85 && tileId == 20) || (mapId == 86 && tileId == 6) || (mapId == 87 && tileId == 11) || (mapId == 88 && tileId == 8) || (mapId == 89 && tileId == 3) || (mapId == 90 && tileId == 10) || (mapId == 91 && tileId == 1) || (mapId == 92 && tileId == 22) || (mapId == 93 && tileId == 22) || (mapId == 94 && tileId == 22) || (mapId == 95 && tileId == 1) || (mapId == 96 && tileId == 22) || (mapId == 97 && tileId == 23) || (mapId == 98 && tileId == 23) || (mapId == 99 && tileId == 23) || (mapId == 100 && tileId == 23) || (mapId == 101 && tileId == 1) || (mapId == 102 && tileId == 22) || (mapId == 103 && tileId == 23) || (mapId == 104 && tileId == 11) || (mapId == 105 && tileId == 24) || (mapId == 106 && tileId == 24) || (mapId == 107 && tileId == 24) || (mapId == 108 && tileId == 25) || (mapId == 109 && tileId == 25) || (mapId == 110 && tileId == 25) || (mapId == 112 && tileId == 26) || (mapId == 113 && tileId == 23) || (mapId == 114 && tileId == 12) || (mapId == 115 && tileId == 12) || (mapId == 116 && tileId == 10) || (mapId == 117 && tileId == 12) || (mapId == 118 && tileId == 13) || (mapId == 119 && tileId == 12) || (mapId == 120 && tileId == 12) || (mapId == 121 && tileId == 23) || (mapId == 122 && tileId == 1) || (mapId == 123 && tileId == 1) || (mapId == 124 && tileId == 1) || (mapId == 125 && tileId == 1) || (mapId == 126 && tileId == 11) || (mapId == 127 && tileId == 12) || (mapId == 128 && tileId == 12) || (mapId == 129 && tileId == 23) || (mapId == 130 && tileId == 1) || (mapId == 131 && tileId == 27) || (mapId == 132 && tileId == 27) || (mapId == 133 && tileId == 27) || (mapId == 134 && tileId == 1) || (mapId == 135 && tileId == 28) || (mapId == 136 && tileId == 28) || (mapId == 137 && tileId == 28) || (mapId == 138 && tileId == 28) || (mapId == 139 && tileId == 9) || (mapId == 140 && tileId == 9) || (mapId == 141 && tileId == 14) || (mapId == 142 && tileId == 14) || (mapId == 143 && tileId == 14) || (mapId == 144 && tileId == 12) || (mapId == 145 && tileId == 13) || (mapId == 147 && tileId == 3) || (mapId == 148 && tileId == 15) || (mapId == 149 && tileId == 11) || (mapId == 151 && tileId == 12) || (mapId == 152 && tileId == 25) || (mapId == 153 && tileId == 1) || (mapId == 154 && tileId == 29) || (mapId == 155 && tileId == 30) || (mapId == 156 && tileId == 19) || (mapId == 157 && tileId == 19) || (mapId == 158 && tileId == 25) || (mapId == 159 && tileId == 25) || (mapId == 160 && tileId == 31) || (mapId == 161 && tileId == 31) || (mapId == 162 && tileId == 31) || (mapId == 163 && tileId == 31) || (mapId == 164 && tileId == 2) || (mapId == 165 && tileId == 12) || (mapId == 166 && tileId == 23) || (mapId == 167 && tileId == 12) || (mapId == 168 && tileId == 12) || (mapId == 169 && tileId == 12) || (mapId == 170 && tileId == 12) || (mapId == 171 && tileId == 13) || (mapId == 172 && tileId == 12) || (mapId == 173 && tileId == 12) || (mapId == 174 && tileId == 1) || (mapId == 175 && tileId == 5) || (mapId == 176 && tileId == 9) || (mapId == 177 && tileId == 1) || (mapId == 178 && tileId == 5) || (mapId == 179 && tileId == 9) || (mapId == 182 && tileId == 1) || (mapId == 183 && tileId == 41) || (mapId == 184 && tileId == 42) || (mapId == 185 && tileId == 42) || (mapId == 191 && tileId == 43) || (mapId == 198 && tileId == 36) || (mapId == 199 && tileId == 32) || (mapId == 200 && tileId == 32) || (mapId == 201 && tileId == 36) || (mapId == 202 && tileId == 36) || (mapId == 203 && tileId == 36) || (mapId == 204 && tileId == 15) || (mapId == 205 && tileId == 25) || (mapId == 206 && tileId == 5) || (mapId == 207 && tileId == 39) || (mapId == 208 && tileId == 36) || (mapId == 209 && tileId == 29) || (mapId == 210 && tileId == 21) || (mapId == 211 && tileId == 30) || (mapId == 213 && tileId == 5) || (mapId == 214 && tileId == 3) || (mapId == 216 && tileId == 20))) {
            return mapId + "_painted_all_b004";
        }
        if (supportsNative95(session) && ((mapId == 2 && tileId == 1)
                || (mapId == 42 && tileId == 93)
                || ((mapId == 14 || mapId == 44) && tileId == 9))) {
            return mapId + "_painted_land_b003";
        }
        if (supportsNative95(session) && tileId == 4
                && (mapId == 47 || mapId == 111 || mapId == 146 || mapId == 215)) {
            return mapId + "_painted_forest_b002";
        }
        if (supportsNative95(session) && ((mapId == 6 && tileId == 4)
                || ((mapId == 27 || mapId == 28) && tileId == 2))) {
            return mapId + "_painted_forest_b001";
        }
        return mapId == 8 && tileId == 5 && supportsNative95(session)
                ? "8_painted_terrain_v1" : fallback;
    }

    // This visual alias removes only reviewed mask0 crest cells. Exact bytes
    // keep stale/missing data from bypassing the native foreground guard.
    private static boolean validMap133SceneryGrid(File file) {
        try {
            byte[] grid = Files.readAllBytes(file.toPath());
            if (grid.length != 1502 || (grid[0] & 255) != 60 || (grid[1] & 255) != 25) return false;
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256").digest(grid);
            StringBuilder hash = new StringBuilder(64);
            for (byte value : digest) {
                hash.append(Character.forDigit((value >>> 4) & 15, 16));
                hash.append(Character.forDigit(value & 15, 16));
            }
            return "2d3d737763e11685a4a4eab29a63b8d06d21a24ee6808e7344687196caf23360".equals(hash.toString());
        } catch (Exception unavailable) {
            return false;
        }
    }

    public static String paintedTerrainGridFile(MySession session, int mapId, String fallback) {
        if (mapId == 133 && supportsNative95(session) && Manager.MAP_TEMPLATES != null) {
            for (MapTemplate template : Manager.MAP_TEMPLATES) {
                if (template != null && template.id == 133 && template.tileId == 27) {
                    String alias = ServerMapScenery.bgFile(session, 133, 27);
                    if ("133_server_scenery_20261010".equals(alias)
                            && validMap133SceneryGrid(new File("data/map/tile_map_data/" + alias))) return alias;
                    break;
                }
            }
        }

        if (mapId == 8 && Manager.MAP_TEMPLATES != null) {
            for (MapTemplate template : Manager.MAP_TEMPLATES) {
                if (template != null && template.id == mapId) {
                    return paintedTerrainBgFile(session, mapId, template.tileId, fallback);
                }
            }
        }
        return fallback;
    }
}


