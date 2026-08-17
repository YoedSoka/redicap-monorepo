import { Navigate, Route, Routes } from 'react-router-dom'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import CapturaPage from './pages/CapturaPage'
import DigitalizacionPage from './pages/DigitalizacionPage'
import VerificacionPage from './pages/VerificacionPage'
import AdminUsuariosPage from './pages/AdminUsuariosPage'
import AdminPartidosPage from './pages/AdminPartidosPage'
import AdminCatalogoPage from './pages/AdminCatalogoPage'
import ConsultaPage from './pages/ConsultaPage'
import RutaProtegida from './components/RutaProtegida'

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/captura"
        element={
          <RutaProtegida rolesPermitidos={['CAPTURISTA']}>
            <CapturaPage />
          </RutaProtegida>
        }
      />
      <Route
        path="/digitalizacion"
        element={
          <RutaProtegida rolesPermitidos={['DIGITALIZADOR']}>
            <DigitalizacionPage />
          </RutaProtegida>
        }
      />
      <Route
        path="/verificacion"
        element={
          <RutaProtegida rolesPermitidos={['VERIFICADOR']}>
            <VerificacionPage />
          </RutaProtegida>
        }
      />
      <Route
        path="/admin/usuarios"
        element={
          <RutaProtegida rolesPermitidos={['ADMINISTRADOR']}>
            <AdminUsuariosPage />
          </RutaProtegida>
        }
      />
      <Route
        path="/admin/partidos"
        element={
          <RutaProtegida rolesPermitidos={['ADMINISTRADOR']}>
            <AdminPartidosPage />
          </RutaProtegida>
        }
      />
      <Route
        path="/admin/catalogo"
        element={
          <RutaProtegida rolesPermitidos={['ADMINISTRADOR']}>
            <AdminCatalogoPage />
          </RutaProtegida>
        }
      />
      <Route
        path="/consulta"
        element={
          <RutaProtegida rolesPermitidos={['CONSULTOR_PUBLICO']}>
            <ConsultaPage />
          </RutaProtegida>
        }
      />
      <Route
        path="/"
        element={
          <RutaProtegida>
            <DashboardPage />
          </RutaProtegida>
        }
      />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default App
