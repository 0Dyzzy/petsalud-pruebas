package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.Cita;
import com.petsalud.backendpetsalud.entity.Mascota;
import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.repository.CitaRepository;
import com.petsalud.backendpetsalud.repository.MascotaRepository;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class CitaServiceImpl implements CitaService {

    private static final Set<String> ESTADOS_VALIDOS = Set.of(
            Cita.ESTADO_AGENDADA,
            Cita.ESTADO_CANCELADA,
            Cita.ESTADO_COMPLETADA
    );

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private MascotaRepository mascotaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public Cita agendarCita(Long mascotaId, Long veterinarioId, LocalDateTime fechaHora, String motivo) {
        if (fechaHora == null) {
            throw new IllegalArgumentException("La fecha y hora de la cita es obligatoria");
        }
        if (fechaHora.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("La fecha y hora de la cita no puede ser anterior a la fecha actual");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("El motivo de la cita es obligatorio");
        }

        Mascota mascota = mascotaRepository.findById(mascotaId)
                .orElseThrow(() -> new IllegalArgumentException("Mascota no encontrada"));

        if (mascota.getPropietario() == null) {
            throw new IllegalArgumentException("La mascota debe tener un propietario registrado");
        }

        Usuario veterinario = usuarioRepository.findById(veterinarioId)
                .orElseThrow(() -> new IllegalArgumentException("Veterinario no encontrado"));

        if (veterinario.getRol() != Rol.VETERINARIO) {
            throw new IllegalArgumentException("El usuario asignado no tiene rol de veterinario");
        }

        boolean veterinarioOcupado = citaRepository
                .existsByVeterinarioIdAndFechaHoraAndEstadoNot(
                        veterinarioId, fechaHora, Cita.ESTADO_CANCELADA);
        if (veterinarioOcupado) {
            throw new IllegalArgumentException("El veterinario ya tiene una cita agendada en ese horario");
        }

        boolean mascotaOcupada = citaRepository
                .existsByMascotaIdAndFechaHoraAndEstadoNot(
                        mascotaId, fechaHora, Cita.ESTADO_CANCELADA);
        if (mascotaOcupada) {
            throw new IllegalArgumentException("La mascota ya tiene una cita agendada en ese horario");
        }

        Cita cita = Cita.builder()
                .mascota(mascota)
                .veterinario(veterinario)
                .fechaHora(fechaHora)
                .motivo(motivo)
                .estado(Cita.ESTADO_AGENDADA)
                .build();

        return citaRepository.save(cita);
    }

    @Override
    public Optional<Cita> obtenerCitaPorId(Long id) {
        return citaRepository.findById(id);
    }

    @Override
    public Cita actualizarEstadoCita(Long id, String nuevoEstado) {
        String estado = nuevoEstado == null ? null : nuevoEstado.trim();
        if (estado == null || !ESTADOS_VALIDOS.contains(estado)) {
            throw new IllegalArgumentException(
                    "Estado de cita inválido: " + nuevoEstado + ". Estados permitidos: " + ESTADOS_VALIDOS);
        }

        return citaRepository.findById(id).map(cita -> {
            cita.setEstado(estado);
            return citaRepository.save(cita);
        }).orElseThrow(() -> new IllegalArgumentException("Cita no encontrada"));
    }

    @Override
    public void cancelarCita(Long id) {
        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cita no encontrada"));

        if (Cita.ESTADO_COMPLETADA.equals(cita.getEstado())) {
            throw new IllegalArgumentException("No se puede cancelar una cita completada");
        }

        cita.setEstado(Cita.ESTADO_CANCELADA);
        citaRepository.save(cita);
    }

    @Override
    public List<Cita> listarCitasPorMascota(Long mascotaId) {
        return citaRepository.findByMascotaId(mascotaId);
    }

    @Override
    public List<Cita> listarCitasPorVeterinario(Long veterinarioId) {
        return citaRepository.findByVeterinarioId(veterinarioId);
    }

    @Override
    public List<Cita> listarCitasEntreFechas(LocalDateTime inicio, LocalDateTime fin) {
        return citaRepository.findByFechaHoraBetween(inicio, fin);
    }
}
