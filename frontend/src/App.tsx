import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { LogProvider } from './context/LogContext'
import ProtectedRoute from './components/ProtectedRoute'
import DevPanel from './components/DevPanel'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import LobbyPage from './pages/LobbyPage'
import ClubPage from './pages/ClubPage'
import GamePage from './pages/GamePage'

export default function App() {
  return (
    <LogProvider>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route
              path="/"
              element={
                <ProtectedRoute>
                  <LobbyPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/clubs/:id"
              element={
                <ProtectedRoute>
                  <ClubPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/game/:tableId"
              element={
                <ProtectedRoute>
                  <GamePage />
                </ProtectedRoute>
              }
            />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
          <DevPanel />
        </BrowserRouter>
      </AuthProvider>
    </LogProvider>
  )
}
