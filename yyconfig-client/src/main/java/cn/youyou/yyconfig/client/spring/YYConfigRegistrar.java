package cn.youyou.yyconfig.client.spring;

import cn.youyou.yyconfig.client.value.SpringValueProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Arrays;
import java.util.Optional;

/**
 * 负责将自定义的bean注册至spring中
 */
@Slf4j
public class YYConfigRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        registerClass(registry, PropertySourcesProcessor.class);
        registerClass(registry, SpringValueProcessor.class);
    }

    private static void registerClass(BeanDefinitionRegistry registry, Class<?> aClass) {
        log.info("[YYCONFIG] register {}", aClass.getName());

        // 重复注册检查
        Optional<String> first = Arrays.stream(registry.getBeanDefinitionNames())
                .filter(name -> aClass.getName().equals(name))
                .findFirst();
        if (first.isPresent()) {
            log.info("[YYCONFIG] {} already registered", aClass.getCanonicalName());
            return;
        }

        AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder
                .genericBeanDefinition(aClass)
                .getBeanDefinition();
        registry.registerBeanDefinition(aClass.getName(), beanDefinition);
    }

}
