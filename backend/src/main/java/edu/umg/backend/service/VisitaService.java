package edu.umg.backend.service;

import edu.umg.backend.model.entity.Visita;
import edu.umg.backend.model.repository.VisitaRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VisitaService {

    private static final ZoneId ZONA = ZoneId.of("America/Guatemala");

    private final VisitaRepository visitas;

    public VisitaService(VisitaRepository visitas) {
        this.visitas = visitas;
    }

    public void registrar(Map<String, Object> cuerpo) {
        String visitanteId = String.valueOf(cuerpo.get("visitanteId"));
        if (!visitanteId.matches("[0-9a-fA-F-]{36}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El identificador de visitante no es válido.");
        }
        Instant ahora = Instant.now();
        if (visitas.existsByVisitanteIdAndFechaAfter(visitanteId, ahora.minus(30, ChronoUnit.MINUTES))) {
            return;
        }
        Visita visita = new Visita();
        visita.setVisitanteId(visitanteId);
        visita.setFecha(ahora);
        visitas.save(visita);
    }

    public Map<String, Object> estadisticas(LocalDate desde, LocalDate hasta) {
        LocalDate fin = hasta != null ? hasta : LocalDate.now(ZONA);
        LocalDate inicio = desde != null ? desde : fin.minusDays(29);
        if (fin.isBefore(inicio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha final no puede ser anterior a la inicial.");
        }
        if (ChronoUnit.DAYS.between(inicio, fin) > 365) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El intervalo máximo es de un año.");
        }

        Instant desdeInstante = inicio.atStartOfDay(ZONA).toInstant();
        Instant hastaInstante = fin.plusDays(1).atStartOfDay(ZONA).toInstant();
        List<Visita> lista = visitas.findByFechaGreaterThanEqualAndFechaLessThan(desdeInstante, hastaInstante);

        Set<String> visitantes = new HashSet<>();
        Map<LocalDate, Long> porFecha = new TreeMap<>();
        for (Visita visita : lista) {
            visitantes.add(visita.getVisitanteId());
            porFecha.merge(visita.getFecha().atZone(ZONA).toLocalDate(), 1L, Long::sum);
        }

        List<Map<String, Object>> porDia = new ArrayList<>();
        for (LocalDate dia = inicio; !dia.isAfter(fin); dia = dia.plusDays(1)) {
            porDia.add(Map.of("fecha", dia.toString(), "visitas", porFecha.getOrDefault(dia, 0L)));
        }
        return Map.of(
                "desde", inicio.toString(),
                "hasta", fin.toString(),
                "visitasTotales", lista.size(),
                "visitantesUnicos", visitantes.size(),
                "porDia", porDia);
    }
}
