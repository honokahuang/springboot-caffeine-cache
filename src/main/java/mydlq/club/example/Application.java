package mydlq.club.example;

import com.alibaba.fastjson.JSON;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.XMemcachedClient;
import net.rubyeye.xmemcached.exception.MemcachedException;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@EnableCaching
@SpringBootApplication
@BenchmarkMode(Mode.AverageTime) // 测试方法平均执行时间
@OutputTimeUnit(TimeUnit.MICROSECONDS) // 输出结果的时间粒度为微秒
@State(Scope.Thread)
public class Application {


    public static void main(String[] args) throws Exception{
        SpringApplication.run(Application.class, args);
        Options opt = new OptionsBuilder().include(Application.class.getSimpleName()).forks(1).warmupIterations(0).measurementIterations(1).build();
        new Runner(opt).run();
//        DB db = DBMaker.fileDB("/Users/honoka/123").make();
//        ConcurrentMap map = db.hashMap("map").createOrOpen();

    }
    private static Cache<String,String> cache = Caffeine.newBuilder()
            .maximumSize(1000000)
            .expireAfterWrite(100,TimeUnit.SECONDS)
            .removalListener((key, value, cause) -> log.info(">>>  Delete cache  [{}]({}), reason is [{}]", key, value, cause))
            //  Turn on status monitoring
            .recordStats()
            .build();

    private static int num=0;
    private static int total=0;

    private void FogcachedTest() throws IOException {

        MemcachedClient client=new XMemcachedClient("localhost",11211);

        try {
            client.set("key",600,"test");
            //Object someObject=client.get("key");
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (MemcachedException e) {
            e.printStackTrace();
        }
        client.shutdown();
    }
    private void  RedisTest(){
        Jedis jedis = new Jedis("localhost");
        jedis.set(String.valueOf("test"), "test");
        jedis.close();
    }
    @org.openjdk.jmh.annotations.Benchmark
    public void test() throws IOException, InterruptedException {
//        Random rand = new Random();
//        int b =rand.nextInt(2000000)+ 1;
//        cache.put(String.valueOf(rand.nextInt(2000000)+ 1),"test");
//        cache.getIfPresent(String.valueOf(b));
//        log.info(String.valueOf(cache.stats().hitRate()));
        DB db = DBMaker.fileDB("file.db").checksumHeaderBypass().make();
        HTreeMap myMap = db.hashMap("myMap1").createOrOpen();

        String MessageFromDB = String.valueOf(myMap.get("test"));
        myMap.keySet();
//        log.info(String.valueOf(userInfo));
        db.commit();
        db.close();

    }




}