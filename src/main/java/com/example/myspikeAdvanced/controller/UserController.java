package com.example.myspikeAdvanced.controller;

import com.alibaba.druid.util.StringUtils;
import com.example.myspikeAdvanced.controller.viewObject.UserVO;
import com.example.myspikeAdvanced.error.BusinessException;
import com.example.myspikeAdvanced.error.EmBusinessError;
import com.example.myspikeAdvanced.response.CommonResultType;
import com.example.myspikeAdvanced.service.UserService;
import com.example.myspikeAdvanced.service.model.UserModel;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import sun.misc.BASE64Encoder;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;


/**
 * @author wangzhe
 * @version 1.0
 * @ClassName DemoController
 * @create 2021-08-03 21:49
 * @description 后端允许授信, 允许任意路径
 */
@RestController
@RequestMapping("/user")
@CrossOrigin(allowCredentials = "true", allowedHeaders = "*")
public class UserController extends BaseController {

    @Autowired
    private UserService userService;

    @Autowired
    private HttpServletRequest httpServletRequest;

    /**
     * 用户注册接口
     *
     * @param telephone
     * @param otpCode
     * @param name
     * @param gender
     * @param age
     * @param password  密码
     * @return com.example.myspikeAdvanced.response.CommonResultType
     * @Date 21:03 2021/8/4
     **/
    @PostMapping(value = "/register", consumes = {CONTENT_TYPE_FORMED})
    public CommonResultType register(@RequestParam("telephone") String telephone,
                                     @RequestParam("otpCode") String otpCode,
                                     @RequestParam("name") String name,
                                     @RequestParam("gender") Byte gender,
                                     @RequestParam("age") Integer age,
                                     @RequestParam("password") String password) throws BusinessException, NoSuchAlgorithmException {
        //验证手机号和对应的otpCode相符合
        //因为我们在获取验证码的方法中把验证码存到了session中,所以这里我们通过session再取出来
        //正常的业务逻辑肯定不是这样的,都是从redis中取,因为redis键值对形式存取方便,而且有值过期,适合验证码
        String inSessionOtpCode = (String) this.httpServletRequest.getSession().getAttribute(telephone);
        if (!StringUtils.equals(otpCode, inSessionOtpCode)) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "短信验证码错误");
        }
        //用户的注册流程
        UserModel userModel = new UserModel();
        userModel.setAge(age);
        userModel.setGender(gender);
        userModel.setName(name);
        userModel.setTelephone(telephone);
        userModel.setRegisterMode("byPhone");
        userModel.setEncryptPassword(this.EncodeByMd5(password));

        userService.register(userModel);
        return CommonResultType.create(null);
    }

    public String EncodeByMd5(String str) throws NoSuchAlgorithmException {
        //确定计算方式
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        BASE64Encoder base64 = new BASE64Encoder();
        //加密字符串
        String newStr = base64.encode(md5.digest(str.getBytes(StandardCharsets.UTF_8)));
        return newStr;
    }
    /**
     * 用来测试的
     * @Date 11:16 2021/8/9
     * @param userId
     * @return  com.example.myspikeAdvanced.response.CommonResultType
     **/
    @RequestMapping("/get/{userId}")
    public CommonResultType home(@PathVariable Integer userId) throws BusinessException {
        //调用service服务获取对应id的用户对象并返回给前端
        UserModel userModel = userService.getUserById(userId);
        if (userModel == null) {
            //会抛到Tomcat容器层,而Tomcat无法处理,只能抛出500错误,所以要定义ExceptionHandler
            //测试未知错误
            //userModel.setEncryptPassword("123");
            throw new BusinessException(EmBusinessError.USER_NOT_EXIST);
        }
        //将核心领域模型用户对象转化为可供UI使用的viewObject
        UserVO userVO = convertFromModel(userModel);
        //返回状态和用户信息
        return CommonResultType.create(userVO);
    }

    private UserVO convertFromModel(UserModel userModel) {
        if (userModel == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(userModel, userVO);
        return userVO;
    }

    /**
     * 用户获取短信验证码
     *
     * @param telephone 用户手机号
     * @return com.example.myspikeAdvanced.response.CommonResultType
     * @Date 15:18 2021/8/4
     **/
    @PostMapping(value = "/getotp", consumes = {CONTENT_TYPE_FORMED})
    public CommonResultType getOtp(@RequestParam(value = "telephone") String telephone) {
        //需要按照一定规则生成验证码
        Random random = new Random();
        //随即生成一个0-99999的数字
        int randomInt = random.nextInt(99999);
        randomInt += 1000;
        String otpCode = String.valueOf(randomInt);

        //将OTP验证码同用户手机号关联,因为这里没有设计分布式,所以我们
        //用httpSession的方式绑定手机号和otpCode
        httpServletRequest.getSession().setAttribute(telephone, otpCode);

        //将OTP验证码通过短信通道发送给用户,这里因为需要第三方服务就省略了,直接控制台输出
        System.out.println("telephone = " + telephone + " && otpCode = " + otpCode);
        return CommonResultType.create(null);
    }
    /**
     * 用户登录界面
     * @Date 11:56 2021/8/5
     * @param telephone
     * @param password
     * @return  com.example.myspikeAdvanced.response.CommonResultType
     **/
    @PostMapping(value = "/login", consumes = {CONTENT_TYPE_FORMED})
    public CommonResultType login(@RequestParam("telephone") String telephone,
                                  @RequestParam("password") String password) throws BusinessException, NoSuchAlgorithmException {
        //入参校验
        if (StringUtils.isEmpty(telephone)) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "请输入用户名");
        } else if (StringUtils.isEmpty(password)) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "请输入密码");
        }
        //用户登录服务,用来校验用户登录是否合法
        UserModel userModel = userService.validateLogin(telephone, this.EncodeByMd5(password));

        //将登录凭证加入到用户登录成功的session内,在这里假设用户是单点session登录
        this.httpServletRequest.getSession().setAttribute("IS_LOGIN", true);
        this.httpServletRequest.getSession().setAttribute("LOGIN_USER", userModel);
        return CommonResultType.create(null);
    }
}
