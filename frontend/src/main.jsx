import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import App from './App';
import './index.css';

/**
 * Application entry point.
 *
 * StrictMode is on in development. It intentionally double-invokes effects to
 * surface missing cleanup — which is exactly the class of bug this app is exposed
 * to, between the object URLs created for QR images and the pending-link creation
 * on first sign-in. Both are written to tolerate it. StrictMode has no effect on
 * the production build.
 */
createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
