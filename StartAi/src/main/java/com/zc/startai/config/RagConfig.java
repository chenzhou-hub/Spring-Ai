package com.zc.startai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * RAG 配置：多格式知识装载 + 向量库快照持久化
 *
 * 启动策略（避免每次启动都重复调用 embedding）：
 * 1. 计算 docs/ 目录指纹（文件名 + 大小 + 修改时间）
 * 2. 指纹没变且存在快照 → 直接 load 快照，零 embedding 调用
 * 3. 指纹变了（增/删/改过文档）→ 重新读取、切块、向量化，并保存新快照
 */
@Configuration
public class RagConfig {

    private static final String KB_SYSTEM_PROMPT = """
            你是"星辰科技"的员工制度助手。
            回答规则：
            1. 只根据下面提供的参考资料回答问题，不要使用你自己知识库里的信息；
            2. 如果参考资料中没有相关内容，如实回答"手册中没有相关规定"，不要编造；
            3. 回答时引用具体条款内容，用简洁的中文。
            """;

    private static final Path SNAPSHOT_FILE = Path.of("vector-store.json");
    private static final Path FINGERPRINT_FILE = Path.of("docs-fingerprint.txt");

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) throws IOException {
        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();

        String fingerprint = docsFingerprint();

        // 命中快照：直接加载，不调用 embedding
        if (fingerprint != null && Files.exists(SNAPSHOT_FILE) && Files.exists(FINGERPRINT_FILE)
                && Files.readString(FINGERPRINT_FILE).equals(fingerprint)) {
            store.load(SNAPSHOT_FILE.toFile());
            return store;
        }

        // 未命中：全量装载（读文档 → 切块 → 向量化入库）
        List<Document> chunks = TokenTextSplitter.builder().build().apply(loadAllDocs());
        store.add(chunks);

        // 保存快照和指纹，下次启动免 embedding
        store.save(SNAPSHOT_FILE.toFile());
        if (fingerprint != null) {
            Files.writeString(FINGERPRINT_FILE, fingerprint);
        }
        return store;
    }

    /**
     * 遍历 classpath:docs/ 目录，按扩展名选择读取器（其它格式由 Tika 兜底）
     */
    private List<Document> loadAllDocs() throws IOException {
        Resource[] files = new PathMatchingResourcePatternResolver().getResources("classpath:docs/*");
        List<Document> all = new ArrayList<>();
        for (Resource file : files) {
            String name = file.getFilename();
            if (name == null || name.isBlank()) {
                continue;
            }
            DocumentReader reader = pickReader(name, file);
            for (Document doc : reader.get()) {
                // 每个块都带上来源，检索命中后可以知道"依据哪份文档"
                doc.getMetadata().put("source", name);
                all.add(doc);
            }
        }
        return all;
    }

    private DocumentReader pickReader(String name, Resource file) {
        String lower = name.toLowerCase();
        if (lower.endsWith(".md") || lower.endsWith(".markdown")) {
            return new MarkdownDocumentReader(file, MarkdownDocumentReaderConfig.builder().build());
        }
        if (lower.endsWith(".pdf")) {
            // 按页读取，稳健；若 PDF 自带目录结构可换 ParagraphPdfDocumentReader 按段落读
            return new PagePdfDocumentReader(file);
        }
        if (lower.endsWith(".txt")) {
            return new TextReader(file);
        }
        // Word/Excel/PPT/HTML/邮件等一律交给 Tika 兜底
        return new TikaDocumentReader(file);
    }

    /**
     * 目录指纹：任何文件新增/删除/修改都会导致指纹变化
     */
    private String docsFingerprint() throws IOException {
        Resource[] files = new PathMatchingResourcePatternResolver().getResources("classpath:docs/*");
        StringBuilder sb = new StringBuilder();
        for (Resource file : files) {
            sb.append(file.getFilename()).append(':');
            try {
                sb.append(file.contentLength()).append(':').append(file.lastModified()).append(';');
            } catch (IOException ignored) {
                sb.append("unreadable;");
            }
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    /**
     * 制度问答专用 ChatClient：挂 QuestionAnswerAdvisor，每次提问自动检索知识库
     */
    @Bean
    public ChatClient kbChatClient(ChatClient.Builder builder, VectorStore vectorStore) {
        return builder
                .defaultSystem(KB_SYSTEM_PROMPT)
                .defaultAdvisors(QuestionAnswerAdvisor.builder(vectorStore).build())
                .build();
    }
}
