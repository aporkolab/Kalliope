package hu.porkolab.kalliope.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Egyszerű kérésszám-korlát az elemző végpontra.
 *
 * <p>Az elemzés processzoridőt eszik, és a szolgáltatás nyilvános. Egy
 * könyvtárnyi vers beküldése másodpercenként nem visszaélés, csak figyelmetlenség
 * — de ugyanúgy megfekteti a példányt. Csúszóablak helyett fix ablak: a
 * pontosság itt nem ér annyit, mint az, hogy a számláló elfér a memóriában és
 * nem kell hozzá Redis.
 */
@Component
@ConfigurationProperties("kalliope.rate-limit")
public class RateLimitFilter extends OncePerRequestFilter {

    /** Percenként engedélyezett elemzés kliensenként. */
    private int requestsPerMinute = 60;

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    private record Window(long minute, AtomicInteger count) {}

    public void setRequestsPerMinute(int requestsPerMinute) {
        this.requestsPerMinute = requestsPerMinute;
    }

    public int getRequestsPerMinute() {
        return requestsPerMinute;
    }

    /**
     * A servlet path már dekódolt és normalizált: a nyers request URI-val a
     * {@code /api/analyze;x=1} alak elkerülte a számlálót, a Spring mégis
     * ugyanarra a végpontra vitte.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        String path = pathInfo == null ? request.getServletPath() : request.getServletPath() + pathInfo;
        return !"/api/analyze".equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (requestsPerMinute <= 0) {
            chain.doFilter(request, response);
            return;
        }
        long minute = System.currentTimeMillis() / Duration.ofMinutes(1).toMillis();
        // Az X-Forwarded-For-t NEM itt olvassuk: a kliens bármit beleírhat, és
        // fejlécenként új számlálót kapott volna. A server.forward-headers-strategy
        // a Tomcat RemoteIpValve-jára bízza, ami csak megbízható proxytól fogadja
        // el, és jobbról olvassa — a remoteAddr így már a valódi kliens.
        String client = request.getRemoteAddr();
        Window window = windows.compute(
                client,
                (key, current) -> current == null || current.minute() != minute
                        ? new Window(minute, new AtomicInteger())
                        : current);
        if (window.count().incrementAndGet() > requestsPerMinute) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("""
                            {"type":"https://kalliope.porkolab.hu/problems/rate-limit",\
                            "title":"Túl sok kérés",\
                            "status":429,\
                            "detail":"Percenként legfeljebb %d elemzés kérhető. Várj egy kicsit."}""".formatted(requestsPerMinute));
            return;
        }
        if (windows.size() > 10_000) {
            windows.entrySet().removeIf(e -> e.getValue().minute() != minute);
        }
        chain.doFilter(request, response);
    }
}
