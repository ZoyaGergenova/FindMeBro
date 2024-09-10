package searchengine.exceptions;

public class IndexingIsNotRunningException extends RuntimeException {
    public IndexingIsNotRunningException() {
        super("Indexing is not running");
    }
}
