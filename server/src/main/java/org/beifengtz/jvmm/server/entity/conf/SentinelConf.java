package org.beifengtz.jvmm.server.entity.conf;

import org.beifengtz.jvmm.server.enums.CollectionType;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Description: TODO
 * </p>
 * <p>
 * Created in 11:32 2022/9/7
 *
 * @author beifengtz
 */
public class SentinelConf {
    private List<SentinelSubscriberConf> subscribers = new ArrayList<>();
    /**
     * 采集项
     */
    private List<CollectionType> tasks = new ArrayList<>();
    private int count = -1;
    /**
     * 采集周期，秒
     */
    private int interval = 10;

    public List<SentinelSubscriberConf> getSubscribers() {
        return subscribers;
    }

    public SentinelConf setSubscribers(List<SentinelSubscriberConf> subscribers) {
        this.subscribers = subscribers;
        return this;
    }

    public List<CollectionType> getTasks() {
        return tasks;
    }

    public SentinelConf setTasks(List<CollectionType> tasks) {
        this.tasks = tasks;
        return this;
    }

    public SentinelConf addTasks(CollectionType task) {
        this.tasks.add(task);
        return this;
    }

    public SentinelConf clearTasks() {
        this.tasks.clear();
        return this;
    }

    public int getCount() {
        return count;
    }

    public SentinelConf setCount(int count) {
        this.count = count;
        return this;
    }

    public int getInterval() {
        return interval;
    }

    public SentinelConf setInterval(int interval) {
        this.interval = interval;
        return this;
    }
}
