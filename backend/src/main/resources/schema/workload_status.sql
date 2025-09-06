-- workload_statusテーブル作成DDL
CREATE TABLE workload_status (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    workload_level VARCHAR(10) NOT NULL CHECK (workload_level IN ('LOW', 'MEDIUM', 'HIGH')),
    project_count INTEGER CHECK (project_count >= 0),
    task_count INTEGER CHECK (task_count >= 0),
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_workload (user_id)
);

-- インデックス作成
CREATE INDEX idx_workload_status_user_id ON workload_status(user_id);
CREATE INDEX idx_workload_status_level ON workload_status(workload_level);
CREATE INDEX idx_workload_status_updated_at ON workload_status(updated_at);

-- コメント追加
COMMENT ON TABLE workload_status IS 'チームメンバーの負荷状況管理テーブル';
COMMENT ON COLUMN workload_status.id IS '主キー';
COMMENT ON COLUMN workload_status.user_id IS 'ユーザーID（外部キー）';
COMMENT ON COLUMN workload_status.workload_level IS '負荷レベル（LOW/MEDIUM/HIGH）';
COMMENT ON COLUMN workload_status.project_count IS '担当案件数（任意）';
COMMENT ON COLUMN workload_status.task_count IS '進行中タスク数（任意）';
COMMENT ON COLUMN workload_status.updated_at IS '最終更新日時';