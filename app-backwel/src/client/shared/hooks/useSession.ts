import { useState, useEffect } from 'react';

interface User {
  uuid: string;
  name: string;
  surname: string;
  email: string;
  avatar_url?: string;
  credits: number;
}

interface SessionState {
  active: boolean;
  user?: User;
}

export function useSession() {
  const [session, setSession] = useState<SessionState | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    fetch('/api/v1/user/userinfo')
      .then((res) => {
        if (!res.ok) throw new Error();
        return res.json();
      })
      .then((user) => {
        setSession({ 
          active: true, 
          user: {
            uuid: user.uuid,
            name: user.name,
            surname: user.surname,
            email: user.email,
            avatar_url: user.pictureUrl,
            credits: 0 
          } 
        });
        setIsLoading(false);
      })
      .catch(() => {
        setSession({ active: false });
        setIsLoading(false);
      });
  }, []);

  return { session, isLoading };
}