package dev.resolveforge;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.resolveforge.rag.KnowledgeLoader;
import dev.resolveforge.rag.PolicyChunk;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class RetrievalDatasetTest {
    @Test void atLeast25LabeledCasesReferenceRealChunksAndSources() throws Exception {
        var chunks=new KnowledgeLoader().load();
        var byId=chunks.stream().collect(Collectors.toMap(PolicyChunk::chunkId,c->c));
        var ids=new HashSet<String>();var categories=new HashSet<String>();int noAnswer=0;
        try(var stream=getClass().getResourceAsStream("/evaluation/retrieval-cases.json")) {
            var cases=new ObjectMapper().readTree(stream);assertThat(cases.size()).isGreaterThanOrEqualTo(25);
            for(var entry:cases) {
                assertThat(ids.add(entry.path("id").asText())).isTrue();categories.add(entry.path("category").asText());
                var sourceIds=new HashSet<String>();
                for(var id:entry.path("expectedChunkIds")) {assertThat(byId).containsKey(id.asText());sourceIds.add(byId.get(id.asText()).sourceId());}
                for(var source:entry.path("expectedSourceIds")) assertThat(sourceIds).contains(source.asText());
                if(entry.path("expectedChunkIds").isEmpty()) {noAnswer++;assertThat(entry.path("expectedSourceIds").isEmpty()).isTrue();}
            }
            assertThat(categories).contains("synonym","mixed","no-answer");assertThat(noAnswer).isGreaterThanOrEqualTo(5);
        }
    }
}
