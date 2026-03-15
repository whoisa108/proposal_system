# ESG 服務部署手冊

本文件記錄從零開始部署 ESG 系統到 K3s 叢集的完整流程，以及日常的啟動、確認、關閉操作。

---

## 架構總覽

```
GitHub repo (proposal_system)
│
├── .github/workflows/
│   ├── backend-ci.yml      → 跑測試、Trivy 掃描、CodeQL
│   ├── frontend-ci.yml     → ESLint、型別檢查、Vite build
│   └── cd-image-build.yml  → 建立 Docker image，更新 k8s/ 的 image tag
│
├── backend/   → Spring Boot (Java 17)
├── frontend/  → React + TypeScript + Vite
└── k8s/       → Kubernetes manifests (ArgoCD 監視這個資料夾)
    ├── namespace.yaml
    ├── ingress.yaml
    ├── argocd-app.yaml
    ├── backend/   (Deployment, Service, ConfigMap, SealedSecret)
    ├── frontend/  (Deployment, Service)
    ├── mongodb/   (Deployment, Service, PVC, SealedSecret)
    └── minio/     (Deployment, Service, PVC, SealedSecret)

K3s 叢集 (本機 WSL)
└── esg namespace
    ├── frontend Pod  → nginx 提供 React 靜態檔案
    ├── backend Pod   → Spring Boot API (port 8080)
    ├── mongodb Pod   → MongoDB 7 資料庫
    └── minio Pod     → MinIO 物件儲存

CI/CD 流程：
push to main → GitHub Actions CI (test) → build Docker image
→ push image to GHCR → update k8s/ image tag → ArgoCD sync → K3s 部署新版本
```

---

## 前置條件

| 工具 | 說明 | 安裝位置 |
|------|------|---------|
| K3s | 輕量 Kubernetes | WSL |
| kubectl | K8s CLI | `~/.local/bin/kubectl` |
| kubeseal | 加密 Secret | `~/.local/bin/kubeseal` |
| ArgoCD CLI | ArgoCD 操作 | `~/.local/bin/argocd` |

---

## 一次性設定（第一次建立環境時執行）

### 1. 安裝 K3s

```bash
curl -sfL https://get.k3s.io | sh -

# 設定 kubeconfig
mkdir -p ~/.kube
sudo cp /etc/rancher/k3s/k3s.yaml ~/.kube/config
sudo chown $USER ~/.kube/config

# 確認
kubectl get nodes
```

### 2. 安裝 kubectl

```bash
curl -sLo ~/.local/bin/kubectl \
  "https://dl.k8s.io/release/$(curl -sL https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
chmod +x ~/.local/bin/kubectl
```

### 3. 安裝 Sealed Secrets

```bash
# 安裝 controller（叢集內）
kubectl apply -f https://github.com/bitnami-labs/sealed-secrets/releases/latest/download/controller.yaml

# 安裝 kubeseal CLI
KUBESEAL_VERSION=$(curl -s https://api.github.com/repos/bitnami-labs/sealed-secrets/releases/latest \
  | grep '"tag_name"' | cut -d'"' -f4 | sed 's/v//')
curl -sLo /tmp/kubeseal.tar.gz \
  "https://github.com/bitnami-labs/sealed-secrets/releases/download/v${KUBESEAL_VERSION}/kubeseal-${KUBESEAL_VERSION}-linux-amd64.tar.gz"
tar -xzf /tmp/kubeseal.tar.gz -C /tmp kubeseal
mv /tmp/kubeseal ~/.local/bin/kubeseal
chmod +x ~/.local/bin/kubeseal
```

### 4. 安裝 ArgoCD

```bash
# 安裝 ArgoCD 到叢集
kubectl create namespace argocd
kubectl apply -n argocd \
  -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# 等待就緒
kubectl wait --for=condition=available deployment/argocd-server \
  -n argocd --timeout=180s

# 安裝 ArgoCD CLI
curl -sSL -o ~/.local/bin/argocd \
  https://github.com/argoproj/argo-cd/releases/latest/download/argocd-linux-amd64
chmod +x ~/.local/bin/argocd
```

### 5. 設定 Windows hosts 檔案

以系統管理員身份開啟 `C:\Windows\System32\drivers\etc\hosts`，加入：

```
172.26.183.117  esg.local
```

> 若 WSL IP 改變（重開機後可能改變），執行 `kubectl get nodes -o wide` 取得新的 INTERNAL-IP 更新此行。

### 6. 設定 GitHub

**a. Workflow 寫入權限：**
GitHub repo → Settings → Actions → General → Workflow permissions → **Read and write permissions**

**b. GHCR packages 設為 Public（第一次 CI 跑完後）：**
GitHub → Profile → Packages → `esg-backend` → Package settings → Change visibility → Public
（`esg-frontend` 同樣操作）

**c. GitHub Actions Secrets（Settings → Secrets and variables → Actions）：**

