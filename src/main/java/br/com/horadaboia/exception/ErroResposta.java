package br.com.horadaboia.exception;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Formato único de erro da API: status, mensagem e, quando houver, os campos inválidos.
 */
public record ErroResposta(int status, String mensagem, Map<String, String> campos, LocalDateTime momento) {
}
