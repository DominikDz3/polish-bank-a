import { Routes, Route } from 'react-router-dom'
import { ROUTES } from './constants/routes.tsx'
import { AuthProvider } from './contexts/AuthContext.tsx'
import { ProtectedRoute } from './components/layout/ProtectedRoute.tsx'
import KlikAuthorizationWatcher from './components/KlikAuthorizationWatcher'
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
import SwiftTransfer from './pages/SwiftTransfer.tsx'
import ExternalTransfer from './pages/auth/ExternalTransfer.tsx'
import KlikP2PTransfer from './pages/KlikP2PTransfer.tsx'
import Settings from './pages/Settings.tsx'
import AmlAdmin from './pages/AmlAdmin.tsx'
import AmlHolds from './pages/AmlHolds.tsx'



function App() {
  return (
    <AuthProvider>
      <KlikAuthorizationWatcher />
      <Routes>
        <Route path={ROUTES.HOME} element={<Home/>}/>
        <Route path={ROUTES.LOGIN} element={<Login/>}/>
        <Route path={ROUTES.REGISTER} element={<Register/>}/>
        <Route path={ROUTES.SETUP_PIN} element={
          <ProtectedRoute>
            <SetupPin />
          </ProtectedRoute>
        }/>
        <Route path={ROUTES.DASHBOARD} element={
          <ProtectedRoute>
            <Dashboard/>
          </ProtectedRoute>
        }/>
        <Route path={ROUTES.INTERNAL_TRANSFER} element={
          <ProtectedRoute>
            <InternalTransfer />
          </ProtectedRoute>
        } />
        <Route path={ROUTES.SWIFT_TRANSFER} element={
          <ProtectedRoute>
            <SwiftTransfer />
          </ProtectedRoute>
        }/>
      
        <Route path={ROUTES.TRANSACTION_HISTORY} element={
          <ProtectedRoute>
            <TransactionHistory />
          </ProtectedRoute>
        }/>
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

        <Route path={ROUTES.EXTERNAL_TRANSFER} element={
          <ProtectedRoute>
            <ExternalTransfer />
          </ProtectedRoute>
        }/>

        <Route path={ROUTES.KLIK_P2P} element={
          <ProtectedRoute>
            <KlikP2PTransfer />
          </ProtectedRoute>
        } />

        <Route path={ROUTES.SETTINGS} element={
          <ProtectedRoute>
            <Settings />
          </ProtectedRoute>
        } />

        <Route path={ROUTES.AML_HOLDS} element={
          <ProtectedRoute>
            <AmlHolds />
          </ProtectedRoute>
        } />

        <Route path={ROUTES.AML_ADMIN} element={
          <ProtectedRoute>
            <AmlAdmin />
          </ProtectedRoute>
        } />

      </Routes>
    </AuthProvider>
  )
}

export default App