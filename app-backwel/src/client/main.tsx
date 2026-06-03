import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { AppRouter } from './core/AppRouter';
import { useTheme } from './shared/hooks/useTheme';
import './index.css';

function ThemeProvider({ children }: { children: React.ReactNode }) {
  useTheme();
  return <>{children}</>;
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ThemeProvider>
      <AppRouter />
    </ThemeProvider>
  </StrictMode>
);