package br.com.horadaboia.exception;

/** Login recusado. A mensagem é genérica para não revelar se o e-mail existe. A API responde 401. */
public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException() {
        super("E-mail ou senha inválidos.");
    }
}
