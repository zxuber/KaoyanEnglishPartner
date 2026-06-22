package com.kaoyan.peipao.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WxLoginRequest {

    @NotBlank(message = "微信登录 code 不能为空")
    private String code;
}
