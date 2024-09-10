package searchengine.exceptions;

public class IndexingIsAlreadyRunningException extends RuntimeException {
    public IndexingIsAlreadyRunningException() {
        super("Indexing is already running");
    }
}
