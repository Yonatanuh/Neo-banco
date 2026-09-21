import { useState, useEffect } from "react";
import { Link, useParams, useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { Lock, Key, ArrowRight, Loader } from "lucide-react";
import clienteAxios from "../config/axios";

const NuevoPassword = () => {
  const [token, setToken] = useState("");
  const [password, setPassword] = useState("");
  const [alerta, setAlerta] = useState({ error: false, msg: "" });
  const [cargando, setCargando] = useState(false);
  const [modificado, setModificado] = useState(false);

  const params = useParams();
  const navigate = useNavigate();

  useEffect(() => {
    if (params.token) {
      setToken(params.token);
    }
  }, [params]);

  const handleSubmit = async (e) => {
    e.preventDefault();

    if ([token, password].includes("")) {
      setAlerta({ error: true, msg: "Todos los campos son obligatorios" });
      return;
    }

    if (password.length < 8) {
      setAlerta({
        error: true,
        msg: "La contraseña debe ser mínimo de 8 caracteres",
      });
      return;
    }

    setCargando(true);
    try {
      const { data } = await clienteAxios.post("/usuarios/nuevo-password", {
        token,
        password,
      });

      setAlerta({
        error: false,
        msg: data.mensaje || "Contraseña modificada correctamente",
      });
      setModificado(true);
      setTimeout(() => navigate("/"), 3000);
    } catch (error) {
      setAlerta({
        error: true,
        msg: error.response?.data?.mensaje || "Token no válido o expirado",
      });
    } finally {
      setCargando(false);
    }
  };

  return (
    <div className="flex h-screen bg-[#050510]">
      {/* LEFT PANEL - Branded */}
      <div className="hidden lg:flex w-1/2 bg-[radial-gradient(ellipse_at_center,_var(--tw-gradient-stops))] from-cyan-900/20 via-[#050510] to-[#050510] flex-col justify-center items-center relative overflow-hidden">
        {/* Decorative background shapes */}
        <div className="absolute top-20 left-20 w-72 h-72 bg-cyan-500/10 rounded-full " />
        <div className="absolute bottom-20 right-20 w-72 h-72 bg-blue-600/10 rounded-full " />

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8 }}
          className="z-10 text-center"
        >
          <div className="mb-8 relative flex justify-center">
            {/* Animated Floating Card */}
            <motion.div
              animate={{ rotate: [0, 5, -5, 0], y: [0, -10, 0] }}
              transition={{ repeat: Infinity, duration: 6, ease: "easeInOut" }}
              className="w-64 h-40 bg-gradient-to-tr from-cyan-600 to-blue-800 rounded-2xl shadow-2xl border border-white/20 p-5 relative overflow-hidden"
            >
              <div className="absolute -right-10 -top-10 w-32 h-32 bg-white/10 rounded-full " />
              <div className="absolute -left-10 -bottom-10 w-32 h-32 bg-black/20 rounded-full " />
              <div className="w-10 h-8 bg-yellow-400/80 rounded mb-6" />
              <div className="h-2 w-3/4 bg-white/40 rounded mb-2" />
              <div className="h-2 w-1/2 bg-white/40 rounded mb-4" />
            </motion.div>
          </div>

          <h1 className="text-5xl font-black text-transparent bg-clip-text bg-gradient-to-r from-cyan-400 to-blue-500 mb-4 tracking-tight">
            NEO BANCO
          </h1>
          <p className="text-slate-400 text-lg max-w-sm mx-auto">
            Protege tu cuenta con una nueva contraseña segura.
          </p>
        </motion.div>
      </div>

      {/* RIGHT PANEL - Form */}
      <div className="w-full lg:w-1/2 flex justify-center items-center p-8 relative">
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-cyan-900/10 via-[#050510] to-[#050510] lg:hidden" />

        <motion.div
          initial={{ opacity: 0, x: 50 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.6, ease: "easeOut" }}
          className="w-full max-w-md bg-[#0a0a1a]/80 border border-white/10 rounded-3xl p-8 shadow-2xl relative z-10"
        >
          <div className="text-center mb-8">
            <h2 className="text-3xl font-bold text-white mb-2">
              Nueva Contraseña
            </h2>
            <p className="text-slate-400">Restablece tu acceso</p>
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

            {!modificado && (
              <>
                <div className="space-y-1">
                  <label className="text-sm font-medium text-slate-300 ml-1">
                    Código de Seguridad (Token)
                  </label>
                  <div className="relative">
                    <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-500">
                      <Key size={18} />
                    </div>
                    <input
                      type="text"
                      placeholder="Pega el código aquí"
                      className="w-full pl-10 p-3 rounded-xl bg-black/40 border border-white/10 text-white focus:outline-none focus:border-cyan-400 transition-colors"
                      value={token}
                      onChange={(e) => {
                        setToken(e.target.value);
                        if (alerta.msg) setAlerta({ error: false, msg: "" });
                      }}
                      disabled={cargando}
                    />
                  </div>
                </div>

                <div className="space-y-1">
                  <label className="text-sm font-medium text-slate-300 ml-1">
                    Nueva Contraseña
                  </label>
                  <div className="relative">
                    <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-500">
                      <Lock size={18} />
                    </div>
                    <input
                      type="password"
                      placeholder="Mínimo 8 caracteres"
                      className="w-full pl-10 p-3 rounded-xl bg-black/40 border border-white/10 text-white focus:outline-none focus:border-cyan-400 transition-colors"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      disabled={cargando}
                    />
                  </div>
                </div>

                <button
                  type="submit"
                  disabled={cargando}
                  className="w-full py-3 px-4 bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-white rounded-xl font-bold shadow-[0_0_20px_rgba(6,182,212,0.4)] transition-all flex justify-center items-center gap-2 group disabled:opacity-70 disabled:cursor-not-allowed"
                >
                  {cargando ? (
                    <>
                      <Loader size={20} className="animate-spin" /> Guardando...
                    </>
                  ) : (
                    <>
                      Actualizar Contraseña{" "}
                      <ArrowRight
                        size={18}
                        className="group-hover:translate-x-1 transition-transform"
                      />
                    </>
                  )}
                </button>
              </>
            )}

            {modificado && (
              <div className="text-center mt-6">
                <Link
                  to="/"
                  className="inline-block px-6 py-3 bg-white/10 hover:bg-white/20 text-white font-bold rounded-xl transition-colors"
                >
                  Iniciar Sesión Ahora
                </Link>
              </div>
            )}
          </form>
        </motion.div>
      </div>
    </div>
  );
};

export default NuevoPassword;
