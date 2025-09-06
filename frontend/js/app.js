// API設定
const API_BASE_URL = 'http://localhost:8081/api';  // ポート番号を合わせる

// DOM要素
const tabButtons = document.querySelectorAll('.nav-btn');
const tabContents = document.querySelectorAll('.tab-content');
const reportForm = document.getElementById('reportForm');
const dateFilter = document.getElementById('dateFilter');

// 初期化
document.addEventListener('DOMContentLoaded', function() {
    initializeTabs();
    initializeCalendar();
    loadDashboard();
    
    // イベントリスナー
    reportForm.addEventListener('submit', handleReportSubmit);
    dateFilter.addEventListener('change', loadRecentReports);
    
    // 負荷状況更新ボタン
    const updateWorkloadBtn = document.getElementById('updateWorkloadBtn');
    if (updateWorkloadBtn) {
        updateWorkloadBtn.addEventListener('click', openWorkloadModal);
    }
    
    // 負荷状況モーダル関連
    initializeWorkloadModal();
    
    // 困りごと関連
    initializeTeamIssues();
});

// タブ機能
function initializeTabs() {
    tabButtons.forEach(button => {
        button.addEventListener('click', () => {
            const tabId = button.getAttribute('data-tab');
            switchTab(tabId);
        });
    });
}

function switchTab(tabId) {
    // タブボタンの状態更新
    tabButtons.forEach(btn => btn.classList.remove('active'));
    document.querySelector(`[data-tab="${tabId}"]`).classList.add('active');
    
    // タブコンテンツの表示切り替え
    tabContents.forEach(content => content.classList.remove('active'));
    document.getElementById(tabId).classList.add('active');
    
    // タブ切り替え時の処理
    if (tabId === 'dashboard') {
        loadDashboard();
    } else if (tabId === 'calendar') {
        loadCalendar();
    }
}

// ダッシュボード読み込み
async function loadDashboard() {
    await Promise.all([
        loadWorkloadStatus(),
        loadTeamIssues(),
        loadTeamStatus(),
        loadRecentReports()
    ]);
}

// 負荷状況読み込み（新しいAPIクライアントを使用）
async function loadWorkloadStatus() {
    try {
        await dataManager.refreshWorkloadStatuses();
    } catch (error) {
        console.error('負荷状況の読み込みに失敗:', error);
        // エラーハンドリングはdataManagerで行われる
    }
}

// 負荷状況カード作成
function createWorkloadCard(status) {
    const card = document.createElement('div');
    card.className = `workload-card level-${status.workloadLevel}`;
    
    const lastUpdated = status.updatedAt ? 
        formatDateTime(status.updatedAt) : '未更新';
    
    const projectCount = status.projectCount ? 
        `<div class="workload-detail">📁 ${status.projectCount}案件</div>` : '';
    
    const taskCount = status.taskCount ? 
        `<div class="workload-detail">📋 ${status.taskCount}タスク</div>` : '';
    
    card.innerHTML = `
        <div class="user-name">${status.displayName}</div>
        <div class="workload-level">
            <span class="workload-level-badge ${status.workloadLevel}">
                ${getWorkloadLevelText(status.workloadLevel)}
            </span>
            <span>${getWorkloadLevelEmoji(status.workloadLevel)}</span>
        </div>
        <div class="workload-details">
            ${projectCount}
            ${taskCount}
        </div>
        <div class="last-updated">最終更新: ${lastUpdated}</div>
    `;
    
    return card;
}

// チーム状況読み込み
async function loadTeamStatus() {
    try {
        const response = await fetch(`${API_BASE_URL}/reports/recent?days=1`);
        const reports = await response.json();
        
        const statusCards = document.getElementById('teamStatusCards');
        statusCards.innerHTML = '';
        
        if (reports.length === 0) {
            statusCards.innerHTML = '<p>今日の日報はまだありません</p>';
            return;
        }
        
        // ユーザーごとの最新状況を表示
        const userReports = new Map();
        reports.forEach(report => {
            if (!userReports.has(report.username) || 
                new Date(report.createdAt) > new Date(userReports.get(report.username).createdAt)) {
                userReports.set(report.username, report);
            }
        });
        
        userReports.forEach(report => {
            const card = createStatusCard(report);
            statusCards.appendChild(card);
        });
        
    } catch (error) {
        console.error('チーム状況の読み込みに失敗:', error);
        showNotification('チーム状況の読み込みに失敗しました', 'error');
    }
}

