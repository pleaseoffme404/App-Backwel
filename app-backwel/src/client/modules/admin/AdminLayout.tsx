import { Outlet, NavLink, useLocation, Link, Navigate } from 'react-router-dom';
import { useSession } from '../../shared/hooks/useSession';
import { useTheme } from '../../shared/hooks/useTheme';
import { useState, useEffect } from 'react';
import defaultAvatar from '../../shared/assets/avatar-default.png';
import { useBusiness } from '../../shared/hooks/useBusiness';

export default function AdminLayout() {
  const { session, isLoading } = useSession();
const { theme, toggleTheme } = useTheme();
  const { business } = useBusiness();
  const bName = business?.businessName || business?.business_name;
  const location = useLocation();
  const [isCollapsed, setIsCollapsed] = useState(false);
  const [isLocked, setIsLocked] = useState(localStorage.getItem('backwel_admin_locked') === 'true');
  const [unlockPassword, setUnlockPassword] = useState('');
  const [unlockError, setUnlockError] = useState('');
useEffect(() => {
    const checkLock = () => setIsLocked(localStorage.getItem('backwel_admin_locked') === 'true');
    window.addEventListener('storage', checkLock);
    return () => window.removeEventListener('storage', checkLock);
  }, []);

  const handleUnlock = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const res = await fetch('/api/v1/auth/unlock', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ password: unlockPassword })
      });
      if (res.ok) {
        localStorage.removeItem('backwel_admin_locked');
        setIsLocked(false);
        setUnlockError('');
        setUnlockPassword('');
      } else {
        setUnlockError('Contraseña incorrecta');
      }
    } catch (error) {
      setUnlockError('Error de red al verificar');
    }
  };

  if (isLoading) return null;

