package com.jd.platform.hotkey.dashboard.service.impl;

import com.jd.platform.hotkey.dashboard.service.UserService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.github.pagehelper.util.StringUtil;
import com.jd.platform.hotkey.common.configcenter.IConfigCenter;
import com.jd.platform.hotkey.dashboard.common.domain.req.PageReq;
import com.jd.platform.hotkey.dashboard.common.domain.req.SearchReq;
import com.jd.platform.hotkey.dashboard.common.eunm.ResultEnum;
import com.jd.platform.hotkey.dashboard.common.ex.BizException;
import com.jd.platform.hotkey.dashboard.mapper.UserMapper;
import com.jd.platform.hotkey.dashboard.model.User;
import com.jd.platform.hotkey.dashboard.util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @ProjectName: hotkey
 * @ClassName: UserServiceImpl
 * @Description: TODO(一句话描述该类的功能)
 * @Author: liyunfeng31
 * @Date: 2020/4/16 20:37
 */
@Service
public class UserServiceImpl implements UserService {

    @Value("${erp.defaultPwd:123}")
    private String defaultPwd;
    @Resource
    private UserMapper userMapper;
    @Resource
    private IConfigCenter configCenter;

    @Override
    public User findByNameAndPwd(User user) {
        user.setPwd(DigestUtils.md5DigestAsHex(user.getPwd().getBytes()));
        return userMapper.findByNameAndPwd(user);
    }

    @Override
    public int insertUser(User user) {
        String name = user.getUserName();
        String phone = user.getPhone();
        String app = user.getAppName();
        List<User> users = userMapper.listUser(null);
        Set<String> set = new HashSet<>();
        for (User u : users) {
            set.add(u.getAppName());
            if(u.getUserName().equals(name) || u.getPhone().equals(phone)){
                throw new BizException(ResultEnum.CONFLICT_ERROR);
            }
        }
        if(StringUtil.isNotEmpty(app) && !set.contains(app)){
            this.initApp(app);
        }
        user.setCreateTime(new Date());
        user.setPwd(DigestUtils.md5DigestAsHex(user.getPwd().getBytes()));
        return userMapper.insertSelective(user);
    }

    @Override
    public int insertUserByErp(User user) {
        User userParam = new User();
        userParam.setUserName(user.getUserName());
        List<User> users = userMapper.selectHkUserList(userParam);
        if(users.size() == 0){
            user.setCreateTime(new Date());
            user.setPwd(DigestUtils.md5DigestAsHex(defaultPwd.getBytes()));
            int ret = userMapper.insertSelective(user);
            System.out.println(user.getId());
            return ret;
        }
        return 0;
    }

    @Override
    public Cookie loginErpUser(User user){
        User userParam = new User();
        userParam.setUserName(user.getUserName());
        List<User> users = userMapper.selectHkUserList(userParam);
        if(users.size() == 0){
            user.setCreateTime(new Date());
            user.setPwd(DigestUtils.md5DigestAsHex(defaultPwd.getBytes()));
            userMapper.insertSelective(user);
            String token = JwtTokenUtil.createJWT(user.getId(), user.getUserName(), "", user.getAppName(), user.getNickName());
            Cookie cookie = new Cookie("token", JwtTokenUtil.TOKEN_PREFIX + token);
            cookie.setMaxAge(3600*24*7);
            //cookie.setDomain("localhost");
            cookie.setPath("/");
            return cookie;
        }else{
            user =users.get(0);
            String token = JwtTokenUtil.createJWT(user.getId(), user.getUserName(), "", user.getAppName(), user.getNickName());
            Cookie cookie = new Cookie("token", JwtTokenUtil.TOKEN_PREFIX + token);
            cookie.setMaxAge(3600*24*7);
            //cookie.setDomain("localhost");
            cookie.setPath("/");
            return cookie;
        }
    }

    @Override
    public int deleteByPrimaryKey(int id) {
        return userMapper.deleteByPrimaryKey(id);
    }

    @Override
    public User selectByPrimaryKey(int id) {
        return userMapper.selectByPrimaryKey(id);
    }

    @Override
    public User selectByUserName(String userName) {
        return userMapper.selectByUserName(userName);
    }

    @Override
    public int updateUser(User user) {
        if(!StringUtils.isEmpty(user.getPwd())){
            user.setPwd(DigestUtils.md5DigestAsHex(user.getPwd().getBytes()));
        };
        return userMapper.updateByPk(user);
    }

    @Override
    public List<String> listApp() {
        return userMapper.listApp();
    }


    @Override
    public int initApp(String app) {
        return 1;
    }

    @Override
    public PageInfo<User> pageUser(PageReq param, SearchReq dto) {
        PageHelper.startPage(param.getPageNum(),param.getPageSize());
        List<User> users = userMapper.listUser(dto);
        return  new PageInfo<>(users);
    }


}
