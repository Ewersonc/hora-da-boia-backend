package br.com.horadaboia.exception;

/**
 * Recurso inexistente, como uma reserva com id desconhecido. A API responde 404 Not Found.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
