import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import StoreLanding from '../modules/store/index';
import POSModule from '../modules/pos/index';
import AdminLayout from '../modules/admin/AdminLayout';
import Dashboard from '../modules/admin/Dashboard/index';
import LandingBuilder from '../modules/admin/LandingBuilder/index';
import Login from '../modules/auth/index';
import AuthCallback from '../modules/auth/Callback';

export function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<StoreLanding />} />
        <Route path="/pos" element={<POSModule />} />
        <Route path="/login" element={<Login />} />
        <Route path="/auth/callback" element={<AuthCallback />} />
        
        <Route path="/admin" element={<AdminLayout />}>
          <Route index element={<Navigate to="dashboard" replace />} />
          <Route path="dashboard" element={<Dashboard />} />
          <Route path="landing-builder" element={<LandingBuilder />} />
          <Route path="*" element={<Navigate to="dashboard" replace />} />
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}