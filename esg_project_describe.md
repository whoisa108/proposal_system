# ESG 點子競賽提案網站 — 規劃文件

> 版本：v1.2　　更新日期：2026-03-05

---

## 一、Tech Stack

| 層級 | 技術 |
|------|------|
| Frontend | React + Vite (Node 22) |
| Backend | Java 17 + Spring Boot 3.5（MVC 架構：Controller → Service → Repository） |
| Database | MongoDB (Docker) |
| File Storage | MinIO (Docker) |
| 容器管理 | Docker Compose |

---

## 二、使用套件

### Backend

| 套件 | 用途 |
|------|------|
| **Lombok** | `@Data` `@Builder` `@RequiredArgsConstructor` 自動產生 getter/setter/constructor，省去大量模板程式碼 |
| **Spring Security + JWT (jjwt)** | 登入驗證、JWT 發行與解析 |
| **Spring AOP** | `@Aspect` 攔截 API，自動寫 Audit Log，不需每支 API 手動記錄 |
| **Spring Validation** | `@Valid` + `@NotBlank` `@Size` 等，自動驗證 request body |
| **MinIO Java SDK** | 檔案上傳／刪除操作 |

### Frontend

| 套件 | 用途 |
|------|------|
| **React Hook Form** | 表單管理，比逐一 useState 省很多程式碼 |
| **Zod** | 搭配 React Hook Form 做前端驗證 schema |
| **Axios** | HTTP client，統一設定 JWT header interceptor |
| **TanStack Query (React Query)** | API 狀態管理，省去手寫 loading / error state |
| **React Router v6** | 路由 + Route Guard |
| **dayjs** | 截止時間倒數計算 |
| **Tailwind CSS** | 基本 class 排版，白底黑字經典風格 |

---

## 三、Enum 定義

### 部門 (department)
```
AAID | BSID | ICSD | TSID | PLED | PEID
```

### 點子類別 (category)
```
酷炫點子獎 | 卓越影響獎
```

### 點子五大方向 (direction)
```
綠色製造 | 建立責任供應鏈 | 打造健康共榮職場 | 培育人才 | 關懷弱勢
```

### 檔名 Prefix 對應
| category | prefix | 範例 |
|----------|--------|------|
| 酷炫點子獎 | `I` | `I_AAID_王大陸_14554_綠能水.pdf` |
| 卓越影響獎 | `O` | `O_BSID_王立文_14554_菁英能量.pptx` |

---

## 四、角色與權限

### 角色
| 角色 | 說明 |
|------|------|
| `ADMIN` | 系統預設一個 admin 帳號（seed data），不開放註冊 |
| `USER` | 一般使用者，即提案人（隊長） |

### 權限矩陣
| 功能 | USER（本人） | ADMIN |
|------|-------------|-------|
| 查看自己的提案 | ✅ | — |
| 新增提案 | ✅（截止前） | — |
| 編輯提案 | ✅（截止前） | ✅（截止前） |
| 刪除自己的提案 | ✅ | — |
| 查看所有提案 | ❌ | ✅ |
| 刪除任意提案 | ❌ | ✅ |
| 編輯任意提案 | ❌ | ✅（截止前） |
| 查看所有使用者 | ❌ | ✅ |
| 刪除使用者 | ❌ | ✅ |
| 設定截止時間 | ❌ | ✅ |
| 查看 Audit Log | ❌ | ✅ |

> 所有刪除動作皆需前端二次確認彈窗，後端不做 Ownership Check

---

## 五、DB Schema (MongoDB)

### `users` collection
```json
{
  "_id": "ObjectId",
  "employeeId": "14554",
  "name": "王大陸",
  "department": "AAID",
  "password": "<bcrypt hash>",
  "role": "USER | ADMIN",
  "createdAt": "ISODate"
}
```
- `employeeId`：唯一索引，不得重複

---

### `proposals` collection
```json
{
  "_id": "ObjectId",
  "proposerId": "ObjectId",
  "employeeId": "14554",
  "proposerName": "王大陸",
  "department": "AAID",
  "category": "酷炫點子獎",
  "direction": "綠色製造",
  "title": "綠能水",
  "summary": "...",
  "fileName": "I_AAID_王大陸_14554_綠能水.pdf",
  "filePath": "<MinIO object key>",
  "teammates": [
    { "name": "李小明", "employeeId": "12345" }
  ],
  "createdAt": "ISODate",
  "updatedAt": "ISODate"
}
```
- 複合唯一索引：`{ employeeId, title }` → 防止同一人重複提案名稱
- 隊友：0–4 人，純文字輸入，不驗證是否為已註冊使用者
- 隊長（提案人）不限只參加 1 隊

---

### `settings` collection
```json
{
  "_id": "ObjectId",
  "key": "DEADLINE",
  "value": "2025-12-31T23:59:59Z"
}
```

---

