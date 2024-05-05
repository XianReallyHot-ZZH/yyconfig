package cn.youyou.yyconfig.client.config;

import cn.youyou.yyconfig.client.meta.ConfigMeta;
import cn.youyou.yyconfig.client.repository.YYRepository;
import cn.youyou.yyconfig.client.repository.YYRepositoryChangeListener;
import org.springframework.context.ApplicationContext;

import java.util.Map;

/**
 * 自定义propertySource的source对象
 */
public interface YYConfigService extends YYRepositoryChangeListener {

    static YYConfigService getDefault(ApplicationContext applicationContext, ConfigMeta configMeta) {
        // 获取配置中心的仓储服务
        YYRepository repository = YYRepository.getDefault(configMeta);
        // 获取配置中心下该应用的所有配置项
        Map<String, String> config = repository.getConfig();
        // 实例化YYConfigService
        YYConfigServiceImpl configService = new YYConfigServiceImpl(applicationContext, config);
        // 向仓储服务注册监听器,由于仓储服务引用了configService，同时configService是不会被回收的（放置到了spring的environment中），
        // 所以该仓储服务对象也不会被回收，虽然这里是一个局部变量，因此这里可以放心的注册监听器
        repository.addChangeListener(configService);
        // 返回实例化后的YYConfigService
        return configService;
    }

    String[] getPropertyNames();

    String getProperty(String name);

}
