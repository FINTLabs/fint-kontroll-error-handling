package no.fintlabs.slack;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class SlackNotificationDispatcherTest {

    @Test
    void deliversMessagesOffTheCallingThreadAndDropsWhenTheQueueIsFull() throws Exception {
        SlackMessenger messenger = mock(SlackMessenger.class);
        CountDownLatch sendingFirst = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        doAnswer(invocation -> {
            sendingFirst.countDown();
            releaseFirst.await(5, TimeUnit.SECONDS);
            return true;
        }).when(messenger).sendErrorMessage("first");

        try (SlackNotificationDispatcher dispatcher = new SlackNotificationDispatcher(messenger, 1)) {
            dispatcher.dispatch("first");
            assertThat(sendingFirst.await(2, TimeUnit.SECONDS)).isTrue();

            dispatcher.dispatch("second");
            dispatcher.dispatch("third");
            verify(messenger, never()).sendErrorMessage("second");

            releaseFirst.countDown();
        } finally {
            releaseFirst.countDown();
        }

        verify(messenger).sendErrorMessage("first");
        verify(messenger).sendErrorMessage("second");
        verify(messenger, never()).sendErrorMessage("third");
    }
}
