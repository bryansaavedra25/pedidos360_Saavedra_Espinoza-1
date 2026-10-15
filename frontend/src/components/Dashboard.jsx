import { useEffect, useState } from 'react';
import { useAuthenticator } from '@aws-amplify/ui-react';
import { apiFetch, getUserClaims } from '../services/apiFetch';

export default function Dashboard() {
  const { signOut } = useAuthenticator();
  const [usuario, setUsuario] = useState(null);
  const [pedidos, setPedidos] = useState(null);
  const [productos, setProductos] = useState([]);
  const [error, setError] = useState(null);

  // Pestaña activa: 'pedidos', 'productos' o 'perfil'
  const [tabActiva, setTabActiva] = useState('pedidos');

  // Estados para el formulario de nuevo pedido
  const [cliente, setCliente] = useState('');
  const [productoSeleccionado, setProductoSeleccionado] = useState('');
  const [cantidad, setCantidad] = useState(1);
  const [total, setTotal] = useState(0);
  const [estadoPedido, setEstadoPedido] = useState('PENDIENTE');
  const [cargandoEnvio, setCargandoEnvio] = useState(false);

  // Estados para el formulario de nuevo producto
  const [nombreProd, setNombreProd] = useState('');
  const [precioProd, setPrecioProd] = useState('');
  const [stockProd, setStockProd] = useState('');
  const [cargandoProd, setCargandoProd] = useState(false);

  // Roles desde Cognito
  const esUsuario = usuario?.roles?.includes('usuario') || usuario?.roles?.includes('USUARIO');
  const esAdmin = usuario?.roles?.includes('admin') || usuario?.roles?.includes('ADMIN');

  // Cambiar pestaña y limpiar errores
  const cambiarTab = (tab) => {
    setError(null);
    setTabActiva(tab);
  };

  // Cargar pedidos desde backend
  const cargarPedidos = () => {
    apiFetch('/pedidos')
      .then((respuesta) => {
        setPedidos(respuesta || []);
        setError(null);
      })
      .catch((err) => setError("Error en pedidos: " + err.message));
  };

  // Cargar productos desde backend
  const cargarProductos = () => {
    apiFetch('/productos')
      .then((respuesta) => {
        setProductos(Array.isArray(respuesta) ? respuesta : []);
        setError(null);
      })
      .catch((err) => setError("Error en productos: " + err.message));
  };

  useEffect(() => {
    getUserClaims().then((data) => setUsuario(data));
    cargarPedidos();
    cargarProductos();
  }, []);

  // Manejar cambio de producto en el formulario de pedido
  const handleProductoChange = (e) => {
    const prodId = e.target.value;
    setProductoSeleccionado(prodId);

    const prod = productos.find((p) => (p._key || p.id) === prodId);
    if (prod && prod.precio) {
      setTotal(prod.precio * cantidad);
    } else {
      setTotal(0);
    }
  };

  // Manejar cambio de cantidad en el formulario de pedido
  const handleCantidadChange = (e) => {
    const cant = Math.max(1, parseInt(e.target.value) || 1);
    setCantidad(cant);

    const prod = productos.find((p) => (p._key || p.id) === productoSeleccionado);
    if (prod && prod.precio) {
      setTotal(prod.precio * cant);
    }
  };

  // Crear nuevo pedido (POST /pedidos)
  const handleCrearPedido = (e) => {
    e.preventDefault();
    if (!cliente || !productoSeleccionado) return;

    const productoObj = productos.find((p) => (p._key || p.id) === productoSeleccionado);

    setCargandoEnvio(true);
    apiFetch('/pedidos', {
      method: 'POST',
      body: JSON.stringify({
        cliente,
        producto: productoObj ? productoObj.nombre : '',
        cantidad,
        total: parseFloat(total),
        estado: estadoPedido
      })
    })
      .then(() => {
        setCliente('');
        setProductoSeleccionado('');
        setCantidad(1);
        setTotal(0);
        setEstadoPedido('PENDIENTE');
        cargarPedidos();
      })
      .catch((err) => setError("Error al crear pedido: " + err.message))
      .finally(() => setCargandoEnvio(false));
  };

  // Eliminar pedido (DELETE)
  const handleEliminarPedido = (pedido) => {
    const key = pedido._key || pedido.id;
    if (!key) {
      setError("Error: El pedido no tiene una clave identificadora válida (_key/id).");
      return;
    }

    if (!window.confirm(`¿Seguro que deseas eliminar el pedido #${key}?`)) return;

    apiFetch(`/pedidos/${key}`, { method: 'DELETE' })
      .then(() => {
        cargarPedidos();
      })
      .catch((err) => setError("Error al eliminar pedido: " + err.message));
  };

  // Crear producto (POST - Admin)
  const handleCrearProducto = (e) => {
    e.preventDefault();
    if (!nombreProd || !precioProd) return;

    setCargandoProd(true);
    apiFetch('/productos', {
      method: 'POST',
      body: JSON.stringify({
        nombre: nombreProd,
        precio: parseFloat(precioProd),
        stock: parseInt(stockProd) || 0
      })
    })
      .then(() => {
        setNombreProd('');
        setPrecioProd('');
        setStockProd('');
        cargarProductos();
      })
      .catch((err) => setError("Error al crear producto: " + err.message))
      .finally(() => setCargandoProd(false));
  };

  // Eliminar producto (DELETE - Admin)
  const handleEliminarProducto = (prod) => {
    const key = prod._key || prod.id;
    if (!key) {
      setError("Error: El producto no tiene una clave identificadora válida (_key/id).");
      return;
    }

    if (!window.confirm(`¿Seguro que deseas eliminar el producto #${key}?`)) return;

    apiFetch(`/productos/${key}`, { method: 'DELETE' })
      .then(() => {
        cargarProductos();
      })
      .catch((err) => setError("Error al eliminar producto: " + err.message));
  };

  return (
    <div style={{
      maxWidth: '1000px',
      margin: '0 auto',
      padding: '30px 20px',
      fontFamily: "'Segoe UI', Roboto, Helvetica, Arial, sans-serif",
      backgroundColor: '#f8fafc',
      minHeight: '100vh',
      color: '#334155'
    }}>
      {/* Encabezado */}
      <header style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        paddingBottom: '20px',
        borderBottom: '2px solid #e2e8f0',
        marginBottom: '20px'
      }}>
        <div>
          <h1 style={{ margin: 0, color: '#0f172a', fontSize: '1.8rem', display: 'flex', alignItems: 'center', gap: '10px' }}>
            🛒 Pedidos360 - Dashboard
          </h1>
          <p style={{ margin: '5px 0 0 0', color: '#64748b', fontSize: '0.9rem' }}>
            Panel de control de gestión de pedidos y productos
          </p>
        </div>
        <button 
          onClick={signOut} 
          style={{
            padding: '8px 16px',
            backgroundColor: '#ef4444',
            color: '#ffffff',
            border: 'none',
            borderRadius: '6px',
            fontWeight: '600',
            cursor: 'pointer'
          }}
        >
          Cerrar Sesión
        </button>
      </header>

      {/* Tabs */}
      <div style={{ display: 'flex', gap: '10px', borderBottom: '2px solid #e2e8f0', marginBottom: '25px' }}>
        <button
          onClick={() => cambiarTab('pedidos')}
          style={{
            padding: '10px 20px',
            border: 'none',
            borderBottom: tabActiva === 'pedidos' ? '3px solid #2563eb' : '3px solid transparent',
            backgroundColor: 'transparent',
            color: tabActiva === 'pedidos' ? '#2563eb' : '#64748b',
            fontWeight: 'bold',
            fontSize: '0.95rem',
            cursor: 'pointer'
          }}
        >
          📦 Gestión de Pedidos
        </button>
        <button
          onClick={() => cambiarTab('productos')}
          style={{
            padding: '10px 20px',
            border: 'none',
            borderBottom: tabActiva === 'productos' ? '3px solid #2563eb' : '3px solid transparent',
            backgroundColor: 'transparent',
            color: tabActiva === 'productos' ? '#2563eb' : '#64748b',
            fontWeight: 'bold',
            fontSize: '0.95rem',
            cursor: 'pointer'
          }}
        >
          🛍️ Catálogo de Productos
        </button>
        <button
          onClick={() => cambiarTab('perfil')}
          style={{
            padding: '10px 20px',
            border: 'none',
            borderBottom: tabActiva === 'perfil' ? '3px solid #2563eb' : '3px solid transparent',
            backgroundColor: 'transparent',
            color: tabActiva === 'perfil' ? '#2563eb' : '#64748b',
            fontWeight: 'bold',
            fontSize: '0.95rem',
            cursor: 'pointer'
          }}
        >
          👤 Perfil del Usuario
        </button>
      </div>

      {/* PESTAÑA: PEDIDOS */}
      {tabActiva === 'pedidos' && (
        <div>
          {esUsuario ? (
            <section style={{ backgroundColor: '#ffffff', padding: '20px', borderRadius: '8px', border: '1px solid #e2e8f0', marginBottom: '25px' }}>
              <h2 style={{ marginTop: 0, fontSize: '1.2rem', color: '#1e293b', borderBottom: '1px solid #f1f5f9', paddingBottom: '10px' }}>
                ➕ Registrar Nuevo Pedido
              </h2>
              <form onSubmit={handleCrearPedido} style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))', gap: '15px', marginTop: '15px', alignItems: 'end' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 'bold', color: '#475569', marginBottom: '5px' }}>Cliente</label>
                  <input
                    type="text"
                    value={cliente}
                    onChange={(e) => setCliente(e.target.value)}
                    placeholder="Ej. Juan Pérez"
                    required
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1', boxSizing: 'border-box' }}
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 'bold', color: '#475569', marginBottom: '5px' }}>Producto</label>
                  <select
                    value={productoSeleccionado}
                    onChange={handleProductoChange}
                    required
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1', boxSizing: 'border-box', backgroundColor: '#fff' }}
                  >
                    <option value="">-- Seleccionar --</option>
                    {productos.map((prod) => {
                      const idProd = prod._key || prod.id;
                      return (
                        <option key={idProd} value={idProd}>
                          {prod.nombre} (${prod.precio})
                        </option>
                      );
                    })}
                  </select>
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 'bold', color: '#475569', marginBottom: '5px' }}>Cant.</label>
                  <input
                    type="number"
                    min="1"
                    value={cantidad}
                    onChange={handleCantidadChange}
                    required
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1', boxSizing: 'border-box' }}
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 'bold', color: '#475569', marginBottom: '5px' }}>Total ($)</label>
                  <input
                    type="number"
                    step="0.01"
                    value={total}
                    onChange={(e) => setTotal(parseFloat(e.target.value) || 0)}
                    required
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1', boxSizing: 'border-box' }}
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 'bold', color: '#475569', marginBottom: '5px' }}>Estado</label>
                  <select
                    value={estadoPedido}
                    onChange={(e) => setEstadoPedido(e.target.value)}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1', boxSizing: 'border-box', backgroundColor: '#fff' }}
                  >
                    <option value="PENDIENTE">PENDIENTE</option>
                    <option value="COMPLETADO">COMPLETADO</option>
                  </select>
                </div>

                <button
                  type="submit"
                  disabled={cargandoEnvio}
                  style={{
                    padding: '9px 16px',
                    backgroundColor: '#2563eb',
                    color: '#ffffff',
                    border: 'none',
                    borderRadius: '6px',
                    fontWeight: '600',
                    cursor: cargandoEnvio ? 'not-allowed' : 'pointer'
                  }}
                >
                  {cargandoEnvio ? 'Guardando...' : 'Guardar Pedido'}
                </button>
              </form>
            </section>
          ) : (
            <div style={{ backgroundColor: '#e0f2fe', color: '#0369a1', padding: '12px 16px', borderRadius: '6px', border: '1px solid #bae6fd', marginBottom: '25px', fontSize: '0.9rem' }}>
              ℹ️ Se requiere el rol de <strong>usuario</strong> para registrar nuevos pedidos.
            </div>
          )}

          {/* Listado de Pedidos */}
          <section style={{ backgroundColor: '#ffffff', padding: '20px', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
            <h2 style={{ marginTop: 0, fontSize: '1.2rem', color: '#1e293b', borderBottom: '1px solid #f1f5f9', paddingBottom: '10px' }}>
              📋 Listado de Pedidos (ArangoDB)
            </h2>

            {error && (
              <div style={{ backgroundColor: '#fef2f2', color: '#dc2626', padding: '12px 16px', borderRadius: '6px', border: '1px solid #fecaca', marginBottom: '15px' }}>
                <strong>Error:</strong> {error}
              </div>
            )}

            {pedidos ? (
              <div style={{ overflowX: 'auto', marginTop: '15px' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
                  <thead>
                    <tr style={{ backgroundColor: '#f8fafc', borderBottom: '2px solid #e2e8f0' }}>
                      <th style={{ padding: '10px', fontSize: '0.85rem', color: '#475569' }}>ID</th>
                      <th style={{ padding: '10px', fontSize: '0.85rem', color: '#475569' }}>Cliente</th>
                      <th style={{ padding: '10px', fontSize: '0.85rem', color: '#475569' }}>Producto</th>
                      <th style={{ padding: '10px', fontSize: '0.85rem', color: '#475569' }}>Cant.</th>
                      <th style={{ padding: '10px', fontSize: '0.85rem', color: '#475569' }}>Total</th>
                      <th style={{ padding: '10px', fontSize: '0.85rem', color: '#475569' }}>Estado</th>
                      {esAdmin && <th style={{ padding: '10px', fontSize: '0.85rem', color: '#475569' }}>Acción</th>}
                    </tr>
                  </thead>
                  <tbody>
                    {pedidos.length === 0 ? (
                      <tr><td colSpan={esAdmin ? 7 : 6} style={{ padding: '20px', textAlign: 'center', color: '#64748b' }}>No hay pedidos registrados.</td></tr>
                    ) : (
                      pedidos.map((p, index) => {
                        const itemKey = p._key || p.id || index;
                        return (
                          <tr key={itemKey} style={{ borderBottom: '1px solid #f1f5f9' }}>
                            <td style={{ padding: '12px 10px', fontFamily: 'monospace', fontSize: '0.9rem' }}>{p._key || p.id || 'N/A'}</td>
                            <td style={{ padding: '12px 10px', fontWeight: '500' }}>{p.cliente}</td>
                            <td style={{ padding: '12px 10px' }}>{p.producto || 'N/A'}</td>
                            <td style={{ padding: '12px 10px' }}>{p.cantidad || 1}</td>
                            <td style={{ padding: '12px 10px', fontWeight: 'bold', color: '#0f172a' }}>${p.total}</td>
                            <td style={{ padding: '12px 10px' }}>
                              <span style={{
                                padding: '4px 8px', borderRadius: '12px', fontSize: '0.75rem', fontWeight: 'bold',
                                backgroundColor: p.estado === 'COMPLETADO' ? '#dcfce7' : '#fef9c3',
                                color: p.estado === 'COMPLETADO' ? '#15803d' : '#a16207'
                              }}>
                                {p.estado}
                              </span>
                            </td>
                            {esAdmin && (
                              <td style={{ padding: '12px 10px' }}>
                                <button onClick={() => handleEliminarPedido(p)} style={{ padding: '4px 8px', backgroundColor: '#fee2e2', color: '#dc2626', border: 'none', borderRadius: '4px', cursor: 'pointer', fontSize: '0.8rem', fontWeight: 'bold' }}>
                                  Eliminar
                                </button>
                              </td>
                            )}
                          </tr>
                        );
                      })
                    )}
                  </tbody>
                </table>
              </div>
            ) : (!error && <p style={{ color: '#64748b', fontStyle: 'italic' }}>Cargando pedidos...</p>)}
          </section>
        </div>
      )}

      {/* PESTAÑA: PRODUCTOS */}
      {tabActiva === 'productos' && (
        <div>
          {esAdmin ? (
            <section style={{ backgroundColor: '#ffffff', padding: '20px', borderRadius: '8px', border: '1px solid #e2e8f0', marginBottom: '25px' }}>
              <h2 style={{ marginTop: 0, fontSize: '1.2rem', color: '#1e293b', borderBottom: '1px solid #f1f5f9', paddingBottom: '10px' }}>
                ➕ Agregar Nuevo Producto (Solo Admin)
              </h2>
              <form onSubmit={handleCrearProducto} style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '15px', marginTop: '15px', alignItems: 'end' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 'bold', color: '#475569', marginBottom: '5px' }}>Nombre</label>
                  <input type="text" value={nombreProd} onChange={(e) => setNombreProd(e.target.value)} placeholder="Ej. Teclado Mecánico" required style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1', boxSizing: 'border-box' }} />
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 'bold', color: '#475569', marginBottom: '5px' }}>Precio ($)</label>
                  <input type="number" step="0.01" value={precioProd} onChange={(e) => setPrecioProd(e.target.value)} placeholder="0.00" required style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1', boxSizing: 'border-box' }} />
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 'bold', color: '#475569', marginBottom: '5px' }}>Stock</label>
                  <input type="number" value={stockProd} onChange={(e) => setStockProd(e.target.value)} placeholder="10" required style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1', boxSizing: 'border-box' }} />
                </div>
                <button type="submit" disabled={cargandoProd} style={{ padding: '9px 16px', backgroundColor: '#16a34a', color: '#ffffff', border: 'none', borderRadius: '6px', fontWeight: '600', cursor: cargandoProd ? 'not-allowed' : 'pointer' }}>
                  {cargandoProd ? 'Guardando...' : 'Agregar Producto'}
                </button>
              </form>
            </section>
          ) : (
            <div style={{ backgroundColor: '#fef3c7', color: '#92400e', padding: '12px 16px', borderRadius: '6px', border: '1px solid #fde68a', marginBottom: '25px', fontSize: '0.9rem' }}>
              🔒 Vista en modo lectura. Se requieren privilegios de <strong>Administrador</strong> para agregar o eliminar productos.
            </div>
          )}

          <section style={{ backgroundColor: '#ffffff', padding: '20px', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
            <h2 style={{ marginTop: 0, fontSize: '1.2rem', color: '#1e293b', borderBottom: '1px solid #f1f5f9', paddingBottom: '10px' }}>
              🏷️ Lista de Productos Disponibles
            </h2>
            {error && <div style={{ backgroundColor: '#fef2f2', color: '#dc2626', padding: '12px 16px', borderRadius: '6px', border: '1px solid #fecaca', marginBottom: '15px' }}><strong>Error:</strong> {error}</div>}
            <div style={{ overflowX: 'auto', marginTop: '15px' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
                <thead>
                  <tr style={{ backgroundColor: '#f8fafc', borderBottom: '2px solid #e2e8f0' }}>
                    <th style={{ padding: '10px', fontSize: '0.85rem', color: '#475569' }}>ID</th>
                    <th style={{ padding: '10px', fontSize: '0.85rem', color: '#475569' }}>Nombre</th>
                    <th style={{ padding: '10px', fontSize: '0.85rem', color: '#475569' }}>Precio</th>
                    <th style={{ padding: '10px', fontSize: '0.85rem', color: '#475569' }}>Stock</th>
                    {esAdmin && <th style={{ padding: '10px', fontSize: '0.85rem', color: '#475569' }}>Acción</th>}
                  </tr>
                </thead>
                <tbody>
                  {productos.length === 0 ? (
                    <tr><td colSpan={esAdmin ? 5 : 4} style={{ padding: '20px', textAlign: 'center', color: '#64748b' }}>No hay productos registrados.</td></tr>
                  ) : (
                    productos.map((prod, index) => {
                      const prodKey = prod._key || prod.id || index;
                      return (
                        <tr key={prodKey} style={{ borderBottom: '1px solid #f1f5f9' }}>
                          <td style={{ padding: '12px 10px', fontFamily: 'monospace', fontSize: '0.9rem' }}>{prod._key || prod.id || 'N/A'}</td>
                          <td style={{ padding: '12px 10px', fontWeight: '500' }}>{prod.nombre}</td>
                          <td style={{ padding: '12px 10px', fontWeight: 'bold', color: '#0f172a' }}>${prod.precio}</td>
                          <td style={{ padding: '12px 10px' }}>{prod.stock}</td>
                          {esAdmin && (
                            <td style={{ padding: '12px 10px' }}>
                              <button onClick={() => handleEliminarProducto(prod)} style={{ padding: '4px 8px', backgroundColor: '#fee2e2', color: '#dc2626', border: 'none', borderRadius: '4px', cursor: 'pointer', fontSize: '0.8rem', fontWeight: 'bold' }}>
                                Eliminar
                              </button>
                            </td>
                          )}
                        </tr>
                      );
                    })
                  )}
                </tbody>
              </table>
            </div>
          </section>
        </div>
      )}

      {/* PESTAÑA: PERFIL */}
      {tabActiva === 'perfil' && (
        <section style={{ backgroundColor: '#ffffff', padding: '20px', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
          <h2 style={{ marginTop: 0, fontSize: '1.2rem', color: '#1e293b', borderBottom: '1px solid #f1f5f9', paddingBottom: '10px' }}>
            👤 Perfil del Usuario (Claims JWT)
          </h2>
          {usuario ? (
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '15px', marginTop: '15px' }}>
              <div style={{ background: '#f8fafc', padding: '15px', borderRadius: '6px', border: '1px solid #e2e8f0' }}>
                <span style={{ fontSize: '0.8rem', color: '#64748b', textTransform: 'uppercase', fontWeight: 'bold' }}>Email Registrado</span>
                <p style={{ margin: '6px 0 0 0', fontWeight: '500', color: '#0f172a', fontSize: '1.05rem' }}>{usuario.email}</p>
              </div>
              <div style={{ background: '#f8fafc', padding: '15px', borderRadius: '6px', border: '1px solid #e2e8f0' }}>
                <span style={{ fontSize: '0.8rem', color: '#64748b', textTransform: 'uppercase', fontWeight: 'bold' }}>Roles / Grupos Cognito</span>
                <p style={{ margin: '6px 0 0 0', fontWeight: '500', color: '#0f172a', fontSize: '1.05rem' }}>
                  {usuario.roles && usuario.roles.length > 0 ? usuario.roles.join(', ') : 'Sin roles asignados'}
                </p>
              </div>
            </div>
          ) : (
            <p style={{ color: '#64748b', fontStyle: 'italic' }}>Cargando datos del usuario...</p>
          )}
        </section>
      )}
    </div>
  );
}