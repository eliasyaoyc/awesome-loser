package com.loserico.cache.operations;

import com.loserico.cache.concurrent.ThreadPool;
import com.loserico.cache.exception.JedisException;
import com.loserico.cache.utils.ByteUtils;
import com.loserico.json.jackson.JacksonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.util.Pool;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * <p>
 * Copyright: (C), 2019/10/24 7:39
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class JedisPoolOperations implements JedisOperations {
	
	private static final Logger log = LoggerFactory.getLogger(JedisPoolOperations.class);
	
	private final Pool<Jedis> pool;
	
	private static final ExecutorService THREAD_POOL = ThreadPool.newThreadPool();
	
	public JedisPoolOperations(Pool<Jedis> pool) {
		this.pool = pool;
	}
	
	@Override
	public String set(byte[] key, byte[] value) {
		return operate((jedis) -> jedis.set(key, value));
	}
	
	@Override
	public Long setnx(byte[] key, byte[] value) {
		return operate((jedis) -> jedis.setnx(key, value));
	}
	
	@Override
	public byte[] get(byte[] key) {
		return operate((jedis) -> jedis.get(key));
	}
	
	@Override
	public Boolean exists(String key) {
		return operate((jedis) -> jedis.exists(key));
	}
	
	@Override
	public Boolean exists(byte[] key) {
		return operate((jedis) -> jedis.exists(key));
	}
	
	@Override
	public Long incr(String key) {
		return operate((jedis) -> jedis.incr(key));
	}
	
	@Override
	public Long incrBy(String key, long increment) {
		return operate((jedis) -> jedis.incrBy(key, increment));
	}
	
	@Override
	public Double zscore(String key, String member) {
		return operate((jedis) -> jedis.zscore(key, member));
	}
	
	@Override
	public Long zadd(String key, double score, String member) {
		return operate((jedis) -> jedis.zadd(key, score, member));
	}
	
	@Override
	public Long zadd(byte[] key, double score, byte[] member) {
		return operate((jedis) -> jedis.zadd(key, score, member));
	}
	
	@Override
	public Long zadd(String key, double score, Object member) {
		return operate((jedis) -> jedis.zadd(ByteUtils.toBytes(key), score, JacksonUtils.toBytes(member)));
	}
	
	@Override
	public Long zcard(String key) {
		return operate((jedis) -> jedis.zcard(key));
	}
	
	@Override
	public Long zremByRank(String key, long start, long end) {
		return operate((jedis) -> jedis.zremrangeByRank(key, start, end));
	}
	
	@Override
	public Long zremRangeByScore(String key, String min, String max) {
		return operate(jedis -> jedis.zremrangeByScore(key, min, max));
	}
	
	@Override
	public Set<String> zrange(String key, long start, long end) {
		return operate((jedis) -> jedis.zrange(key, start, end));
	}
	
	@Override
	public Set<String> zrangeByScore(String key, String min, String max) {
		return operate(jedis -> jedis.zrangeByScore(key, min, max));
	}
	
	@Override
	public byte[] lpop(byte[] key) {
		return operate((jedis) -> jedis.lpop(key));
	}
	
	@Override
	public String lpop(String key) {
		return operate((jedis) -> jedis.lpop(key));
	}
	
	@Override
	public Long lpush(String key, String... strings) {
		return operate((jedis) -> jedis.lpush(key, strings));
	}
	
	@Override
	public Long lpush(byte[] key, byte[]... strings) {
		return operate((jedis) -> jedis.lpush(key, strings));
	}
	
	@Override
	public Long rpush(String key, String... strings) {
		return operate((jedis) -> jedis.rpush(key, strings));
	}
	
	@Override
	public Long rpush(byte[] key, byte[]... strings) {
		return operate((jedis) -> jedis.rpush(key, strings));
	}
	
	@Override
	public List<String> blpop(int timeout, String key) {
		return operate((jedis) -> jedis.blpop(timeout, key));
	}
	
	@Override
	public List<byte[]> blpop(int timeout, byte[]... keys) {
		return operate((jedis) -> jedis.blpop(timeout, keys));
	}
	
	@Override
	public List<String> brpop(int timeout, String key) {
		return operate((jedis) -> jedis.brpop(timeout, key));
	}
	
	@Override
	public List<String> brpop(int timeout, String... keys) {
		return operate((jedis) -> jedis.brpop(timeout, keys));
	}
	
	@Override
	public List<byte[]> brpop(int timeout, byte[]... keys) {
		return operate((jedis) -> jedis.brpop(timeout, keys));
	}
	
	@Override
	public String rpop(String key) {
		return operate(jedis -> jedis.rpop(key));
	}
	
	@Override
	public Long llen(String key) {
		return operate((jedis) -> jedis.llen(key));
	}
	
	@Override
	public List<String> lrange(String key, long start, long stop) {
		return operate((jedis) -> jedis.lrange(key, start, stop));
	}
	
	@Override
	public List<byte[]> lrange(byte[] key, long start, long stop) {
		return operate((jedis) -> jedis.lrange(key, start, stop));
	}
	
	@Override
	public Long lrem(String key, long count, String value) {
		return operate((jedis) -> jedis.lrem(key, count, value));
	}
	
	@Override
	public Long sadd(String key, String... members) {
		return operate((jedis) -> jedis.sadd(key, members));
	}
	
	@Override
	public Long sadd(byte[] key, byte[]... members) {
		return operate((jedis) -> jedis.sadd(key, members));
	}
	
	@Override
	public Long srem(byte[] key, byte[]... members) {
		return operate((jedis) -> jedis.srem(key, members));
	}
	
	@Override
	public Long scard(String key) {
		return operate((jedis) -> jedis.scard(key));
	}
	
	@Override
	public Boolean sismember(byte[] key, byte[] member) {
		return operate((jedis) -> jedis.sismember(key, member));
	}
	
	@Override
	public Set<byte[]> smembers(byte[] key) {
		return operate((jedis) -> jedis.smembers(key));
	}
	
	@Override
	public Set<String> smembers(String key) {
		return operate((jedis) -> jedis.smembers(key));
	}
	
	@Override
	public Boolean hexists(byte[] key, byte[] field) {
		return operate((jedis) -> jedis.hexists(key, field));
	}
	
	@Override
	public Boolean hexists(String key, String field) {
		return operate((jedis) -> jedis.hexists(key, field));
	}
	
	@Override
	public byte[] hget(byte[] key, byte[] field) {
		return operate((jedis) -> jedis.hget(key, field));
	}
	
	@Override
	public Long hset(byte[] key, byte[] field, byte[] value) {
		return operate((jedis) -> jedis.hset(key, field, value));
	}
	
	@Override
	public String hmset(String key, Map<String, String> hash) {
		return operate((jedis) -> jedis.hmset(key, hash));
	}
	
	@Override
	public List<byte[]> hmget(byte[] key, byte[]... fields) {
		return operate((jedis) -> jedis.hmget(key, fields));
	}
	
	@Override
	public List<String> hmget(String key, String... fields) {
		return operate((jedis) -> jedis.hmget(key, fields));
	}
	
	@Override
	public Map<byte[], byte[]> hgetAll(byte[] key) {
		return operate((jedis) -> jedis.hgetAll(key));
	}
	
	@Override
	public Map<String, String> hgetAll(String key) {
		return operate((jedis) -> jedis.hgetAll(key));
	}
	
	@Override
	public List<String> hvals(String key) {
		return operate((jedis) -> jedis.hvals(key));
	}
	
	@Override
	public Long expire(byte[] key, int seconds) {
		return operate((jedis) -> jedis.expire(key, seconds));
	}
	
	@Override
	public Long hlen(byte[] key) {
		return operate((jedis) -> jedis.hlen(key));
	}
	
	@Override
	public Long expire(String key, int seconds) {
		return operate((jedis) -> jedis.expire(key, seconds));
	}
	
	@Override
	public Long expireAt(String key, long unixTime) {
		return operate((jedis) -> jedis.expireAt(key, unixTime));
	}
	
	@Override
	public Long expireAt(byte[] key, long unixTime) {
		return operate((jedis) -> jedis.expireAt(key, unixTime));
	}
	
	@Override
	public Long persist(String key) {
		return operate((jedis) -> jedis.persist(key));
	}
	
	@Override
	public Long persist(byte[] key) {
		return operate((jedis) -> jedis.persist(key));
	}
	
	@Override
	public Long ttl(String key) {
		return operate((jedis) -> jedis.ttl(key));
	}
	
	@Override
	public Long ttl(byte[] key) {
		return operate((jedis) -> jedis.ttl(key));
	}
	
	@Override
	public Long del(String key) {
		return operate((jedis) -> jedis.del(key));
	}
	
	@Override
	public Long del(byte[] key) {
		return operate((jedis) -> jedis.del(key));
	}
	
	@Override
	public Object eval(String script) {
		return operate((jedis) -> jedis.eval(script));
	}
	
	@Override
	public Object eval(String script, int keyCount, String... params) {
		return operate((jedis) -> jedis.eval(script, keyCount, params));
	}
	
	@Override
	public String scriptLoad(String script) {
		return operate((jedis) -> jedis.scriptLoad(script));
	}
	
	@Override
	public Object evalsha(String sha1) {
		return operate((jedis) -> jedis.evalsha(sha1));
	}
	
	@Override
	public Object evalsha(String sha1, int keyCount, String... params) {
		return operate((jedis) -> jedis.evalsha(sha1, keyCount, params));
	}
	
	@Override
	public Object evalsha(byte[] sha1, int keyCount, byte[]... params) {
		return operate((jedis) -> jedis.evalsha(sha1, keyCount, params));
	}
	
	@Override
	public Long publish(byte[] channel, byte[] message) {
		return operate((jedis) -> {
			return jedis.publish(channel, message);
		});
	}
	
	/**
	 * 订阅消息用的Jedis实例不要用完就关掉, 否则接收消息的时候会抛异常
	 * redis.clients.jedis.exceptions.JedisConnectionException: Unexpected end of stream.
	 *
	 * @param jedisPubSub
	 * @param channels
	 */
	@Override
	public void subscribe(JedisPubSub jedisPubSub, String... channels) {
		THREAD_POOL.execute(() -> pool.getResource().subscribe(jedisPubSub, channels));
	}
	
	/**
	 * 订阅消息用的Jedis实例不要用完就关掉, 否则接收消息的时候会抛异常
	 * redis.clients.jedis.exceptions.JedisConnectionException: Unexpected end of stream.
	 *
	 * @param jedisPubSub
	 * @param patterns
	 */
	@Override
	public void psubscribe(JedisPubSub jedisPubSub, String... patterns) {
		THREAD_POOL.execute(() -> pool.getResource().psubscribe(jedisPubSub, patterns));
	}
	
	@Override
	public String ping() {
		return operate((jedis) -> jedis.ping());
	}
	
	@Override
	public Jedis jedis() {
		return pool.getResource();
	}
	
	@Override
	public List<Object> executePipelined(Consumer<Pipeline> consumer) {
		Jedis jedis = pool.getResource();
		try {
			Pipeline pipelined = jedis.pipelined();
			consumer.accept(pipelined);
			return pipelined.syncAndReturnAll();
		} catch (Throwable e) {
			log.error("", e);
			throw new JedisException(e);
		} finally {
			jedis.close();
		}
	}
	
	private <R> R operate(Function<Jedis, R> func) {
		Jedis jedis = pool.getResource();
		try {
			return func.apply(jedis);
		} catch (Throwable e) {
			log.error("", e);
			throw new JedisException(e);
		} finally {
			jedis.close();
		}
	}
	
}
