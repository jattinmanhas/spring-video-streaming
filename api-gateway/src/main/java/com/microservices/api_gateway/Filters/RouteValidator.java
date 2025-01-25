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
            "streaming/api/video/f2a316b8-b328-4fee-9804-accb8c76cafe/master.m3u8",
            "streaming/api/video/f2a316b8-b328-4fee-9804-accb8c76cafe/f2a316b8-b328-4fee-9804-accb8c76cafe/playlist.m3u8"
    );

    public static final List<Pattern> dynamicPatterns = List.of(
            Pattern.compile("/processing/api/video/[a-f0-9\\-]+/master\\.m3u8"),
            Pattern.compile("/processing/api/video/[a-f0-9\\-]+/[a-f0-9\\-]+_[0-9]+x[0-9]+p/playlist\\.m3u8"),
            Pattern.compile("/processing/api/video/[a-f0-9\\-]+/[a-f0-9\\-]+_[0-9]+x[0-9]+p/segment_[0-9]+\\.ts")
    );

    public Predicate<ServerHttpRequest> isSecured = request -> {
        String path = request.getURI().getPath();

        boolean isStaticIgnored = routes.stream().anyMatch(path::contains);
        boolean isDynamicIgnored = dynamicPatterns.stream().anyMatch(pattern -> pattern.matcher(path).matches());

        return !(isStaticIgnored || isDynamicIgnored);
    };

}
