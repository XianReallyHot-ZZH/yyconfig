package cn.youyou.yyconfig.client.repository;

import cn.youyou.yyconfig.client.meta.ConfigMeta;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 监听器
 * 监听事件：监听配置中心的配置更新事件
 * 监听反应：更新propertySource中的source的本地配置
 */
public interface YYRepositoryChangeListener {

    /**
     * 监听事件
     *
     * @param event
     */
    void onChange(ConfigChangeEvent event);

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    class ConfigChangeEvent {
        private ConfigMeta configMeta;
        private Map<String, String> config;
    }

}
