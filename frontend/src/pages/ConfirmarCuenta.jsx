import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import clienteAxios from "../config/axios";

const ConfirmarCuenta = () => {
  const [token, setToken] = useState("");
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

  return (
    <div className="min-h-screen flex items-center justify-center bg-[#050510] text-white bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-[#0f172a] via-[#050510] to-black">
      <div className="bg-white/5 backdrop-blur-xl border border-white/10 p-8 rounded-lg shadow-md w-96">
        <h2 className="text-2xl font-bold mb-6 text-center text-cyan-400 font-black">
          Verificar Cuenta
        </h2>

        {alerta.msg && (
          <div
            className={`p-3 mb-4 text-center rounded text-white font-bold ${
              alerta.error ? "bg-red-500" : "bg-teal-500"
            }`}
          >
            {alerta.msg}
          </div>
        )}

        {!cuentaConfirmada ? (
          <form onSubmit={handleSubmit}>
            <div className="mb-4">
              <label className="block text-slate-300 text-sm font-bold mb-2">
                Código de 6 dígitos
              </label>
              <input
                type="text"
                maxLength={6}
                className="w-full px-3 py-2 border rounded-lg text-center text-2xl tracking-widest focus:outline-none focus:ring-2 focus:ring-teal-500"
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
              className="w-full bg-gradient-to-r from-cyan-500 to-blue-500 shadow-[0_0_15px_rgba(6,182,212,0.5)] text-white font-bold py-2 px-4 rounded-lg hover:bg-teal-700 transition-colors"
            >
              Verificar
            </button>
          </form>
        ) : (
          <div className="text-center mt-5">
            <Link
              to="/"
              className="inline-block bg-gradient-to-r from-cyan-500 to-blue-500 shadow-[0_0_15px_rgba(6,182,212,0.5)] text-white font-bold py-2 px-4 rounded-lg hover:bg-teal-700 transition-colors"
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
