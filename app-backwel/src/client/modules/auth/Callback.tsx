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
        console.error("AuthCallback: Sesión inactiva tras login. Revisa la pestaña Network.");
        return;
      }

      const target = sessionStorage.getItem('postLoginRedirect') || '/';
      sessionStorage.removeItem('postLoginRedirect');

      // Validamos ambos roles aquí también
      const hasAdminAccess = session?.user?.roles?.includes('ADMIN') || session?.user?.roles?.includes('OWNER');

      if (target.startsWith('/admin') && !hasAdminAccess) {
        console.error("AuthCallback: Usuario autenticado pero sin rol suficiente intentó entrar a /admin.");
        return;
      } else {
        navigate(target, { replace: true });
      }
    }
  }, [isLoading, isReady, session, navigate]);

  if (!isLoading && isReady && !session?.active) {
    return (
      <div className="h-screen w-full flex flex-col items-center justify-center bg-bg-primary text-text-primary p-8">
        <h1 className="text-2xl text-accent font-bold mb-4">Error en Callback: Sesión no cargada</h1>
        <p className="mb-4">El Gateway devolvió el token y las cookies, pero la petición de Express a Java falló.</p>
        <pre className="bg-bg-secondary p-4 rounded text-sm text-brand-primary border border-brand-primary/20 mb-6">
          {JSON.stringify(session, null, 2)}
        </pre>
        <button onClick={() => navigate('/')} className="underline hover:text-brand-primary">Volver al inicio manualmente</button>
      </div>
    );
  }

  return (
    <div className="h-screen w-full flex flex-col items-center justify-center bg-bg-primary text-text-primary gap-4">
      <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-brand-primary border-r-transparent"></div>
      <p className="text-lg font-medium opacity-80">Completando inicio de sesión...</p>
    </div>
  );
}