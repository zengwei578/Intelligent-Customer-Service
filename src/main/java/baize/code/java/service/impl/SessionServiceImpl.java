package baize.code.java.service.impl;

import baize.code.java.entity.Role;
import baize.code.java.service.RoleService;
import baize.code.java.utils.KeyUtils;
import baize.code.java.utils.SessionFind;
import baize.code.java.websocket.endpoint.UserServiceEndpoint;
import baize.code.java.websocket.endpoint.WebSocketEndpoint;
import baize.code.java.websocket.message.ChatMessage;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import baize.code.java.code.ResultCode;
import baize.code.java.common.Result;
import baize.code.java.entity.Session;
import baize.code.java.mapper.SessionMapper;
import baize.code.java.service.SessionService;
import jakarta.websocket.EncodeException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SessionServiceImpl extends ServiceImpl<SessionMapper, Session> implements SessionService {
    @Value("${session.key}")
    private String key;
    @Value("${session.expiration-duration}")
    private Integer timeout;

    private final RedisTemplate redisTemplate;

    private  final RoleService roleService;

    private final SessionFind sessionFind;

    @Override
    public Result<List<Session>> userGetLastSessionList(Integer userId) {
        return Result.success(
                ResultCode.GET_SUCCESS,
                lambdaQuery()
                        .eq(Session::getUserId, userId)
                        .orderByDesc(Session::getTimestamp)
                        .list()
        );
    }

    @Override
    public Result<List<Session>> ctGetLastSessionList(Integer ctId) {
        return Result.success(
                ResultCode.GET_SUCCESS,
                lambdaQuery()
                        .eq(Session::getCtId, ctId)
                        .orderByDesc(Session::getTimestamp)
                        .list()
        );
    }

    @Override
    public Session find(ChatMessage message, Integer userId, WebSocketEndpoint webSocketEndpoint) throws EncodeException, IOException {
        Integer ctId=message.getCtId();
        Integer sessionId=message.getSessionId();
        //判断是否有session
        if(!Objects.isNull(sessionId)){
            return sessionFind.getSessionById(sessionId);
        }
        //如果商品id或者商户id为空直接抛出异常
        if(Objects.isNull(message.getGoodsId())||Objects.isNull(ctId)){
            throw new RuntimeException("商品id或者商户id为空");
        }
        Session session = Session.builder()
                .userId(userId)
                .ctId(ctId)
                .goodsId(message.getGoodsId())
                .build();
        //判定商户是否设定了智能客服
        Role role=roleService.getRoleByCtId(ctId);
        if(ObjectUtil.isEmpty(role)){
            session.setConversationStatus(Session.ConversationStatus.HUMAN);
        }else {
            session.setConversationStatus(Session.ConversationStatus.AI);
        }

        //保存会话到数据库
        save(session);
        //保存会话到redis
        redisTemplate.opsForValue().set(KeyUtils.redisKeyUtils(key,session.getId()), JSONUtil.toJsonStr(session),timeout, TimeUnit.MINUTES);

        // 将session的部分信息返回给前端
        webSocketEndpoint.sendMessage(ChatMessage.builder()
                .sessionId(session.getId())
                .state(ChatMessage.State.SURE) //确认建立回话
                .build()
        );
        message.setSessionId(session.getId());
        return session;
    }
}