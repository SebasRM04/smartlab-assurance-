package com.utp.smartlab.repository;

import com.utp.smartlab.entity.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    @Query("SELECT a FROM Auditoria a WHERE a.usuario.id = :usuarioId ORDER BY a.fechaHora DESC")
    List<Auditoria> buscarPorUsuarioId(@Param("usuarioId") Long usuarioId);

    @Query("SELECT COUNT(a) FROM Auditoria a WHERE a.usuario.id = :usuarioId")
    long contarPorUsuarioId(@Param("usuarioId") Long usuarioId);
}
