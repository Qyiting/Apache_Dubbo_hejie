package com.hejiexmu.rpc.auth.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Redis缓存服务
 * 提供通用的缓存操作方法
 * 
 * @author hejiexmu
 */
@Service
public class RedisCacheService {
    
    private static final Logger logger = LoggerFactory.getLogger(RedisCacheService.class);
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    
    // Lua脚本：分布式锁
    private final DefaultRedisScript<Boolean> lockScript;
    private final DefaultRedisScript<Boolean> unlockScript;
    
    public RedisCacheService(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        
        // 初始化Lua脚本
        this.lockScript = createLockScript();
        this.unlockScript = createUnlockScript();
    }
    
    /**
     * 设置缓存
     */
    public boolean set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            logger.error("设置缓存失败: key={}", key, e);
            return false;
        }
    }
    
    /**
     * 设置缓存（带过期时间）
     */
    public boolean set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
            return true;
        } catch (Exception e) {
            logger.error("设置缓存失败: key={}, timeout={}, unit={}", key, timeout, unit, e);
            return false;
        }
    }
    
    /**
     * 获取缓存
     */
    public Object get(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            logger.error("获取缓存失败: key={}", key, e);
            return null;
        }
    }
    
    /**
     * 获取缓存（指定类型）
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return null;
            }
            
            if (clazz.isInstance(value)) {
                return (T) value;
            }
            
            // 尝试JSON反序列化
            if (value instanceof String) {
                return objectMapper.readValue((String) value, clazz);
            }
            
            return null;
        } catch (Exception e) {
            logger.error("获取缓存失败: key={}, class={}", key, clazz.getName(), e);
            return null;
        }
    }
    
    /**
     * 删除缓存
     */
    public boolean delete(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.delete(key));
        } catch (Exception e) {
            logger.error("删除缓存失败: key={}", key, e);
            return false;
        }
    }
    
    /**
     * 批量删除缓存
     */
    public long delete(Collection<String> keys) {
        try {
            Long count = redisTemplate.delete(keys);
            return count != null ? count : 0;
        } catch (Exception e) {
            logger.error("批量删除缓存失败: keys={}", keys, e);
            return 0;
        }
    }
    
    /**
     * 检查缓存是否存在
     */
    public boolean exists(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            logger.error("检查缓存存在性失败: key={}", key, e);
            return false;
        }
    }
    
    /**
     * 设置过期时间
     */
    public boolean expire(String key, long timeout, TimeUnit unit) {
        try {
            return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, unit));
        } catch (Exception e) {
            logger.error("设置过期时间失败: key={}, timeout={}, unit={}", key, timeout, unit, e);
            return false;
        }
    }
    
    /**
     * 获取过期时间
     */
    public long getExpire(String key, TimeUnit unit) {
        try {
            Long expire = redisTemplate.getExpire(key, unit);
            return expire != null ? expire : -1;
        } catch (Exception e) {
            logger.error("获取过期时间失败: key={}, unit={}", key, unit, e);
            return -1;
        }
    }
    
    /**
     * 递增
     */
    public long increment(String key) {
        try {
            Long result = redisTemplate.opsForValue().increment(key);
            return result != null ? result : 0;
        } catch (Exception e) {
            logger.error("递增失败: key={}", key, e);
            return 0;
        }
    }
    
    /**
     * 递增（指定步长）
     */
    public long increment(String key, long delta) {
        try {
            Long result = redisTemplate.opsForValue().increment(key, delta);
            return result != null ? result : 0;
        } catch (Exception e) {
            logger.error("递增失败: key={}, delta={}", key, delta, e);
            return 0;
        }
    }
    
    /**
     * 递减
     */
    public long decrement(String key) {
        try {
            Long result = redisTemplate.opsForValue().decrement(key);
            return result != null ? result : 0;
        } catch (Exception e) {
            logger.error("递减失败: key={}", key, e);
            return 0;
        }
    }
    
    /**
     * 递减（指定步长）
     */
    public long decrement(String key, long delta) {
        try {
            Long result = redisTemplate.opsForValue().decrement(key, delta);
            return result != null ? result : 0;
        } catch (Exception e) {
            logger.error("递减失败: key={}, delta={}", key, delta, e);
            return 0;
        }
    }
    
    /**
     * Hash设置
     */
    public boolean hSet(String key, String field, Object value) {
        try {
            redisTemplate.opsForHash().put(key, field, value);
            return true;
        } catch (Exception e) {
            logger.error("Hash设置失败: key={}, field={}", key, field, e);
            return false;
        }
    }
    
    /**
     * Hash获取
     */
    public Object hGet(String key, String field) {
        try {
            return redisTemplate.opsForHash().get(key, field);
        } catch (Exception e) {
            logger.error("Hash获取失败: key={}, field={}", key, field, e);
            return null;
        }
    }
    
    /**
     * Hash删除
     */
    public boolean hDelete(String key, String... fields) {
        try {
            Long count = redisTemplate.opsForHash().delete(key, (Object[]) fields);
            return count != null && count > 0;
        } catch (Exception e) {
            logger.error("Hash删除失败: key={}, fields={}", key, Arrays.toString(fields), e);
            return false;
        }
    }
    
    /**
     * Hash获取所有字段
     */
    public Map<Object, Object> hGetAll(String key) {
        try {
            return redisTemplate.opsForHash().entries(key);
        } catch (Exception e) {
            logger.error("Hash获取所有字段失败: key={}", key, e);
            return new HashMap<>();
        }
    }
    
    /**
     * Set添加
     */
    public boolean sAdd(String key, Object... values) {
        try {
            Long count = redisTemplate.opsForSet().add(key, values);
            return count != null && count > 0;
        } catch (Exception e) {
            logger.error("Set添加失败: key={}, values={}", key, Arrays.toString(values), e);
            return false;
        }
    }
    
    /**
     * Set移除
     */
    public boolean sRemove(String key, Object... values) {
        try {
            Long count = redisTemplate.opsForSet().remove(key, values);
            return count != null && count > 0;
        } catch (Exception e) {
            logger.error("Set移除失败: key={}, values={}", key, Arrays.toString(values), e);
            return false;
        }
    }
    
    /**
     * Set获取所有成员
     */
    public Set<Object> sMembers(String key) {
        try {
            Set<Object> members = redisTemplate.opsForSet().members(key);
            return members != null ? members : new HashSet<>();
        } catch (Exception e) {
            logger.error("Set获取所有成员失败: key={}", key, e);
            return new HashSet<>();
        }
    }
    
    /**
     * Set检查成员是否存在
     */
    public boolean sIsMember(String key, Object value) {
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, value));
        } catch (Exception e) {
            logger.error("Set检查成员失败: key={}, value={}", key, value, e);
            return false;
        }
    }
    
    /**
     * Set获取大小
     */
    public long sSize(String key) {
        try {
            Long size = redisTemplate.opsForSet().size(key);
            return size != null ? size : 0;
        } catch (Exception e) {
            logger.error("Set获取大小失败: key={}", key, e);
            return 0;
        }
    }
    
    /**
     * List左推
     */
    public boolean lPush(String key, Object... values) {
        try {
            Long count = redisTemplate.opsForList().leftPushAll(key, values);
            return count != null && count > 0;
        } catch (Exception e) {
            logger.error("List左推失败: key={}, values={}", key, Arrays.toString(values), e);
            return false;
        }
    }
    
    /**
     * List右推
     */
    public boolean rPush(String key, Object... values) {
        try {
            Long count = redisTemplate.opsForList().rightPushAll(key, values);
            return count != null && count > 0;
        } catch (Exception e) {
            logger.error("List右推失败: key={}, values={}", key, Arrays.toString(values), e);
            return false;
        }
    }
    
    /**
     * List左弹
     */
    public Object lPop(String key) {
        try {
            return redisTemplate.opsForList().leftPop(key);
        } catch (Exception e) {
            logger.error("List左弹失败: key={}", key, e);
            return null;
        }
    }
    
    /**
     * List右弹
     */
    public Object rPop(String key) {
        try {
            return redisTemplate.opsForList().rightPop(key);
        } catch (Exception e) {
            logger.error("List右弹失败: key={}", key, e);
            return null;
        }
    }
    
    /**
     * List获取范围
     */
    public List<Object> lRange(String key, long start, long end) {
        try {
            List<Object> list = redisTemplate.opsForList().range(key, start, end);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            logger.error("List获取范围失败: key={}, start={}, end={}", key, start, end, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * List获取大小
     */
    public long lSize(String key) {
        try {
            Long size = redisTemplate.opsForList().size(key);
            return size != null ? size : 0;
        } catch (Exception e) {
            logger.error("List获取大小失败: key={}", key, e);
            return 0;
        }
    }
    
    /**
     * 获取匹配的键
     */
    public Set<String> keys(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            return keys != null ? keys : new HashSet<>();
        } catch (Exception e) {
            logger.error("获取匹配键失败: pattern={}", pattern, e);
            return new HashSet<>();
        }
    }
    
    /**
     * 分布式锁 - 加锁
     */
    public boolean lock(String lockKey, String lockValue, long expireTime, TimeUnit unit) {
        try {
            List<String> keys = Collections.singletonList(lockKey);
            List<Object> args = Arrays.asList(lockValue, unit.toSeconds(expireTime));
            
            Boolean result = redisTemplate.execute(lockScript, keys, args.toArray());
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            logger.error("分布式锁加锁失败: lockKey={}, lockValue={}", lockKey, lockValue, e);
            return false;
        }
    }
    
    /**
     * 分布式锁 - 解锁
     */
    public boolean unlock(String lockKey, String lockValue) {
        try {
            List<String> keys = Collections.singletonList(lockKey);
            List<Object> args = Collections.singletonList(lockValue);
            
            Boolean result = redisTemplate.execute(unlockScript, keys, args.toArray());
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            logger.error("分布式锁解锁失败: lockKey={}, lockValue={}", lockKey, lockValue, e);
            return false;
        }
    }
    
    /**
     * 批量操作 - 管道
     */
    public List<Object> executePipelined(List<Runnable> operations) {
        try {
            return redisTemplate.executePipelined(new SessionCallback<Object>() {
                @Override
                public Object execute(org.springframework.data.redis.core.RedisOperations redisOperations) {
                    operations.forEach(Runnable::run);
                    return null;
                }
            });
        } catch (Exception e) {
            logger.error("批量操作失败", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 创建分布式锁Lua脚本
     */
    private DefaultRedisScript<Boolean> createLockScript() {
        DefaultRedisScript<Boolean> script = new DefaultRedisScript<>();
        script.setScriptText(
            "if redis.call('get', KEYS[1]) == ARGV[1] then\n" +
            "    return redis.call('expire', KEYS[1], ARGV[2])\n" +
            "else\n" +
            "    return redis.call('set', KEYS[1], ARGV[1], 'EX', ARGV[2], 'NX')\n" +
            "end"
        );
        script.setResultType(Boolean.class);
        return script;
    }
    
    /**
     * 创建分布式解锁Lua脚本
     */
    private DefaultRedisScript<Boolean> createUnlockScript() {
        DefaultRedisScript<Boolean> script = new DefaultRedisScript<>();
        script.setScriptText(
            "if redis.call('get', KEYS[1]) == ARGV[1] then\n" +
            "    return redis.call('del', KEYS[1])\n" +
            "else\n" +
            "    return 0\n" +
            "end"
        );
        script.setResultType(Boolean.class);
        return script;
    }
}