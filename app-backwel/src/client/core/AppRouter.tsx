import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import StoreLanding from '../modules/store/index';
import POSModule from '../modules/pos/index';
import AdminLayout from '../modules/admin/AdminLayout';
import Dashboard from '../modules/admin/Dashboard/index';
import SiteBuilder from '../modules/admin/SiteBuilder/index';
import UsuariosAdmin from '../modules/admin/Users/index';
import Login from '../modules/auth/index';
import AuthCallback from '../modules/auth/Callback';
import { NotFound } from '../shared/ui/NotFound';

export function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<StoreLanding />} />
        <Route path="/pos" element={<POSModule />} />
        <Route path="/login" element={<Login />} />
        <Route path="/auth/callback" element={<AuthCallback />} />
        
        <Route path="/admin" element={<AdminLayout />}>
          <Route index element={<Navigate to="/admin/dashboard" replace />} />
          <Route path="dashboard" element={<Dashboard />} />
<Route path="site-builder" element={<SiteBuilder />} />
          <Route path="usuarios" element={<UsuariosAdmin />} />
          <Route path="*" element={<NotFound />} />
        </Route>

        <Route path="*" element={<NotFound />} />
      </Routes>
    </BrowserRouter>
  );
}