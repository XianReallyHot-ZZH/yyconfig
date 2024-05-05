package cn.youyou.yyconfig.client.annotation;

import cn.youyou.yyconfig.client.spring.YYConfigRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import({YYConfigRegistrar.class})
public @interface EnableYYConfig {
}
