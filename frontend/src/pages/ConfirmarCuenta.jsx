import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { ShieldCheck, ArrowLeft, Loader, RefreshCw } from "lucide-react";
import clienteAxios from "../config/axios";

const ConfirmarCuenta = () => {
  const [token, setToken] = useState("");
  const [email, setEmail] = useState(
    () => localStorage.getItem("email_pendiente_confirmacion") || ""
  );
  const [mostrarEmail, setMostrarEmail] = useState(false);
  const [reenviando, setReenviando] = useState(false);
  const [alerta, setAlerta] = useState({ error: false, msg: "" });
  const [cuentaConfirmada, setCuentaConfirmada] = useState(false);
  const [verificando, setVerificando] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    const tokenLimpio = token.trim();

    if (tokenLimpio.length !== 6) {
      setAlerta({
        error: true,
        msg: "El código debe tener exactamente 6 dígitos",
      });
      return;
    }

    setVerificando(true);
    try {
      const { data } = await clienteAxios.post("/usuarios/confirmar", {
        token: tokenLimpio,
      });
      setAlerta({ error: false, msg: data.mensaje });
      setCuentaConfirmada(true);
      setToken("");
      localStorage.removeItem("email_pendiente_confirmacion");

      setTimeout(() => {
        navigate("/");
      }, 2000);
    } catch (error) {
      setAlerta({
        error: true,
        msg: error.response?.data?.mensaje || "Código inválido o expirado",
      });
    } finally {
      setVerificando(false);
    }
  };

  const handleReenviarCodigo = async () => {
    if (!email || email.trim() === "") {
      setMostrarEmail(true);
      setAlerta({
        error: true,
        msg: "Ingresa tu correo electrónico para reenviar el código",
      });
      return;
    }

    setReenviando(true);
    setAlerta({ error: false, msg: "" });
    try {
      const { data } = await clienteAxios.post("/usuarios/reenviar-codigo", {
        email: email.trim(),
      });
      setAlerta({ error: false, msg: data.mensaje });
    } catch (error) {
      setAlerta({
        error: true,
        msg: error.response?.data?.mensaje || "Error al reenviar el código",
      });
    } finally {
      setReenviando(false);
    }
  };

  return (
    <div className="flex h-screen bg-[#050510]">
      {/* LEFT PANEL - Branded */}
      <div className="hidden lg:flex w-1/2 bg-[radial-gradient(ellipse_at_center,_var(--tw-gradient-stops))] from-cyan-900/20 via-[#050510] to-[#050510] flex-col justify-center items-center relative overflow-hidden">
        <div className="absolute top-20 left-20 w-72 h-72 bg-cyan-500/10 rounded-full" />
        <div className="absolute bottom-20 right-20 w-72 h-72 bg-blue-600/10 rounded-full" />

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8 }}
          className="z-10 text-center"
        >
          <div className="mb-8 flex justify-center">
            <motion.div
              animate={{ scale: [1, 1.05, 1] }}
              transition={{ repeat: Infinity, duration: 3, ease: "easeInOut" }}
              className="w-24 h-24 bg-gradient-to-tr from-cyan-500 to-blue-600 rounded-2xl shadow-2xl flex items-center justify-center"
            >
              <ShieldCheck className="w-12 h-12 text-white" />
            </motion.div>
          </div>

          <h1 className="text-5xl font-black text-transparent bg-clip-text bg-gradient-to-r from-cyan-400 to-blue-500 mb-4 tracking-tight">
            NEO BANCO
          </h1>
          <p className="text-slate-400 text-lg max-w-sm mx-auto">
            Solo un paso más para acceder a tu cuenta. Verifica tu identidad
            con el código que enviamos a tu correo.
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
            <div className="flex justify-center mb-4 text-cyan-400">
              <ShieldCheck size={40} />
            </div>
            <h2 className="text-3xl font-bold text-white mb-2">
              Verificar Cuenta
            </h2>
            <p className="text-slate-400">
              Ingresa el código de 6 dígitos que enviamos a tu correo
            </p>
          </div>

          {alerta.msg && (
            <motion.div
              initial={{ opacity: 0, y: -10 }}
              animate={{ opacity: 1, y: 0 }}
              className={`p-4 rounded-xl text-center font-bold text-sm mb-5 ${
                alerta.error
                  ? "bg-rose-500/10 text-rose-400 border border-rose-500/20"
                  : "bg-emerald-500/10 text-emerald-400 border border-emerald-500/20"
              }`}
            >
              {alerta.msg}
            </motion.div>
          )}

          {!cuentaConfirmada ? (
            <>
              <form onSubmit={handleSubmit} className="space-y-5">
                <div className="space-y-1">
                  <label className="text-sm font-medium text-slate-300 ml-1">
                    Código de Verificación
                  </label>
                  <input
                    type="text"
                    inputMode="numeric"
                    maxLength={6}
                    className="w-full px-4 py-4 bg-black/40 border border-white/10 rounded-xl text-center text-3xl font-mono tracking-[0.3em] text-cyan-300 focus:outline-none focus:border-cyan-400 transition-colors placeholder:text-slate-600"
                    placeholder="000000"
                    value={token}
                    onChange={(e) => {
                      const val = e.target.value.replace(/\D/g, "");
                      setToken(val);
                      if (alerta.msg) setAlerta({ error: false, msg: "" });
                    }}
                  />
                </div>

                <motion.button
                  whileHover={{ scale: 1.02 }}
                  whileTap={{ scale: 0.98 }}
                  type="submit"
                  disabled={verificando || token.length !== 6}
                  className="w-full flex justify-center items-center gap-2 bg-gradient-to-r from-cyan-500 to-blue-600 shadow-[0_0_20px_rgba(6,182,212,0.4)] hover:shadow-[0_0_25px_rgba(6,182,212,0.6)] text-white font-bold py-3.5 rounded-xl transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {verificando ? (
                    <>
                      <Loader className="animate-spin" size={20} />
                      <span>Verificando...</span>
                    </>
                  ) : (
                    "Activar Mi Cuenta"
                  )}
                </motion.button>
              </form>

              {/* Spam Notice */}
              <div className="mt-6 p-3 bg-amber-500/5 border border-amber-500/15 rounded-xl">
                <p className="text-xs text-amber-300/80 leading-relaxed">
                  <strong>¿No encuentras el correo?</strong> Revisa la carpeta
                  de <strong>Spam</strong> o{" "}
                  <strong>Correo no deseado</strong> en tu bandeja de entrada.
                </p>
              </div>

              {/* Resend Section */}
              <div className="mt-4 text-center space-y-3">
                {mostrarEmail && (
                  <div className="text-left">
                    <label className="block text-xs text-slate-400 mb-1.5 ml-1">
                      Correo electrónico de tu cuenta
                    </label>
                    <input
                      type="email"
                      className="w-full px-3 py-2.5 text-sm bg-black/40 border border-white/10 rounded-xl text-white focus:outline-none focus:border-cyan-400 transition-colors"
                      placeholder="tu@email.com"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                    />
                  </div>
                )}

                <button
                  type="button"
                  onClick={() => {
                    if (!mostrarEmail && !email) {
                      setMostrarEmail(true);
                    } else {
                      handleReenviarCodigo();
                    }
                  }}
                  disabled={reenviando}
                  className="inline-flex items-center gap-1.5 text-xs text-cyan-400 hover:text-cyan-300 font-medium transition-colors disabled:opacity-50"
                >
                  <RefreshCw
                    size={12}
                    className={reenviando ? "animate-spin" : ""}
                  />
                  {reenviando
                    ? "Reenviando código..."
                    : "Solicitar nuevo código"}
                </button>
              </div>
            </>
          ) : (
            <motion.div
              initial={{ opacity: 0, scale: 0.9 }}
              animate={{ opacity: 1, scale: 1 }}
              className="text-center py-6"
            >
              <div className="w-16 h-16 bg-emerald-500/20 rounded-full flex items-center justify-center mx-auto mb-4">
                <ShieldCheck className="w-8 h-8 text-emerald-400" />
              </div>
              <h3 className="text-xl font-bold text-white mb-2">
                ¡Cuenta Verificada!
              </h3>
              <p className="text-slate-400 text-sm mb-6">
                Redirigiendo al inicio de sesión...
              </p>
              <Link
                to="/"
                className="inline-block bg-gradient-to-r from-cyan-500 to-blue-600 shadow-[0_0_20px_rgba(6,182,212,0.4)] text-white font-bold py-3 px-8 rounded-xl hover:shadow-[0_0_25px_rgba(6,182,212,0.6)] transition-all"
              >
                Iniciar Sesión
              </Link>
            </motion.div>
          )}

          <div className="mt-8 text-center text-slate-400 text-sm">
            <Link
              to="/"
              className="flex items-center justify-center gap-2 hover:text-cyan-400 transition-colors"
            >
              <ArrowLeft size={16} /> Volver al Inicio de Sesión
            </Link>
          </div>
        </motion.div>
      </div>
    </div>
  );
};

export default ConfirmarCuenta;
