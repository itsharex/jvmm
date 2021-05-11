package org.beifengtz.jvmm.core.service;

import org.beifengtz.jvmm.tools.factory.ExecutorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>
 * Description: TODO
 * </p>
 * <p>
 * Created in 17:18 2021/5/11
 *
 * @author beifengtz
 */
public class DefaultScheduledService implements ScheduleService {
    private static final Logger log = LoggerFactory.getLogger(DefaultScheduledService.class);

    private final ScheduledExecutorService executor;
    private final AtomicReference<Runnable> task = new AtomicReference<>();
    private final AtomicReference<Thread.State> state;
    private final AtomicBoolean stopFlag = new AtomicBoolean(false);
    private final AtomicBoolean stopOnError = new AtomicBoolean(true);
    private final AtomicInteger timeGap = new AtomicInteger(10);
    private final String taskName;
    private final AtomicInteger targetTimes = new AtomicInteger(-1);
    private final AtomicInteger timesCounter = new AtomicInteger(0);

    public DefaultScheduledService(String taskName) {
        this(taskName, ExecutorFactory.getScheduleThreadPool());
    }

    public DefaultScheduledService(String taskName, ScheduledExecutorService executor) {
        this.taskName = taskName;
        this.executor = executor;
        this.state = new AtomicReference<>(Thread.State.NEW);
    }

    @Override
    public ScheduleService setTask(Runnable task) {
        this.task.set(task);
        return this;
    }

    @Override
    public ScheduleService setTimes(int times) {
        this.targetTimes.set(times);
        return this;
    }

    @Override
    public ScheduleService setTimeGap(int gapSeconds) {
        if (gapSeconds > 0) {
            this.timeGap.set(gapSeconds);
        }
        return this;
    }

    @Override
    public ScheduleService setStopOnError(boolean stopOnError) {
        this.stopOnError.set(stopOnError);
        return this;
    }

    @Override
    public void start() {
        if (state.get() == Thread.State.RUNNABLE) {
            return;
        }
        state.set(Thread.State.RUNNABLE);
        timesCounter.set(0);
        executor.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    if (task.get() != null) {
                        task.get().run();
                        timesCounter.incrementAndGet();
                    }
                } catch (Exception e) {
                    log.error("Execute schedule task error. [" + taskName + "]", e);
                    if (stopOnError.get()) {
                        return;
                    }
                }

                if (!stopFlag.get() || (targetTimes.get() > 0 && timesCounter.get() > targetTimes.get())) {
                    executor.schedule(this, timeGap.get(), TimeUnit.SECONDS);
                } else {
                    log.info("Schedule task stopped. [" + taskName + "]");
                    state.set(Thread.State.TERMINATED);
                }
            }
        }, timeGap.get(), TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        stopFlag.set(true);
    }
}
