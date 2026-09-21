package com.neobanco.backend.controller;

import com.neobanco.backend.model.Transaccion;
import com.neobanco.backend.model.Usuario;
import com.neobanco.backend.repository.TransaccionRepository;
import com.neobanco.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/reportes")
public class ReportesController {

    @Autowired
    private TransaccionRepository transaccionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/estado-cuenta")
    public ResponseEntity<byte[]> descargarEstadoCuenta(@RequestAttribute("usuarioId") String usuarioId) {
        try {
            Optional<Usuario> usuarioOpt = usuarioRepository.findById(usuarioId);
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            Usuario usuario = usuarioOpt.get();

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date fechaInicioMes = cal.getTime();

            List<Transaccion> transacciones = transaccionRepository.findTransaccionesUsuarioDesde(usuario.getId(), fechaInicioMes);
            transacciones.sort(Comparator.comparing(Transaccion::getFecha).reversed());

            int month = cal.get(Calendar.MONTH) + 1;
            String nombreFormatted = usuario.getNombre().replaceAll("\\s", "_");
            String filename = "EstadoCuenta_" + nombreFormatted + "_" + month + ".csv";

            StringBuilder csvBuilder = new StringBuilder();
            csvBuilder.append("Fecha,RemitenteId,DestinatarioId,Monto,Tipo,Categoria\n");
            for (Transaccion t : transacciones) {
                csvBuilder.append(t.getFecha()).append(",")
                          .append(t.getRemitenteId() != null ? t.getRemitenteId() : "").append(",")
                          .append(t.getDestinatarioId() != null ? t.getDestinatarioId() : "").append(",")
                          .append(t.getMonto()).append(",")
                          .append(t.getTipo() != null ? t.getTipo() : "").append(",")
                          .append(t.getCategoria() != null ? t.getCategoria() : "").append("\n");
            }

            byte[] fileBytes = csvBuilder.toString().getBytes(StandardCharsets.UTF_8);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", filename);

            return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
