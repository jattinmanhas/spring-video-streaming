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
    // master .m2u8 file
    /*
     * PlayList Loading: When a video player starts playing an HLS stream,
     * it first requests the master playlist (master.m3u8).
     * This playlist contains links to the different rendition playlists (e.g., different resolutions).
     *
     * Rendition Selection: The player then selects the appropriate rendition playlist
     * based on network conditions and user preferences
     *
     * Segment Downloading: The player downloads the initial segments listed in the chosen rendition
     * playlist to fill its buffer.
     * This is why we see multiple segment requests at the beginning.
     * HLS uses a sliding window approach.
     * It keeps downloading segments ahead of the current playback position to maintain a buffer
     * and ensure smooth playback.
     *
     * Dynamic Loading: Once the initial buffer is filled,
     * the player continues to download segments as needed,
     * based on the #EXT-X-ENDLIST tag (or lack thereof) in the playlist.
     * If the playlist is a live stream (no #EXT-X-ENDLIST),
     * the player will periodically refresh the playlist to get new segments.
     *
     * */

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
