package com.example.signalingserver.socket;

import com.example.signalingserver.model.SignalMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Service
public class SignalingSocketHandler extends TextWebSocketHandler {
    private final Set<WebSocketSession> sessions = new HashSet<>(); // A map could be used to identify different users

    private static final Logger LOG = LoggerFactory.getLogger(SignalingSocketHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        LOG.info("*** Connection established with {} ***", session.getId());
        sessions.add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        LOG.info("*** Handling TextMessage : {} ***", message.getPayload());
        // If the message contained callee information, we could map to an object and publish to one specific session
        publishMessageToOtherClients(session, message);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        LOG.info("*** Connection error {} with reason {} ***", session.getId(), status.getReason());
        sessions.remove(session);
        sendHangUpMessageToOtherSessions();
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        LOG.info("*** Connection error {} with status: {} ***", session.getId(), exception.getLocalizedMessage());
        sessions.remove(session);
        sendHangUpMessageToOtherSessions();
    }

    private void publishMessageToOtherClients(WebSocketSession session, TextMessage message) {

        sessions.stream().filter(s -> !s.equals(session)).forEach(s -> {
            if (s.isOpen()) {
                LOG.info("*** {} published a message to {} ***", session.getId(), s.getId());
                try {
                    s.sendMessage(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void sendHangUpMessageToOtherSessions() {
        final SignalMessage message = new SignalMessage();

        sessions.forEach(session -> {
            try {
                session.sendMessage(new TextMessage(getString(message)));  // "{}" - Empty object: could contain sender information
            } catch (Exception e) {
                LOG.warn("!!! Error while sending hangup message to {} !!!", session.getId());
            }
        });
    }

    private static String getString(final SignalMessage message) throws Exception {
        return objectMapper.writeValueAsString(message);
    }
}