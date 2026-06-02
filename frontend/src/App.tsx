import { Routes, Route} from 'react-router-dom'
import { ROUTES } from './constants/routes.tsx'
import { AuthProvider } from './contexts/AuthContext.tsx'
import { ProtectedRoute } from './components/layout/ProtectedRoute.tsx'
import Home from './pages/Home.tsx'
import Login from './pages/auth/Login.tsx'
import Register from './pages/auth/Register.tsx'
import SetupPin from './pages/auth/SetupPin.tsx'
import Dashboard from './pages/Dashboard.tsx'
import TransactionHistory from './pages/TransactionHistory';
import InternalTransfer from './pages/auth/InternalTransfer';
import KlikCodePage from './pages/KlikCodePage';
import AddJunior from './pages/junior/AddJunior.tsx'
import CardsPage from './pages/cards/CardsPage.tsx'
import PendingApprovals from './pages/junior/PendingApprovals.tsx'
import ManageJunior from './pages/junior/ManageJunior.tsx'


function App() {

  return (
    <AuthProvider>
      <Routes>
        <Route path={ROUTES.HOME} element={<Home/>}/>
        <Route path={ROUTES.LOGIN} element={<Login/>}/>
        <Route path={ROUTES.REGISTER} element={<Register/>}/>

        <Route path={ROUTES.SETUP_PIN} element={
          <ProtectedRoute>
            <SetupPin/>
          </ProtectedRoute>
        } />

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

        <Route path={ROUTES.ADD_JUNIOR} element={
          <ProtectedRoute>
            <AddJunior />
          </ProtectedRoute>
        } />

        <Route path={ROUTES.CARDS} element={
          <ProtectedRoute>
            <CardsPage />
          </ProtectedRoute>
        } />

        <Route path={ROUTES.JUNIOR_APPROVALS} element={
          <ProtectedRoute>
            <PendingApprovals />
          </ProtectedRoute>
        } />

        <Route path={ROUTES.MANAGE_JUNIOR} element={
          <ProtectedRoute>
            <ManageJunior />
          </ProtectedRoute>
        } />
        
      </Routes>
    </AuthProvider>
  )
}

export default App