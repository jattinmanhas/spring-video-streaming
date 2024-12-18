package com.streaming.app.video_streaming_backend.Services.Implementation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streaming.app.video_streaming_backend.Entities.Video;
import com.streaming.app.video_streaming_backend.Services.VideoService;
import com.streaming.app.video_streaming_backend.config.AwsConstants;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.streaming.app.video_streaming_backend.config.AwsConstants.AWSBUCKETNAME;

@Service
public class VideoServiceImpl implements VideoService {

    @Autowired
    private final KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper;

    @Autowired
    private S3Client s3Client;

    private String bucketName = AWSBUCKETNAME;

    String DIRECTORY = "videos";

    String HSL_DIR = "videos_hls";

    public VideoServiceImpl(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

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

            String videoDuration = getVideoDuration(videoPath);
            video.setDuration(videoDuration);

//            videoRepository.save(video);

            // process video.
            processVideo(video, videoPath);

            // delete the original video.
            Files.deleteIfExists(videoPath);

            // send video metadata to the kafka...
            updateVideoMetadata(video);

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

    private void updateVideoMetadata(Video video) {
        try{
            String videoJson = objectMapper.writeValueAsString(video);
            kafkaTemplate.send(AwsConstants.KAFKATOPIC, videoJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Topic Published" + video);
    }

    @Override
    public Video saveVideoToAws(Video video, MultipartFile file){
        String fileName = file.getOriginalFilename();
        String cleanFileName = StringUtils.cleanPath(fileName);
        String s3Key = "original_videos/" + video.getVideoId() +  cleanFileName;

        try{
            s3Client.putObject(PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(s3Key)
                            .contentType(file.getContentType())
                            .acl(ObjectCannedACL.PRIVATE)
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            video.setContentType(file.getContentType());


            return video;
        }catch (Exception e){
            e.printStackTrace();

            // Cleanup: Delete the video file from S3 if something goes wrong
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build());

            throw new RuntimeException("Video processing failed and files were cleaned up.", e);
        }
    }

    public static String getVideoDuration(Path videoPath) {

        try {
            // Construct the ffprobe command
            List<String> command = new ArrayList<>();
            command.add("ffprobe");
            command.add("-v");
            command.add("error");
            command.add("-show_entries");
            command.add("format=duration");
            command.add("-of");
            command.add("default=noprint_wrappers=1:nokey=1");
            command.add(videoPath.toString()); // Convert Path to String

            // Execute the command
            Process process = new ProcessBuilder(command).start();

            // Read the output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            StringBuilder output = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }

            // Wait for the process to finish
            process.waitFor();

            // Parse the duration (can be in seconds.milliseconds or HH:MM:SS.ms format)
            String durationString = output.toString().trim();

            if (durationString.contains(":")) {  // Handle HH:MM:SS.ms format
                return durationString; // Return directly if already in desired format
            } else { // Handle seconds.milliseconds format, convert to HH:MM:SS.ms

                try {
                    double seconds = Double.parseDouble(durationString);
                    return formatSeconds(seconds);

                } catch (NumberFormatException e) {
                    System.err.println("Error parsing duration: " + e.getMessage());
                    return null;
                }
            }




        } catch (IOException | InterruptedException e) {
            System.err.println("Error executing ffprobe: " + e.getMessage());
            return null;
        }
    }

    private static String formatSeconds(double seconds) {
        int hours = (int) (seconds / 3600);
        int minutes = (int) ((seconds % 3600) / 60);
        int secs = (int) (seconds % 60);
        double milliseconds = (seconds - (int)seconds) * 1000;  // Get fractional part for milliseconds

        return String.format("%02d:%02d:%02d.%03d", hours, minutes, secs, (int)milliseconds);
    }

    @Override
    public String processVideo(Video video, Path videoPath) {
        try{
            Path outputPath = Paths.get(HSL_DIR, video.getVideoId());
            Files.createDirectories(outputPath);

            String[] renditions = {
                    "1920x1080 5000k",  // Full HD (1080p) - Highest quality
                    "1280x720 3000k",   // High quality (720p)
                    "854x480 1500k",    // Medium quality (480p)
                    "640x360 800k",     // Low quality (360p)
                    "426x240 400k"      // Very low quality (240p)
            };

            for(String rendition: renditions){
                String[] parts = rendition.split(" ");
                String resolution = parts[0];
                String bitrate = parts[1];

                String renditionOutputPath = String.format("%s/%s_%sp", outputPath, video.getVideoId(), resolution);
                Files.createDirectories(Paths.get(renditionOutputPath));

//                String ffmpegCmd = String.format(
//                        "ffmpeg -i \"%s\" -c:v libx264 -c:a aac -b:v %s -vf scale=%s -strict -2 -f hls -hls_time 3 -hls_list_size 0 " +
//                                "-hls_segment_filename \"%s/segment_%%3d.ts\" \"%s/playlist.m3u8\"",
//                        videoPath, bitrate, resolution, renditionOutputPath, renditionOutputPath
//                );

                String ffmpegCmd = String.format(
                        "ffmpeg -i \"%s\" -c:v libx264 -c:a aac -b:v %s -vf scale=%s -sc_threshold 0 -g 72 -keyint_min 72 -strict -2 -f hls -hls_time 3 -hls_flags split_by_time -hls_list_size 0 " +
                                "-hls_segment_filename \"%s/segment_%%03d.ts\" \"%s/playlist.m3u8\"",
                        videoPath, bitrate, resolution, renditionOutputPath, renditionOutputPath
                );

                System.out.println(ffmpegCmd);


                ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", "-c", ffmpegCmd);
                processBuilder.inheritIO();
                Process process = processBuilder.start();
                int exit = process.waitFor();
                if (exit != 0) {
                    throw new RuntimeException("video processing failed!!");
                }
            }

            Path masterPlaylistPath = Paths.get(outputPath.toString(), "master.m3u8");
            try (BufferedWriter writer = Files.newBufferedWriter(masterPlaylistPath)) {
                writer.write("#EXTM3U\n");

                for (String rendition : renditions) {
                    String[] parts = rendition.split(" ");
                    String resolution = parts[0];
                    String bitrate = parts[1].replace("k", "000"); // Convert to bps for EXT-X-STREAM-INF
                    writer.write(String.format("#EXT-X-STREAM-INF:BANDWIDTH=%s,RESOLUTION=%s\n", bitrate, resolution));
                    writer.write(String.format("%s_%sp/playlist.m3u8\n", video.getVideoId(), resolution));
                }
            }

            return video.getVideoId();

        }catch(IOException ex){
            throw new RuntimeException("Video Processing Failed..");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
