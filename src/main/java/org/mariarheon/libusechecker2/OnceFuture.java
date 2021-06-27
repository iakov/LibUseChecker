package org.mariarheon.libusechecker2;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class OnceFuture {
    private final static ScheduledExecutorService service = Executors.newScheduledThreadPool(100);

    private final Runnable runnable;
    private final int delay;
    private final TimeUnit unit;
    private ScheduledFuture<?> scheduledFuture;
    private boolean done;
    private final boolean shouldRepeat;

    public OnceFuture(Runnable runnable, int delay, TimeUnit unit, boolean shouldRepeat) {
        this.runnable = runnable;
        this.delay = delay;
        this.unit = unit;
        this.shouldRepeat = shouldRepeat;
    }

    public OnceFuture(Runnable runnable, int delay, TimeUnit unit) {
        this(runnable, delay, unit, false);
    }

    public void scheduleOrReschedule() {
        synchronized (this) {
            if (!shouldRepeat && done) {
                return;
            }
        }
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
        scheduledFuture = service.schedule(this::modifiedRunnable, delay, unit);
    }

    private void modifiedRunnable() {
        synchronized (this) {
            if (!shouldRepeat && done) {
                return;
            }
            done = true;
        }
        this.runnable.run();
        // runnable = null;
        // scheduledFuture = null;
    }
}
