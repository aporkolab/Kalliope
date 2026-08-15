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

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"/api/analyze".equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (requestsPerMinute <= 0) {
            chain.doFilter(request, response);
            return;
        }
        long minute = System.currentTimeMillis() / Duration.ofMinutes(1).toMillis();
        String client = clientKey(request);
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

    private static String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }
}
