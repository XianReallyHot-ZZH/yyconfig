package cn.youyou.yyconfig.server;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 使用mysql实现的分布式锁，进而实现server端的选主
 * 选主要求：1、只有一个主节点；2、主节点宕机，有其他节点补充上来，越及时越好；
 * 对应分布式锁要求：1、排它性；2、锁释放后，能有其他事务马上获取，越及时越好；
 */
@Slf4j
@Component
public class DistributedLock {

    @Autowired
    DataSource dataSource;

    // 维护一个单独使用的连接
    Connection connection;

    // 锁状态
    @Getter
    private AtomicBoolean locked = new AtomicBoolean(false);

    // 定时任务,负责不断尝试获取锁
    ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    @PostConstruct
    public void init() {
        try {
            connection = dataSource.getConnection();
            // 设置事务为非自动提交, 事务的完整性由用户掌控
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        } catch (Exception e) {
            log.error(" >>>>>> [yyconfig] 分布式锁初始化失败.", e);
        }
        executor.scheduleWithFixedDelay(this::tryLock, 1000, 5000, TimeUnit.MILLISECONDS);
    }

    // 尝试获取锁,根据获取结果，更新锁状态
    private void tryLock() {
        try {
            // 获取锁
            lock();
            // 成功获取, 更新锁状态为true
            locked.set(true);
        } catch (Exception e) {
            // 获取锁失败，更新锁状态为false
            locked.set(false);
            log.info(" >>>>>> [yyconfig] 分布式锁获取失败， lock failed...");
        }
    }

    /**
     * 基于mysql的行锁实现分布式锁
     * 原理: 只有一个事务能获取到行锁（重复获取）, 其他事务在获取同一个行锁的时候会等待，达到等待超时时间就报错退出
     */
    private void lock() throws SQLException {
        // 设置事务为非自动提交, 事务的完整性由用户掌控
//        connection.setAutoCommit(false);
//        connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        connection.createStatement().execute("set innodb_lock_wait_timeout=5");
        // 获取行锁，如果获取不到，会一直等待，直到超时，报错退出
        connection.createStatement().execute("select app from locks where id = 1 for update");

        if (locked.get()) {
            log.info(" >>>>>> [yyconfig] reenter this dist lock...");
        } else {
            log.info(" >>>>>> [yyconfig] get a dist lock!!!");
        }

        // 事务永远不主动提交，如果获取到行锁了，就一直保持事务状态，不然一旦释放，就会被其他事务获取，不符合分布式锁的排他性要求了，
        // 只有当当前服务宕机，才会释放锁，其他服务获取到锁成为server中的主节点
    }

    // 优雅停，触发资源释放
    @PreDestroy
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.rollback();
                connection.close();
            }
        } catch (Exception e) {
            log.error("ignore this close exception");
        }
    }


}
