package pl.bpiatek.linkshortenerlinkservice.exception;

import java.time.Instant;
import java.util.List;

record ApiError(
        Instant timestamp,
        String type,
        String title,
        int status,
        String detail,
        String instance,
        List<ValidationError> errors
) {
}
