import React from 'react';
import { Link } from 'react-router-dom';

export default function StoreLanding() {
  return (
    <div className="min-h-screen bg-bg-primary flex flex-col items-center justify-center p-6 text-center">
      <h1 className="text-6xl font-black text-text-primary tracking-tighter mb-4">
        BACKWEL <span className="text-brand-secondary">COMMERCE</span>
      </h1>
      <p className="text-xl text-text-primary/70 max-w-2xl mb-12">
        Plataforma unificada de gestión comercial, inventario y punto de venta.
      </p>
      
      <div className="flex flex-col sm:flex-row gap-6">
        <Link 
          to="/pos" 
          className="bg-brand-primary text-white px-8 py-4 rounded-xl font-bold uppercase tracking-wider hover:bg-brand-secondary transition-all active:scale-95 shadow-lg flex items-center justify-center gap-2"
        >
          Abrir Punto de Venta
        </Link>
        <button 
          className="bg-bg-secondary border border-brand-primary/20 text-text-primary px-8 py-4 rounded-xl font-bold uppercase tracking-wider hover:border-brand-primary/80 transition-all active:scale-95 shadow-sm"
        >
          Portal Admin
        </button>
      </div>
    </div>
  );
}