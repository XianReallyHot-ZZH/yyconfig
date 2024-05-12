package cn.youyou.yyconfig.client.value;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.Field;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpringValue {

    // bean
    private Object bean;
    // beanName
    private String beanName;
    // 配置key
    private String key;
    // 注解上配置的原始字符串
    private String placeholder;
    // 属性
    private Field field;

}
