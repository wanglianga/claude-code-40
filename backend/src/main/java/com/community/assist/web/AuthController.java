package com.community.assist.web;

import com.community.assist.model.User;
import com.community.assist.repo.UserRepository;
import com.community.assist.service.BizException;
import com.community.assist.service.CurrentUser;
import com.community.assist.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final CurrentUser currentUser;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil, CurrentUser currentUser) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.currentUser = currentUser;
    }

    public record LoginReq(String username, String password) {
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginReq req) {
        if (req.username() == null || req.password() == null) {
            throw BizException.badRequest("请输入用户名和密码");
        }
        User u = userRepository.findByUsername(req.username())
                .orElseThrow(() -> BizException.badRequest("用户名或密码错误"));
        if (!passwordEncoder.matches(req.password(), u.getPassword())) {
            throw BizException.badRequest("用户名或密码错误");
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("token", jwtUtil.generate(u.getId(), u.getRole().name()));
        res.put("user", view(u));
        return res;
    }

    @GetMapping("/me")
    public Map<String, Object> me() {
        return view(currentUser.get());
    }

    static Map<String, Object> view(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        m.put("name", u.getName());
        m.put("role", u.getRole().name());
        m.put("phone", u.getPhone());
        m.put("elderlyId", u.getElderlyId());
        return m;
    }
}
