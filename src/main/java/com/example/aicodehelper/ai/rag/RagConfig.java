package com.example.aicodehelper.ai.rag;

import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;

import dev.langchain4j.data.document.Document;
import org.springframework.context.annotation.Configuration;

import java.util.List;


@Configuration
public class RagConfig {

    // 注入通义千问模型的嵌入模型 Bean，用于将文本转换为向量表示
    @Resource
    private EmbeddingModel qwenEmbeddingModel;

    // 注入嵌入存储 Bean，用于存储和检索文本段的向量数据
    @Resource
    private EmbeddingStore<TextSegment> embeddingStore;


    // 定义内容检索器 Bean，这是 RAG（检索增强生成）的核心组件
    @Bean
    public ContentRetriever contentRetriever() {
        /*----RAG-----*/
        //1.加载文档：从 resources/docs 目录加载所有文档文件
        List<Document> documents = FileSystemDocumentLoader.loadDocuments("src/main/resources/docs");

        // 创建文档分段器：按段落分割文档，每段最大 1000 个字符，段间重叠 200 个字符以保证上下文连贯性
        DocumentByParagraphSplitter documentByParagraphSplitter =
                new DocumentByParagraphSplitter(1000,200);

        // 构建嵌入存储摄取器：负责将文档分割、转换并存储到向量数据库中
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                // 设置文档分割器，用于将长文档切分为小的文本段
                .documentSplitter(documentByParagraphSplitter)
                // 自定义文本段转换器：在原始内容前添加文件名作为额外上下文信息
                .textSegmentTransformer(textSegment -> TextSegment.from(textSegment.metadata().getString("file_name") +
                        "\n" + textSegment.text(), textSegment.metadata()))
                // 设置嵌入模型，用于将文本段转换为向量
                .embeddingModel(qwenEmbeddingModel)
                // 设置嵌入存储，用于保存生成的向量数据
                .embeddingStore(embeddingStore)
                // 构建摄取器实例
                .build();

        // 执行文档摄取：将所有文档分割并向量化后存储到嵌入存储中
        ingestor.ingest(documents);

        // 构建嵌入存储内容检索器：用于根据用户查询检索相关的文档片段
        EmbeddingStoreContentRetriever contentRetriever =  EmbeddingStoreContentRetriever.builder()
                // 设置嵌入存储，从中检索相关内容
                .embeddingStore(embeddingStore)
                // 设置嵌入模型，用于将用户查询转换为向量以进行相似度匹配
                .embeddingModel(qwenEmbeddingModel)
                // 设置最大返回结果数量为 5 个最相关的文档片段
                .maxResults(5)
                // 设置最小相似度阈值为 0.75，低于此分数的结果将被过滤掉
                .minScore(0.75)
                // 构建内容检索器实例
                .build();

        // 返回配置好的内容检索器 Bean
        return contentRetriever;
    }
}
