package com.utp.smartlab.service;

import com.utp.smartlab.entity.SolicitudRechazada;
import com.utp.smartlab.repository.SolicitudRechazadaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SolicitudRechazadaService {

    @Autowired
    private SolicitudRechazadaRepository solicitudRechazadaRepository;

    public void registrarRechazo(String tipo, String motivo, Long usuarioId) {
        SolicitudRechazada solicitud = new SolicitudRechazada();
        solicitud.setTipo(tipo);
        solicitud.setMotivo(motivo);
        solicitud.setUsuarioId(usuarioId);
        solicitudRechazadaRepository.save(solicitud);
    }
}