package com.community.assist.service;

import com.community.assist.model.Enums.Role;
import com.community.assist.model.User;
import com.community.assist.repo.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * 当前登录用户工具：从 SecurityContext 取 userId 并加载用户。
 */
@Service
public class CurrentUser {

    private final UserRepository userRepository;

    public CurrentUser(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User get() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long userId)) {
            throw new BizException(401, "未登录或登录已过期");
        }
        return userRepository.findById(userId).orElseThrow(() -> new BizException(401, "账号不存在"));
    }

    public User requireAny(Role... roles) {
        User u = get();
        boolean ok = Arrays.asList(roles).contains(u.getRole());
        if (!ok) {
            throw BizException.forbidden("当前角色无权执行该操作");
        }
        return u;
    }

    /** 家属只能操作自己关联老人的数据 */
    public void checkFamilyScope(User u, Long elderlyId) {
        if (u.getRole() == Role.FAMILY && (u.getElderlyId() == null || !u.getElderlyId().equals(elderlyId))) {
            throw BizException.forbidden("只能查看/操作本人关联老人的数据");
        }
    }
}
