package pl.bpiatek.linkshortenerlinkservice.exception;

public class KafkaEventSendingException extends RuntimeException {

    public KafkaEventSendingException(String message) {
        super(message);
    }
}
