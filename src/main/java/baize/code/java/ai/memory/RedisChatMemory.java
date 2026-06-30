package baize.code.java.ai.memory;

import baize.code.java.entity.SessionLog;
import baize.code.java.service.SessionLogService;
import baize.code.java.utils.KeyUtils;
import baize.code.java.utils.TypeConversion;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;

import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public  class RedisChatMemory implements ChatMemory {
    @Resource
    private  RedisTemplate redisTemplate;

    @Value("${memory.key}")
    private  String key;
    @Value("${memory.expiration-duration}")
    private  Integer outTime;
    @Value("${memory.redis-length}")
    private  Integer max_message;
    @Resource
    private SessionLogService sessionLogService;
    @Override
    public void add(String conversationId, List<Message> messages) {
        List<SessionLog> sessionLogs = messages.stream().map(message -> {
            SessionLog.Type type = TypeConversion.messageToSessionType(message.getMessageType());
            return SessionLog.builder()
                    .content(message.getText())
                    .type(type)
                    .sessionId(Integer.parseInt(conversationId))
                    .timestamp(LocalDateTime.now())
                    .build();
        }).toList();
        sessionLogService.saveBatch(sessionLogs);
        String key = KeyUtils.redisKeyUtils(this.key, conversationId);
        for (Message message : messages) {
            JSONObject json = new JSONObject();
            json.set("messageType", message.getMessageType().getValue());
            json.set("content", message.getText());
            redisTemplate.opsForList().rightPush(key, JSONUtil.toJsonStr(json));
        }
        redisTemplate.opsForList().trim(key, 0, max_message - 1);
        redisTemplate.expire(key, outTime, TimeUnit.MINUTES);
    }

    @NotNull
    @Override
    public List<Message> get(@NotNull String conversationId) {
        String key = KeyUtils.redisKeyUtils(this.key, conversationId);
        List<String> range = redisTemplate.opsForList().range(key, 0, -1);
        if (range == null || range.isEmpty()) {
            List<SessionLog> sessionLogs = sessionLogService.getConversationMessagesIsTen(Integer.parseInt(conversationId));
            List<Message> messages = new ArrayList<>(sessionLogs.size());
            if (!sessionLogs.isEmpty()) {
                for (SessionLog sessionLog : sessionLogs) {
                    Message message = TypeConversion.sessionToMessage(sessionLog.getType(), sessionLog.getContent());
                    if (message != null) {
                        messages.add(message);
                    }
                }
                add(conversationId, messages);
            }
            return messages;
        }
        List<Message> messages = new ArrayList<>(range.size());
        for (String json : range) {
            JSONObject obj = JSONUtil.parseObj(json);
            String type = obj.getStr("messageType");
            String content = obj.getStr("content");
            Message message = TypeConversion.sessionToMessage(
                    SessionLog.Type.valueOf(type.toUpperCase()), content);
            if (message != null) {
                messages.add(message);
            }
        }
        return messages;
    }

    @Override
    public void clear(String conversationId) {
        String key = KeyUtils.redisKeyUtils(this.key, conversationId);
        redisTemplate.delete(key);
    }
}
