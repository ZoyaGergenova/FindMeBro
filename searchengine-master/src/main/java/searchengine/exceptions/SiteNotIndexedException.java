package searchengine.exceptions;

public class SiteNotIndexedException extends RuntimeException {
    public SiteNotIndexedException() {
        super("The site is not indexed");
    }
}
