import { BrowserRouter, Routes, Route } from "react-router-dom";
import { AuthProvider } from "./context/AuthProvider";
import Login from "./pages/Login";
import Registrar from "./pages/Registrar";
import ConfirmarCuenta from "./pages/ConfirmarCuenta";
import Dashboard from "./pages/Dashboard";
import OlvidePassword from "./pages/OlvidePassword";
import NuevoPassword from "./pages/NuevoPassword";

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/" element={<Login />} />
          <Route path="/registrar" element={<Registrar />} />
          <Route path="/confirmar" element={<ConfirmarCuenta />} />
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/olvide-password" element={<OlvidePassword />} />
          <Route path="/nuevo-password" element={<NuevoPassword />} />
          <Route path="/nuevo-password/:token" element={<NuevoPassword />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
