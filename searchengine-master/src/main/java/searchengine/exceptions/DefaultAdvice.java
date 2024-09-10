package searchengine.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import searchengine.dto.indexing.ErrorResponse;

@ControllerAdvice
public class DefaultAdvice {

    @ExceptionHandler({IndexingIsAlreadyRunningException.class, IndexingIsNotRunningException.class,
            InvalidPageAddressException.class, EmptySearchQueryException.class, SiteNotIndexedException.class,
            SiteNotIndexingListException.class, SitesNotIndexedException.class})
    public ResponseEntity<ErrorResponse> universalException(Exception e) {
        ErrorResponse response = new ErrorResponse(false, e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(OtherExceptions.class)
    public ResponseEntity<ErrorResponse> indexingException(OtherExceptions e) {
        ErrorResponse response = new ErrorResponse(false, e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.METHOD_NOT_ALLOWED);
    }
}
