package com.teamdashboard.controller;

import com.teamdashboard.dto.IssueCommentRequestDTO;
import com.teamdashboard.dto.IssueCommentResponseDTO;
import com.teamdashboard.dto.TeamIssueRequestDTO;
import com.teamdashboard.dto.TeamIssueResponseDTO;
import com.teamdashboard.entity.IssueComment;
import com.teamdashboard.entity.TeamIssue;
import com.teamdashboard.entity.User;
import com.teamdashboard.repository.UserRepository;
import com.teamdashboard.service.TeamIssueService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Profile;

/**
 * 困りごと共有機能のREST APIコントローラー
 */
// @RestController
// @RequestMapping("/api/team-issues")
// @CrossOrigin(origins = "*")
// @Profile("!dynamodb")
class TeamIssueController_Disabled {

    private final TeamIssueService teamIssueService;
    private final UserRepository userRepository;

    // @Autowired
    public TeamIssueController_Disabled(TeamIssueService teamIssueService, UserRepository userRepository) {
        this.teamIssueService = teamIssueService;
        this.userRepository = userRepository;
    }

    /**
     * 全ての困りごとを取得
     * GET /api/team-issues
     * @param status 困りごとのステータス（任意）
     * @return 困りごとのリスト
     */
    @GetMapping
    public ResponseEntity<List<TeamIssueResponseDTO>> getAllIssues(
            @RequestParam(required = false) String status) {
        try {
            List<TeamIssue> issues;
            
            if (status != null && !status.trim().isEmpty()) {
                // ステータス指定がある場合
                switch (status.toUpperCase()) {
                    case "OPEN":
                        issues = teamIssueService.getOpenIssues();
                        break;
                    case "RESOLVED":
                        issues = teamIssueService.getResolvedIssues();
                        break;
                    default:
                        return ResponseEntity.badRequest().build();
                }
            } else {
                // 全ての困りごとを取得
                issues = teamIssueService.getAllIssues();
            }
            
            List<TeamIssueResponseDTO> response = issues.stream()
                    .map(issue -> {
                        long commentCount = teamIssueService.getCommentCount(issue.getId());
                        return TeamIssueResponseDTO.fromEntity(issue, commentCount);
                    })
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 指定されたIDの困りごとを取得
     * GET /api/team-issues/{id}
     * @param id 困りごとID
     * @return 困りごと
     */
    @GetMapping("/{id}")
    public ResponseEntity<TeamIssueResponseDTO> getIssueById(@PathVariable Long id) {
        try {
            Optional<TeamIssue> issue = teamIssueService.getIssueById(id);
            
            if (issue.isPresent()) {
                long commentCount = teamIssueService.getCommentCount(id);
                TeamIssueResponseDTO response = TeamIssueResponseDTO.fromEntity(issue.get(), commentCount);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 新しい困りごとを投稿
     * POST /api/team-issues
     * @param request 困りごと投稿リクエスト
     * @param principal 認証されたユーザー情報
     * @return 作成された困りごと
     */
    @PostMapping
    public ResponseEntity<TeamIssueResponseDTO> createIssue(
            @Valid @RequestBody TeamIssueRequestDTO request,
            Principal principal) {
        try {
            // 現在のユーザーを取得
            Long userId = getCurrentUserId(principal);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            TeamIssue createdIssue = teamIssueService.createIssue(userId, request.getContent());
            TeamIssueResponseDTO response = TeamIssueResponseDTO.fromEntity(createdIssue, 0);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 困りごとを解決済みにマーク
     * PUT /api/team-issues/{id}/resolve
     * @param id 困りごとID
     * @return 更新された困りごと
     */
    @PutMapping("/{id}/resolve")
    public ResponseEntity<TeamIssueResponseDTO> resolveIssue(@PathVariable Long id) {
        try {
            TeamIssue resolvedIssue = teamIssueService.resolveIssue(id);
            long commentCount = teamIssueService.getCommentCount(id);
            TeamIssueResponseDTO response = TeamIssueResponseDTO.fromEntity(resolvedIssue, commentCount);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 困りごとを未解決に戻す
     * PUT /api/team-issues/{id}/reopen
     * @param id 困りごとID
     * @return 更新された困りごと
     */
    @PutMapping("/{id}/reopen")
    public ResponseEntity<TeamIssueResponseDTO> reopenIssue(@PathVariable Long id) {
        try {
            TeamIssue reopenedIssue = teamIssueService.reopenIssue(id);
            long commentCount = teamIssueService.getCommentCount(id);
            TeamIssueResponseDTO response = TeamIssueResponseDTO.fromEntity(reopenedIssue, commentCount);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 指定された困りごとのコメントを取得
     * GET /api/team-issues/{id}/comments
     * @param id 困りごとID
     * @return コメントのリスト
     */
    @GetMapping("/{id}/comments")
    public ResponseEntity<List<IssueCommentResponseDTO>> getCommentsByIssueId(@PathVariable Long id) {
        try {
            // 困りごとの存在確認
            Optional<TeamIssue> issue = teamIssueService.getIssueById(id);
            if (issue.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            List<IssueComment> comments = teamIssueService.getCommentsByIssueId(id);
            List<IssueCommentResponseDTO> response = comments.stream()
                    .map(IssueCommentResponseDTO::fromEntity)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 困りごとにコメントを投稿
     * POST /api/team-issues/{id}/comments
     * @param id 困りごとID
     * @param request コメント投稿リクエスト
     * @param principal 認証されたユーザー情報
     * @return 作成されたコメント
     */
    @PostMapping("/{id}/comments")
    public ResponseEntity<IssueCommentResponseDTO> addComment(
            @PathVariable Long id,
            @Valid @RequestBody IssueCommentRequestDTO request,
            Principal principal) {
        try {
            // 現在のユーザーを取得
            Long userId = getCurrentUserId(principal);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            IssueComment createdComment = teamIssueService.addComment(id, userId, request.getContent());
            IssueCommentResponseDTO response = IssueCommentResponseDTO.fromEntity(createdComment);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 現在のユーザーのIDを取得
     * @param principal 認証されたユーザー情報
     * @return ユーザーID（認証されていない場合はnull）
     */
    private Long getCurrentUserId(Principal principal) {
        if (principal == null) {
            // 開発・テスト用のデフォルトユーザー
            Optional<User> testUser = userRepository.findByUsername("testuser");
            return testUser.map(User::getId).orElse(null);
        }
        
        String username = principal.getName();
        Optional<User> user = userRepository.findByUsername(username);
        return user.map(User::getId).orElse(null);
    }

    /**
     * エラーハンドリング用の例外ハンドラー
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        ErrorResponse error = new ErrorResponse("VALIDATION_ERROR", e.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException e) {
        ErrorResponse error = new ErrorResponse("STATE_ERROR", e.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e) {
        ErrorResponse error = new ErrorResponse("RUNTIME_ERROR", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * エラーレスポンス用のクラス
     */
    public static class ErrorResponse {
        private String error;
        private String message;

        public ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }

        // Getters and Setters
        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}