package org.beifengtz.jvmm.convey.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.util.concurrent.EventExecutorGroup;

/**
 * <p>
 * Description: TODO
 * </p>
 * <p>
 * Created in 16:35 2021/5/17
 *
 * @author beifengtz
 */
public interface HandlerProvider {

    EventExecutorGroup EXECUTOR_GROUP = new DefaultEventLoopGroup(2 * Runtime.getRuntime().availableProcessors());

    ChannelHandler getHandler();

    default int getReaderIdle() {
        return 10;
    }

    default String getName() {
        return "handler";
    }

    default EventExecutorGroup getGroup() {
        return null;
    }
}
