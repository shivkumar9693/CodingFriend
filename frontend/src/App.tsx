import React, { Suspense } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ThemeProvider } from './context/ThemeContext';
import { Navbar } from './components/Navbar';
import { Footer } from './components/Footer';
import { ProtectedRoute } from './components/ProtectedRoute';
import { AdminRoute } from './components/AdminRoute';
import { LoadingSpinner } from './components/LoadingSpinner';

// Fast critical-path static imports
import { HomePage } from './pages/HomePage';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';

// Route-level code-splitting for high performance and lightweight bundle loading
const ProblemsPage = React.lazy(() => import('./pages/ProblemsPage').then((m) => ({ default: m.ProblemsPage })));
const ProblemDetailPage = React.lazy(() => import('./pages/ProblemDetailPage').then((m) => ({ default: m.ProblemDetailPage })));
const ProfilePage = React.lazy(() => import('./pages/ProfilePage').then((m) => ({ default: m.ProfilePage })));
const DashboardPage = React.lazy(() => import('./pages/DashboardPage').then((m) => ({ default: m.DashboardPage })));
const LeaderboardPage = React.lazy(() => import('./pages/LeaderboardPage').then((m) => ({ default: m.LeaderboardPage })));
const ContestsPage = React.lazy(() => import('./pages/ContestsPage').then((m) => ({ default: m.ContestsPage })));
const ContestArenaPage = React.lazy(() => import('./pages/ContestArenaPage').then((m) => ({ default: m.ContestArenaPage })));
const DuelsPage = React.lazy(() => import('./pages/DuelsPage').then((m) => ({ default: m.DuelsPage })));
const RevisionNotesPage = React.lazy(() => import('./pages/RevisionNotesPage').then((m) => ({ default: m.RevisionNotesPage })));
const ForgotPasswordPage = React.lazy(() => import('./pages/ForgotPasswordPage').then((m) => ({ default: m.ForgotPasswordPage })));
const ResetPasswordPage = React.lazy(() => import('./pages/ResetPasswordPage').then((m) => ({ default: m.ResetPasswordPage })));
const NotFoundPage = React.lazy(() => import('./pages/NotFoundPage').then((m) => ({ default: m.NotFoundPage })));

// Admin lazy-loaded bundle
const AdminDashboardPage = React.lazy(() => import('./pages/admin/AdminDashboardPage').then((m) => ({ default: m.AdminDashboardPage })));
const AdminProblemsPage = React.lazy(() => import('./pages/admin/AdminProblemsPage').then((m) => ({ default: m.AdminProblemsPage })));
const AdminTestCasesPage = React.lazy(() => import('./pages/admin/AdminTestCasesPage').then((m) => ({ default: m.AdminTestCasesPage })));
const AdminSupportPage = React.lazy(() => import('./pages/admin/AdminSupportPage').then((m) => ({ default: m.AdminSupportPage })));
const AdminContestsPage = React.lazy(() => import('./pages/admin/AdminContestsPage').then((m) => ({ default: m.AdminContestsPage })));
const AdminProfilePage = React.lazy(() => import('./pages/admin/AdminProfilePage').then((m) => ({ default: m.AdminProfilePage })));

import './App.css';

const PageFallback: React.FC = () => (
  <div style={{ minHeight: '60vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
    <LoadingSpinner size="md" message="Loading..." />
  </div>
);

export const App: React.FC = () => {
  return (
    <ThemeProvider>
      <AuthProvider>
        <BrowserRouter>
          <div className="app-container">
            <Navbar />
            <main className="main-content">
              <Suspense fallback={<PageFallback />}>
                <Routes>
                {/* Public Routes */}
                <Route path="/" element={<HomePage />} />
                <Route path="/login" element={<LoginPage />} />
                <Route path="/register" element={<RegisterPage />} />
                <Route path="/forgot-password" element={<ForgotPasswordPage />} />
                <Route path="/reset-password" element={<ResetPasswordPage />} />
                <Route path="/leaderboard" element={<LeaderboardPage />} />

                {/* Protected Coding, Competitive & Learning Routes */}
                <Route
                  path="/problems"
                  element={
                    <ProtectedRoute>
                      <ProblemsPage />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/problems/:slug"
                  element={
                    <ProtectedRoute>
                      <ProblemDetailPage />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/contests"
                  element={
                    <ProtectedRoute>
                      <ContestsPage />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/contests/:slug"
                  element={
                    <ProtectedRoute>
                      <ContestArenaPage />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/duels"
                  element={
                    <ProtectedRoute>
                      <DuelsPage />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/duels/:roomCode"
                  element={
                    <ProtectedRoute>
                      <DuelsPage />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/dashboard"
                  element={
                    <ProtectedRoute>
                      <DashboardPage />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/profile"
                  element={
                    <ProtectedRoute>
                      <ProfilePage />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/notes"
                  element={
                    <ProtectedRoute>
                      <RevisionNotesPage />
                    </ProtectedRoute>
                  }
                />
                {/* Redirect legacy submissions routes to Dashboard & Admin Hub */}
                <Route path="/submissions" element={<Navigate to="/dashboard" replace />} />

                {/* Admin Routes */}
                <Route
                  path="/admin"
                  element={
                    <AdminRoute>
                      <AdminDashboardPage />
                    </AdminRoute>
                  }
                />
                <Route
                  path="/admin/problems"
                  element={
                    <AdminRoute>
                      <AdminProblemsPage />
                    </AdminRoute>
                  }
                />
                <Route
                  path="/admin/problems/:problemId/test-cases"
                  element={
                    <AdminRoute>
                      <AdminTestCasesPage />
                    </AdminRoute>
                  }
                />
                <Route
                  path="/admin/contests"
                  element={
                    <AdminRoute>
                      <AdminContestsPage />
                    </AdminRoute>
                  }
                />
                <Route
                  path="/admin/submissions"
                  element={<Navigate to="/admin" replace />}
                />
                <Route
                  path="/admin/support"
                  element={
                    <AdminRoute>
                      <AdminSupportPage />
                    </AdminRoute>
                  }
                />
                <Route
                  path="/admin/profile"
                  element={
                    <AdminRoute>
                      <AdminProfilePage />
                    </AdminRoute>
                  }
                />

                {/* Fallback & Not Found */}
                <Route path="/404" element={<NotFoundPage />} />
                <Route path="*" element={<NotFoundPage />} />
              </Routes>
            </Suspense>
          </main>
          <Footer />
          </div>
        </BrowserRouter>
      </AuthProvider>
    </ThemeProvider>
  );
};

export default App;
