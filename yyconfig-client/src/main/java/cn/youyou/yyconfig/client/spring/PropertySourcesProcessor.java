package cn.youyou.yyconfig.client.spring;

import cn.youyou.yyconfig.client.config.YYConfigServiceImpl;
import cn.youyou.yyconfig.client.config.YYPropertySource;
import lombok.Data;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import java.util.HashMap;
import java.util.Map;

@Data
public class PropertySourcesProcessor implements BeanFactoryPostProcessor, EnvironmentAware, PriorityOrdered {

    private final static String YY_PROPERTIES_SOURCES = "YYPropertySources";
    private final static String YY_PROPERTIES_SOURCE = "YYPropertySource";

    private Environment environment;

    /**
     * spring的扩展点之一，在bean实例化之前，在beanDefinition读取完之后，在这里可以获取到所有的beanDefinition，以bean的定义，影响后续的实例化方式
     * 作用：这里主要是为了改变ConfigurableEnvironment里面的PropertySources属性的值，配合spring机制，在项目启动阶段，将配置参数从注册中心拉取，注入
     *
     * @param beanFactory
     * @throws BeansException
     */
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        ConfigurableEnvironment env = (ConfigurableEnvironment) environment;
        // 放置重复添加
        if (env.getPropertySources().contains(YY_PROPERTIES_SOURCES)) {
            return;
        }
        // 通过 http 请求，去yyconfig-sever获取配置 TODO
        Map<String, String> config = new HashMap<>();
        config.put("yy.a", "dev500");
        config.put("yy.b", "b600");
        config.put("yy.c", "c700");
        YYConfigServiceImpl configService = new YYConfigServiceImpl(config);
        YYPropertySource propertySource = new YYPropertySource(YY_PROPERTIES_SOURCE, configService);
        // CompositePropertySource方便后续追加自定义的PropertySource
        CompositePropertySource composite = new CompositePropertySource(YY_PROPERTIES_SOURCES);
        composite.addPropertySource(propertySource);
        env.getPropertySources().addFirst(composite);
    }

    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
