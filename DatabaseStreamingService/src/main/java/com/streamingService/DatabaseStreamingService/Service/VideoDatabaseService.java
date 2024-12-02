package com.streamingService.DatabaseStreamingService.Service;

import com.streamingService.DatabaseStreamingService.Entities.Video;

import java.util.List;

public interface VideoDatabaseService {
    Video getById(String videoId);

    void saveVideo(String video);

    List<Video> getAllVideos();
}
