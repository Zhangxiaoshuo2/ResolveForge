package dev.resolveforge;

import com.sun.net.httpserver.HttpServer;
import dev.resolveforge.rag.*;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class EmbeddingHttpTest {
    @Test void validatesProtocolDimensionsIndexAndErrorsWithoutLeakingSecrets() throws Exception {
        var response=new AtomicReference<>("{\"model\":\"test-model\",\"data\":[{\"index\":1,\"embedding\":[0,1]},{\"index\":0,\"embedding\":[1,0]}]}");
        var header=new AtomicReference<String>();var body=new AtomicReference<String>();
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/v1/embeddings",e->{
            header.set(e.getRequestHeaders().getFirst("Authorization"));body.set(new String(e.getRequestBody().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));
            byte[] bytes=response.get().getBytes(java.nio.charset.StandardCharsets.UTF_8);e.sendResponseHeaders(200,bytes.length);e.getResponseBody().write(bytes);e.close();
        });server.start();
        try {
            var provider=new OpenAiEmbeddingProvider(new EmbeddingProperties("test-private-key","http://127.0.0.1:"+server.getAddress().getPort()+"/v1","test-model",2,5));
            var vectors=provider.embed(List.of("测试一","测试二"));
            assertThat(vectors.get(0)).containsExactly(1,0);assertThat(vectors.get(1)).containsExactly(0,1);
            assertThat(header.get()).isEqualTo("Bearer test-private-key");assertThat(body.get()).contains("\"dimensions\":2","\"encoding_format\":\"float\"");
            for(String invalid:List.of(
                    "{\"data\":[{\"index\":0,\"embedding\":[1]}]}",
                    "{\"data\":[{\"index\":0,\"embedding\":[0,0]}]}",
                    "{\"data\":[{\"index\":1,\"embedding\":[1,0]}]}",
                    "{\"model\":\"wrong-model\",\"data\":[{\"index\":0,\"embedding\":[1,0]}]}",
                    "{\"data\":[{\"index\":0,\"embedding\":[\"oops\",1]}]}")) {
                response.set(invalid);assertThatThrownBy(()->provider.embed(List.of("测试"))).isInstanceOf(IllegalStateException.class).hasMessageNotContaining("test-private-key");
            }
        } finally {server.stop(0);}
    }
}
