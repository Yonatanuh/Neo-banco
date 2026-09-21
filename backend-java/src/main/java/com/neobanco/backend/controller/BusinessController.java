package com.neobanco.backend.controller;

import com.neobanco.backend.model.*;
import com.neobanco.backend.repository.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/business")
public class BusinessController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TransaccionRepository transaccionRepository;

    public static class PagoRequest {
        private String email;
        private Double monto;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public Double getMonto() { return monto; }
        public void setMonto(Double monto) { this.monto = monto; }
    }

    public static class NominaRequest {
        private List<PagoRequest> pagos;

        public List<PagoRequest> getPagos() { return pagos; }
        public void setPagos(List<PagoRequest> pagos) { this.pagos = pagos; }
    }



    @PostMapping("/nomina")
    @Transactional
    public ResponseEntity<?> procesarNomina(@RequestAttribute("usuarioId") String usuarioId, @RequestBody NominaRequest request) {
        List<PagoRequest> pagos = request.getPagos();

        if (pagos == null || pagos.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "Lista de pagos vacía o inválida"));
        }

        Usuario empleador = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Empleador no encontrado"));

        Double totalNomina = 0.0;
        List<Map<String, Object>> usuariosDestino = new ArrayList<>();

        for (PagoRequest pago : pagos) {
            String email = pago.getEmail();
            Double monto = pago.getMonto();

            if (email == null || monto == null || monto <= 0) {
                throw new RuntimeException("Datos inválidos para " + email);
            }

            Usuario empleado = usuarioRepository.findByEmail(email.toLowerCase())
                    .orElseThrow(() -> new RuntimeException("Empleado no encontrado: " + email));

            usuariosDestino.add(Map.of("empleado", empleado, "monto", monto));
            totalNomina += monto;
        }

        if (empleador.getSaldo() < totalNomina) {
            throw new RuntimeException("Saldo insuficiente. Necesitas $" + totalNomina + " para procesar la nómina.");
        }

        empleador.setSaldo(empleador.getSaldo() - totalNomina);
        usuarioRepository.save(empleador);

        List<Transaccion> transacciones = new ArrayList<>();

        for (Map<String, Object> destino : usuariosDestino) {
            Usuario empleado = (Usuario) destino.get("empleado");
            Double monto = (Double) destino.get("monto");

            empleado.setSaldo(empleado.getSaldo() + monto);
            usuarioRepository.save(empleado);

            Transaccion t = new Transaccion();
            t.setRemitenteId(empleador.getId());
            t.setDestinatarioId(empleado.getId());
            t.setMonto(monto);
            t.setTipo("Transferencia");
            t.setCategoria("Nómina");
            transacciones.add(t);
        }

        transaccionRepository.saveAll(transacciones);

        return ResponseEntity.ok(Map.of(
                "mensaje", "Nómina procesada exitosamente. Se pagaron $" + totalNomina + " a " + pagos.size() + " empleados."
        ));
    }
}

