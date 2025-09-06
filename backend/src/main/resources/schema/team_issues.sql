-- team_issuesテーブル作成DDL
CREATE TABLE team_issues (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'RESOLVED')),
    created_at TIMESTAMP NOT NULL,
    resolved_at TIMESTAMP NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- インデックス作成
CREATE INDEX idx_team_issues_user_id ON team_issues(user_id);
CREATE INDEX idx_team_issues_status ON team_issues(status);
CREATE INDEX idx_team_issues_created_at ON team_issues(created_at);
CREATE INDEX idx_team_issues_resolved_at ON team_issues(resolved_at);

-- コメント追加
COMMENT ON TABLE team_issues IS 'チームの困りごと管理テーブル';
COMMENT ON COLUMN team_issues.id IS '主キー';
COMMENT ON COLUMN team_issues.user_id IS '投稿者のユーザーID（外部キー）';
COMMENT ON COLUMN team_issues.content IS '困りごとの内容（最大1000文字）';
COMMENT ON COLUMN team_issues.status IS '状態（OPEN/RESOLVED）';
COMMENT ON COLUMN team_issues.created_at IS '作成日時';
COMMENT ON COLUMN team_issues.resolved_at IS '解決日時（解決済みの場合のみ）';