// 状況カード作成
function createStatusCard(report) {
    const card = document.createElement('div');
    card.className = `status-card level-${report.workloadLevel || 2}`;
    
    card.innerHTML = `
        <div class="name">${report.displayName}</div>
        <div class="workload">${getWorkloadText(report.workloadLevel)} ${getWorkloadEmoji(report.workloadLevel)}</div>
    `;
    
    return card;
}

// 最新日報読み込み
async function loadRecentReports() {
    try {
        const filterValue = dateFilter.value;
        let url = `${API_BASE_URL}/reports/recent?days=7`;
        
        if (filterValue === 'today') {
            const today = new Date().toISOString().split('T')[0];
            url = `${API_BASE_URL}/reports/date/${today}`;
        } else if (filterValue === 'yesterday') {
            const yesterday = new Date();
            yesterday.setDate(yesterday.getDate() - 1);
            url = `${API_BASE_URL}/reports/date/${yesterday.toISOString().split('T')[0]}`;
        }
        
        const response = await fetch(url);
        const reports = await response.json();
        
        const reportsList = document.getElementById('recentReportsList');
        reportsList.innerHTML = '';
        
        if (reports.length === 0) {
            reportsList.innerHTML = '<p>該当する日報がありません</p>';
            return;
        }
        
        reports.forEach(report => {
            const item = createReportItem(report);
            reportsList.appendChild(item);
        });
        
    } catch (error) {
        console.error('日報の読み込みに失敗:', error);
        showNotification('日報の読み込みに失敗しました', 'error');
    }
}

// 日報アイテム作成
function createReportItem(report) {
    const item = document.createElement('div');
    item.className = 'report-item';
    
    const reportDate = new Date(report.reportDate).toLocaleDateString('ja-JP');
    
    item.innerHTML = `
        <div class="header">
            <div>
                <span class="author">${report.displayName}</span>
                <span class="date">${reportDate}</span>
            </div>
            <span class="workload-badge level-${report.workloadLevel}">
                ${getWorkloadText(report.workloadLevel)} ${getWorkloadEmoji(report.workloadLevel)}
            </span>
        </div>
        <div class="content">
            <strong>作業内容:</strong> ${report.workContent}<br>
            ${report.insights ? `<strong>気づき:</strong> ${report.insights}<br>` : ''}
            ${report.issues ? `<strong>困りごと:</strong> ${report.issues}` : ''}
        </div>
    `;
    
    return item;
}

// 日報投稿処理
async function handleReportSubmit(event) {
    event.preventDefault();
    
    const formData = new FormData(reportForm);
    const reportData = {
        workContent: formData.get('workContent'),
        insights: formData.get('insights'),
        issues: formData.get('issues'),
        workloadLevel: parseInt(formData.get('workloadLevel'))
    };
    
    try {
        const response = await fetch(`${API_BASE_URL}/reports`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(reportData)
        });
        
        if (response.ok) {
            showNotification('日報を投稿しました', 'success');
            reportForm.reset();
            // ダッシュボードを更新
            if (document.getElementById('dashboard').classList.contains('active')) {
                loadDashboard();
            }
        } else {
            const error = await response.text();
            showNotification(`投稿に失敗しました: ${error}`, 'error');
        }
        
    } catch (error) {
        console.error('日報投稿エラー:', error);
        showNotification('投稿に失敗しました', 'error');
    }
}

// カレンダー初期化
function initializeCalendar() {
    const prevBtn = document.getElementById('prevMonth');
    const nextBtn = document.getElementById('nextMonth');
    
    prevBtn.addEventListener('click', () => changeMonth(-1));
    nextBtn.addEventListener('click', () => changeMonth(1));
}

let currentCalendarDate = new Date();

// カレンダー読み込み
async function loadCalendar() {
    await renderCalendar();
}

// 月変更
function changeMonth(delta) {
    currentCalendarDate.setMonth(currentCalendarDate.getMonth() + delta);
    renderCalendar();
}

