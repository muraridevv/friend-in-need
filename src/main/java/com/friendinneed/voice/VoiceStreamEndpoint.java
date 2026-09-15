package com.friendinneed.voice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class VoiceStreamEndpoint extends BinaryWebSocketHandler {
    private static final int MAX_AUDIO_BYTES = 10 * 1024 * 1024;
    private final VoiceService voice;
    private final ObjectMapper json;
    private final Map<String, ByteArrayOutputStream> buffers = new ConcurrentHashMap<>();

    public VoiceStreamEndpoint(VoiceService voice, ObjectMapper json) {
        this.voice = voice;
        this.json = json;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        buffers.put(session.getId(), new ByteArrayOutputStream());
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws IOException {
        message.getPayload().get(new byte[0]);
        ByteArrayOutputStream buffer = buffers.get(session.getId());
        byte[] bytes = new byte[message.getPayloadLength()];
        message.getPayload().get(bytes);
        if (buffer.size() + bytes.length > MAX_AUDIO_BYTES) {
            buffers.remove(session.getId());
            session.close(org.springframework.web.socket.CloseStatus.POLICY_VIOLATION);
            return;
        }
        buffer.write(bytes);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        if ("end".equals(message.getPayload())) {
            try {
                transcribe(session);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) {
        buffers.remove(session.getId());
    }

    private void transcribe(WebSocketSession session) throws Exception {
        ByteArrayOutputStream buffer = buffers.get(session.getId());
        if (buffer == null || buffer.size() == 0) return;
        MultipartFile audio = new AudioChunk(buffer.toByteArray());
        buffer.reset();
        String transcript = voice.transcribe(audio);
        session.sendMessage(new TextMessage(json.writeValueAsString(Map.of("transcript", transcript, "final", true))));
    }

    private static final class AudioChunk extends ByteArrayResource implements MultipartFile {
        AudioChunk(byte[] bytes) {
            super(bytes);
        }

        public String getName() {
            return "audio";
        }

        public String getOriginalFilename() {
            return "speech.pcm";
        }

        public String getContentType() {
            return "audio/pcm";
        }

        public boolean isEmpty() {
            return contentLength() == 0;
        }

        public long getSize() {
            return contentLength();
        }

        public byte[] getBytes() {
            return getByteArray();
        }

        public java.io.InputStream getInputStream() {
            return new java.io.ByteArrayInputStream(getByteArray());
        }

        public void transferTo(java.io.File file) throws IOException {
            java.nio.file.Files.write(file.toPath(), getByteArray());
        }
    }
}
