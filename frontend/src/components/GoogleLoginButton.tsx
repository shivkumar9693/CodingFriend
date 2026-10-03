import React, { useEffect, useRef, useState } from 'react';
import { Loader2 } from 'lucide-react';

declare global {
  interface Window {
    google?: {
      accounts: {
        id: {
          initialize: (config: {
            client_id: string;
            callback: (response: { credential: string }) => void;
            auto_select?: boolean;
            cancel_on_tap_outside?: boolean;
          }) => void;
          renderButton: (
            parent: HTMLElement,
            options: {
              type?: 'standard' | 'icon';
              theme?: 'outline' | 'filled_blue' | 'filled_black';
              size?: 'large' | 'medium' | 'small';
              text?: 'signin_with' | 'signup_with' | 'continue_with' | 'signin';
              shape?: 'rectangular' | 'pill' | 'circle' | 'square';
              logo_alignment?: 'left' | 'center';
              width?: number | string;
              locale?: string;
            }
          ) => void;
          prompt?: () => void;
        };
      };
    };
  }
}

interface GoogleLoginButtonProps {
  onSuccess: (credential: string) => Promise<void> | void;
  onError?: (error: string) => void;
  text?: 'continue_with' | 'signin_with' | 'signup_with';
  disabled?: boolean;
}

export const GoogleLoginButton: React.FC<GoogleLoginButtonProps> = ({
  onSuccess,
  onError,
  text = 'continue_with',
  disabled = false,
}) => {
  const buttonRef = useRef<HTMLDivElement>(null);
  const [isGsiLoaded, setIsGsiLoaded] = useState<boolean>(false);
  const [isProcessing, setIsProcessing] = useState<boolean>(false);
  const clientId = (import.meta as unknown as { env: Record<string, string> }).env?.VITE_GOOGLE_CLIENT_ID || '';

  // Use refs for callbacks to avoid re-triggering effects on parent component re-renders
  const onSuccessRef = useRef(onSuccess);
  onSuccessRef.current = onSuccess;

  const onErrorRef = useRef(onError);
  onErrorRef.current = onError;

  // Track if button has already been rendered for current clientId and text
  const renderedKeyRef = useRef<string>('');

  useEffect(() => {
    // Check if GSI is already available globally
    if (window.google?.accounts?.id) {
      setIsGsiLoaded(true);
      return;
    }

    // Check if script tag is already in DOM
    const existingScript = document.querySelector<HTMLScriptElement>('script[src="https://accounts.google.com/gsi/client"]');
    if (existingScript) {
      existingScript.addEventListener('load', () => setIsGsiLoaded(true));
      return;
    }

    const script = document.createElement('script');
    script.src = 'https://accounts.google.com/gsi/client';
    script.async = true;
    script.defer = true;
    script.onload = () => {
      setIsGsiLoaded(true);
    };
    script.onerror = () => {
      console.warn('Google Identity Services SDK could not be loaded.');
    };
    document.head.appendChild(script);
  }, []);

  useEffect(() => {
    if (!isGsiLoaded || !buttonRef.current) return;

    if (!clientId || clientId.startsWith('your_google')) {
      return;
    }

    const currentKey = `${clientId}:${text}`;
    if (renderedKeyRef.current === currentKey && buttonRef.current.children.length > 0) {
      return;
    }

    try {
      window.google?.accounts.id.initialize({
        client_id: clientId,
        callback: async (response) => {
          if (response.credential) {
            setIsProcessing(true);
            try {
              await onSuccessRef.current(response.credential);
            } catch (err: unknown) {
              const message = err instanceof Error ? err.message : 'Google authentication failed. Please try again.';
              onErrorRef.current?.(message);
            } finally {
              setIsProcessing(false);
            }
          } else {
            onErrorRef.current?.('Google authentication could not be completed.');
          }
        },
      });

      if (buttonRef.current) {
        buttonRef.current.innerHTML = '';
        window.google?.accounts.id.renderButton(buttonRef.current, {
          theme: 'filled_black',
          size: 'large',
          text,
          shape: 'rectangular',
          width: '100%',
          logo_alignment: 'left',
        });
        renderedKeyRef.current = currentKey;
      }
    } catch (err) {
      console.error('Failed to initialize Google Sign-in button', err);
    }
  }, [isGsiLoaded, clientId, text]);

  const handleCustomButtonClick = () => {
    if (!clientId || clientId.startsWith('your_google')) {
      console.info('Google Sign-in: VITE_GOOGLE_CLIENT_ID is not configured yet.');
      onError?.('Google Sign-In is currently unavailable. Please sign in with email and password.');
      return;
    }

    if (window.google?.accounts?.id?.prompt) {
      window.google.accounts.id.prompt();
    }
  };

  return (
    <div className="google-auth-wrapper">
      {/* Official Google Button Render Target */}
      <div
        ref={buttonRef}
        className={`google-btn-rendered ${disabled || isProcessing ? 'google-btn-disabled' : ''}`}
        style={{ width: '100%', display: clientId && !clientId.startsWith('your_google') ? 'block' : 'none' }}
      />

      {/* Styled Fallback / Default Button (shown if Client ID is pending or when custom button is clicked) */}
      {(!clientId || clientId.startsWith('your_google')) && (
        <button
          type="button"
          className="google-custom-btn"
          onClick={handleCustomButtonClick}
          disabled={disabled || isProcessing}
        >
          {isProcessing ? (
            <Loader2 className="animate-spin" size={18} />
          ) : (
            <svg className="google-icon-svg" viewBox="0 0 24 24" width="18" height="18">
              <path
                fill="#4285F4"
                d="M23.745 12.27c0-.7-.06-1.4-.19-2.07H12v4.51h6.6c-.29 1.52-1.14 2.82-2.4 3.68v3.05h3.88c2.27-2.09 3.665-5.17 3.665-9.17z"
              />
              <path
                fill="#34A853"
                d="M12 24c3.24 0 5.95-1.08 7.93-2.91l-3.88-3.05c-1.08.72-2.45 1.16-4.05 1.16-3.12 0-5.77-2.1-6.72-4.93H1.25v3.15C3.26 21.36 7.34 24 12 24z"
              />
              <path
                fill="#FBBC05"
                d="M5.28 14.27c-.25-.72-.38-1.49-.38-2.27s.13-1.55.38-2.27V6.58H1.25C.45 8.19 0 10.04 0 12s.45 3.81 1.25 5.42l4.03-3.15z"
              />
              <path
                fill="#EA4335"
                d="M12 4.75c1.77 0 3.35.61 4.6 1.8l3.42-3.42C17.95 1.19 15.24 0 12 0 7.34 0 3.26 2.64 1.25 6.58l4.03 3.15c.95-2.83 3.6-4.98 6.72-4.98z"
              />
            </svg>
          )}
          <span>
            {text === 'signup_with'
              ? 'Sign up with Google'
              : text === 'signin_with'
              ? 'Sign in with Google'
              : 'Continue with Google'}
          </span>
        </button>
      )}
    </div>
  );
};
