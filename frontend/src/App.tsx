import { Routes, Route} from 'react-router-dom'
import { ROUTES } from './constants/routes.tsx'
import { AuthProvider } from './contexts/AuthContext.tsx'
import { ProtectedRoute } from './components/layout/ProtectedRoute.tsx'
import Home from './pages/Home.tsx'
import Login from './pages/auth/Login.tsx'
import Register from './pages/auth/Register.tsx'
import Dashboard from './pages/Dashboard.tsx'
import TransactionHistory from './pages/TransactionHistory';
import InternalTransfer from './pages/auth/InternalTransfer';
import KlikCodePage from './pages/KlikCodePage';


function App() {

  return (
    <AuthProvider>
      <Routes>
        <Route path={ROUTES.HOME} element={<Home/>}/>
        <Route path={ROUTES.LOGIN} element={<Login/>}/>
        <Route path={ROUTES.REGISTER} element={<Register/>}/>
        
        <Route path={ROUTES.DASHBOARD} element = {
          <ProtectedRoute>
            <Dashboard/>
        </ProtectedRoute>
        } />

        <Route path={ROUTES.INTERNAL_TRANSFER} element={
          <ProtectedRoute>
            <InternalTransfer />
          </ProtectedRoute>
        } />

        <Route path={ROUTES.TRANSACTION_HISTORY} element={
          <ProtectedRoute>
            <TransactionHistory />
          </ProtectedRoute>
        } />

        <Route path={ROUTES.KLIK_CODE} element={
          <ProtectedRoute>
            <KlikCodePage />
          </ProtectedRoute>
        } />
      </Routes>
    </AuthProvider>
  )
}

export default App
