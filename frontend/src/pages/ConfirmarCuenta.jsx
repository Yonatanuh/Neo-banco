import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import clienteAxios from "../config/axios";

const ConfirmarCuenta = () => {
  const [token, setToken] = useState("");
  const [email, setEmail] = useState(() => localStorage.getItem("email_pendiente_confirmacion") || "");
  const [mostrarReenviar, setMostrarReenviar] = useState(false);
  const [reenviando, setReenviando] = useState(false);
  const [alerta, setAlerta] = useState({ error: false, msg: "" });
  const [cuentaConfirmada, setCuentaConfirmada] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();

    const tokenLimpio = token.trim();

    if (tokenLimpio.length !== 6) {
      setAlerta({ error: true, msg: "El código debe tener 6 dígitos exactos" });
      return;
    }

    try {
      const { data } = await clienteAxios.post("/usuarios/confirmar", {
        token: tokenLimpio,
      });
      setAlerta({ error: false, msg: data.mensaje });
      setCuentaConfirmada(true);
      setToken("");
      localStorage.removeItem("email_pendiente_confirmacion");

      // Redirigir automáticamente al login mucho más rápido
      setTimeout(() => {
        navigate("/");
      }, 1000);
    } catch (error) {
      setAlerta({
        error: true,
        msg: error.response?.data?.mensaje || "Hubo un error",
      });
    }
  };

  const handleReenviarCodigo = async () => {
    if (!email || email.trim() === "") {
      setAlerta({ error: true, msg: "Por favor, ingresa tu correo para reenviar el código" });
      setMostrarReenviar(true);
      return;
    }

    setReenviando(true);
    try {
      const { data } = await clienteAxios.post("/usuarios/reenviar-codigo", {
        email: email.trim(),
      });
      setAlerta({ error: false, msg: data.mensaje });
      if (data.token) {
        setToken(data.token);
      }
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
    <div className="min-h-screen flex items-center justify-center bg-[#050510] text-white bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-[#0f172a] via-[#050510] to-black px-4">
      <div className="bg-white/5 backdrop-blur-xl border border-white/10 p-8 rounded-2xl shadow-xl w-full max-w-md">
        <h2 className="text-2xl font-bold mb-4 text-center text-cyan-400 font-black">
          Verificar Cuenta
        </h2>

        <p className="text-sm text-slate-300 text-center mb-6">
          Ingresa el código de 6 dígitos que enviamos a tu correo.
        </p>

        {alerta.msg && (
          <div
            className={`p-3 mb-4 text-center rounded-lg text-sm font-semibold ${
              alerta.error ? "bg-red-500/90 text-white" : "bg-teal-500/90 text-white"
            }`}
          >
            {alerta.msg}
          </div>
        )}

        {!cuentaConfirmada ? (
          <div>
            <form onSubmit={handleSubmit}>
              <div className="mb-4">
                <label className="block text-slate-300 text-sm font-bold mb-2">
                  Código de 6 dígitos
                </label>
                <input
                  type="text"
                  maxLength={6}
                  className="w-full px-3 py-3 border border-white/20 bg-black/40 rounded-lg text-center text-3xl font-mono tracking-[0.25em] text-cyan-300 focus:outline-none focus:ring-2 focus:ring-cyan-500"
                  placeholder="123456"
                  value={token}
                  onChange={(e) => {
                    setToken(e.target.value);
                    if (alerta.msg) setAlerta({ error: false, msg: "" });
                  }}
                />
              </div>

              <button
                type="submit"
                className="w-full bg-gradient-to-r from-cyan-500 to-blue-500 shadow-[0_0_15px_rgba(6,182,212,0.5)] text-white font-bold py-3 px-4 rounded-lg hover:from-cyan-400 hover:to-blue-400 transition-all text-base mb-4"
              >
                Verificar y Activar Cuenta
              </button>
            </form>

            {/* Aviso de Spam y Reenvío */}
            <div className="mt-6 pt-4 border-t border-white/10 text-center space-y-3">
              <p className="text-xs text-amber-300/90 bg-amber-500/10 border border-amber-500/20 p-2.5 rounded-lg text-left">
                💡 <strong>Importante:</strong> Si no lo ves en tu bandeja de entrada principal de Gmail, revisa tu carpeta de <strong>Spam / Correo no deseado</strong> o <strong>Promociones</strong>.
              </p>

              {mostrarReenviar && (
                <div className="mt-2 text-left">
                  <label className="block text-xs text-slate-400 mb-1">
                    Tu correo electrónico:
                  </label>
                  <input
                    type="email"
                    className="w-full px-3 py-2 text-sm bg-black/40 border border-white/20 rounded-lg text-white mb-2"
                    placeholder="ejemplo@gmail.com"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                  />
                </div>
              )}

              <button
                type="button"
                onClick={() => {
                  if (!mostrarReenviar && !email) {
                    setMostrarReenviar(true);
                  } else {
                    handleReenviarCodigo();
                  }
                }}
                disabled={reenviando}
                className="text-xs text-cyan-400 hover:text-cyan-300 hover:underline font-medium transition-colors"
              >
                {reenviando ? "Reenviando..." : "¿No recibiste el código? Reenviar código"}
              </button>
            </div>
          </div>
        ) : (
          <div className="text-center mt-5">
            <Link
              to="/"
              className="inline-block bg-gradient-to-r from-cyan-500 to-blue-500 shadow-[0_0_15px_rgba(6,182,212,0.5)] text-white font-bold py-3 px-6 rounded-lg hover:from-cyan-400 hover:to-blue-400 transition-colors"
            >
              Iniciar Sesión
            </Link>
          </div>
        )}
      </div>
    </div>
  );
};

export default ConfirmarCuenta;
