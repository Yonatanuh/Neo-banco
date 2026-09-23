import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import {
  User,
  Mail,
  Lock,
  UserPlus,
  ArrowRight,
  Loader,
  Eye,
  EyeOff,
} from "lucide-react";
import clienteAxios from "../config/axios";

const Registrar = () => {
  const [nombre, setNombre] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [mostrarPassword, setMostrarPassword] = useState(false);
  const [alerta, setAlerta] = useState({ error: false, msg: "" });
  const [cargando, setCargando] = useState(false);

  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();

    if ([nombre, email, password].includes("")) {
      setAlerta({ error: true, msg: "Todos los campos son obligatorios" });
      return;
    }

    // Validación estricta de formato de correo (Regex)
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      setAlerta({ error: true, msg: "El correo electrónico no es válido" });
      return;
    }

    if (password.length < 8) {
      setAlerta({
        error: true,
        msg: "La contraseña debe tener al menos 8 caracteres",
      });
      return;
    }

    setCargando(true);
    try {
      // Petición al backend seguro
      const { data } = await clienteAxios.post("/usuarios/registro", {
        nombre,
        email,
        password,
      });

      localStorage.setItem("email_pendiente_confirmacion", email);

      setAlerta({
        error: false,
        msg: data.mensaje || "Cuenta creada. Revisa tu correo (y Spam) para activarla...",
      });

      // Redirigir a confirmar después de 2.5 segundos
      setTimeout(() => {
        navigate("/confirmar");
      }, 2500);
    } catch (error) {
      setAlerta({
        error: true,
        msg: error.response?.data?.mensaje || "Hubo un error al registrarse",
      });

      if (error.response?.data?.unconfirmed) {
        setTimeout(() => {
          navigate("/confirmar");
        }, 3000);
      }
    } finally {
      if (!alerta.error && !alerta.msg.includes("Cuenta creada")) {
        setCargando(false);
      }
    }
  };

  return (
    <div className="flex h-screen bg-[#050510]">
      {/* LEFT PANEL - Form */}
      <div className="w-full lg:w-1/2 flex justify-center items-center p-8 relative">
        {/* Mobile background effect */}
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-blue-900/10 via-[#050510] to-[#050510] lg:hidden" />

        <motion.div
          initial={{ opacity: 0, x: -50 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.6, ease: "easeOut" }}
          className="w-full max-w-md bg-[#0a0a1a]/80  border border-white/10 rounded-3xl p-8 shadow-2xl relative z-10"
        >
          <div className="text-center mb-8">
            <div className="flex justify-center mb-4 text-cyan-400">
              <UserPlus size={40} />
            </div>
            <h2 className="text-3xl font-bold text-white mb-2">Crear Cuenta</h2>
            <p className="text-slate-400">Únete a la revolución financiera</p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-5">
            {alerta.msg && (
              <motion.div
                initial={{ opacity: 0, y: -10 }}
                animate={{ opacity: 1, y: 0 }}
                className={`p-4 rounded-xl text-center font-bold text-sm ${alerta.error ? "bg-rose-500/10 text-rose-400 border border-rose-500/20" : "bg-emerald-500/10 text-emerald-400 border border-emerald-500/20"}`}
              >
                {alerta.msg}
              </motion.div>
            )}

            <div className="space-y-1">
              <label className="text-sm font-medium text-slate-300 ml-1">
                Nombre Completo
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-500">
                  <User size={18} />
                </div>
                <input
                  type="text"
                  placeholder="Tu Nombre"
                  className="w-full pl-10 p-3 rounded-xl bg-black/40 border border-white/10 text-white focus:outline-none focus:border-cyan-400 transition-colors"
                  value={nombre}
                  onChange={(e) => setNombre(e.target.value)}
                  maxLength="50"
                  disabled={cargando}
                />
              </div>
            </div>

            <div className="space-y-1">
              <label className="text-sm font-medium text-slate-300 ml-1">
                Correo Electrónico
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-500">
                  <Mail size={18} />
                </div>
                <input
                  type="email"
                  placeholder="tu@email.com"
                  className="w-full pl-10 p-3 rounded-xl bg-black/40 border border-white/10 text-white focus:outline-none focus:border-cyan-400 transition-colors"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  maxLength="50"
                  disabled={cargando}
                />
              </div>
            </div>

            <div className="space-y-1">
              <label className="text-sm font-medium text-slate-300 ml-1">
                Contraseña
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-500">
                  <Lock size={18} />
                </div>
                <input
                  type={mostrarPassword ? "text" : "password"}
                  placeholder="Mínimo 8 caracteres"
                  className="w-full pl-10 pr-10 p-3 rounded-xl bg-black/40 border border-white/10 text-white focus:outline-none focus:border-cyan-400 transition-colors"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  maxLength="50"
                  disabled={cargando}
                />
                <button
                  type="button"
                  onClick={() => setMostrarPassword(!mostrarPassword)}
                  className="absolute inset-y-0 right-0 pr-3 flex items-center text-slate-500 hover:text-cyan-400 transition-colors"
                >
                  {mostrarPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </div>

            <motion.button
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
              type="submit"
              disabled={cargando}
              className="w-full flex justify-center items-center gap-2 bg-gradient-to-r from-cyan-500 to-blue-600 shadow-[0_0_20px_rgba(6,182,212,0.4)] hover:shadow-[0_0_25px_rgba(6,182,212,0.6)] text-white font-bold py-3.5 rounded-xl transition-all disabled:opacity-70 disabled:cursor-not-allowed mt-4"
            >
              {cargando ? (
                <>
                  <Loader className="animate-spin" size={20} />
                  <span>Procesando...</span>
                </>
              ) : (
                <>
                  <span>Registrarse</span>
                  <ArrowRight size={18} />
                </>
              )}
            </motion.button>
          </form>

          <div className="mt-8 text-center text-sm text-slate-400">
            ¿Ya tienes una cuenta?{" "}
            <Link
              to="/"
              className="text-cyan-400 hover:text-cyan-300 font-medium transition-colors"
            >
              Inicia Sesión
            </Link>
          </div>
        </motion.div>
      </div>

      {/* RIGHT PANEL - Branded (Mirrored from Login) */}
      <div className="hidden lg:flex w-1/2 bg-[radial-gradient(ellipse_at_center,_var(--tw-gradient-stops))] from-blue-900/20 via-[#050510] to-[#050510] flex-col justify-center items-center relative overflow-hidden">
        {/* Decorative background shapes */}
        <div className="absolute top-20 right-20 w-72 h-72 bg-blue-600/10 rounded-full " />
        <div className="absolute bottom-20 left-20 w-72 h-72 bg-cyan-500/10 rounded-full " />

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8 }}
          className="z-10 text-center"
        >
          <div className="mb-8 relative flex justify-center">
            {/* Animated Floating Card */}
            <motion.div
              animate={{ rotate: [0, -5, 5, 0], y: [0, -10, 0] }}
              transition={{
                repeat: Infinity,
                duration: 6,
                ease: "easeInOut",
                delay: 0.5,
              }}
              className="w-64 h-40 bg-gradient-to-bl from-blue-600 to-cyan-800 rounded-2xl shadow-2xl border border-white/20 p-5 relative overflow-hidden"
            >
              <div className="absolute -left-10 -top-10 w-32 h-32 bg-white/10 rounded-full " />
              <div className="absolute -right-10 -bottom-10 w-32 h-32 bg-black/20 rounded-full " />
              <div className="w-10 h-8 bg-slate-300/80 rounded mb-6" />
              <div className="h-2 w-3/4 bg-white/40 rounded mb-2" />
              <div className="h-2 w-1/2 bg-white/40 rounded mb-4" />
              <div className="flex justify-between items-center text-white/80 font-mono text-xs">
                <span>NEW USER</span>
                <span>01/30</span>
              </div>
            </motion.div>
          </div>

          <h1 className="text-5xl font-black text-transparent bg-clip-text bg-gradient-to-r from-blue-400 to-cyan-500 mb-4 tracking-tight">
            ÚNETE HOY
          </h1>
          <p className="text-slate-400 text-lg max-w-sm mx-auto">
            Abre tu cuenta en minutos. Sin comisiones ocultas, sin papeleo.
          </p>
        </motion.div>
      </div>
    </div>
  );
};

export default Registrar;
