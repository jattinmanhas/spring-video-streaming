package com.streaming.app.videoprocessing.Service;

import com.streaming.app.videoprocessing.DTO.VideoDTO;
import com.streaming.app.videoprocessing.Entities.Video;

import java.io.IOException;

public interface VideoProcessingService {
    String processVideo(VideoDTO video) throws IOException;
}
