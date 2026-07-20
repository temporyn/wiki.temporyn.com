# Temporyn-Wiki — Architecture

A single-user, self-hosted wiki built on **Spring Boot + Thymeleaf** with a
**TipTap (Notion-style) WYSIWYG editor**. Content is stored as plain Markdown
files in a filesystem "vault", so documents remain fully compatible with
external tools such as Obsidian.

---

## 1. High-level overview

```
Browser (Thymeleaf shell + vanilla JS + TipTap bundle)
   │  HTML pages (/, /view/**, /edit/**, /login)
   │  REST/JSON (/api/**), media (/media/**)
   ▼
Spring Boot (controllers → services → VaultPathResolver → filesystem)
   ▼
Vault directory  (folders = sections, *.md = articles, .assets/ = images)
```

- **Storage model**: the vault is a directory tree. Folders are sections and
  `*.md` files are articles. Uploaded images live in a hidden `.assets/` folder
  so they travel with the vault but never appear in the navigation tree.
- **Rendering model**: viewing and editing share one pipeline. Both load the raw
  Markdown and render it client-side with TipTap (`editable:false` for view,
  `editable:true` for edit). There is no server-side Markdown-to-HTML step.
- **Auth**: a single admin account (form login, session cookie) with an optional
  TOTP second factor. Every route except static assets and `/login` requires the
  `ADMIN` role.

---

## 2. Backend structure

Package root: `com.temporyn.wiki`

### 2.1 Configuration
| Class | Responsibility |
|-------|----------------|
| `WikiApplication` | Spring Boot entry point. |
| `config.SecurityConfig` | Form login, public paths (`/assets/**`, `/lib/**`, `/login`, `/error`, `/favicon.ico`), the in-memory admin user (BCrypt hash from env), the TOTP authentication provider, and the auth success/failure handlers. |

### 2.1.1 Security package (`security`)
| Class | Responsibility |
|-------|----------------|
| `TotpValidator` | RFC 6238 TOTP verification (HMAC-SHA1, 6 digits, 30s step, +-1 drift), Base32 secret, no external deps. |
| `TotpAuthenticationProvider` | Extends `DaoAuthenticationProvider`; after the password check, requires a valid TOTP code when `app.admin.totp-secret` is set (skipped when blank). |
| `TotpAuthenticationException` | Internal marker for a TOTP-stage failure so the log can record the stage while the user response stays generic. |
| `TotpWebAuthenticationDetails` | Captures the login form `code` parameter into the authentication details. |
| `AuthEventLogger` | Writes structured auth events to the dedicated `AUTH_EVENT` logger (per-day rolling file). Never logs passwords, TOTP codes/secret, cookies, or tokens. |
| `AuthSuccessHandler` / `AuthFailureHandler` | Log the success/failure event (with failing stage) around the redirect; failures always redirect to a generic `/login?error`. |

### 2.2 Controllers (thin HTTP layer)
| Class | Type | Routes | Delegates to |
|-------|------|--------|--------------|
| `PageController` | MVC | `GET /`, `GET /view/**`, `GET /edit/**`, `POST /edit/**`, `GET /login` | `VaultTreeService`, `ArticleService` |
| `VaultNodeController` | REST | `POST /api/directories`, `/api/articles` (+ `/rename`, `/move`, `/delete`), `POST /api/files` (upload) + `/api/files/rename`, `/move`, `/delete` | `VaultNodeService` |
| `MediaController` | REST | `POST /api/images`, `GET /media/**`, `POST /api/media/cleanup` | `MediaService` |
| `FileController` | REST | `GET /download/**` (attachment download of any vault file) | `VaultPathResolver` |
| `SearchController` | REST | `GET /api/search?q=` | `SearchService` |

Controllers only parse/validate the HTTP shape and format the response; all
domain logic lives in services.

