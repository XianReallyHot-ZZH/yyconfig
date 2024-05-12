package cn.youyou.yyconfig.client.value;

import cn.youyou.yyconfig.client.util.PlaceholderHelper;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * spring value processor 专门处理spring的value注解
 * 1、扫描所有bean，找到所有value注解的地方，保存起来
 * 2、当配置发生变化时，更新value注解的值
 */
@Slf4j
public class SpringValueProcessor implements BeanPostProcessor, BeanFactoryAware, ApplicationListener<EnvironmentChangeEvent> {

    static final PlaceholderHelper helper = PlaceholderHelper.getInstance();

    // 存储所有value注解的出处，key为配置key，value为抽象出来的SpringValue对象，方便后面反射注入
    static final MultiValueMap<String, SpringValue> VALUE_HOLDER = new LinkedMultiValueMap<>();

    @Setter
    private BeanFactory beanFactory;


    /**
     * 完成对Spring @Value注解的扫描
     * @param bean the new bean instance
     * @param beanName the name of the bean
     * @return
     * @throws BeansException
     */
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        // 扫描bean，找到所有value注解的属性，保存起来
        findAnnotatedField(bean.getClass(), Value.class).forEach(field -> {
            // 处理每个属性，根据value注解信息，封装成SpringValue对象，保存起来
            String placeholder = field.getAnnotation(Value.class).value();
            log.info("[YYCONFIG] >> find spring @value annotation, placeholder:{}, from beanName:{}, field:{}", placeholder, beanName, field);
            // 提取出注解上占位符字符串中的所有配置项key
            helper.extractPlaceholderKeys(placeholder).forEach(key -> {
                log.info("[YYCONFIG] >> find spring value key: {}, from placeholder:{}, beanName:{}, field:{}", key, placeholder, beanName, field);
                SpringValue springValue = new SpringValue(bean, beanName, key, placeholder, field);
                VALUE_HOLDER.add(key, springValue);
            });
        });
        return bean;
    }

    /**
     * 扫描指定class中所有标注了指定注解的属性
     * @param aClass
     * @param annotationClass
     * @return
     */
    private List<Field> findAnnotatedField(Class<?> aClass, Class<? extends Annotation> annotationClass) {
        List<Field> result = new ArrayList<>();
        while (aClass != null) {
            Field[] fields = aClass.getDeclaredFields();
            for (Field field : fields) {
                if (field.isAnnotationPresent(annotationClass)) {
                    result.add(field);
                }
            }
            aClass = aClass.getSuperclass();
        }
        return result;
    }


    /**
     * spring的时间机制，监听EnvironmentChangeEvent事件
     * 配置发生变化时，利用反射更新value注解的值
     *
     * @param event
     */
    @Override
    public void onApplicationEvent(EnvironmentChangeEvent event) {
        log.info("[YYCONFIG] >> update spring value for keys: {}", event.getKeys());
        event.getKeys().forEach(key -> {
            log.info("[YYCONFIG] >> update spring value: {}", key);
            List<SpringValue> springValues = VALUE_HOLDER.get(key);
            if (springValues == null || springValues.isEmpty()) {
                return;
            }
            springValues.forEach(springValue -> {
                log.info("[YYCONFIG] >> update spring value: {} for key {}", springValue, key);
                try {
                    // 利用spring的相关机制，获取value注解对应的最新的真实值
                    // 原理：spring提供了获取Value注解最新值的方法，注解上的最新的配置真实值的来源是：Environment中的propertySource，只要propertySource更新了，那么就能映射计算得到value最新的真实值
                    Object actualValue = helper.resolvePropertyValue((ConfigurableBeanFactory) beanFactory, springValue.getBeanName(), springValue.getPlaceholder());
                    log.info("[YYCONFIG] >> update actual value: {} for holder {}", actualValue, springValue.getPlaceholder());
                    springValue.getField().setAccessible(true);
                    springValue.getField().set(springValue.getBean(), actualValue);
                } catch (Exception ex) {
                    log.error("[YYCONFIG] >> update spring value error for springValue:{}", springValue, ex);
                }
            });
        });
    }
}
