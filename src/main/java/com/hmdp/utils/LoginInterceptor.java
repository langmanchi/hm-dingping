package com.hmdp.utils;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

//这个类的对象不是spring创建的，是手动创建的，利用构造函数注入
public class LoginInterceptor implements HandlerInterceptor {

    //为什么不通过注解注入
    private StringRedisTemplate stringredisTemplate;

    public LoginInterceptor(StringRedisTemplate stringredisTemplate) {
        this.stringredisTemplate = stringredisTemplate;
    }

    //前置拦截
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1 获取请求头中的token
        String token = request.getHeader("authorization");
        if (StrUtil.isBlank(token)){
            //不存在，拦截  返回401状态码
            response.setStatus(401);
            return false;
        }
        //2 基于TOKEN获取redis中的用户
        String key = RedisConstants.LOGIN_USER_KEY + token;
       Map<Object,Object> userMap =  stringredisTemplate.opsForHash()
               .entries(RedisConstants.LOGIN_USER_KEY + token);

        //3  判断用户是否存在
        if (userMap.isEmpty()){
            //4 不存在，拦截
            response.setStatus(401);
            return false;
        }
        //5将查询到的Hash数据转为UserDTO对象
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap,new UserDTO(),false);

        //6 存在，保存用户到ThreadLocal
        UserHolder.saveUser(userDTO);

        //7 刷新token有效期
       stringredisTemplate.expire(key,RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        //8 放行
       return true;
    }



    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