const hasAdminAccess = session?.user?.roles?.includes('ADMIN') || session?.user?.roles?.includes('OWNER');

 if (!session?.active || !hasAdminAccess) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  }

  if (isLocked) {
    return (
      <div className="h-screen w-full flex flex-col items-center justify-center bg-bg-primary text-text-primary p-8">
        <div className="bg-bg-secondary p-8 rounded-2xl border border-brand-primary/20 max-w-md w-full shadow-2xl flex flex-col items-center text-center">
          <img src={session?.user?.avatar_url || defaultAvatar} alt="Avatar" className="w-24 h-24 rounded-full mb-4 border-4 border-bg-primary shadow-lg object-cover bg-bg-primary" />
          <h2 className="text-2xl font-black mb-1 text-text-primary">{session?.user?.name} {session?.user?.surname}</h2>
          <p className="opacity-70 mb-8 font-medium">Panel de Administración Bloqueado</p>
          
          <form onSubmit={handleUnlock} className="w-full flex flex-col gap-4">
            <input 
              type="password" 
              value={unlockPassword} 
              onChange={e => setUnlockPassword(e.target.value)} 
              placeholder="Ingresa tu contraseña..." 
              className="w-full bg-bg-primary border border-brand-primary/20 p-3 rounded-lg focus:border-brand-primary focus:ring-1 focus:ring-brand-primary focus:outline-none text-center text-lg tracking-widest text-text-primary" 
              autoFocus 
            />
            {unlockError && <p className="text-red-500 text-sm font-bold">{unlockError}</p>}
            <button 
              type="submit" 
              disabled={!unlockPassword}
              className="w-full py-3 bg-accent text-white font-bold rounded-lg hover:opacity-90 disabled:opacity-50 transition-opacity uppercase tracking-wider shadow-lg"
            >
              Desbloquear Panel
            </button>
          </form>
          
          <Link to="/pos" className="mt-6 text-sm text-text-primary/50 hover:text-brand-primary font-bold transition-colors">
            ← Volver al Punto de Venta
          </Link>
        </div>
      </div>
    );
  }

  const baseLink = "p-3 rounded-md transition-all flex items-center gap-3 overflow-hidden whitespace-nowrap group";
  const activeLink = `${baseLink} bg-text-primary text-bg-primary`;
  const inactiveLink = `${baseLink} hover:bg-bg-primary`;

  return (
    <div className="flex h-screen bg-bg-primary text-text-primary">
      <aside className={`${isCollapsed ? 'w-20' : 'w-64'} transition-all duration-300 border-r border-text-primary/10 bg-bg-secondary flex flex-col shrink-0 z-50`}>
        <div className="p-4 border-b border-text-primary/10 h-20 flex items-center justify-between overflow-hidden">
          {!isCollapsed && <h1 className="text-xl font-bold tracking-tight shrink-0">Admin Panel</h1>}
          <button 
            onClick={() => setIsCollapsed(!isCollapsed)}
            className="p-2 rounded-lg hover:bg-bg-primary text-text-primary/70 hover:text-brand-primary transition-colors mx-auto shrink-0"
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
            </svg>
          </button>
        </div>
        <nav className="flex-1 p-4 flex flex-col gap-2 overflow-y-auto overflow-x-hidden">
          <NavLink to="/admin/dashboard" className={({ isActive }) => isActive ? activeLink : inactiveLink} title="Dashboard">
            <svg className="w-6 h-6 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z" /></svg>
            {!isCollapsed && <span>Dashboard</span>}
          </NavLink>
          <NavLink to="/admin/tienda" className={({ isActive }) => isActive ? activeLink : inactiveLink} title="Tienda">
            <svg className="w-6 h-6 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z" /></svg>
            {!isCollapsed && <span>Tienda</span>}
          </NavLink>
          <NavLink to="/pos" className={({ isActive }) => isActive ? activeLink : inactiveLink} title="Punto de Venta">
            <svg className="w-6 h-6 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 7h6m0 10v-3m-3 3h.01M9 17h.01M9 14h.01M12 14h.01M15 11h.01M12 11h.01M9 11h.01M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z" /></svg>
            {!isCollapsed && <span>Punto de Venta</span>}
          </NavLink>
          <NavLink to="/admin/productos" className={({ isActive }) => isActive ? activeLink : inactiveLink} title="Productos">
            <svg className="w-6 h-6 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" /></svg>
            {!isCollapsed && <span>Productos</span>}
          </NavLink>
          <NavLink to="/admin/usuarios" className={({ isActive }) => isActive ? activeLink : inactiveLink} title="Usuarios">
            <svg className="w-6 h-6 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" /></svg>
            {!isCollapsed && <span>Usuarios</span>}
          </NavLink>
          <NavLink to="/admin/configuracion" className={({ isActive }) => isActive ? activeLink : inactiveLink} title="Configuración">
            <svg className="w-6 h-6 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" /><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /></svg>
            {!isCollapsed && <span>Configuración</span>}
          </NavLink>
          <NavLink to="/admin/site-builder" className={({ isActive }) => isActive ? activeLink : inactiveLink} title="Site Builder">
            <svg className="w-6 h-6 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 21a4 4 0 01-4-4V5a2 2 0 012-2h4a2 2 0 012 2v12a4 4 0 01-4 4zm0 0h12a2 2 0 002-2v-4a2 2 0 00-2-2h-2.343M11 7.343l1.657-1.657a2 2 0 012.828 0l2.829 2.829a2 2 0 010 2.828l-8.486 8.485M7 17h.01" /></svg>
            {!isCollapsed && <span>Site Builder</span>}
          </NavLink>
        </nav>
        
        <div className="p-4 border-t border-text-primary/10 flex flex-col items-center gap-2">
          <button 
            onClick={toggleTheme}
            className="w-full flex items-center justify-center p-2 rounded-lg hover:bg-bg-primary text-text-primary/70 hover:text-brand-primary transition-colors"
            title={theme === 'dark' ? 'Cambiar a Modo Claro' : 'Cambiar a Modo Oscuro'}
          >
            {theme === 'dark' ? (
              <svg className="w-6 h-6 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" /></svg>
            ) : (
              <svg className="w-6 h-6 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" /></svg>
            )}
            {!isCollapsed && <span className="ml-3 font-medium">Tema {theme === 'dark' ? 'Oscuro' : 'Claro'}</span>}
          </button>

          <button 
            onClick={() => {
              window.location.href = 'http://localhost:8080/logout';
            }}
            className="w-full flex items-center justify-center p-2 rounded-lg hover:bg-red-500/10 text-text-primary/70 hover:text-red-500 transition-colors"
            title="Cerrar Sesión"
          >
            <svg className="w-6 h-6 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" /></svg>
            {!isCollapsed && <span className="ml-3 font-medium">Cerrar Sesión</span>}
          </button>
        </div>
      </aside>
      
      <main className="flex-1 p-8 overflow-y-auto min-w-0 flex flex-col">
        <div className="flex-1">
          <Outlet />
        </div>
        <footer className="mt-8 pt-4 border-t border-text-primary/10 text-center text-xs opacity-50 font-medium shrink-0">
          {bName || 'Backwel Software Solutions'} &copy; {new Date().getFullYear()}
        </footer>
      </main>
    </div>
  );
}