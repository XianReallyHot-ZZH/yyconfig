package cn.youyou.yyconfig.demo;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 模拟验证spring提供配置处理的第二种用法：@ConfigurationProperties 生成注解配置类的方式
 */
@Data
@ConfigurationProperties(prefix = "yy")
public class YYDemoConfig {

    String a;
    String b;
    String c;

}
