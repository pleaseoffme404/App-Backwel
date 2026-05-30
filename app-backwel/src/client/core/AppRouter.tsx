import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import StoreLanding from '../modules/store/index';
import POSModule from '../modules/pos/index';
import LandingBuilder from '../modules/admin/LandingBuilder/index';

export function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<StoreLanding />} />
        <Route path="/pos" element={<POSModule />} />
        <Route path="/admin/landing-builder" element={<LandingBuilder />} />
      </Routes>
    </BrowserRouter>
  );
}