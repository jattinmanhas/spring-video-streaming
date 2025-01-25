package com.streaming.app.video_streaming_backend.Services;

import com.streaming.app.video_streaming_backend.DTO.VideoDTO;

import java.io.IOException;

public interface S3Service {
    public String saveVideoToAws(VideoDTO videoDTO) throws IOException;

}
