package com.streaming.app.video_streaming_backend.Controllers;

import com.streaming.app.video_streaming_backend.AppConstants;
import com.streaming.app.video_streaming_backend.Entities.Video;
import com.streaming.app.video_streaming_backend.Services.VideoService;
import com.streaming.app.video_streaming_backend.Payload.CustomMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/video")
public class VideoController {

    private final VideoService videoService;

    @Autowired
    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @PostMapping("createVideo")
    public ResponseEntity<?> createVideo(
            @RequestParam("file")MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("description") String description
            ){

        Video video = new Video();
        video.setTitle(title);
        video.setDescription(description);
        video.setVideoId(UUID.randomUUID().toString());

 
        Video savedVideo = videoService.saveVideo(video, file);
        if(savedVideo != null){
            return ResponseEntity.status(HttpStatus.OK).body(video);
        }else{
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new CustomMessage("Video not Uploaded", HttpStatus.INTERNAL_SERVER_ERROR, true)
            );
        }
    }

    @GetMapping("/stream/{videoId}")
    public ResponseEntity<Resource> streamVideo(
            @PathVariable String videoId
    ){
        Video video = videoService.getById(videoId);
        String contentType = video.getContentType();
        String filePath = video.getFilePath();

        if(contentType == null){
            contentType="application/octet-stream";
        }

        Resource resource = new FileSystemResource(filePath);

        return ResponseEntity
                .ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @GetMapping("allVideos")
    public List<Video> getAllVideos(){
        return videoService.getAllVideos();
    }

    // stream video in chunks...
    @GetMapping("/stream/range/{videoId}")
    public ResponseEntity<Resource> streamVideoRange(
            @PathVariable String videoId,
            @RequestHeader(value = "Range", required = false) String range
    ){

        System.out.println(range);

        Video video = videoService.getById(videoId);
        Path path = Paths.get(video.getFilePath());

        Resource resource = new FileSystemResource(path);
        String contentType = video.getContentType();

        if(contentType == null){
            contentType="application/octet-stream";
        }

        long fileLength = path.toFile().length();

        // return full video if range header is null.
        if(range == null){
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType)).body(resource);
        }

        // calculating video streaming ranges...
        long rangeStart;
        long rangeEnd;
        InputStream inputStream;

        String[] ranges = range.replace("bytes=", "").split("-");
        rangeStart = Long.parseLong(ranges[0]);

        rangeEnd = rangeStart + AppConstants.CHUNK_SIZE - 1;
        if(rangeEnd >= fileLength){
            rangeEnd = fileLength - 1;
        }

        try{
            inputStream = Files.newInputStream(path);
            inputStream.skip(rangeStart);

            long contentLength = rangeEnd - rangeStart + 1;
            byte[] data = new byte[(int)contentLength];
            int read = inputStream.read(data, 0, data.length);

            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add("Content-Range", "bytes " + rangeStart + "-" + rangeEnd + "/" + fileLength);
            httpHeaders.add("Cache-Control", "no-cache, no-store, must-revalidate");
            httpHeaders.add("Pragma", "no-cache");
            httpHeaders.add("Expires", "0");
            httpHeaders.add("X-Content-Type-Options", "nosniff");
            httpHeaders.setContentLength(contentLength);

            return ResponseEntity
                    .status(HttpStatus.PARTIAL_CONTENT)
                    .headers(httpHeaders)
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(new ByteArrayResource(data));

        }catch(IOException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // serve hls playlist

    // master .m2u8 file

    @GetMapping("/{videoId}/master.m3u8")
    public ResponseEntity<Resource> serveMasterFile(@PathVariable String videoId){
        Path path = Paths.get("videos_hls", videoId, "master.m3u8");

        if(!Files.exists(path)){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Resource resource = new FileSystemResource(path);

        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, "application/vnd.apple.mpegurl")
                .body(resource);
    }

    // serve the segments...
    @GetMapping("/{videoId}/{segment}.ts")
    public ResponseEntity<Resource> serveSegments(
            @PathVariable String videoId,
            @PathVariable String segment
    ) {

        // create path for segment
        Path path = Paths.get("videos_hls", videoId, segment + ".ts");
        if (!Files.exists(path)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Resource resource = new FileSystemResource(path);

        return ResponseEntity
                .ok()
                .header(
                        HttpHeaders.CONTENT_TYPE, "video/mp2t"
                )
                .body(resource);

    }

}