### 2.3 Services (domain layer)
| Class | Responsibility |
|-------|----------------|
| `VaultPathResolver` (`@Component`) | **Single source of truth for path resolution and security.** Owns the vault root, resolves article/directory/new-child/existing-file paths, guarantees every path stays within the root (no traversal), validates names, and builds view URLs. All other services depend on it. |
| `VaultTreeService` | Scans the vault into the `DirectoryNode` tree for the sidebar (folders first, alphabetical). Markdown files are openable; other file types are listed and served as downloads (`/download/**`) but not opened as documents. |
| `ArticleService` | Reads and writes a single article's Markdown; derives title/breadcrumb. |
| `VaultNodeService` | Structural mutations: create / rename / move / delete for both folders and articles (including recursive folder delete). |
| `MediaService` | Stores uploaded images under `.assets/`, resolves them for serving, and removes orphans not referenced by any document. |
| `SearchService` | Case-insensitive title + content search with single-line snippets; skips dot-folders. |

**Why this split:** previously a single `VaultService` handled tree, articles,
structure, media, search, and path security together (a God object). Each
concern is now an independent service sharing the `VaultPathResolver` for path
safety, so responsibilities are clear and individually testable.

### 2.4 DTOs (`dto`)
| Record | Purpose |
|--------|---------|
| `DirectoryNode` | A folder node: name, path, article count, child folders, articles. |
| `ArticleLink` | A sidebar file entry: title, path, view URL, and an `openable` flag (`true` for `.md`, `false` for other file types). |
| `ArticleContent` | An article's raw Markdown plus title/breadcrumb (used for both view and edit). |
| `SearchResult` | A search hit: title, path, url, snippet. |

---

## 3. Frontend structure

Served from `src/main/resources/static/` and `templates/`.

### 3.1 Templates (Thymeleaf)
| File | Purpose |
|------|---------|
| `templates/index.html` | The app shell: topbar, sidebar (search + tree), and the main pane that switches between **view** (`#tiptap-view`), **edit** (`#tiptap-editor` + hidden `#content`), and placeholder. Also hosts the reusable modal (`#app-modal`) and context menu (`#context-menu`). |
| `templates/login.html` | Login form. |
| `templates/fragments/tree.html` | Recursive fragment rendering folder nodes. |

### 3.2 Scripts (`static/assets/js`)
| File | Responsibility |
|------|----------------|
| `site.js` | Theme toggle, sidebar tree state (expand/active/persist), server-backed search (debounced), mobile sidebar. |
| `tree-menu.js` | Sidebar interactions: CSRF POST helper, modal dialogs (prompt/confirm/alert replacing native popups), create/rename/delete, expand/collapse, right-click context menu (viewport-aware), drag & drop move, and the orphan-image cleanup trigger. |
| `tiptap-editor.js` | Boots the TipTap bundle in edit or read-only mode, wires image upload (with CSRF), and adds heading anchors + smooth scroll in the view. |

### 3.3 TipTap bundle
The editor is built locally (no CDN) and committed under
`static/lib/tiptap/{version}/`.

- **Source**: `frontend/tiptap/` (esbuild → single IIFE bundle).
  - `src/index.js` — `createNotionEditor({element, content, editable, uploadImage})`, exposed as `window.TemporynTiptap`. Configures StarterKit, tables, task lists, links, images, Markdown import/export, placeholder, bubble menu, and slash command (edit mode only).
  - `src/slash-command.js` — `/` block menu (headings, lists, checklist, quote, code, table, image upload, …).
  - `src/table-menu.js` — in-table floating toolbar (add/remove row & column, header, merge, delete table).
  - `src/bundle.css` — structural popup styling.
- **Build**: `cd frontend/tiptap && node build.mjs` → writes `static/lib/tiptap/{version}/tiptap.bundle.{js,css}`.
- **Storage format**: TipTap ↔ Markdown via `tiptap-markdown`, keeping `.md` files portable. (Table column widths are not representable in Markdown and are therefore not persisted.)

### 3.4 Styles (`static/assets/css`)
| File | Purpose |
|------|---------|
| `temporyn.css` | Global theme (light/dark variables), layout, sidebar, search, context menu, modal, document (`.doc-body`) typography incl. GFM task-list checkboxes. |
| `editor.css` | TipTap editing surface, bubble/slash/table menus, drag handle, read-only view tweaks. |

---

## 4. Feature map

