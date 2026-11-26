package br.com.horadaboia.exception;

/** Parâmetro ou dado de entrada inválido. A API responde 400 Bad Request. */
public class RequisicaoInvalidaException extends RuntimeException {

    public RequisicaoInvalidaException(String mensagem) {
        super(mensagem);
    }
}
