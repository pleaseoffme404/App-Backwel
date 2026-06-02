import { useState, useEffect } from 'react';

export function useTheme() {
  const [theme, setTheme] = useState<'dark' | 'light'>(() => {
    const stored = localStorage.getItem('backwel-theme');
    if (stored === 'dark' || stored === 'light') {
      return stored;
    }
    return 'dark';
  });

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('backwel-theme', theme);
  }, [theme]);

  const toggleTheme = () => {
    setTheme((prev) => (prev === 'dark' ? 'light' : 'dark'));
  };

  return { theme, toggleTheme };
}