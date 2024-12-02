package com.streaming.app.video_streaming_backend.Services;

import com.streaming.app.video_streaming_backend.Entities.Video;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

public interface VideoService {
    // save video
    Video saveVideo(Video video, MultipartFile file);

    // video_processing
    String processVideo(Video video, Path videoPath);

    public Video saveVideoToAws(Video video, MultipartFile file);
}
