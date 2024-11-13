package com.streaming.app.video_streaming_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.streaming.app.video_streaming_backend")
public class VideoStreamingBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(VideoStreamingBackendApplication.class, args);
	}

}
