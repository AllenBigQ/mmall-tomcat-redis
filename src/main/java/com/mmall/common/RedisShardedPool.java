package com.mmall.common;

import com.mmall.util.PropertiesUtil;
import redis.clients.jedis.*;
import redis.clients.util.Hashing;
import redis.clients.util.Sharded;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Allen
 */
public class RedisShardedPool {
    private static ShardedJedisPool pool;//sharded jedis连接池
    private static Integer maxTotal = Integer.parseInt(PropertiesUtil.getProperty("redis.max.total","20")); //最大连接数
    private static Integer maxIdle = Integer.parseInt(PropertiesUtil.getProperty("redis.max.idle","20"));//在jedispool中最大的idle状态(空闲的)的jedis实例的个数
    private static Integer minIdle = Integer.parseInt(PropertiesUtil.getProperty("redis.min.idle","20"));//在jedispool中最小的idle状态(空闲的)的jedis实例的个数

    private static Boolean testOnBorrow = Boolean.parseBoolean(PropertiesUtil.getProperty("redis.test.borrow","true"));//在borrow一个jedis实例的时候，是否要进行验证操作，如果赋值true。则得到的jedis实例肯定是可以用的。
    private static Boolean testOnReturn = Boolean.parseBoolean(PropertiesUtil.getProperty("redis.test.return","true"));//在return一个jedis实例的时候，是否要进行验证操作，如果赋值true。则放回jedispool的jedis实例肯定是可以用的。

    private static String redis1Ip = PropertiesUtil.getProperty("redis1.ip");
    private static Integer redis1Port = Integer.parseInt(PropertiesUtil.getProperty("redis1.port"));


    private static String redis2Ip = PropertiesUtil.getProperty("redis2.ip");
    private static Integer redis2Port = Integer.parseInt(PropertiesUtil.getProperty("redis2.port"));

    private static void initPool(){
        JedisPoolConfig config = new JedisPoolConfig();

        config.setMaxTotal(maxTotal);
        config.setMaxIdle(maxIdle);
        config.setMinIdle(minIdle);

        config.setTestOnBorrow(testOnBorrow);
        config.setTestOnReturn(testOnReturn);

        config.setBlockWhenExhausted(true);//连接耗尽的时候，是否阻塞，false会抛出异常，true阻塞直到超时。默认为true。

        //JedisShardInfo类包含了jedis服务器的一些信息，每个代表一个真实的节点，我们有两个redis，所以需要创建两个
        //redis，传入ip，端口，默认也是2秒
        JedisShardInfo info1 = new JedisShardInfo(redis1Ip,redis1Port,1000*2);
        JedisShardInfo info2 = new JedisShardInfo(redis2Ip,redis2Port,1000*2);
        //创建list集合存放所有的节点
        List<JedisShardInfo> jedisShardInfoList = new ArrayList<JedisShardInfo>(2);
        jedisShardInfoList.add(info1);
        jedisShardInfoList.add(info2);
        //第一个参数是config配置，第二个参数是list，存放的是节点数，第三个参数是hashing调用murmur_hash策略，这是默认策略
        //还有一个是MD5策略，这个策略对应的就是一致性算法。MD5 is really not good
        pool = new ShardedJedisPool(config,jedisShardInfoList, Hashing.MURMUR_HASH, Sharded.DEFAULT_KEY_TAG_PATTERN);

    }

    static{
        initPool();
    }
    /*获取一个ShardedJedis，ShardedJedis继承BinaryShardedJedis再继承Sharded
    *
     * Sharded中initialize源码，其中有一个weight属性，权重默认为1，所以默认创建
      * 的虚拟节点个数为160，改成2，虚拟节点就是320个，然后将字符串hash后作为键(虚拟节点)，
      * shardInfo作为值存到一个TreeMap中(这是模拟一致性哈希算法中将虚拟节点映射
      * 到环上的操作)，最后将shardInfo与对应的jedis类的映射关系存储到resources里。
    private void initialize(List<S> shards) {
	nodes = new TreeMap<Long, S>();

	for (int i = 0; i != shards.size(); ++i) {
	    final S shardInfo = shards.get(i);
	    if (shardInfo.getName() == null)
		for (int n = 0; n < 160 * shardInfo.getWeight(); n++) {
		    nodes.put(this.algo.hash("SHARD-" + i + "-NODE-" + n),
			    shardInfo);
		}
	    else
		for (int n = 0; n < 160 * shardInfo.getWeight(); n++) {
		    nodes.put(
			    this.algo.hash(shardInfo.getName() + "*"
				    + shardInfo.getWeight() + n), shardInfo);
		}
	    resources.put(shardInfo, shardInfo.createResource());
	}
    }
    如何根据一个key找到存在哪个里面呢
    public R getShard(byte[] key) {
	return resources.get(getShardInfo(key));
    }

    tailMap(K fromKey)->返回其键大于或者等于的部分，如果这个SortedMap为空
    说明这个key在这个环的末尾，没有比它还大的了，所以按照顺时针，直接取这个环node的
    第一个节点，如果不为空，就取第一个，也就是按照顺时针里这个key最近的节点，这就是一致性哈希

    public S getShardInfo(byte[] key) {
	SortedMap<Long, S> tail = nodes.tailMap(algo.hash(key));
	if (tail.isEmpty()) {
	    return nodes.get(nodes.firstKey());
	}
	return tail.get(tail.firstKey());
    }
*/
    public static ShardedJedis getJedis(){
        return pool.getResource();
    }


    public static void returnBrokenResource(ShardedJedis jedis){
        pool.returnBrokenResource(jedis);
    }



    public static void returnResource(ShardedJedis jedis){
        pool.returnResource(jedis);
    }

    public static void main(String[] args) {
        ShardedJedis jedis = pool.getResource();

        for(int i =0 ;i<10;i++){
            jedis.set("key"+i,"value"+1);
        }

        returnResource(jedis);

        pool.destroy();//临时调用，销毁连接池中的所有连接
        System.out.println("program is end");


    }

}
