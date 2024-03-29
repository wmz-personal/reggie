package com.example.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.common.R;
import com.example.pojo.User;
import com.example.service.UserService;
import com.example.utils.SMSUtils;
import com.example.utils.ValidateCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RequestMapping("/user")
@RestController
@Slf4j
public class UserController {
    @Value("${sms.signName}")
    private String signName;

    @Value("${sms.templateCode}")
    private String templateCode;

    @Autowired
    UserService userService;

    @Autowired
    private RedisTemplate redisTemplate;



    //手机验证码
    @PostMapping("/sendMsg")
    public R<String> sendMsg(@RequestBody Map<String, String> phoneMap, HttpSession session) {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        //从phoneMap中获取手机号
        String phoneNum = phoneMap.get("phone");
        //获取验证码
        String code = ValidateCodeUtils.generateValidateCode4String(6);
        //把验证码储存到redis中,设置生命长度为5分钟
        valueOperations.set(phoneNum,code,5, TimeUnit.MINUTES);
        log.info("验证码是{}",code);
        //把cade存到phoneNum 对应的session中
        //session.setAttribute(phoneNum, code);
        //短信发送
        //SMSUtils.sendMessage(signName,templateCode,phoneNum,code);

        return R.success("验证成功");
    }


    //登录
    @PostMapping("/login")
    public R<User> userLogin(@RequestBody Map<String, String> phoneMap,HttpSession session){
        ValueOperations valueOperations = redisTemplate.opsForValue();
        //用户输入的code,根据phone取值
        String number = phoneMap.get("phone");

        //根据手机号进行查询
        LambdaQueryWrapper<User> lqw=new LambdaQueryWrapper<>();
        lqw.eq(User::getPhone,number);
        User user = userService.getOne(lqw);
        if (user==null) {
            user = new User();
            user.setPhone(number);
            userService.save(user);
        }
        //根据手机号获取验证码
        String code = phoneMap.get("code");
        /*//再从session中获取code值
        String validCode = (String) session.getAttribute(number);*/
        //从redis中取值
        Object validCode = valueOperations.get(number);
        //进行比较
        if (!code.equals(validCode)) {
            return R.error("验证码错误");
        }

        session.setAttribute("userId",user.getId());

        //当登录成功时,清除redis中的手机号及其数据
        redisTemplate.delete(number);
        log.info("user.getId==>{}",user.getId());
        return R.success(user);
    }

    //退出
    @PostMapping("/loginout")
    public R<String> userLogout(HttpSession session){
        session.removeAttribute("userId");
        return R.success("成功退出");
    }


}
