package baize.code.java.ai.service;

import baize.code.java.entity.Session;
import baize.code.java.websocket.message.ChatMessage;

public interface AIService {

    void HumanOperation(ChatMessage chatMessage, Session session);

    void chat(ChatMessage chatMessage,Integer ctId,Integer userId) throws IllegalAccessException;
}
