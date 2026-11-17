package com.utp.smartlab.repository;

import com.utp.smartlab.entity.Equipo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EquipoRepository extends JpaRepository<Equipo, Long> {

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("SELECT e FROM Equipo e WHERE e.id = :id")
    Optional<Equipo> buscarParaOperacion(@Param("id") Long id);

    @Query("SELECT e FROM Equipo e WHERE e.contadorUsoHoras >= :umbralHoras")
    List<Equipo> buscarPorUsoMayorOIgualA(@Param("umbralHoras") int umbralHoras);

    @Query("SELECT COUNT(e) FROM Equipo e WHERE e.categoria.id = :categoriaId")
    long contarPorCategoriaId(@Param("categoriaId") Long categoriaId);
}
