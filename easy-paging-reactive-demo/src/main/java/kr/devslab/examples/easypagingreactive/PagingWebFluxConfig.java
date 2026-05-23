package kr.devslab.examples.easypagingreactive;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.ReactivePageableHandlerMethodArgumentResolver;
import org.springframework.data.web.ReactiveSortHandlerMethodArgumentResolver;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;

/**
 * Registers Spring Data's reactive argument resolvers for {@code Pageable}
 * and {@code Sort} on WebFlux controllers.
 *
 * <p>Spring Boot's {@code SpringDataWebAutoConfiguration} only registers the
 * <strong>servlet</strong> versions — the {@code @EnableSpringDataWebSupport}
 * annotation is also servlet-only. WebFlux apps that want
 * {@code Pageable}/{@code Sort} method parameters must wire the reactive
 * resolvers themselves, which is exactly what this class does. Without it
 * controllers like {@code list(Pageable pageable, ...)} fail at request time
 * with {@code IllegalStateException: No primary or single unique constructor
 * found for interface org.springframework.data.domain.Pageable}.
 */
@Configuration
class PagingWebFluxConfig implements WebFluxConfigurer {

    @Override
    public void configureArgumentResolvers(ArgumentResolverConfigurer configurer) {
        // Order matters: pageable depends on sort, so sort goes first.
        configurer.addCustomResolver(new ReactiveSortHandlerMethodArgumentResolver());
        configurer.addCustomResolver(new ReactivePageableHandlerMethodArgumentResolver());
    }
}
