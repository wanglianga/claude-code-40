package com.community.assist.web;

import com.community.assist.model.Enums.Role;
import com.community.assist.model.User;
import com.community.assist.repo.UserRepository;
import com.community.assist.service.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户查询（如：指派评估师下拉）。
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepo;
    private final CurrentUser currentUser;

    public UserController(UserRepository userRepo, CurrentUser currentUser) {
        this.userRepo = userRepo;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(required = false) Role role) {
        currentUser.get();
        List<Map<String, Object>> res = new ArrayList<>();
        for (User u : userRepo.findAll()) {
            if (role != null && u.getRole() != role) {
                continue;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("name", u.getName());
            m.put("role", u.getRole().name());
            res.add(m);
        }
        return res;
    }
}
