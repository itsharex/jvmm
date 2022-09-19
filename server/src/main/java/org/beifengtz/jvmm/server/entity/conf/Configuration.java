package org.beifengtz.jvmm.server.entity.conf;

import com.google.gson.Gson;
import org.beifengtz.jvmm.common.util.FileUtil;
import org.beifengtz.jvmm.common.util.IOUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * <p>
 * Description: TODO
 * </p>
 * <p>
 * Created in 16:48 2021/5/22
 *
 * @author beifengtz
 */
public final class Configuration {

    private String name = "jvmm-server";
    private ServerConf server = new ServerConf();
    private LogConf log = new LogConf();

    private int workThread = 2;

    /**
     * 从一个地址中载入配置，这个地址需要指定到一个yml文件，配置格式需满足与 Configuration 类一一对应，
     * 这个地址的格式，可以是一个http或https的网络地址，也可以是一个文件路径
     */
    public static Configuration parseFromUrl(String url) {
        try {
            if (url.startsWith("http://") || url.startsWith("https://")) {
                boolean loaded = FileUtil.readFileFromNet(url, "tmp", "config.yml");
                if (loaded) {
                    return new Gson().fromJson(FileUtil.readYml2Json(new File(url)), Configuration.class);
                } else {
                    throw new RuntimeException("Can not load 'config.yml' from " + url);
                }
            } else {
                return new Gson().fromJson(FileUtil.readYml2Json(new File(url)), Configuration.class);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Configuration parseFromStream(InputStream is) {
        try {
            File tempFile = File.createTempFile("config", "yml");
            try {
                FileUtil.writeByteArrayToFile(tempFile, IOUtil.toByteArray(is));
                return new Gson().fromJson(FileUtil.readYml2Json(tempFile), Configuration.class);
            } finally {
                tempFile.delete();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    public String getName() {
        return name;
    }

    public Configuration setName(String name) {
        this.name = name;
        return this;
    }

    public ServerConf getServer() {
        return server;
    }

    public Configuration setServer(ServerConf server) {
        this.server = server;
        return this;
    }

    public LogConf getLog() {
        return log;
    }

    public Configuration setLog(LogConf log) {
        this.log = log;
        return this;
    }

    public int getWorkThread() {
        return workThread;
    }

    public Configuration setWorkThread(int workThread) {
        this.workThread = workThread;
        return this;
    }
}