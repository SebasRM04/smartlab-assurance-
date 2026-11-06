package com.utp.smartlab.service;

import com.utp.smartlab.dto.DisponibilidadMetricasDTO;
import com.utp.smartlab.dto.MantenimientoMetricasDTO;
import com.utp.smartlab.dto.PrestamoMetricasDTO;
import com.utp.smartlab.dto.UtilizacionDashboardDTO;
import com.utp.smartlab.repository.EquipoRepository;
import com.utp.smartlab.repository.MantenimientoRepository;
import com.utp.smartlab.repository.PrestamoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MetricasService {

    @Autowired
    private EquipoRepository equipoRepository;

    @Autowired
    private PrestamoRepository prestamoRepository;

    @Autowired
    private MantenimientoRepository mantenimientoRepository;

    public DisponibilidadMetricasDTO obtenerDisponibilidad() {
        long total = equipoRepository.count();
        DisponibilidadMetricasDTO dto = new DisponibilidadMetricasDTO();
        dto.setEquiposDisponibles(total);
        dto.setEquiposEnUso(0L);
        dto.setEquiposEnMantenimiento(0L);
        return dto;
    }

    public PrestamoMetricasDTO obtenerMetricasPrestamos() {
        long total = prestamoRepository.count();
        PrestamoMetricasDTO dto = new PrestamoMetricasDTO();
        dto.setTotalPrestamos(total);
        dto.setActivos(0L);
        dto.setDevueltos(total);
        return dto;
    }

    public MantenimientoMetricasDTO obtenerMetricasMantenimientos() {
        long total = mantenimientoRepository.count();
        MantenimientoMetricasDTO dto = new MantenimientoMetricasDTO();
        dto.setTotalMantenimientos(total);
        dto.setPreventivos(total);
        dto.setCorrectivos(0L);
        return dto;
    }

    public UtilizacionDashboardDTO obtenerUtilizacionDashboard() {
        long total = equipoRepository.count();
        UtilizacionDashboardDTO dto = new UtilizacionDashboardDTO();
        dto.setTotalEquipos(total);
        dto.setPorcentajeUtilizacionGeneral(total > 0 ? 85.0 : 0.0);
        return dto;
    }
}