# ESG 點子競賽提案網站 — 自我檢查清單

> 版本：v1.0　　更新日期：2026-03-12

---

## 身份驗證（Authentication）

| ✅ | 類別 | 方法 | 流程 | 摘要 | 備註 |
|---|------|------|------|------|------|
| ☐ | 身份驗證 | `POST /api/auth/register` | 1. `@Valid` 驗證 request body<br>2. 查詢 `employeeId` 是否已存在<br>3. BCrypt 雜湊密碼<br>4. 寫入 `users` collection<br>5. 回傳 201 | 註冊新使用者帳號 | 角色固定為 `USER`；不開放自行註冊 `ADMIN`；`employeeId` 唯一索引 |
| ☐ | 身份驗證 | `POST /api/auth/login` | 1. 以 `employeeId` 查詢使用者<br>2. BCrypt 比對密碼<br>3. 產生 JWT（sub、role、exp）<br>4. 回傳 `{ token }` | 登入並取得 JWT | 找不到使用者或密碼錯誤回傳 401；前端將 token 存入 `localStorage` |

---

## 授權（Authorization）

| ✅ | 類別 | 方法 | 流程 | 摘要 | 備註 |
|---|------|------|------|------|------|
| ☐ | 授權 — 後端 | `OncePerRequestFilter`（JWT Filter） | 1. 從 `Authorization` header 取出 Bearer token<br>2. 驗證 HMAC 簽章<br>3. 檢查 `exp` 是否過期<br>4. 將 role 注入 `SecurityContextHolder` | 每次 API 請求皆自動驗證 JWT | 公開路徑豁免：`/api/auth/**`、`/api/settings/deadline`；token 無效或過期回傳 401 |
| ☐ | 授權 — 後端 | `@PreAuthorize("hasRole('ADMIN')")` | 1. 從 JWT claim 取出 role<br>2. Spring Security 比對 annotation<br>3. role 不符回傳 403 | 限制 `/api/admin/**` 僅 `ADMIN` 可存取 | 套用於每個 Admin Controller method；一般使用者誤呼叫回傳 403 Forbidden |
| ☐ | 授權 — 前端 | React Router `<PrivateRoute>` | 1. 檢查 `localStorage` 是否有 token<br>2. 解碼 JWT 取得 role<br>3. 無 token 導向 `/login`；role 不符導向 403 或首頁 | 前端頁面依角色限制存取 | 保護所有需登入頁面；ADMIN 頁面限定 `ADMIN` role |
| ☐ | 授權 — 前端 | Axios Request Interceptor | 1. 從 `localStorage` 讀取 token<br>2. 自動附加 `Authorization: Bearer <token>` header<br>3. 收到 401 response 時清除 token 並導向 `/login` | 每次 HTTP 請求自動注入 JWT | 全域設定一次即可；統一處理 token 過期後的跳轉 |

---

## API — 公開（Public）

| ✅ | 類別 | 方法 | 流程 | 摘要 | 備註 |
|---|------|------|------|------|------|
| ☐ | 公開 API | `GET /api/settings/deadline` | 1. 查詢 `settings` collection（key = `DEADLINE`）<br>2. 回傳截止時間 | 查詢截止時間，供倒數元件使用 | 無需身份驗證；任何人皆可存取 |

---

## API — 提案（Proposal，需登入）

| ✅ | 類別 | 方法 | 流程 | 摘要 | 備註 |
|---|------|------|------|------|------|
| ☐ | 提案 API | `GET /api/proposals/my` | 1. 從 `SecurityContext` 取得目前使用者 ID<br>2. 以 `proposerId` 查詢 proposals<br>3. 回傳清單 | 查看自己的提案列表 | 無提案時回傳空陣列；不會回傳他人提案 |
| ☐ | 提案 API | `POST /api/proposals`（multipart/form-data） | 1. `@Valid` 驗證欄位<br>2. 檢查截止時間是否已過<br>3. 依規則組合 `fileName`（prefix+dept+name+employeeId+title）<br>4. 上傳檔案至 MinIO<br>5. 寫入 proposals doc<br>6. AOP 自動記錄 `CREATE_PROPOSAL` | 新增提案（含檔案上傳） | 截止後拒絕；檔案限 PDF / PPT / PPTX，≤ 5 MB；複合唯一索引：`{ employeeId, title }` |
| ☐ | 提案 API | `PUT /api/proposals/{id}` | 1. 驗證提案屬於目前使用者（proposerId 比對）<br>2. 檢查截止時間<br>3. 若有新檔：刪除 MinIO 舊檔、上傳新檔<br>4. 更新 proposal doc<br>5. AOP 記錄 `EDIT_PROPOSAL` | 編輯自己的提案（可換檔） | 需 Ownership Check；截止後拒絕；換檔時自動刪除 MinIO 舊檔 |
| ☐ | 提案 API | `DELETE /api/proposals/{id}` | 1. 驗證提案屬於目前使用者<br>2. 刪除 MinIO 檔案<br>3. 刪除 proposal doc<br>4. AOP 記錄 `DELETE_PROPOSAL` | 刪除自己的提案及檔案 | 前端需彈窗二次確認；後端執行 Ownership Check |

