package baize.code.java.websocket.endpoint;

import baize.code.java.ai.service.AIService;
import baize.code.java.config.ChatMessageCoder;
import baize.code.java.mapper.SessionLogMapper;
import baize.code.java.service.RoleService;
import baize.code.java.service.SessionService;
import baize.code.java.utils.SessionFind;
import baize.code.java.websocket.message.ChatMessage;
import baize.code.java.entity.SessionLog;
import cn.hutool.core.util.ObjectUtil;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ServerEndpoint(value = "/user/chat/{userId}", decoders = ChatMessageCoder.class, encoders = ChatMessageCoder.class)
public class UserServiceEndpoint implements WebSocketEndpoint {
    private static final ConcurrentHashMap<Integer, UserServiceEndpoint> userEndpointPool = new ConcurrentHashMap<>();

    private static SessionService sessionService;
    private Session session;
    private Integer userId;
    private  static SessionLogMapper sessionLogMapper;
    private static AIService aIService;
    private static RoleService roleService;
    private static SessionFind sessionFind;

    @Autowired
    public void setSessionService(RoleService roleService,AIService aIService,SessionService sessionService,SessionLogMapper sessionLogMapper,SessionFind sessionFind) {
         UserServiceEndpoint.sessionService = sessionService;
         UserServiceEndpoint.sessionLogMapper = sessionLogMapper;
         UserServiceEndpoint.sessionFind = sessionFind;
         UserServiceEndpoint.aIService = aIService;
         UserServiceEndpoint.roleService = roleService;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") Integer userId) {
        this.session = session;
        this.userId = userId;
        userEndpointPool.put(userId, this);
    }

    @OnClose
    public void onClose() {
        if (userId != null) {
            userEndpointPool.remove(userId);
        }
    }

    // TODO:005
    @OnMessage
    public void onMessage(ChatMessage message, Session session) throws EncodeException, IOException, IllegalAccessException {
        message.setType(getEndpointType());
        baize.code.java.entity.Session session1= sessionService.find(message, userId,this);
        CommercialTenantEndpoint commercialTenantEndpointById = sessionFind.getCommercialTenantEndpointById(session1.getId());
        switch (session1.getConversationStatus()){
            case AI -> {
                //判断当前用户是否有转人工的意愿
                aIService.HumanOperation(message, session1);
                aIService.chat(message, session1.getCtId(), userId);
            }
            case HUMAN -> {
                sessionLogMapper.insert(SessionLog.builder()
                        .sessionId(session1.getId())
                        .type(message.getType())
                        .content(message.getMessage())
                        .readStatus(SessionLog.ReadStatus.UNREAD)
                        .build());
                if(!ObjectUtil.isEmpty(commercialTenantEndpointById)){
                    commercialTenantEndpointById.sendMessage(message);
                }
            }
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        try {
            error.printStackTrace();
            session.getBasicRemote().sendObject(ChatMessage.builder()
                    .state(ChatMessage.State.ERROR)
                    .message(error.getMessage())
                    .build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 封装发送消息的方法
     */
    public void sendMessage(ChatMessage chatMessage) throws EncodeException, IOException {
        this.session.getBasicRemote().sendObject(chatMessage);
    }

    @Override
    public SessionLog.Type getEndpointType() {
        return SessionLog.Type.USER;
    }

    public static UserServiceEndpoint findEndPoint(Integer userId) {
        return userEndpointPool.get(userId);
    }
}
