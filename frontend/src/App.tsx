import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { Navbar } from './components/Navbar';
import { Footer } from './components/Footer';
import { ProtectedRoute } from './components/ProtectedRoute';
import { AdminRoute } from './components/AdminRoute';

import { HomePage } from './pages/HomePage';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { ProblemsPage } from './pages/ProblemsPage';
import { ProblemDetailPage } from './pages/ProblemDetailPage';
import { ProfilePage } from './pages/ProfilePage';
import { DashboardPage } from './pages/DashboardPage';
import { SubmissionsHistoryPage } from './pages/SubmissionsHistoryPage';
import { AdminDashboardPage } from './pages/admin/AdminDashboardPage';
import { AdminProblemsPage } from './pages/admin/AdminProblemsPage';
import { AdminTestCasesPage } from './pages/admin/AdminTestCasesPage';
import { AdminSubmissionsPage } from './pages/admin/AdminSubmissionsPage';
import { NotFoundPage } from './pages/NotFoundPage';

import './App.css';

export const App: React.FC = () => {
  return (
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

              {/* Protected Coding & Learning Routes */}
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
                path="/submissions"
                element={
                  <ProtectedRoute>
                    <SubmissionsHistoryPage />
                  </ProtectedRoute>
                }
              />

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
                path="/admin/submissions"
                element={
                  <AdminRoute>
                    <AdminSubmissionsPage />
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
  );
};

export default App;
