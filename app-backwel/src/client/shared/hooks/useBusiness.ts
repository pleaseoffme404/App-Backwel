import { useState, useEffect, useCallback } from 'react';

export interface BusinessConfig {
  businessName?: string;
  business_name?: string;
  logoUrl?: string;
  logo_url?: string;
  contactEmail?: string;
  contactPhone?: string;
  address?: string;
  taxId?: string;
  currency?: string;
}

let cachedBusinessConfig: BusinessConfig | null = null;
let isFetching = false;
const listeners = new Set<() => void>();

export function useBusiness() {
  const [business, setBusiness] = useState<BusinessConfig | null>(cachedBusinessConfig);
  const [isLoading, setIsLoading] = useState(!cachedBusinessConfig);

  const fetchBusiness = useCallback(async () => {
    if (isFetching) return;
    isFetching = true;
    try {
      // Pedimos la configuración a la API
      const res = await fetch('/api/config/business');
      
      if (res.ok) {
        const data = await res.json();
        console.log(" Datos de negocio recibidos del backend:", data);

        cachedBusinessConfig = data.business || data || {};
        listeners.forEach(listener => listener());
      } else {
        console.error("El backend respondió con error:", res.status); 
      }
    } catch (error) {
      console.error("Error de red al pedir configuración de negocio:", error);
    } finally {
      isFetching = false;
      setIsLoading(false);
    }
  }, []);

useEffect(() => {
    const update = () => {
      setBusiness(cachedBusinessConfig);
      setIsLoading(false);

      if (cachedBusinessConfig) {
        const name = cachedBusinessConfig.businessName || cachedBusinessConfig.business_name;
        const logo = cachedBusinessConfig.logoUrl || cachedBusinessConfig.logo_url;

        if (name) {
          document.title = name;
        }
        if (logo) {
          let link = document.querySelector("link[rel~='icon']") as HTMLLinkElement;
          if (!link) {
            link = document.createElement('link');
            link.rel = 'icon';
            document.head.appendChild(link);
          }
          link.href = logo;
        }
      }
    };
    listeners.add(update);

    if (!cachedBusinessConfig) fetchBusiness();
    else update();

    return () => { listeners.delete(update); };
  }, [fetchBusiness]);

  return { business, isLoading, refreshBusiness: fetchBusiness };
}