package com.postechfiap.meuhospital;

import com.azure.communication.email.EmailClient;
import com.azure.communication.email.EmailClientBuilder;
import com.azure.communication.email.models.EmailMessage;
import com.azure.communication.email.models.EmailSendResult;
import com.azure.core.util.polling.SyncPoller;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.models.*;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.models.UserSendMailParameterSet;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class FraseFunction {

    @FunctionName("enviarEmail")
    public HttpResponseMessage run(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.POST},
                    authLevel = AuthorizationLevel.FUNCTION
            )
            HttpRequestMessage<Optional<EmailDTO>> request,
            final ExecutionContext context) {

        context.getLogger().info("Azure Function iniciar: enviarEmail");

        EmailDTO dto = request.getBody().orElse(null);

        try {
            context.getLogger().info("Iniciando envio via Azure Communication Services");

            // O ACS_ENDPOINT é o que você configurou na variável de ambiente (da imagem anterior)
            String endpoint = System.getenv("ACS_ENDPOINT");
            String senderEmail = System.getenv("GRAPH_SENDER_EMAIL"); // O domínio que você provisionou no ACS

            // Autenticação automática via Managed Identity (O "Pulo do Gato")
            EmailClient emailClient = new EmailClientBuilder()
                    .endpoint(endpoint)
                    .credential(new DefaultAzureCredentialBuilder().build())
                    .buildClient();

            EmailMessage emailMessage = new EmailMessage()
                    .setSenderAddress(senderEmail)
                    .setSubject(dto.assunto)
                    .setBodyHtml(dto.corpo)
                    .setToRecipients(dto.emailDestino);

            context.getLogger().info("Enviando...");

            // Envio síncrono aguardando confirmação
            SyncPoller<EmailSendResult, EmailSendResult> poller = emailClient.beginSend(emailMessage, null);
            EmailSendResult result = poller.getFinalResult();

            context.getLogger().info("E-mail enviado! ID: " + result.getId());

            return request.createResponseBuilder(HttpStatus.OK).body("Enviado com ACS").build();

        } catch (Exception e) {
            context.getLogger().severe("Erro ACS: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage()).build();
        }

    }

    // DTO simples
    public static class EmailDTO {
        private String emailDestino;
        private String assunto;
        private String corpo;
        private String alunoNome;
        private String comentario;
        private String prioridade = "Urgente";
        private int nota;

        public String getEmailDestino() {
            return emailDestino;
        }

        public void setEmailDestino(String emailDestino) {
            this.emailDestino = emailDestino;
        }

        public String getAssunto() {
            return assunto;
        }

        public void setAssunto(String assunto) {
            this.assunto = assunto;
        }

        public String getCorpo() {
            return corpo;
        }

        public void setCorpo(String corpo) {
            this.corpo = corpo;
        }

        public String getAlunoNome() {
            return alunoNome;
        }

        public void setAlunoNome(String alunoNome) {
            this.alunoNome = alunoNome;
        }

        public String getComentario() {
            return comentario;
        }

        public void setComentario(String comentario) {
            this.comentario = comentario;
        }

        public String getPrioridade() {
            return prioridade;
        }

        public void setPrioridade(String prioridade) {
            this.prioridade = prioridade;
        }

        public int getNota() {
            return nota;
        }

        public void setNota(int nota) {
            this.nota = nota;
        }
    }
}
