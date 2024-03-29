package com.resume.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.resume.auth.dto.LoginDTO;
import com.resume.auth.mapstruct.UserMapstruct;
import com.resume.auth.utils.SM3Util;
import com.resume.dubbo.domian.MemberDTO;
import com.resume.auth.mapper.RoleMapper;
import com.resume.auth.mapper.UserMapper;
import com.resume.auth.pojo.Company;
import com.resume.auth.pojo.Operation;
import com.resume.auth.pojo.User;
import com.resume.auth.utils.RedisUtil;
import com.resume.base.utils.Constant;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/*
 *@filename: UserService
 *@author: lyh
 *@date:2023/6/11 23:20
 *@version 1.0
 *@description TODO
 */
@Service
@Getter
@Slf4j
public class UserService extends ServiceImpl<UserMapper, User> {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private CompanyService companyService;
    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private RedisUtil redisUtil;

    public User login(User user) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUserEmail, user.getUserEmail()).eq(User::getIsDeleted, 0);
        return getOne(queryWrapper);
    }

    public boolean checkUserEmailIsExist(String userEmail) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        //防止重复注册
        queryWrapper.eq(User::getUserEmail, userEmail).eq(User::getIsDeleted, 0);
        User result = getOne(queryWrapper);
        return result == null;
    }

    public LoginDTO getPermissions(Long userId) {
        LoginDTO loginDTO = new LoginDTO();
        List<Operation> userPermissions = userMapper.getUserPermissions(userId);
        if (userPermissions != null && userPermissions.size() > 0) {
            //将用户对应的权限传给前端，控制具体按钮是否存在
            loginDTO.setPermissionsList(userPermissions.parallelStream().collect(Collectors.toMap(Operation::getOperationCode, operation -> true)));
            //将用户对应的权限缓存，给后端网关使用的:Method+InterfaceUrl
            Set<String> operation = userPermissions.parallelStream().map((resource -> resource.getMethod() +":"+ resource.getInterfaceUrl())).collect(Collectors.toSet());
            //权限存储时间跟access_token一样长
            redisUtil.sSetAndTime(Constant.USER_KEY + userId, Constant.USER_TTL, operation);
        }
        //菜单权限
        loginDTO.setMenusList(userMapper.getMenus(userId));
        return loginDTO;
    }

    public boolean editPersonalMessage(User user) {
        LambdaUpdateWrapper<User> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.eq(User::getPkUserId, user.getPkUserId());
        return update(user, lambdaUpdateWrapper);
    }

    public Company getCompanyMessage(Long companyId) {
        return companyService.getCompanyMapper().getCompanyMessage(companyId);
    }

    public boolean editCompanyMessage(Company company) {
        LambdaUpdateWrapper<Company> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.eq(Company::getPkCompanyId, company.getPkCompanyId());
        return companyService.update(company, lambdaUpdateWrapper);
    }

    public void addTeamRole(Long userId,String role) {
        switch (role) {
            case Constant.COMPANY_ADMIN:
                roleMapper.addCompanyAdmin(userId);
                break;
            case Constant.HR:
                roleMapper.addCompanyHr(userId);
                break;
            case Constant.INTERVIEWER:
                roleMapper.addCompanyInterviewer(userId);
                break;
        }
    }

    public List<MemberDTO> selectTeamMembers(Long companyId) {
        List<MemberDTO> teamMembers = roleMapper.selectTeamAdmin(companyId);//admin
        List<MemberDTO> otherMembers = roleMapper.selectTeamOtherMember(companyId);
        otherMembers.sort(new Comparator<MemberDTO>() {
            @Override
            public int compare(MemberDTO o1, MemberDTO o2) {
                String role1 = o1.getRoleName();
                String role2 = o2.getRoleName();
                return role1.compareTo(role2);
            }
        });
        teamMembers.addAll(otherMembers);
        return teamMembers;
    }
    public boolean addTeamMembers(MemberDTO memberDTO,Long companyId) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUserEmail, memberDTO.getUserEmail());
        User result = getOne(queryWrapper);
        if(result==null){
            User user= UserMapstruct.INSTANCT.conver(memberDTO);
            //所属公司
            user.setCompanyId(companyId);
            //加密
            user.setPassword(SM3Util.encryptPassword(user.getPassword()));
            save(user);
            //赋予角色
            addTeamRole(user.getPkUserId(),memberDTO.getRoleName());
            return true;
        }else{
                return false;
        }
    }
    public boolean deleteTeamMembers(Long userId) {
        String role=roleMapper.selectUserRole(userId);
        //公司管理员 无权删除
        if(Constant.COMPANY_ADMIN.equals(role))return false;
        roleMapper.deleteUserPermissions(userId);
        roleMapper.deleteUser(userId);
        return true;
    }
}
