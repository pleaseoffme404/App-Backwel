import React from 'react';
import { Link } from 'react-router-dom';
import { useSession } from '../../hooks/useSession';
import { useBusiness } from '../../hooks/useBusiness';

export function Header() {
  const { session } = useSession();
  const { business } = useBusiness();
  const bName = business?.businessName || business?.business_name;
  const bLogo = business?.logoUrl || business?.logo_url;

  return (
    <header className="bg-bg-primary border-b border-brand-primary/10 sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 h-20 flex items-center justify-between">
        <Link to="/" className="text-2xl font-black text-brand-primary tracking-tight flex items-center gap-3">
          {bLogo && (
            <img src={bLogo} alt="Logo" className="h-8 w-auto object-contain" />
          )}
          {bName ? (
            <span>
              {bName.split(' ')[0]} <span className="text-accent">{bName.split(' ').slice(1).join(' ')}</span>
            </span>
          ) : (
            <span>BACKWEL <span className="text-accent">STORE</span></span>
          )}
        </Link>
        
        <nav className="hidden md:flex items-center gap-8">
          <a href="#about" className="text-sm font-bold text-text-primary/70 hover:text-brand-primary transition-colors">Nosotros</a>
          <a href="#featured" className="text-sm font-bold text-text-primary/70 hover:text-brand-primary transition-colors">Destacados</a>
          <a href="#contacto" className="text-sm font-bold text-text-primary/70 hover:text-brand-primary transition-colors">Contacto</a>
        </nav>

        <div className="flex items-center gap-4">
          <Link 
            to={session?.active ? "/admin/dashboard" : "/login"} 
            className="text-sm font-bold text-text-primary/70 hover:text-brand-primary transition-colors"
          >
            {session?.active ? `Hola, ${session.user?.name || 'Admin'}` : 'Iniciar Sesión'}
          </Link>
          <Link 
            to="/store" 
            className="px-5 py-2.5 bg-brand-primary text-bg-primary font-bold rounded-lg hover:opacity-90 transition-all active:scale-95 text-sm"
          >
            Ir a la Tienda
          </Link>
        </div>
      </div>
    </header>
  );
}