| Feature | Frontend | Backend |
|---------|----------|---------|
| Browse tree | `site.js` (tree) + `fragments/tree.html` | `VaultTreeService` |
| View article | `tiptap-editor.js` (read-only) | `PageController` → `ArticleService` |
| Edit / save article | `tiptap-editor.js` (editable) + form POST | `PageController` → `ArticleService` |
| Create / rename / move / delete | `tree-menu.js` (context menu, drag & drop, modals) | `VaultNodeController` → `VaultNodeService` |
| Image upload | `tiptap-editor.js` + slash "image" | `MediaController` → `MediaService` |
| Orphan image cleanup | `tree-menu.js` (header ↻ button) | `MediaController` → `MediaService` |
| Search (title + content) | `site.js` (debounced) | `SearchController` → `SearchService` |
| Theme toggle | `site.js` | — (localStorage) |
| Auth | `login.html` | `SecurityConfig` |

---

## 5. Request flows

**View** `GET /view/{path}` → `PageController.view` → `ArticleService.load`
(raw Markdown) + `VaultTreeService.buildTree` → `index.html` → `tiptap-editor.js`
renders read-only TipTap.

**Save** editor serializes to Markdown into hidden `#content` → `POST /edit/{path}`
→ `ArticleService.save` → redirect to `/view/{path}`.

**Image upload** slash "image" picks a file → `POST /api/images` (multipart, CSRF)
→ `MediaService.store` writes `.assets/{uuid}.ext` → returns `/media/...` → inserted
into the document.

**Search** keystroke (200 ms debounce) → `GET /api/search?q=` → `SearchService.search`
walks `*.md`, matches title/content → flat 1-depth result list in the sidebar.

---

## 6. Security notes

- **Path traversal**: every request-derived path goes through `VaultPathResolver`,
  which normalizes and asserts the result stays under the vault root.
- **Uploads**: images only (png/jpg/gif/webp), 10 MB limit; SVG is rejected to avoid script injection. Served with a long immutable cache header.
- **CSRF**: enabled; the frontend reads the token from meta tags for all mutating requests.
- **Auth**: all non-static routes require the `ADMIN` role.
- **TOTP (2FA)**: when `app.admin.totp-secret` (Base32) is set, login also requires a
  valid RFC 6238 code (`security.TotpAuthenticationProvider` + `TotpValidator`, +-1 step
  drift). When the secret is blank the TOTP step is skipped (convenient for local dev).
- **Auth logging**: every login success/failure is written to a dedicated, per-day
  rolling file (`logs/auth/auth-YYYY-MM-DD.log`) via the `AUTH_EVENT` logger
  (`logback-spring.xml`), separate from the app log. Each event is a single
  `key="value"` line: event id, UTC + KST time, account, result, failure stage,
  source/forwarded IP, user agent, path, device-cookie hash, and placeholders for
  fields pending later work (new IP/device, rate limit, auto-block, mail status).
  Secrets (password, TOTP code/secret, cookies, tokens) are never logged. Override
  the directory with `LOG_DIR`. IPv4-mapped IPv6 addresses are normalized to IPv4.
- **Reverse proxy / TLS**: TLS is terminated by Nginx; the `prod` profile sets
  `server.forward-headers-strategy=framework` so the app sees the real client IP
  (via `X-Forwarded-For`) and treats requests as HTTPS (via `X-Forwarded-Proto`).
  Session cookies are `Secure`, `HttpOnly`, `SameSite=Lax`. Security headers
  (HSTS, `X-Content-Type-Options`, `X-Frame-Options=SAMEORIGIN`, `Referrer-Policy`)
  are set in `SecurityConfig`; HSTS only appears on HTTPS requests.

---

## 7. Build & run

| Task | Command |
|------|---------|
| Backend build | `./gradlew build` |
| Compile only | `./gradlew compileJava processResources` |
| Run (dev) | `./gradlew bootRun` (profile `local` serves static/templates from source with caching off) |
| Rebuild editor bundle | `cd frontend/tiptap && node build.mjs` |

**Required configuration** (`application.properties` / env):
`app.content.dir` (vault path), `app.admin.username`, `app.admin.password-hash`
(BCrypt), and the optional `app.admin.totp-secret` (Base32; blank disables 2FA). Profiles: `local` (no caching, live reload) and `prod` (Thymeleaf cache on, forwarded-header handling, secure session cookies, binds `0.0.0.0:8080` for containers).
