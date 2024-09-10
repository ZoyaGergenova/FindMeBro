package searchengine.exceptions;

public class EmptySearchQueryException extends RuntimeException {
    public EmptySearchQueryException() {
        super("An empty search query is specified");
    }
}
