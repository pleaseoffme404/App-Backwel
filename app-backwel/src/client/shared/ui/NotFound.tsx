import { Link } from 'react-router-dom';
import { usePageConfig } from '../hooks/usePageConfig';

interface NotFoundConfig {
  title?: string;
  message?: string;
  description?: string;
  support_email?: string;
  button_text?: string;
}

export function NotFound({ previewConfig }: { previewConfig?: NotFoundConfig }) {
  const { publishedConfig } = usePageConfig();
  const config = previewConfig || publishedConfig?.not_found || {};

  const title = config.title || '404';
  const message = config.message || 'Página no encontrada';
  const description = config.description || 'La ruta a la que intentas acceder no existe, fue movida o no tienes permisos para verla.';
  const buttonText = config.button_text || 'Volver al inicio';
  const supportEmail = config.support_email || '';

  return (
    <div className="min-h-[80vh] w-full flex flex-col items-center justify-center bg-bg-primary text-text-primary p-6">
      <h1 className="text-9xl font-black text-brand-primary mb-2">{title}</h1>
      <h2 className="text-3xl font-bold mb-4">{message}</h2>
      <p className="text-lg opacity-70 mb-8 text-center max-w-md">
        {description}
      </p>
      <Link
        to="/"
        className="px-8 py-3 bg-text-primary text-bg-primary font-bold rounded-lg hover:opacity-90 transition-opacity mb-4"
      >
        {buttonText}
      </Link>
      {supportEmail && (
        <p className="text-sm opacity-50 mt-8">
          ¿Necesitas ayuda? Contacta a:{' '}
          <a href={`mailto:${supportEmail}`} className="text-brand-secondary hover:underline font-bold">
            {supportEmail}
          </a>
        </p>
      )}
    </div>
  );
}