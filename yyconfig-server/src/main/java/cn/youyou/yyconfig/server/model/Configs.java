package cn.youyou.yyconfig.server.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模拟数据库中的配置信息数据结构
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Configs {

    // 应用名称
    private String app;
    // 环境名称
    private String env;
    // 命名空间
    private String ns;
    // 配置项key
    private String pkey;
    // 配置项value
    private String pval;

}
