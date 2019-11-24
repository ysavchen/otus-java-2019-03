package com.mycompany.mutiprocess.message_system;

import com.mycompany.mutiprocess.ms_client.ClientType;
import com.mycompany.mutiprocess.ms_client.Message;
import com.mycompany.mutiprocess.ms_client.MsClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.Socket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class MessageSystemImpl implements MessageSystem {

    private static final int MESSAGE_QUEUE_SIZE = 1_000;
    private static final int MSG_HANDLER_THREAD_LIMIT = 2;

    private final AtomicBoolean runFlag = new AtomicBoolean(true);

    private final Map<UUID, MsClient> clientMap = new ConcurrentHashMap<>();
    private final Map<UUID, Socket> clientSockets = new ConcurrentHashMap<>();
    private final Map<UUID, MessageConsumer> serverSocketMap = new ConcurrentHashMap<>();

    private final BlockingQueue<Message> messageQueue = new ArrayBlockingQueue<>(MESSAGE_QUEUE_SIZE);

    private final ExecutorService msgProcessor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable);
        thread.setName("msg-processor-thread");
        return thread;
    });

    private final ExecutorService msgHandler = Executors.newFixedThreadPool(MSG_HANDLER_THREAD_LIMIT, new ThreadFactory() {
        private final AtomicInteger threadNameSeq = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable);
            thread.setName("msg-handler-thread-" + threadNameSeq.incrementAndGet());
            return thread;
        }
    });

    public MessageSystemImpl() {
        msgProcessor.submit(this::msgProcessor);
    }

    private void msgProcessor() {
        logger.info("msgProcessor started");
        while (runFlag.get()) {
            try {
                Message msg = messageQueue.take();
                if (msg == Message.VOID_MESSAGE) {
                    logger.info("received the stop message");
                } else {
                    clientMap.values().stream()
                            .filter(client -> client.getType() == msg.getTo())
                            .findAny()
                            .ifPresentOrElse(
                                    clientTo -> msgHandler.submit(() -> handleMessage(clientTo, msg)),
                                    () -> logger.warn("client not found"));
                }
            } catch (InterruptedException ex) {
                logger.error(ex.getMessage(), ex);
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
        msgHandler.submit(this::messageHandlerShutdown);
        logger.info("msgProcessor finished");
    }

    private void messageHandlerShutdown() {
        msgHandler.shutdown();
        logger.info("msgHandler has been shut down");
    }

    private void handleMessage(MsClient msClient, Message message) {
        try {
            //if a message supposed to be sent to DB, only one (any) server must get it (otherwise duplicate data is stored)
            if (msClient.getType() == ClientType.DATABASE_SERVICE) {
                serverSocketMap.values()
                        .stream()
                        .filter(server -> server.getType() == msClient.getType())
                        .findAny()
                        .ifPresentOrElse(
                                server -> msClient.sendMessage(message, server.getClientSocket()),
                                () -> logger.warn("server not found"));

            //if a message supposed to be sent to Frontend, both servers must get it, as only one of them has the needed dataConsumer for this message.
            // Currently it's not known which one.
            } else if (msClient.getType() == ClientType.FRONTEND_SERVICE) {
                serverSocketMap.values()
                        .stream()
                        .filter(server -> server.getType() == msClient.getType())
                        .forEach(server -> msClient.sendMessage(message, server.getClientSocket()));
            } else {
                logger.warn("Server with type {} not found", msClient.getType());
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            logger.error("message:{}", msClient);
        }
    }

    private void insertStopMessage() throws InterruptedException {
        boolean result = messageQueue.offer(Message.VOID_MESSAGE);
        while (!result) {
            Thread.sleep(100);
            result = messageQueue.offer(Message.VOID_MESSAGE);
        }
    }

    @Override
    public void addClient(MsClient msClient, Socket clientSocket) {
        logger.info("new client: {}", msClient);
        if (clientMap.containsKey(msClient.getId())) {
            throw new IllegalArgumentException(msClient + " already exists");
        }
        clientMap.put(msClient.getId(), msClient);
        clientSockets.put(msClient.getId(), clientSocket);
    }

    @SneakyThrows
    @Override
    public void addMessageConsumer(MessageConsumer consumer) {
        logger.info("new server: {}", consumer);
        serverSocketMap.put(consumer.getId(), consumer);
    }

    @SneakyThrows
    @Override
    public void removeClient(MsClient msClient) {
        MsClient removedClient = clientMap.remove(msClient.getId());
        Socket clientSocket = clientSockets.remove(msClient.getId());
        clientSocket.close();
        if (removedClient == null) {
            logger.warn("client not found: {}", msClient);
        } else {
            logger.info("removed client: {}", removedClient);
        }
    }

    @Override
    public boolean newMessage(Message msg) {
        if (runFlag.get()) {
            return messageQueue.offer(msg);
        } else {
            logger.warn("MS is being shutting down... rejected:{}", msg);
            return false;
        }
    }

    @Override
    public void dispose() throws InterruptedException {
        runFlag.set(false);
        insertStopMessage();
        msgProcessor.shutdown();
        msgHandler.awaitTermination(60, TimeUnit.SECONDS);
    }
}
