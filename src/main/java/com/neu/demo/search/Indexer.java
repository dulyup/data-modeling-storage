package com.neu.demo.search;

import lombok.val;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.io.IOException;

/**
 * @author lyupingdu
 * @date 2019-07-25.
 */
@Service
public class Indexer {

    private final String indexName;
    private final String type;
    private RestHighLevelClient esClient;
    private Jedis jedis;

    public Indexer() {
        this.jedis = new Jedis();
        this.esClient = new RestHighLevelClient(RestClient.builder(HttpHost.create("http://localhost:9200")));
        this.indexName = "medical_plan";
        this.type = "_doc";
    }

    private void process() throws IOException {
        while (true) {
            System.out.println("Retrieving from the queue");
            val work = jedis.brpoplpush("waiting_queue", "working_queue", 0);
            if (work == null) {
                System.out.println("work is null");
                continue;
            }
            val parts = work.split(IndexerService.DELIMITER);
            String workType = parts[0];
            String id = parts[1];
            String content = parts[2];
            System.out.println("id: " + id);
            System.out.println("workType: " + workType);
            if (workType.equals(IndexerService.NEW)) {
                addJsonDocument(id, content);
            } else {
                update(id, content);
            }
        }
    }

    public void addJsonDocument(String id, String json) throws IOException {
        if (!indexExists()) {
            createIndex();
        }
        val indexRequest = new IndexRequest(indexName, type, id).source(json, XContentType.JSON);
        val bulkRequest = new BulkRequest();
        bulkRequest.add(indexRequest);
        BulkResponse bulkResponse;
        bulkResponse = esClient.bulk(bulkRequest, RequestOptions.DEFAULT);
        if (bulkResponse.hasFailures()) {
            throw new RuntimeException(bulkResponse.buildFailureMessage());
        }
    }

    public void update(String id, String json) throws IOException {
        UpdateRequest request = new UpdateRequest(indexName, type, id);
        request.doc(json, XContentType.JSON);
        esClient.update(request, RequestOptions.DEFAULT);
    }

    private boolean indexExists() throws IOException {
        val response = esClient.getLowLevelClient().performRequest("HEAD", "/" + indexName);
        int statusCode = response.getStatusLine().getStatusCode();
        return statusCode != 404;
    }

    public void deleteIndex() throws IOException {
        esClient.indices().delete(new DeleteIndexRequest(indexName), RequestOptions.DEFAULT);
    }

    private void createIndex() throws IOException {
        val request = new CreateIndexRequest(indexName);
        esClient.indices().create(request, RequestOptions.DEFAULT);
    }

    public static void main(String[] args) throws IOException {
        Indexer indexer = new Indexer();
        indexer.process();
    }
}