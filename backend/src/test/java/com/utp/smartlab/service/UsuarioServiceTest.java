package com.utp.smartlab.service;

import com.utp.smartlab.dto.JwtResponseDTO;
import com.utp.smartlab.dto.LoginRequestDTO;
import com.utp.smartlab.dto.UsuarioRequestDTO;
import com.utp.smartlab.dto.UsuarioResponseDTO;
import com.utp.smartlab.dto.UsuarioUpdateDTO;
import com.utp.smartlab.entity.NombreRol;
import com.utp.smartlab.entity.Rol;
import com.utp.smartlab.entity.Usuario;
import com.utp.smartlab.exception.CredencialesInvalidasException;
import com.utp.smartlab.exception.EmailDuplicadoException;
import com.utp.smartlab.exception.OperacionNoPermitidaException;
import com.utp.smartlab.repository.AuditoriaRepository;
import com.utp.smartlab.repository.RolRepository;
import com.utp.smartlab.repository.UsuarioRepository;
import com.utp.smartlab.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuditoriaRepository auditoriaRepository;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    void registrar_guardaElHash_yNoLaContrasenaEnClaro() {
        Rol tecnico = Rol.builder().id(2L).nombre(NombreRol.TECNICO).build();
        when(usuarioRepository.existsByEmail("tec@smartlab.com")).thenReturn(false);
        when(rolRepository.findByNombre(NombreRol.TECNICO)).thenReturn(Optional.of(tecnico));
        when(passwordEncoder.encode("Demo123!")).thenReturn("hash-bcrypt");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario usuario = inv.getArgument(0);
            usuario.setId(8L);
            return usuario;
        });

        UsuarioResponseDTO resultado = usuarioService.registrar(
                new UsuarioRequestDTO("Tomás", "tec@smartlab.com", "Demo123!", NombreRol.TECNICO));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hash-bcrypt");
        assertThat(resultado.getId()).isEqualTo(8L);
        assertThat(resultado.getRol()).isEqualTo(NombreRol.TECNICO);
    }

    @Test
    void registrar_lanzaExcepcion_cuandoElEmailYaExiste() {
        when(usuarioRepository.existsByEmail("admin@smartlab.com")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.registrar(
                new UsuarioRequestDTO("Otro", "admin@smartlab.com", "Demo123!", NombreRol.DOCENTE)))
                .isInstanceOf(EmailDuplicadoException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void login_devuelveElToken_cuandoLaContrasenaCoincide() {
        Usuario usuario = usuario(4L, "ana@smartlab.com", NombreRol.ESTUDIANTE);
        when(usuarioRepository.buscarPorEmail("ana@smartlab.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Demo123!", "hash")).thenReturn(true);
        when(jwtUtil.generarToken("ana@smartlab.com", "ESTUDIANTE")).thenReturn("jwt-ana");

        JwtResponseDTO resultado = usuarioService.login(new LoginRequestDTO("ana@smartlab.com", "Demo123!"));

        assertThat(resultado.getToken()).isEqualTo("jwt-ana");
        assertThat(resultado.getTipo()).isEqualTo("Bearer");
        assertThat(resultado.getRol()).isEqualTo(NombreRol.ESTUDIANTE);
        assertThat(resultado.getUsuarioId()).isEqualTo(4L);
    }

    @Test
    void login_lanzaExcepcion_cuandoLaContrasenaNoCoincide() {
        Usuario usuario = usuario(4L, "ana@smartlab.com", NombreRol.ESTUDIANTE);
        when(usuarioRepository.buscarPorEmail("ana@smartlab.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("mala", "hash")).thenReturn(false);

        assertThatThrownBy(() -> usuarioService.login(new LoginRequestDTO("ana@smartlab.com", "mala")))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void actualizar_rechazaQueUnAdministradorSeCambieElRol() {
        Usuario admin = usuario(1L, "admin@smartlab.com", NombreRol.ADMINISTRADOR);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(rolRepository.findByNombre(NombreRol.DOCENTE))
                .thenReturn(Optional.of(Rol.builder().id(3L).nombre(NombreRol.DOCENTE).build()));

        assertThatThrownBy(() -> usuarioService.actualizar(
                1L,
                new UsuarioUpdateDTO("Admin", "admin@smartlab.com", NombreRol.DOCENTE, null),
                "admin@smartlab.com"))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("propio rol");
    }

    @Test
    void actualizar_conservaLaContrasena_cuandoNoSeEnviaPassword() {
        Usuario usuario = usuario(5L, "doc@smartlab.com", NombreRol.DOCENTE);
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(usuario));
        when(rolRepository.findByNombre(NombreRol.DOCENTE))
                .thenReturn(Optional.of(usuario.getRol()));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponseDTO resultado = usuarioService.actualizar(
                5L,
                new UsuarioUpdateDTO("Diana", "doc@smartlab.com", NombreRol.DOCENTE, null),
                "admin@smartlab.com");

        assertThat(resultado.getNombre()).isEqualTo("Diana");
        assertThat(usuario.getPasswordHash()).isEqualTo("hash");
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void eliminar_rechazaEliminarseASiMismo() {
        Usuario admin = usuario(1L, "admin@smartlab.com", NombreRol.ADMINISTRADOR);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> usuarioService.eliminar(1L, "admin@smartlab.com"))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("propio usuario");
        verify(usuarioRepository, never()).delete(any());
    }

    @Test
    void eliminar_rechaza_cuandoTieneAccionesEnLaBitacora() {
        Usuario usuario = usuario(5L, "doc@smartlab.com", NombreRol.DOCENTE);
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(usuario));
        when(auditoriaRepository.contarPorUsuarioId(5L)).thenReturn(2L);

        assertThatThrownBy(() -> usuarioService.eliminar(5L, "admin@smartlab.com"))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("bitácora");
        verify(usuarioRepository, never()).delete(any());
    }

    @Test
    void eliminar_borra_cuandoNoTieneAuditoria() {
        Usuario usuario = usuario(5L, "doc@smartlab.com", NombreRol.DOCENTE);
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(usuario));
        when(auditoriaRepository.contarPorUsuarioId(5L)).thenReturn(0L);

        usuarioService.eliminar(5L, "admin@smartlab.com");

        verify(usuarioRepository).delete(usuario);
    }

    private Usuario usuario(Long id, String email, NombreRol rol) {
        return Usuario.builder()
                .id(id)
                .nombre("Usuario")
                .email(email)
                .passwordHash("hash")
                .rol(Rol.builder().id(id).nombre(rol).build())
                .build();
    }
}
