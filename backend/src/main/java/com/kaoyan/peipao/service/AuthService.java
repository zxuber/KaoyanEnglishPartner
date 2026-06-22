package com.kaoyan.peipao.service;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kaoyan.peipao.dto.response.LoginResponse;
import com.kaoyan.peipao.entity.User;
import com.kaoyan.peipao.mapper.UserMapper;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final WxMaService wxMaService;
    private final UserMapper userMapper;
    private final SecretKey jwtSecretKey;

    @Value("${app.jwt.expire-days}")
    private long expireDays;

    public LoginResponse wxLogin(String code) {
        try {
            WxMaJscode2SessionResult session = wxMaService.getUserService().getSessionInfo(code);
            String openid = session.getOpenid();

            User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getOpenid, openid)
                    .last("limit 1"));

            if (user == null) {
                user = new User();
                user.setOpenid(openid);
                user.setSessionId(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
                user.setCurrentPhase("phase1");
                user.setPhaseStartDay(1);
                user.setTotalCheckins(0);
                userMapper.insert(user);
                log.info("Created wx user: id={}, openid={}", user.getId(), openid);
            }

            return LoginResponse.builder()
                    .token(buildToken(user))
                    .userId(user.getId())
                    .onboardingDone(user.getPlanJson() != null && !user.getPlanJson().isBlank())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("微信登录失败: " + e.getMessage(), e);
        }
    }

    private String buildToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("openid", user.getOpenid())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expireDays, ChronoUnit.DAYS)))
                .signWith(jwtSecretKey)
                .compact();
    }
}
