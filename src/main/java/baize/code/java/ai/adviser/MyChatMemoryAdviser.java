package baize.code.java.ai.adviser;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Builder
public class MyChatMemoryAdviser implements BaseChatMemoryAdvisor {
    private final ChatMemory chatMemory;
    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        //在大模型执行之前获取redis中的数据，如果没有从数据库获取最新的10条然后保存到redis中，拿到数据在和用户提示词拼接。
        Map<String, Object> context = chatClientRequest.context();
        String chatMemoryId = getConversationId(context, "default");
        if("default".equals(chatMemoryId)){
            throw new RuntimeException("chatMemoryId is null");
        }
        List<Message> messages = chatMemory.get(chatMemoryId);

        List<Message> processedMessages = new ArrayList<>(messages);
        processedMessages.addAll(chatClientRequest.prompt().getInstructions());


        // 3. Create a new request with the advised messages.
        ChatClientRequest processedChatClientRequest = chatClientRequest.mutate()
                .prompt(chatClientRequest.prompt().mutate().messages(processedMessages).build())
                .build();

        // 4. Add the new user message to the conversation memory.
        UserMessage userMessage = processedChatClientRequest.prompt().getUserMessage();
        this.chatMemory.add(chatMemoryId, userMessage);

        //根据会话id获取上下文信息

        return processedChatClientRequest;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        return null;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
