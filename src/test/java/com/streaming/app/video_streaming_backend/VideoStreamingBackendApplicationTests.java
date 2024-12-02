package com.streaming.app.video_streaming_backend;

import com.streaming.app.video_streaming_backend.Services.VideoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class VideoStreamingBackendApplicationTests {

	@Autowired
	VideoService videoService;

	@Test
	void contextLoads() {

	}

}
