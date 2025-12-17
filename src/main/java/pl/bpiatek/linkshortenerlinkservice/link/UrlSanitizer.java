package pl.bpiatek.linkshortenerlinkservice.link;

class UrlSanitizer {

    private UrlSanitizer() {
    }

    static String prependProtocolIfMissing(String url) {
        if (url == null) return null;
        if (!url.matches("^(http|https)://.*")) {
            return "https://" + url;
        }
        return url;
    }
}