// カレンダー描画
async function renderCalendar() {
    const year = currentCalendarDate.getFullYear();
    const month = currentCalendarDate.getMonth();
    
    // 月表示更新
    document.getElementById('currentMonth').textContent = 
        `${year}年${month + 1}月`;
    
    // カレンダーグリッド作成
    const grid = document.getElementById('calendarGrid');
    grid.innerHTML = '';
    
    // 曜日ヘッダー
    const weekdays = ['日', '月', '火', '水', '木', '金', '土'];
    weekdays.forEach(day => {
        const dayElement = document.createElement('div');
        dayElement.className = 'calendar-day header';
        dayElement.textContent = day;
        grid.appendChild(dayElement);
    });
    
    // 月の最初の日と最後の日
    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);
    const startDate = new Date(firstDay);
    startDate.setDate(startDate.getDate() - firstDay.getDay());
    
    // 日報データ取得
    const startDateStr = startDate.toISOString().split('T')[0];
    const endDate = new Date(startDate);
    endDate.setDate(endDate.getDate() + 41); // 6週間分
    const endDateStr = endDate.toISOString().split('T')[0];
    
    let reports = [];
    try {
        const response = await fetch(`${API_BASE_URL}/reports/recent?days=42`);
        reports = await response.json();
    } catch (error) {
        console.error('カレンダーデータ取得エラー:', error);
    }
    
    // 日付ごとの日報数をカウント
    const reportCounts = {};
    reports.forEach(report => {
        const date = report.reportDate;
        reportCounts[date] = (reportCounts[date] || 0) + 1;
    });
    
    // カレンダー日付生成
    const currentDate = new Date(startDate);
    const today = new Date().toDateString();
    
    for (let i = 0; i < 42; i++) {
        const dayElement = document.createElement('div');
        dayElement.className = 'calendar-day';
        
        const dateStr = currentDate.toISOString().split('T')[0];
        const reportCount = reportCounts[dateStr] || 0;
        
        // クラス設定
        if (currentDate.getMonth() !== month) {
            dayElement.classList.add('other-month');
        }
        if (currentDate.toDateString() === today) {
            dayElement.classList.add('today');
        }
        if (reportCount > 0) {
            dayElement.classList.add('has-report');
        }
        
        dayElement.innerHTML = `
            <div class="date">${currentDate.getDate()}</div>
            ${reportCount > 0 ? `<div class="report-count">${reportCount}件</div>` : ''}
        `;
        
        grid.appendChild(dayElement);
        currentDate.setDate(currentDate.getDate() + 1);
    }
}

// ユーティリティ関数
function getWorkloadText(level) {
    const texts = {
        1: '軽い',
        2: '普通',
        3: 'やや重い',
        4: '重い',
        5: '非常に重い'
    };
    return texts[level] || '未設定';
}

function getWorkloadEmoji(level) {
    const emojis = {
        1: '😊',
        2: '🙂',
        3: '😐',
        4: '😰',
        5: '😵'
    };
    return emojis[level] || '❓';
}

// 新しい負荷レベル用ユーティリティ関数
function getWorkloadLevelText(level) {
    const texts = {
        'LOW': '低',
        'MEDIUM': '中',
        'HIGH': '高'
    };
    return texts[level] || '未設定';
}

function getWorkloadLevelEmoji(level) {
    const emojis = {
        'LOW': '😊',
        'MEDIUM': '😐',
        'HIGH': '😰'
    };
    return emojis[level] || '❓';
}

function formatDateTime(dateTimeString) {
    const date = new Date(dateTimeString);
    const now = new Date();
    const diffMs = now - date;
    const diffMins = Math.floor(diffMs / (1000 * 60));
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
    
    if (diffMins < 1) {
        return 'たった今';
    } else if (diffMins < 60) {
        return `${diffMins}分前`;
    } else if (diffHours < 24) {
        return `${diffHours}時間前`;
    } else if (diffDays < 7) {
        return `${diffDays}日前`;
    } else {
        return date.toLocaleDateString('ja-JP', {
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    }
}

function showNotification(message, type = 'success', duration = 3000) {
    const notification = document.getElementById('notification');
    
    // 既存の通知をクリア
    notification.classList.remove('show');
    
    // 短い遅延後に新しい通知を表示
    setTimeout(() => {
        notification.textContent = message;
        notification.className = `notification ${type}`;
        notification.classList.add('show');
        
        // 指定された時間後に非表示
        setTimeout(() => {
            notification.classList.remove('show');
        }, duration);
    }, 100);
}

// 複数の通知を順次表示する機能
function showNotificationQueue(notifications) {
    let delay = 0;
    
    notifications.forEach((notif, index) => {
        setTimeout(() => {
            showNotification(notif.message, notif.type, notif.duration || 3000);
        }, delay);
        
        delay += (notif.duration || 3000) + 500; // 通知間の間隔
    });
}

// 成功通知のショートカット
function showSuccessNotification(message, duration = 3000) {
    showNotification(message, 'success', duration);
}

// エラー通知のショートカット
function showErrorNotification(message, duration = 5000) {
    showNotification(message, 'error', duration);
}

// 警告通知のショートカット
function showWarningNotification(message, duration = 4000) {
    showNotification(message, 'warning', duration);
}

// 情報通知のショートカット
function showInfoNotification(message, duration = 3000) {
    showNotification(message, 'info', duration);
}

// 負荷状況モーダル関連
function initializeWorkloadModal() {
    const modal = document.getElementById('workloadModal');
    const closeBtn = document.getElementById('workloadModalClose');
    const cancelBtn = document.getElementById('workloadCancelBtn');
    const form = document.getElementById('workloadForm');
    
    // モーダルを閉じる
    const closeModal = () => {
        modal.classList.remove('show');
        form.reset();
    };
    
    // イベントリスナー
    closeBtn.addEventListener('click', closeModal);
    cancelBtn.addEventListener('click', closeModal);
    
    // モーダル背景クリックで閉じる
    modal.addEventListener('click', (e) => {
        if (e.target === modal) {
            closeModal();
        }
    });
    
    // ESCキーで閉じる
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && modal.classList.contains('show')) {
            closeModal();
        }
    });
    
    // フォーム送信
    form.addEventListener('submit', handleWorkloadSubmit);
}

