package com.neu.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.neu.demo.search.IndexerService;
import com.neu.demo.util.JsonMapUtil;
import org.springframework.stereotype.Service;
import lombok.val;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.*;

/**
 * This class will deal with data modeling and interact with redis
 *
 * @author lyupingdu
 * @date 2019-07-24.
 */
@Service
public class DataModelingService {

    private static JsonNode schemaNode = JsonMapUtil.schemaNode;

    private Jedis jedis;
    private IndexerService indexerService;

    public DataModelingService(Jedis jedis, IndexerService indexerService) {
        this.jedis = jedis;
        this.indexerService = indexerService;
    }

    public Map<String, Object> getById(String id) {
        Map<String, String> planMap = jedis.hgetAll(id);
        Map<String, Object> res = new HashMap<>();
        for (Map.Entry<String, String> entry : planMap.entrySet()) {
            val value = schemaNode.findValue(entry.getKey());
            if (value == null) {
                // new property
                res.put(entry.getKey(), entry.getValue());
            } else if (value.get("$ref") != null) {
                reTypeObject(entry, res);
            } else {
                val typeText = value.get("type").textValue();
                if ("array".equals(typeText)) {
                    val list = jedis.smembers(entry.getValue());
                    val linkedPlanServices = new ArrayList<>();
                    for (val planserviceKey : list) {
                        val planservice = jedis.hgetAll(planserviceKey);
                        Map<String, Object> planserviceRes = new HashMap<>();
                        for (Map.Entry<String, String> planserviceEntry : planservice.entrySet()) {
                            val keyInSchema = schemaNode.findValue(planserviceEntry.getKey());
                            if (keyInSchema.get("$ref") != null) {
                                reTypeObject(planserviceEntry, planserviceRes);
                            } else if ("number".equals(keyInSchema.get("type").textValue())) {
                                planserviceRes.put(planserviceEntry.getKey(), Integer.valueOf(planserviceEntry.getValue()));
                            } else {
                                planserviceRes.put(planserviceEntry.getKey(), planserviceEntry.getValue());
                            }
                        }
                        linkedPlanServices.add(planserviceRes);
                    }
                    res.put(entry.getKey(), linkedPlanServices);
                } else if ("number".equals(typeText)) {
                    res.put(entry.getKey(), Integer.valueOf(entry.getValue()));
                } else {
                    res.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return res;
    }

    public String addNew(String content) throws IOException {
        Map<String, Object> bodyContent = JsonMapUtil.jsonToMap(content);
        String key = composeKey(bodyContent);
        Map<String, String> planMap = new HashMap<>(bodyContent.size());
        if (key == null) {
            return null;
        }
        decomposeDetails(bodyContent, planMap, key);
        // record the plan id for getAll
        jedis.sadd("plan_id", key);
        indexerService.saveToQueue(IndexerService.NEW, key, content);
        return key;
    }

    public void partialUpdateById(String id, String content) throws IOException {
        Map<String, Object> updatingContent = JsonMapUtil.jsonToMap(content);
        Map<String, String> planMap = jedis.hgetAll(id);
        for (Map.Entry<String, Object> updatingEntry : updatingContent.entrySet()) {
            val updatingProperty = updatingEntry.getValue();
            if (schemaNode.findValue(updatingEntry.getKey()) == null) {
                // TODO: new property, can't update
                continue;
            } else if (updatingProperty instanceof Map) {
                val updatingPropertyMap = (Map<String, Object>) updatingProperty;
                val propertyComposedKey = composeInvKey(updatingEntry.getKey(), updatingPropertyMap);
                val searchRes = jedis.hgetAll(propertyComposedKey);
                for (Map.Entry<String, Object> et : updatingPropertyMap.entrySet()) {
                    searchRes.put(et.getKey(), String.valueOf(et.getValue()));
                }
                jedis.hmset(propertyComposedKey, searchRes);
            } else if (updatingEntry.getValue() instanceof List) {
                val updatingPlanServiceList = (List<Map<String, Object>>) updatingEntry.getValue();
                val updatingPropertyListComposedKey = updatingPlanServiceList.get(0).get("objectType") + "__inv__" + id;
                val linkedPlanServiceIdList = jedis.smembers(updatingPropertyListComposedKey);
                for (val updatingPlanService : updatingPlanServiceList) {
                    val updatingPlanServiceKey = composeInvKey(updatingEntry.getKey(), updatingPlanService);
                    if (!linkedPlanServiceIdList.contains(updatingPlanServiceKey)) {
                        // new
                        jedis.sadd(updatingPropertyListComposedKey, updatingPlanServiceKey);
                        val updatingPlanServiceMap = new HashMap<String, String>();
                        decomposeNestedObject(updatingPlanService, updatingPlanServiceMap, updatingPlanServiceKey, false);
                    } else {
                        val originalPlanService = jedis.hgetAll(updatingPlanServiceKey);
                        decomposeNestedObject(updatingPlanService, originalPlanService, updatingPlanServiceKey, true);
                    }
                }
            } else {
                planMap.put(updatingEntry.getKey(), String.valueOf(updatingEntry.getValue()));
            }
        }
        jedis.hmset(id, planMap);
        indexerService.saveToQueue(IndexerService.UPDATE, id, content);
    }

    public void fullUpdateById(String id, String content) throws IOException {
        Map<String, Object> bodyContent = JsonMapUtil.jsonToMap(content);
        Map<String, String> planMap = jedis.hgetAll(id);
        removeProperty(bodyContent, planMap, id);
        decomposeDetails(bodyContent, planMap, id);
        indexerService.saveToQueue(IndexerService.UPDATE, id, content);
    }

    private void removeProperty(Map<String, Object> bodyContent, Map<String, String> planMap, String id) {
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry entry : planMap.entrySet()) {
            if (!bodyContent.containsKey(entry.getKey())) {
                toRemove.add((String)entry.getKey());
            }
        }
        for (String key : toRemove) {
            planMap.remove(key);
            jedis.hdel(id, key);
        }
    }

    public void deleteById(String id) {
        val planMap = jedis.hgetAll(id);
        for (Map.Entry<String, String> entry : planMap.entrySet()) {
            val value = schemaNode.findValue(entry.getKey());
            if (value == null) {
                continue;
            }
            if (value.get("$ref") != null) {
                jedis.del(entry.getValue());
            } else {
                val typeText = value.get("type").textValue();
                if ("array".equals(typeText)) {
                    val list = jedis.smembers(entry.getValue());
                    Iterator iterator = list.iterator();
                    while (iterator.hasNext()) {
                        val planserviceKey = (String)iterator.next();
                        val planservice = jedis.hgetAll(planserviceKey);
                        for (Map.Entry<String, String> planserviceEntry : planservice.entrySet()) {
                            val keyInSchema = schemaNode.findValue(planserviceEntry.getKey());
                            if (keyInSchema.get("$ref") != null) {
                                jedis.del(planserviceEntry.getValue());
                            }
                        }
                        jedis.del(planserviceKey);
                        iterator.remove();
                    }
                    jedis.del(entry.getValue());
                }
            }
        }
        jedis.del(id);
        jedis.srem("plan_id", id);
    }

    public Map<String, Object> getAll() {
        val planIdList = jedis.smembers("plan_id");
        Map<String, Object> res = new HashMap<>();
        for (val id : planIdList) {
            val planMap = getById(id);
            res.put(id, planMap);
        }
        return res;
    }

    private void decomposeDetails(Map<String, Object> bodyContent, Map<String, String> planMap, String key) {
        for (val entry : bodyContent.entrySet()) {
            val property = entry.getValue();
            if (schemaNode.findValue(entry.getKey()) == null) {
                // new property, no need to decompose
                planMap.put(entry.getKey(), String.valueOf(entry.getValue()));
            } else if (property instanceof Map) {
                decomposeObject(planMap, entry, false);
            } else if (property instanceof List) {
                val propertyList = (List<Map<String, Object>>) entry.getValue();
                val propertyListComposedKey = propertyList.get(0).get("objectType") + "__inv__" + key;
                // if put request has removed some list properties
                jedis.del(propertyListComposedKey);
                planMap.put(entry.getKey(), propertyListComposedKey);
                for (val planService : propertyList) {
                    val planServiceKey = composeInvKey(entry.getKey(), planService);
                    jedis.sadd(propertyListComposedKey, planServiceKey);
                    Map<String, String> planServiceMap = new HashMap<>();
                    decomposeNestedObject(planService, planServiceMap, planServiceKey, false);
                }
            } else {
                planMap.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        jedis.hmset(key, planMap);
    }

    private void decomposeNestedObject(Map<String, Object> updatingPlanService, Map<String, String> updatingPlanServiceMap, String updatingPlanServiceKey, boolean existing) {
        for (Map.Entry updatingPlanServiceEntry : updatingPlanService.entrySet()) {
            if (updatingPlanServiceEntry.getValue() instanceof Map) {
                decomposeObject(updatingPlanServiceMap, updatingPlanServiceEntry, existing);
            } else {
                updatingPlanServiceMap.put((String) updatingPlanServiceEntry.getKey(), String.valueOf(updatingPlanServiceEntry.getValue()));
            }
            jedis.hmset(updatingPlanServiceKey, updatingPlanServiceMap);
        }
    }

    private void decomposeObject(Map<String, String> updatingPlanServiceMap, Map.Entry updatingPlanServiceEntry, boolean existing) {
        val updatingService = (Map<String, Object>) updatingPlanServiceEntry.getValue();
        val updatingServiceKey = composeInvKey((String) updatingPlanServiceEntry.getKey(), updatingService);
        Map<String, String> toSave;
        if (existing) {
            toSave = jedis.hgetAll(updatingServiceKey);
        } else {
            toSave = new HashMap<>();
        }
        for (Map.Entry<String, Object> updatingServiceEntry : updatingService.entrySet()) {
            toSave.put(updatingServiceEntry.getKey(), String.valueOf(updatingServiceEntry.getValue()));
        }
        jedis.hmset(updatingServiceKey, toSave);
        updatingPlanServiceMap.put((String) updatingPlanServiceEntry.getKey(), updatingServiceKey);
    }

    private void reTypeObject(Map.Entry<String, String> entry, Map<String, Object> res) {
        val planCostShares = jedis.hgetAll(entry.getValue());
        Map<String, Object> planCostSharesRes = new HashMap<>();
        for (Map.Entry<String, String> et : planCostShares.entrySet()) {
            if ("number".equals(schemaNode.findValue(et.getKey()).get("type").textValue())) {
                planCostSharesRes.put(et.getKey(), Integer.valueOf(et.getValue()));
            } else {
                planCostSharesRes.put(et.getKey(), et.getValue());
            }
        }
        res.put(entry.getKey(), planCostSharesRes);
    }

    private String composeKey(Map content) {
        val id = content.get("objectId");
        val type = content.get("objectType");
        if (id != null && type != null) {
            val idString = (String) id;
            val typeString = (String) type;
            if (!idString.isEmpty() && !typeString.isEmpty()) {
                return typeString + "__" + idString;
            }
        }
        return null;
    }

    private String composeInvKey(String parent, Map content) {
        return composeKey(content) + "__inv__" + parent;
    }


}


