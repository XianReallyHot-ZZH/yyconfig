package cn.youyou.yyconfig.client.repository;

import cn.kimmking.utils.HttpUtils;
import cn.youyou.yyconfig.client.meta.ConfigMeta;
import com.alibaba.fastjson.TypeReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class YYRepositoryImpl implements YYRepository {

    // 注册中心配置
    ConfigMeta meta;

    // 应用配置的版本号，key: 指向应用的唯一字符串, value: 版本号
    Map<String, Long> versionMap = new HashMap<>();

    // 应用的所有配置项，key: 指向应用的唯一字符串, value: 所有配置项
    Map<String, Map<String, String>> configMap = new HashMap<>();

    // 定时任务，用于定时轮询服务端的配置版本
    ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    // 持有的监听器
    List<YYRepositoryChangeListener> listeners = new ArrayList<>();

    public YYRepositoryImpl(ConfigMeta meta) {
        this.meta = meta;
        executor.scheduleWithFixedDelay(this::heartbeat, 1000, 5000, TimeUnit.MILLISECONDS);
    }

    @Override
    public Map<String, String> getConfig() {
        String configKey = meta.genKey();
        // 如果缓存中有，直接返回
        if (configMap.containsKey(configKey)) {
            return configMap.get(configKey);
        }
        // 缓存中没有，则从服务端拉取
        return findAll();
    }

    private Map<String, String> findAll() {
        System.out.println("[YYCONFIG] list all configs from kk config server.");
        List<Configs> configs = HttpUtils.httpGet(meta.listPath(), new TypeReference<List<Configs>>() {
        });
        Map<String, String> resultMap = new HashMap<>();
        configs.forEach(config -> resultMap.put(config.getPkey(), config.getPval()));
        return resultMap;
    }

    @Override
    public void addChangeListener(YYRepositoryChangeListener listener) {
        this.listeners.add(listener);
    }

    /**
     * 轮询（心跳），检测服务端配置的最新版本号
     */
    private void heartbeat() {
        try {
            Long versionRemote = HttpUtils.httpGet(meta.versionPath(), new TypeReference<Long>() {
            });
            String configKey = meta.genKey();
            Long versionLocal = versionMap.getOrDefault(configKey, -1L);
            // 如果服务端有更新，则更新本地版本号和配置
            if (versionRemote > versionLocal) {
                System.out.println("[KKCONFIG] remote version = " + versionRemote + ", local version = " + versionLocal
                        + ", need update new configs from server.");
                versionMap.put(configKey, versionRemote);
                Map<String, String> newConfigs = findAll();
                configMap.put(configKey, newConfigs);
                // 通知(触发)监听器
                listeners.forEach(listener -> listener.onChange(new YYRepositoryChangeListener.ConfigChangeEvent(meta, newConfigs)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
