package com.neobanco.backend.controller;

import com.neobanco.backend.model.Inversion;
import com.neobanco.backend.model.Transaccion;
import com.neobanco.backend.model.Usuario;
import com.neobanco.backend.repository.InversionRepository;
import com.neobanco.backend.repository.TransaccionRepository;
import com.neobanco.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/inversiones")
public class InversionController {

    @Autowired
    private InversionRepository inversionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TransaccionRepository transaccionRepository;

    private Double obtenerPrecioActual(String cryptoId) throws Exception {
        String url = "https://api.coingecko.com/api/v3/simple/price?ids=" + cryptoId + "&vs_currencies=usd";
        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        if (response != null && response.containsKey(cryptoId)) {
            Map<String, Object> data = (Map<String, Object>) response.get(cryptoId);
            Number price = (Number) data.get("usd");
            return price.doubleValue();
        }
        throw new Exception("No se pudo obtener el precio de la criptomoneda");
    }

    @GetMapping
    public ResponseEntity<?> obtenerPortafolio(@RequestAttribute("usuarioId") String usuarioId) {
        try {
            List<Inversion> inversiones = inversionRepository.findByUsuario(usuarioId);
            return ResponseEntity.ok(inversiones);
        } catch (Exception e) {
            Map<String, String> res = new HashMap<>();
            res.put("mensaje", "Error al obtener el portafolio");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(res);
        }
    }

    @PostMapping("/comprar")
    @Transactional
    public ResponseEntity<?> comprarCripto(@RequestAttribute("usuarioId") String usuarioId,
                                           @RequestBody Map<String, Object> body) {
        String criptomoneda = (String) body.get("criptomoneda");
        String simbolo = (String) body.get("simbolo");
        Number montoUsdNum = (Number) body.get("monto_usd");
        Double montoUsd = montoUsdNum != null ? montoUsdNum.doubleValue() : null;

        Map<String, String> errorRes = new HashMap<>();

        try {
            Optional<Usuario> optUsuario = usuarioRepository.findById(usuarioId);
            if (optUsuario.isEmpty()) throw new RuntimeException("Usuario no encontrado");
            Usuario usuario = optUsuario.get();

            if (usuario.getSaldo() < montoUsd) {
                errorRes.put("mensaje", "Saldo insuficiente para esta compra");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorRes);
            }

            Double precioActual = obtenerPrecioActual(criptomoneda);
            Double cantidadCripto = montoUsd / precioActual;

            usuario.setSaldo(usuario.getSaldo() - montoUsd);
            usuarioRepository.save(usuario);

            Optional<Inversion> optInversion = inversionRepository.findByUsuarioAndCriptomoneda(usuarioId, criptomoneda);
            if (optInversion.isPresent()) {
                Inversion inversion = optInversion.get();
                inversion.setCantidad(inversion.getCantidad() + cantidadCripto);
                inversion.setInvertidoUsd(inversion.getInvertidoUsd() + montoUsd);
                inversionRepository.save(inversion);
            } else {
                Inversion nuevaInversion = new Inversion();
                nuevaInversion.setUsuario(usuarioId);
                nuevaInversion.setCriptomoneda(criptomoneda);
                nuevaInversion.setSimbolo(simbolo);
                nuevaInversion.setCantidad(cantidadCripto);
                nuevaInversion.setInvertidoUsd(montoUsd);
                inversionRepository.save(nuevaInversion);
            }

            Transaccion tx = new Transaccion();
            tx.setUsuario(usuarioId);
            tx.setTipo("Inversión");
            tx.setMonto(montoUsd);
            tx.setCategoria("Criptomonedas");
            tx.setDescripcion(String.format("Compra de %.6f %s", cantidadCripto, simbolo));
            transaccionRepository.save(tx);

            Map<String, Object> res = new HashMap<>();
            res.put("mensaje", String.format("Compraste exitosamente %.6f %s", cantidadCripto, simbolo));
            res.put("saldo", usuario.getSaldo());
            return ResponseEntity.ok(res);

        } catch (Exception e) {
            errorRes.put("mensaje", e.getMessage() != null ? e.getMessage() : "Error al procesar la compra");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorRes);
        }
    }

    @PostMapping("/vender")
    @Transactional
    public ResponseEntity<?> venderCripto(@RequestAttribute("usuarioId") String usuarioId,
                                          @RequestBody Map<String, Object> body) {
        String criptomoneda = (String) body.get("criptomoneda");
        Number cantNum = (Number) body.get("cantidad_cripto");
        Double cantidadCripto = cantNum != null ? cantNum.doubleValue() : null;

        Map<String, String> errorRes = new HashMap<>();

        try {
            Optional<Inversion> optInversion = inversionRepository.findByUsuarioAndCriptomoneda(usuarioId, criptomoneda);
            if (optInversion.isEmpty() || optInversion.get().getCantidad() < cantidadCripto) {
                errorRes.put("mensaje", "No tienes suficiente cantidad de esta criptomoneda");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorRes);
            }
            Inversion inversion = optInversion.get();

            Double precioActual = obtenerPrecioActual(criptomoneda);
            Double montoUsd = cantidadCripto * precioActual;

            Optional<Usuario> optUsuario = usuarioRepository.findById(usuarioId);
            if (optUsuario.isEmpty()) throw new RuntimeException("Usuario no encontrado");
            Usuario usuario = optUsuario.get();

            usuario.setSaldo(usuario.getSaldo() + montoUsd);
            usuarioRepository.save(usuario);

            inversion.setCantidad(inversion.getCantidad() - cantidadCripto);
            Double proporcion = cantidadCripto / (inversion.getCantidad() + cantidadCripto);
            inversion.setInvertidoUsd(inversion.getInvertidoUsd() - (inversion.getInvertidoUsd() * proporcion));

            if (inversion.getCantidad() <= 0.00000001) {
                inversionRepository.delete(inversion);
            } else {
                inversionRepository.save(inversion);
            }

            Transaccion tx = new Transaccion();
            tx.setUsuario(usuarioId);
            tx.setTipo("Depósito");
            tx.setMonto(montoUsd);
            tx.setCategoria("Venta Cripto");
            tx.setDescripcion(String.format("Venta de %s %s", cantidadCripto, inversion.getSimbolo()));
            transaccionRepository.save(tx);

            Map<String, Object> res = new HashMap<>();
            res.put("mensaje", String.format("Vendiste exitosamente. Recibiste $%.2f USD", montoUsd));
            res.put("saldo", usuario.getSaldo());
            return ResponseEntity.ok(res);

        } catch (Exception e) {
            errorRes.put("mensaje", e.getMessage() != null ? e.getMessage() : "Error al procesar la venta");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorRes);
        }
    }
}
