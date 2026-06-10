import React from 'react';
import { useBusiness } from '../../../shared/hooks/useBusiness';
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';

import icon from 'leaflet/dist/images/marker-icon.png';
import iconShadow from 'leaflet/dist/images/marker-shadow.png';

let DefaultIcon = L.icon({
    iconUrl: icon,
    shadowUrl: iconShadow,
    iconSize: [25, 41],
    iconAnchor: [12, 41]
});
L.Marker.prototype.options.icon = DefaultIcon;

export function FooterSection() {
  const { business } = useBusiness();
  const bName = business?.businessName || business?.business_name || 'Backwel Store';
  
  const position: [number, number] = [19.4785, -99.2324];

  return (
    <footer id="contacto" className="bg-bg-secondary border-t border-brand-primary/10 pt-16 pb-8 px-4">
      <div className="max-w-7xl mx-auto grid grid-cols-1 lg:grid-cols-3 gap-12 mb-12">
        <div className="flex flex-col gap-6">
          <div>
            <h3 className="text-2xl font-black text-brand-primary tracking-tight mb-2">{bName}</h3>
            <p className="text-text-primary/60 text-sm leading-relaxed">
              Ofrecemos los mejores productos de tecnologia y mobiliario con calidad garantizada.
            </p>
          </div>
          <div className="flex gap-4">
            <a href="#" className="w-10 h-10 rounded-full bg-bg-primary flex items-center justify-center text-brand-primary hover:bg-brand-primary hover:text-bg-primary transition-colors border border-brand-primary/20">
              <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24"><path d="M24 4.557c-.883.392-1.832.656-2.828.775 1.017-.609 1.798-1.574 2.165-2.724-.951.564-2.005.974-3.127 1.195-.897-.957-2.178-1.555-3.594-1.555-3.179 0-5.515 2.966-4.797 6.045-4.091-.205-7.719-2.165-10.148-5.144-1.29 2.213-.669 5.108 1.523 6.574-.806-.026-1.566-.247-2.229-.616-.054 2.281 1.581 4.415 3.949 4.89-.693.188-1.452.232-2.224.084.626 1.956 2.444 3.379 4.6 3.419-2.07 1.623-4.678 2.348-7.29 2.04 2.179 1.397 4.768 2.212 7.548 2.212 9.142 0 14.307-7.721 13.995-14.646.962-.695 1.797-1.562 2.457-2.549z"/></svg>
            </a>
            <a href="#" className="w-10 h-10 rounded-full bg-bg-primary flex items-center justify-center text-brand-primary hover:bg-brand-primary hover:text-bg-primary transition-colors border border-brand-primary/20">
              <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24"><path d="M12 2.163c3.204 0 3.584.012 4.85.07 3.252.148 4.771 1.691 4.919 4.919.058 1.265.069 1.645.069 4.849 0 3.205-.012 3.584-.069 4.849-.149 3.225-1.664 4.771-4.919 4.919-1.266.058-1.644.07-4.85.07-3.204 0-3.584-.012-4.849-.07-3.26-.149-4.771-1.699-4.919-4.92-.058-1.265-.07-1.644-.07-4.849 0-3.204.013-3.583.07-4.849.149-3.227 1.664-4.771 4.919-4.919 1.266-.057 1.645-.069 4.849-.069zm0-2.163c-3.259 0-3.667.014-4.947.072-4.358.2-6.78 2.618-6.98 6.98-.059 1.281-.073 1.689-.073 4.948 0 3.259.014 3.668.072 4.948.2 4.358 2.618 6.78 6.98 6.98 1.281.058 1.689.072 4.948.072 3.259 0 3.668-.014 4.948-.072 4.354-.2 6.782-2.618 6.979-6.98.059-1.28.073-1.689.073-4.948 0-3.259-.014-3.667-.072-4.947-.196-4.354-2.617-6.78-6.979-6.98-1.281-.059-1.69-.073-4.949-.073zm0 5.838c-3.403 0-6.162 2.759-6.162 6.162s2.759 6.163 6.162 6.163 6.162-2.759 6.162-6.163c0-3.403-2.759-6.162-6.162-6.162zm0 10.162c-2.209 0-4-1.79-4-4 0-2.209 1.791-4 4-4s4 1.791 4 4c0 2.21-1.791 4-4 4zm6.406-11.845c-.796 0-1.441.645-1.441 1.44s.645 1.44 1.441 1.44c.795 0 1.439-.645 1.439-1.44s-.644-1.44-1.439-1.44z"/></svg>
            </a>
          </div>
        </div>
        <div className="flex flex-col gap-4">
          <h4 className="font-black text-text-primary uppercase tracking-wider text-sm mb-2">Contacto</h4>
          <div className="flex items-start gap-3 text-text-primary/70">
            <svg className="w-5 h-5 shrink-0 text-accent mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" /><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" /></svg>
            <span className="text-sm">{business?.address || 'Direccion no configurada'}</span>
          </div>
          <div className="flex items-center gap-3 text-text-primary/70">
            <svg className="w-5 h-5 shrink-0 text-accent" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" /></svg>
            <span className="text-sm">{business?.contactEmail || 'soporte@tuempresa.com'}</span>
          </div>
          <div className="flex items-center gap-3 text-text-primary/70">
            <svg className="w-5 h-5 shrink-0 text-accent" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z" /></svg>
            <span className="text-sm">{business?.contactPhone || '+52 55 0000 0000'}</span>
          </div>
        </div>
        <div className="h-64 w-full rounded-xl overflow-hidden border border-brand-primary/20 z-10 relative">
          <MapContainer center={position} zoom={13} scrollWheelZoom={false} style={{ height: '100%', width: '100%' }}>
            <TileLayer
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OSM</a>'
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />
            <Marker position={position}>
              <Popup className="font-sans font-bold">
                {bName} <br /> ¡Aqui estamos!
              </Popup>
            </Marker>
          </MapContainer>
        </div>
      </div>
      <div className="max-w-7xl mx-auto border-t border-brand-primary/10 pt-8 flex flex-col md:flex-row items-center justify-between gap-4 text-xs font-bold text-text-primary/40">
        <p>&copy; {new Date().getFullYear()} {bName}. Todos los derechos reservados.</p>
        <div className="flex gap-6">
          <a href="#" className="hover:text-brand-primary transition-colors">Terminos y Condiciones</a>
          <a href="#" className="hover:text-brand-primary transition-colors">Aviso de Privacidad</a>
          <a href="#" className="hover:text-brand-primary transition-colors">Politica de Reembolsos</a>
        </div>
      </div>
    </footer>
  );
}