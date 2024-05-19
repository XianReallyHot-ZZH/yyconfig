package cn.youyou.yyconfig.server;

import cn.youyou.yyconfig.server.mapper.ConfigsMapper;
import cn.youyou.yyconfig.server.model.Configs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
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
     * 某个具体应用的版本信息的异步请求结果集合
     */
    MultiValueMap<String, DeferredResult<Long>> versionDeferredMap = new LinkedMultiValueMap<>();

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
        // 处理异步请求
        List<DeferredResult<Long>> deferredResults = versionDeferredMap.remove(app + "-" + env + "-" + ns);
        if (deferredResults != null && deferredResults.size() > 0) {
            deferredResults.forEach(deferredResult -> {
                deferredResult.setResult(VERSIONS.get(app + "-" + env + "-" + ns));
            });
        }

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
     * 异步请求，进行挂载的请求方式，配合DeferredResult机制，如果服务端有更新，由于请求挂载在服务端，会直接调用DeferredResult的setResult方法，返回最新的版本号
     * @param app
     * @param env
     * @param ns
     * @param vers
     * @return
     */
    @GetMapping("/versiondeffered")
    public DeferredResult<Long> versiondeffered(@RequestParam("app") String app,
                        @RequestParam("env") String env,
                        @RequestParam("ns") String ns,
                        @RequestParam("vers") Long vers) {
        // 如果client端的版本并没有明确落后，那么进行挂载监听
        DeferredResult<Long> deferredResult = new DeferredResult<>();
        String service = app + "-" + env + "-" + ns;
        Long versionAtServer = VERSIONS.getOrDefault(service, -1L);

        // 设置等待超时处理逻辑，超时后，会调用onTimeout方法，然后触发AsyncRequestTimeoutException，调用我们设置的全局异常处理器GlobalExceptionHandler
        deferredResult.onTimeout(() -> {
            log.info(" >>>>>> [yyconfig] service:{} deferredResult onTimeout", service);
            versionDeferredMap.get(service).remove(deferredResult);
        });
        // 设置onCompletion方法逻辑，其他地方调用了本deferredResult的setResult方法，会触发onCompletion方法
        deferredResult.onCompletion(() -> {
            log.info(" >>>>>> [yyconfig] service:{} deferredResult onCompletion", service);
//            versionDeferredMap.remove(service);
        });

        // 如果client端已经落后了，那直接返回就行了，不需要进行异步挂载DeferredResult, 不等更新接口操作的触发了
        if (vers < versionAtServer) {
            log.info(" >>>>>> [yyconfig] service:{} client version is behind, return version:{}", service, versionAtServer);
            deferredResult.setResult(versionAtServer);
            List<DeferredResult<Long>> deferredResults = versionDeferredMap.get(service);
            if (deferredResults != null && deferredResults.size() > 0) {
                deferredResults.remove(deferredResult);
            }
        }

        versionDeferredMap.add(service, deferredResult);
        return deferredResult;
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
