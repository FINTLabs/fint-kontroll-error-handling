package no.fintlabs.slack;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/** Sends Slack messages off the request or logging thread, with a bounded backlog. */
@Slf4j
public class SlackNotificationDispatcher implements AutoCloseable {

    private static final int DEFAULT_QUEUE_CAPACITY = 256;

    private final SlackMessenger messenger;
    private final ThreadPoolExecutor executor;
    private final AtomicLong droppedMessages = new AtomicLong();

    public SlackNotificationDispatcher(SlackMessenger messenger) {
        this(messenger, DEFAULT_QUEUE_CAPACITY);
    }

    SlackNotificationDispatcher(SlackMessenger messenger, int queueCapacity) {
        this.messenger = messenger;
        this.executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity), task -> {
                    Thread thread = new Thread(task, "kontroll-slack-notifications");
                    thread.setDaemon(true);
                    return thread;
                }, new ThreadPoolExecutor.AbortPolicy());
    }

    public void dispatch(String message) {
        try {
            executor.execute(() -> {
                try {
                    messenger.sendErrorMessage(message);
                } catch (RuntimeException exception) {
                    log.warn("Unable to send Slack error notification", exception);
                }
            });
        } catch (RejectedExecutionException exception) {
            long dropped = droppedMessages.incrementAndGet();
            if (dropped == 1 || dropped % 100 == 0) {
                log.warn("Slack notification queue is full or shutting down; dropped {} messages", dropped);
            }
        }
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
