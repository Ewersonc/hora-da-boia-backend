package br.com.horadaboia.exception;

/**
 * Violação de uma regra de negócio (RN01 a RN12), como horário indisponível.
 * A API responde 409 Conflict.
 */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