function openWorkloadModal() {
    const modal = document.getElementById('workloadModal');
    modal.classList.add('show');
    
    // 現在の負荷状況を取得して表示
    loadCurrentWorkloadStatus();
}

async function loadCurrentWorkloadStatus() {
    try {
        const currentStatus = await apiClient.getMyWorkloadStatus();
        
        if (currentStatus) {
            // フォームに現在の値を設定
            const form = document.getElementById('workloadForm');
            if (currentStatus.workloadLevel) {
                const levelRadio = form.querySelector(`input[value="${currentStatus.workloadLevel}"]`);
                if (levelRadio) {
                    levelRadio.checked = true;
                }
            }
            
            if (currentStatus.projectCount !== null) {
                form.projectCount.value = currentStatus.projectCount;
            }
            
            if (currentStatus.taskCount !== null) {
                form.taskCount.value = currentStatus.taskCount;
            }
        }
    } catch (error) {
        console.error('現在の負荷状況取得エラー:', error);
        // エラーは表示しない（新規作成として扱う）
    }
}

async function handleWorkloadSubmit(event) {
    event.preventDefault();
    
    const form = event.target;
    const formData = new FormData(form);
    
    // バリデーション
    const workloadLevel = formData.get('workloadLevel');
    if (!workloadLevel) {
        showNotification('負荷レベルを選択してください', 'error');
        return;
    }
    
    const workloadData = {
        workloadLevel: workloadLevel
    };
    
    // 任意項目の追加
    const projectCount = formData.get('projectCount');
    if (projectCount && projectCount.trim() !== '') {
        const count = parseInt(projectCount);
        if (count >= 0 && count <= 20) {
            workloadData.projectCount = count;
        } else {
            showNotification('案件数は0〜20の範囲で入力してください', 'error');
            return;
        }
    }
    
    const taskCount = formData.get('taskCount');
    if (taskCount && taskCount.trim() !== '') {
        const count = parseInt(taskCount);
        if (count >= 0 && count <= 100) {
            workloadData.taskCount = count;
        } else {
            showNotification('タスク数は0〜100の範囲で入力してください', 'error');
            return;
        }
    }
    
    try {
        await dataManager.updateWorkloadStatus(workloadData);
        
        // モーダルを閉じる
        const modal = document.getElementById('workloadModal');
        modal.classList.remove('show');
        form.reset();
        
    } catch (error) {
        console.error('負荷状況更新エラー:', error);
        // エラーハンドリングはdataManagerで行われる
    }
}

// 困りごと関連
let currentIssuesFilter = 'all';

function initializeTeamIssues() {
    // フィルターボタン
    const filterBtns = document.querySelectorAll('.filter-btn');
    filterBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            const status = btn.getAttribute('data-status');
            setIssuesFilter(status);
        });
    });
    
    // 困りごと投稿ボタン
    const addIssueBtn = document.getElementById('addIssueBtn');
    if (addIssueBtn) {
        addIssueBtn.addEventListener('click', openIssueModal);
    }
    
    // 困りごと投稿モーダル関連
    initializeIssueModal();
}

function setIssuesFilter(status) {
    currentIssuesFilter = status;
    
    // フィルターボタンの状態更新
    const filterBtns = document.querySelectorAll('.filter-btn');
    filterBtns.forEach(btn => {
        btn.classList.toggle('active', btn.getAttribute('data-status') === status);
    });
    
    // 困りごと一覧を再表示
    displayFilteredIssues();
}

