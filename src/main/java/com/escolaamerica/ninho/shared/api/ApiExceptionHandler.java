package com.escolaamerica.ninho.shared.api;

import com.escolaamerica.ninho.shared.domain.BusinessRuleViolationException;
import com.escolaamerica.ninho.shared.domain.ConflictException;
import com.escolaamerica.ninho.shared.domain.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Tabela de erros da SPEC §5.1: o único {@code @RestControllerAdvice} do sistema. */
@RestControllerAdvice
@RequiredArgsConstructor
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    static final String MENSAGEM_VALIDACAO = "Dados inválidos.";
    static final String MENSAGEM_REQUISICAO_INVALIDA = "Requisição inválida.";
    static final String MENSAGEM_CONFLITO_VERSAO = "O registro foi alterado por outra pessoa. Recarregue e tente de novo.";
    static final String MENSAGEM_CONFLITO_DADOS = "A operação conflita com dados existentes.";

    private final Clock clock;

    @ExceptionHandler({ResourceNotFoundException.class, NoResourceFoundException.class})
    ResponseEntity<ApiErrorResponse> handleNotFound(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    ResponseEntity<ApiErrorResponse> handleBusinessRule(BusinessRuleViolationException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    /** Cobre também {@code MethodArgumentNotValidException} (subclasse de {@link BindException}). */
    @ExceptionHandler(BindException.class)
    ResponseEntity<ApiErrorResponse> handleBind(BindException ex, HttpServletRequest request) {
        List<FieldError> erros = ex.getBindingResult().getFieldErrors();
        List<CampoInvalido> campos = erros.stream()
            .map(erro -> new CampoInvalido(erro.getField(), CampoInvalido.codigoDaRestricao(erro.getCode())))
            .toList();
        String mensagem = erros.stream()
            .map(FieldError::getDefaultMessage)
            .filter(m -> m != null && !m.isBlank())
            .findFirst()
            .orElse(MENSAGEM_VALIDACAO);
        return build(HttpStatus.BAD_REQUEST, mensagem, request, campos);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        List<ConstraintViolation<?>> violacoes = ex.getConstraintViolations().stream()
            .sorted(Comparator.comparing(v -> v.getPropertyPath().toString()))
            .toList();
        List<CampoInvalido> campos = violacoes.stream()
            .map(v -> new CampoInvalido(ultimoNo(v.getPropertyPath().toString()),
                CampoInvalido.codigoDaRestricao(v.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName())))
            .toList();
        String mensagem = violacoes.stream().map(ConstraintViolation::getMessage).findFirst().orElse(MENSAGEM_VALIDACAO);
        return build(HttpStatus.BAD_REQUEST, mensagem, request, campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiErrorResponse> handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, MENSAGEM_REQUISICAO_INVALIDA, request);
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ApiErrorResponse> handleConflict(ConflictException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ApiErrorResponse> handleOptimisticLock(ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, MENSAGEM_CONFLITO_VERSAO, request);
    }

    /** Nunca devolve o texto do banco (nome de constraint, SQL): só a mensagem genérica. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.info("Violação de integridade em {} {}", request.getMethod(), request.getRequestURI());
        return build(HttpStatus.CONFLICT, MENSAGEM_CONFLITO_DADOS, request);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiErrorResponse> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiErrorResponse> handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        return build(status == null ? HttpStatus.INTERNAL_SERVER_ERROR : status, ex.getReason(), request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erro inesperado", request);
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String message, HttpServletRequest request) {
        return build(status, message, request, null);
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String message, HttpServletRequest request,
                                                   List<CampoInvalido> fields) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(
            Instant.now(clock), status.value(), status.getReasonPhrase(), message, request.getRequestURI(), fields));
    }

    private static String ultimoNo(String caminho) {
        int ponto = caminho.lastIndexOf('.');
        return ponto < 0 ? caminho : caminho.substring(ponto + 1);
    }
}
