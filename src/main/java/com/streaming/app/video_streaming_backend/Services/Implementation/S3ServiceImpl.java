package com.streaming.app.video_streaming_backend.Services.Implementation;

import com.streaming.app.video_streaming_backend.DTO.VideoDTO;
import com.streaming.app.video_streaming_backend.Entities.Video;
import com.streaming.app.video_streaming_backend.Mapper.VideoMapper;
import com.streaming.app.video_streaming_backend.Repository.VideoStreamingRepository;
import com.streaming.app.video_streaming_backend.Services.S3Service;
import com.streaming.app.video_streaming_backend.config.AwsConstants;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class S3ServiceImpl implements S3Service {

    private final S3Client s3Client;
    private final VideoStreamingRepository repository;
    private final VideoMapper videoMapper;
    private final KafkaTemplate<String, VideoDTO> kafkaTemplate;

    public S3ServiceImpl(S3Client s3Client, VideoStreamingRepository repository, VideoMapper videoMapper, KafkaTemplate<String, VideoDTO> kafkaTemplate) {
        this.s3Client = s3Client;
        this.repository = repository;
        this.videoMapper = videoMapper;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    @KafkaListener(topics = AwsConstants.KAFKATOPIC, groupId = "s3-consumer-group")
    public String saveVideoToAws(VideoDTO videoDTO) throws IOException {
        PutObjectResponse videoUpload = UploadVideoToS3(videoDTO);
        String s3Key = "original_videos/" + videoDTO.getVideoId() + videoDTO.getVideoPath().getFileName().toString();
        System.out.println(videoUpload);

        if(videoUpload == null) {
            System.out.println("Video upload failed");
            return "Video Upload to S3 failed";
        }

        Video video = repository.findById(videoDTO.getVideoId()).orElse(null);
        if(video == null) {
            System.out.println("Video not found");
            return "Video not found";
        }

        video.setStatus("UPLOADED");
        repository.save(video);

        Files.deleteIfExists(videoDTO.getVideoPath());

        VideoDTO kafkaVideoDTO = videoMapper.mapVideoToVideoDTO(video, null, s3Key);
        System.out.println(kafkaVideoDTO);
        publishVideoToKafkaForProcessing(kafkaVideoDTO);

        return "Video Upload to S3 Success";
    }

    public PutObjectResponse UploadVideoToS3(VideoDTO videoDTO) throws IOException {
        Path filePath = videoDTO.getVideoPath();
        if(!Files.exists(filePath)) {
            throw new IllegalArgumentException("Invalid video file path: " + filePath);
        }

        try{
            byte[] bytes = Files.readAllBytes(filePath);
            String s3Key = "original_videos/" + videoDTO.getVideoId() + filePath.getFileName().toString();

            PutObjectRequest putObjectRequest = PutObjectRequest
                    .builder()
                    .bucket(AwsConstants.AWSBUCKETNAME)
                    .key(s3Key)
                    .contentType(videoDTO.getContentType())
                    .build();

            PutObjectResponse response =  s3Client.putObject(putObjectRequest, RequestBody.fromBytes(bytes));

            return response;

        } catch (S3Exception e) {
            e.printStackTrace();
            System.out.println("File upload failed: " + e.awsErrorDetails().errorMessage());
            return null;
        }
    }

    public void publishVideoToKafkaForProcessing(VideoDTO videoDTO) {
        kafkaTemplate.send(AwsConstants.KAFKAPROCESSINGTOPIC, videoDTO);
        System.out.println("Published KafkaVideoDTO for Processing: " + videoDTO);

    }
}
