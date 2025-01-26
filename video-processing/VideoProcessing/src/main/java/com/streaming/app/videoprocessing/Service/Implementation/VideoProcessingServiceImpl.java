package com.streaming.app.videoprocessing.Service.Implementation;

import com.streaming.app.videoprocessing.Config.AppConstants;
import com.streaming.app.videoprocessing.Config.AwsConstants;
import com.streaming.app.videoprocessing.Config.PropertiesVariables;
import com.streaming.app.videoprocessing.DTO.VideoDTO;
import com.streaming.app.videoprocessing.Entities.Video;
import com.streaming.app.videoprocessing.Repository.VideoProcessingRepository;
import com.streaming.app.videoprocessing.Service.VideoProcessingService;
import jakarta.annotation.PostConstruct;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class VideoProcessingServiceImpl implements VideoProcessingService {
    private final PropertiesVariables propertiesVariables;
    private final S3Client s3Client;
    private final VideoProcessingRepository repository;

    public VideoProcessingServiceImpl(PropertiesVariables propertiesVariables, S3Client s3Client, VideoProcessingRepository repository) {
        this.propertiesVariables = propertiesVariables;
        this.s3Client = s3Client;
        this.repository = repository;
    }

    @PostConstruct
    public void init(){
        File file = new File(propertiesVariables.getVideoPath());
        try {
            Files.createDirectories(Paths.get(propertiesVariables.getVideoHsl()));
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
    @KafkaListener(topics = AppConstants.KAFKATOPIC, groupId = AppConstants.GROUP_ID)
    public String processVideo(VideoDTO video) throws IOException {
        // Clean the directory path (e.g., "videos/")
        String cleanDirPath = StringUtils.cleanPath(propertiesVariables.getVideoPath());
        Path videoDir = Paths.get(cleanDirPath);

        // Extract the filename from the S3 key (e.g., "video123.mp4")
        String s3Key = video.getS3Key();
        String fileName = Paths.get(s3Key).getFileName().toString(); // Get the filename

        // Resolve the output path (e.g., "videos/video123.mp4")
        Path outputFilePath = videoDir.resolve(fileName);

        // Fetch the video from S3 into the resolved output path
        Path videoPath = fetchVideo(s3Key, outputFilePath);
        Path hlsDirPath = Paths.get(propertiesVariables.getVideoHsl(), video.getVideoId());
        Video videoDb = repository.findById(video.getVideoId()).orElse(null);

        String duration = getVideoDuration(videoPath);

        boolean isProcessed =  processVideoAsync(video, videoPath, hlsDirPath);
        if(!isProcessed){
            assert videoDb != null;
            videoDb.setStatus("FAILED");
            repository.save(videoDb);
            return "Failed to Process Video";
        }

        assert videoDb != null;
        videoDb.setStatus("PROCESSED");
        videoDb.setDuration(duration);
        repository.save(videoDb);

        return "VIDEO PROCESSED SUCCESSFULLY";
    }

    public Path fetchVideo(String key, Path outputPath) throws IOException {
        try {
            File outputFile = outputPath.toFile();
            File parentDir = outputFile.getParentFile();

            if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                throw new IOException("Failed to create parent directories for " + outputPath);
            }

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(AwsConstants.AWSBUCKETNAME)
                    .key(key)
                    .build();

            try (ResponseInputStream<GetObjectResponse> responseInputStream = s3Client.getObject(getObjectRequest);
                 FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
                 BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream)) {

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = responseInputStream.read(buffer)) != -1) {
                    bufferedOutputStream.write(buffer, 0, bytesRead);
                }
            }

            return outputPath;
        } catch (S3Exception e) {
            throw new IOException("Failed to fetch video from S3: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    public boolean processVideoAsync(VideoDTO video, Path videoPath, Path hlsDirPath){
        try{
            // process video.
            VideoProcessingFfmpeg(video, videoPath, hlsDirPath);

            // delete the original video.
            Files.deleteIfExists(videoPath);

            return true;
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
                return false;
            }

            return false;
        }
    }

    public String VideoProcessingFfmpeg(VideoDTO video, Path videoPath, Path outputPath){
        try{
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

}
