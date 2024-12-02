package com.microservices.api_gateway.Filters;


import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@Component
public class RouteValidator {
    public static final List<String> routes = List.of(
            "/streamingdb/api/video/allVideos",
            "/actuator/health",
            "/authentication/api/auth/register",
            "/authentication/api/auth/authenticate",
            "streaming/api/video/e5bc992a-bc9d-40fe-82e5-8ba03a2e5161/master.m3u8",
            "streaming/api/video/e5bc992a-bc9d-40fe-82e5-8ba03a2e5161/e5bc992a-bc9d-40fe-82e5-8ba03a2e5161_1920x1080p/playlist.m3u8"
    );

    public static final List<Pattern> dynamicPatterns = List.of(
            Pattern.compile("/streaming/api/video/[a-f0-9\\-]+/master\\.m3u8"),
            Pattern.compile("/streaming/api/video/[a-f0-9\\-]+/[a-f0-9\\-]+_[0-9]+x[0-9]+p/playlist\\.m3u8"),
            Pattern.compile("/streaming/api/video/[a-f0-9\\-]+/[a-f0-9\\-]+_[0-9]+x[0-9]+p/segment_[0-9]+\\.ts")
    );

    public Predicate<ServerHttpRequest> isSecured = request -> {
        String path = request.getURI().getPath();

        boolean isStaticIgnored = routes.stream().anyMatch(path::contains);
        boolean isDynamicIgnored = dynamicPatterns.stream().anyMatch(pattern -> pattern.matcher(path).matches());

        return !(isStaticIgnored || isDynamicIgnored);
    };

}
