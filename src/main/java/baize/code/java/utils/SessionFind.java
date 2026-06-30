package baize.code.java.utils;

import baize.code.java.entity.Session;
import baize.code.java.mapper.SessionMapper;
import baize.code.java.service.SessionService;
import baize.code.java.websocket.endpoint.CommercialTenantEndpoint;
import baize.code.java.websocket.endpoint.UserServiceEndpoint;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Component
public class SessionFind {
    @Value("${session.key}")
    String key;
    @Value("${session.expiration-duration}")
    Integer outTime;


    private final SessionMapper mapper;

    private  final RedisTemplate redisTemplate;


    public  Session getSessionById(Integer sessionId){
        Object andExpire = redisTemplate.opsForValue().get(KeyUtils.redisKeyUtils(key,sessionId));

        if(!ObjectUtil.isEmpty(andExpire)){
            return JSONUtil.toBean(andExpire.toString(),Session.class);
        }
        Session session = mapper.selectById(sessionId);
        if(session==null){
            throw new RuntimeException("会话不存在");
        }
        redisTemplate.opsForValue().set(KeyUtils.redisKeyUtils(key,sessionId),JSONUtil.toJsonStr(session),outTime,TimeUnit.MINUTES);

        return session;

    }

    public UserServiceEndpoint getUserEndpointById(Integer sessionId){
        Session session = getSessionById(sessionId);

        return UserServiceEndpoint.findEndPoint(session.getUserId());
    }

    public CommercialTenantEndpoint getCommercialTenantEndpointById(Integer sessionId){
        Session session = getSessionById(sessionId);

        return CommercialTenantEndpoint.findEndPoint(session.getCtId());
    }
}
