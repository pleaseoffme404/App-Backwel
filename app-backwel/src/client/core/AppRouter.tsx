import React, { Suspense } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useSession } from '../shared/hooks/useSession';
import Onboarding from '../modules/auth/Onboarding';

// Importaciones originales
import StoreLanding from '../modules/store/index';
import POSModule from '../modules/pos/index';
import AdminLayout from '../modules/admin/AdminLayout';
import Dashboard from '../modules/admin/Dashboard/index';
import SiteBuilder from '../modules/admin/SiteBuilder/index';
import ProductosAdmin from '../modules/admin/Products/index';
import UsuariosAdmin from '../modules/admin/Users/index';
import Login from '../modules/auth/index';
import AuthCallback from '../modules/auth/Callback';
import StoreCatalog from '../modules/store/Catalog/index';
import { NotFound } from '../shared/ui/NotFound';
import ConfiguracionAdmin from '../modules/admin/Configuration/index';

const ProtectedRoute = ({ children, requireAdmin = false }: { children: React.ReactNode, requireAdmin?: boolean }) => {
  const { session, isLoading } = useSession();
  
  if (isLoading) return <div className="min-h-screen bg-bg-primary flex items-center justify-center text-brand-primary font-bold">Cargando...</div>;
  if (!session?.active) return <Navigate to="/login" replace />;
  
  if (requireAdmin) {
    const isAdmin = session.user?.roles?.some(role => role.toUpperCase().includes('ADMIN'));
    if (!isAdmin) return <Navigate to="/client/dashboard" replace />;
  }

  return <>{children}</>;
};

export function AppRouter() {
  const { session } = useSession();

  if (session?.needsOnboarding) {
    return <Onboarding onComplete={() => window.location.reload()} />;
  }

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<StoreLanding />} />
        <Route path="/store" element={<StoreCatalog />} />
        <Route path="/login" element={<Login />} />
        <Route path="/auth/callback" element={<AuthCallback />} />
        
        <Route path="/admin" element={
          <ProtectedRoute requireAdmin={true}>
            <AdminLayout />
          </ProtectedRoute>
        }>
          <Route index element={<Navigate to="/admin/dashboard" replace />} />
          <Route path="dashboard" element={<Dashboard />} />
          <Route path="site-builder" element={<SiteBuilder />} />
          <Route path="productos" element={<ProductosAdmin />} />
          <Route path="usuarios" element={<UsuariosAdmin />} />
          <Route path="configuracion" element={<ConfiguracionAdmin />} />
          <Route path="*" element={<NotFound />} />
        </Route>

        <Route path="/pos" element={
          <ProtectedRoute requireAdmin={true}>
            <POSModule />
          </ProtectedRoute>
        } />

        <Route path="/client/dashboard" element={
          <ProtectedRoute>
            <div className="min-h-screen bg-bg-primary flex flex-col">
              <div className="container mx-auto p-8 mt-16">
                <h1 className="text-3xl font-black text-brand-primary">Mi Área de Cliente</h1>
                <p className="text-text-primary/70 mt-2">Historial de pedidos y configuración próximamente.</p>
              </div>
            </div>
          </ProtectedRoute>
        } />

        <Route path="*" element={<NotFound />} />
      </Routes>
    </BrowserRouter>
  );
}

export default AppRouter;