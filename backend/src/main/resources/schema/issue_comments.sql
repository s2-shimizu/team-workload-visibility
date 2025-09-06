-- issue_commentsテーブル作成DDL
CREATE TABLE issue_comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    issue_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (issue_id) REFERENCES team_issues(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- インデックス作成
CREATE INDEX idx_issue_comments_issue_id ON issue_comments(issue_id);
CREATE INDEX idx_issue_comments_user_id ON issue_comments(user_id);
CREATE INDEX idx_issue_comments_created_at ON issue_comments(created_at);

-- コメント追加
COMMENT ON TABLE issue_comments IS '困りごとに対するコメント管理テーブル';
COMMENT ON COLUMN issue_comments.id IS '主キー';
COMMENT ON COLUMN issue_comments.issue_id IS '困りごとID（外部キー）';
COMMENT ON COLUMN issue_comments.user_id IS 'コメント投稿者のユーザーID（外部キー）';
COMMENT ON COLUMN issue_comments.content IS 'コメント内容（最大500文字）';
COMMENT ON COLUMN issue_comments.created_at IS '作成日時';