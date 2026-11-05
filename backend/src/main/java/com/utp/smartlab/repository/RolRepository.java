package com.utp.smartlab.repository;

import com.utp.smartlab.entity.NombreRol;
import com.utp.smartlab.entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Long> {
    Optional<Rol> findByNombre(NombreRol nombre);
}
