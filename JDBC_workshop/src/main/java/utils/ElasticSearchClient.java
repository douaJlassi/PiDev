package utils;

import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import co.elastic.clients.elasticsearch.ElasticsearchClient;

public class ElasticSearchClient {
    private static ElasticsearchClient client = null;


    public static ElasticsearchClient getInstance(){
        if(client == null){
            RestClient restClient = RestClient.builder(new HttpHost("localhost", 9200, "http")).build();
            RestClientTransport transport = new RestClientTransport(
                    restClient, new JacksonJsonpMapper()
            );
            client = new ElasticsearchClient(transport);
            
        }
        return client;
    }
}
