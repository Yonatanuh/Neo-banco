package com.neobanco.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "cuentas_compartidas")
public class CuentaCompartida {

    @Id
    private String id;
    private String creador;
    private String nombre;
    private List<String> miembros = new ArrayList<>();
    private Double limiteAprobacion = 500.0;
    private Double saldo = 0.0;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCreador() { return creador; }
    public void setCreador(String creador) { this.creador = creador; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public List<String> getMiembros() { return miembros; }
    public void setMiembros(List<String> miembros) { this.miembros = miembros; }

    public Double getLimiteAprobacion() { return limiteAprobacion; }
    public void setLimiteAprobacion(Double limiteAprobacion) { this.limiteAprobacion = limiteAprobacion; }

    public Double getSaldo() { return saldo; }
    public void setSaldo(Double saldo) { this.saldo = saldo; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
