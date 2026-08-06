package br.edu.ufape.backend.exception;

public class PerfilNaoPermitidoException extends RuntimeException {

    public PerfilNaoPermitidoException() {
        super("Apenas perfis de ESTUDANTE podem ser criados através do cadastro público.");
    }
}
