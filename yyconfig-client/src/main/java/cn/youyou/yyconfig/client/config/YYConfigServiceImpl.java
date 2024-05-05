package cn.youyou.yyconfig.client.config;

import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationContext;

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

    ApplicationContext applicationContext;

    public YYConfigServiceImpl(ApplicationContext applicationContext, Map<String, String> config) {
        this.applicationContext = applicationContext;
        this.config = config;
    }

    public String[] getPropertyNames() {
        return this.config.keySet().toArray(new String[0]);
    }

    public String getProperty(String name) {
        return this.config.get(name);
    }

    @Override
    public void onChange(ConfigChangeEvent event) {
        System.out.println("[YYCONFIG] 监听到配置变更，执行本地配置项变更...");
        this.config = event.getConfig();
        if (!this.config.isEmpty()) {
            System.out.println("[YYCONFIG] fire an EnvironmentChangeEvent with keys:" + config.keySet());
            // spring cloud对配置项变更的监听，后续触发后会动态变更由@ConfigurationProperties注解注入的配置项（bean）
            applicationContext.publishEvent(new EnvironmentChangeEvent(config.keySet()));
        }
    }
}
