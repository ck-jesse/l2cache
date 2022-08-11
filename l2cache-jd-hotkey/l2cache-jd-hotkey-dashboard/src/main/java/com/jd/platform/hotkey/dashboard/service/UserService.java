package com.jd.platform.hotkey.dashboard.service;

import com.github.pagehelper.PageInfo;
import com.jd.platform.hotkey.dashboard.common.domain.req.PageReq;
import com.jd.platform.hotkey.dashboard.common.domain.req.SearchReq;
import com.jd.platform.hotkey.dashboard.model.User;

import javax.servlet.http.Cookie;
import java.util.List;

/**
 * @ProjectName: hotkey
 * @ClassName: UserService
 * @Description: TODO(一句话描述该类的功能)
 * @Author: liyunfeng31
 * @Date: 2020/4/16 20:37
 */
public interface UserService {

    PageInfo<User> pageUser(PageReq page, SearchReq dto);

    User findByNameAndPwd(User user);

    int insertUser(User user);

    int insertUserByErp(User user);

    Cookie loginErpUser(User user);

    int deleteByPrimaryKey(int id);

    User selectByPrimaryKey(int id);

    User selectByUserName(String userName);

    int updateUser(User user);

    List<String> listApp();

    /**
     * 初始化APP
     * @param app
     * @return
     */
    int initApp(String app);
}
