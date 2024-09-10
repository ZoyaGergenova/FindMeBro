package searchengine.exceptions;

public class SiteNotIndexingListException extends RuntimeException {
    public SiteNotIndexingListException() {
        super("The site is not in the indexing list");
    }
}
