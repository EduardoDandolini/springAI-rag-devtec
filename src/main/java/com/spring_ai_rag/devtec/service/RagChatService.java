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
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
public class RagChatService {

    private final VectorStore vectorStore;

    private final ChatClient chatClient;

    private final RetrievalAugmentationAdvisor retrievalAugmentationAdvisor;

    private static final String PROMPT = String.join("\n",
            "Você é um assistente virtual de estudos que auxilia estudantes a compreender conteúdos acadêmicos.",
            "Suas respostas devem ser claras, organizadas e objetivas, adequadas para ajudar em atividades da faculdade.",
            "",
            "Regras importantes:",
            "- Responda APENAS com base nos conteúdos disponíveis nos documentos fornecidos.",
            "- Quando possível, cite a parte ou referência do material de onde a resposta foi retirada.",
            "- Se a pergunta não estiver respondida no material, diga educadamente que não encontrou a informação no conteúdo disponível.",
            "- Nunca invente informações ou especule além do que está nos documentos.",
            "- Se o usuário pedir algo fora do escopo do material, sugira consultar a bibliografia ou entrar em contato com o professor.",
            "",
            "Objetivo:",
            "Ajudar os estudantes a compreender o conteúdo de forma didática e confiável."
    );

    public void ingestPdf(MultipartFile file) throws IOException {

        PagePdfDocumentReader reader = new PagePdfDocumentReader(
                new InputStreamResource(file.getInputStream()),
                PdfDocumentReaderConfig.builder()
                        .withPageExtractedTextFormatter(
                                ExtractedTextFormatter.builder()
                                        .withNumberOfBottomTextLinesToDelete(3)
                                        .withNumberOfTopPagesToSkipBeforeDelete(1)
                                        .build()
                        )
                        .withPagesPerDocument(1)
                        .build()
        );

        TokenTextSplitter tokenTextSplitter = new TokenTextSplitter();

        List<Document> splitDocuments = tokenTextSplitter.split(reader.read());
        for (Document splitDocument: splitDocuments) {
            splitDocument.getMetadata().put("filename", file.getOriginalFilename());
            splitDocument.getMetadata().put("version", 1);
            splitDocument.getMetadata().put("created_date", LocalDateTime.now());
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
