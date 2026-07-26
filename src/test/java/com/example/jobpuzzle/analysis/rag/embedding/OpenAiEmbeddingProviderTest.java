package com.example.jobpuzzle.analysis.rag.embedding;

import com.example.jobpuzzle.analysis.rag.config.RagProperties;
import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class OpenAiEmbeddingProviderTest {
 @Test void sendsModelInputAndDimensionsAndValidatesResponse(){RagProperties p=props(); RestClient.Builder b=RestClient.builder().baseUrl("https://openai.test"); MockRestServiceServer s=MockRestServiceServer.bindTo(b).build(); OpenAiEmbeddingProvider provider=new OpenAiEmbeddingProvider(p,b.build()); s.expect(once(),requestTo("https://openai.test/v1/embeddings")).andExpect(method(org.springframework.http.HttpMethod.POST)).andExpect(content().json("{\"model\":\"text-embedding-3-small\",\"input\":\"hello\",\"dimensions\":3}" )).andRespond(withSuccess("{\"data\":[{\"embedding\":[0.1,0.2,0.3]}]}",MediaType.APPLICATION_JSON)); assertThat(provider.embed("hello").dimension()).isEqualTo(3);s.verify();}
 @Test void maps429(){RagProperties p=props();RestClient.Builder b=RestClient.builder().baseUrl("https://openai.test");MockRestServiceServer s=MockRestServiceServer.bindTo(b).build();OpenAiEmbeddingProvider provider=new OpenAiEmbeddingProvider(p,b.build());s.expect(requestTo("https://openai.test/v1/embeddings")).andRespond(withStatus(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS));assertThatThrownBy(()->provider.embed("x")).isInstanceOf(CustomException.class).extracting(e->((CustomException)e).getErrorCode()).isEqualTo(ErrorCode.EMBEDDING_RATE_LIMITED);}
 @Test void maps5xx(){RagProperties p=props();RestClient.Builder b=RestClient.builder().baseUrl("https://openai.test");MockRestServiceServer s=MockRestServiceServer.bindTo(b).build();OpenAiEmbeddingProvider provider=new OpenAiEmbeddingProvider(p,b.build());s.expect(requestTo("https://openai.test/v1/embeddings")).andRespond(withServerError());assertThatThrownBy(()->provider.embed("x")).isInstanceOf(CustomException.class).extracting(e->((CustomException)e).getErrorCode()).isEqualTo(ErrorCode.EMBEDDING_PROVIDER_ERROR);}
 private RagProperties props(){RagProperties p=new RagProperties();p.getOpenai().setApiKey("test");p.getEmbedding().setDimension(3);return p;}
}