let allIssues = [];

async function loadTeamIssues() {
    try {
        await dataManager.refreshTeamIssues();
    } catch (error) {
        console.error('困りごとの読み込みに失敗:', error);
        // エラーハンドリングはdataManagerで行われる
    }
}

function displayFilteredIssues() {
    const issuesList = document.getElementById('teamIssuesList');
    
    let filteredIssues = allIssues;
    if (currentIssuesFilter !== 'all') {
        filteredIssues = allIssues.filter(issue => issue.status === currentIssuesFilter);
    }
    
    issuesList.innerHTML = '';
    
    if (filteredIssues.length === 0) {
        const emptyMessage = currentIssuesFilter === 'all' ? 
            'まだ困りごとが投稿されていません' :
            `${currentIssuesFilter === 'OPEN' ? '未解決の' : '解決済みの'}困りごとはありません`;
        issuesList.innerHTML = `<div class="issues-empty">${emptyMessage}</div>`;
        return;
    }
    
    // 日付順でソート（新しい順）
    filteredIssues.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
    
    filteredIssues.forEach(issue => {
        const item = createIssueItem(issue);
        issuesList.appendChild(item);
    });
}

function createIssueItem(issue) {
    const item = document.createElement('div');
    item.className = `issue-item ${issue.status.toLowerCase()}`;
    item.setAttribute('data-issue-id', issue.id);
    
    const createdDate = formatDateTime(issue.createdAt);
    const resolvedDate = issue.resolvedAt ? formatDateTime(issue.resolvedAt) : null;
    
    item.innerHTML = `
        <div class="issue-header">
            <div class="issue-author">
                <span class="name">${issue.displayName}</span>
                <span class="date">${createdDate}</span>
            </div>
            <div class="issue-status">
                <span class="status-badge ${issue.status}">
                    ${issue.status === 'OPEN' ? '未解決' : '解決済み'}
                </span>
                ${issue.status === 'OPEN' ? 
                    '<button class="resolve-btn" onclick="resolveIssue(' + issue.id + ')">解決</button>' : 
                    (resolvedDate ? `<span class="resolved-date">解決: ${resolvedDate}</span>` : '')
                }
            </div>
        </div>
        <div class="issue-content">${issue.content}</div>
        <div class="issue-actions">
            <button class="comment-btn" onclick="toggleComments(${issue.id})">
                💬 コメント
                <span class="comment-count" id="commentCount-${issue.id}">0</span>
            </button>
        </div>
        <div class="comments-section" id="comments-${issue.id}">
            <div class="comments-list" id="commentsList-${issue.id}">
                <!-- コメント一覧 -->
            </div>
            <div class="comment-form" id="commentForm-${issue.id}">
                <textarea class="comment-input" placeholder="コメントを入力してください..." 
                         id="commentInput-${issue.id}"></textarea>
                <div class="comment-actions">
                    <button class="comment-cancel" onclick="hideCommentForm(${issue.id})">キャンセル</button>
                    <button class="comment-submit" onclick="submitComment(${issue.id})">投稿</button>
                </div>
            </div>
        </div>
    `;
    
    return item;
}

async function toggleComments(issueId) {
    const commentsSection = document.getElementById(`comments-${issueId}`);
    const commentForm = document.getElementById(`commentForm-${issueId}`);
    
    if (commentsSection.classList.contains('show')) {
        commentsSection.classList.remove('show');
        commentForm.classList.remove('show');
    } else {
        commentsSection.classList.add('show');
        commentForm.classList.add('show');
        await loadComments(issueId);
    }
}

async function loadComments(issueId) {
    const commentsList = document.getElementById(`commentsList-${issueId}`);
    const commentCount = document.getElementById(`commentCount-${issueId}`);
    
    try {
        const comments = await apiClient.getIssueComments(issueId);
        
        commentsList.innerHTML = '';
        commentCount.textContent = comments.length;
        
        if (comments.length === 0) {
            commentsList.innerHTML = '<div style="text-align: center; color: #7f8c8d; padding: 20px; font-style: italic;">まだコメントがありません</div>';
            return;
        }
        
        // 日付順でソート（古い順）
        comments.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
        
        comments.forEach(comment => {
            const commentItem = createCommentItem(comment);
            commentsList.appendChild(commentItem);
        });
        
    } catch (error) {
        console.error('コメント読み込みエラー:', error);
        commentsList.innerHTML = '<div style="text-align: center; color: #e74c3c; padding: 20px;">コメントの読み込みに失敗しました</div>';
    }
}

