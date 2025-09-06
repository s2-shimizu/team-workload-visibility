package com.teamdashboard.service;

import com.teamdashboard.entity.IssueComment;
import com.teamdashboard.entity.IssueStatus;
import com.teamdashboard.entity.TeamIssue;
import com.teamdashboard.entity.User;
import com.teamdashboard.repository.IssueCommentRepository;
import com.teamdashboard.repository.TeamIssueRepository;
import com.teamdashboard.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TeamIssueServiceの単体テスト
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TeamIssueService テスト")
class TeamIssueServiceTest {

    @Mock
    private TeamIssueRepository teamIssueRepository;

    @Mock
    private IssueCommentRepository issueCommentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TeamIssueService teamIssueService;

    private User testUser1;
    private User testUser2;
    private TeamIssue openIssue;
    private TeamIssue resolvedIssue;
    private IssueComment comment1;
    private IssueComment comment2;

    @BeforeEach
    void setUp() {
        testUser1 = new User("user1", "password1", "user1@example.com", "ユーザー1");
        testUser1.setId(1L);
        
        testUser2 = new User("user2", "password2", "user2@example.com", "ユーザー2");
        testUser2.setId(2L);

        openIssue = new TeamIssue(testUser1, "データベース接続エラーが発生しています");
        openIssue.setId(1L);
        openIssue.setCreatedAt(LocalDateTime.now().minusHours(1));

        resolvedIssue = new TeamIssue(testUser2, "APIのレスポンスが遅い問題");
        resolvedIssue.setId(2L);
        resolvedIssue.setCreatedAt(LocalDateTime.now().minusHours(2));
        resolvedIssue.resolve();

        comment1 = new IssueComment(openIssue, testUser1, "設定ファイルを確認してください");
        comment1.setId(1L);
        comment1.setCreatedAt(LocalDateTime.now().minusMinutes(30));

        comment2 = new IssueComment(openIssue, testUser2, "データベースの接続設定が間違っているかもしれません");
        comment2.setId(2L);
        comment2.setCreatedAt(LocalDateTime.now().minusMinutes(15));
    }

