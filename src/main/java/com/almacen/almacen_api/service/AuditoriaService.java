package com.almacen.almacen_api.service;

import com.almacen.almacen_api.dto.EquipoEliminadoResponse;
import com.almacen.almacen_api.dto.PaginaResponse;
import com.almacen.almacen_api.entity.EquipoEliminado;
import com.almacen.almacen_api.repository.EquipoEliminadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final EquipoEliminadoRepository repo;

    @Transactional(readOnly = true)
    public PaginaResponse<EquipoEliminadoResponse> listarEliminados(String q, Pageable pageable) {
        String buscar = q == null ? "" : q.trim();
        Page<EquipoEliminado> page = buscar.isEmpty()
                ? repo.findAll(pageable)
                : repo.findByCodigoTkContainingIgnoreCase(buscar, pageable);
        return new PaginaResponse<>(
                page.getContent().stream().map(this::aResponse).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    private EquipoEliminadoResponse aResponse(EquipoEliminado e) {
        return new EquipoEliminadoResponse(
                e.getId(), e.getEquipoId(), e.getCodigoTk(), e.getCategoria(), e.getMarca(),
                e.getNumeroSerie(), e.getDescripcion(), e.getEstado(), e.getUbicacion(),
                e.getTotalMovimientos(), e.getDetalle(), e.getMotivo(), e.getEliminadoPor(),
                e.getFechaEliminacion());
    }
}
