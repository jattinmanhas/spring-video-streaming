package com.streaming.app.videoprocessing.Controllers;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/video")
public class VideoProcessingControllers {
    @CrossOrigin(origins = "http://localhost:63342")
    @GetMapping("/{videoId}/master.m3u8")
    public ResponseEntity<Resource> serveMasterFile(@PathVariable String videoId){
        Path path = Paths.get("videos_hls", videoId, "master.m3u8");

        if(!Files.exists(path)){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Resource resource = new FileSystemResource(path);

        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, "application/vnd.apple.mpegurl")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, must-revalidate")
                .body(resource);
    }

    // serve the segments...
    @GetMapping("/{videoId}/{resolution}/{segment}.ts")
    @CrossOrigin(origins = "http://localhost:63342")
    public ResponseEntity<Resource> serveSegments(
            @PathVariable String videoId,
            @PathVariable String resolution,
            @PathVariable String segment
    ) {
        // create path for segment
        Path segmentPath = Paths.get("videos_hls", videoId, resolution, segment + ".ts");
        if (!Files.exists(segmentPath)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Resource resource = new FileSystemResource(segmentPath);

        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, "video/mp2t")
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=30")
                .body(resource);
    }

    @CrossOrigin(origins = "http://localhost:63342")
    @GetMapping("/{videoId}/{resolution}/playlist.m3u8")
    public ResponseEntity<Resource> servePlaylistFile(
            @PathVariable String videoId,
            @PathVariable String resolution) {
        Path path = Paths.get("videos_hls", videoId, resolution, "playlist.m3u8");

        if (!Files.exists(path)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Resource resource = new FileSystemResource(path);

        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, "application/vnd.apple.mpegurl")
                .body(resource);
    }
}