    @Test
    @DisplayName("全困りごと取得")
    void getAllIssues() {
        // Given
        List<TeamIssue> expectedList = Arrays.asList(openIssue, resolvedIssue);
        when(teamIssueRepository.findAllWithUserOrderByCreatedAtDesc()).thenReturn(expectedList);

        // When
        List<TeamIssue> result = teamIssueService.getAllIssues();

        // Then
        assertEquals(2, result.size());
        assertEquals(openIssue.getId(), result.get(0).getId());
        assertEquals(resolvedIssue.getId(), result.get(1).getId());
        verify(teamIssueRepository).findAllWithUserOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("ステータス別困りごと取得")
    void getIssuesByStatus() {
        // Given
        List<TeamIssue> openIssues = Arrays.asList(openIssue);
        List<TeamIssue> resolvedIssues = Arrays.asList(resolvedIssue);
        
        when(teamIssueRepository.findByStatusWithUserOrderByCreatedAtDesc(IssueStatus.OPEN))
            .thenReturn(openIssues);
        when(teamIssueRepository.findByStatusWithUserOrderByCreatedAtDesc(IssueStatus.RESOLVED))
            .thenReturn(resolvedIssues);

        // When
        List<TeamIssue> openResult = teamIssueService.getIssuesByStatus(IssueStatus.OPEN);
        List<TeamIssue> resolvedResult = teamIssueService.getIssuesByStatus(IssueStatus.RESOLVED);

        // Then
        assertEquals(1, openResult.size());
        assertEquals(openIssue.getId(), openResult.get(0).getId());
        assertEquals(1, resolvedResult.size());
        assertEquals(resolvedIssue.getId(), resolvedResult.get(0).getId());
        
        verify(teamIssueRepository).findByStatusWithUserOrderByCreatedAtDesc(IssueStatus.OPEN);
        verify(teamIssueRepository).findByStatusWithUserOrderByCreatedAtDesc(IssueStatus.RESOLVED);
    }

    @Test
    @DisplayName("未解決困りごと取得")
    void getOpenIssues() {
        // Given
        List<TeamIssue> openIssues = Arrays.asList(openIssue);
        when(teamIssueRepository.findByStatusWithUserOrderByCreatedAtDesc(IssueStatus.OPEN))
            .thenReturn(openIssues);

        // When
        List<TeamIssue> result = teamIssueService.getOpenIssues();

        // Then
        assertEquals(1, result.size());
        assertEquals(openIssue.getId(), result.get(0).getId());
        assertEquals(IssueStatus.OPEN, result.get(0).getStatus());
        verify(teamIssueRepository).findByStatusWithUserOrderByCreatedAtDesc(IssueStatus.OPEN);
    }

    @Test
    @DisplayName("解決済み困りごと取得")
    void getResolvedIssues() {
        // Given
        List<TeamIssue> resolvedIssues = Arrays.asList(resolvedIssue);
        when(teamIssueRepository.findByStatusWithUserOrderByCreatedAtDesc(IssueStatus.RESOLVED))
            .thenReturn(resolvedIssues);

        // When
        List<TeamIssue> result = teamIssueService.getResolvedIssues();

        // Then
        assertEquals(1, result.size());
        assertEquals(resolvedIssue.getId(), result.get(0).getId());
        assertEquals(IssueStatus.RESOLVED, result.get(0).getStatus());
        verify(teamIssueRepository).findByStatusWithUserOrderByCreatedAtDesc(IssueStatus.RESOLVED);
    }

    @Test
    @DisplayName("ID別困りごと取得 - 存在する場合")
    void getIssueById_Found() {
        // Given
        when(teamIssueRepository.findById(1L)).thenReturn(Optional.of(openIssue));

        // When
        Optional<TeamIssue> result = teamIssueService.getIssueById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(openIssue.getId(), result.get().getId());
        verify(teamIssueRepository).findById(1L);
    }

    @Test
    @DisplayName("ID別困りごと取得 - 存在しない場合")
    void getIssueById_NotFound() {
        // Given
        when(teamIssueRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<TeamIssue> result = teamIssueService.getIssueById(999L);

        // Then
        assertFalse(result.isPresent());
        verify(teamIssueRepository).findById(999L);
    }

    @Test
    @DisplayName("ユーザーID別困りごと取得")
    void getIssuesByUserId() {
        // Given
        List<TeamIssue> userIssues = Arrays.asList(openIssue);
        when(teamIssueRepository.findByUserIdWithUserOrderByCreatedAtDesc(1L))
            .thenReturn(userIssues);

        // When
        List<TeamIssue> result = teamIssueService.getIssuesByUserId(1L);

        // Then
        assertEquals(1, result.size());
        assertEquals(openIssue.getId(), result.get(0).getId());
        verify(teamIssueRepository).findByUserIdWithUserOrderByCreatedAtDesc(1L);
    }

    @Test
    @DisplayName("困りごと作成 - 正常ケース")
    void createIssue_Success() {
        // Given
        String content = "新しい困りごとです";
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser1));
        when(teamIssueRepository.save(any(TeamIssue.class))).thenReturn(openIssue);

        // When
        TeamIssue result = teamIssueService.createIssue(1L, content);

        // Then
        assertNotNull(result);
        assertEquals(openIssue.getId(), result.getId());
        verify(userRepository).findById(1L);
        verify(teamIssueRepository).save(any(TeamIssue.class));
    }

