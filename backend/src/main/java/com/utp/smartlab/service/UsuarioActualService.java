package com.utp.smartlab.service;

import com.utp.smartlab.entity.NombreRol;
import com.utp.smartlab.entity.Usuario;
import com.utp.smartlab.exception.RecursoNoEncontradoException;
import com.utp.smartlab.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UsuarioActualService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioActualService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario obtener() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new RecursoNoEncontradoException("No hay un usuario autenticado en este contexto");
        }

        return usuarioRepository.buscarPorEmail(auth.getName())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el usuario autenticado con email " + auth.getName()));
    }

    public boolean esEstudiante() {
        return obtener().getRol().getNombre() == NombreRol.ESTUDIANTE;
    }
}
