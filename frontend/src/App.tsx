import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ThemeProvider } from './context/ThemeContext';
import { Navbar } from './components/Navbar';
import { Footer } from './components/Footer';
import { ProtectedRoute } from './components/ProtectedRoute';
import { AdminRoute } from './components/AdminRoute';

import { HomePage } from './pages/HomePage';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { ForgotPasswordPage } from './pages/ForgotPasswordPage';
import { ResetPasswordPage } from './pages/ResetPasswordPage';
import { ProblemsPage } from './pages/ProblemsPage';
import { ProblemDetailPage } from './pages/ProblemDetailPage';
import { ProfilePage } from './pages/ProfilePage';
import { DashboardPage } from './pages/DashboardPage';
import { LeaderboardPage } from './pages/LeaderboardPage';
import { ContestsPage } from './pages/ContestsPage';
import { ContestArenaPage } from './pages/ContestArenaPage';
import { DuelsPage } from './pages/DuelsPage';
import { RevisionNotesPage } from './pages/RevisionNotesPage';
import { AdminDashboardPage } from './pages/admin/AdminDashboardPage';
import { AdminProblemsPage } from './pages/admin/AdminProblemsPage';
import { AdminTestCasesPage } from './pages/admin/AdminTestCasesPage';
import { AdminSupportPage } from './pages/admin/AdminSupportPage';
import { AdminContestsPage } from './pages/admin/AdminContestsPage';
import { AdminProfilePage } from './pages/admin/AdminProfilePage';
import { NotFoundPage } from './pages/NotFoundPage';

import './App.css';

export const App: React.FC = () => {
  return (
    <ThemeProvider>
      <AuthProvider>
        <BrowserRouter>
          <div className="app-container">
            <Navbar />
            <main className="main-content">
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
            </main>
            <Footer />
          </div>
        </BrowserRouter>
      </AuthProvider>
    </ThemeProvider>
  );
};

export default App;
