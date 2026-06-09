import { useAuth } from '../contexts/AuthContext';
import { usePendingKlikAuthorization } from '../hooks/usePendingKlikAuthorization';
import KlikAuthorizationModal from './KlikAuthorizationModal';

export default function KlikAuthorizationWatcher() {
  const { user } = useAuth();
  const { pending, refresh } = usePendingKlikAuthorization(2000, !!user);

  if (!user || !pending) return null;

  return <KlikAuthorizationModal authorization={pending} onResolved={refresh} />;
}