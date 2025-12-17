package pl.bpiatek.linkshortenerlinkservice.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.bpiatek.linkshortenerlinkservice.api.dto.CreateLinkRequest;
import pl.bpiatek.linkshortenerlinkservice.api.dto.CreateLinkResponse;
import pl.bpiatek.linkshortenerlinkservice.api.dto.LinkDto;
import pl.bpiatek.linkshortenerlinkservice.api.dto.UpdateLinkRequest;
import pl.bpiatek.linkshortenerlinkservice.link.LinkFacade;

@RestController
@RequestMapping("/links")
class LinkController {

    private final LinkFacade linkFacade;

    LinkController(LinkFacade linkFacade) {
        this.linkFacade = linkFacade;
    }

    @PostMapping
    ResponseEntity<CreateLinkResponse> createLink(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateLinkRequest request) {
        var response = linkFacade.createLink(userId, request.longUrl(), request.shortUrl(), request.isActive(), request.title());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{linkId}")
    ResponseEntity<LinkDto> updateLink(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable Long linkId,
            @Valid @RequestBody UpdateLinkRequest request) {

        var response = linkFacade.updateLink(userId, linkId, request);
        return ResponseEntity.ok(response);
    }
}
