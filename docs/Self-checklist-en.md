# ESG Idea Contest Proposal Website — Self-Checklist

> Version: v1.0　　Updated: 2026-03-12

---

## Authentication

| ✅ | Category | Method | Process | Summary | Note |
|---|----------|--------|---------|---------|------|
| ☐ | Authentication | `POST /api/auth/register` | 1. `@Valid` validate request body<br>2. Check `employeeId` uniqueness<br>3. BCrypt hash password<br>4. Save to `users` collection<br>5. Return 201 | Register a new user account | Role always defaults to `USER`; admin self-registration not allowed; `employeeId` unique index |
| ☐ | Authentication | `POST /api/auth/login` | 1. Find user by `employeeId`<br>2. BCrypt password comparison<br>3. Build JWT (sub, role, exp)<br>4. Return `{ token }` | Authenticate user and issue JWT | Return 401 if user not found or password mismatch; frontend stores token in `localStorage` |

---

## Authorization

| ✅ | Category | Method | Process | Summary | Note |
|---|----------|--------|---------|---------|------|
| ☐ | Authorization — Backend | `OncePerRequestFilter` (JWT Filter) | 1. Extract Bearer token from `Authorization` header<br>2. Verify HMAC signature<br>3. Check `exp` expiry<br>4. Inject role into `SecurityContextHolder` | Authenticate every incoming API request via JWT | Public paths exempted: `/api/auth/**`, `/api/settings/deadline`; return 401 if token invalid or expired |
| ☐ | Authorization — Backend | `@PreAuthorize("hasRole('ADMIN')")` | 1. Extract role from JWT claim<br>2. Spring Security checks annotation<br>3. Return 403 if role mismatch | Restrict `/api/admin/**` to ADMIN role only | Applied at controller method level; returns 403 Forbidden if a USER calls an admin endpoint |
| ☐ | Authorization — Frontend | React Router `<PrivateRoute>` | 1. Check token in `localStorage`<br>2. Decode JWT to get role<br>3. No token → redirect to `/login`; wrong role → redirect to 403 or home | Protect frontend pages by authentication and role | Guards all login-required pages; ADMIN pages restricted to `ADMIN` role |
| ☐ | Authorization — Frontend | Axios Request Interceptor | 1. Read token from `localStorage`<br>2. Attach `Authorization: Bearer <token>` header to every request<br>3. On 401 response → clear token and redirect to `/login` | Auto-inject JWT on every HTTP request | Configured once globally; handles token expiry redirect uniformly |

---

## API — Public

| ✅ | Category | Method | Process | Summary | Note |
|---|----------|--------|---------|---------|------|
| ☐ | Public API | `GET /api/settings/deadline` | 1. Query `settings` collection where key = `DEADLINE`<br>2. Return value | Retrieve current deadline for the countdown widget | No authentication required; accessible by anyone |

---

## API — Proposal (Login Required)

| ✅ | Category | Method | Process | Summary | Note |
|---|----------|--------|---------|---------|------|
| ☐ | Proposal API | `GET /api/proposals/my` | 1. Extract current user ID from `SecurityContext`<br>2. Query proposals by `proposerId`<br>3. Return list | List the current user's own proposals | Returns empty array if no proposals; never returns other users' proposals |
| ☐ | Proposal API | `POST /api/proposals` (multipart/form-data) | 1. `@Valid` validate fields<br>2. Check deadline has not passed<br>3. Build `fileName` (prefix + dept + name + employeeId + title)<br>4. Upload file to MinIO<br>5. Save proposal doc<br>6. AOP auto-logs `CREATE_PROPOSAL` | Create a new proposal with file attachment | Reject after deadline; file must be PDF / PPT / PPTX, ≤ 5 MB; compound unique index: `{ employeeId, title }` |
| ☐ | Proposal API | `PUT /api/proposals/{id}` | 1. Verify proposal belongs to current user (proposerId match)<br>2. Check deadline<br>3. If new file: delete old MinIO file, upload new file<br>4. Update proposal doc<br>5. AOP logs `EDIT_PROPOSAL` | Edit own proposal (file replacement supported) | Ownership check required; reject after deadline; old MinIO file deleted automatically on replacement |
| ☐ | Proposal API | `DELETE /api/proposals/{id}` | 1. Verify proposal belongs to current user<br>2. Delete file from MinIO<br>3. Delete proposal doc<br>4. AOP logs `DELETE_PROPOSAL` | Delete own proposal and associated file | Frontend must show confirmation dialog; backend performs ownership check |

