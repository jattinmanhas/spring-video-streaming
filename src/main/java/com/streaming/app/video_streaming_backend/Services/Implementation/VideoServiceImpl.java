package com.streaming.app.video_streaming_backend.Services.Implementation;

import com.streaming.app.video_streaming_backend.Entities.Video;
import com.streaming.app.video_streaming_backend.Repositories.VideoRepository;
import com.streaming.app.video_streaming_backend.Services.VideoService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;

@Service
public class VideoServiceImpl implements VideoService {
    @Autowired
    private VideoRepository videoRepository;

    @Value("${files.video}")
    String DIRECTORY;

    String HSL_DIR = "videos_hls";

    @PostConstruct
    public void init(){
        File file = new File(DIRECTORY);
        try {
            Files.createDirectories(Paths.get(HSL_DIR));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(!file.exists()){
            file.mkdir();
            System.out.println("Folder Created");;
        }else{
            System.out.println("Folder Already exists.");
        }
    }

    @Override
    public Video saveVideo(Video video, MultipartFile file) {
        String filename = file.getOriginalFilename(); // gets original filename
        String cleanFileName = StringUtils.cleanPath(filename);
        String cleanDirPath = StringUtils.cleanPath(DIRECTORY);
        Path videoPath = Paths.get(cleanDirPath, cleanFileName);
        Path hlsDirPath = Paths.get(HSL_DIR, video.getVideoId());

        try{
            String contentType = file.getContentType();
            InputStream inputStream = file.getInputStream();

            // copy file to  folder
            Files.copy(inputStream, videoPath, StandardCopyOption.REPLACE_EXISTING);

            // video metadata
            video.setContentType(file.getContentType());
            video.setFilePath(videoPath.toString());
            videoRepository.save(video);

            // process video.
            processVideo(video.getVideoId());

            // metadata save to the db.
            return video;
        } catch (Exception e) {
            e.printStackTrace();

            try {
                Files.deleteIfExists(videoPath);

                if(Files.exists(hlsDirPath)){
                    Files.walk(hlsDirPath)
                            .sorted(Comparator.reverseOrder())
                            .forEach(path -> {
                                try {
                                    Files.delete(path);
                                } catch (IOException ioException) {
                                    System.err.println("Failed to delete HLS file: " + path.toString());
                                }
                            });
                }

            }catch (IOException ioException){
                System.err.println("Failed to delete original video file: " + ioException.getMessage());
            }
            throw new RuntimeException("Video processing failed and files were cleaned up.", e);
        }
    }

    @Override
    public Video getById(String videoId) {
        Video video = videoRepository.findById(videoId).orElseThrow(() -> new RuntimeException("Video not Found"));
        return video;
    }

    @Override
    public Video getByTitle(String title) {
        return null;
    }

    @Override
    public List<Video> getAllVideos() {
        return videoRepository.findAll();
    }

    @Override
    public String processVideo(String videoId) {
        Video video = this.getById(videoId);
        String filePath = video.getFilePath();
        // path where to store data...
        Path videoPath = Paths.get(filePath);

        try{
            Path outputPath = Paths.get(HSL_DIR, videoId);
            Files.createDirectories(outputPath);

            String ffmpegCmd = String.format(
                    "ffmpeg -i \"%s\" -c:v libx264 -c:a aac -strict -2 -f hls -hls_time 10 -hls_list_size 0 -hls_segment_filename \"%s/segment_%%3d.ts\"  \"%s/master.m3u8\" ",
                    videoPath, outputPath, outputPath
            );
            System.out.println(ffmpegCmd);

            ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", "-c", ffmpegCmd);
            processBuilder.inheritIO();
            Process process = processBuilder.start();
            int exit = process.waitFor();
            if (exit != 0) {
                throw new RuntimeException("video processing failed!!");
            }

            return videoId;

        }catch(IOException ex){
            throw new RuntimeException("Video Processing Failed..");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
