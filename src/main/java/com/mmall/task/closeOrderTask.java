package com.mmall.task;

import com.mmall.common.Const;
import com.mmall.common.RedissonManager;
import com.mmall.service.IOrderService;
import com.mmall.util.PropertiesUtil;
import com.mmall.util.RedisShardedPoolUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

/**
 * Created by Allen
 * 定时关单
 */
@Component
@Slf4j
public class closeOrderTask {
    @Autowired
    private IOrderService iOrderService;

    @Autowired
    private RedissonManager redissonManager;

    /*版本1 适合没有Tomcat集群的环境 由于是集群需要创建分布式锁，
    这个是没有分布式锁的，单Tomcat简单版
    如果在Tomcat集群环境中使用，那么每个Tomcat都会执行一遍任务
    浪费资源的同时还会导致系统数据错乱。可以使用redis分布式锁来解决这个问题
     */
//    @Scheduled(cron = "0 */1 * * * ?")//每一分钟（每个1分钟的整数倍）
    public void closeOrderTaskV1() {
        log.info("关闭订单定时任务启动");
        int hour = Integer.parseInt(PropertiesUtil.getProperty("close.order.task.time.hour", "2"));
        iOrderService.closeOrder(hour);
        log.info("关闭订单定时任务结束");
    }

    //    @Scheduled(cron = "0 */1 * * * ?")//每一分钟（每个1分钟的整数倍）
    public void closeOrderTaskV2() {
        log.info("关闭订单定时任务启动");
        //lockTimeout，锁的超时时间
        long lockTimeout = Long.parseLong(PropertiesUtil.getProperty("lock.timeout", "5000"));
        Long setnxResult = RedisShardedPoolUtil.setnx(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK, String.valueOf(System.currentTimeMillis() + lockTimeout));
        if (setnxResult != null && setnxResult.intValue() == 1) {
            //如果返回值是1，代表设置成功，获取到锁，调用closeOrder方法，给锁设置有效期后再关闭订单再删除锁
            closeOrder(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
        } else {
            //如果没获取到锁
            log.info("没有获得分布式锁：{}", Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
        }
        log.info("关闭订单定时任务结束");
    }

    @Scheduled(cron = "0 */1 * * * ?")//每一分钟（每个1分钟的整数倍）
    public void closeOrderTaskV3() {
        log.info("关闭订单定时任务启动");
        //获取这个锁的超时时间
        long lockTimeout = Long.parseLong(PropertiesUtil.getProperty("lock.timeout", "5000"));
        //使用setnx,创建锁，key为先前设置好的名称，value为当前时间+超时时间
        Long setnxResult = RedisShardedPoolUtil.setnx(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK, String.valueOf(System.currentTimeMillis() + lockTimeout));
        //如果setnx后的结果不为null并且值为1就说明创建成功已经获取到锁了，直接调用关单功能，传入这个锁的名字
        if (setnxResult != null && setnxResult.intValue() == 1) {
            closeOrder(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
        } else {
            //没有获取到锁，继续判断，判断时间戳，看是否可以重置并获取到锁
            //通过key获取这个锁的值，也就是这个锁的 ( 创建时间+超时时间 )
            String lockValueStr = RedisShardedPoolUtil.get(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
            //如果这个值不为空并且（此时的系统时间>创建时间+超时时间）就说明这个锁已经超时了，可以重新获取锁了！
            if (lockValueStr != null && System.currentTimeMillis() > Long.parseLong(lockValueStr)) {
                //使用getset方法重新设置value，并且获取原来key的值（和lockValueStr原理相同）set一个新的value值，get获取旧的值
                //而getset操作是具有原子性的，不用担心期间有其他进程修改
                String getSetResult = RedisShardedPoolUtil.getSet(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK, String.valueOf(System.currentTimeMillis() + lockTimeout));
                //返回给定的key的旧值 ->旧值判断，是否可以获取锁
                //当key没有旧值时，即key不存在时，返回nil (null) ->获取锁
                //get返回值为null说明锁已经消失了，所以可以现在获取
                //lockValueStr和getSetResult相等意味着这段程序执行期间没有其他进程改变这个值，可以获取锁，如果不同，因为是Tomcat集群，
                // 说明这个值被其他就进程修改了，就不能获取锁。防死锁。
                if (getSetResult == null || (getSetResult != null && StringUtils.equals(lockValueStr, getSetResult))) {
                    //真正获取到锁
                    closeOrder(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
                } else {
                    log.info("没有获得分布式锁:{}", Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
                }
            } else {//这个else以为这旧的锁还没有失效
                log.info("没有获得分布式锁:{}", Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
            }
        }
        log.info("关闭订单定时任务结束");
    }

    //使用redisson搞定分布式锁
    @Scheduled(cron = "0 */1 * * * ?")//每一分钟（每个1分钟的整数倍）
    public void closeOrderTaskV4() {
        RLock lock = redissonManager.getRedisson().getLock(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
        boolean getLock = false;
        try {
            /*tryLock(long waitTime, long leaseTime, TimeUnit unit)
            * waitTime 尝试加锁，最多能等待的时间
            * leaseTime 多长时间后自动释放锁
            * */
            if ( getLock = lock.tryLock(0, 5, TimeUnit.SECONDS)){
                log.info("Redisson获取分布式锁：{}，ThreadName：{}",Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK,Thread.currentThread().getName());
                int hour = Integer.parseInt(PropertiesUtil.getProperty("close.order.task.time.hour", "2"));
                iOrderService.closeOrder(hour);
            }else {
                log.info("Redissonm没有获取到分布式锁：{}，ThreadName：{}",Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK,Thread.currentThread().getName());
            }
        } catch (InterruptedException e) {
            log.error("Redisson分布式锁获取异常",e);
        }finally {
            if (!getLock){
                return;
            }
            lock.unlock();
            log.info("Redisson分布式锁释放锁");
        }
    }

    //这个关闭订单的功能是在iOrderService.closeOrder(hour);的基础上增加了有效期
    private void closeOrder(String lockName) {
        //对这个key设置有效期
        RedisShardedPoolUtil.expire(lockName, 5);//有效期50秒 防止死锁
        log.info("获取{}，ThreadName:{}", Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK, Thread.currentThread().getName());
        int hour = Integer.parseInt(PropertiesUtil.getProperty("close.order.task.time.hour", "2"));
        iOrderService.closeOrder(hour);
        //释放锁
        RedisShardedPoolUtil.del(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
        log.info("释放{}，ThreadName:{}", Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK, Thread.currentThread().getName());
        log.info("==============");
    }
}
