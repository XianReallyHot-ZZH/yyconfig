package cn.youyou.yyconfig.client.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 自定义propertySource的source对象的实现
 */
@Slf4j
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
        // 拿到的是当前应用的全量配置，计算出变更的配置项，只对有变更的配置进行更新
        Set<String> diffKeys = calcChangedKeys(this.config, event.getConfig());
        if (diffKeys.isEmpty()) {
            log.info("[YYCONFIG] calcChangedKeys return empty, ignore update.");
            return;
        }
        // 更新本地source中的配置
        this.config = event.getConfig();
        log.info("[YYCONFIG] fire an EnvironmentChangeEvent with changed keys: {}", diffKeys);
        applicationContext.publishEvent(new EnvironmentChangeEvent(diffKeys));
    }

    private Set<String> calcChangedKeys(Map<String, String> localConfigs, Map<String, String> remoteConfigs) {
        if (localConfigs.isEmpty()) {
            return remoteConfigs.keySet();
        }
        if (remoteConfigs.isEmpty()) {
            return localConfigs.keySet();
        }
        // 远程配置和本地配置不一样的（1、不一样的，2、远程多出来的配置）
        Set<String> diff = remoteConfigs.keySet().stream().filter(key -> !remoteConfigs.get(key).equals(localConfigs.get(key))).collect(Collectors.toSet());
        // 不在远程的配置，可能远程删掉了配置，那么这个配置其实也需要进行变更，该key要以本地的配置为准了
        localConfigs.keySet().stream().filter(key -> !remoteConfigs.containsKey(key)).forEach(diff::add);
        return diff;
    }
}
