package com.streamingService.DatabaseStreamingService.Controller;

import com.streamingService.DatabaseStreamingService.Entities.Video;
import com.streamingService.DatabaseStreamingService.Service.VideoDatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/video")
public class VideoDatabaseController {

    private final VideoDatabaseService videoService;

    @Autowired
    public VideoDatabaseController(VideoDatabaseService videoService) {
        this.videoService = videoService;
    }

    @GetMapping("allVideos")
    public List<Video> getAllVideos(){
        return videoService.getAllVideos();
    }

    @GetMapping("{id}")
    public Video getVideoById(@PathVariable String id){
        return videoService.getById(id);
    }

}
