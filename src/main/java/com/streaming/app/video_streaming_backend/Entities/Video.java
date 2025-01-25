package com.streaming.app.video_streaming_backend.Entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "videos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Video {
    @Id
    private String videoId;
    private String title;
    private String description;
    private String contentType;
    private String duration;
    private String status;
}
