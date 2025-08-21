package com.milou.cli.exception;

public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String email) {
        super("The email '" + email + "' is already in use.");
    }
}
