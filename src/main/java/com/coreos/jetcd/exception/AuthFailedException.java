package com.coreos.jetcd.exception;

/**
 * Signals that an error occurred while attempting to auth, this
 * may caused by wrong username or password
 */
public class AuthFailedException extends Exception {
    public AuthFailedException(String cause, Throwable throwable){
        super(cause, throwable);
    }
}
