package com.utp.smartlab.service;

import com.utp.smartlab.dto.AuditoriaResponseDTO;
import com.utp.smartlab.entity.Auditoria;
import com.utp.smartlab.entity.Usuario;
import com.utp.smartlab.repository.AuditoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuditoriaService {

    @Autowired
    private AuditoriaRepository auditoriaRepository;

    @Autowired
    private UsuarioActualService usuarioActualService;

    public void registrarAccion(String accion, String detalles) {
        try {
            Usuario usuario = usuarioActualService.obtenerUsuarioActual();
            Auditoria auditoria = new Auditoria();
            auditoria.setUsuario(usuario);
            auditoria.setAccion(accion);
            auditoria.setDetalles(detalles);
            auditoria.setFechaCreacion(LocalDateTime.now());
            auditoriaRepository.save(auditoria);
        } catch (Exception e) {
            // Fallback si no hay sesión activa
        }
    }

    public List<AuditoriaResponseDTO> listarAuditorias() {
        return auditoriaRepository.findAll().stream().map(a -> {
            AuditoriaResponseDTO dto = new AuditoriaResponseDTO();
            dto.setId(a.getId());
            dto.setUsuarioEmail(a.getUsuario() != null ? a.getUsuario().getEmail() : "Sistema");
            dto.setAccion(a.getAccion());
            dto.setDetalles(a.getDetalles());
            dto.setFecha(a.getFechaCreacion());
            return dto;
        }).collect(Collectors.toList());
    }
}