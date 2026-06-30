package baize.code.java.config;

import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VectorStoreConfig {

    private final DashScopeEmbeddingModel dashScopeEmbeddingModel;

    @Value("${spring.ai.vectorstore.milvus.client.host}")
    private String milvusHost;

    @Value("${spring.ai.vectorstore.milvus.client.port}")
    private Integer milvusPort;

    @Bean
    public MilvusServiceClient milvusServiceClient() {
        ConnectParam connectParam = ConnectParam.newBuilder()
                .withHost(milvusHost)
                .withPort(milvusPort)
                .build();
        return new MilvusServiceClient(connectParam);
    }

    @Bean
    public MilvusVectorStore milvusVectorStore(MilvusServiceClient milvusClient) {
        return MilvusVectorStore.builder(milvusClient, dashScopeEmbeddingModel).build();
    }
}
