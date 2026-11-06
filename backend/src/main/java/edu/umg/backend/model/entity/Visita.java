package edu.umg.backend.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "visita")
public class Visita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "visitante_id", nullable = false, length = 36)
    private String visitanteId;

    @Column(nullable = false)
    private Instant fecha;

    public Long getId() {
        return id;
    }

    public String getVisitanteId() {
        return visitanteId;
    }

    public void setVisitanteId(String visitanteId) {
        this.visitanteId = visitanteId;
    }

    public Instant getFecha() {
        return fecha;
    }

    public void setFecha(Instant fecha) {
        this.fecha = fecha;
    }
}
