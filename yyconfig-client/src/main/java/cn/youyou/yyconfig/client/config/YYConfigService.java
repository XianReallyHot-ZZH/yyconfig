package cn.youyou.yyconfig.client.config;

/**
 * 自定义propertySource的source对象
 */
public interface YYConfigService {

    String[] getPropertyNames();

    String getProperty(String name);

}
