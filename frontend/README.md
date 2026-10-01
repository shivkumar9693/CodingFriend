# AI Coding Mentor - Frontend Application

The modern, responsive web application foundation for **AI Coding Mentor**, built with React 18, TypeScript, Vite, and React Router.

---

## 🚀 Overview

The frontend interface provides comprehensive user workflows for both **Students** and **Administrators**:

### Student Capabilities:
- Register and authenticate securely with JWT Bearer tokens and automatic silent token refresh.
- Browse, search, and filter Java coding problems by difficulty (`EASY`, `MEDIUM`, `HARD`) and concept tags.
- Solve challenges in an interactive Monaco Editor split-view with local autosave, keyboard shortcuts, starter code reset, and syntax highlighting.
- Submit solutions to the backend pipeline (`POST /api/submissions`) with real-time status polling (`QUEUED`, `RUNNING`, `EVALUATING`, `COMPLETED`, `FAILED`).
- Review personal submission histories and inspection modal on `/submissions` and in-problem tabs.
- Manage personal learner profiles (full name, bio, GitHub handle) on `/profile`.

### Administrator Capabilities:
- Role-based access control with `AdminRoute` protecting all `/admin/*` routes.
- **Admin Dashboard** (`/admin`): High-level platform telemetry (problem count, total submissions, active runtime) and live submission stream.
- **Problem Catalog Management** (`/admin/problems`): Create, edit, and delete problems with slug auto-generation, difficulty badges, and starter code templates.
- **Test Case Management** (`/admin/problems/:problemId/test-cases`): Curate public sample test suites and hidden grading test cases with sequence ordering.
- **Submission Auditing** (`/admin/submissions`): Inspect learner submitted code, runtime status, and error logs platform-wide.

---

## 🛠️ Technology Stack

