import React from 'react';
import ReactDOM from 'react-dom/client';
import { useTheme } from './shared/hooks/useTheme';
import './index.css';

const App = () => {
  useTheme();

  return (
    <div>
      <h1>Backwel UI</h1>
    </div>
  );
};

ReactDOM.createRoot(document.getElementById('root') as HTMLElement).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);