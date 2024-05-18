package cn.youyou.yyconfig.server;

import cn.youyou.yyconfig.server.mapper.ConfigsMapper;
import cn.youyou.yyconfig.server.model.Configs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class YyConfigController {

    @Autowired
    DistributedLock lock;

    @Autowired
    ConfigsMapper mapper;

    /**
     * 具体到某个环境下的某个应用的配置的版本信息
     * 作用：当客户端的版本号与服务端最新版本号不一致时，客户端会拉取最新的配置，用于判定客户端侧配置是否需要更新
     */
    Map<String, Long> VERSIONS = new HashMap<>();

    /**
     * 查询返回某个具体应用的所有配置项信息
     *
     * @param app
     * @param env
     * @param ns
     * @return
     */
    @GetMapping("/list")
    public List<Configs> list(@RequestParam("app") String app,
                              @RequestParam("env") String env,
                              @RequestParam("ns") String ns) {
        return mapper.list(app, env, ns);
    }

    /**
     * 更新某个具体应用的配置信息(支持多个配置项),并返回该应用的所有最新的配置项信息
     *
     * @param app
     * @param env
     * @param ns
     * @param params
     */
    @RequestMapping("/update")
    public List<Configs> update(@RequestParam("app") String app,
                                @RequestParam("env") String env,
                                @RequestParam("ns") String ns,
                                @RequestBody Map<String, String> params) {
        params.forEach((k, v) -> {
            insertOrUpdate(new Configs(app, env, ns, k, v));
        });
        // 更新版本信息
        VERSIONS.put(app + "-" + env + "-" + ns, System.currentTimeMillis());
        return mapper.list(app, env, ns);
    }

    private void insertOrUpdate(Configs configs) {
        Configs conf = mapper.select(configs.getApp(), configs.getEnv(), configs.getNs(), configs.getPkey());
        if (conf == null) {
            mapper.insert(configs);
        } else {
            mapper.update(configs);
        }
    }

    /**
     * 查询返回某个具体应用的配置信息的最新版本号
     *
     * @param app
     * @param env
     * @param ns
     * @return
     */
    @GetMapping("/version")
    public Long version(@RequestParam("app") String app,
                        @RequestParam("env") String env,
                        @RequestParam("ns") String ns) {
        return VERSIONS.getOrDefault(app + "-" + env + "-" + ns, -1L);
    }


    /**
     * 判断当前服务器是否是主节点
     * @return
     */
    @GetMapping("/isMasterServer")
    public boolean isMasterServer() {
        return lock.getLocked().get();
    }

}
