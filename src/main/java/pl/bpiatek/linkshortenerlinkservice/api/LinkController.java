package pl.bpiatek.linkshortenerlinkservice.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.bpiatek.linkshortenerlinkservice.api.dto.CreateLinkRequest;
import pl.bpiatek.linkshortenerlinkservice.api.dto.CreateLinkResponse;
import pl.bpiatek.linkshortenerlinkservice.link.LinkFacade;

import java.util.Collections;

@RestController
@RequestMapping("/links")
class LinkController {

    private static final Logger log = LoggerFactory.getLogger(LinkController.class);

    private final LinkFacade linkFacade;

    LinkController(LinkFacade linkFacade) {
        this.linkFacade = linkFacade;
    }

    @PostMapping
    ResponseEntity<CreateLinkResponse> createLink(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateLinkRequest request,
            HttpServletRequest httpRequest) {
        var response = linkFacade.createLink(userId, request.longUrl(), request.shortUrl());
        logHeaders(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private void logHeaders(HttpServletRequest request) {
        StringBuilder headerLog = new StringBuilder("--- Incoming Request Headers ---\n");
        Collections.list(request.getHeaderNames()).forEach(headerName ->
                headerLog.append(String.format("%s: %s%n", headerName, request.getHeader(headerName)))
        );
        log.debug(headerLog.toString());
    }
}
