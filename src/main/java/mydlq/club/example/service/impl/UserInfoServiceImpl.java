package mydlq.club.example.service.impl;

import com.alibaba.fastjson.JSON;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import mydlq.club.example.entity.UserInfo;
import mydlq.club.example.service.UserInfoService;
import net.rubyeye.xmemcached.XMemcachedClient;
import net.rubyeye.xmemcached.exception.MemcachedException;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;

import redis.clients.jedis.Jedis;
import net.spy.memcached.MemcachedClient;
@Slf4j
@Service
public class UserInfoServiceImpl implements UserInfoService {

    /**
     * 模拟数据库存储数据
     */
    private HashMap<Integer, UserInfo> userInfoMap = new HashMap<>();

    private UserInfo FogcachedGet(String key) throws IOException {
        MemcachedClient mcc = new MemcachedClient(new InetSocketAddress("127.0.0.1", 11211));
        String MessageFromDB = String.valueOf(mcc.get(key));
        UserInfo userInfo1 =JSON.parseObject(MessageFromDB,UserInfo.class);
        mcc.shutdown();
        log.info("success");
        return userInfo1;
    }

    private void FogcachedSet(String key, UserInfo userInfo) throws IOException {
        try {
            log.info("Add User Info in FogCached");
            MemcachedClient mcc = new MemcachedClient(new InetSocketAddress("127.0.0.1", 11211));
            mcc.set(String.valueOf(userInfo.getId()), 1100,JSON.toJSONString(userInfo));
            mcc.shutdown();
        }catch (Exception ex){
            log.info("fail");
        }
        log.info("success");
    }
    @Autowired
    Cache<String, Object> caffeineCache;

    @Autowired
    Cache<String, Object> mapdb;


    @Override
    public void addUserInfo(UserInfo userInfo) {
        log.info("Add User Info in mapDB");
        DB db = DBMaker.fileDB("file.db").checksumHeaderBypass().make();
        HTreeMap myMap = db.hashMap("myMap").createOrOpen();
        myMap.put(String.valueOf(userInfo.getId()), JSON.toJSONString(userInfo));
        myMap.keySet();
        log.info(String.valueOf(userInfo));
        db.commit();
        db.close();
        // 加入缓存
        log.info("Add User Info in caffeineCache");
        caffeineCache.put(String.valueOf(userInfo.getId()),userInfo);
    }

    @Override
    public void addUserInfoJ(UserInfo userInfo) {
        log.info("Add User Info in Redis");
        //userInfoMap.put(userInfo.getId(), userInfo);
        Jedis jedis = new Jedis("localhost");
        jedis.set(String.valueOf(userInfo.getId()), String.valueOf(userInfo));
        log.info("success");
    }

    @Override
    public void addUserInfoF(UserInfo userInfo) {
        try {
            log.info("Add User Info in FogCached");
            MemcachedClient mcc = new MemcachedClient(new InetSocketAddress("127.0.0.1", 11211));
            mcc.set(String.valueOf(userInfo.getId()), 1100,JSON.toJSONString(userInfo));
            mcc.shutdown();
        }catch (Exception ex){
            log.info("fail");
        }
        log.info("success");
    }

    @Override
    public void addUserInfoC(UserInfo userInfo) {
        log.info("Add User Info in mapDB");
        DB db = DBMaker.fileDB("file.db").checksumHeaderBypass().make();
        HTreeMap myMap = db.hashMap("myMap").createOrOpen();
        log.info(JSON.toJSONString(userInfo));
        myMap.put(String.valueOf(userInfo.getId()), JSON.toJSONString(userInfo));
        myMap.keySet();
        log.info(String.valueOf(userInfo));
        db.commit();
        db.close();
        log.info("success");
    }

    @Override
    public UserInfo getByName(Integer id) {
//        caffeineCache.getIfPresent(id);
//        UserInfo userInfo = (UserInfo) caffeineCache.asMap().get(String.valueOf(id));
        // Fogcached
        UserInfo userInfo = null;
        try {
            userInfo = FogcachedGet(String.valueOf(id));
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (userInfo != null){
            log.info("get from caffeineCache");
            return userInfo;
        }

        DB db = DBMaker.fileDB("file.db").make();
        HTreeMap myMap = db.hashMap("myMap").createOrOpen();
        String MessageFromDB = String.valueOf(myMap.get(String.valueOf(id)));
        db.close();
        if(MessageFromDB != null){
            log.info("get from NVMM");
            log.info(String.valueOf(MessageFromDB));
            UserInfo userInfo1 =JSON.parseObject(MessageFromDB,UserInfo.class);
            // caffeine
//            caffeineCache.put(String.valueOf(id), userInfo1);
            // Fogcached
            try {
                FogcachedSet(String.valueOf(id), userInfo1);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return userInfo1;
        }
//        log.info(String.valueOf(myMap.get(String.valueOf(id))));
        // 加入缓存
        // 如果用户信息不为空，则加入缓存
//        if (userInfo != null){
//            caffeineCache.put(String.valueOf(userInfo.getId()),userInfo);
//        }
//        UserInfo userInfo1 = (UserInfo) caffeineCache.asMap().get(String.valueOf(id));
//        log.info(String.valueOf(userInfo1));
        return null;
    }

    @Override
    public UserInfo updateUserInfo(UserInfo userInfo) {
        log.info("update");
        if (!userInfoMap.containsKey(userInfo.getId())) {
            return null;
        }
        // 取旧的值
        UserInfo oldUserInfo = userInfoMap.get(userInfo.getId());
        // 替换内容
        if (!StringUtils.isEmpty(oldUserInfo.getAge())) {
            oldUserInfo.setAge(userInfo.getAge());
        }
        if (!StringUtils.isEmpty(oldUserInfo.getName())) {
            oldUserInfo.setName(userInfo.getName());
        }
        if (!StringUtils.isEmpty(oldUserInfo.getSex())) {
            oldUserInfo.setSex(userInfo.getSex());
        }
        // 将新的对象存储，更新旧对象信息
        userInfoMap.put(oldUserInfo.getId(), oldUserInfo);
        // 替换缓存中的值
        caffeineCache.put(String.valueOf(oldUserInfo.getId()),oldUserInfo);
        return oldUserInfo;
    }

    @Override
    public void deleteById(Integer id) {
        log.info("delete");
        userInfoMap.remove(id);
        // 从缓存中删除
        caffeineCache.asMap().remove(String.valueOf(id));
    }

}
