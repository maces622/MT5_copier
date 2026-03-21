package com.zyc.copier_v0.modules.copy.followerexec.web;

import com.zyc.copier_v0.modules.copy.followerexec.api.FollowerExecSessionResponse;
import com.zyc.copier_v0.modules.copy.followerexec.service.FollowerExecWebSocketService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/follower-exec")
public class FollowerExecController {

    private final FollowerExecWebSocketService followerExecWebSocketService;

    public FollowerExecController(FollowerExecWebSocketService followerExecWebSocketService) {
        this.followerExecWebSocketService = followerExecWebSocketService;
    }

    @GetMapping("/sessions")
    public List<FollowerExecSessionResponse> listSessions() {
        return followerExecWebSocketService.listSessions();
    }
}