| Secret 名稱 | 說明 |
|------------|------|
| `MONGODB_URI` | MongoDB 連線字串（CI 測試用） |
| `JWT_SECRET` | JWT 簽名金鑰 |
| `MINIO_ENDPOINT` | MinIO 位址 |
| `MINIO_ACCESS_KEY` | MinIO 帳號 |
| `MINIO_SECRET_KEY` | MinIO 密碼 |
| `ADMIN_EMPLOYEE_ID` | 預設管理員工號 |
| `ADMIN_PASSWORD` | 預設管理員密碼 |
| `USER_EMPLOYEE_ID` | 預設使用者工號 |
| `USER_PASSWORD` | 預設使用者密碼 |

### 7. 產生 Sealed Secrets

> 以下指令的值對應到 `.env` 檔案中的設定。

**Backend Sealed Secret：**
```bash
JWT_SECRET="your-jwt-secret"  # pragma: allowlist secret 用 openssl rand -hex 64 產生

kubectl create secret generic backend-secret \
  --namespace esg --dry-run=client \
  --from-literal=MONGODB_URI="mongodb://admin:MONGO_PASSWORD@mongodb-svc:27017/esg?authSource=admin" \  # pragma: allowlist secret
  --from-literal=JWT_SECRET="${JWT_SECRET}" \  # pragma: allowlist secret
  --from-literal=MINIO_ACCESS_KEY="minioadmin" \  # pragma: allowlist secret
  --from-literal=MINIO_SECRET_KEY="MINIO_PASSWORD" \  # pragma: allowlist secret
  --from-literal=ADMIN_EMPLOYEE_ID="your-admin-id" \  # pragma: allowlist secret
  --from-literal=ADMIN_PASSWORD="your-admin-password" \  # pragma: allowlist secret
  --from-literal=USER_EMPLOYEE_ID="your-user-id" \  # pragma: allowlist secret
  --from-literal=USER_PASSWORD="your-user-password" \  # pragma: allowlist secret
  -o yaml | kubeseal --format=yaml > k8s/backend/sealed-secret.yaml
```

**MongoDB Sealed Secret：**
```bash
kubectl create secret generic mongodb-secret \
  --namespace esg --dry-run=client \
  --from-literal=MONGO_INITDB_ROOT_USERNAME="admin" \
  --from-literal=MONGO_INITDB_ROOT_PASSWORD="admin123" \
  --from-literal=MONGO_INITDB_DATABASE="esg" \
  -o yaml | kubeseal --format=yaml > k8s/mongodb/sealed-secret.yaml
```

**MinIO Sealed Secret：**
```bash
kubectl create secret generic minio-secret \
  --namespace esg --dry-run=client \
  --from-literal=MINIO_ROOT_USER="minioadmin" \
  --from-literal=MINIO_ROOT_PASSWORD="minioadmin123" \
  -o yaml | kubeseal --format=yaml > k8s/minio/sealed-secret.yaml
```

### 8. 建立 ArgoCD Application

```bash
kubectl apply -f k8s/argocd-app.yaml
```

---

## 日常操作

### 啟動服務

WSL 重開機後，K3s 服務應該會自動啟動（`restart: unless-stopped`）。
確認狀態：

```bash
# 確認 K3s 服務正在跑
systemctl is-active k3s

# 確認所有 Pod 正常
kubectl get pods -n esg
kubectl get pods -n argocd
```

若 K3s 沒有自動啟動：
```bash
sudo systemctl start k3s
```

**開啟 ArgoCD Web UI（每次需要操作 UI 時）：**
```bash
# 背景執行 port-forward
kubectl port-forward svc/argocd-server -n argocd 9080:443 &

# 取得 admin 密碼
kubectl -n argocd get secret argocd-initial-admin-secret \
  -o jsonpath="{.data.password}" | base64 -d && echo
```

瀏覽器開啟 `https://localhost:9080`，帳號 `admin`。

---

### 確認服務狀態

```bash
# 查看所有 esg Pod 狀態
kubectl get pods -n esg

# 預期輸出：
# NAME                      READY   STATUS    RESTARTS
# frontend-xxx              1/1     Running   0
# backend-xxx               1/1     Running   0
# mongodb-xxx               1/1     Running   0
# minio-xxx                 1/1     Running   0

# 查看 ArgoCD sync 狀態
argocd app get esg-app

# 手動觸發 ArgoCD 同步（不等 3 分鐘輪詢）
argocd app sync esg-app

# 查看 backend 即時 log
kubectl logs -n esg deployment/backend -f

# 測試 API 是否正常
curl http://esg.local/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"employeeId":"admin01","password":"Admin@123"}'
```

瀏覽器開啟 `http://esg.local` 即可使用服務。

---

### 關閉服務（不刪除資料）

只是暫停 Pod，資料（MongoDB、MinIO）保留在 PVC 裡。

```bash
# 將所有 Deployment 縮減為 0 個 replica（Pod 停止，資料保留）
kubectl scale deployment backend frontend mongodb minio \
  --replicas=0 -n esg

# 確認所有 Pod 已停止
kubectl get pods -n esg
# 應該顯示 No resources found
```