function createCommentItem(comment) {
    const item = document.createElement('div');
    item.className = 'comment-item';
    
    const createdDate = formatDateTime(comment.createdAt);
    
    item.innerHTML = `
        <div class="comment-header">
            <span class="comment-author">${comment.displayName}</span>
            <span class="comment-date">${createdDate}</span>
        </div>
        <div class="comment-content">${comment.content}</div>
    `;
    
    return item;
}

async function submitComment(issueId) {
    const input = document.getElementById(`commentInput-${issueId}`);
    const content = input.value.trim();
    
    if (!content) {
        showNotification('コメント内容を入力してください', 'error');
        return;
    }
    
    try {
        await dataManager.addIssueComment(issueId, { content });
        input.value = '';
        await loadComments(issueId);
    } catch (error) {
        console.error('コメント投稿エラー:', error);
        // エラーハンドリングはdataManagerで行われる
    }
}

function hideCommentForm(issueId) {
    const commentForm = document.getElementById(`commentForm-${issueId}`);
    const input = document.getElementById(`commentInput-${issueId}`);
    
    commentForm.classList.remove('show');
    input.value = '';
}

async function resolveIssue(issueId) {
    if (!confirm('この困りごとを解決済みにしますか？')) {
        return;
    }
    
    try {
        await dataManager.resolveTeamIssue(issueId);
    } catch (error) {
        console.error('解決処理エラー:', error);
        // エラーハンドリングはdataManagerで行われる
    }
}

// 困りごと投稿モーダル関連
function initializeIssueModal() {
    const modal = document.getElementById('issueModal');
    const closeBtn = document.getElementById('issueModalClose');
    const cancelBtn = document.getElementById('issueCancelBtn');
    const form = document.getElementById('issueForm');
    
    // モーダルを閉じる
    const closeModal = () => {
        modal.classList.remove('show');
        form.reset();
    };
    
    // イベントリスナー
    closeBtn.addEventListener('click', closeModal);
    cancelBtn.addEventListener('click', closeModal);
    
    // モーダル背景クリックで閉じる
    modal.addEventListener('click', (e) => {
        if (e.target === modal) {
            closeModal();
        }
    });
    
    // ESCキーで閉じる
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && modal.classList.contains('show')) {
            closeModal();
        }
    });
    
    // フォーム送信
    form.addEventListener('submit', handleIssueSubmit);
}

function openIssueModal() {
    const modal = document.getElementById('issueModal');
    modal.classList.add('show');
    
    // テキストエリアにフォーカス
    setTimeout(() => {
        const textarea = document.getElementById('issueContent');
        textarea.focus();
    }, 100);
}

async function handleIssueSubmit(event) {
    event.preventDefault();
    
    const form = event.target;
    const formData = new FormData(form);
    
    // バリデーション
    const content = formData.get('content').trim();
    if (!content) {
        showNotification('困りごとの内容を入力してください', 'error');
        return;
    }
    
    if (content.length < 10) {
        showNotification('困りごとの内容をもう少し詳しく書いてください（10文字以上）', 'error');
        return;
    }
    
    if (content.length > 1000) {
        showNotification('困りごとの内容は1000文字以内で入力してください', 'error');
        return;
    }
    
    const issueData = {
        content: content
    };
    
    try {
        await dataManager.createTeamIssue(issueData);
        
        // モーダルを閉じる
        const modal = document.getElementById('issueModal');
        modal.classList.remove('show');
        form.reset();
        
        // フィルターを「すべて」に設定
        setIssuesFilter('all');
        
    } catch (error) {
        console.error('困りごと投稿エラー:', error);
        // エラーハンドリングはdataManagerで行われる
    }
}
/
/ ===== API統合とデータフロー実装 =====

// データマネージャーの初期化と設定
document.addEventListener('DOMContentLoaded', function() {
    // 既存の初期化処理の後に実行
    setTimeout(() => {
        initializeDataManager();
    }, 100);
});

