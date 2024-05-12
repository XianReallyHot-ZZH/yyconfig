package cn.youyou.yyconfig.client.util;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Stack;

/**
 * 处理spring value注解上占位符字符串中的key
 */
public class PlaceholderHelper {

    // 配置占位符
    private static final String PLACEHOLDER_PREFIX = "${";
    private static final String PLACEHOLDER_SUFFIX = "}";
    // 默认值
    private static final String VALUE_SEPARATOR = ":";
    // 简单占位符
    private static final String SIMPLE_PLACEHOLDER_PREFIX = "{";
    // spring表达式 SPEL
    private static final String EXPRESSION_PREFIX = "#{";
    private static final String EXPRESSION_SUFFIX = "}";

    private PlaceholderHelper() {
    }

    private static PlaceholderHelper INSTANCE = new PlaceholderHelper();

    public static PlaceholderHelper getInstance() {
        return INSTANCE;
    }

    /**
     * Resolve placeholder property values 处理出指定bean的占位符字符串（配置）对应的真实值
     * 功能："${somePropertyValue}" -> "the actual property value"
     *
     * @param beanFactory
     * @param beanName
     * @param placeholder
     * @return
     */
    public Object resolvePropertyValue(ConfigurableBeanFactory beanFactory, String beanName, String placeholder) {
        // resolve string value
        String strVal = beanFactory.resolveEmbeddedValue(placeholder);
        BeanDefinition bd = beanFactory.containsBean(beanName) ? beanFactory.getMergedBeanDefinition(beanName) : null;
        // resolve expressions like "#{systemProperties.myProp}"
        return evaluateBeanDefinitionString(beanFactory, strVal, bd);
    }

    private Object evaluateBeanDefinitionString(ConfigurableBeanFactory beanFactory, String value, BeanDefinition beanDefinition) {
        if (beanFactory.getBeanExpressionResolver() == null) {
            return value;
        }
        Scope scope = beanDefinition != null ? beanFactory.getRegisteredScope(beanDefinition.getScope()) : null;
        return beanFactory.getBeanExpressionResolver().evaluate(value, new BeanExpressionContext(beanFactory, scope));
    }

    /**
     * 从占位符字符串中提取出配置项的key
     * Extract keys from placeholder, e.g.
     * <ul>
     * <li>${some.key} => "some.key"</li>
     * <li>${some.key:${some.other.key:100}} => "some.key", "some.other.key"</li>
     * <li>${${some.key}} => "some.key"</li>
     * <li>${${some.key:other.key}} => "some.key"</li>
     * <li>${${some.key}:${another.key}} => "some.key", "another.key"</li>
     * <li>${a}.${b} => "a", "b"</li>
     * <li>#{new java.text.SimpleDateFormat('${some.key}').parse('${another.key}')} => "some.key", "another.key"</li>
     * </ul>
     *
     * @param propertyString
     * @return
     */
    public Set<String> extractPlaceholderKeys(String propertyString) {
        Set<String> placeholderKeys = new LinkedHashSet<>();

        // 既不是普通的占位符字符串，也不是SpEL表达式带占位符的字符串，那么就直接返回，结果就已经是key了，不需要在进行解析了
        if (!isNormalizedPlaceholder(propertyString) && !isExpressionWithPlaceholder(propertyString)) {
            return placeholderKeys;
        }

        Stack<String> stack = new Stack<>();
        stack.push(propertyString);

        while (!stack.isEmpty()) {
            String strVal = stack.pop();
            int startIndex = strVal.indexOf(PLACEHOLDER_PREFIX);
            // 字符串中已经没有占位符了，那就是key了，放到结果中
            if (startIndex == -1) {
                placeholderKeys.add(strVal);
                continue;
            }

            // 找到占位符${}里}的结束位置，支持处理占位符嵌套的情况，类似回文处理，就是要找到第一个"${"对应的"}"
            int endIndex = findPlaceholderEndIndex(strVal, startIndex);
            if (endIndex == -1) {   // 可能是个非法的占位符，这种情况一般不会出现
                // invalid placeholder?
                continue;
            }

            // 提取出最外层占位符内含的String
            String placeholderCandidate = strVal.substring(startIndex + PLACEHOLDER_PREFIX.length(), endIndex);

            // ${${some.key:other.key}}这种情况，直接嵌套的情况,placeholderCandidate对应了${some.key:other.key}，需要继续嵌入分析
            if (placeholderCandidate.startsWith(PLACEHOLDER_PREFIX)) {
                stack.push(placeholderCandidate);
            } else {
                // 有默认值的情况下，placeholderCandidate对应了some.key:other.key或者some.key:${some.other.key:100}等情况
                int separatorIndex = placeholderCandidate.indexOf(VALUE_SEPARATOR);

                if (separatorIndex == -1) {     // 没有默认值，那么就是结果key了
                    stack.push(placeholderCandidate);   // 这里我觉得可以值放置进结果里，然后跳出本次循环就行了，但是它这里是放置进stack等待下次弹出放置到结果里，效果一样，多了一步
                } else {
                    stack.push(placeholderCandidate.substring(0, separatorIndex));  // 取出key，放置进stack等待下次弹出后放置进结果中
                    // 处理默认值对应的":"后面的字符串，返回null或者最外层占位符内含的字符串，比如some.key:${some.other.key:100}，默认值是${some.other.key:100}，defaultValuePart等于some.other.key:100
                    String defaultValuePart = normalizeToPlaceholder(placeholderCandidate.substring(separatorIndex + VALUE_SEPARATOR.length()));
                    if (StringUtils.hasText(defaultValuePart)) {
                        stack.push(defaultValuePart);
                    }
                }
            }

            // 还有一种情况, e.g. ${a}.${b}，前面的逻辑处理完了${a}，处理剩余的字符串
            if (endIndex + PLACEHOLDER_SUFFIX.length() < strVal.length() - 1) {
                String remainingPart = normalizeToPlaceholder(strVal.substring(endIndex + PLACEHOLDER_SUFFIX.length()));
                if (StringUtils.hasText(remainingPart)) {
                    stack.push(remainingPart);
                }
            }
        }

        return placeholderKeys;
    }

