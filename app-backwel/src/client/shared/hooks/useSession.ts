import { useState, useEffect } from 'react';

interface User {
  uuid: string;
  name: string;
  surname: string;
  email: string;
  avatar_url?: string;
  credits: number;
  roles: string[];
}

interface SessionState {
  active: boolean;
  user?: User;
}

let globalSessionPromise: Promise<any> | null = null;

export function useSession() {
  const [session, setSession] = useState<SessionState | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    if (!globalSessionPromise) {
      globalSessionPromise = fetch('/api/v1/user/userinfo')
        .then(async (res) => {
          if (!res.ok) {
            const setupRes = await fetch('/api/v1/user/complete-account', {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({
                name: "Admin",
                surname: "System",
                phoneNumber: "0000000000",
                countryCode: "MX",
                currencyCode: "MXN"
              })
            });
            
            if (setupRes.ok) return setupRes.json();
            
            const retryRes = await fetch('/api/v1/user/userinfo');
            if (retryRes.ok) return retryRes.json();
            
            throw new Error();
          }
          return res.json();
        });
    }

    globalSessionPromise
      .then((user) => {
        setSession({ 
          active: true, 
          user: {
            uuid: user.id,
            name: user.name,
            surname: user.surname,
            email: user.email,
            avatar_url: user.pictureUrl,
            credits: 0,
            roles: user.role ? [user.role] : []
          } 
        });
        setIsLoading(false);
      })
      .catch(() => {
        setSession({ active: false });
        setIsLoading(false);
        globalSessionPromise = null;
      });
  }, []);

  return { session, isLoading };
}