package searchengine.exceptions;

public class InvalidPageAddressException extends RuntimeException {
    public InvalidPageAddressException() {
        super("This page is located outside the sites, specified in the configuration file");
    }
}
