package com.recommender.service;

import com.recommender.dto.CourseDetailDTO;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class CourseScraperService {

    // ─── Request Configuration ───
    private static final int TIMEOUT_MS = 15_000;  // 15 sec for slow pages
    private static final int MAX_RETRIES = 2;
    private static final int INITIAL_RETRY_DELAY_MS = 1_000;  // 1 sec
    private static final int MAX_RETRY_DELAY_MS = 2_000;      // 2 sec
    private static final Random RANDOM = new Random();
    
    // ─── User-Agent Rotation ───
    private static final String[] USER_AGENTS = {
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    };
    
    // ─── In-Memory Cache (TTL: 10 minutes) ───
    private static final long CACHE_TTL_MS = 10 * 60 * 1_000;  // 10 minutes
    private static class CacheEntry {
        final CourseDetailDTO dto;
        final long expiryTime;
        CacheEntry(CourseDetailDTO dto) {
            this.dto = dto;
            this.expiryTime = System.currentTimeMillis() + CACHE_TTL_MS;
        }
        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }
    private final Map<String, CacheEntry> responseCache = new ConcurrentHashMap<>();

    /**
     * Main entry point — scrapes any course URL and returns structured details.
     * Now includes caching (TTL: 10 min) and retry logic.
     */
    public CourseDetailDTO scrape(String url) {
        if (url == null || url.isBlank()) {
            return fallback("Invalid URL", url);
        }

        // ── Check cache first ──
        CacheEntry cached = responseCache.get(url);
        if (cached != null && !cached.isExpired()) {
            log.debug("Cache HIT for {}", url);
            return cached.dto;
        }
        if (cached != null) {
            responseCache.remove(url);  // Clean expired entry
        }

        try {
            String host = extractHost(url);

            CourseDetailDTO result = null;
            // Route to platform-specific scraper
            if (host.contains("udemy.com"))        result = scrapeUdemy(url);
            else if (host.contains("youtube.com") || host.contains("youtu.be")) result = scrapeYouTube(url);
            else if (host.contains("coursera.org"))     result = scrapeCoursera(url);
            else if (host.contains("edx.org"))          result = scrapeGeneric(url, "edX");
            else if (host.contains("freecodecamp.org")) result = scrapeGeneric(url, "freeCodeCamp");
            else if (host.contains("udacity.com"))      result = scrapeGeneric(url, "Udacity");
            else if (host.contains("pluralsight.com"))  result = scrapeGeneric(url, "Pluralsight");
            else result = scrapeGeneric(url, capitalize(host));

            // Cache result if scraped successfully
            if (result != null && result.isScraped()) {
                responseCache.put(url, new CacheEntry(result));
                log.debug("Cache stored for {}", url);
            }
            return result;

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
        if (dto.getTitle() == null) {
            Element titleEl = doc.selectFirst("h1[data-purpose='course-title'], h1[class*='title']");
            if (titleEl != null) dto.setTitle(titleEl.text().trim());
        }

        // Description
        dto.setDescription(ogTag(doc, "og:description"));
        if (dto.getDescription() == null) {
            Element descEl = doc.selectFirst("div[data-purpose='course-description'], [class*='course-description']");
            if (descEl != null) dto.setDescription(descEl.text().trim());
        }

        // Thumbnail
        dto.setThumbnail(ogTag(doc, "og:image"));
        if (dto.getThumbnail() == null) {
            Element imgEl = doc.selectFirst("img[data-purpose='course-image'], img[class*='course-image']");
            if (imgEl != null) {
                String src = imgEl.attr("src");
                if (!src.isEmpty()) dto.setThumbnail(src);
            }
        }

        // Instructor — primary, fallback, JSON-LD
        Element instructorEl = doc.selectFirst("[data-purpose='instructor-name'], span[class*='instructor'], [itemprop='name']");
        if (instructorEl != null) {
            dto.setInstructor(instructorEl.text().trim());
        }

        // Rating
        Element ratingEl = doc.selectFirst("[data-purpose='rating-number'], [class*='star-rating'], [itemprop='ratingValue']");
        if (ratingEl != null) {
            String rating = ratingEl.text().trim();
            if (!rating.isEmpty()) dto.setRating(rating);
        }

        // Students / Enrollments
        Element studentsEl = doc.selectFirst("[data-purpose='enrollment-count'], [class*='enrollment'], [itemprop='interactionCount']");
        if (studentsEl != null) {
            String students = studentsEl.text().trim();
            if (!students.isEmpty()) dto.setStudents(students);
        }

        // Price — multiple selectors
        Element priceEl = doc.selectFirst("[data-purpose='course-price-text'], [class*='price'], meta[itemprop='price']");
        if (priceEl != null) {
            String price = priceEl.hasAttr("content") ? priceEl.attr("content") : priceEl.text();
            if (!price.isEmpty()) dto.setPrice(price.trim());
        }

        // What you'll learn
        Elements learnEls = doc.select("[data-purpose='course-what-you-will-learn-section'] li, [class*='learning-objectives'] li, li");
        dto.setWhatYouLearn(extractTexts(learnEls, 8));

        // Requirements
        Elements reqEls = doc.select("[class*='requirements'] li, [class*='requirements'] p, ul li");
        dto.setRequirements(extractTexts(reqEls, 5));

        // Fallback from JSON-LD structured data
        Element jsonLd = doc.selectFirst("script[type='application/ld+json']");
        if (jsonLd != null) {
            try {
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
            } catch (Exception e) {
                log.debug("Error parsing Udemy JSON-LD: {}", e.getMessage());
            }
        }

        // Final fallbacks
        if (dto.getTitle() == null) dto.setTitle(doc.title());
        if (dto.getDescription() == null) dto.setDescription(metaContent(doc, "description"));
        if (dto.getPrice() == null) dto.setPrice("Paid");
        if (dto.getLevel() == null) {
            String difficulty = metaContent(doc, "skill_difficulty");
            if (difficulty != null) dto.setLevel(difficulty);
        }

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

        // Use oEmbed — no API key needed (most reliable for YouTube)
        if (videoId != null) {
            try {
                String oembedUrl = "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=" 
                    + videoId + "&format=json";
                Document oembedDoc = Jsoup.connect(oembedUrl)
                    .userAgent(rotateUserAgent())
                    .timeout(TIMEOUT_MS)
                    .ignoreContentType(true)
                    .get();
                String json = oembedDoc.body().text();

                dto.setTitle(extractJson(json, "title"));
                dto.setInstructor(extractJson(json, "author_name"));
                dto.setThumbnail("https://img.youtube.com/vi/" + videoId + "/maxresdefault.jpg");
                
                log.debug("YouTube oEmbed successful for video {}", videoId);
            } catch (Exception e) {
                log.debug("oEmbed failed for {}, falling back to page scrape: {}", videoId, e.getMessage());
            }
        }

        // Also scrape the page for additional details
        try {
            Document doc = fetchDocument(url);
            
            if (dto.getTitle() == null) {
                dto.setTitle(ogTag(doc, "og:title"));
            }
            if (dto.getThumbnail() == null) {
                dto.setThumbnail(ogTag(doc, "og:image"));
            }

            String desc = ogTag(doc, "og:description");
            if (desc != null && !desc.isEmpty()) {
                dto.setDescription(desc);
                
                // Extract bullet points from description
                String[] lines = desc.split("[\\n•\\-]");
                List<String> learn = new ArrayList<>();
                for (String line : lines) {
                    line = line.trim();
                    if (line.length() > 20) {
                        learn.add(line);
                        if (learn.size() >= 5) break;
                    }
                }
                if (!learn.isEmpty()) dto.setWhatYouLearn(learn);
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
        if (dto.getTitle() == null) {
            Element titleEl = doc.selectFirst("h1[class*='title'], [data-test*='title']");
            if (titleEl != null) dto.setTitle(titleEl.text().trim());
        }

        dto.setDescription(ogTag(doc, "og:description"));
        if (dto.getDescription() == null) {
            Element descEl = doc.selectFirst("[class*='description'], [data-test*='description']");
            if (descEl != null) dto.setDescription(descEl.text().trim());
        }

        dto.setThumbnail(ogTag(doc, "og:image"));
        if (dto.getThumbnail() == null) {
            Element imgEl = doc.selectFirst("img[class*='course'], img[data-test*='image']");
            if (imgEl != null) {
                String src = imgEl.attr("src");
                if (!src.isEmpty()) dto.setThumbnail(src);
            }
        }

        // Try JSON-LD for structured data (most reliable for Coursera)
        Element jsonLd = doc.selectFirst("script[type='application/ld+json']");
        if (jsonLd != null) {
            try {
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
            } catch (Exception e) {
                log.debug("Error parsing Coursera JSON-LD: {}", e.getMessage());
            }
        }

        // DOM selectors for Coursera-specific fields
        Element instrEl = doc.selectFirst("[class*='instructor'], [data-e2e='instructors-section'], [data-test*='instructor']");
        if (instrEl != null) {
            String instructor = instrEl.text().trim();
            if (!instructor.isEmpty()) dto.setInstructor(instructor);
        }

        Element levelEl = doc.selectFirst("[class*='difficulty'], [class*='level'], [data-test*='level']");
        if (levelEl != null) {
            String level = levelEl.text().trim();
            if (!level.isEmpty()) dto.setLevel(level);
        }

        // Price/availability
        if (dto.getPrice() == null) {
            Element priceEl = doc.selectFirst("[class*='price'], [data-test*='price']");
            if (priceEl != null) {
                String price = priceEl.text().trim();
                if (!price.isEmpty()) dto.setPrice(price);
            }
        }
        if (dto.getPrice() == null) dto.setPrice("Free / Audit Available");

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
        if (dto.getTitle() == null) {
            dto.setTitle(ogTag(doc, "twitter:title"));
        }
        if (dto.getTitle() == null) {
            dto.setTitle(doc.title());
        }

        dto.setDescription(ogTag(doc, "og:description"));
        if (dto.getDescription() == null) {
            dto.setDescription(ogTag(doc, "twitter:description"));
        }
        if (dto.getDescription() == null) {
            dto.setDescription(metaContent(doc, "description"));
        }

        // Thumbnail — multiple fallbacks
        dto.setThumbnail(ogTag(doc, "og:image"));
        if (dto.getThumbnail() == null) {
            dto.setThumbnail(ogTag(doc, "twitter:image"));
        }
        if (dto.getThumbnail() == null) {
            Element imgEl = doc.selectFirst("img[class*='thumb'], img[class*='header'], img[class*='banner']");
            if (imgEl != null) {
                String src = imgEl.attr("src");
                if (!src.isEmpty()) dto.setThumbnail(src);
            }
        }

        // JSON-LD structured data (most reliable for course metadata)
        Element jsonLd = doc.selectFirst("script[type='application/ld+json']");
        if (jsonLd != null) {
            try {
                String json = jsonLd.data();
                if (dto.getRating() == null) dto.setRating(extractJson(json, "ratingValue"));
                if (dto.getInstructor() == null) {
                    dto.setInstructor(extractJson(json, "author"));
                    if (dto.getInstructor() == null) {
                        dto.setInstructor(extractJsonObject(json, "author", "name"));
                    }
                }
                if (dto.getPrice() == null) {
                    String price = extractJson(json, "price");
                    if (price != null) dto.setPrice(price);
                }
                if (dto.getDuration() == null) {
                    dto.setDuration(extractJson(json, "duration"));
                }
            } catch (Exception e) {
                log.debug("Error parsing generic JSON-LD: {}", e.getMessage());
            }
        }

        // Common selectors for instructor / pricing
        if (dto.getInstructor() == null) {
            Element instrEl = doc.selectFirst("[class*='instructor'], [class*='author'], [class*='creator']");
            if (instrEl != null) {
                String instructor = instrEl.text().trim();
                if (!instructor.isEmpty()) dto.setInstructor(instructor);
            }
        }

        if (dto.getPrice() == null) {
            Element priceEl = doc.selectFirst("[itemprop='price'], [class*='price'], [class*='cost']");
            if (priceEl != null) {
                String price = priceEl.hasAttr("content") ? priceEl.attr("content") : priceEl.text();
                if (!price.isEmpty()) dto.setPrice(price.trim());
            }
        }

        // Duration if available
        if (dto.getDuration() == null) {
            Element durationEl = doc.selectFirst("[class*='duration'], [class*='hours'], [itemprop='duration']");
            if (durationEl != null) {
                String duration = durationEl.text().trim();
                if (!duration.isEmpty()) dto.setDuration(duration);
            }
        }

        dto.setScraped(true);
        return clean(dto);
    }

    // ─────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────

    /**
     * Fetch document with retry logic on transient errors.
     * Retries on timeout / connection errors with exponential backoff.
     * Rotates User-Agent on each attempt.
     */
    private Document fetchDocument(String url) throws Exception {
        IOException lastException = null;

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                long startMs = System.currentTimeMillis();
                String userAgent = rotateUserAgent();
                
                Document doc = Jsoup.connect(url)
                    .userAgent(userAgent)
                    .timeout(TIMEOUT_MS)
                    .followRedirects(true)
                    .referrer("https://www.google.com")  // Anti-bot: realistic referrer
                    .header("Accept-Language", "en-US,en;q=0.9,en;q=0.8")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Cache-Control", "no-cache")
                    .header("Pragma", "no-cache")
                    .ignoreContentType(true)
                    .get();

                long durationMs = System.currentTimeMillis() - startMs;
                log.debug("Fetched {} in {}ms (attempt {}/{})", url, durationMs, attempt + 1, MAX_RETRIES + 1);
                
                return doc;

            } catch (javax.net.ssl.SSLException | java.net.SocketTimeoutException | java.net.ConnectException ex) {
                lastException = (IOException) ex;
                
                if (attempt < MAX_RETRIES) {
                    // Calculate exponential backoff with jitter
                    long delayMs = INITIAL_RETRY_DELAY_MS * (long) Math.pow(2, attempt) 
                                 + RANDOM.nextInt(500);  // Add 0-500ms jitter
                    delayMs = Math.min(delayMs, MAX_RETRY_DELAY_MS);
                    
                    log.warn("Transient error on attempt {}/{}: {} — retrying in {}ms", 
                        attempt + 1, MAX_RETRIES + 1, ex.getClass().getSimpleName(), delayMs);
                    
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Interrupted during retry backoff", ie);
                    }
                } else {
                    log.error("Failed to fetch {} after {} retries", url, MAX_RETRIES + 1);
                }

            } catch (Exception e) {
                // Non-retryable errors (4xx, parsing errors, etc.)
                log.warn("Non-retryable error fetching {}: {}", url, e.getMessage());
                throw e;
            }
        }

        throw lastException != null ? lastException : new IOException("Unknown error fetching " + url);
    }

    /**
     * Rotate between available User-Agents to avoid bot detection.
     */
    private String rotateUserAgent() {
        return USER_AGENTS[RANDOM.nextInt(USER_AGENTS.length)];
    }

    private String ogTag(Document doc, String property) {
        if (doc == null || property == null) return null;
        try {
            Element el = doc.selectFirst("meta[property='" + property + "']");
            if (el == null) el = doc.selectFirst("meta[name='" + property + "']");
            return el != null ? el.attr("content").trim() : null;
        } catch (Exception e) {
            log.debug("Error extracting og: tag {}: {}", property, e.getMessage());
            return null;
        }
    }

    private String metaContent(Document doc, String name) {
        if (doc == null || name == null) return null;
        try {
            Element el = doc.selectFirst("meta[name='" + name + "']");
            return el != null ? el.attr("content").trim() : null;
        } catch (Exception e) {
            log.debug("Error extracting meta {}: {}", name, e.getMessage());
            return null;
        }
    }

    private List<String> extractTexts(Elements els, int max) {
        List<String> list = new ArrayList<>();
        if (els == null || els.isEmpty()) return list;
        
        for (Element el : els) {
            if (el == null) continue;
            String t = el.text().trim();
            if (!t.isEmpty()) {
                list.add(t);
                if (list.size() >= max) break;
            }
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

    /** Truncate long descriptions and clean title formatting */
    private CourseDetailDTO clean(CourseDetailDTO dto) {
        if (dto == null) return new CourseDetailDTO();  // Defensive

        try {
            // Clean description
            if (dto.getDescription() != null && dto.getDescription().length() > 600) {
                dto.setDescription(dto.getDescription().substring(0, 597) + "...");
            }

            // Clean title
            if (dto.getTitle() != null && !dto.getTitle().isEmpty()) {
                dto.setTitle(dto.getTitle()
                    .replaceAll("\\s*\\|.*$", "")      // Strip "| SiteName"
                    .replaceAll("\\s*-\\s*Udemy.*$", "") // Strip "- Udemy"
                    .replaceAll("\\s*-\\s*Coursera.*$", "") // Strip "- Coursera"
                    .trim());
                
                // Ensure title isn't just whitespace after cleaning
                if (dto.getTitle().isEmpty()) {
                    dto.setTitle(null);
                }
            }

            return dto;
        } catch (Exception e) {
            log.warn("Error cleaning CourseDetailDTO: {}", e.getMessage());
            return dto;  // Return as-is if cleaning fails
        }
    }

    private CourseDetailDTO fallback(String message, String url) {
        log.warn("Fallback scraper used for {}: {}", url, message);
        CourseDetailDTO dto = new CourseDetailDTO();
        dto.setTitle("Course Details");
        dto.setDescription(message);
        dto.setUrl(url);
        dto.setScraped(false);
        return dto;
    }
}
