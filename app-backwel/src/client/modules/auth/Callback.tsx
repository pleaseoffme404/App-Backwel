import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useSession } from '../../shared/hooks/useSession';

export default function AuthCallback() {
  const { session, isLoading } = useSession();
  const navigate = useNavigate();
  const [isReady, setIsReady] = useState(false);

  useEffect(() => {
    const timer = setTimeout(() => setIsReady(true), 1200);
    return () => clearTimeout(timer);
  }, []);

useEffect(() => {
    if (!isLoading && isReady) {
      if (!session?.active) {
        window.location.href = 'http://localhost:8080/login';
        return;
      }

      const target = sessionStorage.getItem('postLoginRedirect') || '/';
      sessionStorage.removeItem('postLoginRedirect');

      const hasAdminAccess = session?.user?.roles?.includes('ADMIN') || session?.user?.roles?.includes('OWNER');

      if (target.startsWith('/admin') && !hasAdminAccess) {
        navigate('/', { replace: true });
        return;
      } else {
        navigate(target, { replace: true });
      }
    }
  }, [isLoading, isReady, session, navigate]);

  return (
    <div className="h-screen w-full flex flex-col items-center justify-center bg-bg-primary text-text-primary gap-4">
      <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-brand-primary border-r-transparent"></div>
      <p className="text-lg font-medium opacity-80">Completando inicio de sesión...</p>
    </div>
  );
}