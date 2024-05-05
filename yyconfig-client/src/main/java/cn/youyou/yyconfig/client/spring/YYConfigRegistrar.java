package cn.youyou.yyconfig.client.spring;

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
public class YYConfigRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        System.out.println("register PropertySourcesProcessor");

        // 重复注册检查
        Optional<String> first = Arrays.stream(registry.getBeanDefinitionNames())
                .filter(name -> PropertySourcesProcessor.class.getName().equals(name))
                .findFirst();
        if (first.isPresent()) {
            System.out.println("PropertySourcesProcessor already registered");
            return;
        }

        AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder
                .genericBeanDefinition(PropertySourcesProcessor.class)
                .getBeanDefinition();
        registry.registerBeanDefinition(PropertySourcesProcessor.class.getName(), beanDefinition);
    }

}
