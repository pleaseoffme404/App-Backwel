import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import StoreLanding from '../modules/store/index';
import POSModule from '../modules/pos/index';

export function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<StoreLanding />} />
        <Route path="/pos" element={<POSModule />} />
      </Routes>
    </BrowserRouter>
  );
}