package com.streaming.backend.repositories;

import com.streaming.backend.models.Video;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoRepository extends JpaRepository<Video, Long> {

}
