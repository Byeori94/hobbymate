package com.byeori.hobbymate.file.controller;

import java.nio.file.Path;
import java.time.Duration;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.byeori.hobbymate.file.storage.ClubImageStorage;

@Controller
@RequestMapping("/club-images")
public class ClubImageController {

    private final ClubImageStorage clubImageStorage;

    public ClubImageController(ClubImageStorage clubImageStorage) {
        this.clubImageStorage = clubImageStorage;
    }

    @GetMapping("/{fileName:.+}")
    public ResponseEntity<Resource> image(@PathVariable String fileName) {
        Path image = clubImageStorage.find(fileName).orElse(null);
        if (image == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(clubImageStorage.mediaType(fileName))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(image));
    }
}
