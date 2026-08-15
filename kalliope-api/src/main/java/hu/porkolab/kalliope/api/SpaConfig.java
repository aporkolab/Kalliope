package hu.porkolab.kalliope.api;

import java.io.IOException;
import java.time.Duration;
import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Az Angular felület kiszolgálása a jar-ból.
 *
 * <p>A kliensoldali útvonalakat (deep link) az {@code index.html}-re vezetjük
 * vissza, de az {@code /api/**} alatt NEM — különben egy hibás API-hívás
 * HTML-lel válaszolna JSON helyett, és a hiba a kliensben derülne ki, nem itt.
 *
 * <p>Szándékosan nem a gyakran másolt {@code addViewController(…, "forward:/index.html")}
 * megoldás: az a resource handlerek ELÉ kerül, és elnyeli a kiterjesztés nélküli
 * statikus fájlokat is.
 */
@Configuration
class SpaConfig implements WebMvcConfigurer {

    private static final String STATIC_ROOT = "classpath:/static/";
    private static final ClassPathResource INDEX = new ClassPathResource("static/index.html");

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // A hash-nevű fájlok tartalma sosem változik — ezek egy évig gyorsítótárazhatók.
        registry.addResourceHandler("/*-*.js", "/*-*.css", "/media/**")
                .addResourceLocations(STATIC_ROOT)
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).immutable());

        // Az index.html viszont SOSEM: ő mutat a hash-nevű fájlokra, és ha
        // gyorsítótárazódik, a friss telepítés után is a régi bundle töltődik be.
        registry.addResourceHandler("/**")
                .setCacheControl(CacheControl.noStore())
                .addResourceLocations(STATIC_ROOT)
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected @Nullable Resource getResource(String resourcePath, Resource location)
                            throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        if (resourcePath.startsWith("api/")) {
                            return null;
                        }
                        return INDEX.exists() ? INDEX : null;
                    }
                });
    }
}
