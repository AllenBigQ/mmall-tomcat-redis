package com.mmall.service;

import com.mmall.common.ServerResponse;
import com.mmall.pojo.User;

/**
 * Created by Allen
 */
public interface IUserService {
    //登陆
    ServerResponse<User> login(String username, String password);
    //注册
    ServerResponse<String> register(User user);
    //检查用户名或者邮箱是否有效
    ServerResponse<String> checkValid(String str,String type);
    //找回密码的问题
    ServerResponse selectQuestion(String username);
    //检查答案
    ServerResponse<String> checkAnswer(String username,String question,String answer);
    //忘记密码中的重设密码
    ServerResponse<String>forgetResetPassword(String username,String passwordNew,String forgetToken);
    //登陆状态中的重设密码
    ServerResponse<String> resetPassword(String passwordOld,String passwordNew,User user);
    //更新个人信息
    ServerResponse<User> updateInformation(User user);
    //得到用户信息
    ServerResponse<User>getInformation(Integer userId);
    ServerResponse checkAdminRole(User user);
}
