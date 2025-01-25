package com.streaming.app.video_streaming_backend.Mapper;

import com.streaming.app.video_streaming_backend.DTO.VideoDTO;
import com.streaming.app.video_streaming_backend.Entities.Video;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class VideoMapper {
    public VideoDTO mapVideoToVideoDTO(Video video, Path videoPath, String s3Key) {
        VideoDTO videoDTO = new VideoDTO();
        videoDTO.setVideoId(video.getVideoId());
        videoDTO.setVideoPath(videoPath);
        videoDTO.setTitle(video.getTitle());
        videoDTO.setDescription(video.getDescription());
        videoDTO.setDuration(video.getDuration());
        videoDTO.setContentType(video.getContentType());
        videoDTO.setStatus(video.getStatus());
        videoDTO.setS3Key(s3Key);

        return videoDTO;
    }
}