**重新啟動：**
```bash
kubectl scale deployment backend frontend mongodb minio \
  --replicas=1 -n esg
```

> **注意：** ArgoCD 的 `selfHeal=true` 會偵測到 replica 被改為 0，並在幾分鐘內自動改回 1。
> 若要讓關閉狀態持續，需要暫停 ArgoCD 同步：

```bash
# 暫停 ArgoCD 自動 sync（關閉時使用）
argocd app pause esg-app
kubectl scale deployment backend frontend mongodb minio \
  --replicas=0 -n esg

# 恢復 ArgoCD 自動 sync（重新啟動時使用）
argocd app resume esg-app
kubectl scale deployment backend frontend mongodb minio \
  --replicas=1 -n esg
```

---

### 完全關閉叢集（關電腦前）

Pod 和 K3s 的資源會在 WSL 關閉時自動停止，不需要手動操作。
下次開啟 WSL 後，K3s 和所有 Pod 會自動恢復。

若要完全停止 K3s 服務（釋放記憶體）：
```bash
sudo systemctl stop k3s
```

---

### 刪除整個 ESG 服務（清除所有資源和資料）

```bash
# 刪除整個 esg namespace（包含所有 Pod、Service、PVC、資料）
kubectl delete namespace esg

# 從 ArgoCD 移除 Application
kubectl delete -f k8s/argocd-app.yaml
```

> ⚠️ PVC 刪除後資料無法恢復，請確認不需要資料後再執行。

---

## CI/CD 完整流程說明

### 觸發條件

| Workflow | 觸發時機 |
|---------|---------|
| `backend-ci.yml` | push/PR → `backend/**` 或 `backend-ci.yml` 有變動 |
| `frontend-ci.yml` | push/PR → `frontend/**` 或 `frontend-ci.yml` 有變動 |
| `cd-image-build.yml` | push → `main` 且 `backend/**` 或 `frontend/**` 有變動 |

### 流程圖

```
開發者 push to main
        │
        ├──→ backend-ci.yml
        │       ├── detect-secrets
        │       ├── Trivy (掃 CVE)
        │       ├── CodeQL (靜態分析)
        │       └── Maven verify (測試 + JaCoCo 覆蓋率 ≥ 80%)
        │
        ├──→ frontend-ci.yml
        │       ├── detect-secrets
        │       ├── Trivy
        │       ├── CodeQL
        │       └── ESLint + tsc + vite build
        │
        └──→ cd-image-build.yml（與 CI 平行執行）
                ├── build-backend job
                │     docker build ./backend → ghcr.io/whoisa108/esg-backend:sha
                ├── build-frontend job
                │     docker build ./frontend → ghcr.io/whoisa108/esg-frontend:sha
                └── update-manifests job（等 build 完成後）
                      sed 更新 k8s/backend/deployment.yaml 的 image tag
                      sed 更新 k8s/frontend/deployment.yaml 的 image tag
                      git commit & push → "ci: update image tags to abc1234"
                                │
                                ▼
                   ArgoCD 偵測到新 commit（每 3 分鐘輪詢）
                                │
                                ▼
                   kubectl apply 更新後的 manifests
                                │
                                ▼
                   K3s Rolling Update（新 Pod 啟動 → 舊 Pod 停止）
                                │
                                ▼
                   服務更新完成，零停機時間
```

### Image 存放位置

```
ghcr.io/whoisa108/esg-backend:latest     ← 永遠指向最新版
ghcr.io/whoisa108/esg-backend:<sha>      ← 每個 commit 一個 tag（用於 rollback）
ghcr.io/whoisa108/esg-frontend:latest
ghcr.io/whoisa108/esg-frontend:<sha>
```

---

## Rollback（退版）

```bash
# 方法 1：透過 ArgoCD UI
# ArgoCD UI → esg-app → History → 選擇舊版本 → Rollback

# 方法 2：透過 git revert
git revert HEAD   # 建立一個反轉最新 commit 的新 commit
git push          # ArgoCD 自動部署回上一版

# 方法 3：透過 ArgoCD CLI
argocd app history esg-app          # 查看版本歷史
argocd app rollback esg-app <版本號>
```

---

## 常用指令速查

```bash
# Pod 狀態
kubectl get pods -n esg -w

# Pod 詳細事件（排查問題用）
kubectl describe pod <pod-name> -n esg

# 查看 log
kubectl logs -n esg deployment/backend --tail=50
kubectl logs -n esg deployment/frontend --tail=50

# ArgoCD
argocd app get esg-app              # 查看狀態
argocd app sync esg-app             # 立即同步
argocd app pause esg-app            # 暫停自動 sync
argocd app resume esg-app           # 恢復自動 sync

# 確認 image tag
kubectl get deployment backend -n esg \
  -o jsonpath='{.spec.template.spec.containers[0].image}'

# 確認 WSL IP（Windows hosts 用）
kubectl get nodes -o wide
```