---

## API — 管理員（Admin，需 ADMIN 角色）

| ✅ | 類別 | 方法 | 流程 | 摘要 | 備註 |
|---|------|------|------|------|------|
| ☐ | 管理員 API | `GET /api/admin/proposals` | 1. ADMIN role 驗證<br>2. 查詢所有 proposals<br>3. 回傳清單 | 查看所有使用者的提案 | 僅 ADMIN 可存取；包含檔案下載連結 |
| ☐ | 管理員 API | `PUT /api/admin/proposals/{id}` | 1. ADMIN 驗證<br>2. 檢查截止時間<br>3. 若有新檔：刪除舊檔、上傳新檔<br>4. 更新 doc<br>5. AOP 記錄 `EDIT_PROPOSAL` | 管理員編輯任意提案 | 不需 Ownership Check；截止時間仍生效；AOP 自動寫 Audit Log |
| ☐ | 管理員 API | `DELETE /api/admin/proposals/{id}` | 1. ADMIN 驗證<br>2. 刪除 MinIO 檔案<br>3. 刪除 proposal doc<br>4. AOP 記錄 `DELETE_PROPOSAL` | 管理員刪除任意提案 | 前端需彈窗二次確認；不需 Ownership Check |
| ☐ | 管理員 API | `GET /api/admin/users` | 1. ADMIN 驗證<br>2. 查詢所有使用者<br>3. 回傳清單（排除密碼欄位） | 查看所有已註冊使用者 | 密碼欄位不得回傳；僅 ADMIN 可存取 |
| ☐ | 管理員 API | `DELETE /api/admin/users/{id}` | 1. ADMIN 驗證<br>2. 刪除使用者 doc<br>3. AOP 記錄 `DELETE_USER` | 管理員刪除使用者帳號 | 前端需彈窗二次確認；不連帶刪除該使用者的提案 |
| ☐ | 管理員 API | `PUT /api/admin/deadline` | 1. ADMIN 驗證<br>2. 驗證日期時間格式<br>3. Upsert `settings` doc（key = `DEADLINE`）<br>4. AOP 記錄 `SET_DEADLINE` | 設定提案截止時間 | 立即生效，影響所有使用者；儲存於 `settings` collection |
| ☐ | 管理員 API | `GET /api/admin/audit-logs` | 1. ADMIN 驗證<br>2. 查詢 `audit_logs` collection<br>3. 依 `timestamp` 降冪排序後回傳 | 查看系統稽核日誌 | 唯讀；由 AOP 自動寫入；僅 ADMIN 可查看 |

---

## 跨切面（Cross-Cutting）

| ✅ | 類別 | 方法 | 流程 | 摘要 | 備註 |
|---|------|------|------|------|------|
| ☐ | AOP Audit Log | `@Aspect` + `@Around` pointcut | 1. 攔截所有寫操作 API<br>2. 從 `SecurityContext` 取出操作者資訊<br>3. 記錄 action、targetId、request 摘要、IP、timestamp<br>4. 寫入 `audit_logs` collection | 自動記錄所有寫操作，無需每支 API 手動寫 Log | 涵蓋 CREATE / EDIT / DELETE；查詢操作不記錄 |
| ☐ | 全域例外處理 | `@RestControllerAdvice`（GlobalExceptionHandler） | 1. 攔截 Validation 錯誤 → 400<br>2. 攔截業務邏輯錯誤（截止後操作、重複提案等）→ 4xx<br>3. 攔截未預期錯誤 → 500<br>4. 統一回傳 `{ message }` 格式 | 集中處理所有例外，統一 error response 格式 | 所有 exception message 皆需有值；避免洩漏 stack trace |
| ☐ | Spring Validation | `@Valid` + `@NotBlank` / `@Size` 等 | 1. Controller 加上 `@Valid`<br>2. DTO 欄位加上 constraint annotation<br>3. 驗證失敗由 GlobalExceptionHandler 統一回傳 400 | 自動驗證所有 request body 欄位 | 避免空字串、超長字串等無效輸入進入 Service 層 |