### `audit_logs` collection
```json
{
  "_id": "ObjectId",
  "operatorId": "14554",
  "operatorName": "王大陸",
  "action": "CREATE_PROPOSAL | EDIT_PROPOSAL | DELETE_PROPOSAL | DELETE_USER | SET_DEADLINE | ...",
  "targetId": "<ObjectId or string>",
  "detail": "<request 摘要 JSON string>",
  "ip": "192.168.1.1",
  "timestamp": "ISODate"
}
```
- 記錄所有寫操作（新增、編輯、刪除）
- 查看操作不記錄
- 僅 ADMIN 可查看

---

## 六、API 設計

### Auth（Public）
| Method | Path | 說明 |
|--------|------|------|
| POST | `/api/auth/register` | 註冊 |
| POST | `/api/auth/login` | 登入，回傳 JWT |

### Proposal（需登入）
| Method | Path | 說明 | 備註 |
|--------|------|------|------|
| GET | `/api/proposals/my` | 查看自己的提案 | |
| POST | `/api/proposals` | 新增提案（含檔案 multipart） | 截止後拒絕 |
| PUT | `/api/proposals/{id}` | 編輯自己的提案（含換檔） | 截止後拒絕 |
| DELETE | `/api/proposals/{id}` | 刪除自己的提案 | |

### Admin（需 ADMIN 角色）
| Method | Path | 說明 |
|--------|------|------|
| GET | `/api/admin/proposals` | 查看所有提案 |
| PUT | `/api/admin/proposals/{id}` | 編輯任意提案（含換檔） |
| DELETE | `/api/admin/proposals/{id}` | 刪除任意提案 |
| GET | `/api/admin/users` | 查看所有使用者 |
| DELETE | `/api/admin/users/{id}` | 刪除使用者 |
| PUT | `/api/admin/deadline` | 設定截止時間 |
| GET | `/api/admin/audit-logs` | 查看 Audit Log |

### Public
| Method | Path | 說明 |
|--------|------|------|
| GET | `/api/settings/deadline` | 查詢截止時間（供倒數元件使用） |

---

## 七、身份驗證架構

```
1. 前端 Route Guard
   └─ 無 token → 導向 /login
   └─ role 不符 → 導向 403 或首頁

2. Spring Security Filter（每支 API）
   └─ 驗 JWT signature + expiry
   └─ 注入 SecurityContext（含 role）
   └─ ADMIN 路由限定 ADMIN role
```

---

## 八、前端頁面規劃

| 路徑 | 頁面 | 權限 |
|------|------|------|
| `/login` | 登入 | Public |
| `/register` | 註冊 | Public |
| `/proposals` | 我的提案列表 + 截止倒數 | USER |
| `/proposals/new` | 新增提案 | USER |
| `/proposals/:id/edit` | 編輯自己的提案 | USER |
| `/admin/proposals` | 所有提案管理 | ADMIN |
| `/admin/proposals/:id/edit` | 編輯任意提案 | ADMIN |
| `/admin/users` | 使用者管理 | ADMIN |
| `/admin/deadline` | 截止時間設定 | ADMIN |
| `/admin/audit-logs` | Audit Log 查看 | ADMIN |

---

## 九、前端 UI 風格

- 白底、黑字、灰色邊框
- 標準 HTML 表單樣式
- 無動畫、無漸層、無花俏效果
- Tailwind CSS 基本 class（`border`, `rounded`, `px-4`, `py-2` 等）
- 表格使用基本 `<table>` 樣式

---

## 十、業務規則整理

| 規則 | 說明 |
|------|------|
| 工號唯一 | 同一工號不得重複註冊 |
| 提案不得重複 | 同一提案人 + 相同點子名稱不得重複提案 |
| 截止後禁止操作 | 截止時間後，前端禁止新增/編輯，後端 API 同樣拒絕 |
| 編輯換檔 | 重新上傳檔案時，MinIO 舊檔自動刪除 |
| 刪除二次確認 | 所有刪除動作皆需彈窗二次確認 |
| 隊友不驗證 | 隊友姓名 + 工號為純文字輸入，不查詢 DB |
| 隊長多隊 | 同一人可以是多個提案的隊長 |
| 檔案格式 | 僅接受 PDF、PPT/PPTX，大小 ≤ 5 MB |

---

## 十一、Backend MVC 架構

```
Controller  ← 接收 HTTP request，呼叫 Service，回傳 response
Service     ← 業務邏輯（截止時間檢查、檔名組合、MinIO 操作）
Repository  ← MongoDB CRUD（繼承 MongoRepository）
```

每個模組一組：`XxxController` → `XxxService` → `XxxRepository`

---

## 十二、開發順序建議

```
1. Docker Compose — MongoDB + MinIO 環境建立
2. Backend
   ├── 專案初始化 + 共用設定（Security, JWT, Exception Handler）
   ├── Auth API（register / login）
   ├── Proposal API（CRUD + MinIO 上傳）
   ├── Admin API（proposals / users / deadline）
   └── AOP Audit Log
3. Frontend
   ├── 登入 / 註冊
   ├── 提案列表 + 倒數元件
   ├── 提案新增 / 編輯表單
   └── 管理後台
```