    private boolean isNormalizedPlaceholder(String propertyString) {
        return propertyString.startsWith(PLACEHOLDER_PREFIX) && propertyString.endsWith(PLACEHOLDER_SUFFIX);
    }

    private boolean isExpressionWithPlaceholder(String propertyString) {
        return propertyString.startsWith(EXPRESSION_PREFIX) && propertyString.endsWith(EXPRESSION_SUFFIX)
                && propertyString.contains(PLACEHOLDER_PREFIX);
    }

    /**
     * 处理默认值对应的":"后面的字符串
     * @param strVal
     * @return
     */
    private String normalizeToPlaceholder(String strVal) {
        int startIndex = strVal.indexOf(PLACEHOLDER_PREFIX);
        if (startIndex == -1) { // 不含占位符了，直接返回null
            return null;
        }
        int endIndex = strVal.lastIndexOf(PLACEHOLDER_SUFFIX);
        if (endIndex == -1) {   // 不含占位符了，直接返回null
            return null;
        }

        // 处理处最外层占位符内含的字符串
        return strVal.substring(startIndex, endIndex + PLACEHOLDER_SUFFIX.length());
    }

    private int findPlaceholderEndIndex(CharSequence buf, int startIndex) {
        int index = startIndex + PLACEHOLDER_PREFIX.length();
        int withinNestedPlaceholder = 0;
        while (index < buf.length()) {
            if (StringUtils.substringMatch(buf, index, PLACEHOLDER_SUFFIX)) {
                if (withinNestedPlaceholder > 0) {
                    withinNestedPlaceholder--;
                    index = index + PLACEHOLDER_SUFFIX.length();
                } else {
                    return index;
                }
            } else if (StringUtils.substringMatch(buf, index, SIMPLE_PLACEHOLDER_PREFIX)) {
                withinNestedPlaceholder++;
                index = index + SIMPLE_PLACEHOLDER_PREFIX.length();
            } else {
                index++;
            }
        }
        return -1;
    }

    public static void main(String[] args) {

        String strVal = "${some.key}";  // some.key
        System.out.println(new PlaceholderHelper().extractPlaceholderKeys(strVal));
        strVal = "${some.key:other.key}";   // some.key
        System.out.println(new PlaceholderHelper().extractPlaceholderKeys(strVal));
        strVal = "${some.key:${some.other.key:100}}";   // some.key, some.other.key
        System.out.println(new PlaceholderHelper().extractPlaceholderKeys(strVal));
        strVal = "${${some.key}}";  // some.key
        System.out.println(new PlaceholderHelper().extractPlaceholderKeys(strVal));
        strVal = "${${some.key:other.key}}";    // some.key
        System.out.println(new PlaceholderHelper().extractPlaceholderKeys(strVal));
        strVal = "${${some.key}:${another.key}}";   // some.key, another.key
        System.out.println(new PlaceholderHelper().extractPlaceholderKeys(strVal));
        strVal = "${a}.${b}";   // a, b
        System.out.println(new PlaceholderHelper().extractPlaceholderKeys(strVal));
        strVal = "#{new java.text.SimpleDateFormat('${some.key}').parse('${another.key}')}";    // some.key, another.key
        System.out.println(new PlaceholderHelper().extractPlaceholderKeys(strVal));
    }

}
