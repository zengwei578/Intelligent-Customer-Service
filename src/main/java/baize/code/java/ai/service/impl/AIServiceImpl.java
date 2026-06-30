package baize.code.java.ai.service.impl;

import baize.code.java.ai.adviser.MyChatMemoryAdviser;
import baize.code.java.ai.memory.RedisChatMemory;
import baize.code.java.ai.service.AIService;
import baize.code.java.entity.Role;
import baize.code.java.entity.Session;
import baize.code.java.entity.SessionLog;
import baize.code.java.service.RoleService;
import baize.code.java.service.SessionService;
import baize.code.java.utils.KeyUtils;
import baize.code.java.utils.SessionFind;
import baize.code.java.websocket.endpoint.UserServiceEndpoint;
import baize.code.java.websocket.message.ChatMessage;
import jakarta.annotation.Resource;
import jakarta.websocket.EncodeException;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AIServiceImpl implements AIService {

    @Autowired
    private ChatClient chatClient;
    @Autowired
    private ResourceLoader resourceLoader;
    @Autowired
    private SessionService sessionService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RoleService roleService;
    @Resource
    private RedisChatMemory redisChatMemory;
    @Value("${session.key}")
    private String key;
    @Override
    public void HumanOperation(ChatMessage chatMessage, Session session){
        String content = chatClient.prompt()
                .system(resourceLoader.getResource("classpath:template/convert-to-manual-judgment-prompts.st"))
                .user(chatMessage.getMessage())
                .call()
                .content();
        //如果为负面情绪则转人工
        if(Boolean.parseBoolean(content)){
            session.setConversationStatus(Session.ConversationStatus.HUMAN);
            sessionService.updateById(session);
            //删除redis中的相关缓存
            redisTemplate.delete(KeyUtils.redisKeyUtils(key, session.getId()));
        }
    }

    public void chat(ChatMessage chatMessage,Integer ctId,Integer userId) throws IllegalAccessException {
        Role role = roleService.getRoleByCtId(ctId);
        PromptTemplate promptTemplate = PromptTemplate.builder()
                .renderer(StTemplateRenderer.builder()
                        .startDelimiterToken('<')
                        .endDelimiterToken('>')
                        .build()).resource(resourceLoader.getResource("classpath:template/customer-service-role.st")).build();

        Map<String,Object> hashMap=new HashMap<>();

        //将所有role的string类型的属性都映射到map中
        Class<? extends Role> aClass = role.getClass();
        for (Field declaredField : aClass.getDeclaredFields()) {
            if(declaredField.getGenericType().equals(String.class)){
                declaredField.setAccessible(true);
                hashMap.put(declaredField.getName(),declaredField.get(role));
            }
        }
        String template = promptTemplate.create(hashMap).getContents();
        Flux<String> stream = chatClient.prompt()
                .system(template)
                .user(chatMessage.getMessage())
                .advisors(MessageChatMemoryAdvisor
                        .builder(redisChatMemory)
                        .conversationId(chatMessage.getSessionId().toString())
                        .build())
                        .stream().content();


        stream.toIterable().forEach(
                s -> {
                    UserServiceEndpoint endPoint = UserServiceEndpoint.findEndPoint(userId);
                    try {
                        endPoint.sendMessage(ChatMessage
                                .builder()
                                .goodsId(chatMessage.getGoodsId())
                                .type(SessionLog.Type.ASSISTANT)
                                .sessionId(chatMessage.getSessionId())
                                .ctId(chatMessage.getCtId())
                                .message(s)
                                .build());
                    } catch (EncodeException e) {
                        throw new RuntimeException(e);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
        );

    }
}
