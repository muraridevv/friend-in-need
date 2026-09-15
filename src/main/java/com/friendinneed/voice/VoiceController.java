package com.friendinneed.voice;
import org.springframework.http.*; import org.springframework.web.bind.annotation.*; import org.springframework.web.multipart.MultipartFile; import java.io.IOException; import java.util.Map;
@RestController @RequestMapping("/api/voice") public class VoiceController {
    private final VoiceService voice; public VoiceController(VoiceService voice) { this.voice=voice; }
    @PostMapping(value="/transcriptions",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) Map<String,String> transcribe(@RequestPart("audio") MultipartFile audio) throws IOException { return Map.of("text",voice.transcribe(audio)); }
    @PostMapping(value="/speech",produces="audio/mpeg") byte[] speech(@RequestBody SpeechRequest request) { return voice.speak(request.text()); }
    record SpeechRequest(String text) { }
}