    @Test
    @DisplayName("困りごと作成 - ユーザーが存在しない場合")
    void createIssue_UserNotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> teamIssueService.createIssue(999L, "困りごと内容")
        );
        assertEquals("ユーザーが見つかりません: 999", exception.getMessage());
        verify(userRepository).findById(999L);
        verify(teamIssueRepository, never()).save(any());
    }

    @Test
    @DisplayName("困りごと作成 - 空のコンテンツ")
    void createIssue_EmptyContent() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser1));

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> teamIssueService.createIssue(1L, "  ")
        );
        assertEquals("困りごとの内容は必須です", exception.getMessage());
        verify(teamIssueRepository, never()).save(any());
    }

    @Test
    @DisplayName("困りごと作成 - nullコンテンツ")
    void createIssue_NullContent() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser1));

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> teamIssueService.createIssue(1L, null)
        );
        assertEquals("困りごとの内容は必須です", exception.getMessage());
        verify(teamIssueRepository, never()).save(any());
    }

    @Test
    @DisplayName("困りごと作成 - 長すぎるコンテンツ")
    void createIssue_TooLongContent() {
        // Given
        String longContent = "a".repeat(1001); // 1001文字
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser1));

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> teamIssueService.createIssue(1L, longContent)
        );
        assertEquals("困りごとの内容は1000文字以内で入力してください", exception.getMessage());
        verify(teamIssueRepository, never()).save(any());
    }

    @Test
    @DisplayName("困りごと作成 - 1000文字ちょうどは有効")
    void createIssue_MaxLengthContent() {
        // Given
        String maxContent = "a".repeat(1000); // 1000文字
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser1));
        when(teamIssueRepository.save(any(TeamIssue.class))).thenReturn(openIssue);

        // When
        TeamIssue result = teamIssueService.createIssue(1L, maxContent);

        // Then
        assertNotNull(result);
        verify(teamIssueRepository).save(any(TeamIssue.class));
    }

    @Test
    @DisplayName("困りごと解決 - 正常ケース")
    void resolveIssue_Success() {
        // Given
        when(teamIssueRepository.findById(1L)).thenReturn(Optional.of(openIssue));
        when(teamIssueRepository.save(any(TeamIssue.class))).thenReturn(openIssue);

        // When
        TeamIssue result = teamIssueService.resolveIssue(1L);

        // Then
        assertNotNull(result);
        verify(teamIssueRepository).findById(1L);
        verify(teamIssueRepository).save(openIssue);
    }

    @Test
    @DisplayName("困りごと解決 - 困りごとが存在しない場合")
    void resolveIssue_IssueNotFound() {
        // Given
        when(teamIssueRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> teamIssueService.resolveIssue(999L)
        );
        assertEquals("困りごとが見つかりません: 999", exception.getMessage());
        verify(teamIssueRepository).findById(999L);
        verify(teamIssueRepository, never()).save(any());
    }

    @Test
    @DisplayName("困りごと解決 - 既に解決済みの場合")
    void resolveIssue_AlreadyResolved() {
        // Given
        when(teamIssueRepository.findById(2L)).thenReturn(Optional.of(resolvedIssue));

        // When & Then
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> teamIssueService.resolveIssue(2L)
        );
        assertEquals("この困りごとは既に解決済みです", exception.getMessage());
        verify(teamIssueRepository).findById(2L);
        verify(teamIssueRepository, never()).save(any());
    }

    @Test
    @DisplayName("困りごと再オープン - 正常ケース")
    void reopenIssue_Success() {
        // Given
        when(teamIssueRepository.findById(2L)).thenReturn(Optional.of(resolvedIssue));
        when(teamIssueRepository.save(any(TeamIssue.class))).thenReturn(resolvedIssue);

        // When
        TeamIssue result = teamIssueService.reopenIssue(2L);

        // Then
        assertNotNull(result);
        verify(teamIssueRepository).findById(2L);
        verify(teamIssueRepository).save(resolvedIssue);
    }

    @Test
    @DisplayName("困りごと再オープン - 困りごとが存在しない場合")
    void reopenIssue_IssueNotFound() {
        // Given
        when(teamIssueRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> teamIssueService.reopenIssue(999L)
        );
        assertEquals("困りごとが見つかりません: 999", exception.getMessage());
        verify(teamIssueRepository).findById(999L);
        verify(teamIssueRepository, never()).save(any());
    }

    @Test
    @DisplayName("困りごと再オープン - 既に未解決の場合")
    void reopenIssue_AlreadyOpen() {
        // Given
        when(teamIssueRepository.findById(1L)).thenReturn(Optional.of(openIssue));

        // When & Then
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> teamIssueService.reopenIssue(1L)
        );
        assertEquals("この困りごとは既に未解決状態です", exception.getMessage());
        verify(teamIssueRepository).findById(1L);
        verify(teamIssueRepository, never()).save(any());
    }

    @Test
    @DisplayName("困りごと別コメント取得")
    void getCommentsByIssueId() {
        // Given
        List<IssueComment> comments = Arrays.asList(comment1, comment2);
        when(issueCommentRepository.findByIssueIdWithUserOrderByCreatedAtAsc(1L))
            .thenReturn(comments);

        // When
        List<IssueComment> result = teamIssueService.getCommentsByIssueId(1L);

        // Then
        assertEquals(2, result.size());
        assertEquals(comment1.getId(), result.get(0).getId());
        assertEquals(comment2.getId(), result.get(1).getId());
        verify(issueCommentRepository).findByIssueIdWithUserOrderByCreatedAtAsc(1L);
    }

    @Test
    @DisplayName("コメント追加 - 正常ケース")
    void addComment_Success() {
        // Given
        String content = "新しいコメントです";
        when(teamIssueRepository.findById(1L)).thenReturn(Optional.of(openIssue));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser1));
        when(issueCommentRepository.save(any(IssueComment.class))).thenReturn(comment1);

        // When
        IssueComment result = teamIssueService.addComment(1L, 1L, content);

        // Then
        assertNotNull(result);
        assertEquals(comment1.getId(), result.getId());
        verify(teamIssueRepository).findById(1L);
        verify(userRepository).findById(1L);
        verify(issueCommentRepository).save(any(IssueComment.class));
    }

    @Test
    @DisplayName("コメント追加 - 困りごとが存在しない場合")
    void addComment_IssueNotFound() {
        // Given
        when(teamIssueRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> teamIssueService.addComment(999L, 1L, "コメント内容")
        );
        assertEquals("困りごとが見つかりません: 999", exception.getMessage());
        verify(teamIssueRepository).findById(999L);
        verify(issueCommentRepository, never()).save(any());
    }

    @Test
    @DisplayName("コメント追加 - ユーザーが存在しない場合")
    void addComment_UserNotFound() {
        // Given
        when(teamIssueRepository.findById(1L)).thenReturn(Optional.of(openIssue));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> teamIssueService.addComment(1L, 999L, "コメント内容")
        );
        assertEquals("ユーザーが見つかりません: 999", exception.getMessage());
        verify(userRepository).findById(999L);
        verify(issueCommentRepository, never()).save(any());
    }

    @Test
    @DisplayName("コメント追加 - 空のコンテンツ")
    void addComment_EmptyContent() {
        // Given
        when(teamIssueRepository.findById(1L)).thenReturn(Optional.of(openIssue));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser1));

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> teamIssueService.addComment(1L, 1L, "  ")
        );
        assertEquals("コメント内容は必須です", exception.getMessage());
        verify(issueCommentRepository, never()).save(any());
    }

    @Test
    @DisplayName("コメント追加 - 長すぎるコンテンツ")
    void addComment_TooLongContent() {
        // Given
        String longContent = "a".repeat(501); // 501文字
        when(teamIssueRepository.findById(1L)).thenReturn(Optional.of(openIssue));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser1));

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> teamIssueService.addComment(1L, 1L, longContent)
        );
        assertEquals("コメントは500文字以内で入力してください", exception.getMessage());
        verify(issueCommentRepository, never()).save(any());
    }

    @Test
    @DisplayName("未解決困りごと件数取得")
    void getOpenIssueCount() {
        // Given
        when(teamIssueRepository.countByStatus(IssueStatus.OPEN)).thenReturn(5L);

        // When
        long result = teamIssueService.getOpenIssueCount();

        // Then
        assertEquals(5L, result);
        verify(teamIssueRepository).countByStatus(IssueStatus.OPEN);
    }

    @Test
    @DisplayName("解決済み困りごと件数取得")
    void getResolvedIssueCount() {
        // Given
        when(teamIssueRepository.countByStatus(IssueStatus.RESOLVED)).thenReturn(3L);

        // When
        long result = teamIssueService.getResolvedIssueCount();

        // Then
        assertEquals(3L, result);
        verify(teamIssueRepository).countByStatus(IssueStatus.RESOLVED);
    }

    @Test
    @DisplayName("困りごと別コメント数取得")
    void getCommentCount() {
        // Given
        when(issueCommentRepository.countByIssueId(1L)).thenReturn(2L);

        // When
        long result = teamIssueService.getCommentCount(1L);

        // Then
        assertEquals(2L, result);
        verify(issueCommentRepository).countByIssueId(1L);
    }

    @Test
    @DisplayName("困りごと作成 - 前後の空白を除去")
    void createIssue_TrimContent() {
        // Given
        String contentWithSpaces = "  困りごと内容  ";
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser1));
        when(teamIssueRepository.save(any(TeamIssue.class))).thenAnswer(invocation -> {
            TeamIssue issue = invocation.getArgument(0);
            assertEquals("困りごと内容", issue.getContent()); // 空白が除去されていることを確認
            return issue;
        });

        // When
        teamIssueService.createIssue(1L, contentWithSpaces);

        // Then
        verify(teamIssueRepository).save(any(TeamIssue.class));
    }

    @Test
    @DisplayName("コメント追加 - 前後の空白を除去")
    void addComment_TrimContent() {
        // Given
        String contentWithSpaces = "  コメント内容  ";
        when(teamIssueRepository.findById(1L)).thenReturn(Optional.of(openIssue));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser1));
        when(issueCommentRepository.save(any(IssueComment.class))).thenAnswer(invocation -> {
            IssueComment comment = invocation.getArgument(0);
            assertEquals("コメント内容", comment.getContent()); // 空白が除去されていることを確認
            return comment;
        });

        // When
        teamIssueService.addComment(1L, 1L, contentWithSpaces);

        // Then
        verify(issueCommentRepository).save(any(IssueComment.class));
    }
}