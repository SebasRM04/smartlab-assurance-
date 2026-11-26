package com.utp.smartlab.service;

import com.utp.smartlab.dto.JwtResponseDTO;
import com.utp.smartlab.dto.LoginRequestDTO;
import com.utp.smartlab.dto.UsuarioOpcionDTO;
import com.utp.smartlab.dto.UsuarioRequestDTO;
import com.utp.smartlab.dto.UsuarioResponseDTO;
import com.utp.smartlab.dto.UsuarioUpdateDTO;
import com.utp.smartlab.entity.NombreRol;
import com.utp.smartlab.entity.Rol;
import com.utp.smartlab.entity.Usuario;
import com.utp.smartlab.exception.CredencialesInvalidasException;
import com.utp.smartlab.exception.EmailDuplicadoException;
import com.utp.smartlab.exception.OperacionNoPermitidaException;
import com.utp.smartlab.exception.RecursoNoEncontradoException;
import com.utp.smartlab.repository.AuditoriaRepository;
import com.utp.smartlab.repository.HistorialEstadoEquipoRepository;
import com.utp.smartlab.repository.IncidenciaRepository;
import com.utp.smartlab.repository.MantenimientoRepository;
import com.utp.smartlab.repository.ReservaRepository;
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
    private final ReservaRepository reservaRepository;
    private final MantenimientoRepository mantenimientoRepository;
    private final IncidenciaRepository incidenciaRepository;
    private final HistorialEstadoEquipoRepository historialRepository;

    public UsuarioService(UsuarioRepository usuarioRepository,
                           RolRepository rolRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil,
                           AuditoriaRepository auditoriaRepository,
                           MantenimientoRepository mantenimientoRepository,
                           ReservaRepository reservaRepository,
                           IncidenciaRepository incidenciaRepository, 
                           HistorialEstadoEquipoRepository historialRepository) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.auditoriaRepository = auditoriaRepository;
        this.mantenimientoRepository = mantenimientoRepository;
        this.reservaRepository = reservaRepository;
        this.incidenciaRepository = incidenciaRepository;
        this.historialRepository = historialRepository;
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
        if (reservaRepository.existsByUsuarioId(id)) {
            throw new OperacionNoPermitidaException(
                    "No se puede eliminar a '" + usuario.getNombre() + "': tiene reservas");
        }
        if (mantenimientoRepository.existsByUsuarioId(id)) {
            throw new OperacionNoPermitidaException(
                    "No se puede eliminar a '" + usuario.getNombre() + "': tiene mantenimientos");
        }
        if (incidenciaRepository.existsByUsuarioId(id)) {
            throw new OperacionNoPermitidaException(
                    "No se puede eliminar a '" + usuario.getNombre() + "': tiene incidencias");
        }

        historialRepository.desvincularUsuario(id);
        auditoriaRepository.eliminarPorUsuarioId(id);
        usuarioRepository.deleteById(id);
    }

    public JwtResponseDTO login(LoginRequestDTO dto) {
        Usuario usuario = usuarioRepository.buscarPorEmail(dto.getEmail())
                .orElseThrow(() -> new CredencialesInvalidasException("Email o contraseña incorrectos"));

        if (!passwordEncoder.matches(dto.getPassword(), usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException("Email o contraseña incorrectos");
        }
        
        if (!usuario.isActivo()) {
        throw new CredencialesInvalidasException(
                "La cuenta está desactivada. Pide a un administrador que la active");
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
                .activo(usuario.isActivo())
                .build();
    }

    @Transactional
    public UsuarioResponseDTO cambiarActivo(Long id, boolean activo, String emailSolicitante) {
        Usuario usuario = buscarEntidadPorId(id);

        if (!activo && usuario.getEmail().equals(emailSolicitante)) {
            throw new OperacionNoPermitidaException("No puedes desactivar tu propio usuario");
        }
        if (usuario.isActivo() == activo) {
            throw new OperacionNoPermitidaException(activo
                    ? "El usuario ya está activo"
                    : "El usuario ya está desactivado");
        }
        if (!activo
                && usuario.getRol().getNombre() == NombreRol.ADMINISTRADOR
                && usuarioRepository.contarAdministradoresActivos() <= 1) {
            throw new OperacionNoPermitidaException("No se puede desactivar al último administrador activo");
        }

        usuario.setActivo(activo);
        return mapearAResponseDTO(usuarioRepository.save(usuario));
    }
}
