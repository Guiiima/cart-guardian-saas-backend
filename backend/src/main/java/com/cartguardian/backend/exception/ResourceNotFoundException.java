package com.cartguardian.backend.exception;

/**
 * Exceção para representar erros 404 (recurso não encontrado).
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
