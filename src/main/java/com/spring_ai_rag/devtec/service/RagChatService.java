package com.spring_ai_rag.devtec.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.core.io.Resource;

import java.util.List;

@RequiredArgsConstructor
@Service
public class RagChatService {

    private final VectorStore vectorStore;

    private final ChatClient chatClient;

    private final RetrievalAugmentationAdvisor retrievalAugmentationAdvisor;

    private static final String PROMPT = String.join("\n",
            "Você é o atendente virtual do Hotel Paraíso Azul.",
            "Responda de forma clara, amigável e objetiva dúvidas de clientes com base na política de reservas, cancelamentos e outras informações disponíveis.",
            "",
            "Se a pergunta não estiver diretamente relacionada às informações disponíveis, responda gentilmente que não tem essa informação,",
            "e sugira entrar em contato com a recepção pelo WhatsApp: (99) 99999-9999."
    );

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
        List<Document> results = vectorStore.similaritySearch(question);

        if (results == null || results.isEmpty()) {
            return "Desculpe, não tenho essa informação. Por favor, entre em contato com a recepção pelo WhatsApp: (99) 99999-9999.";
        }

        return chatClient.prompt(PROMPT)
                .advisors(retrievalAugmentationAdvisor)
                .user(question)
                .call()
                .content();
    }
}
