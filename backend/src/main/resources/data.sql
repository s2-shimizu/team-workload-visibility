-- Test users
INSERT INTO users (username, password, email, display_name, department, created_at, updated_at) VALUES
('testuser', 'password123', 'test@example.com', 'Test User', 'Development', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('yamada', 'password123', 'yamada@example.com', 'Yamada Taro', 'Development', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('suzuki', 'password123', 'suzuki@example.com', 'Suzuki Hanako', 'Design', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('tanaka', 'password123', 'tanaka@example.com', 'Tanaka Jiro', 'Development', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Sample daily reports
INSERT INTO daily_reports (user_id, report_date, work_content, insights, issues, workload_level, created_at, updated_at) VALUES
((SELECT id FROM users WHERE username = 'yamada'), CURRENT_DATE, 
 'Implemented user authentication feature. Had some trouble with Spring Security configuration, but basic functionality is complete.', 
 'Gained deeper understanding of Spring Security configuration methods.', 
 'Complex authorization rule implementation is taking time.', 
 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

((SELECT id FROM users WHERE username = 'suzuki'), CURRENT_DATE, 
 'Improved UI design and responsive support. Spent time adjusting mobile display.', 
 'Using CSS Grid enabled more flexible layouts.', 
 NULL, 
 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

((SELECT id FROM users WHERE username = 'tanaka'), CURRENT_DATE, 
 'Reviewed database design and improved performance. Adding indexes significantly improved speed.', 
 'Recognized the importance of checking query execution plans.', 
 'Complex JOIN query optimization is difficult and needs more time.', 
 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Sample workload status data
INSERT INTO workload_status (user_id, workload_level, project_count, task_count, updated_at) VALUES
((SELECT id FROM users WHERE username = 'yamada'), 'HIGH', 3, 8, CURRENT_TIMESTAMP),
((SELECT id FROM users WHERE username = 'suzuki'), 'MEDIUM', 2, 5, CURRENT_TIMESTAMP),
((SELECT id FROM users WHERE username = 'tanaka'), 'HIGH', 4, 12, CURRENT_TIMESTAMP),
((SELECT id FROM users WHERE username = 'testuser'), 'LOW', 1, 3, CURRENT_TIMESTAMP);

-- Sample team issues data
INSERT INTO team_issues (user_id, content, status, created_at, resolved_at) VALUES
((SELECT id FROM users WHERE username = 'yamada'), 
 'Spring Securityの設定で認可ルールが複雑になってしまい、実装に時間がかかっています。良いアプローチがあれば教えてください。', 
 'OPEN', DATEADD('DAY', -2, CURRENT_TIMESTAMP), NULL),

((SELECT id FROM users WHERE username = 'tanaka'), 
 'データベースのJOINクエリの最適化に苦戦しています。実行計画を見ても改善点が分からない状況です。', 
 'OPEN', DATEADD('DAY', -1, CURRENT_TIMESTAMP), NULL),

((SELECT id FROM users WHERE username = 'suzuki'), 
 'モバイル対応のレスポンシブデザインで、一部のコンポーネントが崩れてしまう問題がありました。', 
 'RESOLVED', DATEADD('DAY', -3, CURRENT_TIMESTAMP), DATEADD('DAY', -1, CURRENT_TIMESTAMP));

-- Sample issue comments data (using direct IDs to avoid complex queries)
INSERT INTO issue_comments (issue_id, user_id, content, created_at) VALUES
(1, 2, 'Spring Securityの設定は確かに複雑ですね。設定クラスを機能別に分割することをお勧めします。', DATEADD('DAY', -1, CURRENT_TIMESTAMP)),
(1, 3, '私も以前同じ問題に遭遇しました。公式ドキュメントのサンプルが参考になると思います。', DATEADD('DAY', -1, CURRENT_TIMESTAMP)),
(2, 1, 'インデックスの設定は確認されましたか？適切なインデックスがないとJOINのパフォーマンスが大幅に低下します。', DATEADD('HOUR', -12, CURRENT_TIMESTAMP)),
(3, 1, 'CSS Gridを使った解決方法、とても参考になりました！', DATEADD('DAY', -2, CURRENT_TIMESTAMP));