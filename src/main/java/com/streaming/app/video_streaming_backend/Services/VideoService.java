package com.streaming.app.video_streaming_backend.Services;

import com.streaming.app.video_streaming_backend.Entities.Video;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface VideoService {
    // save video
    Video saveVideo(Video video, MultipartFile file);
    //get video by id
    Video getById(String videoId);
    // get by title
    Video getByTitle(String title);
    //get all videos
    List<Video> getAllVideos();

    // video_processing
    String processVideo(String videoId);

    public Video saveVideoToAws(Video video, MultipartFile file);
}
