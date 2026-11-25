import { createRoot } from 'react-dom/client';
import App from './App.tsx';
import { FirestoreErrorBoundary } from './components/FirestoreErrorBoundary';
import './index.css';

// Tier 1 Case A Defense: Detect Maps JS auth/quota errors before mounting
declare global {
  interface Window {
    gm_authFailure?: () => void;
  }
}

window.gm_authFailure = () => {
  window.dispatchEvent(new CustomEvent('gmp-quota-exceeded'));
};

const origConsoleError = console.error.bind(console);
console.error = (...args: unknown[]) => {
  const text = args
    .map((arg) => (typeof arg === 'string' ? arg : String(arg)))
    .join(' ');
  if (
    text.includes('OverQuotaMapError') ||
    text.includes('QuotaExceededError') ||
    text.includes('You have exceeded your daily request quota')
  ) {
    window.dispatchEvent(new CustomEvent('gmp-quota-exceeded'));
  }
  origConsoleError(...args);
};

createRoot(document.getElementById('root')!).render(
  <FirestoreErrorBoundary>
    <App />
  </FirestoreErrorBoundary>
);