| Layer | Technology |
|---|---|
| **Code Editor** | [Monaco Editor](https://microsoft.github.io/monaco-editor/) (`@monaco-editor/react`) |
| **Framework** | [React 18](https://react.dev/) + [TypeScript](https://www.typescriptlang.org/) |
| **Build Tool & Dev Server** | [Vite 5](https://vitejs.dev/) |
| **Client Routing** | [React Router v6](https://reactrouter.com/) |
| **Icons** | [Lucide React](https://lucide.dev/) |
| **Styling** | Vanilla CSS Design System (hsl tokens, dark mode, glassmorphism, responsive grid) |
| **Testing** | [Vitest](https://vitest.dev/) |
| **Deployment Target** | [Vercel](https://vercel.com/) / Static Hosting |

---

## 📁 Directory Structure

```
frontend/
├── public/                  # Static public assets & favicon
├── src/
│   ├── components/          # Reusable UI components
│   │   ├── CodeEditor.tsx   # Monaco Editor integration with Java formatting & shortcuts
│   │   ├── EditorPanel.tsx  # Interactive editor workspace (Autosave, Reset, Run, Submit)
│   │   ├── Navbar.tsx       # Responsive navigation with role badge & mobile drawer
│   │   ├── Footer.tsx       # Standard platform footer
│   │   ├── ProtectedRoute.tsx # Route authentication guard
│   │   ├── AdminRoute.tsx   # Admin role-based route guard (ROLE_ADMIN)
│   │   ├── Modal.tsx        # Responsive reusable modal dialog
│   │   ├── StatusBadge.tsx  # Submission status pill badge
│   │   ├── LoadingSpinner.tsx # SVG animated spinner
│   │   ├── ErrorMessage.tsx # Dismissible alert banner
│   │   └── DifficultyBadge.tsx # Styled pill badges for problem difficulty
│   ├── context/
│   │   └── AuthContext.tsx  # React Context for authentication and user profile state
│   ├── pages/
│   │   ├── HomePage.tsx     # Landing page with feature highlights & CTA
│   │   ├── LoginPage.tsx    # Login form with validation and auto-redirect
│   │   ├── RegisterPage.tsx # Registration form
│   │   ├── ProblemsPage.tsx # Problem catalog with search, difficulty, and tag filters
│   │   ├── ProblemDetailPage.tsx # Problem statement, test cases, and Monaco editor
│   │   ├── SubmissionsHistoryPage.tsx # Learner submission history & code modal
│   │   ├── ProfilePage.tsx  # Protected learner profile management
│   │   ├── admin/
│   │   │   ├── AdminDashboardPage.tsx # Admin control center & live telemetry
│   │   │   ├── AdminProblemsPage.tsx  # Problem catalog CRUD & modal editor
│   │   │   ├── AdminTestCasesPage.tsx # Public & hidden test case curation
│   │   │   └── AdminSubmissionsPage.tsx # Platform submission audits & inspector
│   │   └── NotFoundPage.tsx # 404 page handler
│   ├── services/
│   │   ├── apiClient.ts     # Central HTTP client with auth headers and 401 token refresh
│   │   ├── authService.ts   # Login, registration, token refresh, and logout APIs
│   │   ├── problemService.ts # Problem catalog and detail API endpoints
│   │   ├── submissionService.ts # Code submission and polling APIs
│   │   ├── adminService.ts  # Admin problem and test case CRUD endpoints
│   │   └── userService.ts   # Learner profile retrieval and update endpoints
│   ├── types/               # TypeScript data models and DTO interfaces
│   │   ├── auth.ts
│   │   ├── problem.ts
│   │   ├── submission.ts
│   │   ├── admin.ts
│   │   └── user.ts
│   ├── App.css              # Component styling and layout styles
│   ├── App.test.ts          # Automated unit & integration tests
│   ├── App.tsx              # Root component & route tree
│   ├── config.ts            # Environment variable resolver
│   ├── index.css            # Base design system, CSS variables, and reset
│   ├── main.tsx             # DOM entrypoint
│   └── vite-env.d.ts        # Vite environment variable definitions
├── .env.example             # Example environment configuration
├── package.json             # NPM dependencies and scripts
├── tsconfig.json            # TypeScript configuration
├── vercel.json              # Vercel SPA routing rewrite configuration
└── vite.config.ts           # Vite bundler configuration
```

---

## ⚙️ Environment Configuration

The application uses Vite environment variables prefixed with `VITE_`:

| Variable | Description | Default Local Value |
|---|---|---|
| `VITE_API_BASE_URL` | Base URL of the AI Coding Mentor Spring Boot backend | `http://localhost:8081` |

### Setup `.env`
Create a `.env` file in the `frontend/` directory:
```bash
cp .env.example .env
```
Ensure `VITE_API_BASE_URL` points to your active backend instance.

---

## 🏃 Getting Started (Local Development)

### Prerequisites
- Node.js 18+ or 20+
- npm 9+

### 1. Install Dependencies
```bash
npm install
```

### 2. Start Development Server
```bash
npm run dev
```
The application will start at `http://localhost:5173`.

### 3. Run Automated Tests
```bash
npm run test
```

### 4. Build for Production
```bash
npm run build
```
Production assets are compiled into the `dist/` directory.

---

## 🌐 Deploying to Vercel

The frontend is fully pre-configured for zero-friction deployment to Vercel.

### Vercel Routing Configuration (`vercel.json`)
Single Page Applications (SPAs) using HTML5 `pushState` routing require all non-static asset routes to rewrite to `index.html`. This is pre-configured in `frontend/vercel.json`:
```json
{
  "rewrites": [
    {
      "source": "/(.*)",
      "destination": "/index.html"
    }
  ]
}
```

### Deployment Steps:

#### Option A: Deploy via Vercel Web Dashboard (Recommended)
1. Push your repository to GitHub / GitLab / Bitbucket.
2. Log in to [Vercel](https://vercel.com) and click **"Add New Project"**.
3. Import your repository.
4. Set the **Root Directory** to `frontend`.
5. Under **Environment Variables**, add:
   - `VITE_API_BASE_URL`: `https://your-backend-domain.com` (or your deployed backend URL)
6. Click **Deploy**.

#### Option B: Deploy via Vercel CLI
```bash
npm i -g vercel
cd frontend
vercel
```
When prompted for production deployment:
```bash
vercel --prod
```
Set `VITE_API_BASE_URL` in the project settings on Vercel or via `vercel env add VITE_API_BASE_URL`.

---

## 🔒 Security & Best Practices
- **Token Handling**: JWT Bearer tokens and refresh tokens are managed securely in client storage and injected through `apiClient.ts`.
- **Zero Hidden Test Exposure**: The frontend solely consumes public sample test cases (`is_sample=true`), ensuring hidden grading test cases remain confidential on the server.
- **Route Protection**: The `ProtectedRoute` component guards authenticated routes (`/profile`) against unauthorized access.
