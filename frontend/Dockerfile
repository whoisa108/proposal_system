# ─── Stage 1: Build ───────────────────────────────────────────────────────────
# node:20-alpine has Node.js + npm. We use it to install dependencies and
# run `npm run build`, which produces the dist/ folder (HTML + JS + CSS).
FROM node:20-alpine AS builder

WORKDIR /app

# Copy package files first — same layer-caching trick as the backend.
# If package.json and package-lock.json haven't changed, Docker reuses the
# cached npm install layer (saves minutes on every rebuild).
COPY package*.json ./
RUN npm ci --legacy-peer-deps

# Copy all source files and build.
# Output lands in /app/dist/
COPY . .
RUN npm run build

# ─── Stage 2: Serve ───────────────────────────────────────────────────────────
# nginx:alpine is a minimal web server (~25MB).
# It serves the static files produced by the build stage.
FROM nginx:alpine

# Replace nginx's default config with ours.
# Our config handles React SPA routing and proxies /api calls to the backend.
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy the built React app from the builder stage.
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

# nginx starts automatically — no custom CMD needed.
