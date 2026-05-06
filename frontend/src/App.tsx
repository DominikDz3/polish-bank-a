import { Routes, Route} from 'react-router-dom'
import { ROUTES } from './constants/routes.tsx'
import Home from './pages/Home.tsx'
import Login from './pages/auth/Login.tsx'
import Register from './pages/auth/Register.tsx'
import Dashboard from './pages/Dashboard.tsx'
import { AuthProvider } from './contexts/AuthContext.tsx'
import { ProtectedRoute } from './components/layout/ProtectedRoute.tsx'
import InternalTransfer from './pages/auth/InternalTransfer';


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
      </Routes>
    </AuthProvider>
  )
}

export default App
