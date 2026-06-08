import React, { useEffect, useState } from 'react';
import { usePageConfig } from '../hooks/usePageConfig';

export function SplashScreen() {
  const [isVisible, setIsVisible] = useState(true);
  const [isFading, setIsFading] = useState(false);
  const { publishedConfig, isLoading } = usePageConfig();

  useEffect(() => {
    // Verificamos si ya vio la pantalla en esta sesión
    const hasSeenSplash = sessionStorage.getItem('backwel_splash_seen');
    if (hasSeenSplash) {
      setIsVisible(false);
      return;
    }

    const timer = setTimeout(() => {
      setIsFading(true);
      sessionStorage.setItem('backwel_splash_seen', 'true');
    }, 2200);

    const removeTimer = setTimeout(() => {
      setIsVisible(false);
    }, 2700);

    return () => { 
      clearTimeout(timer); 
      clearTimeout(removeTimer); 
    };
  }, []);

  // Si la configuración cargó y el dueño la apagó explícitamente, destruimos el componente
  if (!isLoading && publishedConfig?.theme?.enable_splash === false) {
    return null;
  }

  if (!isVisible) return null;

  return (
    <div className={`fixed inset-0 z-[99999] bg-bg-primary flex flex-col items-center justify-center transition-opacity duration-500 ${isFading ? 'opacity-0' : 'opacity-100'}`}>
      <div className="w-24 h-24 mb-8 rounded-full border-4 border-bg-secondary border-t-brand-primary border-r-accent animate-spin shadow-[0_0_40px_rgba(56,189,248,0.2)]"></div>
      
      <h1 className="text-5xl md:text-6xl font-black uppercase tracking-[0.3em] text-transparent bg-clip-text bg-gradient-to-r from-brand-primary via-accent to-brand-primary bg-[length:200%_auto] animate-text-glow ml-4">
        Backwel
      </h1>
      
      <p className="mt-8 text-brand-secondary font-mono text-sm uppercase tracking-widest animate-pulse flex items-center gap-2">
        <span className="w-2 h-2 bg-accent rounded-full inline-block"></span>
        Iniciando sistema
      </p>
    </div>
  );
}