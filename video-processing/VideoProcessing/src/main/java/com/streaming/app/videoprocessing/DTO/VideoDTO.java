package com.streaming.app.videoprocessing.DTO;

import lombok.*;

import java.nio.file.Path;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class VideoDTO {
    private String videoId;
    private String title;
    private String description;
    private String contentType;
    private Path videoPath;
    private String S3Key;
    private String duration;
    private String status;
}
