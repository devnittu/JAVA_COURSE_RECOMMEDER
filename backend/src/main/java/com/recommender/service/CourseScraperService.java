package com.recommender.service;

import com.recommender.dto.CourseDetailDTO;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class CourseScraperService {

    private static final int TIMEOUT_MS = 10_000;
    private static final String USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    /**
     * Main entry point — scrapes any course URL and returns structured details.
     */
    public CourseDetailDTO scrape(String url) {
        if (url == null || url.isBlank()) {
            return fallback("Invalid URL", url);
        }

        try {
            String host = extractHost(url);

            // Route to platform-specific scraper
            if (host.contains("udemy.com"))        return scrapeUdemy(url);
            if (host.contains("youtube.com") || host.contains("youtu.be")) return scrapeYouTube(url);
            if (host.contains("coursera.org"))     return scrapeCoursera(url);
            if (host.contains("edx.org"))          return scrapeGeneric(url, "edX");
            if (host.contains("freecodecamp.org")) return scrapeGeneric(url, "freeCodeCamp");
            if (host.contains("udacity.com"))      return scrapeGeneric(url, "Udacity");
            if (host.contains("pluralsight.com"))  return scrapeGeneric(url, "Pluralsight");

            return scrapeGeneric(url, capitalize(host));

        } catch (Exception e) {
            log.error("Scrape failed for {}: {}", url, e.getMessage());
            return fallback("Could not fetch course details.", url);
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Udemy scraper — uses og: tags + JSON-LD + DOM selectors
    // ─────────────────────────────────────────────────────────────
    private CourseDetailDTO scrapeUdemy(String url) throws Exception {
        log.debug("Scraping Udemy: {}", url);
        Document doc = fetchDocument(url);

        CourseDetailDTO dto = new CourseDetailDTO();
        dto.setPlatform("Udemy");
        dto.setUrl(url);

        // Title — og:title is most reliable
        dto.setTitle(ogTag(doc, "og:title"));
        dto.setDescription(ogTag(doc, "og:description"));
        dto.setThumbnail(ogTag(doc, "og:image"));

        // Instructor — data attribute, itemprop, or JSON-LD
        Element instructorEl = doc.selectFirst("[data-purpose='instructor-name'], [class*='instructor-name'], [itemprop='name']");
        if (instructorEl != null) dto.setInstructor(instructorEl.text().trim());

        // Rating
        Element ratingEl = doc.selectFirst("[data-purpose='rating-number'], [class*='star-rating--rating-number'], [itemprop='ratingValue']");
        if (ratingEl != null) dto.setRating(ratingEl.text().trim());

        // Students
        Element studentsEl = doc.selectFirst("[data-purpose='enrollment-count'], [class*='enrollment'], [itemprop='interactionCount']");
        if (studentsEl != null) dto.setStudents(studentsEl.text().trim());

        // Price
        Element priceEl = doc.selectFirst("[data-purpose='course-price-text'], [class*='price-text--price-part'], [itemprop='price'], meta[itemprop='price']");
        if (priceEl != null) {
            String price = priceEl.hasAttr("content") ? priceEl.attr("content") : priceEl.text();
            dto.setPrice(price.trim());
        }

        // What you'll learn
        Elements learnEls = doc.select("[data-purpose='course-what-you-will-learn-section'] li, [class*='what-you-will-learn'] li, [class*='curriculum-item']");
        dto.setWhatYouLearn(extractTexts(learnEls, 8));

        // Requirements
        Elements reqEls = doc.select("[class*='requirements'] li, [class*='requirements'] p");
        dto.setRequirements(extractTexts(reqEls, 5));

        // Fallback from JSON-LD structured data
        Element jsonLd = doc.selectFirst("script[type='application/ld+json']");
        if (jsonLd != null) {
            String json = jsonLd.data();
            if (dto.getTitle() == null) dto.setTitle(extractJson(json, "name"));
            if (dto.getDescription() == null) dto.setDescription(extractJson(json, "description"));
            if (dto.getThumbnail() == null) dto.setThumbnail(extractJson(json, "image"));
            if (dto.getInstructor() == null) dto.setInstructor(extractJsonObject(json, "author", "name"));
            if (dto.getRating() == null) dto.setRating(extractJson(json, "ratingValue"));

            if (dto.getRatingCount() == null) {
                String count = extractJson(json, "ratingCount");
                if (count == null) count = extractJson(json, "reviewCount");
                if (count != null) dto.setRatingCount(count + " ratings");
            }

            if (dto.getPrice() == null) {
                String price = extractJsonObject(json, "offers", "price");
                String currency = extractJsonObject(json, "offers", "priceCurrency");
                if (price != null) dto.setPrice(currency != null ? currency + " " + price : price);
            }

            if (dto.getStudents() == null) {
                String interactionCount = extractJson(json, "interactionCount");
                if (interactionCount != null) dto.setStudents(interactionCount + " learners");
            }

            if (dto.getLevel() == null) dto.setLevel(extractJson(json, "educationalLevel"));
            if (dto.getLanguage() == null) dto.setLanguage(extractJson(json, "inLanguage"));
            if (dto.getDuration() == null) dto.setDuration(extractJson(json, "timeRequired"));
        }

        if (dto.getTitle() == null) dto.setTitle(doc.title());
        if (dto.getDescription() == null) dto.setDescription(metaContent(doc, "description"));
        if (dto.getPrice() == null) dto.setPrice("Paid");
        dto.setLevel(firstNonEmpty(dto.getLevel(), metaContent(doc, "skill_difficulty")));

        dto.setScraped(true);
        return clean(dto);
    }

    // ─────────────────────────────────────────────────────────────
    //  YouTube scraper — uses oEmbed API + og: tags
    // ─────────────────────────────────────────────────────────────
    private CourseDetailDTO scrapeYouTube(String url) throws Exception {
        log.debug("Scraping YouTube: {}", url);

        // Extract video ID
        String videoId = extractYouTubeId(url);
        CourseDetailDTO dto = new CourseDetailDTO();
        dto.setPlatform("YouTube");
        dto.setUrl(url);
        dto.setPrice("Free");

        // Use oEmbed — no API key needed
        if (videoId != null) {
            try {
                String oembedUrl = "https://www.youtube.com/oembed?url=" +
                    "https://www.youtube.com/watch?v=" + videoId + "&format=json";
                Document oembedDoc = Jsoup.connect(oembedUrl)
                    .userAgent(USER_AGENT).timeout(TIMEOUT_MS).ignoreContentType(true).get();
                String json = oembedDoc.body().text();

                dto.setTitle(extractJson(json, "title"));
                dto.setInstructor(extractJson(json, "author_name"));
                dto.setThumbnail("https://img.youtube.com/vi/" + videoId + "/maxresdefault.jpg");
            } catch (Exception e) {
                log.debug("oEmbed failed, falling back to page scrape: {}", e.getMessage());
            }
        }

        // Also scrape the page for description
        try {
            Document doc = fetchDocument(url);
            if (dto.getTitle() == null) dto.setTitle(ogTag(doc, "og:title"));
            if (dto.getThumbnail() == null) dto.setThumbnail(ogTag(doc, "og:image"));

            String desc = ogTag(doc, "og:description");
            if (desc != null) {
                dto.setDescription(desc);
                // Extract bullet points from description
                String[] lines = desc.split("[\\n•\\-]");
                List<String> learn = new ArrayList<>();
                for (String line : lines) {
                    line = line.trim();
                    if (line.length() > 20) learn.add(line);
                    if (learn.size() >= 5) break;
                }
                dto.setWhatYouLearn(learn);
            }
        } catch (Exception e) {
            log.debug("YouTube page scrape failed: {}", e.getMessage());
        }

        dto.setScraped(true);
        return clean(dto);
    }

    // ─────────────────────────────────────────────────────────────
    //  Coursera scraper — JSON-LD is very rich
    // ─────────────────────────────────────────────────────────────
    private CourseDetailDTO scrapeCoursera(String url) throws Exception {
        log.debug("Scraping Coursera: {}", url);
        Document doc = fetchDocument(url);

        CourseDetailDTO dto = new CourseDetailDTO();
        dto.setPlatform("Coursera");
        dto.setUrl(url);

        // Coursera has good og: tags
        dto.setTitle(ogTag(doc, "og:title"));
        dto.setDescription(ogTag(doc, "og:description"));
        dto.setThumbnail(ogTag(doc, "og:image"));

        // Try JSON-LD for structured data
        Element jsonLd = doc.selectFirst("script[type='application/ld+json']");
        if (jsonLd != null) {
            String json = jsonLd.data();
            String name = extractJson(json, "name");
            if (name != null && dto.getTitle() == null) dto.setTitle(name);

            String desc = extractJson(json, "description");
            if (desc != null && dto.getDescription() == null) dto.setDescription(desc);

            String rating = extractJson(json, "ratingValue");
            if (rating != null) dto.setRating(rating);

            String ratingCount = extractJson(json, "reviewCount");
            if (ratingCount != null) dto.setRatingCount(ratingCount + " ratings");

            String provider = extractJsonObject(json, "provider", "name");
            if (provider != null) dto.setInstructor(provider);
        }

        // DOM selectors for Coursera-specific fields
        Element instrEl = doc.selectFirst("[class*='instructor-name'], [data-e2e='instructors-section-instructor-name']");
        if (instrEl != null) dto.setInstructor(instrEl.text());

        Element levelEl = doc.selectFirst("[class*='difficulty']");
        if (levelEl != null) dto.setLevel(levelEl.text());

        dto.setPrice("Free / Audit Available");
        dto.setScraped(true);
        return clean(dto);
    }

    // ─────────────────────────────────────────────────────────────
    //  Generic scraper — works on any site via og: tags + JSON-LD
    // ─────────────────────────────────────────────────────────────
    private CourseDetailDTO scrapeGeneric(String url, String platform) throws Exception {
        log.debug("Scraping {} (generic): {}", platform, url);
        Document doc = fetchDocument(url);

        CourseDetailDTO dto = new CourseDetailDTO();
        dto.setPlatform(platform);
        dto.setUrl(url);

        // Open Graph — works on virtually every modern website
        dto.setTitle(ogTag(doc, "og:title"));
        if (dto.getTitle() == null) dto.setTitle(doc.title());

        dto.setDescription(ogTag(doc, "og:description"));
        if (dto.getDescription() == null) dto.setDescription(metaContent(doc, "description"));

        dto.setThumbnail(ogTag(doc, "og:image"));

        // JSON-LD structured data
        Element jsonLd = doc.selectFirst("script[type='application/ld+json']");
        if (jsonLd != null) {
            String json = jsonLd.data();
            if (dto.getRating() == null) dto.setRating(extractJson(json, "ratingValue"));
            if (dto.getInstructor() == null) dto.setInstructor(extractJson(json, "author"));
        }

        // Common selectors for instructor / pricing
        Element priceEl = doc.selectFirst("[itemprop='price'], [class*='price']");
        if (priceEl != null) dto.setPrice(priceEl.attr("content").isBlank() ? priceEl.text() : priceEl.attr("content"));

        dto.setScraped(true);
        return clean(dto);
    }

    // ─────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────

    private Document fetchDocument(String url) throws Exception {
        return Jsoup.connect(url)
            .userAgent(USER_AGENT)
            .timeout(TIMEOUT_MS)
            .followRedirects(true)
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .get();
    }

    private String ogTag(Document doc, String property) {
        Element el = doc.selectFirst("meta[property='" + property + "']");
        if (el == null) el = doc.selectFirst("meta[name='" + property + "']");
        return el != null ? el.attr("content").trim() : null;
    }

    private String metaContent(Document doc, String name) {
        Element el = doc.selectFirst("meta[name='" + name + "']");
        return el != null ? el.attr("content").trim() : null;
    }

    private List<String> extractTexts(Elements els, int max) {
        List<String> list = new ArrayList<>();
        for (Element el : els) {
            String t = el.text().trim();
            if (!t.isEmpty()) list.add(t);
            if (list.size() >= max) break;
        }
        return list;
    }

    private String firstNonEmpty(String... values) {
        if (values == null) return null;
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    /** Extract a JSON string value by key (simple regex, no full parse needed) */
    private String extractJson(String json, String key) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(?:\"([^\"]+)\"|([0-9]+(?:\\.[0-9]+)?))");
        Matcher m = p.matcher(json);
        if (!m.find()) return null;
        String value = m.group(1) != null ? m.group(1) : m.group(2);
        return value != null ? value.trim() : null;
    }

    /** Extract nested JSON: { "outerKey": { "innerKey": "value" } } */
    private String extractJsonObject(String json, String outerKey, String innerKey) {
        Pattern p = Pattern.compile("(?s)\"" + Pattern.quote(outerKey) + "\"\\s*:\\s*\\{[^}]*\"" + Pattern.quote(innerKey) + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1).trim() : null;
    }

    /** Extract YouTube video ID from multiple URL formats */
    private String extractYouTubeId(String url) {
        Pattern p = Pattern.compile("(?:youtu\\.be/|watch\\?v=|embed/|shorts/)([a-zA-Z0-9_-]{11})");
        Matcher m = p.matcher(url);
        return m.find() ? m.group(1) : null;
    }

    private String extractHost(String url) {
        try {
            return java.net.URI.create(url).getHost().toLowerCase();
        } catch (Exception e) {
            return url.toLowerCase();
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** Truncate long descriptions */
    private CourseDetailDTO clean(CourseDetailDTO dto) {
        if (dto.getDescription() != null && dto.getDescription().length() > 600) {
            dto.setDescription(dto.getDescription().substring(0, 597) + "...");
        }
        if (dto.getTitle() != null) {
            dto.setTitle(dto.getTitle()
                .replaceAll("\\s*\\|.*$", "")  // strip "| Site Name"
                .replaceAll("\\s*-\\s*Udemy.*$", "")
                .trim());
        }
        return dto;
    }

    private CourseDetailDTO fallback(String message, String url) {
        CourseDetailDTO dto = new CourseDetailDTO();
        dto.setTitle("Course Details");
        dto.setDescription(message);
        dto.setUrl(url);
        dto.setScraped(false);
        return dto;
    }
}
