package com.spring_ai_rag.devtec.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileUrlResource;
import org.springframework.stereotype.Service;
import org.springframework.core.io.Resource;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

@RequiredArgsConstructor
@Service
public class RagChatService {

    private final VectorStore vectorStore;

    private final ChatClient chatClient;

    private final RetrievalAugmentationAdvisor retrievalAugmentationAdvisor;

    public void ingestPdf(String url)  {
        Resource resource = new DefaultResourceLoader().getResource(url);

        PagePdfDocumentReader reader = new PagePdfDocumentReader(resource, PdfDocumentReaderConfig.builder()
                .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                        .withNumberOfBottomTextLinesToDelete(3)
                        .withNumberOfTopPagesToSkipBeforeDelete(1)
                        .build())
                .withPagesPerDocument(1)
                .build());

        TokenTextSplitter tokenTextSplitter = new TokenTextSplitter();

        List<Document> splitDocuments = tokenTextSplitter.split(reader.read());
        for (Document splitDocument: splitDocuments) {
            splitDocument.getMetadata().put("filename", resource.getFilename());
            splitDocument.getMetadata().put("version", 1);
        }

        vectorStore.write(splitDocuments);
    }

    public String chat(String question) {
        return chatClient.prompt()
                .advisors(retrievalAugmentationAdvisor)
                .user(question)
                .call()
                .content();
    }
}
