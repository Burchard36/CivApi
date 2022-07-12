package org.example.socket.user;

import lombok.Getter;
import org.java_websocket.WebSocket;


@Getter
public class ConnectedClient {

    protected final WebSocket clientSocket;
    protected final String ipAddress;

    public ConnectedClient(final WebSocket clientConnection) {
        this.clientSocket = clientConnection;
        this.ipAddress = clientConnection.getRemoteSocketAddress().getAddress().getHostAddress();
    }
}
