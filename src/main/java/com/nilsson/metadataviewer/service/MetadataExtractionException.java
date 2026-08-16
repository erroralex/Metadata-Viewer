package com.nilsson.metadataviewer.service;

public class MetadataExtractionException extends RuntimeException {
    public MetadataExtractionException(String message) {
        super(message);
    }

    public MetadataExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
