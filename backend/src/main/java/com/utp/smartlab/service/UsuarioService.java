package com.utp.smartlab.service;

import com.utp.smartlab.dto.JwtResponseDTO;
import com.utp.smartlab.dto.LoginRequestDTO;
import com.utp.smartlab.dto.UsuarioOpcionDTO;
import com.utp.smartlab.dto.UsuarioRequestDTO;
import com.utp.smartlab.dto.UsuarioResponseDTO;
import com.utp.smartlab.dto.UsuarioUpdateDTO;
import com.utp.smartlab.entity.Rol;
import com.utp.smartlab.entity.Usuario;
import com.utp.smartlab.exception.CredencialesInvalidasException;
import com.utp.smartlab.exception.EmailDuplicadoException;
import com.utp.smartlab.exception.OperacionNoPermitidaException;
import com.utp.smartlab.exception.RecursoNoEncontradoException;
import com.utp.smartlab.repository.AuditoriaRepository;
import com.utp.smartlab.repository.RolRepository;
import com.utp.smartlab.repository.UsuarioRepository;
import com.utp.smartlab.security.JwtUtil;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuditoriaRepository auditoriaRepository;

    public UsuarioService(UsuarioRepository usuarioRepository,
                           RolRepository rolRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil,
                           AuditoriaRepository auditoriaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.auditoriaRepository = auditoriaRepository;
    }

    @Transactional
    public UsuarioResponseDTO registrar(UsuarioRequestDTO dto) {
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new EmailDuplicadoException("Ya existe un usuario registrado con el email " + dto.getEmail());
        }

        Rol rol = rolRepository.findByNombre(dto.getRol())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el rol " + dto.getRol()));

        Usuario usuario = Usuario.builder()
                .nombre(dto.getNombre())
                .email(dto.getEmail())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .rol(rol)
                .build();

        Usuario guardado = usuarioRepository.save(usuario);
        return mapearAResponseDTO(guardado);
    }

    public List<UsuarioResponseDTO> listar() {
        return usuarioRepository.findAll().stream()
                .map(this::mapearAResponseDTO)
                .toList();
    }

    // Solo id y nombre, ordenados por nombre: alimenta el filtro "por usuario" de las reservas
    public List<UsuarioOpcionDTO> listarOpciones() {
        return usuarioRepository.findAll(Sort.by("nombre")).stream()
                .map(u -> new UsuarioOpcionDTO(u.getId(), u.getNombre()))
                .toList();
    }

    public UsuarioResponseDTO obtenerPorId(Long id) {
        return mapearAResponseDTO(buscarEntidadPorId(id));
    }

    @Transactional
    public UsuarioResponseDTO actualizar(Long id, UsuarioUpdateDTO dto, String emailSolicitante) {
        Usuario usuario = buscarEntidadPorId(id);

        if (!usuario.getEmail().equals(dto.getEmail()) && usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new EmailDuplicadoException("Ya existe un usuario registrado con el email " + dto.getEmail());
        }

        Rol nuevoRol = rolRepository.findByNombre(dto.getRol())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el rol " + dto.getRol()));

        boolean esElMismoUsuario = usuario.getEmail().equals(emailSolicitante);
        // Regla de negocio: un administrador no cambia su propio rol
        if (esElMismoUsuario && usuario.getRol().getNombre() != dto.getRol()) {
            throw new OperacionNoPermitidaException("No puedes cambiar tu propio rol; pídele a otro administrador que lo haga");
        }

        usuario.setNombre(dto.getNombre());
        usuario.setEmail(dto.getEmail());
        usuario.setRol(nuevoRol);
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        }

        return mapearAResponseDTO(usuarioRepository.save(usuario));
    }

    @Transactional
    public void eliminar(Long id, String emailSolicitante) {
        Usuario usuario = buscarEntidadPorId(id);

        if (usuario.getEmail().equals(emailSolicitante)) {
            throw new OperacionNoPermitidaException("No puedes eliminar tu propio usuario");
        }
        // Regla de negocio: no se borra a quien ya tiene auditoría
        if (auditoriaRepository.contarPorUsuarioId(id) > 0) {
            throw new OperacionNoPermitidaException(
                    "No se puede eliminar a '" + usuario.getNombre()
                            + "': tiene acciones registradas en la bitácora de auditoría, que se conservan por trazabilidad");
        }

        usuarioRepository.delete(usuario);
    }

    public JwtResponseDTO login(LoginRequestDTO dto) {
        Usuario usuario = usuarioRepository.buscarPorEmail(dto.getEmail())
                .orElseThrow(() -> new CredencialesInvalidasException("Email o contraseña incorrectos"));

        if (!passwordEncoder.matches(dto.getPassword(), usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException("Email o contraseña incorrectos");
        }

        String token = jwtUtil.generarToken(usuario.getEmail(), usuario.getRol().getNombre().name());

        return JwtResponseDTO.builder()
                .token(token)
                .tipo("Bearer")
                .usuarioId(usuario.getId())
                .nombre(usuario.getNombre())
                .email(usuario.getEmail())
                .rol(usuario.getRol().getNombre())
                .build();
    }

    private Usuario buscarEntidadPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el usuario con id " + id));
    }

    private UsuarioResponseDTO mapearAResponseDTO(Usuario usuario) {
        return UsuarioResponseDTO.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .email(usuario.getEmail())
                .rol(usuario.getRol().getNombre())
                .build();
    }
}