function initializeDataManager() {
    // データ更新コールバックを登録
    dataManager.onDataUpdate('workloadStatuses', (workloadStatuses) => {
        console.log('負荷状況データが更新されました:', workloadStatuses.length, '件');
        updateDataFreshnessIndicator('workload', Date.now());
    });

    dataManager.onDataUpdate('teamIssues', (teamIssues) => {
        console.log('困りごとデータが更新されました:', teamIssues.length, '件');
        updateDataFreshnessIndicator('issues', Date.now());
        
        // グローバル変数を更新（既存コードとの互換性のため）
        window.allIssues = teamIssues;
    });

    // 自動更新を開始（30秒間隔）
    dataManager.startAutoRefresh('workloadStatuses', 30000);
    dataManager.startAutoRefresh('teamIssues', 30000);

    // ページの可視性変更時の処理
    document.addEventListener('visibilitychange', () => {
        if (!document.hidden) {
            // ページが再表示された時に古いデータを更新
            dataManager.refreshStaleData(60000); // 1分以上古いデータを更新
        }
    });

    // ネットワーク状態の監視
    window.addEventListener('online', () => {
        dataManager.showConnectionStatus('online');
        // オンラインになったらデータを更新
        dataManager.refreshAllData();
    });

    window.addEventListener('offline', () => {
        dataManager.showConnectionStatus('offline');
    });

    console.log('データマネージャーが初期化されました');
}

// データの新鮮度インジケーターを更新
function updateDataFreshnessIndicator(type, timestamp) {
    const indicators = {
        'workload': '.workload-status .data-freshness',
        'issues': '.team-issues .data-freshness'
    };

    const selector = indicators[type];
    if (!selector) return;

    let indicator = document.querySelector(selector);
    if (!indicator) {
        // インジケーターが存在しない場合は作成
        const section = document.querySelector(selector.split(' .data-freshness')[0]);
        if (section) {
            indicator = document.createElement('div');
            indicator.className = 'data-freshness';
            section.appendChild(indicator);
        }
    }

    if (indicator) {
        const now = Date.now();
        const age = now - timestamp;
        const ageMinutes = Math.floor(age / (1000 * 60));

        if (ageMinutes < 1) {
            indicator.textContent = 'たった今更新';
            indicator.className = 'data-freshness fresh';
        } else if (ageMinutes < 5) {
            indicator.textContent = `${ageMinutes}分前に更新`;
            indicator.className = 'data-freshness fresh';
        } else if (ageMinutes < 30) {
            indicator.textContent = `${ageMinutes}分前に更新`;
            indicator.className = 'data-freshness';
        } else {
            indicator.textContent = `${ageMinutes}分前に更新（古い可能性があります）`;
            indicator.className = 'data-freshness stale';
        }
    }
}

// 接続状態の表示（DataManagerに移行済み）
function showConnectionStatus(status) {
    // DataManagerの同名メソッドを使用
    if (window.dataManager) {
        window.dataManager.showConnectionStatus(status);
    }
    let indicator = document.querySelector('.connection-status');
    if (!indicator) {
        indicator = document.createElement('div');
        indicator.className = 'connection-status';
        document.body.appendChild(indicator);
    }

    indicator.className = `connection-status ${status}`;
    
    const messages = {
        'online': '✅ オンライン',
        'offline': '❌ オフライン',
        'reconnecting': '🔄 再接続中...'
    };

    indicator.textContent = messages[status] || status;
    indicator.classList.add('show');

    // 3秒後に非表示
    setTimeout(() => {
        indicator.classList.remove('show');
    }, 3000);
}

// 自動更新インジケーターの表示
function showAutoRefreshIndicator() {
    let indicator = document.querySelector('.auto-refresh-indicator');
    if (!indicator) {
        indicator = document.createElement('div');
        indicator.className = 'auto-refresh-indicator';
        indicator.innerHTML = '<div class="spinner"></div><span>データを更新中...</span>';
        document.body.appendChild(indicator);
    }

    indicator.classList.add('show');
    return indicator;
}

function hideAutoRefreshIndicator() {
    const indicator = document.querySelector('.auto-refresh-indicator');
    if (indicator) {
        indicator.classList.remove('show');
    }
}

// ローディング状態の管理を強化
document.addEventListener('loadingStateChange', (event) => {
    const { key, isLoading } = event.detail;
    
    // 自動更新インジケーターの表示/非表示
    if (key === 'workload-statuses' || key === 'team-issues') {
        if (isLoading) {
            showAutoRefreshIndicator();
        } else {
            hideAutoRefreshIndicator();
        }
    }

    // ボタンのローディング状態を管理
    const buttonMappings = {
        'update-workload-status': '#updateWorkloadBtn',
        'create-team-issue': '#addIssueBtn',
        'resolve-team-issue': '.resolve-btn'
    };

    const buttonSelector = buttonMappings[key];
    if (buttonSelector) {
        const buttons = document.querySelectorAll(buttonSelector);
        buttons.forEach(button => {
            if (isLoading) {
                button.classList.add('loading');
                button.disabled = true;
            } else {
                button.classList.remove('loading');
                button.disabled = false;
            }
        });
    }
});

