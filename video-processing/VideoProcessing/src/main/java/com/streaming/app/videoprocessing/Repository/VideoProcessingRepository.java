package com.streaming.app.videoprocessing.Repository;

import com.streaming.app.videoprocessing.Entities.Video;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoProcessingRepository extends JpaRepository<Video, String> {
}
