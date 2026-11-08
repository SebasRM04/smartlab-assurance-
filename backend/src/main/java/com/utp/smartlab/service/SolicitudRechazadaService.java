package com.utp.smartlab.service;

import com.utp.smartlab.entity.Equipo;
import com.utp.smartlab.entity.SolicitudRechazada;
import com.utp.smartlab.exception.RecursoNoEncontradoException;
import com.utp.smartlab.repository.EquipoRepository;
import com.utp.smartlab.repository.SolicitudRechazadaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SolicitudRechazadaService {

    private final SolicitudRechazadaRepository solicitudRechazadaRepository;
    private final EquipoRepository equipoRepository;

    public SolicitudRechazadaService(SolicitudRechazadaRepository solicitudRechazadaRepository,
                                     EquipoRepository equipoRepository) {
        this.solicitudRechazadaRepository = solicitudRechazadaRepository;
        this.equipoRepository = equipoRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarIntentoRechazado(Long equipoId, LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        Equipo equipo = equipoRepository.findById(equipoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el equipo con id " + equipoId));

        solicitudRechazadaRepository.save(SolicitudRechazada.builder()
                .equipo(equipo)
                .fechaInicioSolicitada(fechaInicio)
                .fechaFinSolicitada(fechaFin)
                .fechaIntento(LocalDateTime.now())
                .build());
    }

    @Transactional(readOnly = true)
    public long contar() {
        return solicitudRechazadaRepository.count();
    }
}
