import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';

export default function Login() {
  const location = useLocation();

  useEffect(() => {
    const from = location.state?.from || '/';
    sessionStorage.setItem('postLoginRedirect', from);
    window.location.replace('http://localhost:8080/oauth2/authorization/gateway-client');
  }, [location]);

  return (
    <div className="h-screen w-full flex flex-col items-center justify-center bg-bg-primary text-text-primary gap-4">
      <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-text-primary border-r-transparent"></div>
      <p className="text-lg font-medium opacity-80">Redirigiendo a zona segura...</p>
    </div>
  );
}