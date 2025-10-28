package com.yourorg.gcdesk.preferences;

/**
 * Signals an unrecoverable failure when reading or writing the preferences file.
 */
public class PreferencesException extends RuntimeException {

    public PreferencesException(String message, Throwable cause) {
        super(message, cause);
    }

    public PreferencesException(String message) {
        super(message);
    }
}
