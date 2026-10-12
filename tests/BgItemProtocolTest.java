import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import nro.models.data.DataGame;
import nro.models.network.Message;
import nro.models.network.MySession;
import nro.models.player_system.Template.BgItem;
import nro.models.server.Manager;

/** Verifies command -31 exactly as the active C:\nroteam client reads it. */
public final class BgItemProtocolTest {

    private static final class CaptureSession extends MySession {
        byte command;
        byte[] payload;

        CaptureSession(Socket socket) {
            super(socket);
        }

        @Override
        public void sendMessage(Message message) {
            command = message.command;
            payload = message.getData();
        }
    }

    private static BgItem item(int id, int imageId, int layer, int dx, int dy) {
        BgItem bg = new BgItem();
        bg.id = id;
        bg.idImage = (short) imageId;
        bg.layer = (byte) layer;
        bg.dx = (short) dx;
        bg.dy = (short) dy;
        return bg;
    }

    private static void assertEntry(DataInputStream in, int id, int imageId,
            int layer, int dx, int dy) throws Exception {
        if (in.readShort() != id || in.readShort() != imageId
                || in.readByte() != layer || in.readShort() != dx
                || in.readShort() != dy || in.readByte() != 0) {
            throw new AssertionError("BG template record differs from active client protocol");
        }
    }

    public static void main(String[] args) throws Exception {
        // Sparse IDs catch the old position-as-ID writer even if sizes match.
        Manager.BG_ITEMS.clear();
        Manager.BG_ITEMS.add(item(37, 2012, 2, -3, 4));
        Manager.BG_ITEMS.add(item(3032, 2014, 2, 0, 0));

        try (ServerSocket listener = new ServerSocket(0);
                Socket clientSocket = new Socket("127.0.0.1", listener.getLocalPort());
                Socket serverSocket = listener.accept()) {
            CaptureSession session = new CaptureSession(serverSocket);
            DataGame.sendDataItemBG(session);
            if (session.command != -31 || session.payload == null) {
                throw new AssertionError("Command -31 was not sent");
            }
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(session.payload));
            if (in.readShort() != 2) {
                throw new AssertionError("Template count differs from record count");
            }
            assertEntry(in, 37, 2012, 2, -3, 4);
            assertEntry(in, 3032, 2014, 2, 0, 0);
            if (in.available() != 0) {
                throw new AssertionError("Trailing bytes after BG templates");
            }
            System.out.println("PASS: explicit BG IDs and two sparse records match active client");
        }
    }
}
