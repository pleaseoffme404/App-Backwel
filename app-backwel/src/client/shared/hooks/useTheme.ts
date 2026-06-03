import { useState, useEffect, useCallback } from 'react';

const defaultTheme: Record<string, string> = {
  dark_bg_primary: '#0F172A',
  dark_bg_secondary: '#1E293B',
  dark_brand_primary: '#38BDF8',
  dark_brand_secondary: '#7DD3FC',
  dark_accent: '#F97316',
  dark_text_primary: '#F8FAFC',
  light_bg_primary: '#F4F7FE',
  light_bg_secondary: '#FFFFFF',
  light_brand_primary: '#0A3C51',
  light_brand_secondary: '#126385',
  light_accent: '#E85D04',
  light_text_primary: '#1A202C'
};

let cachedConfig: Record<string, string> | null = null;
let isFetching = false;
const listeners = new Set<() => void>();

export function useTheme() {
  const [theme, setTheme] = useState<'dark' | 'light'>(() => {
    return (localStorage.getItem('backwel-theme') as 'dark' | 'light') || 'dark';
  });

  const applyVariables = useCallback((currentTheme: 'dark' | 'light', config: Record<string, string>) => {
    const root = document.documentElement;
    root.setAttribute('data-theme', currentTheme);
    const vars = ['bg-primary', 'bg-secondary', 'brand-primary', 'brand-secondary', 'accent', 'text-primary'];
    
    vars.forEach(v => {
      const key = `${currentTheme}_${v.replace('-', '_')}`;
      root.style.setProperty(`--${v}`, config[key] || defaultTheme[key]);
    });
  }, []);

  const fetchTheme = useCallback(async () => {
    if (isFetching) return;
    isFetching = true;
    try {
      const res = await fetch('/api/config/page');
      if (res.ok) {
        const data = await res.json();
        cachedConfig = data.theme || {};
        listeners.forEach(listener => listener());
      }
    } finally {
      isFetching = false;
    }
  }, []);

  useEffect(() => {
    const update = () => applyVariables(theme, cachedConfig || {});
    listeners.add(update);
    
    if (!cachedConfig) fetchTheme();
    else update();
    
    return () => { listeners.delete(update); };
  }, [theme, applyVariables, fetchTheme]);

  const toggleTheme = () => {
    setTheme(prev => {
      const next = prev === 'dark' ? 'light' : 'dark';
      localStorage.setItem('backwel-theme', next);
      return next;
    });
  };

  return { theme, toggleTheme, refreshTheme: fetchTheme };
}