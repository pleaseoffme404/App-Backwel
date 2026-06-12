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
  needsOnboarding?: boolean;
}

let cachedSession: SessionState | null = null;
let globalSessionPromise: Promise<SessionState> | null = null;

export function useSession() {
  const [session, setSession] = useState<SessionState | null>(cachedSession);
  const [isLoading, setIsLoading] = useState(!cachedSession);

  useEffect(() => {
    let isMounted = true;

    const fetchSession = async () => {
      if (cachedSession) {
        if (isMounted) {
          setSession(cachedSession);
          setIsLoading(false);
        }
        return;
      }

      if (!globalSessionPromise) {
        globalSessionPromise = (async () => {
          try {
            const res = await fetch('/api/v1/user/userinfo');
            
            if (res.status === 404) {
              return { active: true, needsOnboarding: true };
            }
            
            if (!res.ok) throw new Error('Unauthorized');

            const user = await res.json();
            return { 
              active: true, 
              needsOnboarding: false,
              user: { 
                uuid: user.id, 
                name: user.name, 
                surname: user.surname, 
                email: user.email, 
                avatar_url: user.pictureUrl, 
                credits: 0, 
                roles: [user.role, user.roles, user.authorities].flat().filter(Boolean).join(',').split(',')
              }
            };
          } catch (error) {
            return { active: false, needsOnboarding: false };
          }
        })();
      }

      const result = await globalSessionPromise;
      cachedSession = result;
      
      if (isMounted) {
        setSession(result);
        setIsLoading(false);
      }
    };

    fetchSession();

    return () => {
      isMounted = false;
    };
  }, []);

  const clearSession = () => {
    cachedSession = null;
    globalSessionPromise = null;
  };

  return { session, isLoading, clearSession };
}