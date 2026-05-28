import React from 'react';
import ReactDOM from 'react-dom/client';
import { useTheme } from './shared/hooks/useTheme';
import { AppRouter } from './core/AppRouter';
import './index.css';

const App = () => {
  useTheme();

  return <AppRouter />;
};

ReactDOM.createRoot(document.getElementById('root') as HTMLElement).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);