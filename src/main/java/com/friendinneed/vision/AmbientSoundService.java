package com.friendinneed.vision;

import org.springframework.stereotype.Service;

@Service
public class AmbientSoundService {
    /**
     * Future: integrate YAMNet or a sound classification model.
     */
    public String classifySound(byte[] audioChunk) {
        return "speech";
    }
}
