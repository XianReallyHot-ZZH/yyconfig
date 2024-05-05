package cn.youyou.yyconfig.server.mapper;

import cn.youyou.yyconfig.server.model.Configs;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Configs表的Mapper
 */
@Repository
@Mapper
public interface ConfigsMapper {

    /**
     * 查询指定环境下的对应应用的所有配置
     *
     * @param app
     * @param env
     * @param ns
     * @return
     */
    @Select("select * from configs where app = #{app} and env = #{env} and ns = #{ns}")
    List<Configs> list(String app, String env, String ns);

    /**
     * 查询指定环境下的对应应用的指定配置
     *
     * @param app
     * @param env
     * @param ns
     * @param pkey
     * @return
     */
    @Select("select * from configs where app = #{app} and env = #{env} and ns = #{ns} and pkey = #{pkey}")
    Configs select(String app, String env, String ns, String pkey);

    /**
     * 插入一条配置信息
     *
     * @param configs
     * @return
     */
    @Insert("insert into configs(app, env, ns, pkey, pval) values(#{app}, #{env}, #{ns}, #{pkey}, #{pval})")
    int insert(Configs configs);

    /**
     * 更新一条配置信息
     *
     * @param configs
     * @return
     */
    @Update("update configs set pval = #{pval} where app = #{app} and env = #{env} and ns = #{ns} and pkey = #{pkey}")
    int update(Configs configs);
}
