package cn.youyou.yyconfig.client.spring;

import cn.youyou.yyconfig.client.config.YYConfigService;
import cn.youyou.yyconfig.client.config.YYPropertySource;
import cn.youyou.yyconfig.client.meta.ConfigMeta;
import lombok.Data;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

@Data
public class PropertySourcesProcessor implements BeanFactoryPostProcessor, ApplicationContextAware, EnvironmentAware, PriorityOrdered {

    private final static String YY_PROPERTIES_SOURCES = "YYPropertySources";
    private final static String YY_PROPERTIES_SOURCE = "YYPropertySource";

    private Environment environment;

    private ApplicationContext applicationContext;

    /**
     * spring的扩展点之一，在bean实例化之前，在beanDefinition读取完之后，在这里可以获取到所有的beanDefinition，以bean的定义，影响后续的实例化方式
     * 作用：这里主要是为了改变ConfigurableEnvironment里面的PropertySources属性的值，配合spring机制，在项目启动阶段，将配置参数从注册中心拉取，注入
     *
     * @param beanFactory
     * @throws BeansException
     */
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        ConfigurableEnvironment ENV = (ConfigurableEnvironment) environment;
        // 放置重复添加
        if (ENV.getPropertySources().contains(YY_PROPERTIES_SOURCES)) {
            return;
        }
        // 通过 http 请求，去yyconfig-sever获取配置
        String app = ENV.getProperty("yyconfig.app", "app1");
        String env = ENV.getProperty("yyconfig.env", "dev");
        String ns = ENV.getProperty("yyconfig.ns", "public");
        String configServer = ENV.getProperty("yyconfig.server", "http://localhost:9129");
        ConfigMeta configMeta = new ConfigMeta(app, env, ns, configServer);

        YYConfigService configService = YYConfigService.getDefault(applicationContext, configMeta);
        YYPropertySource propertySource = new YYPropertySource(YY_PROPERTIES_SOURCE, configService);
        // CompositePropertySource方便后续追加自定义的PropertySource
        CompositePropertySource composite = new CompositePropertySource(YY_PROPERTIES_SOURCES);
        composite.addPropertySource(propertySource);
        ENV.getPropertySources().addFirst(composite);
    }

    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
