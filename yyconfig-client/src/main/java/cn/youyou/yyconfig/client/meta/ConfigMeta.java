package cn.youyou.yyconfig.client.meta;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对应到配置中心的配置信息，即当前应用配置的yyconfig 配置
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConfigMeta {

    String app;
    String env;
    String ns;
    String server;

    /**
     * 返回配置中心侧对某一个具体应用的配置的key
     *
     * @return
     */
    public String genKey() {
        return this.getApp() + "_" + this.getEnv() + "_" + this.getNs();
    }

    /**
     * 对应配置中心侧的list接口的请求路径
     *
     * @return
     */
    public String listPath() {
        return path("list");
    }

    public String versionPath() {
        return path("version");
    }

    private String path(String context) {
        return this.getServer() + "/" + context +
                "?app=" + this.getApp() + "&env=" + this.getEnv() + "&ns=" + this.getNs();
    }

}
