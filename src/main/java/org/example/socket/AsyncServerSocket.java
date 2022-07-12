package org.example.socket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.ludovicianul.prettylogger.PrettyLogger;
import lombok.Getter;
import org.example.socket.action.ActionRunner;
import org.example.socket.action.MethodHolder;
import org.example.socket.action.SocketAction;
import org.example.socket.interfaces.ActionHandler;
import org.example.socket.json.ActionDataStructure;
import org.example.socket.user.ConnectedClient;
import org.example.utils.Errors;
import org.example.utils.Loggable;
import org.example.utils.json.ParsingResult;
import org.example.utils.json.SafeJsonParser;
import org.example.utils.notifier.Notifier;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AsyncServerSocket extends Thread {

    protected final Executor threadPoolExecutor;
    protected final SafeJsonParser safeJsonParser;
    public final Notifier startNotifer;

    protected final Loggable loggable = new Loggable(this.getClass());
    protected final PrettyLogger log = loggable.logger();
    protected static AsyncServerSocket INSTANCE;
    protected final ActionRunner runner;

    protected final Gson gson;

    @Getter
    protected final ServerSettings serverSettings;

    protected final int port;
    protected WebSocketServer server;
    protected final HashMap<UUID, ConnectedClient> connectedClients;

    public AsyncServerSocket(final int port) {
        this.port = port;
        this.threadPoolExecutor = Executors.newWorkStealingPool();
        this.safeJsonParser = new SafeJsonParser();
        this.startNotifer = new Notifier();
        this.runner = new ActionRunner();
        this.gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        this.serverSettings = new ServerSettings();
        this.connectedClients = new HashMap<>();
        this.loggable.startAsync("Startup");
        this.log.start("Initializing AsyncServerSocket ...");
        this.server = new WebSocketServer(new InetSocketAddress(this.port)) {

            @Override
            public void onOpen(WebSocket conn, ClientHandshake handshake) {
                assignUuid(conn);
                CompletableFuture.runAsync(() -> {
                    logNewConnection(conn);

                });
            }

            @Override
            public void onClose(WebSocket conn, int code, String reason, boolean remote) {
                clearUuid(conn);
                CompletableFuture.runAsync(() -> {
                    logDisconnection(conn);
                });
            }

            @Override
            public void onMessage(WebSocket conn, String message) {
                CompletableFuture.runAsync(() -> {
                    UUID uuid = UUID.randomUUID();
                    loggable.startAsync(uuid.toString());
                    log.start("Received message from client: %s".formatted(message));
                    logMessage(conn, message);
                    ParsingResult parsingResult = safeJsonParser.parseJson(ActionDataStructure.class,
                            message,
                            gson);

                    if (!handleParsingResult(parsingResult, conn)) {
                        logActionCompleted(conn, uuid);
                        return;
                    }

                    final ActionDataStructure structure = (ActionDataStructure) parsingResult.getData();
                    final String action = structure.getAction();
                    final Object data = structure.getData();
                    final MethodHolder methodHolder = runner.getAction(action);

                    if (methodHolder == null) {
                        logInvalidAction(conn);
                        conn.send(Errors.malformedJsonObject().toString());
                        logActionCompleted(conn, uuid);
                        return;
                    }
                    parsingResult = safeJsonParser.parseJson(methodHolder.getParsingClass(),
                            data,
                            gson);
                    if (!handleParsingResult(parsingResult, conn)) {
                        logActionCompleted(conn, uuid);
                        return;
                    }
                    final Object parsedData = parsingResult.getData();
                    final ConnectedClient connectedClient = getConnectedClient(conn.getAttachment());
                    methodHolder.invokeMethod(parsedData, connectedClient);
                    logActionCompleted(conn, uuid);
                }, threadPoolExecutor);
            }

            @Override
            public void onError(WebSocket conn, Exception ex) {
                CompletableFuture.runAsync(ex::printStackTrace);
            }

            @Override
            public void onStart() {
                CompletableFuture.runAsync(() -> {

                    final String time = loggable.prettyAsyncMilliElapsed("Startup");
                    log.complete("Successfully started AsyncServerSocket! (Completed in %s)"
                            .formatted(time));
                    startNotifer.notifySync();
                });
            }
        };

    }

    protected boolean handleParsingResult(ParsingResult parsingResult, WebSocket conn) {
        if (parsingResult.isInvalidSchema()) {
            logInvalidSchema(conn);
            conn.send(Errors.malformedJsonObject().toString());
            return false;
        } else if (parsingResult.isInvalidJson()) {
            logInvalidJson(conn);
            conn.send(Errors.malformedJsonObject().toString());
            return false;
        } else return true;
    }

    @SafeVarargs
    public final void registerSocketActions(Class<? extends SocketAction>... actions) {
        for (final Class<? extends SocketAction> actionClass : actions) {
            for (Method method : actionClass.getMethods()) {
                if (method.isAnnotationPresent(ActionHandler.class)) {
                    for (Class<?> parameter : method.getParameterTypes()) {
                        if (parameter == ConnectedClient.class) continue;
                        runner.addAction(actionClass.getSimpleName(), new MethodHolder(
                                method,
                                parameter,
                                actionClass
                        ));
                        this.log.info("Successfully registered socket action: " + actionClass.getSimpleName());
                    }
                }
            }
        }
    }

    public final void startSocket() {
        this.server.start();
    }

    public final ConnectedClient getConnectedClient(UUID uuid) {
        return this.connectedClients.get(uuid);
    }

    protected final void assignUuid(final WebSocket socket) {
        final UUID uuid = UUID.randomUUID();
        if (this.getConnectedClient(uuid) != null) {
            this.assignUuid(socket);
        } else {
            socket.setAttachment(uuid);
            this.connectedClients.putIfAbsent(uuid, new ConnectedClient(socket));
        }
    }

    protected final void clearUuid(final WebSocket socket) {
        final UUID uuid = (UUID) socket.getAttachment();
        this.connectedClients.remove(uuid);
    }

    protected final void logNewConnection(final WebSocket socket) {
        if (!this.getServerSettings().isLogNewConnections()) return;
        this.log.info("Client has connected with Temporary UUID: %s. (IP: %s)"
                .formatted(socket.getAttachment().toString(), socket.getRemoteSocketAddress().getAddress().getHostAddress()));
    }

    protected final void logDisconnection(final WebSocket socket) {
        if (!this.getServerSettings().isLogDisconnections()) return;
        this.log.info("Client has disconnected with Temporary UUID: %s. (IP: %s)"
                .formatted(socket.getAttachment().toString(), socket.getRemoteSocketAddress().getAddress().getHostAddress()));
    }

    protected final void logInvalidSchema(final WebSocket socket) {
        if (!this.getServerSettings().isLogInvalidSchema()) return;
        this.log.warn("Invalid schema received! Client Temporary UUID: %s (IP: %s)"
                .formatted(socket.getAttachment().toString(), socket.getRemoteSocketAddress().getAddress().getHostAddress()));
    }

    protected final void logInvalidJson(final WebSocket socket) {
        if (!this.getServerSettings().isLogInvalidJson()) return;
        this.log.warn("Invalid json received! Client Temporary UUID: %s (IP: %s)"
                .formatted(socket.getAttachment().toString(), socket.getRemoteSocketAddress().getAddress().getHostAddress()));
    }

    protected final void logInvalidAction(final WebSocket socket) {
        if (!this.getServerSettings().isLogInvalidAction()) return;
        this.log.warn("Invalid action received! Client Temporary UUID: %s (IP: %s)"
                .formatted(socket.getAttachment().toString(), socket.getRemoteSocketAddress().getAddress().getHostAddress()));
    }

    protected final void logActionCompleted(final WebSocket socket, final UUID timerUuid) {
        final String time = loggable.prettyAsyncMilliElapsed(timerUuid.toString());
        log.complete("Successfully completed ACTION request in %s".formatted(time));
    }


    protected final void logMessage(final WebSocket socket, final String message) {
        if (!this.getServerSettings().isLogMessages()) return;
        this.log.info("Message received! Client Temporary UUID: %s (IP: %s) Message: %s"
                .formatted(socket.getAttachment().toString(),
                        socket.getRemoteSocketAddress().getAddress().getHostAddress(),
                        message));
    }

}
