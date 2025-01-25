package com.streaming.app.videoprocessing.Config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "files.video")
@Getter
@Setter
public class PropertiesVariables {
    private String videoPath;
    private String videoHsl;
}
