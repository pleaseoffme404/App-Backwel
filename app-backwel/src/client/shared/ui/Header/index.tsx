import React, { useState, useEffect } from 'react';
import { useBusiness } from '../../hooks/useBusiness';
import { useSession } from '../../hooks/useSession';
import { Skeleton } from '../Skeleton';
import defaultAvatar from '../../assets/avatar-default.png';

export function Header() {
  const { business, isLoading: isBusinessLoading } = useBusiness();
  const { session, isLoading: isSessionLoading } = useSession();
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [unreadNotifications, setUnreadNotifications] = useState(0);

  useEffect(() => {
    if (session?.active) {
      fetch('/api/v1/notifications/unread')
        .then((res) => res.json())
        .then((data) => setUnreadNotifications(data.count || 0))
        .catch(() => {});
    }
  }, [session]);

  const handleLogout = async () => {
    try {
      await fetch('/api/v1/auth/logout', { method: 'POST' });
      window.location.reload();
    } catch (error) {
      window.location.reload();
    }
  };

  return (
    <header className="h-20 w-full bg-bg-secondary border-b border-brand-primary/20 px-6 flex items-center justify-between relative z-50">
      <div className="flex items-center gap-4">
        {isBusinessLoading ? (
          <>
            <Skeleton className="w-10 h-10 rounded-full" />
            <Skeleton className="w-48 h-6" />
          </>
        ) : (
          <>
            {business?.logo_url && (
              <img src={business.logo_url} alt="Logo" className="w-10 h-10 rounded-md object-cover" />
            )}
            <span className="text-xl font-bold text-text-primary tracking-tight">
              {business?.business_name || 'BACKWEL'}
            </span>
          </>
        )}
      </div>

      <div className="flex items-center gap-6">
        {session?.active && (
          <button className="relative p-2 text-text-primary hover:text-brand-primary transition-colors">
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
            </svg>
            {unreadNotifications > 0 && (
              <span className="absolute top-1 right-1 w-2.5 h-2.5 bg-accent rounded-full border-2 border-bg-secondary"></span>
            )}
          </button>
        )}

        <button className="p-2 text-text-primary hover:text-brand-primary transition-colors">
          <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z" />
          </svg>
        </button>

        {isSessionLoading ? (
          <Skeleton className="w-24 h-10 rounded-lg" />
        ) : session?.active ? (
          <div className="relative">
            <button 
              onClick={() => setIsMenuOpen(!isMenuOpen)}
              className="flex items-center gap-2 border border-brand-primary/20 p-1 pr-3 rounded-full hover:bg-bg-primary transition-colors"
            >
              <img 
                src={session.user?.avatar_url || defaultAvatar} 
                alt="Avatar" 
                className="w-8 h-8 rounded-full object-cover bg-bg-primary"
              />
              <svg className="w-4 h-4 text-text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
              </svg>
            </button>

            {isMenuOpen && (
              <div className="absolute right-0 mt-2 w-56 bg-bg-secondary border border-brand-primary/20 rounded-xl shadow-lg py-2 flex flex-col">
                <div className="px-4 py-2 border-b border-brand-primary/10">
                  <span className="block text-sm font-medium text-text-primary truncate">{session.user?.name}</span>
                  <span className="block text-xs text-brand-secondary font-bold mt-1">
                    {session.user?.credits?.toFixed(2)} Créditos
                  </span>
                </div>
                <button className="text-left px-4 py-2 text-sm text-text-primary hover:bg-brand-primary/10 hover:text-brand-secondary transition-colors">
                  Pedidos
                </button>
                <button className="text-left px-4 py-2 text-sm text-text-primary hover:bg-brand-primary/10 hover:text-brand-secondary transition-colors">
                  Área de cliente
                </button>
                <div className="border-t border-brand-primary/10 my-1"></div>
                <button 
                  onClick={handleLogout}
                  className="text-left px-4 py-2 text-sm text-accent hover:bg-accent/10 font-medium transition-colors"
                >
                  Cerrar sesión
                </button>
              </div>
            )}
          </div>
        ) : (
          <a 
            href="http://localhost:8080/login" 
            className="bg-brand-primary text-white px-5 py-2 rounded-lg font-bold text-sm tracking-wide hover:bg-brand-secondary transition-all active:scale-95"
          >
            Iniciar Sesión
          </a>
        )}
      </div>
    </header>
  );
}