import { useState, useEffect } from 'react';

interface BusinessConfig {
  business_name: string;
  logo_url: string;
}

export function useBusiness() {
  const [business, setBusiness] = useState<BusinessConfig | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    fetch('/api/config/business')
      .then((res) => {
        if (!res.ok) throw new Error();
        return res.json();
      })
      .then((data) => {
        setBusiness(data);
        setIsLoading(false);
      })
      .catch(() => {
        setIsLoading(false);
      });
  }, []);

  return { business, isLoading };
}