---

## API — Admin (ADMIN Role Required)

| ✅ | Category | Method | Process | Summary | Note |
|---|----------|--------|---------|---------|------|
| ☐ | Admin API | `GET /api/admin/proposals` | 1. Verify ADMIN role<br>2. Query all proposals<br>3. Return list | List all proposals from all users | ADMIN only; includes file download links |
| ☐ | Admin API | `PUT /api/admin/proposals/{id}` | 1. Verify ADMIN role<br>2. Check deadline<br>3. If new file: delete old file, upload new<br>4. Update proposal doc<br>5. AOP logs `EDIT_PROPOSAL` | Admin edits any proposal | No ownership check required; deadline still enforced; AOP auto-writes audit log |
| ☐ | Admin API | `DELETE /api/admin/proposals/{id}` | 1. Verify ADMIN role<br>2. Delete file from MinIO<br>3. Delete proposal doc<br>4. AOP logs `DELETE_PROPOSAL` | Admin deletes any proposal | Frontend must show confirmation dialog; no ownership check required |
| ☐ | Admin API | `GET /api/admin/users` | 1. Verify ADMIN role<br>2. Query all users<br>3. Return list (exclude password field) | List all registered users | Password field must be excluded from response; ADMIN only |
| ☐ | Admin API | `DELETE /api/admin/users/{id}` | 1. Verify ADMIN role<br>2. Delete user doc<br>3. AOP logs `DELETE_USER` | Admin deletes a user account | Frontend must show confirmation dialog; does NOT cascade-delete the user's proposals |
| ☐ | Admin API | `PUT /api/admin/deadline` | 1. Verify ADMIN role<br>2. Validate datetime format<br>3. Upsert `settings` doc (key = `DEADLINE`)<br>4. AOP logs `SET_DEADLINE` | Set the proposal submission deadline | Takes effect immediately for all users; stored in `settings` collection |
| ☐ | Admin API | `GET /api/admin/audit-logs` | 1. Verify ADMIN role<br>2. Query `audit_logs` collection<br>3. Return sorted by `timestamp` descending | View all system audit logs | Read-only; auto-populated by AOP; ADMIN only |

---

## Cross-Cutting Concerns

| ✅ | Category | Method | Process | Summary | Note |
|---|----------|--------|---------|---------|------|
| ☐ | AOP Audit Log | `@Aspect` + `@Around` pointcut | 1. Intercept all write operation APIs<br>2. Extract operator info from `SecurityContext`<br>3. Record action, targetId, request summary, IP, timestamp<br>4. Save to `audit_logs` collection | Auto-record all write operations without manual logging in each API | Covers CREATE / EDIT / DELETE actions; read operations are not logged |
| ☐ | Global Exception Handler | `@RestControllerAdvice` (GlobalExceptionHandler) | 1. Catch validation errors → 400<br>2. Catch business logic errors (post-deadline, duplicate proposal, etc.) → 4xx<br>3. Catch unexpected errors → 500<br>4. Return unified `{ message }` format | Centralize all exception handling with consistent error response format | All exception messages must be non-null; avoid leaking stack traces |
| ☐ | Spring Validation | `@Valid` + `@NotBlank` / `@Size` etc. | 1. Add `@Valid` to Controller method<br>2. Add constraint annotations on DTO fields<br>3. Validation failure handled by GlobalExceptionHandler → 400 | Auto-validate all request body fields | Prevents empty strings, oversized inputs, etc. from reaching the Service layer |
