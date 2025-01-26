package com.streaming.app.video_streaming_backend.Services.Implementation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streaming.app.video_streaming_backend.DTO.VideoDTO;
import com.streaming.app.video_streaming_backend.Entities.Video;
import com.streaming.app.video_streaming_backend.Mapper.VideoMapper;
import com.streaming.app.video_streaming_backend.Payload.PropertiesVariables;
import com.streaming.app.video_streaming_backend.Repository.VideoStreamingRepository;
import com.streaming.app.video_streaming_backend.Services.VideoService;
import com.streaming.app.video_streaming_backend.config.AwsConstants;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
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
    private final KafkaTemplate<String, VideoDTO> kafkaTemplate;
    private final PropertiesVariables propertiesVariables;
    private final VideoMapper videoMapper;
    private final VideoStreamingRepository repository;


    public VideoServiceImpl(KafkaTemplate<String, VideoDTO> kafkaTemplate, PropertiesVariables propertiesVariables, VideoMapper videoMapper, VideoStreamingRepository repository) {
        this.kafkaTemplate = kafkaTemplate;
        this.propertiesVariables = propertiesVariables;
        this.videoMapper = videoMapper;
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
    public Video saveVideo(Video video, MultipartFile file) {
        String filename = file.getOriginalFilename(); // gets original filename
        assert filename != null;
        String cleanFileName = StringUtils.cleanPath(filename);
        String cleanDirPath = StringUtils.cleanPath(propertiesVariables.getVideoPath());
        Path videoPath = Paths.get(cleanDirPath, cleanFileName);

        try{
            String contentType = file.getContentType();
             InputStream inputStream = file.getInputStream();

            // copy file to  folder
             Files.copy(inputStream, videoPath, StandardCopyOption.REPLACE_EXISTING);

            // video metadata
            video.setContentType(contentType);

//            String videoDuration = getVideoDuration(videoPath);
            video.setDuration("00:00");
            video.setStatus("PROCESSING");

            VideoDTO videoDTO = videoMapper.mapVideoToVideoDTO(video, videoPath, null);
            sendVideoDTO(videoDTO);

            repository.save(video);

            return video;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Video processing failed and files were cleaned up.", e);
        }
    }

    public void sendVideoDTO(VideoDTO videoDTO) {
        kafkaTemplate.send(AwsConstants.KAFKATOPIC, videoDTO);
        System.out.println("Published UserDTO: " + videoDTO);
    }
}
