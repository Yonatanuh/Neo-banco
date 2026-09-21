import React, { useState, useEffect, useContext } from "react";
import { useNavigate } from "react-router-dom";
import AuthContext from "../context/AuthProvider";
import clienteAxios from "../config/axios";

const GastosChart = React.lazy(() => import("../components/GastosChart"));

import {
  CreditCard,
  Home,
  Send,
  PiggyBank,
  History,
  LogOut,
  Download,
  Lock,
  Unlock,
  Plus,
  Wallet,
  ShieldCheck,
  Banknote,
  AlertTriangle,
  Bell,
  Package,
  Smartphone,
  Globe,
  LockKeyhole,
  Users,
  Gift,
  PieChart as PieChartIcon,
  MessageSquare,
  Star,
  Building,
  HeadphonesIcon,
  Menu,
  X,
  Loader2,
} from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";

const COLORS = [
  "#0d9488",
  "#3b82f6",
  "#f59e0b",
  "#ef4444",
  "#8b5cf6",
  "#10b981",
];

import { io } from "socket.io-client";
import { QRCodeSVG } from "qrcode.react";
import QRScanner from "../components/QRScanner";
import AIChat from "../components/AIChat";

const Dashboard = () => {
  const { auth, setAuth, cerrarSesion, cargando } = useContext(AuthContext);
  const navigate = useNavigate();

  const [opcionActiva, setOpcionActiva] = useState("inicio");
  const [menuAbierto, setMenuAbierto] = useState(false);
  const [cargandoDatos, setCargandoDatos] = useState(true);

  const [tarjetas, setTarjetas] = useState([]);
  const [contactos, setContactos] = useState([]);
  const [metas, setMetas] = useState([]);
  const [historial, setHistorial] = useState([]);
  const [gastosPorCategoria, setGastosPorCategoria] = useState([]);
  const [prestamos, setPrestamos] = useState([]);
  const [portafolioCripto, setPortafolioCripto] = useState([]);
  const [logros, setLogros] = useState([]);
  const [plazosFijos, setPlazosFijos] = useState([]);
  const [cuentasCompartidas, setCuentasCompartidas] = useState([]);
  const [divisaMonto, setDivisaMonto] = useState("");
  const [divisaDe, setDivisaDe] = useState("USD");
  const [divisaA, setDivisaA] = useState("EUR");

  // Estados para formularios
  const [montoTransfer, setMontoTransfer] = useState("");
  const [montoMeta, setMontoMeta] = useState("");
  const [montoCajero, setMontoCajero] = useState("");
  const [montoPrestamo, setMontoPrestamo] = useState("");
  const [emailDestino, setEmailDestino] = useState("");
  const [nombreMeta, setNombreMeta] = useState("");
  const [mensaje, setMensaje] = useState({ tipo: "", texto: "" });
  const [montoInversion, setMontoInversion] = useState("");
  const [scanMode, setScanMode] = useState(false);
  const [suscripciones, setSuscripciones] = useState({
    detectadas: [],
    guardadas: [],
  });
  const [tarjetasFisicas, setTarjetasFisicas] = useState([]);
  const [direccionEnvio, setDireccionEnvio] = useState("");
  const [digitos4, setDigitos4] = useState("");

  const [recompensasInfo, setRecompensasInfo] = useState({
    cashbackTotal: 0,
    historial: [],
  });
  const [misSplits, setMisSplits] = useState([]);
  const [chatContactos, setChatContactos] = useState([]);
  const [chatMensajes, setChatMensajes] = useState([]);
  const [chatUsuarioActivo, setChatUsuarioActivo] = useState(null);
  const [nuevoMensaje, setNuevoMensaje] = useState("");
  const [splitForm, setSplitForm] = useState({
    descripcion: "",
    montoTotal: "",
    participantes: "",
  });

  // Eliminar Cuenta
  const [pasoEliminar, setPasoEliminar] = useState(0); // 0: Inicial, 1: Esperando Token
  const [tokenEliminar, setTokenEliminar] = useState("");

  // Notificación WebSocket
  const [notificacionWS, setNotificacionWS] = useState(null);

  // Criptomonedas (CoinGecko)
  const [criptos, setCriptos] = useState([]);

  // Phase 13
  const [tickets, setTickets] = useState([]);
  const [swifts, setSwifts] = useState([]);

  useEffect(() => {
    if (!cargando && !auth._id) {
      navigate("/");
    }

    // Conectar WebSocket si hay un usuario logueado
    if (auth?._id) {
      const socket = io(
        import.meta.env.VITE_BACKEND_URL || "http://localhost:5000",
      );
      socket.emit("unirse", auth._id);

      socket.on("nueva_transferencia", (data) => {
        setNotificacionWS(
          `¡${data.remitente} te ha enviado $${data.monto.toFixed(2)}!`,
        );
        // Actualizar datos base para reflejar nuevo saldo
        setTimeout(() => {
          setNotificacionWS(null);
        }, 6000);
      });

      return () => socket.disconnect();
    }
  }, [auth?._id, cargando]);

  useEffect(() => {
    if (auth._id) {
      cargarDatosBase();
      obtenerCriptos();
    }
  }, [auth._id]);

  async function obtenerCriptos() {
    try {
      // CoinGecko API gratuita
      const url =
        "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin,ethereum,solana&vs_currencies=usd&include_24hr_change=true";
      const res = await fetch(url);
      const data = await res.json();

      setCriptos([
        {
          id: "bitcoin",
          name: "Bitcoin",
          symbol: "BTC",
          price: data.bitcoin.usd,
          change: data.bitcoin.usd_24h_change,
        },
        {
          id: "ethereum",
          name: "Ethereum",
          symbol: "ETH",
          price: data.ethereum.usd,
          change: data.ethereum.usd_24h_change,
        },
        {
          id: "solana",
          name: "Solana",
          symbol: "SOL",
          price: data.solana.usd,
          change: data.solana.usd_24h_change,
        },
      ]);
    } catch (error) {
      console.log("Error al cargar criptos", error);
    }
  }
  async function cargarDatosBase() {
    try {
      // 🚀 OPTIMIZACIÓN EXTREMA: En lugar de 16 peticiones HTTP,
      // ahora hacemos 1 sola al endpoint de resumen en Java.
      // ⏱️ Delay artificial de 800ms para evitar el "parpadeo" visual del spinner de carga
      const [response] = await Promise.all([
        clienteAxios("/dashboard/summary"),
        new Promise((resolve) => setTimeout(resolve, 800)),
      ]);
      const data = response.data;

      setTarjetas(data.tarjetas || []);
      setContactos(data.contactos || []);
      setMetas(data.metas || []);
      setHistorial(data.historial || []);
      setPrestamos(data.prestamos || []);
      setPortafolioCripto(data.inversiones || []);
      setLogros(data.logros || []);
      setSuscripciones(data.suscripciones || { detectadas: [], guardadas: [] });
      setTarjetasFisicas(data.tarjetasFisicaEstado || []);
      setPlazosFijos(data.plazosFijos || []);
      setCuentasCompartidas(data.cuentasCompartidas || []);
      setRecompensasInfo(
        data.recompensas || { cashbackTotal: 0, historial: [] },
      );
      setMisSplits(data.splits || []);
      setChatContactos(data.contactosChat || []);
      setTickets(data.tickets || []);
      setSwifts(data.swifts || []);

      procesarGrafico(data.historial || []);
    } catch (error) {
      console.error("Error cargando dashboard:", error);
    } finally {
      setCargandoDatos(false);
    }
  }

  const procesarGrafico = (transacciones) => {
    // Filtrar solo los egresos para el gráfico
    const egresos = transacciones.filter(
      (tx) =>
        tx.tipo === "Retiro" ||
        (tx.tipo === "Transferencia" && tx.remitente === auth._id),
    );

    const categorias = {};
    egresos.forEach((tx) => {
      categorias[tx.categoria] = (categorias[tx.categoria] || 0) + tx.monto;
    });

    const data = Object.keys(categorias).map((cat) => ({
      name: cat,
      value: categorias[cat],
    }));

    setGastosPorCategoria(data);
  };

  const handleToggleCongelar = async (id) => {
    try {
      const { data } = await clienteAxios.post(`/tarjetas/${id}/congelar`);
      setMensaje({ tipo: "exito", texto: data.mensaje });
      cargarDatosBase();
    } catch (error) {
      setMensaje({ tipo: "error", texto: "Error al congelar/descongelar" });
    }
  };

  const generarTarjeta = async () => {
    try {
      const { data } = await clienteAxios.post("/tarjetas/desechable");
      setMensaje({ tipo: "exito", texto: "Tarjeta desechable creada" });
      cargarDatosBase();
    } catch (error) {
      setMensaje({ tipo: "error", texto: "Error al crear tarjeta" });
    }
  };

  const eliminarTarjeta = async (id) => {
    if (!window.confirm("¿Seguro que deseas eliminar esta tarjeta desechable?"))
      return;
    try {
      const { data } = await clienteAxios.delete(`/tarjetas/${id}`);
      setMensaje({ tipo: "exito", texto: data.mensaje });
      cargarDatosBase();
    } catch (error) {
      setMensaje({
        tipo: "error",
        texto: error.response?.data?.mensaje || "Error al eliminar",
      });
    }
  };

  const procesarTransferencia = async (e) => {
    e.preventDefault();
    setMensaje({ tipo: "", texto: "" });
    if (!montoTransfer || montoTransfer <= 0 || !emailDestino) {
      return setMensaje({
        tipo: "error",
        texto: "Ingresa un monto y destino válidos",
      });
    }

    try {
      const { data } = await clienteAxios.post("/usuarios/transferir", {
        monto: montoTransfer,
        emailDestino,
      });
      setMensaje({ tipo: "exito", texto: data.mensaje });
      setAuth({ ...auth, saldo: data.saldo });
      setMontoTransfer("");
      setEmailDestino("");
      cargarDatosBase(); // Refrescar historial
    } catch (error) {
      setMensaje({
        tipo: "error",
        texto: error.response?.data?.mensaje || "Error en transferencia",
      });
    }
  };

  const crearMeta = async (e) => {
    e.preventDefault();
    try {
      await clienteAxios.post("/metas", {
        nombre: nombreMeta,
        monto_objetivo: montoMeta,
      });
      setNombreMeta("");
      setMontoMeta("");
      cargarDatosBase();
      setMensaje({ tipo: "exito", texto: "Meta creada correctamente" });
    } catch (error) {
      setMensaje({
        tipo: "error",
        texto: error.response?.data?.mensaje || "Error al crear meta",
      });
    }
  };

  const depositarFondos = async (e) => {
    e.preventDefault();
    if (!montoCajero || montoCajero <= 0)
      return setMensaje({ tipo: "error", texto: "Monto inválido" });
    try {
      const { data } = await clienteAxios.post("/usuarios/depositar", {
        monto: montoCajero,
      });
      setMensaje({ tipo: "exito", texto: data.mensaje });
      setAuth({ ...auth, saldo: data.saldo });
      setMontoCajero("");
      cargarDatosBase();
    } catch (error) {
      setMensaje({
        tipo: "error",
        texto: error.response?.data?.mensaje || "Error al depositar",
      });
    }
  };

  const convertirDivisa = async (e) => {
    e.preventDefault();
    try {
      const { data } = await clienteAxios.post("/divisas/convertir", {
        de: divisaDe,
        a: divisaA,
        monto: Number(divisaMonto),
      });
      setMensaje({ tipo: "exito", texto: data.mensaje });
      setDivisaMonto("");
      cargarDatosBase();
    } catch (error) {
      setMensaje({
        tipo: "error",
        texto: error.response?.data?.mensaje || "Error",
      });
    }
  };

  const toggleRedondeo = async (activo) => {
    try {
      const metaId = metas.length > 0 ? metas[0]._id : null;
      const { data } = await clienteAxios.patch("/usuarios/redondeo", {
        activo,
        metaId,
      });
      setMensaje({ tipo: "exito", texto: data.mensaje });
      setAuth({ ...auth, redondeoActivo: activo });
      cargarDatosBase();
    } catch (error) {
      setMensaje({
        tipo: "error",
        texto: error.response?.data?.mensaje || "Error",
      });
    }
  };

  const crearSplitBill = async (e) => {
    e.preventDefault();
    try {
      const { data } = await clienteAxios.post("/split", {
        descripcion: splitForm.descripcion,
        montoTotal: Number(splitForm.montoTotal),
        participantes: splitForm.participantes.split(",").map((e) => e.trim()),
      });
      setMensaje({ tipo: "exito", texto: data.mensaje });
      setSplitForm({ descripcion: "", montoTotal: "", participantes: "" });
      cargarDatosBase();
    } catch (error) {
      setMensaje({
        tipo: "error",
        texto: error.response?.data?.mensaje || "Error",
      });
    }
  };

  const pagarSplit = async (id) => {
    try {
      const { data } = await clienteAxios.post(`/split/${id}/pagar`);
      setMensaje({ tipo: "exito", texto: data.mensaje });
      cargarDatosBase();
    } catch (error) {
      setMensaje({
        tipo: "error",
        texto: error.response?.data?.mensaje || "Error",
      });
    }
  };

  const cargarMensajes = async (usuarioId) => {
    try {
      const { data } = await clienteAxios.get(`/mensajes/${usuarioId}`);
      setChatMensajes(data);
      setChatUsuarioActivo(usuarioId);
    } catch (error) {
      console.error(error);
    }
  };

  const enviarMensajeP2P = async (e) => {
    e.preventDefault();
    if (!nuevoMensaje.trim() || !chatUsuarioActivo) return;
    try {
      await clienteAxios.post("/mensajes", {
        destinatarioId: chatUsuarioActivo,
        texto: nuevoMensaje,
      });
      setNuevoMensaje("");
      cargarMensajes(chatUsuarioActivo);
    } catch (error) {
      console.error(error);
    }
  };

  const procesarNomina = async (e) => {
    e.preventDefault();
    const rawData = e.target.nominaData.value;
    try {
      const pagos = JSON.parse(rawData);
      const { data } = await clienteAxios.post("/business/nomina", { pagos });
      setMensaje({ tipo: "exito", texto: data.mensaje });
      e.target.reset();
      cargarDatosBase();
    } catch (error) {
      setMensaje({
        tipo: "error",
        texto:
          error.response?.data?.mensaje ||
          'Error. Usa formato JSON: [{"email":"...", "monto":100}]',
      });
    }
  };

  const solicitarSwift = async (e) => {
    e.preventDefault();
    const formData = new FormData(e.target);
    try {
      const { data } = await clienteAxios.post(
        "/swift",
        Object.fromEntries(formData),
      );
      setMensaje({ tipo: "exito", texto: data.mensaje });
      e.target.reset();
      cargarDatosBase();
    } catch (error) {
      setMensaje({
        tipo: "error",
        texto: error.response?.data?.mensaje || "Error",
      });
    }
  };

  const crearTicket = async (e) => {
    e.preventDefault();
    const formData = new FormData(e.target);
    try {
      const { data } = await clienteAxios.post(
        "/soporte",
        Object.fromEntries(formData),
      );
      setMensaje({ tipo: "exito", texto: data.mensaje });
      e.target.reset();
      cargarDatosBase();
    } catch (error) {
      setMensaje({
        tipo: "error",
        texto: error.response?.data?.mensaje || "Error",
      });
    }
  };

  const procesarInversion = async (criptomoneda, simbolo, tipo, monto) => {
    setMensaje({ tipo: "", texto: "" });
    if (!monto || monto <= 0) {
      return setMensaje({ tipo: "error", texto: "Ingresa un monto válido" });
    }

    try {
      if (tipo === "comprar") {
        const { data } = await clienteAxios.post("/inversiones/comprar", {
          criptomoneda,
          simbolo,
          monto_usd: Number(monto),
        });
        setMensaje({ tipo: "exito", texto: data.mensaje });
        setAuth({ ...auth, saldo: data.saldo });
      } else {
        const { data } = await clienteAxios.post("/inversiones/vender", {
          criptomoneda,
          cantidad_cripto: Number(monto),
        });
        setMensaje({ tipo: "exito", texto: data.mensaje });
        setAuth({ ...auth, saldo: data.saldo });
      }
      cargarDatosBase();
    } catch (error) {
      setMensaje({
        tipo: "error",
        texto: error.response?.data?.mensaje || "Error procesando inversión",
      });
    }
  };
  const retirarFondos = async (e) => {
    e.preventDefault();
    if (!montoCajero || montoCajero <= 0)
      return setMensaje({ tipo: "error", texto: "Monto inválido" });
    try {
      const { data } = await clienteAxios.post("/usuarios/retirar", {
        monto: montoCajero,
      });
      setMensaje({ tipo: "exito", texto: data.mensaje });
      setAuth({ ...auth, saldo: data.saldo });
      setMontoCajero("");
      cargarDatosBase();
    } catch (error) {
      setMensaje({
        tipo: "error",
        texto: error.response?.data?.mensaje || "Error al retirar",
      });
    }
  };

  const crearBurnerCard = async () => {
    try {
      const { data } = await clienteAxios.post("/tarjetas/burner");
      setMensaje({ tipo: "exito", texto: data.mensaje });
      cargarDatosBase();
    } catch (error) {
      setMensaje({
        tipo: "error",
        texto: error.response?.data?.mensaje || "Error",
      });
    }
  };

  const solicitarTarjetaFisica = async (e) => {
    e.preventDefault();
    if (!direccionEnvio)
      return setMensaje({ tipo: "error", texto: "Ingresa tu dirección" });
    try {
      const { data } = await clienteAxios.post("/tarjetas/fisica/solicitar", {
        direccionEnvio,
      });
      setMensaje({ tipo: "exito", texto: data.mensaje });
      setDireccionEnvio("");
      cargarDatosBase();
    } catch (error) {
      setMensaje({
        tipo: "error",
        texto: error.response?.data?.mensaje || "Error",
      });
    }
  };

  const activarTarjetaFisica = async (e) => {
    e.preventDefault();
    try {
      const { data } = await clienteAxios.post("/tarjetas/fisica/activar", {
        ultimos4Digitos: digitos4,
      });
      setMensaje({ tipo: "exito", texto: data.mensaje });
      setDigitos4("");
      cargarDatosBase();
    } catch (error) {
      setMensaje({
        tipo: "error",
        texto: error.response?.data?.mensaje || "Error",
      });
    }
  };

  const tokenizarTarjeta = async (tarjetaId, proveedor) => {
    try {
      const { data } = await clienteAxios.post(
        `/tarjetas/${tarjetaId}/tokenizar`,
        { proveedor },
      );
      setMensaje({ tipo: "exito", texto: data.mensaje });
      cargarDatosBase();
    } catch (error) {
      setMensaje({
        tipo: "error",
        texto: error.response?.data?.mensaje || "Error",
      });
    }
  };

  const revocarToken = async (tarjetaId) => {
    try {
      const { data } = await clienteAxios.delete(
        `/tarjetas/${tarjetaId}/tokenizar`,
      );
      setMensaje({ tipo: "exito", texto: data.mensaje });
      cargarDatosBase();
    } catch (error) {
      setMensaje({
        tipo: "error",
        texto: error.response?.data?.mensaje || "Error",
      });
    }
  };

  const toggleBloquearSuscripcion = async (id) => {
    try {
      const { data } = await clienteAxios.patch(
        `/suscripciones/${id}/bloquear`,
      );
      setMensaje({ tipo: "exito", texto: data.mensaje });
      cargarDatosBase();
    } catch (error) {
      setMensaje({
        tipo: "error",
        texto: error.response?.data?.mensaje || "Error",
      });
    }
  };

  const solicitarPrestamo = async (e) => {
    e.preventDefault();
    if (!montoPrestamo || montoPrestamo <= 0)
      return setMensaje({ tipo: "error", texto: "Monto inválido" });
    try {
      const { data } = await clienteAxios.post("/prestamos", {
        monto: montoPrestamo,
      });
      setMensaje({ tipo: "exito", texto: data.mensaje });
      setAuth({ ...auth, saldo: data.saldo });
      setMontoPrestamo("");
      cargarDatosBase();
    } catch (error) {
      setMensaje({
        tipo: "error",
        texto: error.response?.data?.mensaje || "Error al solicitar préstamo",
      });
    }
  };

  const descargarPDFBlob = async () => {
    try {
      const response = await clienteAxios.get("/reportes/estado-cuenta", {
        responseType: "blob",
      });
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute(
        "download",
        `EstadoCuenta_${auth.nombre.replace(/\s/g, "_")}.pdf`,
      );
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (err) {
      alert("Error descargando PDF");
    }
  };

  // KYC y 2FA
  const [archivoKYC, setArchivoKYC] = useState(null);
  const [qrCodeUrl, setQrCodeUrl] = useState("");
  const [token2FA, setToken2FA] = useState("");

  const subirKYC = async (e) => {
    e.preventDefault();
    if (!archivoKYC)
      return setMensaje({ tipo: "error", texto: "Selecciona una imagen" });
    const formData = new FormData();
    formData.append("documento", archivoKYC);
    try {
      const { data } = await clienteAxios.post(
        "/seguridad/kyc/upload",
        formData,
        {
          headers: { "Content-Type": "multipart/form-data" },
        },
      );
      setMensaje({ tipo: "exito", texto: data.mensaje });
      setAuth({ ...auth, estadoKYC: data.estadoKYC });
    } catch (error) {
      setMensaje({
        tipo: "error",
        texto: error.response?.data?.mensaje || "Error al subir KYC",
      });
    }
  };

  const generar2FA = async () => {
    try {
      const { data } = await clienteAxios.post("/seguridad/2fa/generar");
      setQrCodeUrl(data.qrCodeUrl);
      setMensaje({
        tipo: "exito",
        texto: "QR Generado, escanealo con Google Authenticator",
      });
    } catch (error) {
      setMensaje({ tipo: "error", texto: "Error al generar QR" });
    }
  };

  const activar2FA = async (e) => {
    e.preventDefault();
    try {
      const { data } = await clienteAxios.post("/seguridad/2fa/activar", {
        token: token2FA,
      });
      setMensaje({ tipo: "exito", texto: data.mensaje });
      setAuth({ ...auth, dosFA_activo: true });
      setQrCodeUrl("");
      setToken2FA("");
    } catch (error) {
      setMensaje({
        tipo: "error",
        texto: error.response?.data?.mensaje || "Error al activar 2FA",
      });
    }
  };

  const solicitarEliminacion = async () => {
    if (
      !window.confirm(
        "¿Estás seguro de que quieres solicitar la eliminación de tu cuenta? Se enviará un código a tu correo.",
      )
    )
      return;
    try {
      const { data } = await clienteAxios.post("/usuarios/solicitar-eliminar");
      setMensaje({ tipo: "exito", texto: data.mensaje });
      setPasoEliminar(1);
    } catch (error) {
      setMensaje({
        tipo: "error",
        texto:
          error.response?.data?.mensaje || "Error al solicitar eliminación",
      });
    }
  };

  const confirmarEliminacion = async (e) => {
    e.preventDefault();
    try {
      const { data } = await clienteAxios.delete("/usuarios/eliminar", {
        data: { token: tokenEliminar },
      });
      setMensaje({ tipo: "exito", texto: data.mensaje });
      setTimeout(() => {
        cerrarSesion();
      }, 3000);
    } catch (error) {
      setMensaje({
        tipo: "error",
        texto: error.response?.data?.mensaje || "Código incorrecto",
      });
    }
  };

  const suscribirsePro = async () => {
    try {
      const { data } = await clienteAxios.post("/usuarios/suscripcion-pro");
      setMensaje({ tipo: "exito", texto: data.mensaje });
      setAuth({ ...auth, esPremium: data.esPremium, saldo: data.saldo });
    } catch (error) {
      setMensaje({
        tipo: "error",
        texto: error.response?.data?.mensaje || "Error al suscribirse",
      });
    }
  };

  if (cargando) {
    return (
      <div className="min-h-screen bg-[#050510] flex items-center justify-center">
        <Loader2 className="w-16 h-16 text-cyan-400 animate-spin" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#050510] bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-indigo-900/20 via-[#050510] to-black text-white font-sans flex relative overflow-hidden">
      {/* Decorative Glow Elements */}

      {/* Toast Notificación WebSockets */}
      <AnimatePresence>
        {notificacionWS && (
          <motion.div
            initial={{ opacity: 0, y: -50, scale: 0.8 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -50, scale: 0.8 }}
            className="absolute top-10 right-10 bg-gradient-to-r from-teal-600 to-emerald-500 text-white px-6 py-4 rounded-xl shadow-[0_0_30px_rgba(20,184,166,0.4)] z-50 flex items-center gap-3 font-bold border border-teal-400/30"
          >
            <Wallet /> {notificacionWS}
          </motion.div>
        )}
      </AnimatePresence>

      {/* Sidebar Lateral */}

      {/* Overlay para móvil */}
      {menuAbierto && (
        <div
          className="fixed inset-0 bg-black/60  z-40 md:hidden"
          onClick={() => setMenuAbierto(false)}
        />
      )}
      <aside
        className={`w-64 bg-[#0a0a1a]/95 md:bg-[#0a0a1a]/60  border-r border-white/10 flex flex-col fixed h-full z-50 md:z-10 shadow-[4px_0_24px_rgba(0,0,0,0.5)] transition-transform duration-300 ease-in-out ${menuAbierto ? "translate-x-0" : "-translate-x-full md:translate-x-0"}`}
      >
        <button
          className="md:hidden absolute top-4 right-4 text-slate-400 hover:text-white"
          onClick={() => setMenuAbierto(false)}
        >
          <X size={24} />
        </button>

        <div className="p-6 text-center border-b border-white/10">
          <h2 className="text-3xl font-black bg-clip-text text-transparent bg-gradient-to-r from-cyan-400 via-fuchsia-400 to-indigo-400 uppercase tracking-widest drop-shadow-[0_0_10px_rgba(34,211,238,0.5)]">
            Neo Banco
          </h2>
          <p className="text-cyan-200/60 text-sm mt-2 font-medium tracking-wide">
            El banco del futuro
          </p>
        </div>

        <nav className="flex-1 p-4 space-y-2 mt-4 overflow-y-auto custom-scrollbar">
          <button
            onClick={() => {
              setOpcionActiva("inicio");
              setMensaje({ tipo: "", texto: "" });
            }}
            className={`flex items-center gap-3 w-full text-left px-4 py-3 rounded-xl transition-all duration-300 ${opcionActiva === "inicio" ? "bg-gradient-to-r from-cyan-500 to-blue-500 text-white shadow-[0_0_15px_rgba(6,182,212,0.5)]" : "text-slate-400 hover:bg-white/5 hover:text-white"}`}
          >
            <Home size={20} /> Inicio
          </button>
          <button
            onClick={() => {
              setOpcionActiva("transferir");
              setMensaje({ tipo: "", texto: "" });
            }}
            className={`flex items-center gap-3 w-full text-left px-4 py-3 rounded-xl transition-all duration-300 ${opcionActiva === "transferir" ? "bg-gradient-to-r from-cyan-500 to-blue-500 text-white shadow-[0_0_15px_rgba(6,182,212,0.5)]" : "text-slate-400 hover:bg-white/5 hover:text-white"}`}
          >
            <Send size={20} /> Transferencias
          </button>
          <button
            onClick={() => {
              setOpcionActiva("prestamos");
              setMensaje({ tipo: "", texto: "" });
            }}
            className={`flex items-center gap-3 w-full text-left px-4 py-3 rounded-xl transition-all duration-300 ${opcionActiva === "prestamos" ? "bg-gradient-to-r from-cyan-500 to-blue-500 text-white shadow-[0_0_15px_rgba(6,182,212,0.5)]" : "text-slate-400 hover:bg-white/5 hover:text-white"}`}
          >
            <Banknote size={20} /> Préstamos
          </button>
          <button
            onClick={() => {
              setOpcionActiva("cajero");
              setMensaje({ tipo: "", texto: "" });
            }}
            className={`flex items-center gap-3 w-full text-left px-4 py-3 rounded-xl transition-all duration-300 ${opcionActiva === "cajero" ? "bg-gradient-to-r from-cyan-500 to-blue-500 text-white shadow-[0_0_15px_rgba(6,182,212,0.5)]" : "text-slate-400 hover:bg-white/5 hover:text-white"}`}
          >
            <Wallet size={20} /> Cajero ATM
          </button>
          <button
            onClick={() => {
              setOpcionActiva("seguridad");
              setMensaje({ tipo: "", texto: "" });
            }}
            className={`flex items-center gap-3 w-full text-left px-4 py-3 rounded-xl transition-all duration-300 ${opcionActiva === "seguridad" ? "bg-gradient-to-r from-cyan-500 to-blue-500 text-white shadow-[0_0_15px_rgba(6,182,212,0.5)]" : "text-slate-400 hover:bg-white/5 hover:text-white"}`}
          >
            <Lock size={20} /> Seguridad y KYC
          </button>
          <button
            onClick={() => {
              setOpcionActiva("bolsillos");
              setMensaje({ tipo: "", texto: "" });
            }}
            className={`flex items-center gap-3 w-full text-left px-4 py-3 rounded-xl transition-all duration-300 ${opcionActiva === "bolsillos" ? "bg-gradient-to-r from-cyan-500 to-blue-500 text-white shadow-[0_0_15px_rgba(6,182,212,0.5)]" : "text-slate-400 hover:bg-white/5 hover:text-white"}`}
          >
            <PiggyBank size={20} /> Bolsillos
          </button>
          <button
            onClick={() => {
              setOpcionActiva("movimientos");
              setMensaje({ tipo: "", texto: "" });
            }}
            className={`flex items-center gap-3 w-full text-left px-4 py-3 rounded-xl transition-all duration-300 ${opcionActiva === "movimientos" ? "bg-gradient-to-r from-cyan-500 to-blue-500 text-white shadow-[0_0_15px_rgba(6,182,212,0.5)]" : "text-slate-400 hover:bg-white/5 hover:text-white"}`}
          >
            <History size={20} /> Historial
          </button>

          <button
            onClick={() => {
              setOpcionActiva("suscripciones");
              setMensaje({ tipo: "", texto: "" });
            }}
            className={`flex items-center gap-3 w-full text-left px-4 py-3 rounded-xl transition-all duration-300 ${opcionActiva === "suscripciones" ? "bg-gradient-to-r from-cyan-500 to-blue-500 text-white shadow-[0_0_15px_rgba(6,182,212,0.5)]" : "text-slate-400 hover:bg-white/5 hover:text-white"}`}
          >
            <Bell size={20} /> Suscripciones
          </button>
          <button
            onClick={() => {
              setOpcionActiva("tarjeta-fisica");
              setMensaje({ tipo: "", texto: "" });
            }}
            className={`flex items-center gap-3 w-full text-left px-4 py-3 rounded-xl transition-all duration-300 ${opcionActiva === "tarjeta-fisica" ? "bg-gradient-to-r from-cyan-500 to-blue-500 text-white shadow-[0_0_15px_rgba(6,182,212,0.5)]" : "text-slate-400 hover:bg-white/5 hover:text-white"}`}
          >
            <Package size={20} /> Tarjeta Física
          </button>
          <button
            onClick={() => {
              setOpcionActiva("wallet-movil");
              setMensaje({ tipo: "", texto: "" });
            }}
            className={`flex items-center gap-3 w-full text-left px-4 py-3 rounded-xl transition-all duration-300 ${opcionActiva === "wallet-movil" ? "bg-gradient-to-r from-cyan-500 to-blue-500 text-white shadow-[0_0_15px_rgba(6,182,212,0.5)]" : "text-slate-400 hover:bg-white/5 hover:text-white"}`}
          >
            <Smartphone size={20} /> Wallet Móvil
          </button>

          <button
            onClick={() => {
              setOpcionActiva("divisas");
              setMensaje({ tipo: "", texto: "" });
            }}
            className={`flex items-center gap-3 w-full text-left px-4 py-3 rounded-xl transition-all duration-300 ${opcionActiva === "divisas" ? "bg-gradient-to-r from-cyan-500 to-blue-500 text-white shadow-[0_0_15px_rgba(6,182,212,0.5)]" : "text-slate-400 hover:bg-white/5 hover:text-white"}`}
          >
            <Globe size={20} /> Divisas
          </button>
          <button
            onClick={() => {
              setOpcionActiva("plazo-fijo");
              setMensaje({ tipo: "", texto: "" });
            }}
            className={`flex items-center gap-3 w-full text-left px-4 py-3 rounded-xl transition-all duration-300 ${opcionActiva === "plazo-fijo" ? "bg-gradient-to-r from-cyan-500 to-blue-500 text-white shadow-[0_0_15px_rgba(6,182,212,0.5)]" : "text-slate-400 hover:bg-white/5 hover:text-white"}`}
          >
            <LockKeyhole size={20} /> Plazo Fijo
          </button>
          <button
            onClick={() => {
              setOpcionActiva("cuentas-compartidas");
              setMensaje({ tipo: "", texto: "" });
            }}
            className={`flex items-center gap-3 w-full text-left px-4 py-3 rounded-xl transition-all duration-300 ${opcionActiva === "cuentas-compartidas" ? "bg-gradient-to-r from-cyan-500 to-blue-500 text-white shadow-[0_0_15px_rgba(6,182,212,0.5)]" : "text-slate-400 hover:bg-white/5 hover:text-white"}`}
          >
            <Users size={20} /> Cuentas Compartidas
          </button>

          <button
            onClick={() => {
              setOpcionActiva("recompensas");
              setMensaje({ tipo: "", texto: "" });
            }}
            className={`flex items-center gap-3 w-full text-left px-4 py-3 rounded-xl transition-all duration-300 ${opcionActiva === "recompensas" ? "bg-gradient-to-r from-cyan-500 to-blue-500 text-white shadow-[0_0_15px_rgba(6,182,212,0.5)]" : "text-slate-400 hover:bg-white/5 hover:text-white"}`}
          >
            <Gift size={20} /> Recompensas & Cashback
          </button>
          <button
            onClick={() => {
              setOpcionActiva("split-bill");
              setMensaje({ tipo: "", texto: "" });
            }}
            className={`flex items-center gap-3 w-full text-left px-4 py-3 rounded-xl transition-all duration-300 ${opcionActiva === "split-bill" ? "bg-gradient-to-r from-cyan-500 to-blue-500 text-white shadow-[0_0_15px_rgba(6,182,212,0.5)]" : "text-slate-400 hover:bg-white/5 hover:text-white"}`}
          >
            <PieChartIcon size={20} /> Dividir Cuenta
          </button>
          <button
            onClick={() => {
              setOpcionActiva("chat-p2p");
              setMensaje({ tipo: "", texto: "" });
            }}
            className={`flex items-center gap-3 w-full text-left px-4 py-3 rounded-xl transition-all duration-300 ${opcionActiva === "chat-p2p" ? "bg-gradient-to-r from-cyan-500 to-blue-500 text-white shadow-[0_0_15px_rgba(6,182,212,0.5)]" : "text-slate-400 hover:bg-white/5 hover:text-white"}`}
          >
            <MessageSquare size={20} /> Chat P2P
          </button>
          <button
            onClick={() => {
              setOpcionActiva("suscripcion-pro");
              setMensaje({ tipo: "", texto: "" });
            }}
            className={`flex items-center gap-3 w-full text-left px-4 py-3 rounded-xl transition-all duration-300 font-bold ${opcionActiva === "suscripcion-pro" ? "bg-gradient-to-r from-amber-400 to-orange-500 text-white shadow-[0_0_15px_rgba(251,191,36,0.5)] border border-amber-400/50" : "text-amber-400/70 hover:bg-white/5 hover:text-amber-400"}`}
          >
            <Star size={20} /> NeoBanco Pro
          </button>

          <button
            onClick={() => {
              setOpcionActiva("business");
              setMensaje({ tipo: "", texto: "" });
            }}
            className={`flex items-center gap-3 w-full text-left px-4 py-3 rounded-xl transition-all duration-300 ${opcionActiva === "business" ? "bg-gradient-to-r from-cyan-500 to-blue-500 text-white shadow-[0_0_15px_rgba(6,182,212,0.5)]" : "text-slate-400 hover:bg-white/5 hover:text-white"}`}
          >
            <Building size={20} /> B2B Nómina
          </button>
          <button
            onClick={() => {
              setOpcionActiva("swift");
              setMensaje({ tipo: "", texto: "" });
            }}
            className={`flex items-center gap-3 w-full text-left px-4 py-3 rounded-xl transition-all duration-300 ${opcionActiva === "swift" ? "bg-gradient-to-r from-cyan-500 to-blue-500 text-white shadow-[0_0_15px_rgba(6,182,212,0.5)]" : "text-slate-400 hover:bg-white/5 hover:text-white"}`}
          >
            <Globe size={20} /> Transferencias SWIFT
          </button>
          <button
            onClick={() => {
              setOpcionActiva("soporte");
              setMensaje({ tipo: "", texto: "" });
            }}
            className={`flex items-center gap-3 w-full text-left px-4 py-3 rounded-xl transition-all duration-300 ${opcionActiva === "soporte" ? "bg-gradient-to-r from-cyan-500 to-blue-500 text-white shadow-[0_0_15px_rgba(6,182,212,0.5)]" : "text-slate-400 hover:bg-white/5 hover:text-white"}`}
          >
            <HeadphonesIcon size={20} /> Soporte (Tickets)
          </button>

          <button
            onClick={() => {
              setOpcionActiva("logros");
              setMensaje({ tipo: "", texto: "" });
            }}
            className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl transition-all font-bold ${opcionActiva === "logros" ? "bg-cyan-500/20 text-cyan-400 border border-cyan-500/30 shadow-[0_0_15px_rgba(6,182,212,0.3)]" : "text-slate-400 hover:bg-white/5 hover:text-white"}`}
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="20"
              height="20"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <path d="M6 9H4.5a2.5 2.5 0 0 1 0-5H6" />
              <path d="M18 9h1.5a2.5 2.5 0 0 0 0-5H18" />
              <path d="M4 22h16" />
              <path d="M10 14.66V17c0 .55-.47.98-.97 1.21C7.85 18.75 7 20.24 7 22" />
              <path d="M14 14.66V17c0 .55.47.98.97 1.21C16.15 18.75 17 20.24 17 22" />
              <path d="M18 2H6v7a6 6 0 0 0 12 0V2Z" />
            </svg>{" "}
            Logros
          </button>
        </nav>

        <div className="p-4 border-t border-white/10 bg-black/20">
          <button
            onClick={cerrarSesion}
            className="flex items-center gap-3 w-full bg-rose-500/10 text-rose-400 hover:bg-rose-500 hover:text-white hover:shadow-[0_0_20px_rgba(244,63,94,0.6)] px-4 py-3 rounded-xl transition-all duration-300 font-bold"
          >
            <LogOut size={20} /> Cerrar Sesión
          </button>
        </div>
      </aside>

      {/* Contenido Principal */}
      <main className="flex-1 p-4 md:p-10 w-full md:ml-64 overflow-y-scroll h-screen z-10 relative custom-scrollbar">
        <header className="flex flex-col md:flex-row justify-between items-start md:items-end mb-10 border-b border-white/10/50 pb-6 bg-slate-900/40  p-6 rounded-2xl shadow-xl gap-4 md:gap-0">
          <div className="flex items-center gap-4 w-full md:w-auto">
            <button
              className="md:hidden p-2 bg-white/5 rounded-lg text-white"
              onClick={() => setMenuAbierto(true)}
            >
              <Menu size={24} />
            </button>
            <div>
              <p className="text-slate-400 text-lg font-medium">
                Hola de nuevo,
              </p>
              <h1 className="text-2xl md:text-4xl font-black capitalize flex items-center gap-3">
                <span className="bg-clip-text text-transparent bg-gradient-to-r from-white to-slate-400">
                  {auth.nombre}
                </span>
                {auth.esPremium && (
                  <span className="text-xs bg-gradient-to-r from-amber-400 to-yellow-600 text-black px-2 py-1 rounded-full font-black tracking-widest uppercase shadow-[0_0_15px_rgba(251,191,36,0.5)]">
                    PRO
                  </span>
                )}
              </h1>
              <div className="mt-2">
                <p className="text-cyan-400 text-sm font-bold">
                  Nivel {auth.nivel || 1}
                </p>
                <div className="w-48 h-1.5 bg-black/50 rounded-full mt-1 overflow-hidden border border-white/5">
                  <div
                    className="h-full bg-gradient-to-r from-cyan-500 to-blue-500 rounded-full"
                    style={{ width: `${(auth.experiencia || 0) % 100}%` }}
                  ></div>
                </div>
              </div>
            </div>
          </div>
          <div className="flex items-center gap-6">
            <div className="text-right">
              <p className="text-slate-400 font-medium">Saldo Disponible</p>
              <h2 className="text-5xl font-black bg-clip-text text-transparent bg-gradient-to-r from-teal-400 to-emerald-300 drop-shadow-md">
                {"$"}
                {Number(auth.saldo).toFixed(2)}
              </h2>
            </div>
            {/* Desktop Logout (if sidebar overflows on small screens) */}
            <button
              onClick={cerrarSesion}
              className="hidden lg:flex items-center gap-2 bg-rose-500/10 text-rose-400 hover:bg-rose-500 hover:text-white px-4 py-2 rounded-xl transition-colors font-medium border border-rose-500/20 h-fit"
            >
              <LogOut size={18} /> Cerrar Sesión
            </button>
          </div>
        </header>

        {mensaje.texto && (
          <div
            className={`p-4 rounded-lg mb-8 font-bold ${mensaje.tipo === "error" ? "bg-red-500/20 border-l-4 border-red-500 text-red-300" : "bg-cyan-900/300/20 border-l-4 border-cyan-400 text-cyan-300"}`}
          >
            {mensaje.texto}
          </div>
        )}

        {/* CONTENEDOR PARA LAS VISTAS */}
        <div className="pb-10 min-h-[500px]">
          {cargandoDatos ? (
            <div className="flex flex-col items-center justify-center py-40 gap-6">
              <Loader2 className="w-16 h-16 text-cyan-400 animate-spin" />
              <h2 className="text-2xl font-black bg-clip-text text-transparent bg-gradient-to-r from-cyan-400 to-indigo-400 animate-pulse">
                Sincronizando Bóveda...
              </h2>
            </div>
          ) : (
            <div>
              {/* VISTA: INICIO */}
              {opcionActiva === "inicio" && (
                <>
                  <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                    {/* Tarjetas */}
                    <div className="animate-in fade-in slide-in-from-bottom-4 duration-500 bg-white/5 border border-white/10 rounded-2xl p-8 shadow-[0_8px_30px_rgb(0,0,0,0.12)] relative overflow-hidden group hover:border-cyan-500/30 transition-colors duration-500">
                      <div className="flex justify-between items-center mb-6">
                        <h3 className="text-xl font-bold flex items-center gap-2 text-cyan-400">
                          <CreditCard /> Mis Tarjetas
                        </h3>
                        <div className="flex gap-2">
                          <button
                            onClick={crearBurnerCard}
                            className="text-xs bg-gradient-to-r from-orange-500 to-red-500 hover:from-orange-400 hover:to-red-400 px-3 py-1.5 rounded-lg font-bold text-white shadow-lg transition-colors flex items-center gap-1"
                          >
                            <Plus size={14} /> Burner Card
                          </button>
                          <button
                            onClick={generarTarjeta}
                            className="text-xs bg-fuchsia-600 hover:bg-fuchsia-500 px-3 py-1.5 rounded-lg font-bold text-white shadow-lg transition-colors flex items-center gap-1"
                          >
                            <Plus size={14} /> Nueva Desechable
                          </button>
                        </div>
                      </div>

                      <div className="space-y-6">
                        {tarjetas.length > 0 ? (
                          tarjetas.map((tarjeta) => (
                            <div key={tarjeta._id}>
                              <div
                                className={`relative w-full h-56 rounded-2xl p-6 flex flex-col justify-between shadow-2xl transition-all duration-500 ${tarjeta.isFrozen ? "bg-slate-800/80 opacity-60 grayscale" : tarjeta.tipo === "Desechable" ? "bg-gradient-to-tr from-rose-600 via-orange-600 to-amber-500 shadow-[0_0_30px_rgba(244,63,94,0.4)]" : "bg-gradient-to-tr from-indigo-600 via-purple-600 to-fuchsia-600 shadow-[0_0_30px_rgba(168,85,247,0.4)]"}`}
                              >
                                {tarjeta.isFrozen && (
                                  <div className="absolute inset-0 flex items-center justify-center bg-black/60 rounded-2xl z-10 ">
                                    <p className="text-rose-400 font-black text-2xl flex items-center gap-2 drop-shadow-md">
                                      <Lock /> CONGELADA
                                    </p>
                                  </div>
                                )}
                                <div className="flex justify-between items-center z-0">
                                  <span className="text-xl font-black tracking-widest text-teal-100 flex flex-col">
                                    NEO BANCO
                                    <span className="text-xs text-white/70 font-medium tracking-normal mt-1">
                                      {tarjeta.tipo.toUpperCase()}
                                    </span>
                                  </span>
                                  <svg
                                    className="w-12 h-12 text-cyan-200 opacity-50"
                                    fill="none"
                                    stroke="currentColor"
                                    viewBox="0 0 24 24"
                                  >
                                    <path
                                      strokeLinecap="round"
                                      strokeLinejoin="round"
                                      strokeWidth="2"
                                      d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                                    />
                                  </svg>
                                </div>

                                <div className="mt-4 z-0">
                                  <p className="text-3xl font-mono tracking-[0.25em] drop-shadow-sm">
                                    {tarjeta.numero_tarjeta
                                      .match(/.{1,4}/g)
                                      .join(" ")}
                                  </p>
                                </div>

                                <div className="flex justify-between mt-4 z-0">
                                  <div>
                                    <p className="text-xs text-indigo-200 font-medium uppercase tracking-wider">
                                      Titular
                                    </p>
                                    <p className="font-bold text-lg tracking-wide uppercase drop-shadow-sm text-white">
                                      {auth.nombre}
                                    </p>
                                  </div>
                                  <div className="text-right flex gap-6">
                                    <div>
                                      <p className="text-xs text-indigo-200 font-medium uppercase tracking-wider">
                                        Vence
                                      </p>
                                      <p className="font-bold text-lg drop-shadow-sm text-white">
                                        {tarjeta.fecha_expiracion}
                                      </p>
                                    </div>
                                    <div>
                                      <p className="text-xs text-indigo-200 font-medium uppercase tracking-wider">
                                        CVV
                                      </p>
                                      <p className="font-bold text-lg drop-shadow-sm text-white">
                                        {tarjeta.cvv}
                                      </p>
                                    </div>
                                  </div>
                                </div>
                              </div>

                              <div className="mt-3 flex justify-end gap-2">
                                {tarjeta.tipo === "Desechable" && (
                                  <button
                                    onClick={() => eliminarTarjeta(tarjeta._id)}
                                    className="text-xs px-4 py-2 rounded font-bold border border-rose-500/30 text-rose-400 hover:bg-rose-500 hover:text-white transition-colors flex items-center gap-1"
                                  >
                                    Eliminar
                                  </button>
                                )}
                                <button
                                  onClick={() =>
                                    handleToggleCongelar(tarjeta._id)
                                  }
                                  className={`text-xs flex items-center gap-1 px-4 py-2 rounded font-bold transition-colors ${tarjeta.isFrozen ? "bg-gradient-to-r from-cyan-500 to-blue-500 hover:from-cyan-400 hover:to-blue-400 shadow-[0_0_15px_rgba(6,182,212,0.5)] border border-cyan-400/30 text-white" : "bg-red-500/20 text-red-500 hover:bg-red-500 hover:text-white"}`}
                                >
                                  {tarjeta.isFrozen ? (
                                    <>
                                      <Unlock size={14} /> Descongelar
                                    </>
                                  ) : (
                                    <>
                                      <Lock size={14} /> Congelar
                                    </>
                                  )}
                                </button>
                              </div>
                            </div>
                          ))
                        ) : (
                          <div className="h-56 bg-slate-700 rounded-2xl flex items-center justify-center animate-pulse">
                            Cargando tarjetas...
                          </div>
                        )}
                      </div>
                    </div>

                    {/* Gráfica de Gastos */}
                    <div className="animate-in fade-in slide-in-from-bottom-4 duration-500 bg-white/5 border border-white/10 rounded-2xl p-8 shadow-[0_8px_30px_rgb(0,0,0,0.12)]">
                      <h3 className="text-xl font-bold mb-2">
                        Gastos por Categoría
                      </h3>
                      <p className="text-slate-400 text-sm mb-6">
                        Análisis de tus egresos
                      </p>

                      {gastosPorCategoria.length > 0 ? (
                        <div className="h-64 flex items-center justify-center">
                          <React.Suspense
                            fallback={
                              <Loader2 className="animate-spin text-cyan-400 w-8 h-8" />
                            }
                          >
                            <GastosChart
                              gastosPorCategoria={gastosPorCategoria}
                            />
                          </React.Suspense>
                        </div>
                      ) : (
                        <div className="h-64 flex items-center justify-center text-slate-500 border-2 border-dashed border-slate-600 rounded-xl">
                          No hay suficientes datos de gastos
                        </div>
                      )}
                    </div>
                  </div>

                  <div className="animate-in fade-in slide-in-from-bottom-4 duration-500 bg-white/5 border border-white/10 rounded-2xl p-8 shadow-[0_8px_30px_rgb(0,0,0,0.12)] mt-10">
                    <h3 className="text-xl font-bold mb-6 flex items-center gap-2">
                      Mercado Cripto{" "}
                      <span className="text-xs bg-cyan-900/300 text-white px-2 py-1 rounded-full">
                        En Vivo
                      </span>
                    </h3>
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                      {criptos.length > 0 ? (
                        criptos.map((cripto) => {
                          const miInversion = portafolioCripto.find(
                            (p) => p.criptomoneda === cripto.id,
                          );
                          const balanceUSD = miInversion
                            ? miInversion.cantidad * cripto.price
                            : 0;
                          const ganancias = miInversion
                            ? balanceUSD - miInversion.invertido_usd
                            : 0;

                          return (
                            <div
                              key={cripto.id}
                              className="bg-black/30 border border-white/20 p-4 rounded-xl flex flex-col justify-between"
                            >
                              <div>
                                <div className="flex justify-between items-center mb-2">
                                  <span className="font-bold text-white">
                                    {cripto.name}{" "}
                                    <span className="text-slate-500 text-sm">
                                      ({cripto.symbol})
                                    </span>
                                  </span>
                                  <span
                                    className={`text-sm font-bold ${cripto.change >= 0 ? "text-emerald-400" : "text-rose-400"}`}
                                  >
                                    {cripto.change >= 0 ? "+" : ""}
                                    {cripto.change.toFixed(2)}%
                                  </span>
                                </div>
                                <span className="text-2xl font-black text-cyan-300">
                                  ${cripto.price.toLocaleString()}
                                </span>
                              </div>

                              {/* Portafolio Info */}
                              <div className="mt-4 p-3 bg-white/5 rounded-lg border border-white/5">
                                <p className="text-xs text-slate-400 mb-1">
                                  Mi Portafolio
                                </p>
                                {miInversion ? (
                                  <div>
                                    <p className="font-bold text-white text-sm">
                                      {miInversion.cantidad.toFixed(6)}{" "}
                                      {cripto.symbol}
                                    </p>
                                    <p className="text-xs text-slate-400 flex justify-between">
                                      <span>${balanceUSD.toFixed(2)} USD</span>
                                      <span
                                        className={
                                          ganancias >= 0
                                            ? "text-emerald-400"
                                            : "text-rose-400"
                                        }
                                      >
                                        {ganancias >= 0 ? "+" : ""}$
                                        {ganancias.toFixed(2)}
                                      </span>
                                    </p>
                                  </div>
                                ) : (
                                  <p className="text-xs text-slate-500">
                                    Sin inversión
                                  </p>
                                )}
                              </div>

                              {/* Acciones */}
                              <div className="mt-4 flex gap-2">
                                <button
                                  onClick={() => {
                                    const amt = prompt(
                                      `¿Cuántos USD de ${cripto.name} quieres comprar?`,
                                    );
                                    if (amt) {
                                      procesarInversion(
                                        cripto.id,
                                        cripto.symbol,
                                        "comprar",
                                        amt,
                                      );
                                    }
                                  }}
                                  className="flex-1 bg-cyan-500/20 text-cyan-400 hover:bg-cyan-500 hover:text-white py-1.5 rounded font-bold text-sm transition-colors border border-cyan-500/30"
                                >
                                  Comprar
                                </button>
                                {miInversion && (
                                  <button
                                    onClick={() => {
                                      const amt = prompt(
                                        `¿Qué cantidad de ${cripto.symbol} quieres vender? (Tienes ${miInversion.cantidad})`,
                                      );
                                      if (amt) {
                                        procesarInversion(
                                          cripto.id,
                                          cripto.symbol,
                                          "vender",
                                          amt,
                                        );
                                      }
                                    }}
                                    className="flex-1 bg-rose-500/20 text-rose-400 hover:bg-rose-500 hover:text-white py-1.5 rounded font-bold text-sm transition-colors border border-rose-500/30"
                                  >
                                    Vender
                                  </button>
                                )}
                              </div>
                            </div>
                          );
                        })
                      ) : (
                        <div className="col-span-3 text-center text-slate-500 py-4">
                          Cargando mercado...
                        </div>
                      )}
                    </div>
                  </div>
                </>
              )}

              {/* VISTA: TRANSFERENCIAS */}
              {opcionActiva === "transferir" && (
                <div className="animate-in fade-in slide-in-from-bottom-4 duration-500 bg-white/5 border border-white/10 rounded-2xl p-8 shadow-[0_8px_30px_rgb(0,0,0,0.12)] max-w-2xl mx-auto">
                  <div className="flex justify-between items-center mb-6">
                    <h3 className="text-2xl font-bold text-cyan-300 flex items-center gap-3">
                      <Send /> Transferencias
                    </h3>
                    <button
                      type="button"
                      onClick={() => setScanMode(!scanMode)}
                      className="text-sm bg-cyan-900/40 text-cyan-300 px-4 py-2 rounded-lg font-bold border border-cyan-500/30 hover:bg-cyan-500 hover:text-white transition-colors"
                    >
                      {scanMode ? "Ingreso Manual" : "Escanear / Mostrar QR"}
                    </button>
                  </div>

                  {!scanMode ? (
                    <form onSubmit={procesarTransferencia}>
                      <div className="mb-6">
                        <label className="block text-slate-400 mb-2 font-semibold">
                          Destinatario (Correo)
                        </label>
                        <input
                          type="email"
                          value={emailDestino}
                          onChange={(e) => setEmailDestino(e.target.value)}
                          className="w-full bg-black/30 border border-white/20 rounded-lg px-4 py-3 text-white focus:outline-none focus:border-cyan-400 focus:ring-1 focus:ring-cyan-400"
                          placeholder="usuario@gmail.com"
                          list="contactos-list"
                        />
                        <datalist id="contactos-list">
                          {contactos.map((c) => (
                            <option key={c._id} value={c.email_contacto}>
                              {c.alias}
                            </option>
                          ))}
                        </datalist>
                        <p className="text-xs text-slate-500 mt-2">
                          Puedes seleccionar un contacto frecuente o escribir un
                          nuevo correo.
                        </p>
                      </div>
                      <div className="mb-8">
                        <label className="block text-slate-400 mb-2 font-semibold">
                          Monto a transferir ($)
                        </label>
                        <input
                          type="number"
                          step="0.01"
                          min="1"
                          value={montoTransfer}
                          onChange={(e) => setMontoTransfer(e.target.value)}
                          className="w-full bg-black/30 border border-white/20 rounded-lg px-4 py-3 text-2xl text-white focus:outline-none focus:border-cyan-400 focus:ring-1 focus:ring-cyan-400"
                          placeholder="0.00"
                        />
                      </div>
                      <button
                        type="submit"
                        className="w-full bg-gradient-to-r from-cyan-500 to-blue-500 hover:from-cyan-400 hover:to-blue-400 shadow-[0_0_15px_rgba(6,182,212,0.5)] border border-cyan-400/30 text-white font-bold py-4 rounded-lg shadow-lg shadow-teal-900/50 transition-all text-lg"
                      >
                        Confirmar Transferencia
                      </button>
                    </form>
                  ) : (
                    <div className="flex flex-col md:flex-row gap-8">
                      {/* Mi Código QR */}
                      <div className="flex-1 flex flex-col items-center justify-center bg-black/20 p-6 rounded-xl border border-white/10">
                        <p className="text-slate-400 font-semibold mb-4">
                          Mi Código QR (Para Recibir)
                        </p>
                        <div className="bg-white p-4 rounded-xl">
                          <QRCodeSVG value={auth.email} size={150} />
                        </div>
                        <p className="text-xs text-slate-500 mt-4 text-center">
                          Muestra este código para que otros te envíen dinero
                          directamente.
                        </p>
                      </div>

                      {/* Escáner */}
                      <div className="flex-1 flex flex-col items-center justify-center bg-black/20 p-6 rounded-xl border border-white/10">
                        <p className="text-slate-400 font-semibold mb-4">
                          Escanear QR (Para Enviar)
                        </p>
                        <QRScanner
                          onScanSuccess={(decodedText) => {
                            // The decoded text should be the email directly if they scanned our QR
                            setEmailDestino(decodedText);
                            setScanMode(false); // Go back to the form
                            setMensaje({
                              tipo: "exito",
                              texto:
                                "Correo escaneado exitosamente. Ingresa el monto.",
                            });
                          }}
                        />
                      </div>
                    </div>
                  )}
                </div>
              )}

              {/* VISTA: CAJERO */}
              {opcionActiva === "cajero" && (
                <div className="animate-in fade-in slide-in-from-bottom-4 duration-500 bg-white/5 border border-white/10 rounded-2xl p-8 shadow-[0_8px_30px_rgb(0,0,0,0.12)] max-w-2xl mx-auto">
                  <h3 className="text-2xl font-bold text-cyan-300 mb-6 flex items-center gap-3">
                    <Wallet /> Cajero Automático
                  </h3>
                  <p className="text-slate-400 mb-6">
                    Simula un depósito o retiro de efectivo físico hacia/desde
                    tu cuenta bancaria.
                  </p>

                  <div className="mb-8">
                    <label className="block text-slate-400 mb-2 font-semibold">
                      Monto de la operación ($)
                    </label>
                    <input
                      type="number"
                      step="0.01"
                      min="1"
                      value={montoCajero}
                      onChange={(e) => setMontoCajero(e.target.value)}
                      className="w-full bg-black/30 border border-white/20 rounded-lg px-4 py-3 text-2xl text-white focus:outline-none focus:border-cyan-400 focus:ring-1 focus:ring-cyan-400"
                      placeholder="0.00"
                    />
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <button
                      onClick={depositarFondos}
                      className="w-full bg-emerald-600 hover:bg-emerald-500 text-white font-bold py-4 rounded-lg shadow-lg shadow-emerald-900/50 transition-all text-lg"
                    >
                      Ingresar Efectivo (Depositar)
                    </button>
                    <button
                      onClick={retirarFondos}
                      className="w-full bg-rose-600 hover:bg-rose-500 text-white font-bold py-4 rounded-lg shadow-lg shadow-rose-900/50 transition-all text-lg"
                    >
                      Sacar Efectivo (Retirar)
                    </button>
                  </div>
                </div>
              )}

              {/* VISTA: B2B NÓMINA */}
              {opcionActiva === "business" && (
                <div className="animate-in fade-in slide-in-from-bottom-4 duration-500 bg-white/5 border border-white/10 rounded-2xl p-8 shadow-[0_8px_30px_rgb(0,0,0,0.12)] max-w-2xl mx-auto">
                  <h3 className="text-2xl font-bold text-cyan-300 mb-6 flex items-center gap-3">
                    <Building /> Cuentas Business - Pago de Nómina
                  </h3>
                  <p className="text-slate-400 mb-6">
                    Pega un arreglo JSON con los correos y montos para realizar
                    pagos masivos. Ejemplo:{" "}
                    <code className="bg-black/30 px-2 py-1 rounded text-cyan-300">
                      [&#123;"email":"empleado@mail.com", "monto": 500&#125;]
                    </code>
                  </p>
                  <form onSubmit={procesarNomina}>
                    <textarea
                      name="nominaData"
                      rows="6"
                      className="w-full bg-black/30 border border-white/20 rounded-lg p-4 text-white font-mono text-sm focus:outline-none focus:border-cyan-400 focus:ring-1 focus:ring-cyan-400 mb-6"
                      placeholder='[&#123;"email": "...", "monto": ...&#125;]'
                    ></textarea>
                    <button
                      type="submit"
                      className="w-full bg-gradient-to-r from-cyan-500 to-blue-500 hover:from-cyan-400 hover:to-blue-400 text-white font-bold py-4 rounded-lg transition-all text-lg shadow-[0_0_15px_rgba(6,182,212,0.5)] border border-cyan-400/30"
                    >
                      Procesar Nómina Masiva
                    </button>
                  </form>
                </div>
              )}

              {/* VISTA: SWIFT */}
              {opcionActiva === "swift" && (
                <div className="animate-in fade-in slide-in-from-bottom-4 duration-500 bg-white/5 border border-white/10 rounded-2xl p-8 shadow-[0_8px_30px_rgb(0,0,0,0.12)] max-w-3xl mx-auto">
                  <h3 className="text-2xl font-bold text-cyan-300 mb-2 flex items-center gap-3">
                    <Globe /> Transferencias Internacionales SWIFT
                  </h3>
                  <p className="text-slate-400 mb-8 text-sm">
                    Envía dinero a cualquier parte del mundo. Comisión fija de
                    $25. Tarda aprox 3 días hábiles.
                  </p>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                    <form onSubmit={solicitarSwift} className="space-y-4">
                      <div>
                        <label className="block text-slate-400 mb-1 font-semibold text-sm">
                          Nombre del Destinatario
                        </label>
                        <input
                          name="destinatario_nombre"
                          type="text"
                          required
                          className="w-full bg-black/30 border border-white/20 rounded-lg px-4 py-2 text-white focus:outline-none focus:border-cyan-400"
                        />
                      </div>
                      <div>
                        <label className="block text-slate-400 mb-1 font-semibold text-sm">
                          Código SWIFT / BIC
                        </label>
                        <input
                          name="codigo_swift"
                          type="text"
                          required
                          className="w-full bg-black/30 border border-white/20 rounded-lg px-4 py-2 text-white uppercase focus:outline-none focus:border-cyan-400"
                        />
                      </div>
                      <div>
                        <label className="block text-slate-400 mb-1 font-semibold text-sm">
                          Banco Destino
                        </label>
                        <input
                          name="banco_destino"
                          type="text"
                          required
                          className="w-full bg-black/30 border border-white/20 rounded-lg px-4 py-2 text-white focus:outline-none focus:border-cyan-400"
                        />
                      </div>
                      <div>
                        <label className="block text-slate-400 mb-1 font-semibold text-sm">
                          Monto ($ USD) - Min $100
                        </label>
                        <input
                          name="monto"
                          type="number"
                          min="100"
                          step="0.01"
                          required
                          className="w-full bg-black/30 border border-white/20 rounded-lg px-4 py-2 text-white focus:outline-none focus:border-cyan-400"
                        />
                      </div>
                      <button
                        type="submit"
                        className="w-full bg-gradient-to-r from-cyan-500 to-blue-500 hover:from-cyan-400 hover:to-blue-400 text-white font-bold py-3 rounded-lg transition-all mt-4"
                      >
                        Solicitar Transferencia
                      </button>
                    </form>
                    <div className="bg-black/20 p-4 rounded-xl border border-white/5 overflow-y-auto max-h-[400px] custom-scrollbar">
                      <h4 className="font-bold text-cyan-300 mb-4 sticky top-0 bg-[#0a0a1a] p-2 rounded z-10">
                        Mis Transferencias SWIFT
                      </h4>
                      {swifts.length > 0 ? (
                        swifts.map((s) => (
                          <div
                            key={s._id}
                            className="bg-white/5 p-3 rounded-lg mb-3 border border-white/5"
                          >
                            <div className="flex justify-between items-center mb-1">
                              <span className="font-bold">
                                {s.banco_destino}
                              </span>
                              <span
                                className={`text-xs px-2 py-1 rounded-full ${s.estado === "Completado" ? "bg-emerald-500/20 text-emerald-400" : s.estado === "Rechazado" ? "bg-rose-500/20 text-rose-400" : "bg-amber-500/20 text-amber-400"}`}
                              >
                                {s.estado}
                              </span>
                            </div>
                            <p className="text-slate-400 text-sm">
                              {s.destinatario_nombre}
                            </p>
                            <p className="text-cyan-400 font-bold mt-2">
                              ${s.monto}
                            </p>
                          </div>
                        ))
                      ) : (
                        <p className="text-slate-500 text-sm text-center mt-10">
                          No tienes transferencias SWIFT
                        </p>
                      )}
                    </div>
                  </div>
                </div>
              )}

              {/* VISTA: SOPORTE */}
              {opcionActiva === "soporte" && (
                <div className="animate-in fade-in slide-in-from-bottom-4 duration-500 bg-white/5 border border-white/10 rounded-2xl p-8 shadow-[0_8px_30px_rgb(0,0,0,0.12)] max-w-4xl mx-auto">
                  <h3 className="text-2xl font-bold text-cyan-300 mb-6 flex items-center gap-3">
                    <HeadphonesIcon /> Centro de Soporte
                  </h3>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                    <form onSubmit={crearTicket} className="space-y-4">
                      <div>
                        <label className="block text-slate-400 mb-1 font-semibold text-sm">
                          Asunto
                        </label>
                        <input
                          name="asunto"
                          type="text"
                          required
                          className="w-full bg-black/30 border border-white/20 rounded-lg px-4 py-2 text-white focus:outline-none focus:border-cyan-400"
                        />
                      </div>
                      <div>
                        <label className="block text-slate-400 mb-1 font-semibold text-sm">
                          Categoría
                        </label>
                        <select
                          name="categoria"
                          className="w-full bg-black/30 border border-white/20 rounded-lg px-4 py-2 text-white focus:outline-none focus:border-cyan-400 [&>option]:bg-slate-900"
                        >
                          <option value="Cuenta">Cuenta</option>
                          <option value="Tarjeta">Tarjeta</option>
                          <option value="Transferencia">Transferencia</option>
                          <option value="Otro">Otro</option>
                        </select>
                      </div>
                      <div>
                        <label className="block text-slate-400 mb-1 font-semibold text-sm">
                          Mensaje
                        </label>
                        <textarea
                          name="mensaje"
                          rows="5"
                          required
                          className="w-full bg-black/30 border border-white/20 rounded-lg px-4 py-2 text-white focus:outline-none focus:border-cyan-400"
                        ></textarea>
                      </div>
                      <button
                        type="submit"
                        className="w-full bg-gradient-to-r from-cyan-500 to-blue-500 hover:from-cyan-400 hover:to-blue-400 text-white font-bold py-3 rounded-lg transition-all"
                      >
                        Crear Ticket
                      </button>
                    </form>
                    <div className="bg-black/20 p-4 rounded-xl border border-white/5 overflow-y-auto max-h-[500px] custom-scrollbar">
                      <h4 className="font-bold text-cyan-300 mb-4 sticky top-0 bg-[#0a0a1a] p-2 rounded z-10">
                        Mis Tickets
                      </h4>
                      {tickets.length > 0 ? (
                        tickets.map((t) => (
                          <div
                            key={t._id}
                            className="bg-white/5 p-4 rounded-lg mb-3 border border-white/5"
                          >
                            <div className="flex justify-between items-center mb-2">
                              <span className="font-bold text-white truncate max-w-[70%]">
                                {t.asunto}
                              </span>
                              <span
                                className={`text-xs px-2 py-1 rounded-full shrink-0 ${t.estado === "Resuelto" ? "bg-emerald-500/20 text-emerald-400" : t.estado === "Cerrado" ? "bg-slate-500/20 text-slate-400" : "bg-amber-500/20 text-amber-400"}`}
                              >
                                {t.estado}
                              </span>
                            </div>
                            <p className="text-xs text-slate-400 mb-2 bg-black/30 inline-block px-2 py-1 rounded">
                              {t.categoria}
                            </p>
                            <p className="text-slate-300 text-sm whitespace-pre-wrap">
                              {t.mensaje}
                            </p>
                            {t.respuesta && (
                              <div className="mt-3 p-3 bg-cyan-900/20 border-l-2 border-cyan-500 rounded-r">
                                <p className="text-xs text-cyan-400 font-bold mb-1">
                                  Respuesta de Soporte:
                                </p>
                                <p className="text-sm text-cyan-100">
                                  {t.respuesta}
                                </p>
                              </div>
                            )}
                          </div>
                        ))
                      ) : (
                        <p className="text-slate-500 text-sm text-center mt-10">
                          No hay tickets de soporte
                        </p>
                      )}
                    </div>
                  </div>
                </div>
              )}

              {/* VISTA: SEGURIDAD Y KYC */}
              {opcionActiva === "seguridad" && (
                <div className="space-y-8">
                  <div className="animate-in fade-in slide-in-from-bottom-4 duration-500 bg-white/5 border border-white/10 rounded-2xl p-8 shadow-[0_8px_30px_rgb(0,0,0,0.12)]">
                    <h3 className="text-2xl font-bold text-cyan-300 mb-4 flex items-center gap-3">
                      <ShieldCheck /> Verificación de Identidad (KYC)
                    </h3>
                    <div className="mb-6">
                      <p className="text-slate-400 mb-2">
                        Estado actual de tu verificación:
                      </p>
                      <span
                        className={`px-4 py-2 rounded-full font-bold text-sm ${
                          auth.estadoKYC === "Aprobado"
                            ? "bg-emerald-500/20 text-emerald-400"
                            : auth.estadoKYC === "Rechazado"
                              ? "bg-rose-500/20 text-rose-400"
                              : "bg-amber-500/20 text-amber-400"
                        }`}
                      >
                        {auth.estadoKYC || "Pendiente"}
                      </span>
                    </div>

                    {auth.estadoKYC !== "Aprobado" && (
                      <form
                        onSubmit={subirKYC}
                        className="bg-black/20 p-6 rounded-xl border border-white/10"
                      >
                        <label className="block text-slate-400 mb-2 font-semibold">
                          Sube una foto de tu ID oficial (Cédula o Pasaporte)
                        </label>
                        <input
                          type="file"
                          accept="image/*,.pdf"
                          onChange={(e) => setArchivoKYC(e.target.files[0])}
                          className="block w-full text-sm text-slate-400 file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-sm file:font-semibold file:bg-cyan-900/30 file:text-cyan-300 hover:file:bg-cyan-800/50 mb-4"
                        />
                        <button
                          type="submit"
                          className="bg-gradient-to-r from-cyan-500 to-blue-500 hover:from-cyan-400 hover:to-blue-400 shadow-[0_0_15px_rgba(6,182,212,0.5)] border border-cyan-400/30 text-white font-bold py-2 px-6 rounded-lg transition-colors"
                        >
                          Subir Documento
                        </button>
                      </form>
                    )}
                  </div>

                  <div className="animate-in fade-in slide-in-from-bottom-4 duration-500 bg-white/5 border border-white/10 rounded-2xl p-8 shadow-[0_8px_30px_rgb(0,0,0,0.12)]">
                    <h3 className="text-2xl font-bold text-cyan-300 mb-4 flex items-center gap-3">
                      <Lock /> Autenticación de Dos Factores (2FA)
                    </h3>
                    <p className="text-slate-400 mb-6">
                      Protege tu cuenta requiriendo un código temporal al
                      iniciar sesión.
                    </p>

                    {auth.dosFA_activo ? (
                      <div className="bg-emerald-500/20 text-emerald-400 p-4 rounded-xl border border-emerald-500/30 flex items-center gap-3">
                        <Unlock size={24} /> Tu cuenta está protegida con 2FA.
                      </div>
                    ) : (
                      <div className="bg-black/20 p-6 rounded-xl border border-white/10 flex flex-col items-center sm:items-start">
                        {!qrCodeUrl ? (
                          <button
                            onClick={generar2FA}
                            className="bg-gradient-to-r from-cyan-500 to-blue-500 hover:from-cyan-400 hover:to-blue-400 shadow-[0_0_15px_rgba(6,182,212,0.5)] border border-cyan-400/30 text-white font-bold py-3 px-6 rounded-lg transition-colors w-full sm:w-auto"
                          >
                            Configurar 2FA (Google Authenticator)
                          </button>
                        ) : (
                          <div className="w-full flex flex-col sm:flex-row gap-8 items-center">
                            <div className="bg-white p-4 rounded-xl">
                              <img
                                src={qrCodeUrl}
                                alt="QR Code 2FA"
                                className="w-48 h-48"
                              />
                            </div>
                            <div className="flex-1 w-full">
                              <p className="text-slate-300 mb-4">
                                1. Escanea el código con tu app Authenticator.
                              </p>
                              <p className="text-slate-300 mb-2">
                                2. Ingresa el código de 6 dígitos para
                                verificar:
                              </p>
                              <form
                                onSubmit={activar2FA}
                                className="flex gap-2"
                              >
                                <input
                                  type="text"
                                  maxLength="6"
                                  placeholder="000000"
                                  value={token2FA}
                                  onChange={(e) => setToken2FA(e.target.value)}
                                  className="bg-slate-800 border border-slate-600 rounded-lg px-4 py-2 text-white focus:outline-none focus:border-cyan-400 text-center tracking-[0.5em] text-xl w-40"
                                />
                                <button
                                  type="submit"
                                  className="bg-gradient-to-r from-cyan-500 to-blue-500 hover:from-cyan-400 hover:to-blue-400 shadow-[0_0_15px_rgba(6,182,212,0.5)] border border-cyan-400/30 text-white font-bold py-2 px-6 rounded-lg transition-colors"
                                >
                                  Activar
                                </button>
                              </form>
                            </div>
                          </div>
                        )}
                      </div>
                    )}
                  </div>

                  {/* SECCION ELIMINAR CUENTA */}
                  <div className="bg-rose-900/10 border border-rose-500/30 rounded-2xl p-8 shadow-2xl">
                    <h3 className="text-2xl font-bold text-rose-500 mb-4 flex items-center gap-3">
                      <AlertTriangle /> Zona de Peligro
                    </h3>
                    <p className="text-slate-400 mb-6">
                      Una vez que elimines tu cuenta, no hay vuelta atrás. Por
                      favor, asegúrate de estar seguro.
                    </p>

                    {pasoEliminar === 0 ? (
                      <button
                        onClick={solicitarEliminacion}
                        className="bg-rose-600 hover:bg-rose-700 text-white font-bold py-3 px-6 rounded-lg transition-colors border border-rose-500 shadow-lg shadow-rose-900/50"
                      >
                        Solicitar Eliminación de Cuenta
                      </button>
                    ) : (
                      <form
                        onSubmit={confirmarEliminacion}
                        className="bg-rose-950/40 p-6 rounded-xl border border-rose-800/50"
                      >
                        <p className="text-rose-300 mb-4 font-semibold">
                          Revisa tu correo. Ingresa el código de seguridad de 6
                          dígitos que te enviamos para confirmar la eliminación.
                        </p>
                        <div className="flex gap-4">
                          <input
                            type="text"
                            maxLength="6"
                            placeholder="000000"
                            value={tokenEliminar}
                            onChange={(e) => setTokenEliminar(e.target.value)}
                            className="bg-slate-900 border border-rose-700 rounded-lg px-4 py-2 text-white focus:outline-none focus:border-rose-400 text-center tracking-[0.5em] text-xl w-48"
                          />
                          <button
                            type="submit"
                            className="bg-rose-600 hover:bg-rose-700 text-white font-bold py-2 px-6 rounded-lg transition-colors border border-rose-500"
                          >
                            Eliminar Permanentemente
                          </button>
                        </div>
                      </form>
                    )}
                  </div>
                </div>
              )}

              {/* VISTA: BOLSILLOS / METAS */}
              {opcionActiva === "bolsillos" && (
                <div>
                  <div className="flex justify-between items-center mb-8">
                    <h3 className="text-2xl font-bold flex items-center gap-2">
                      <PiggyBank className="text-cyan-400" /> Mis Bolsillos
                    </h3>
                    <button
                      onClick={() => toggleRedondeo(!auth.redondeoActivo)}
                      className={`px-4 py-2 rounded-lg font-bold transition-all border ${auth.redondeoActivo ? "bg-emerald-500/20 text-emerald-400 border-emerald-500/30" : "bg-slate-800 text-slate-400 border-slate-600 hover:bg-slate-700"}`}
                    >
                      {auth.redondeoActivo
                        ? "Redondeo Activado"
                        : "Redondeo Desactivado"}
                    </button>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6 mb-10">
                    {/* Formulario Crear Meta */}
                    <div className="bg-slate-800 border border-white/10 border-dashed rounded-2xl p-6 flex flex-col justify-center">
                      <h4 className="text-lg font-bold text-cyan-300 mb-4 flex items-center gap-2">
                        <Plus size={18} /> Crear nueva meta
                      </h4>
                      <form onSubmit={crearMeta}>
                        <input
                          type="text"
                          placeholder="Nombre (Ej. Vacaciones)"
                          value={nombreMeta}
                          onChange={(e) => setNombreMeta(e.target.value)}
                          className="w-full mb-3 bg-black/30 border border-white/20 rounded p-2 text-white"
                        />
                        <input
                          type="number"
                          placeholder="Monto objetivo $"
                          value={montoMeta}
                          onChange={(e) => setMontoMeta(e.target.value)}
                          className="w-full mb-3 bg-black/30 border border-white/20 rounded p-2 text-white"
                        />
                        <button
                          type="submit"
                          className="w-full bg-gradient-to-r from-cyan-500 to-blue-500 hover:from-cyan-400 hover:to-blue-400 shadow-[0_0_15px_rgba(6,182,212,0.5)] border border-cyan-400/30 text-white font-bold py-2 rounded"
                        >
                          Crear Bolsillo
                        </button>
                      </form>
                    </div>

                    {/* Lista de Metas */}
                    {metas.map((meta) => {
                      const porcentaje = Math.min(
                        100,
                        Math.round(
                          (meta.balance_actual / meta.monto_objetivo) * 100,
                        ),
                      );
                      return (
                        <div
                          key={meta._id}
                          className="bg-white/5  border border-white/10 rounded-2xl p-6 shadow-[0_8px_30px_rgb(0,0,0,0.12)]"
                        >
                          <div className="flex justify-between items-start mb-4">
                            <h4 className="text-xl font-bold text-white capitalize">
                              {meta.nombre}
                            </h4>
                            <span className="text-xs bg-slate-700 px-2 py-1 rounded text-cyan-300">
                              {porcentaje}%
                            </span>
                          </div>

                          <div className="w-full bg-slate-900 rounded-full h-3 mb-6 border border-white/10 overflow-hidden">
                            <div
                              className="bg-gradient-to-r from-teal-600 to-teal-400 h-3 rounded-full transition-all duration-1000"
                              style={{ width: `${porcentaje}%` }}
                            ></div>
                          </div>

                          <div className="flex justify-between text-sm mb-6">
                            <div>
                              <p className="text-slate-400">Ahorrado</p>
                              <p className="font-bold text-white">
                                {"$"}
                                {meta.balance_actual.toFixed(2)}
                              </p>
                            </div>
                            <div className="text-right">
                              <p className="text-slate-400">Objetivo</p>
                              <p className="font-bold text-white">
                                {"$"}
                                {meta.monto_objetivo.toFixed(2)}
                              </p>
                            </div>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}

              {/* VISTA: PRESTAMOS */}
              {opcionActiva === "prestamos" && (
                <div className="space-y-8">
                  <div className="animate-in fade-in slide-in-from-bottom-4 duration-500 bg-white/5 border border-white/10 rounded-2xl p-8 shadow-[0_8px_30px_rgb(0,0,0,0.12)] max-w-2xl mx-auto">
                    <h3 className="text-2xl font-bold text-cyan-300 mb-6 flex items-center gap-3">
                      <Banknote /> Solicitar Préstamo
                    </h3>
                    <p className="text-slate-400 mb-6">
                      Solicita un crédito inmediato con nuestra tasa de interés
                      competitiva. Los intereses se calculan diariamente
                      mediante nuestros sistemas automatizados.
                    </p>

                    <form onSubmit={solicitarPrestamo}>
                      <div className="mb-6">
                        <label className="block text-slate-400 mb-2 font-semibold">
                          Monto solicitado ($)
                        </label>
                        <input
                          type="number"
                          step="0.01"
                          min="10"
                          value={montoPrestamo}
                          onChange={(e) => setMontoPrestamo(e.target.value)}
                          className="w-full bg-black/30 border border-white/20 rounded-lg px-4 py-3 text-2xl text-white focus:outline-none focus:border-cyan-400 focus:ring-1 focus:ring-cyan-400"
                          placeholder="100.00"
                        />
                      </div>
                      <button
                        type="submit"
                        className="w-full bg-gradient-to-r from-cyan-500 to-blue-500 hover:from-cyan-400 hover:to-blue-400 shadow-[0_0_15px_rgba(6,182,212,0.5)] border border-cyan-400/30 text-white font-bold py-4 rounded-lg shadow-lg shadow-teal-900/50 transition-all text-lg"
                      >
                        Confirmar Préstamo
                      </button>
                    </form>
                  </div>

                  {prestamos.length > 0 && (
                    <div className="animate-in fade-in slide-in-from-bottom-4 duration-500 bg-white/5 border border-white/10 rounded-2xl p-8 shadow-[0_8px_30px_rgb(0,0,0,0.12)]">
                      <h3 className="text-2xl font-bold text-cyan-300 mb-6">
                        Mis Préstamos Activos
                      </h3>
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        {prestamos.map((prestamo) => (
                          <div
                            key={prestamo._id}
                            className="bg-black/20 p-6 rounded-xl border border-white/10"
                          >
                            <div className="flex justify-between items-center mb-4">
                              <span className="font-bold text-lg text-white">
                                Préstamo #{prestamo._id.slice(-4)}
                              </span>
                              <span
                                className={`px-3 py-1 rounded-full text-xs font-bold ${prestamo.estado === "Activo" ? "bg-amber-500/20 text-amber-400" : "bg-emerald-500/20 text-emerald-400"}`}
                              >
                                {prestamo.estado}
                              </span>
                            </div>
                            <div className="space-y-2">
                              <div className="flex justify-between">
                                <span className="text-slate-400">
                                  Monto Inicial
                                </span>
                                <span className="font-bold text-white">
                                  ${prestamo.monto_inicial.toFixed(2)}
                                </span>
                              </div>
                              <div className="flex justify-between">
                                <span className="text-slate-400">
                                  Deuda Actual
                                </span>
                                <span className="font-black text-rose-400">
                                  ${prestamo.deuda_actual.toFixed(2)}
                                </span>
                              </div>
                              <div className="flex justify-between">
                                <span className="text-slate-400">
                                  Tasa Diaria
                                </span>
                                <span className="font-bold text-white">
                                  {(prestamo.tasa_interes * 100).toFixed(2)}%
                                </span>
                              </div>
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              )}

              {/* VISTA: HISTORIAL Y REPORTES */}
              {opcionActiva === "movimientos" && (
                <div className="animate-in fade-in slide-in-from-bottom-4 duration-500 bg-white/5 border border-white/10 rounded-2xl shadow-[0_8px_30px_rgb(0,0,0,0.12)] overflow-hidden">
                  <div className="p-6 border-b border-white/10 flex justify-between items-center bg-white/5">
                    <h3 className="text-xl font-bold flex items-center gap-2">
                      <History className="text-cyan-400" /> Historial de
                      Transacciones
                    </h3>
                    <button
                      onClick={descargarPDFBlob}
                      className="flex items-center gap-2 bg-slate-700 hover:bg-slate-600 border border-slate-600 px-4 py-2 rounded-lg font-bold transition-colors text-sm text-white"
                    >
                      <Download size={16} /> Descargar PDF
                    </button>
                  </div>

                  <div className="overflow-x-auto">
                    <table className="w-full text-left border-collapse">
                      <thead>
                        <tr className="bg-slate-900/50 text-slate-400 text-sm uppercase tracking-wider">
                          <th className="p-4 font-semibold">Fecha</th>
                          <th className="p-4 font-semibold">Tipo</th>
                          <th className="p-4 font-semibold">Categoría</th>
                          <th className="p-4 font-semibold text-right">
                            Monto
                          </th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-700/50">
                        {historial.length === 0 ? (
                          <tr>
                            <td
                              colSpan="4"
                              className="p-8 text-center text-slate-500"
                            >
                              No hay movimientos recientes.
                            </td>
                          </tr>
                        ) : (
                          historial.map((tx) => {
                            // Determinar si es ingreso o egreso
                            let isIngreso = true;
                            if (tx.tipo === "Retiro" || tx.tipo === "Ahorro")
                              isIngreso = false;
                            if (
                              tx.tipo === "Transferencia" &&
                              tx.remitente === auth._id
                            )
                              isIngreso = false;

                            return (
                              <tr
                                key={tx._id}
                                className="hover:bg-white/5 transition-colors"
                              >
                                <td className="p-4 text-slate-300 text-sm">
                                  {new Date(tx.fecha).toLocaleDateString()}
                                </td>
                                <td className="p-4 text-white font-medium">
                                  {tx.tipo}
                                </td>
                                <td className="p-4 text-slate-400">
                                  {tx.categoria}
                                </td>
                                <td
                                  className={`p-4 text-right font-black ${isIngreso ? "text-emerald-400" : "text-rose-400"}`}
                                >
                                  {isIngreso ? "+" : "-"}
                                  {"$"}
                                  {tx.monto.toFixed(2)}
                                </td>
                              </tr>
                            );
                          })
                        )}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}

              {/* VISTA: SUSCRIPCIONES */}
              {opcionActiva === "suscripciones" && (
                <div className="animate-in fade-in slide-in-from-bottom-4 duration-500 bg-white/5 border border-white/10 rounded-2xl p-8 shadow-[0_8px_30px_rgb(0,0,0,0.12)]">
                  <h3 className="text-2xl font-bold text-cyan-300 mb-6 flex items-center gap-3">
                    <Bell /> Gestión de Suscripciones
                  </h3>
                  <div className="space-y-6">
                    <div>
                      <h4 className="text-lg font-bold text-white mb-4">
                        Suscripciones Detectadas
                      </h4>
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        {suscripciones.detectadas?.map((sub, i) => (
                          <div
                            key={i}
                            className="bg-black/30 border border-white/10 p-4 rounded-xl flex justify-between items-center"
                          >
                            <div>
                              <p className="font-bold text-white">
                                {sub.comercio}
                              </p>
                              <p className="text-xs text-slate-400">
                                {sub.frecuencia}
                              </p>
                            </div>
                            <span className="font-bold text-rose-400">
                              ${sub.monto}
                            </span>
                          </div>
                        ))}
                      </div>
                    </div>
                    <div>
                      <h4 className="text-lg font-bold text-white mb-4">
                        Suscripciones Guardadas
                      </h4>
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        {suscripciones.guardadas?.map((sub) => (
                          <div
                            key={sub._id}
                            className={`bg-black/30 border p-4 rounded-xl flex justify-between items-center ${sub.bloqueada ? "border-rose-500/50" : "border-white/10"}`}
                          >
                            <div>
                              <p className="font-bold text-white">
                                {sub.comercio}
                              </p>
                              <div className="flex gap-2 items-center">
                                <span className="text-xs text-slate-400">
                                  {sub.frecuencia} - ${sub.monto}
                                </span>
                                {sub.bloqueada && (
                                  <span className="text-[10px] bg-rose-500/20 text-rose-400 px-2 py-0.5 rounded font-bold">
                                    BLOQUEADA
                                  </span>
                                )}
                              </div>
                            </div>
                            <button
                              onClick={() => toggleBloquearSuscripcion(sub._id)}
                              className={`text-xs px-3 py-1.5 rounded font-bold transition-colors ${sub.bloqueada ? "bg-cyan-500/20 text-cyan-400 hover:bg-cyan-500 hover:text-white" : "bg-rose-500/20 text-rose-400 hover:bg-rose-500 hover:text-white"}`}
                            >
                              {sub.bloqueada ? "Desbloquear" : "Bloquear"}
                            </button>
                          </div>
                        ))}
                      </div>
                    </div>
                  </div>
                </div>
              )}

              {/* VISTA: TARJETA FISICA */}
              {opcionActiva === "tarjeta-fisica" && (
                <div className="animate-in fade-in slide-in-from-bottom-4 duration-500 bg-white/5 border border-white/10 rounded-2xl p-8 shadow-[0_8px_30px_rgb(0,0,0,0.12)]">
                  <h3 className="text-2xl font-bold text-cyan-300 mb-6 flex items-center gap-3">
                    <Package /> Tarjeta Física
                  </h3>

                  <form
                    onSubmit={solicitarTarjetaFisica}
                    className="mb-8 bg-black/30 border border-white/10 p-6 rounded-xl"
                  >
                    <h4 className="text-lg font-bold text-white mb-4">
                      Solicitar Nueva Tarjeta
                    </h4>
                    <div className="flex gap-4">
                      <input
                        type="text"
                        value={direccionEnvio}
                        onChange={(e) => setDireccionEnvio(e.target.value)}
                        placeholder="Dirección de envío"
                        className="flex-1 bg-slate-900 border border-white/20 rounded-lg px-4 py-2 text-white focus:outline-none focus:border-cyan-400"
                      />
                      <button
                        type="submit"
                        className="bg-gradient-to-r from-cyan-500 to-blue-500 hover:from-cyan-400 hover:to-blue-400 px-6 py-2 rounded-lg font-bold text-white shadow-lg shadow-teal-900/50"
                      >
                        Solicitar
                      </button>
                    </div>
                  </form>

                  <div className="space-y-4">
                    {tarjetasFisicas.map((tf) => (
                      <div
                        key={tf._id}
                        className="bg-black/30 border border-white/10 p-6 rounded-xl"
                      >
                        <div className="flex justify-between items-center mb-6">
                          <span className="font-bold text-white">
                            Solicitud de Tarjeta
                          </span>
                          <span className="px-3 py-1 rounded-full text-xs font-bold bg-cyan-500/20 text-cyan-400">
                            {tf.estado}
                          </span>
                        </div>

                        <div className="flex justify-between text-xs text-slate-400 mb-6 relative">
                          <div className="absolute top-1/2 left-0 right-0 h-0.5 bg-white/10 -z-10 -translate-y-1/2"></div>
                          {[
                            "Fabricación",
                            "En camino",
                            "Entregada",
                            "Activada",
                          ].map((paso, i) => {
                            const estados = [
                              "Fabricación",
                              "En camino",
                              "Entregada",
                              "Activada",
                            ];
                            const actualIdx = estados.indexOf(tf.estado);
                            const isActive = i <= actualIdx;
                            return (
                              <div
                                key={paso}
                                className="flex flex-col items-center gap-2 bg-[#050510] px-2 z-0"
                              >
                                <div
                                  className={`w-4 h-4 rounded-full ${isActive ? "bg-cyan-400 shadow-[0_0_10px_rgba(34,211,238,0.5)]" : "bg-slate-700"}`}
                                ></div>
                                <span
                                  className={
                                    isActive ? "text-cyan-400 font-bold" : ""
                                  }
                                >
                                  {paso}
                                </span>
                              </div>
                            );
                          })}
                        </div>

                        {tf.estado === "Entregada" && (
                          <form
                            onSubmit={activarTarjetaFisica}
                            className="flex gap-4 items-center mt-4"
                          >
                            <input
                              type="text"
                              maxLength="4"
                              value={digitos4}
                              onChange={(e) => setDigitos4(e.target.value)}
                              placeholder="Últimos 4 dígitos"
                              className="w-40 bg-slate-900 border border-white/20 rounded-lg px-4 py-2 text-white focus:outline-none focus:border-cyan-400 text-center tracking-[0.5em]"
                            />
                            <button
                              type="submit"
                              className="bg-emerald-600 hover:bg-emerald-500 px-6 py-2 rounded-lg font-bold text-white"
                            >
                              Activar Tarjeta
                            </button>
                          </form>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* VISTA: WALLET MOVIL */}
              {opcionActiva === "wallet-movil" && (
                <div className="animate-in fade-in slide-in-from-bottom-4 duration-500 bg-white/5 border border-white/10 rounded-2xl p-8 shadow-[0_8px_30px_rgb(0,0,0,0.12)]">
                  <h3 className="text-2xl font-bold text-cyan-300 mb-6 flex items-center gap-3">
                    <Smartphone /> Wallet Móvil
                  </h3>
                  <p className="text-slate-400 mb-6">
                    Vincula tus tarjetas con Apple Pay o Google Pay para pagos
                    sin contacto.
                  </p>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    {tarjetas.map((tarjeta) => (
                      <div
                        key={tarjeta._id}
                        className="bg-gradient-to-br from-slate-900 to-black border border-white/10 p-6 rounded-xl flex flex-col justify-between"
                      >
                        <div>
                          <div className="flex justify-between items-center mb-4">
                            <span className="font-bold text-white">
                              Tarjeta {tarjeta.tipo}
                            </span>
                            <span className="text-slate-500 font-mono">
                              **{tarjeta.numero_tarjeta.slice(-4)}
                            </span>
                          </div>
                        </div>

                        {tarjeta.tokenMovil ? (
                          <div className="mt-4 flex justify-between items-center bg-white/5 p-3 rounded-lg border border-white/5">
                            <div className="flex items-center gap-2">
                              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
                              <span className="text-sm font-bold text-emerald-400">
                                Vinculada a {tarjeta.proveedorToken || "Wallet"}
                              </span>
                            </div>
                            <button
                              onClick={() => revocarToken(tarjeta._id)}
                              className="text-xs bg-rose-500/20 text-rose-400 hover:bg-rose-500 hover:text-white px-3 py-1.5 rounded font-bold transition-colors"
                            >
                              Revocar
                            </button>
                          </div>
                        ) : (
                          <div className="mt-4 flex gap-2">
                            <button
                              onClick={() =>
                                tokenizarTarjeta(tarjeta._id, "Apple Pay")
                              }
                              className="flex-1 bg-white text-black hover:bg-slate-200 py-2 rounded-lg font-bold text-sm transition-colors flex items-center justify-center gap-2"
                            >
                              Apple Pay
                            </button>
                            <button
                              onClick={() =>
                                tokenizarTarjeta(tarjeta._id, "Google Pay")
                              }
                              className="flex-1 bg-transparent border border-white text-white hover:bg-white/10 py-2 rounded-lg font-bold text-sm transition-colors flex items-center justify-center gap-2"
                            >
                              Google Pay
                            </button>
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* VISTA: DIVISAS */}
              {opcionActiva === "divisas" && (
                <div className="animate-in fade-in slide-in-from-bottom-4 duration-500 bg-white/5 border border-white/10 rounded-2xl p-8 shadow-[0_8px_30px_rgb(0,0,0,0.12)]">
                  <h3 className="text-2xl font-bold text-cyan-300 mb-6 flex items-center gap-3">
                    <Globe /> Cambio de Divisas
                  </h3>
                  <div className="flex gap-4 mb-8">
                    <div className="bg-black/30 border border-white/20 p-4 rounded-xl flex-1 text-center">
                      <p className="text-slate-400">USD</p>
                      <p className="text-2xl font-bold text-white">
                        ${Number(auth.balances?.USD || 0).toFixed(2)}
                      </p>
                    </div>
                    <div className="bg-black/30 border border-white/20 p-4 rounded-xl flex-1 text-center">
                      <p className="text-slate-400">EUR</p>
                      <p className="text-2xl font-bold text-white">
                        €{Number(auth.balances?.EUR || 0).toFixed(2)}
                      </p>
                    </div>
                    <div className="bg-black/30 border border-white/20 p-4 rounded-xl flex-1 text-center">
                      <p className="text-slate-400">DOP</p>
                      <p className="text-2xl font-bold text-white">
                        RD${Number(auth.balances?.DOP || 0).toFixed(2)}
                      </p>
                    </div>
                  </div>

                  <form
                    onSubmit={convertirDivisa}
                    className="bg-black/20 p-6 rounded-xl border border-white/10 flex flex-col gap-4"
                  >
                    <div className="flex gap-4">
                      <div className="flex-1">
                        <label className="text-slate-400 block mb-2">De</label>
                        <select
                          value={divisaDe}
                          onChange={(e) => setDivisaDe(e.target.value)}
                          className="w-full bg-slate-900 border border-white/20 rounded-lg px-4 py-2 text-white"
                        >
                          <option value="USD">USD</option>
                          <option value="EUR">EUR</option>
                          <option value="DOP">DOP</option>
                        </select>
                      </div>
                      <div className="flex-1">
                        <label className="text-slate-400 block mb-2">A</label>
                        <select
                          value={divisaA}
                          onChange={(e) => setDivisaA(e.target.value)}
                          className="w-full bg-slate-900 border border-white/20 rounded-lg px-4 py-2 text-white"
                        >
                          <option value="USD">USD</option>
                          <option value="EUR">EUR</option>
                          <option value="DOP">DOP</option>
                        </select>
                      </div>
                    </div>
                    <div>
                      <label className="text-slate-400 block mb-2">Monto</label>
                      <input
                        type="number"
                        step="0.01"
                        min="1"
                        value={divisaMonto}
                        onChange={(e) => setDivisaMonto(e.target.value)}
                        placeholder="0.00"
                        className="w-full bg-slate-900 border border-white/20 rounded-lg px-4 py-2 text-white"
                      />
                    </div>
                    <button
                      type="submit"
                      className="mt-2 bg-gradient-to-r from-cyan-500 to-blue-500 hover:from-cyan-400 hover:to-blue-400 py-2 rounded-lg font-bold text-white transition-colors"
                    >
                      Convertir Divisa
                    </button>
                  </form>
                </div>
              )}

              {/* VISTA: PLAZO FIJO */}
              {opcionActiva === "plazo-fijo" && (
                <div className="animate-in fade-in slide-in-from-bottom-4 duration-500 bg-white/5 border border-white/10 rounded-2xl p-8 shadow-[0_8px_30px_rgb(0,0,0,0.12)]">
                  <h3 className="text-2xl font-bold text-cyan-300 mb-6 flex items-center gap-3">
                    <LockKeyhole /> Depósitos a Plazo Fijo
                  </h3>
                  <p className="text-slate-400 mb-6">
                    Bloquea tus fondos por un tiempo determinado para ganar
                    altos intereses.
                  </p>

                  <form
                    onSubmit={async (e) => {
                      e.preventDefault();
                      try {
                        const { data } = await clienteAxios.post(
                          "/plazo-fijo",
                          {
                            monto: Number(e.target.monto.value),
                            dias: Number(e.target.dias.value),
                          },
                        );
                        setMensaje({ tipo: "exito", texto: data.mensaje });
                        cargarDatosBase();
                      } catch (err) {
                        setMensaje({
                          tipo: "error",
                          texto: err.response?.data?.mensaje || "Error",
                        });
                      }
                    }}
                    className="bg-black/20 p-6 rounded-xl border border-white/10 flex flex-col gap-4 mb-8"
                  >
                    <div className="flex gap-4">
                      <div className="flex-1">
                        <label className="text-slate-400 block mb-2">
                          Monto ($)
                        </label>
                        <input
                          type="number"
                          name="monto"
                          step="0.01"
                          min="100"
                          placeholder="Min. $100"
                          className="w-full bg-slate-900 border border-white/20 rounded-lg px-4 py-2 text-white"
                          required
                        />
                      </div>
                      <div className="flex-1">
                        <label className="text-slate-400 block mb-2">
                          Plazo (Días)
                        </label>
                        <select
                          name="dias"
                          className="w-full bg-slate-900 border border-white/20 rounded-lg px-4 py-2 text-white"
                        >
                          <option value="30">30 Días (5% APY)</option>
                          <option value="60">60 Días (6% APY)</option>
                          <option value="90">90 Días (7% APY)</option>
                          <option value="180">180 Días (8.5% APY)</option>
                          <option value="365">365 Días (10% APY)</option>
                        </select>
                      </div>
                    </div>
                    <button
                      type="submit"
                      className="bg-gradient-to-r from-emerald-500 to-teal-500 hover:from-emerald-400 hover:to-teal-500 py-2 rounded-lg font-bold text-white transition-colors"
                    >
                      Abrir Plazo Fijo
                    </button>
                  </form>

                  <h4 className="text-lg font-bold text-white mb-4">
                    Mis Plazos Fijos
                  </h4>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {plazosFijos.map((pf) => (
                      <div
                        key={pf._id}
                        className="bg-black/30 border border-white/20 p-4 rounded-xl"
                      >
                        <div className="flex justify-between items-center mb-2">
                          <span className="font-bold text-white">
                            ${pf.monto.toFixed(2)}
                          </span>
                          <span className="text-sm bg-cyan-500/20 text-cyan-400 px-2 py-1 rounded">
                            {pf.tasaInteres}% APY
                          </span>
                        </div>
                        <p className="text-slate-400 text-sm mb-1">
                          Vence:{" "}
                          {new Date(pf.fechaVencimiento).toLocaleDateString()}
                        </p>
                        <p className="text-emerald-400 text-sm font-bold">
                          Interés estimado: +$
                          {pf.interesGanado?.toFixed(2) ||
                            (
                              pf.monto *
                              (pf.tasaInteres / 100) *
                              (pf.dias / 365)
                            ).toFixed(2)}
                        </p>
                      </div>
                    ))}
                    {plazosFijos.length === 0 && (
                      <p className="text-slate-500 text-sm">
                        No tienes plazos fijos activos.
                      </p>
                    )}
                  </div>
                </div>
              )}

              {/* VISTA: CUENTAS COMPARTIDAS */}
              {opcionActiva === "cuentas-compartidas" && (
                <div className="animate-in fade-in slide-in-from-bottom-4 duration-500 bg-white/5 border border-white/10 rounded-2xl p-8 shadow-[0_8px_30px_rgb(0,0,0,0.12)]">
                  <h3 className="text-2xl font-bold text-cyan-300 mb-6 flex items-center gap-3">
                    <Users /> Cuentas Compartidas
                  </h3>
                  <form
                    onSubmit={async (e) => {
                      e.preventDefault();
                      try {
                        const emails = e.target.emails.value
                          .split(",")
                          .map((m) => m.trim());
                        const { data } = await clienteAxios.post(
                          "/cuentas-compartidas",
                          {
                            nombre: e.target.nombre.value,
                            participantes: emails,
                          },
                        );
                        setMensaje({ tipo: "exito", texto: data.mensaje });
                        cargarDatosBase();
                      } catch (err) {
                        setMensaje({
                          tipo: "error",
                          texto: err.response?.data?.mensaje || "Error",
                        });
                      }
                    }}
                    className="bg-black/20 p-6 rounded-xl border border-white/10 flex flex-col gap-4 mb-8"
                  >
                    <div>
                      <label className="text-slate-400 block mb-2">
                        Nombre de la cuenta (Ej. Viaje a Japón)
                      </label>
                      <input
                        type="text"
                        name="nombre"
                        placeholder="Nombre"
                        className="w-full bg-slate-900 border border-white/20 rounded-lg px-4 py-2 text-white"
                        required
                      />
                    </div>
                    <div>
                      <label className="text-slate-400 block mb-2">
                        Participantes (Correos separados por coma)
                      </label>
                      <input
                        type="text"
                        name="emails"
                        placeholder="amigo@correo.com, otro@correo.com"
                        className="w-full bg-slate-900 border border-white/20 rounded-lg px-4 py-2 text-white"
                        required
                      />
                    </div>
                    <button
                      type="submit"
                      className="bg-gradient-to-r from-cyan-500 to-blue-500 hover:from-cyan-400 hover:to-blue-400 py-2 rounded-lg font-bold text-white transition-colors"
                    >
                      Crear Cuenta Compartida
                    </button>
                  </form>

                  <h4 className="text-lg font-bold text-white mb-4">
                    Mis Cuentas Compartidas
                  </h4>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {cuentasCompartidas.map((cc) => (
                      <div
                        key={cc._id}
                        className="bg-black/30 border border-white/20 p-4 rounded-xl"
                      >
                        <div className="flex justify-between items-center mb-2">
                          <span className="font-bold text-white">
                            {cc.nombre}
                          </span>
                          <span className="text-lg font-bold text-cyan-400">
                            ${cc.balance?.toFixed(2) || "0.00"}
                          </span>
                        </div>
                        <p className="text-slate-400 text-sm mb-2">
                          {cc.participantes?.length || 0} Participantes
                        </p>
                        <button
                          onClick={async () => {
                            const amt = prompt(
                              "Monto a depositar en la cuenta compartida:",
                            );
                            if (amt) {
                              try {
                                const { data } = await clienteAxios.post(
                                  `/cuentas-compartidas/${cc._id}/depositar`,
                                  { monto: Number(amt) },
                                );
                                setMensaje({
                                  tipo: "exito",
                                  texto: data.mensaje,
                                });
                                cargarDatosBase();
                              } catch (err) {
                                setMensaje({
                                  tipo: "error",
                                  texto: err.response?.data?.mensaje || "Error",
                                });
                              }
                            }
                          }}
                          className="w-full bg-white/10 hover:bg-white/20 py-1.5 rounded font-bold text-white transition-colors text-sm"
                        >
                          Aportar Fondos
                        </button>
                      </div>
                    ))}
                    {cuentasCompartidas.length === 0 && (
                      <p className="text-slate-500 text-sm">
                        No perteneces a ninguna cuenta compartida.
                      </p>
                    )}
                  </div>
                </div>
              )}

              {/* VISTA: RECOMPENSAS */}
              {opcionActiva === "recompensas" && (
                <div className="animate-in fade-in slide-in-from-bottom-4 duration-500 bg-white/5 border border-white/10 rounded-2xl p-8 shadow-[0_8px_30px_rgb(0,0,0,0.12)]">
                  <h3 className="text-2xl font-bold text-cyan-300 mb-6 flex items-center gap-3">
                    <Gift /> Recompensas & Cashback
                  </h3>
                  <div className="text-center mb-10 bg-black/30 border border-white/10 rounded-xl p-8">
                    <p className="text-slate-400 mb-2">Total Cashback Ganado</p>
                    <h2 className="text-5xl font-black bg-clip-text text-transparent bg-gradient-to-r from-teal-400 to-emerald-300">
                      ${recompensasInfo.cashbackTotal?.toFixed(2) || "0.00"}
                    </h2>
                  </div>
                  <h4 className="text-lg font-bold text-white mb-4">
                    Historial de Recompensas
                  </h4>
                  <div className="overflow-x-auto">
                    <table className="w-full text-left border-collapse">
                      <thead>
                        <tr className="bg-slate-900/50 text-slate-400 text-sm uppercase tracking-wider">
                          <th className="p-4 font-semibold">Fecha</th>
                          <th className="p-4 font-semibold">Descripción</th>
                          <th className="p-4 font-semibold text-right">
                            Monto
                          </th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-700/50">
                        {recompensasInfo.historial?.length === 0 ? (
                          <tr>
                            <td
                              colSpan="3"
                              className="p-8 text-center text-slate-500"
                            >
                              No hay recompensas recientes.
                            </td>
                          </tr>
                        ) : (
                          recompensasInfo.historial?.map((rec, index) => (
                            <tr
                              key={index}
                              className="hover:bg-white/5 transition-colors"
                            >
                              <td className="p-4 text-slate-300 text-sm">
                                {new Date(rec.fecha).toLocaleDateString()}
                              </td>
                              <td className="p-4 text-white font-medium">
                                {rec.descripcion}
                              </td>
                              <td className="p-4 text-right font-black text-emerald-400">
                                +${rec.monto.toFixed(2)}
                              </td>
                            </tr>
                          ))
                        )}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}

              {/* VISTA: SPLIT BILL */}
              {opcionActiva === "split-bill" && (
                <div className="animate-in fade-in slide-in-from-bottom-4 duration-500 bg-white/5 border border-white/10 rounded-2xl p-8 shadow-[0_8px_30px_rgb(0,0,0,0.12)]">
                  <h3 className="text-2xl font-bold text-cyan-300 mb-6 flex items-center gap-3">
                    <PieChartIcon /> Dividir Cuenta
                  </h3>

                  <form
                    onSubmit={crearSplitBill}
                    className="bg-black/20 p-6 rounded-xl border border-white/10 flex flex-col gap-4 mb-8"
                  >
                    <h4 className="text-lg font-bold text-white mb-2">
                      Nuevo Split
                    </h4>
                    <div>
                      <label className="text-slate-400 block mb-2">
                        Descripción (Ej. Cena Pizza)
                      </label>
                      <input
                        type="text"
                        value={splitForm.descripcion}
                        onChange={(e) =>
                          setSplitForm({
                            ...splitForm,
                            descripcion: e.target.value,
                          })
                        }
                        className="w-full bg-slate-900 border border-white/20 rounded-lg px-4 py-2 text-white focus:outline-none focus:border-cyan-400"
                        required
                      />
                    </div>
                    <div className="flex gap-4">
                      <div className="flex-1">
                        <label className="text-slate-400 block mb-2">
                          Monto Total
                        </label>
                        <input
                          type="number"
                          step="0.01"
                          value={splitForm.montoTotal}
                          onChange={(e) =>
                            setSplitForm({
                              ...splitForm,
                              montoTotal: e.target.value,
                            })
                          }
                          className="w-full bg-slate-900 border border-white/20 rounded-lg px-4 py-2 text-white focus:outline-none focus:border-cyan-400"
                          required
                        />
                      </div>
                      <div className="flex-1">
                        <label className="text-slate-400 block mb-2">
                          Participantes (emails separados por coma)
                        </label>
                        <input
                          type="text"
                          value={splitForm.participantes}
                          onChange={(e) =>
                            setSplitForm({
                              ...splitForm,
                              participantes: e.target.value,
                            })
                          }
                          className="w-full bg-slate-900 border border-white/20 rounded-lg px-4 py-2 text-white focus:outline-none focus:border-cyan-400"
                          required
                        />
                      </div>
                    </div>
                    <button
                      type="submit"
                      className="mt-2 bg-gradient-to-r from-cyan-500 to-blue-500 hover:from-cyan-400 hover:to-blue-400 py-2 rounded-lg font-bold text-white transition-colors"
                    >
                      Crear Split Bill
                    </button>
                  </form>

                  <h4 className="text-lg font-bold text-white mb-4">
                    Mis Splits Activos
                  </h4>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {misSplits.map((split) => (
                      <div
                        key={split._id}
                        className="bg-black/30 border border-white/20 p-4 rounded-xl"
                      >
                        <div className="flex justify-between items-center mb-2">
                          <span className="font-bold text-white">
                            {split.descripcion}
                          </span>
                          <span className="text-sm bg-cyan-500/20 text-cyan-400 px-2 py-1 rounded">
                            {split.estado}
                          </span>
                        </div>
                        <p className="text-slate-400 text-sm mb-1">
                          Monto Total: ${split.montoTotal.toFixed(2)}
                        </p>

                        {split.creador === auth._id ? (
                          <p className="text-emerald-400 text-sm mt-2 font-bold">
                            Creado por ti
                          </p>
                        ) : (
                          <div className="mt-4 border-t border-white/10 pt-4">
                            <p className="text-slate-400 text-sm mb-2">
                              Tu parte:{" "}
                              <span className="font-bold text-white">
                                $
                                {(
                                  split.montoTotal /
                                  (split.participantes.length + 1)
                                ).toFixed(2)}
                              </span>
                            </p>
                            {split.participantes.find(
                              (p) => p.usuario === auth._id,
                            )?.estado === "Pendiente" && (
                              <button
                                onClick={() => pagarSplit(split._id)}
                                className="w-full bg-emerald-600 hover:bg-emerald-500 py-1.5 rounded font-bold text-white transition-colors text-sm"
                              >
                                Pagar Mi Parte
                              </button>
                            )}
                            {split.participantes.find(
                              (p) => p.usuario === auth._id,
                            )?.estado === "Pagado" && (
                              <p className="text-emerald-400 text-sm font-bold">
                                ¡Ya pagaste tu parte!
                              </p>
                            )}
                          </div>
                        )}
                      </div>
                    ))}
                    {misSplits.length === 0 && (
                      <p className="text-slate-500 text-sm">
                        No tienes splits activos.
                      </p>
                    )}
                  </div>
                </div>
              )}

              {/* VISTA: CHAT P2P */}
              {opcionActiva === "chat-p2p" && (
                <div className="animate-in fade-in slide-in-from-bottom-4 duration-500 bg-white/5 border border-white/10 rounded-2xl shadow-[0_8px_30px_rgb(0,0,0,0.12)] h-[600px] flex overflow-hidden">
                  {/* Sidebar Contactos Chat */}
                  <div className="w-1/3 bg-black/40 border-r border-white/10 flex flex-col">
                    <div className="p-4 border-b border-white/10">
                      <h3 className="text-xl font-bold text-cyan-300 flex items-center gap-2">
                        <MessageSquare size={20} /> Chat P2P
                      </h3>
                    </div>
                    <div className="flex-1 overflow-y-auto custom-scrollbar p-2">
                      {chatContactos.map((contacto) => (
                        <button
                          key={contacto._id}
                          onClick={() => cargarMensajes(contacto._id)}
                          className={`w-full text-left p-3 rounded-lg flex items-center gap-3 transition-colors mb-1 ${chatUsuarioActivo === contacto._id ? "bg-white/10" : "hover:bg-white/5"}`}
                        >
                          <div className="w-10 h-10 rounded-full bg-gradient-to-r from-cyan-500 to-blue-500 flex items-center justify-center font-bold text-white uppercase">
                            {contacto.nombre.charAt(0)}
                          </div>
                          <div>
                            <p className="font-bold text-white text-sm">
                              {contacto.nombre}
                            </p>
                            <p className="text-xs text-slate-400 truncate">
                              {contacto.email}
                            </p>
                          </div>
                        </button>
                      ))}
                      {chatContactos.length === 0 && (
                        <p className="text-slate-500 text-sm text-center mt-10">
                          No hay contactos con chat.
                        </p>
                      )}
                    </div>
                  </div>

                  {/* Ventana de Chat */}
                  <div className="w-2/3 flex flex-col bg-[#0a0a1a]/40">
                    {chatUsuarioActivo ? (
                      <>
                        <div className="p-4 border-b border-white/10 bg-black/20 flex items-center gap-3">
                          <div className="w-10 h-10 rounded-full bg-gradient-to-r from-cyan-500 to-blue-500 flex items-center justify-center font-bold text-white uppercase">
                            {chatContactos
                              .find((c) => c._id === chatUsuarioActivo)
                              ?.nombre.charAt(0)}
                          </div>
                          <div>
                            <p className="font-bold text-white">
                              {
                                chatContactos.find(
                                  (c) => c._id === chatUsuarioActivo,
                                )?.nombre
                              }
                            </p>
                            <p className="text-xs text-emerald-400">
                              Encriptado de extremo a extremo
                            </p>
                          </div>
                        </div>

                        <div className="flex-1 overflow-y-auto p-4 flex flex-col gap-3 custom-scrollbar">
                          {chatMensajes.map((msg) => {
                            const esMio = msg.remitente === auth._id;
                            return (
                              <div
                                key={msg._id}
                                className={`max-w-[70%] rounded-xl p-3 ${esMio ? "bg-cyan-600 text-white self-end rounded-tr-none" : "bg-white/10 text-white self-start rounded-tl-none"}`}
                              >
                                <p className="text-sm">{msg.texto}</p>
                                <p className="text-[10px] text-white/50 mt-1 text-right">
                                  {new Date(msg.createdAt).toLocaleTimeString(
                                    [],
                                    { hour: "2-digit", minute: "2-digit" },
                                  )}
                                </p>
                              </div>
                            );
                          })}
                          {chatMensajes.length === 0 && (
                            <div className="h-full flex flex-col items-center justify-center text-slate-500">
                              <MessageSquare
                                size={40}
                                className="mb-2 opacity-50"
                              />
                              <p>Inicia la conversación</p>
                            </div>
                          )}
                        </div>

                        <form
                          onSubmit={enviarMensajeP2P}
                          className="p-4 bg-black/40 border-t border-white/10 flex gap-2"
                        >
                          <input
                            type="text"
                            value={nuevoMensaje}
                            onChange={(e) => setNuevoMensaje(e.target.value)}
                            placeholder="Escribe un mensaje..."
                            className="flex-1 bg-slate-900 border border-white/20 rounded-lg px-4 py-2 text-white focus:outline-none focus:border-cyan-400"
                          />
                          <button
                            type="submit"
                            className="bg-gradient-to-r from-cyan-500 to-blue-500 hover:from-cyan-400 hover:to-blue-400 p-2 px-4 rounded-lg text-white transition-colors"
                          >
                            <Send size={18} />
                          </button>
                        </form>
                      </>
                    ) : (
                      <div className="h-full flex flex-col items-center justify-center text-slate-500">
                        <MessageSquare
                          size={60}
                          className="mb-4 opacity-50 text-cyan-500"
                        />
                        <p className="text-lg">
                          Selecciona un contacto para chatear
                        </p>
                      </div>
                    )}
                  </div>
                </div>
              )}

              {/* VISTA: NEOBANCO PRO */}
              {opcionActiva === "suscripcion-pro" && (
                <div className="animate-in fade-in slide-in-from-bottom-4 duration-500 bg-white/5 border border-white/10 rounded-2xl p-8 shadow-[0_8px_30px_rgb(0,0,0,0.12)]">
                  <h3 className="text-2xl font-bold text-amber-400 mb-6 flex items-center gap-3">
                    <Star className="text-amber-400" /> NeoBanco Pro
                  </h3>

                  {auth.esPremium ? (
                    <div className="bg-gradient-to-r from-amber-500/20 to-yellow-600/20 border border-amber-500/50 rounded-xl p-8 text-center max-w-2xl mx-auto shadow-[0_0_30px_rgba(251,191,36,0.3)]">
                      <div className="w-20 h-20 bg-amber-500/20 rounded-full flex items-center justify-center mx-auto mb-4 text-amber-400">
                        <Star size={40} />
                      </div>
                      <h4 className="text-2xl font-bold text-white mb-2">
                        Ya eres miembro Pro
                      </h4>
                      <p className="text-amber-200/80 mb-6">
                        Disfruta de todos tus beneficios exclusivos:
                      </p>

                      <ul className="text-left space-y-4 text-slate-300 w-fit mx-auto">
                        <li className="flex items-center gap-3">
                          <span className="text-amber-400">✨</span> 0% de
                          comisiones en transferencias internacionales
                        </li>
                        <li className="flex items-center gap-3">
                          <span className="text-amber-400">✨</span> +5%
                          Cashback adicional en todas tus compras
                        </li>
                        <li className="flex items-center gap-3">
                          <span className="text-amber-400">✨</span> Tarjetas
                          metálicas exclusivas
                        </li>
                        <li className="flex items-center gap-3">
                          <span className="text-amber-400">✨</span> Atención
                          prioritaria 24/7
                        </li>
                      </ul>
                    </div>
                  ) : (
                    <div className="bg-black/40 border border-white/10 rounded-xl p-8 text-center max-w-2xl mx-auto">
                      <div className="w-20 h-20 bg-slate-800 rounded-full flex items-center justify-center mx-auto mb-4 text-slate-400">
                        <Star size={40} />
                      </div>
                      <h4 className="text-3xl font-black text-white mb-2">
                        Sube al siguiente nivel
                      </h4>
                      <div className="text-5xl font-black text-transparent bg-clip-text bg-gradient-to-r from-amber-400 to-yellow-600 my-6">
                        $9.99
                        <span className="text-xl text-slate-500">/mes</span>
                      </div>

                      <ul className="text-left space-y-4 text-slate-300 w-fit mx-auto mb-8">
                        <li className="flex items-center gap-3">
                          <span className="text-amber-400">✨</span> 0% de
                          comisiones en transferencias internacionales
                        </li>
                        <li className="flex items-center gap-3">
                          <span className="text-amber-400">✨</span> +5%
                          Cashback adicional en todas tus compras
                        </li>
                        <li className="flex items-center gap-3">
                          <span className="text-amber-400">✨</span> Tarjetas
                          metálicas exclusivas
                        </li>
                        <li className="flex items-center gap-3">
                          <span className="text-amber-400">✨</span> Atención
                          prioritaria 24/7
                        </li>
                      </ul>

                      <button
                        onClick={suscribirsePro}
                        className="w-full sm:w-auto bg-gradient-to-r from-amber-500 to-orange-500 hover:from-amber-400 hover:to-orange-400 text-white font-bold py-4 px-12 rounded-xl shadow-[0_0_20px_rgba(245,158,11,0.5)] transition-all text-lg"
                      >
                        Mejorar a Pro Ahora
                      </button>
                    </div>
                  )}
                </div>
              )}

              {/* VISTA: LOGROS (Gamificación) */}
              {opcionActiva === "logros" && (
                <div className="animate-in fade-in slide-in-from-bottom-4 duration-500 bg-white/5 border border-white/10 rounded-2xl p-8 shadow-[0_8px_30px_rgb(0,0,0,0.12)]">
                  <h3 className="text-2xl font-bold text-cyan-300 mb-6 flex items-center gap-3">
                    <svg
                      xmlns="http://www.w3.org/2000/svg"
                      width="28"
                      height="28"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="2"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                    >
                      <path d="M6 9H4.5a2.5 2.5 0 0 1 0-5H6" />
                      <path d="M18 9h1.5a2.5 2.5 0 0 0 0-5H18" />
                      <path d="M4 22h16" />
                      <path d="M10 14.66V17c0 .55-.47.98-.97 1.21C7.85 18.75 7 20.24 7 22" />
                      <path d="M14 14.66V17c0 .55.47.98.97 1.21C16.15 18.75 17 20.24 17 22" />
                      <path d="M18 2H6v7a6 6 0 0 0 12 0V2Z" />
                    </svg>{" "}
                    Mis Trofeos y Logros
                  </h3>
                  <p className="text-slate-400 mb-8">
                    Completa misiones financieras para desbloquear insignias y
                    demostrar que eres un maestro de Neo Banco.
                  </p>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    {logros.map((logro) => (
                      <div
                        key={logro.id}
                        className={`p-6 rounded-xl border flex gap-4 transition-all duration-500 ${logro.desbloqueado ? "bg-gradient-to-r from-cyan-900/40 to-blue-900/40 border-cyan-500/50 shadow-[0_0_15px_rgba(6,182,212,0.2)]" : "bg-black/40 border-white/5 opacity-70 grayscale"}`}
                      >
                        <div
                          className={`w-16 h-16 rounded-full flex items-center justify-center text-3xl flex-shrink-0 ${logro.desbloqueado ? "bg-cyan-500/20 text-cyan-400" : "bg-slate-800 text-slate-500"}`}
                        >
                          🏆
                        </div>
                        <div className="flex-1">
                          <h4
                            className={`text-lg font-bold ${logro.desbloqueado ? "text-white" : "text-slate-400"}`}
                          >
                            {logro.titulo}
                          </h4>
                          <p className="text-sm text-slate-500 mb-3">
                            {logro.descripcion}
                          </p>

                          <div className="w-full bg-slate-800 h-2 rounded-full overflow-hidden">
                            <div
                              className={`h-full transition-all duration-1000 ${logro.desbloqueado ? "bg-cyan-400" : "bg-slate-600"}`}
                              style={{ width: `${logro.porcentaje}%` }}
                            ></div>
                          </div>
                          <p className="text-xs text-right mt-1 font-mono text-slate-400">
                            {logro.progreso} / {logro.requerido}
                          </p>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      </main>

      <AIChat />
    </div>
  );
};

export default Dashboard;
