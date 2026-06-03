import { useState } from 'react';

export default function UsuariosAdmin() {
  const [email, setEmail] = useState('');
  const [role, setRole] = useState('ADMIN');
  const [status, setStatus] = useState<{ type: 'idle' | 'loading' | 'success' | 'error', message: string }>({ type: 'idle', message: '' });

  const handleRoleAction = async (action: 'grant' | 'revoke') => {
    if (!email) {
      setStatus({ type: 'error', message: 'Ingresa un correo electrónico.' });
      return;
    }

    setStatus({ type: 'loading', message: `Procesando petición para ${action}...` });

    try {
      const response = await fetch(`/api/v1/roles/${action}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email, roleName: role }),
      });

      if (!response.ok) {
        throw new Error(response.status === 403 ? 'No tienes permisos suficientes para asignar este rol.' : 'Error al procesar la solicitud.');
      }

      setStatus({ 
        type: 'success', 
        message: `Rol ${role} ${action === 'grant' ? 'otorgado' : 'revocado'} exitosamente para ${email}` 
      });
      setEmail('');
    } catch (error: any) {
      setStatus({ type: 'error', message: error.message || 'Error de conexión.' });
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-8">
      <div>
        <h1 className="text-3xl font-bold mb-2">Gestión de Usuarios y Roles</h1>
        <p className="opacity-80">Administra los permisos de acceso de tu equipo al panel de administración.</p>
      </div>

      <div className="bg-bg-secondary border border-text-primary/10 rounded-lg p-6">
        <h2 className="text-xl font-bold mb-4">Otorgar / Revocar Rango</h2>
        
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
          <div className="col-span-2">
            <label className="block text-sm font-medium mb-1 opacity-80">Correo del Usuario</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="ejemplo@backwell.com"
              className="w-full bg-bg-primary border border-text-primary/20 rounded p-2 focus:outline-none focus:border-brand-primary"
            />
          </div>
          <div>
            <label className="block text-sm font-medium mb-1 opacity-80">Rango a asignar</label>
            <select
              value={role}
              onChange={(e) => setRole(e.target.value)}
              className="w-full bg-bg-primary border border-text-primary/20 rounded p-2 focus:outline-none focus:border-brand-primary"
            >
              <option value="ADMIN">ADMIN</option>
              <option value="USER">USER</option>
            </select>
          </div>
        </div>

        {status.type !== 'idle' && (
          <div className={`p-3 rounded mb-6 text-sm font-medium ${
            status.type === 'error' ? 'bg-red-500/10 text-red-500 border border-red-500/20' : 
            status.type === 'success' ? 'bg-green-500/10 text-green-500 border border-green-500/20' : 
            'bg-brand-primary/10 text-brand-primary border border-brand-primary/20'
          }`}>
            {status.message}
          </div>
        )}

        <div className="flex gap-4">
          <button
            onClick={() => handleRoleAction('grant')}
            disabled={status.type === 'loading'}
            className="px-6 py-2 bg-text-primary text-bg-primary font-bold rounded hover:opacity-90 disabled:opacity-50 transition-opacity"
          >
            Otorgar Rango
          </button>
          <button
            onClick={() => handleRoleAction('revoke')}
            disabled={status.type === 'loading'}
            className="px-6 py-2 border border-text-primary/20 hover:bg-red-500 hover:text-white hover:border-red-500 font-bold rounded disabled:opacity-50 transition-colors"
          >
            Quitar Rango
          </button>
        </div>
      </div>

      <div className="bg-bg-secondary border border-text-primary/10 rounded-lg p-6 opacity-50 relative overflow-hidden">
        <div className="absolute inset-0 bg-bg-secondary/50 backdrop-blur-sm z-10 flex items-center justify-center">
          <span className="bg-bg-primary px-4 py-2 rounded shadow border border-text-primary/10 font-medium">
            Esperando endpoint del backend...
          </span>
        </div>
        <h2 className="text-xl font-bold mb-4">Directorio de Usuarios</h2>
        <div className="h-64 bg-bg-primary rounded animate-pulse"></div>
      </div>
    </div>
  );
}