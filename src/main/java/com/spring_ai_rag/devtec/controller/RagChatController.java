package com.spring_ai_rag.devtec.controller;

import com.spring_ai_rag.devtec.service.RagChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;

@RestController
@RequiredArgsConstructor
public class RagChatController {

    private final RagChatService ragChatService;

    @PostMapping("/pdf")
    public ResponseEntity<String> ingestPdf(@RequestParam("file")  @NotNull MultipartFile file) {
        try {
            ragChatService.ingestPdf(file);
            return ResponseEntity.ok("PDF ingerido com sucesso!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Erro ao processar o PDF: " + e.getMessage());
        }
    }

    @GetMapping("/chat")
    public ResponseEntity<String> askQuestion(@RequestParam String question) {
        try {
            return ResponseEntity.ok(ragChatService.chat(question));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao gerar resposta: " + e.getMessage());
        }
    }
}