// エラーハンドリングの強化
apiClient.onError('workload-statuses', (error) => {
    console.error('負荷状況API エラー:', error);
    if (error.status === 401) {
        showNotification('認証が必要です。ログインしてください。', 'error');
    } else if (error.status >= 500) {
        showNotification('サーバーエラーが発生しました。しばらく待ってから再試行してください。', 'error');
    }
});

apiClient.onError('team-issues', (error) => {
    console.error('困りごとAPI エラー:', error);
    if (error.status === 401) {
        showNotification('認証が必要です。ログインしてください。', 'error');
    } else if (error.status >= 500) {
        showNotification('サーバーエラーが発生しました。しばらく待ってから再試行してください。', 'error');
    }
});

// 手動更新ボタンの追加
function addManualRefreshButtons() {
    // 負荷状況セクションに手動更新ボタンを追加
    const workloadHeader = document.querySelector('.workload-status .section-header');
    if (workloadHeader && !workloadHeader.querySelector('.refresh-btn')) {
        const refreshBtn = document.createElement('button');
        refreshBtn.className = 'refresh-btn';
        refreshBtn.innerHTML = '🔄 更新';
        refreshBtn.title = '負荷状況を手動で更新';
        refreshBtn.addEventListener('click', async () => {
            try {
                await dataManager.refreshWorkloadStatuses();
                showNotification('負荷状況を更新しました', 'success');
            } catch (error) {
                console.error('手動更新エラー:', error);
            }
        });
        workloadHeader.appendChild(refreshBtn);
    }

    // 困りごとセクションに手動更新ボタンを追加
    const issuesHeader = document.querySelector('.team-issues .section-header');
    if (issuesHeader && !issuesHeader.querySelector('.refresh-btn')) {
        const refreshBtn = document.createElement('button');
        refreshBtn.className = 'refresh-btn';
        refreshBtn.innerHTML = '🔄 更新';
        refreshBtn.title = '困りごとを手動で更新';
        refreshBtn.addEventListener('click', async () => {
            try {
                await dataManager.refreshTeamIssues();
                showNotification('困りごとを更新しました', 'success');
            } catch (error) {
                console.error('手動更新エラー:', error);
            }
        });
        issuesHeader.appendChild(refreshBtn);
    }
}

// ページ読み込み完了後に手動更新ボタンを追加
document.addEventListener('DOMContentLoaded', () => {
    setTimeout(addManualRefreshButtons, 500);
});

// データの整合性チェック
function validateDataIntegrity() {
    const workloadData = dataManager.getData('workloadStatuses');
    const issuesData = dataManager.getData('teamIssues');

    // データの基本的な検証
    if (!Array.isArray(workloadData)) {
        console.warn('負荷状況データが配列ではありません:', workloadData);
        return false;
    }

    if (!Array.isArray(issuesData)) {
        console.warn('困りごとデータが配列ではありません:', issuesData);
        return false;
    }

    // 必須フィールドの検証
    const invalidWorkloadItems = workloadData.filter(item => 
        !item.id || !item.displayName || !item.workloadLevel
    );

    if (invalidWorkloadItems.length > 0) {
        console.warn('無効な負荷状況データ:', invalidWorkloadItems);
    }

    const invalidIssueItems = issuesData.filter(item => 
        !item.id || !item.displayName || !item.content || !item.status
    );

    if (invalidIssueItems.length > 0) {
        console.warn('無効な困りごとデータ:', invalidIssueItems);
    }

    return invalidWorkloadItems.length === 0 && invalidIssueItems.length === 0;
}

// 定期的なデータ整合性チェック
setInterval(() => {
    if (document.getElementById('dashboard').classList.contains('active')) {
        validateDataIntegrity();
    }
}, 60000); // 1分間隔

// デバッグ用の関数をグローバルスコープに追加
window.debugAPI = {
    getWorkloadData: () => dataManager.getData('workloadStatuses'),
    getIssuesData: () => dataManager.getData('teamIssues'),
    refreshAll: () => dataManager.refreshAllData(),
    validateData: validateDataIntegrity,
    getLastUpdated: (type) => dataManager.getLastUpdated(type),
    apiClient: apiClient,
    dataManager: dataManager
};

console.log('API統合とデータフロー実装が完了しました');
console.log('デバッグ用: window.debugAPI でAPIクライアントとデータマネージャーにアクセスできます');