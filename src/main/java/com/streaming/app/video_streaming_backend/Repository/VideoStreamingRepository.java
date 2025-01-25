package com.streaming.app.video_streaming_backend.Repository;

import com.streaming.app.video_streaming_backend.Entities.Video;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoStreamingRepository extends JpaRepository<Video, String> {
}
