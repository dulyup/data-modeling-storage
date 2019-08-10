package com.neu.demo.search;

import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

/**
 * @author lyupingdu
 * @date 2019-07-29.
 */
@Service
public class IndexerService {

    private Jedis jedis;
    public final static String NEW = "NEW";
    public final static String UPDATE = "UPDATE";
    public final static String DELIMITER = "___";

    public IndexerService() {
        this.jedis = new Jedis();
    }

    public void saveToQueue(String workType, String id, String content) {
        String queueContent = workType +
                DELIMITER +
                id +
                DELIMITER +
                content;
        jedis.rpush("waiting_queue", queueContent);
    }
}
