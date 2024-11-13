package com.streaming.app.video_streaming_backend.Repositories;

import com.streaming.app.video_streaming_backend.Entities.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VideoRepository extends JpaRepository<Video,String> {
    Optional<Video> findByTitle(String title);
}
