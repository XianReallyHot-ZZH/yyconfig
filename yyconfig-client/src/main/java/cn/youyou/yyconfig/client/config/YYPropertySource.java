package cn.youyou.yyconfig.client.config;

import org.springframework.core.env.EnumerablePropertySource;

/**
 * 自定义的 PropertySource，source为自己对配置项管理+存储的对象（YYConfigService）
 * 用法：后续这个PropertySource会被添加到environment中，从而影响spring对配置参数的赋值行为
 */
public class YYPropertySource extends EnumerablePropertySource<YYConfigService> {

    public YYPropertySource(String name, YYConfigService source) {
        super(name, source);
    }

    public String[] getPropertyNames() {
        return source.getPropertyNames();
    }

    public Object getProperty(String name) {
        return source.getProperty(name);
    }
}
