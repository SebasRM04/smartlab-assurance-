package com.utp.smartlab.service;

import com.utp.smartlab.dto.AuditoriaResponseDTO;
import com.utp.smartlab.entity.Auditoria;
import com.utp.smartlab.repository.AuditoriaRepository;
import com.utp.smartlab.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;

    public AuditoriaService(AuditoriaRepository auditoriaRepository, UsuarioRepository usuarioRepository) {
        this.auditoriaRepository = auditoriaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public void registrar(String accion, String entidadAfectada, String detalle) {
        String email = obtenerEmailAutenticado();
        if (email == null) {
            return;
        }
        registrarParaEmail(email, accion, entidadAfectada, detalle);
    }

    @Transactional
    public void registrarParaEmail(String email, String accion, String entidadAfectada, String detalle) {
        usuarioRepository.buscarPorEmail(email).ifPresent(usuario -> {
            Auditoria registro = Auditoria.builder()
                    .usuario(usuario)
                    .accion(accion)
                    .entidadAfectada(entidadAfectada)
                    .fechaHora(LocalDateTime.now())
                    .detalle(detalle)
                    .build();
            auditoriaRepository.save(registro);
        });
    }

    public List<AuditoriaResponseDTO> listar() {
        return auditoriaRepository.findAll().stream()
                .map(this::mapearAResponseDTO)
                .toList();
    }

    public List<AuditoriaResponseDTO> listarPorUsuario(Long usuarioId) {
        return auditoriaRepository.buscarPorUsuarioId(usuarioId).stream()
                .map(this::mapearAResponseDTO)
                .toList();
    }

    private String obtenerEmailAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return auth.getName();
    }

    private AuditoriaResponseDTO mapearAResponseDTO(Auditoria auditoria) {
        return AuditoriaResponseDTO.builder()
                .id(auditoria.getId())
                .usuarioId(auditoria.getUsuario().getId())
                .usuarioNombre(auditoria.getUsuario().getNombre())
                .accion(auditoria.getAccion())
                .entidadAfectada(auditoria.getEntidadAfectada())
                .fechaHora(auditoria.getFechaHora())
                .detalle(auditoria.getDetalle())
                .build();
    }
}
