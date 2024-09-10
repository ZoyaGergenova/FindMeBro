package searchengine.exceptions;

public class SitesNotIndexedException extends RuntimeException {
    public SitesNotIndexedException() {
        super("The sites are not indexed");
    }
}
