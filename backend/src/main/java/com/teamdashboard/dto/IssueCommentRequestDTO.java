package com.teamdashboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * コメント投稿リクエスト用DTO
 */
public class IssueCommentRequestDTO {
    
    @NotBlank(message = "コメント内容は必須です")
    @Size(max = 500, message = "コメントは500文字以内で入力してください")
    private String content;

    // Constructors
    public IssueCommentRequestDTO() {}

    public IssueCommentRequestDTO(String content) {
        this.content = content;
    }

    // Getters and Setters
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}