package org.beifengtz.jvmm.server.service;

import io.netty.util.concurrent.Promise;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.beifengtz.jvmm.common.factory.ExecutorFactory;
import org.beifengtz.jvmm.core.entity.JvmmData;
import org.beifengtz.jvmm.server.ServerContext;
import org.beifengtz.jvmm.server.entity.conf.SentinelConf;
import org.beifengtz.jvmm.server.entity.conf.SentinelSubscriberConf;
import org.beifengtz.jvmm.server.entity.conf.SentinelSubscriberConf.SubscriberType;
import org.beifengtz.jvmm.server.exporter.HttpExporter;
import org.beifengtz.jvmm.server.exporter.PrometheusExporter;
import org.beifengtz.jvmm.prometheus.Remote;
import org.beifengtz.jvmm.prometheus.Types;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * Description: TODO
 * </p>
 * <p>
 * Created in 09:50 2022/9/7
 *
 * @author beifengtz
 */
public class JvmmSentinelService implements JvmmService {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(JvmmSentinelService.class);

    protected ScheduledExecutorService executor;
    protected ScheduledFuture<?> scheduledFuture;

    protected final Set<ShutdownListener> shutdownListeners = new HashSet<>();

    protected final Queue<SentinelTask> taskList = new ConcurrentLinkedQueue<>();

    protected volatile HttpExporter httpExporter;
    protected volatile PrometheusExporter prometheusExporter;

    public JvmmSentinelService() {
        this(ExecutorFactory.getThreadPool());
    }

    public JvmmSentinelService(ScheduledExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public void start(Promise<Integer> promise) {
        List<SentinelConf> sentinels = ServerContext.getConfiguration().getServer().getSentinel();
        sentinels = sentinels.stream().filter(o -> !o.getSubscribers().isEmpty() && !o.getTasks().isEmpty()).collect(Collectors.toList());
        if (!sentinels.isEmpty()) {

            //  初始化任务
            taskList.clear();
            httpExporter = null;
            prometheusExporter = null;

            long now = System.currentTimeMillis();
            int minInterval = Integer.MAX_VALUE;
            for (SentinelConf conf : sentinels) {
                SentinelTask task = new SentinelTask(conf);
                task.execTime = now;
                minInterval = Math.min(minInterval, conf.getInterval());
                taskList.add(task);
            }

            scheduledFuture = executor.scheduleWithFixedDelay(() -> {
                Iterator<SentinelTask> it = taskList.iterator();
                while (it.hasNext()) {
                    SentinelTask task = it.next();
                    if (System.currentTimeMillis() >= task.execTime) {
                        if (task.run()) {
                            it.remove();
                        }
                    }
                }
            }, 0, minInterval, TimeUnit.SECONDS);

            promise.trySuccess(0);
            logger.info("Jvmm sentinel service started, sentinel num:{}, min interval: {}", sentinels.size(), minInterval);
        } else {
            promise.tryFailure(new RuntimeException("No valid jvmm sentinel configuration."));
        }
    }

    @Override
    public void shutdown() {
        logger.info("Trigger to shutdown jvmm sentinel service...");
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
        for (ShutdownListener listener : shutdownListeners) {
            try {
                listener.onShutdown();
            } catch (Exception e) {
                logger.error("An exception occurred while executing the shutdown listener: " + e.getMessage(), e);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends JvmmService> T addShutdownListener(ShutdownListener listener) {
        shutdownListeners.add(listener);
        return (T) this;
    }

    @Override
    public int hashCode() {
        return 3;
    }

    class SentinelTask {
        public final SentinelConf conf;
        public long execTime;
        public int counter = 0;
        private boolean hasHttpSubscriber;
        private boolean hasPrometheusSubscriber;

        public SentinelTask(SentinelConf conf) {
            for (SentinelSubscriberConf subscriber : conf.getSubscribers()) {
                if (subscriber.getType() == SubscriberType.http && httpExporter == null) {
                    httpExporter = new HttpExporter();
                    hasHttpSubscriber = true;
                } else if (subscriber.getType() == SubscriberType.prometheus && prometheusExporter == null) {
                    prometheusExporter = new PrometheusExporter();
                    hasPrometheusSubscriber = true;
                }
            }
            this.conf = conf;
        }

        /**
         * 执行任务
         *
         * @return true-任务已结束，需要从任务队列中移除；false-还有下一次任务
         */
        public boolean run() {
            counter++;
            if (conf.getCount() > 0 && counter > conf.getCount()) {
                return true;
            }
            executor.execute(() -> {
                try {
                    JvmmService.collectByOptions(conf.getTasks(), conf.getListenedPorts(), conf.getListenedThreadPools(), pair -> {
                        if (pair.getLeft().get() <= 0) {
                            JvmmData data = pair.getRight().setNode(ServerContext.getConfiguration().getName());
                            String dataStr = hasHttpSubscriber ? data.toJsonStr() : null;
                            byte[] dataBytes = hasPrometheusSubscriber ? parsePrometheusData(data) : null;
                            for (SentinelSubscriberConf subscriber : conf.getSubscribers()) {
                                if (subscriber.getType() == SubscriberType.http && httpExporter != null) {
                                    executor.submit(() -> httpExporter.export(subscriber, dataStr));
                                } else if (subscriber.getType() == SubscriberType.prometheus && prometheusExporter != null) {
                                    executor.submit(() -> prometheusExporter.export(subscriber, dataBytes));
                                }
                            }
                            execTime = System.currentTimeMillis() + conf.getInterval() * 1000L;
                        }
                    });
                } catch (Exception e) {
                    logger.error("Sentinel execute task failed: " + e.getMessage(), e);
                }
            });
            return false;
        }

        private byte[] parsePrometheusData(JvmmData data) {
            Remote.WriteRequest.Builder writeRequestBuilder = Remote.WriteRequest.newBuilder();
            Types.MetricMetadata.Builder builder = Types.MetricMetadata.newBuilder();
            builder.setType(Types.MetricMetadata.MetricType.UNKNOWN);
            builder.setMetricFamilyName(data.getNode());
            builder.setHelp("helper");
            Types.MetricMetadata metricMetadata = builder.build();
            writeRequestBuilder.addMetadata(metricMetadata);

            List<Types.Label> labels = new ArrayList<>();
            Types.TimeSeries.Builder timeSeriesBuilder = Types.TimeSeries.newBuilder();
            //  自定义标签信息
            Types.Label nameLabel = Types.Label.newBuilder().setName("__name__").setValue(data.getNode()).build();
            labels.add(nameLabel);

            //  写入的时间戳到毫秒级
            Types.Sample sample = Types.Sample.newBuilder()
                    .setTimestamp(System.currentTimeMillis())
                    .setValue(Double.parseDouble(resultValues[1])).build();

            // 远程写入prometheus
            timeSeriesBuilder.addAllLabels(labels);
            timeSeriesBuilder.addSamples(sample);
            writeRequestBuilder.addTimeseries(timeSeriesBuilder.build());

            return writeRequestBuilder.build().toByteArray();
        }
    }
}
