package com.kaoyan.peipao.config;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WxMiniappConfig {

    @Bean
    public WxMaService wxMaService(
            @Value("${wx.miniapp.appid}") String appid,
            @Value("${wx.miniapp.secret}") String secret
    ) {
        WxMaDefaultConfigImpl config = new WxMaDefaultConfigImpl();
        config.setAppid(appid);
        config.setSecret(secret);

        cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl service =
                new cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl();
        service.setWxMaConfig(config);
        return service;
    }
}
