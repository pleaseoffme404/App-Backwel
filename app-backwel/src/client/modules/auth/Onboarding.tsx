import React, { useState, useEffect, useRef } from 'react';
import Swal from 'sweetalert2';

export default function Onboarding({ onComplete }: { onComplete: () => void }) {
  // Estado para controlar en qué paso del onboarding estamos
  const [step, setStep] = useState<1 | 2>(1);
  
  const [formData, setFormData] = useState({
    name: '',
    surname: '',
    phoneNumber: ''
  });

  const [addressData, setAddressData] = useState({
    placeId: '',
    formattedAddress: '',
    streetNumber: '',
    route: '',
    locality: '',
    administrativeAreaLevel1: '',
    postalCode: '',
    countryCode: 'MX',
    latitude: 0,
    longitude: 0
  });

  const [pin, setPin] = useState('');
  const [loading, setLoading] = useState(false);
  const addressInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (step !== 1) return; // Solo inicializar mapas en el paso 1

    let checkInterval: any;

    const initAutocomplete = () => {
      const google = (window as any).google;
      if (google && google.maps && google.maps.places && addressInputRef.current) {
        clearInterval(checkInterval);

        const autocomplete = new google.maps.places.Autocomplete(addressInputRef.current, {
          types: ['address'],
          componentRestrictions: { country: 'mx' }
        });

        autocomplete.addListener('place_changed', () => {
          const place = autocomplete.getPlace();
          
          if (!place.geometry || !place.address_components) return;

          let streetNumber = '';
          let route = '';
          let locality = '';
          let adminArea = '';
          let postalCode = '';

          place.address_components.forEach((component: any) => {
            const types = component.types;
            if (types.includes('street_number')) streetNumber = component.long_name;
            if (types.includes('route')) route = component.long_name;
            if (types.includes('locality') || types.includes('sublocality')) locality = component.long_name;
            if (types.includes('administrative_area_level_1')) adminArea = component.long_name;
            if (types.includes('postal_code')) postalCode = component.long_name;
          });

          setAddressData({
            placeId: place.place_id || '',
            formattedAddress: place.formatted_address || '',
            streetNumber,
            route,
            locality,
            administrativeAreaLevel1: adminArea,
            postalCode,
            countryCode: 'MX',
            latitude: place.geometry.location.lat(),
            longitude: place.geometry.location.lng()
          });
        });
      }
    };

    checkInterval = setInterval(initAutocomplete, 200);
    return () => clearInterval(checkInterval);
  }, [step]);

  const handleProfileSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!addressData.placeId) {
      Swal.fire({
        title: 'Dirección Requerida',
        text: 'Por favor, selecciona una dirección válida de la lista sugerida por Google Maps.',
        icon: 'warning',
        confirmButtonColor: '#38BDF8'
      });
      return;
    }
    
    setLoading(true);

    const payload = {
      name: formData.name,
      surname: formData.surname,
      phoneNumber: formData.phoneNumber,
      countryCode: "MX",
      currencyCode: "MXN",
      addressName: "Principal",
      firstAddress: {
        addressName: "Principal",
        slotIndex: 0,
        googleAddressDTO: addressData
      }
    };

    try {
      const res = await fetch('/api/v1/user/complete-account', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      if (res.ok) {
        const userData = await res.json();
        
        // Verificamos si el usuario tiene privilegios para requerir PIN
        const roleStr = String(userData.role || '').toUpperCase();
        const requiresPin = ['ADMIN', 'OWNER', 'MANAGER', 'STAFF'].some(r => roleStr.includes(r));

        if (requiresPin) {
          // Es un administrador/staff. Pasamos al paso 2.
          setStep(2);
        } else {
          // Es un cliente regular. Terminamos el onboarding sin pedir PIN.
          Swal.fire({
            title: '¡Registro completado!',
            text: 'Tus datos se han guardado exitosamente.',
            icon: 'success',
            confirmButtonColor: '#38BDF8',
            allowOutsideClick: false
          }).then(() => {
            onComplete();
          });
        }
      } else {
        const errorData = await res.json();
        let errorMsg = 'Ocurrió un error validando tus datos.';
        if (errorData.validationErrors) {
          const details = Object.entries(errorData.validationErrors)
            .map(([_, val]) => `• ${val}`)
            .join('<br/>');
          errorMsg = `Revisa los siguientes campos:<br/><br/><div style="text-align: left; font-size: 0.9em; color: #ef4444;">${details}</div>`;
        } else if (errorData.message) {
          errorMsg = errorData.message;
        }

        Swal.fire({
          title: 'Error de Validación',
          html: errorMsg,
          icon: 'error',
          confirmButtonColor: '#38BDF8'
        });
      }
    } catch (err) {
      Swal.fire({
        title: 'Error de red',
        text: 'No se pudo contactar con el servidor.',
        icon: 'error',
        confirmButtonColor: '#38BDF8'
      });
    } finally {
      setLoading(false);
    }
  };

  const handlePinSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (pin.length !== 6 || !/^\d+$/.test(pin)) {
      Swal.fire({
        title: 'PIN Inválido',
        text: 'El PIN debe contener exactamente 6 números.',
        icon: 'warning',
        confirmButtonColor: '#38BDF8'
      });
      return;
    }

    setLoading(true);

    try {
      // Hacemos la petición al auth-server para registrar el PIN maestro
      const res = await fetch('/auth/pin-authentication/setup', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ pin })
      });

      if (res.ok) {
        Swal.fire({
          title: '¡Seguridad configurada!',
          text: 'Tu PIN maestro se ha guardado correctamente.',
          icon: 'success',
          confirmButtonColor: '#38BDF8',
          allowOutsideClick: false
        }).then(() => {
          onComplete();
        });
      } else {
        const errorData = await res.json();
        Swal.fire({
          title: 'Error configurando PIN',
          text: errorData.message || 'El sistema rechazó el PIN.',
          icon: 'error',
          confirmButtonColor: '#38BDF8'
        });
      }
    } catch (err) {
      Swal.fire({
        title: 'Error de red',
        text: 'No se pudo contactar con el servidor de seguridad.',
        icon: 'error',
        confirmButtonColor: '#38BDF8'
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-bg-primary flex flex-col justify-center items-center p-4 pb-20">
      <div className="max-w-md w-full bg-bg-secondary p-8 rounded-2xl shadow-xl border border-brand-primary/10">
        
        {step === 1 && (
          <>
            <h1 className="text-2xl font-black text-brand-primary mb-2">Completa tu perfil</h1>
            <p className="text-text-primary/60 mb-6 text-sm">Necesitamos algunos datos básicos para procesar tus futuras compras.</p>
            
            <form onSubmit={handleProfileSubmit} className="flex flex-col gap-4">
              <div className="grid grid-cols-2 gap-4">
                <input required type="text" placeholder="Nombre(s)" value={formData.name} onChange={e => setFormData({...formData, name: e.target.value})} className="p-3 bg-bg-primary border border-brand-primary/20 rounded-lg text-text-primary outline-none focus:border-brand-primary w-full" />
                <input required type="text" placeholder="Apellidos" value={formData.surname} onChange={e => setFormData({...formData, surname: e.target.value})} className="p-3 bg-bg-primary border border-brand-primary/20 rounded-lg text-text-primary outline-none focus:border-brand-primary w-full" />
              </div>
              <input required type="tel" pattern="[0-9]{10}" placeholder="Teléfono (10 dígitos)" value={formData.phoneNumber} onChange={e => setFormData({...formData, phoneNumber: e.target.value})} className="p-3 bg-bg-primary border border-brand-primary/20 rounded-lg text-text-primary outline-none focus:border-brand-primary w-full" />
              
              <div className="border-t border-brand-primary/10 pt-4 mt-2 flex flex-col gap-3">
                <h2 className="text-sm font-bold text-text-primary/80 uppercase">Dirección de Envío</h2>
                <input 
                  ref={addressInputRef}
                  type="text" 
                  placeholder="Empieza a escribir tu dirección..." 
                  className="p-3 bg-bg-primary border border-brand-primary/20 rounded-lg text-text-primary outline-none focus:border-brand-primary w-full shadow-inner" 
                />
                
                {addressData.placeId && (
                  <div className="p-4 bg-bg-primary border border-brand-primary/20 rounded-lg text-sm flex flex-col gap-3 mt-2 transition-all">
                    <span className="font-bold text-brand-primary text-xs uppercase tracking-wide">Verifica los datos obtenidos:</span>
                    
                    <input required type="text" placeholder="Calle" value={addressData.route} onChange={e => setAddressData({...addressData, route: e.target.value})} className="p-2 bg-bg-secondary border border-brand-primary/10 rounded w-full outline-none focus:border-brand-primary text-text-primary" />
                    
                    <div className="grid grid-cols-2 gap-2">
                      <input required type="text" placeholder="No. Ext/Int" value={addressData.streetNumber} onChange={e => setAddressData({...addressData, streetNumber: e.target.value})} className="p-2 bg-bg-secondary border border-brand-primary/10 rounded w-full outline-none focus:border-brand-primary text-text-primary" />
                      <input required type="text" placeholder="C.P." value={addressData.postalCode} onChange={e => setAddressData({...addressData, postalCode: e.target.value})} className="p-2 bg-bg-secondary border border-brand-primary/10 rounded w-full outline-none focus:border-brand-primary text-text-primary" />
                    </div>
                    
                    <input required type="text" placeholder="Ciudad / Municipio" value={addressData.locality} onChange={e => setAddressData({...addressData, locality: e.target.value})} className="p-2 bg-bg-secondary border border-brand-primary/10 rounded w-full outline-none focus:border-brand-primary text-text-primary" />
                    
                    <span className="text-[10px] text-text-primary/40 mt-1 italic">
                      *Si Google no encontró tu número exterior o código postal, escríbelos manualmente arriba.
                    </span>
                  </div>
                )}
              </div>

              <button disabled={loading} type="submit" className="mt-4 py-4 bg-brand-primary text-bg-primary font-black rounded-xl hover:opacity-90 disabled:opacity-50 transition-all shadow-lg shadow-brand-primary/20">
                {loading ? 'Guardando...' : 'Finalizar Registro'}
              </button>
            </form>
          </>
        )}

        {step === 2 && (
          <div className="animate-fade-in">
            <h1 className="text-2xl font-black text-brand-primary mb-2">Seguridad del Sistema</h1>
            <p className="text-text-primary/60 mb-6 text-sm">Como administrador, necesitas establecer un PIN maestro de 6 dígitos para desbloquear el Punto de Venta.</p>
            
            <form onSubmit={handlePinSubmit} className="flex flex-col gap-4">
              <input 
                required 
                type="password" 
                maxLength={6}
                pattern="\d{6}"
                placeholder="Ingresa 6 dígitos" 
                value={pin} 
                onChange={e => setPin(e.target.value.replace(/\D/g, ''))} 
                className="p-4 text-center tracking-[1em] text-2xl font-black bg-bg-primary border border-brand-primary/20 rounded-lg text-text-primary outline-none focus:border-brand-primary w-full" 
              />
              
              <button disabled={loading || pin.length !== 6} type="submit" className="mt-4 py-4 bg-accent text-white font-black rounded-xl hover:opacity-90 disabled:opacity-50 transition-all shadow-lg">
                {loading ? 'Validando...' : 'Establecer PIN'}
              </button>
            </form>
          </div>
        )}

      </div>
    </div>
  );
}