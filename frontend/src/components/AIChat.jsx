import { useState, useRef, useEffect } from "react";
import clienteAxios from "../config/axios";
import { Bot, Send, X, Loader } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";

const AIChat = () => {
  const [isOpen, setIsOpen] = useState(false);
  const [mensaje, setMensaje] = useState("");
  const [chat, setChat] = useState([
    {
      role: "ai",
      content:
        "¡Hola! Soy Neo, tu asistente financiero. Puedo analizar tu cuenta y responder preguntas sobre tus finanzas.",
    },
  ]);
  const [loading, setLoading] = useState(false);
  const scrollRef = useRef(null);

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [chat, loading]);

  const enviarMensaje = async (e) => {
    e.preventDefault();
    if (!mensaje.trim()) return;

    const query = mensaje;
    setChat((prev) => [...prev, { role: "user", content: query }]);
    setMensaje("");
    setLoading(true);

    try {
      const { data } = await clienteAxios.post("/ia", { mensaje: query });
      setChat((prev) => [...prev, { role: "ai", content: data.respuesta }]);
    } catch (error) {
      setChat((prev) => [
        ...prev,
        {
          role: "ai",
          content:
            "Lo siento, tuve un problema analizando tus datos en este momento.",
        },
      ]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed bottom-6 right-6 z-50">
      <AnimatePresence>
        {isOpen && (
          <motion.div
            initial={{ opacity: 0, y: 50, scale: 0.9 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 50, scale: 0.9 }}
            className="mb-4 w-[350px] bg-[#0f172a]/90  border border-white/10 rounded-2xl shadow-[0_10px_40px_rgba(0,0,0,0.5)] overflow-hidden flex flex-col"
          >
            <div className="bg-gradient-to-r from-cyan-600 to-blue-600 p-4 flex justify-between items-center text-white">
              <div className="flex items-center gap-2 font-bold">
                <Bot /> Asistente Neo
              </div>
              <button
                onClick={() => setIsOpen(false)}
                className="hover:bg-white/20 p-1 rounded-full transition-colors"
              >
                <X size={20} />
              </button>
            </div>

            <div ref={scrollRef} className="h-80 overflow-y-auto p-4 space-y-4">
              {chat.map((c, i) => (
                <div
                  key={i}
                  className={`flex ${c.role === "ai" ? "justify-start" : "justify-end"}`}
                >
                  <div
                    className={`p-3 rounded-2xl max-w-[85%] text-sm shadow-md ${c.role === "ai" ? "bg-slate-800 text-white rounded-tl-sm" : "bg-cyan-500 text-white rounded-tr-sm"}`}
                  >
                    {c.content}
                  </div>
                </div>
              ))}
              {loading && (
                <div className="flex justify-start">
                  <div className="bg-slate-800 text-cyan-400 p-3 rounded-2xl rounded-tl-sm flex items-center gap-2 shadow-md">
                    <Loader size={16} className="animate-spin" /> Pensando...
                  </div>
                </div>
              )}
            </div>

            <form
              onSubmit={enviarMensaje}
              className="p-3 bg-black/40 border-t border-white/5 flex gap-2"
            >
              <input
                type="text"
                value={mensaje}
                onChange={(e) => setMensaje(e.target.value)}
                placeholder="Pregunta sobre tus finanzas..."
                className="flex-1 bg-black/30 border border-white/10 rounded-full px-4 py-2 text-sm text-white focus:outline-none focus:border-cyan-400"
                disabled={loading}
              />
              <button
                type="submit"
                disabled={loading || !mensaje.trim()}
                className="bg-cyan-500 hover:bg-cyan-400 text-white p-2 rounded-full transition-colors disabled:opacity-50"
              >
                <Send size={18} />
              </button>
            </form>
          </motion.div>
        )}
      </AnimatePresence>

      {!isOpen && (
        <button
          onClick={() => setIsOpen(true)}
          className="bg-gradient-to-r from-cyan-500 to-blue-500 hover:from-cyan-400 hover:to-blue-400 text-white p-4 rounded-full shadow-[0_0_20px_rgba(6,182,212,0.5)] border border-cyan-400/30 transition-transform transform hover:scale-110"
        >
          <Bot size={32} />
        </button>
      )}
    </div>
  );
};

export default AIChat;
