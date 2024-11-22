package com.streaming.app.video_streaming_backend.config;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.streaming.app.video_streaming_backend.config.AwsConstants.*;


@Configuration
public class AwsConfig {

    private String accessKeyId = AWSACCESSKEY;

    private String secretAccessKey = AWSSECRETKEY;

    private String region = AWSREGION;

    @Bean
    public S3Client s3Client() {
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .build();
    }
}
