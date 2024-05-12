package cn.youyou.yyconfig.demo;

import cn.youyou.yyconfig.client.annotation.EnableYYConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@Slf4j
@SpringBootApplication
@EnableConfigurationProperties({YYDemoConfig.class})
@EnableYYConfig
@RestController
public class YyconfigDemoApplication {

    @Value("${yy.a}.${yy.c}")
    private String a;

    @Value("${yy.b}")
    private String b;

    @Autowired
    private YYDemoConfig yyDemoConfig;

    @Autowired
    Environment environment;

    public static void main(String[] args) {
        SpringApplication.run(YyconfigDemoApplication.class, args);
    }

    @GetMapping("/demo")
    public String demo() {
        return "yy.a = " + a + "\n"
                + "yy.b = " + b + "\n"
                + "demo.a = " + yyDemoConfig.getA() + "\n"
                + "demo.b = " + yyDemoConfig.getB() + "\n";
    }

    @Bean
    public ApplicationRunner applicationRunner() {
        // 打印看一下environment，观察一下propertySource
        // spring加载配置信息的原理就是：将配置文件的内容加载到environment中，存储在propertySource中，
        // 然后扫描注解，在bean加载的时候从environment的propertySource中按顺序获取配置信息，注入到bean中
        System.out.println(Arrays.toString(environment.getActiveProfiles()));
        // 项目启动完毕打印加载下来的应用业务配置信息
        return args -> {
            log.info("a(from @value) = {}", a);
            log.info("demo.a(from @ConfigurationProperties) = {}", yyDemoConfig.getA());
            log.info("b(from @value) = {}", b);
            log.info("demo.b(from @ConfigurationProperties) = {}", yyDemoConfig.getB());
        };
    }

}
