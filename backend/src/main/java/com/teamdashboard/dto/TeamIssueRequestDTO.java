package com.teamdashboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 困りごと投稿リクエスト用DTO
 */
public class TeamIssueRequestDTO {
    
    @NotBlank(message = "困りごとの内容は必須です")
    @Size(max = 1000, message = "困りごとの内容は1000文字以内で入力してください")
    private String content;

    // Constructors
    public TeamIssueRequestDTO() {}

    public TeamIssueRequestDTO(String content) {
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