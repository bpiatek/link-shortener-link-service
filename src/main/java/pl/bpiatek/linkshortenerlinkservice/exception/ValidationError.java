package pl.bpiatek.linkshortenerlinkservice.exception;

record ValidationError(
        String field,
        Object rejectedValue,
        String message
) {}
