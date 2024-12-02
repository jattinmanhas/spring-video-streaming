package com.streamingService.DatabaseStreamingService.Service.Implementations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamingService.DatabaseStreamingService.Config.AppConstants;
import com.streamingService.DatabaseStreamingService.Entities.Video;
import com.streamingService.DatabaseStreamingService.Repositories.VideoRepository;
import com.streamingService.DatabaseStreamingService.Service.VideoDatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VideoServiceImplementation implements VideoDatabaseService {
    @Autowired
    private final VideoRepository videoRepository;

    @Autowired
    private final ObjectMapper objectMapper;

    public VideoServiceImplementation(VideoRepository videoRepository, ObjectMapper objectMapper) {
        this.videoRepository = videoRepository;
        this.objectMapper = objectMapper;
    }


    @Override
    public Video getById(String videoId) {
        Video video = videoRepository.findById(videoId).orElseThrow(() -> new RuntimeException("Video not Found"));
        return video;
    }

    @Override
    @KafkaListener(topics = AppConstants.KAFKATOPIC, groupId = AppConstants.GROUP_ID)
    public void saveVideo(String videoJson){
        try{
            Video video = objectMapper.readValue(videoJson, Video.class);
            videoRepository.save(video);

            System.out.println("Saved video: " + video);
        } catch (Exception e) {
            System.err.println("Failed to process video: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public List<Video> getAllVideos() {
        return videoRepository.findAll();
    }

}
