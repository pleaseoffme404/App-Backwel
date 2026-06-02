import { Outlet, NavLink, useLocation, Link } from 'react-router-dom';
import { useSession } from '../../shared/hooks/useSession';

export default function AdminLayout() {
  const { session, isLoading } = useSession();
  const location = useLocation();

  if (isLoading) return null;

  // Verificamos si tiene ADMIN o OWNER
  const hasAdminAccess = session?.user?.roles?.includes('ADMIN') || session?.user?.roles?.includes('OWNER');

  if (!session?.active || !hasAdminAccess) {
    return (
      <div className="h-screen w-full flex flex-col items-center justify-center bg-bg-primary text-text-primary p-8">
        <h1 className="text-3xl font-bold text-accent mb-4">🛑 Redirección Pausada (Debug)</h1>
        <p className="mb-4 opacity-80">El sistema intentó patearte porque detectó que no tienes sesión o no eres ADMIN/OWNER.</p>
        <div className="bg-bg-secondary p-6 rounded-lg border border-brand-primary/20 w-full max-w-2xl overflow-auto">
          <h2 className="font-bold mb-2">Estado actual de useSession():</h2>
          <pre className="text-sm font-mono text-brand-primary">
            {JSON.stringify(session, null, 2)}
          </pre>
        </div>
        <Link 
          to="/login" 
          state={{ from: location.pathname }}
          className="text-brand-secondary hover:text-brand-primary underline transition-colors mt-6"
        >
          Forzar redirección manual al Login
        </Link>
      </div>
    );
  }

  const baseLink = "p-3 rounded-md transition-colors";
  const activeLink = `${baseLink} bg-text-primary text-bg-primary`;
  const inactiveLink = `${baseLink} hover:bg-bg-primary`;

  return (
    <div className="flex h-screen bg-bg-primary text-text-primary">
      <aside className="w-64 border-r border-text-primary/10 bg-bg-secondary flex flex-col">
        <div className="p-6 border-b border-text-primary/10">
          <h1 className="text-xl font-bold">Admin Panel</h1>
        </div>
        <nav className="flex-1 p-4 flex flex-col gap-2">
          <NavLink to="/admin/dashboard" className={({ isActive }) => isActive ? activeLink : inactiveLink}>Dashboard</NavLink>
          <NavLink to="/admin/tienda" className={({ isActive }) => isActive ? activeLink : inactiveLink}>Tienda</NavLink>
          <NavLink to="/admin/productos" className={({ isActive }) => isActive ? activeLink : inactiveLink}>Productos</NavLink>
          <NavLink to="/admin/usuarios" className={({ isActive }) => isActive ? activeLink : inactiveLink}>Usuarios</NavLink>
          <NavLink to="/admin/configuracion" className={({ isActive }) => isActive ? activeLink : inactiveLink}>Configuración</NavLink>
          <NavLink to="/admin/landing-builder" className={({ isActive }) => isActive ? activeLink : inactiveLink}>Landing Builder</NavLink>
        </nav>
      </aside>
      <main className="flex-1 p-8 overflow-y-auto">
        <Outlet />
      </main>
    </div>
  );
}