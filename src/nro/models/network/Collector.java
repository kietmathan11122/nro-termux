package nro.models.network;

import java.net.Socket;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import nro.models.interfaces.IMessageHandler;
import nro.models.interfaces.IMessageSendCollect;
import nro.models.interfaces.ISession;

public final class Collector
        implements Runnable {

    // Application-level firewall proof. The server sends no identifying data
    // until this exact proof is received in the first -27 packet.
    private static final byte[] CLIENT_PROOF = (
            "NQGATE1:4c8f2b11a0e7436d9d257cb835f14e26"
            + "7a091e5c3b684f32a821d90c6e47bb15")
            .getBytes(StandardCharsets.US_ASCII);

    private ISession session;
    private DataInputStream dis;
    private IMessageSendCollect collect;
    private IMessageHandler messageHandler;

    public Collector(ISession session, Socket socket) {
        this.session = session;
        this.setSocket(socket);
    }

    public Collector setSocket(Socket socket) {
        try {
            this.dis = new DataInputStream(socket.getInputStream());
        } catch (IOException iOException) {
        }
        return this;
    }

    @Override
    public void run() {
        try {
            while (this.session != null && this.session.isConnected()) {
                Message msg = this.collect.readMessage(this.session, this.dis);

                Session concreteSession = (Session) this.session;
                if (!concreteSession.isVerifiedClient()) {
                    if (!this.verifyClientProof(msg)) {
                        msg.cleanup();
                        throw new IOException("Client proof rejected");
                    }
                    concreteSession.markClientVerified();
                    this.session.sendKey();
                } else if (msg.command == -27) {
                    msg.cleanup();
                    throw new IOException("Duplicate key request");
                } else {
                    this.messageHandler.onMessage(this.session, msg);
                }
                msg.cleanup();
            }
        } catch (Exception exception) {
        }
        try {
            Network.gI().getAcceptHandler().sessionDisconnect(this.session);
        } catch (Exception exception) {
        }
        if (this.session != null) {
            this.session.disconnect();
        }
    }

    private boolean verifyClientProof(Message msg) throws IOException {
        if (msg.command != -27 || msg.reader().available() != CLIENT_PROOF.length) {
            return false;
        }

        int difference = 0;
        for (byte expected : CLIENT_PROOF) {
            difference |= msg.reader().readUnsignedByte() ^ (expected & 0xFF);
        }
        return difference == 0;
    }

    public void setCollect(IMessageSendCollect collect) {
        this.collect = collect;
    }

    public void setMessageHandler(IMessageHandler handler) {
        this.messageHandler = handler;
    }

    public void close() {
        if (this.dis != null) {
            try {
                this.dis.close();
            } catch (IOException iOException) {
            }
        }
    }

    public void dispose() {
        this.session = null;
        this.dis = null;
        this.collect = null;
    }
}
