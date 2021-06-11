package com.example.signalingserver.socket;

import com.example.signalingserver.model.SignalMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Service
public class SignalingSocketHandler extends TextWebSocketHandler {
    private static final Logger LOG = LoggerFactory.getLogger(SignalingSocketHandler.class);

    private static final String OFFER_TYPE = "offer";
    private static final String ANSWER_TYPE = "answer";
    private static final String HANGUP_TYPE = "hangup";
    private static final String ICE_TYPE = "ice-candidate";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private Set<WebSocketSession> sessions = new HashSet<>();

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        LOG.debug("handleTextMessage : {}", message.getPayload());

        SignalMessage signalMessage = objectMapper.readValue(message.getPayload(), SignalMessage.class);
        System.out.println(signalMessage.toString());

        publishToOtherClients(session, message);

//        if (OFFER_TYPE.equalsIgnoreCase(signalMessage.getType())) {
//
//
//
//        } else if (ICE_TYPE.equalsIgnoreCase(signalMessage.getType())){
////            System.out.println(signalMessage.getData());
//
//            if (this.session == null || !this.session.isOpen()) {
//                this.session = session;
//            } else {
//                LOG.debug("Client already in use...");
//            }
//            this.session.sendMessage(new TextMessage(resendingMessage));
//        } else if (HANGUP_TYPE.equalsIgnoreCase(signalMessage.getType())){
//            this.session = null;
//        }
    }

    private void publishToOtherClients(WebSocketSession session, TextMessage message) {

        sessions.stream().filter(s -> !s.equals(session)).forEach(s -> {
            if (s.isOpen()) {
                LOG.warn(session.getId() + " publishes to " + s.getId());
                try {
                    s.sendMessage(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        LOG.info("[" + session.getId() + "] Connection established " + session.getId());

        sessions.add(session);
    }

    private static String getString(final SignalMessage message) throws Exception {
        return objectMapper.writeValueAsString(message);
    }
}
