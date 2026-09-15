package com.friendinneed.vision;

import java.io.IOException;
import java.util.Map;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/vision")
public class VisionController {
    private final VisionService vision;

    public VisionController(VisionService vision) {
        this.vision = vision;
    }

    @PostMapping("/describe")
    Map<String, String> describe(@RequestPart("image") MultipartFile image, @RequestParam(defaultValue = "Describe what you see") String prompt) throws IOException {
        return Map.of("description", vision.describeImage(image.getBytes(), prompt));
    }
}
