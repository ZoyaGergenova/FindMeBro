package searchengine.dto.indexing;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ErrorResponse {

    private final boolean result;

    private final String error;
}
