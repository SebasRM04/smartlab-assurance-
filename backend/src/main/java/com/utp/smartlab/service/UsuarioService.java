package com.utp.smartlab.service;

import com.utp.smartlab.dto.UsuarioRequestDTO;
import com.utp.smartlab.dto.UsuarioResponseDTO;
import com.utp.smartlab.dto.UsuarioUpdateDTO;
import com.utp.smartlab.entity.Rol;
import com.utp.smartlab.entity.Usuario;
import com.utp.smartlab.exception.EmailDuplicadoException;
import com.utp.smartlab.exception.RecursoNoEncontradoException;
import com.utp.smartlab.repository.RolRepository;
import com.utp.smartlab.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditoriaService auditoriaService;

    public List<UsuarioResponseDTO> listarUsuarios() {
        return usuarioRepository.findAll().stream().map(this::mapearADTO).collect(Collectors.toList());
    }

    public UsuarioResponseDTO obtenerPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con ID: " + id));
        return mapearADTO(usuario);
    }

    public UsuarioResponseDTO crearUsuario(UsuarioRequestDTO dto) {
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new EmailDuplicadoException("El email ya está registrado: " + dto.getEmail());
        }
        Rol rol = rolRepository.findById(dto.getRolId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Rol no encontrado con ID: " + dto.getRolId()));

        Usuario usuario = new Usuario();
        usuario.setNombre(dto.getNombre());
        usuario.setApellido(dto.getApellido());
        usuario.setEmail(dto.getEmail());
        usuario.setPassword(passwordEncoder.encode(dto.getPassword()));
        usuario.setRol(rol);
        usuario.setActivo(true);

        Usuario guardado = usuarioRepository.save(usuario);
        auditoriaService.registrarAccion("CREAR_USUARIO", "Se creó el usuario: " + guardado.getEmail());
        return mapearADTO(guardado);
    }

    public UsuarioResponseDTO actualizarUsuario(Long id, UsuarioUpdateDTO dto) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con ID: " + id));

        Rol rol = rolRepository.findById(dto.getRolId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Rol no encontrado con ID: " + dto.getRolId()));

        usuario.setNombre(dto.getNombre());
        usuario.setApellido(dto.getApellido());
        usuario.setEmail(dto.getEmail());
        usuario.setRol(rol);

        Usuario actualizado = usuarioRepository.save(usuario);
        auditoriaService.registrarAccion("ACTUALIZAR_USUARIO", "Se actualizó el usuario ID: " + id);
        return mapearADTO(actualizado);
    }

    public void eliminarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con ID: " + id));
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
        auditoriaService.registrarAccion("ELIMINAR_USUARIO", "Se desactivó el usuario ID: " + id);
    }

    private UsuarioResponseDTO mapearADTO(Usuario u) {
        UsuarioResponseDTO dto = new UsuarioResponseDTO();
        dto.setId(u.getId());
        dto.setNombre(u.getNombre());
        dto.setApellido(u.getApellido());
        dto.setEmail(u.getEmail());
        dto.setRol(u.getRol() != null ? u.getRol().getNombre().name() : null);
        dto.setActivo(u.getActivo());
        return dto;
    }
}