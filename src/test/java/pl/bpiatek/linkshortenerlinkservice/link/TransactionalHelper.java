package pl.bpiatek.linkshortenerlinkservice.link;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class TransactionalHelper {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Transactional
    public void publishEventInsideTransaction(LinkCreatedApplicationEvent event) {
        eventPublisher.publishEvent(event);
    }

    @Transactional
    public void publishEventInsideFailingTransaction(LinkCreatedApplicationEvent event) {
        eventPublisher.publishEvent(event);
        throw new RuntimeException("Simulating a transaction rollback");
    }
}
