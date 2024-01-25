package org.beifengtz.jvmm.server.exporter;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.beifengtz.jvmm.common.QuickFailManager;
import org.beifengtz.jvmm.common.util.CommonUtil;
import org.beifengtz.jvmm.common.util.HttpUtil;
import org.beifengtz.jvmm.server.entity.conf.AuthOptionConf;
import org.beifengtz.jvmm.server.entity.conf.SentinelSubscriberConf;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * description: TODO
 * date: 15:46 2024/1/25
 *
 * @author beifengtz
 */
public class HttpExporter implements JvmmExporter<String> {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(HttpExporter.class);
    protected static final Map<String, String> globalHeaders = CommonUtil.hasMapOf("Content-Type", "application/json;charset=UTF-8");
    /**
     * 连续失败超过 5 次数后进入快失败冷却时间 1 分钟
     */
    protected final QuickFailManager<String> quickFailManager = new QuickFailManager<>(5, (int) TimeUnit.MINUTES.toMillis(1));

    @Override
    public void export(SentinelSubscriberConf subscriber, String data) {
        String url = subscriber.getUrl();
        if (quickFailManager.check(url)) {
            boolean success = false;
            try {
                Map<String, String> headers;
                AuthOptionConf auth = subscriber.getAuth();
                if (auth != null && auth.isEnable()) {
                    headers = new HashMap<>(globalHeaders);
                    String authKey = Base64.getEncoder().encodeToString((auth.getUsername() + ":" + auth.getPassword()).getBytes(StandardCharsets.UTF_8));
                    headers.put("Authorization", "Basic " + authKey);
                } else {
                    headers = globalHeaders;
                }

                HttpUtil.post(url, data, headers);
                success = true;
            } catch (IOException e) {
                logger.warn("Can not connect monitor subscriber '{}': {}", url, e.getMessage());
            } catch (Throwable e) {
                logger.error("Monitor publish to " + url + " failed: " + e.getMessage(), e);
            } finally {
                quickFailManager.result(url, success);
            }
        }
    }
}
