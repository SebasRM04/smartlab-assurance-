package com.utp.smartlab.config;

import com.utp.smartlab.entity.NombreRol;
import com.utp.smartlab.entity.Rol;
import com.utp.smartlab.entity.Usuario;
import com.utp.smartlab.repository.RolRepository;
import com.utp.smartlab.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class DataSeeder implements CommandLineRunner {

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    private final String adminNombre;
    private final String adminEmail;
    private final String adminPassword;

    public DataSeeder(RolRepository rolRepository,
                      UsuarioRepository usuarioRepository,
                      PasswordEncoder passwordEncoder,
                      @Value("${app.seed.admin-nombre:Administrador SmartLab}") String adminNombre,
                      @Value("${app.seed.admin-email:admin@smartlab.com}") String adminEmail,
                      @Value("${app.seed.admin-password:Admin123!}") String adminPassword) {
        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminNombre = adminNombre;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        sembrarRoles();
        sembrarAdministrador();
    }

    private void sembrarRoles() {
        for (NombreRol nombre : NombreRol.values()) {
            if (rolRepository.findByNombre(nombre).isEmpty()) {
                rolRepository.save(Rol.builder().nombre(nombre).build());
            }
        }
    }

    private void sembrarAdministrador() {
        if (usuarioRepository.existsByEmail(adminEmail)) {
            return;
        }

        Rol rolAdmin = rolRepository.findByNombre(NombreRol.ADMINISTRADOR)
                .orElseThrow(() -> new IllegalStateException("No se pudo crear el rol ADMINISTRADOR"));

        Usuario admin = Usuario.builder()
                .nombre(adminNombre)
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .rol(rolAdmin)
                .build();

        usuarioRepository.save(admin);
    }
}
