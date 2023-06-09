package com.resume.position.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RedisData implements Serializable{
    private Object data;
    // 过期时间单位为 s
    private long expireTime;
}
