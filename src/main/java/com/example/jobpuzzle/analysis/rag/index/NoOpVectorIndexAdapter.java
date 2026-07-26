package com.example.jobpuzzle.analysis.rag.index;
import com.example.jobpuzzle.analysis.entity.AnalysisMaterialChunk;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.util.Collection;
@Component @ConditionalOnProperty(prefix="app.rag",name="mode",havingValue="fake",matchIfMissing=true)
public class NoOpVectorIndexAdapter implements VectorIndexPort { public void index(Collection<AnalysisMaterialChunk> c){} public void delete(Collection<AnalysisMaterialChunk> c){} public void requireReady(Long u,Long s,Collection<AnalysisMaterialChunk> c){} }
