package cn.youyou.yyconfig.client.repository;

import cn.youyou.yyconfig.client.meta.ConfigMeta;

import java.util.Map;

public interface YYRepository {

    static YYRepository getDefault(ConfigMeta meta) {
        return new YYRepositoryImpl(meta);
    }

    /**
     * 获取应用的所有配置项
     *
     * @return
     */
    Map<String, String> getConfig();

    /**
     * 添加配置项变更的监听器
     *
     * @param listener
     */
    void addChangeListener(YYRepositoryChangeListener listener);

}
