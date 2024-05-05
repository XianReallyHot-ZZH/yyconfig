package cn.youyou.yyconfig.client.config;

import java.util.Map;

/**
 * 自定义propertySource的source对象的实现
 */
public class YYConfigServiceImpl implements YYConfigService {

    /**
     * 包含了该应用在配置中心的所有配置项
     * key:配置项的key，value:配置项的值
     */
    Map<String, String> config;

    public YYConfigServiceImpl(Map<String, String> config) {
        this.config = config;
    }

    public String[] getPropertyNames() {
        return this.config.keySet().toArray(new String[0]);
    }

    public String getProperty(String name) {
        return this.config.get(name);
    }
}
