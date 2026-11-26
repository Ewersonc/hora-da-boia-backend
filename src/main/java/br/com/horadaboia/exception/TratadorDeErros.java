package br.com.horadaboia.exception;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Converte exceções em respostas HTTP no formato de {@link ErroResposta} (RNF05).
 * Erros inesperados seguem o tratamento padrão do Spring, que não expõe detalhes internos.
 */
@RestControllerAdvice
public class TratadorDeErros {

    private static final Logger log = LoggerFactory.getLogger(TratadorDeErros.class);

    private final Clock clock;

    public TratadorDeErros(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResposta> aoValidar(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(erro -> campos.putIfAbsent(erro.getField(), erro.getDefaultMessage()));
        String mensagem = campos.isEmpty() ? "Dados inválidos." : campos.values().iterator().next();
        return resposta(HttpStatus.BAD_REQUEST, mensagem, campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResposta> aoLerCorpo(HttpMessageNotReadableException ex) {
        return resposta(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido.", Map.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResposta> aoConverterParametro(MethodArgumentTypeMismatchException ex) {
        return resposta(HttpStatus.BAD_REQUEST, "Valor inválido para o parâmetro " + ex.getName() + ".", Map.of());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErroResposta> aoFaltarParametro(MissingServletRequestParameterException ex) {
        return resposta(HttpStatus.BAD_REQUEST, "Informe o parâmetro " + ex.getParameterName() + ".", Map.of());
    }

    @ExceptionHandler(RequisicaoInvalidaException.class)
    public ResponseEntity<ErroResposta> aoReceberRequisicaoInvalida(RequisicaoInvalidaException ex) {
        return resposta(HttpStatus.BAD_REQUEST, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<ErroResposta> aoRecusarLogin(CredenciaisInvalidasException ex) {
        return resposta(HttpStatus.UNAUTHORIZED, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResposta> aoNegarAcesso(AccessDeniedException ex) {
        return resposta(HttpStatus.FORBIDDEN, "Acesso negado.", Map.of());
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResposta> aoNaoEncontrar(RecursoNaoEncontradoException ex) {
        return resposta(HttpStatus.NOT_FOUND, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroResposta> aoViolarRegra(RegraNegocioException ex) {
        return resposta(HttpStatus.CONFLICT, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResposta> aoViolarIntegridade(DataIntegrityViolationException ex) {
        log.warn("Violação de integridade no banco: {}", ex.getMostSpecificCause().getMessage());
        return resposta(HttpStatus.CONFLICT, "A operação conflita com dados já existentes.", Map.of());
    }

    private ResponseEntity<ErroResposta> resposta(HttpStatus status, String mensagem, Map<String, String> campos) {
        ErroResposta corpo = new ErroResposta(status.value(), mensagem, campos, LocalDateTime.now(clock));
        return ResponseEntity.status(status).body(corpo);
    }
}
