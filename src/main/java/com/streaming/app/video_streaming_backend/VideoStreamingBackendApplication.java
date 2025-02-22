package com.streaming.app.video_streaming_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
public class VideoStreamingBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(VideoStreamingBackendApplication.class, args);
	}

}
