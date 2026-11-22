package com.petsalud.backendpetsalud;

import com.petsalud.backendpetsalud.entity.Cita;
import com.petsalud.backendpetsalud.entity.Mascota;
import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.repository.CitaRepository;
import com.petsalud.backendpetsalud.repository.MascotaRepository;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import com.petsalud.backendpetsalud.service.CitaServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas unitarias de la entidad Cita")
class CitaServiceTest {

    @Mock
    private CitaRepository citaRepository;

    @Mock
    private MascotaRepository mascotaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private CitaServiceImpl citaService;

    private final LocalDateTime fechaFutura = LocalDateTime.now().plusDays(1).withNano(0);

    private Usuario crearVeterinario() {
        return Usuario.builder()
                .id(10L)
                .email("vet@petsalud.cl")
                .nombre("Ana")
                .apellido("Vet")
                .password("hash")
                .rol(Rol.VETERINARIO)
                .build();
    }

    private Usuario crearCliente() {
        return Usuario.builder()
                .id(20L)
                .email("cliente@petsalud.cl")
                .nombre("Juan")
                .apellido("Perez")
                .password("hash")
                .rol(Rol.CLIENTE)
                .build();
    }

    private Mascota crearMascotaConPropietario() {
        return Mascota.builder()
                .id(1L)
                .nombre("Firulais")
                .especie("Perro")
                .raza("Labrador")
                .propietario(crearCliente())
                .build();
    }

    private void simularHorarioDisponible(Mascota mascota, Usuario veterinario) {
        when(citaRepository.existsByVeterinarioIdAndFechaHoraAndEstadoNot(
                veterinario.getId(), fechaFutura, Cita.ESTADO_CANCELADA)).thenReturn(false);
        when(citaRepository.existsByMascotaIdAndFechaHoraAndEstadoNot(
                mascota.getId(), fechaFutura, Cita.ESTADO_CANCELADA)).thenReturn(false);
    }

    @Test
    @DisplayName("Agenda una cita correctamente con estado inicial Agendada")
    void agendarCitaExitosa() {
        Mascota mascota = crearMascotaConPropietario();
        Usuario veterinario = crearVeterinario();

        when(mascotaRepository.findById(1L)).thenReturn(Optional.of(mascota));
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(veterinario));
        simularHorarioDisponible(mascota, veterinario);
        when(citaRepository.save(any(Cita.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cita cita = citaService.agendarCita(1L, 10L, fechaFutura, "Control general");

        assertAll(
                () -> assertEquals(Cita.ESTADO_AGENDADA, cita.getEstado()),
                () -> assertSame(mascota, cita.getMascota()),
                () -> assertSame(veterinario, cita.getVeterinario()),
                () -> assertEquals(fechaFutura, cita.getFechaHora()),
                () -> assertEquals("Control general", cita.getMotivo())
        );
        verify(citaRepository).save(cita);
    }

    @Test
    @DisplayName("No agenda una cita sin fecha y hora")
    void agendarCitaSinFechaHoraFalla() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> citaService.agendarCita(1L, 10L, null, "Control"));

        assertEquals("La fecha y hora de la cita es obligatoria", error.getMessage());
        verifyNoInteractions(mascotaRepository, usuarioRepository, citaRepository);
    }

    @Test
    @DisplayName("No agenda una cita en una fecha pasada")
    void agendarCitaEnFechaPasadaFalla() {
        LocalDateTime ayer = LocalDateTime.now().minusDays(1);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> citaService.agendarCita(1L, 10L, ayer, "Control"));

        assertEquals("La fecha y hora de la cita no puede ser anterior a la fecha actual", error.getMessage());
        verifyNoInteractions(mascotaRepository, usuarioRepository, citaRepository);
    }

    @Test
    @DisplayName("No agenda una cita sin motivo")
    void agendarCitaSinMotivoFalla() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> citaService.agendarCita(1L, 10L, fechaFutura, "  "));

        assertEquals("El motivo de la cita es obligatorio", error.getMessage());
        verifyNoInteractions(mascotaRepository, usuarioRepository, citaRepository);
    }

    @Test
    @DisplayName("No agenda una cita si la mascota no existe")
    void agendarCitaConMascotaInexistenteFalla() {
        when(mascotaRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> citaService.agendarCita(99L, 10L, fechaFutura, "Control"));

        assertEquals("Mascota no encontrada", error.getMessage());
        verifyNoInteractions(usuarioRepository, citaRepository);
    }

    @Test
    @DisplayName("No agenda una cita si la mascota no tiene propietario registrado")
    void agendarCitaConMascotaSinPropietarioFalla() {
        Mascota mascotaSinDueno = Mascota.builder().id(1L).nombre("Firulais").especie("Perro").build();
        when(mascotaRepository.findById(1L)).thenReturn(Optional.of(mascotaSinDueno));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> citaService.agendarCita(1L, 10L, fechaFutura, "Control"));

        assertEquals("La mascota debe tener un propietario registrado", error.getMessage());
        verifyNoInteractions(usuarioRepository, citaRepository);
    }

    @Test
    @DisplayName("No agenda una cita si el veterinario no existe")
    void agendarCitaConVeterinarioInexistenteFalla() {
        Mascota mascota = crearMascotaConPropietario();
        when(mascotaRepository.findById(1L)).thenReturn(Optional.of(mascota));
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> citaService.agendarCita(1L, 99L, fechaFutura, "Control"));

        assertEquals("Veterinario no encontrado", error.getMessage());
        verifyNoInteractions(citaRepository);
    }

    @Test
    @DisplayName("No agenda una cita si el usuario asignado no es veterinario")
    void agendarCitaConUsuarioNoVeterinarioFalla() {
        Mascota mascota = crearMascotaConPropietario();
        Usuario cliente = crearCliente();

        when(mascotaRepository.findById(1L)).thenReturn(Optional.of(mascota));
        when(usuarioRepository.findById(20L)).thenReturn(Optional.of(cliente));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> citaService.agendarCita(1L, 20L, fechaFutura, "Control"));

        assertEquals("El usuario asignado no tiene rol de veterinario", error.getMessage());
        verifyNoInteractions(citaRepository);
    }

    @Test
    @DisplayName("No agenda una cita si el veterinario ya tiene una cita en ese horario")
    void agendarCitaConVeterinarioOcupadoFalla() {
        Mascota mascota = crearMascotaConPropietario();
        Usuario veterinario = crearVeterinario();

        when(mascotaRepository.findById(1L)).thenReturn(Optional.of(mascota));
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(veterinario));
        when(citaRepository.existsByVeterinarioIdAndFechaHoraAndEstadoNot(
                10L, fechaFutura, Cita.ESTADO_CANCELADA)).thenReturn(true);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> citaService.agendarCita(1L, 10L, fechaFutura, "Control"));

        assertEquals("El veterinario ya tiene una cita agendada en ese horario", error.getMessage());
        verify(citaRepository, never()).save(any(Cita.class));
    }

    @Test
    @DisplayName("No agenda una cita si la mascota ya tiene una cita en ese horario")
    void agendarCitaConMascotaOcupadaFalla() {
        Mascota mascota = crearMascotaConPropietario();
        Usuario veterinario = crearVeterinario();

        when(mascotaRepository.findById(1L)).thenReturn(Optional.of(mascota));
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(veterinario));
        when(citaRepository.existsByVeterinarioIdAndFechaHoraAndEstadoNot(
                10L, fechaFutura, Cita.ESTADO_CANCELADA)).thenReturn(false);
        when(citaRepository.existsByMascotaIdAndFechaHoraAndEstadoNot(
                1L, fechaFutura, Cita.ESTADO_CANCELADA)).thenReturn(true);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> citaService.agendarCita(1L, 10L, fechaFutura, "Control"));

        assertEquals("La mascota ya tiene una cita agendada en ese horario", error.getMessage());
        verify(citaRepository, never()).save(any(Cita.class));
    }

    @Test
    @DisplayName("Una cita cancelada libera el horario del veterinario")
    void citaCanceladaNoBloqueaElHorario() {
        Mascota mascota = crearMascotaConPropietario();
        Usuario veterinario = crearVeterinario();

        when(mascotaRepository.findById(1L)).thenReturn(Optional.of(mascota));
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(veterinario));
        when(citaRepository.existsByVeterinarioIdAndFechaHoraAndEstadoNot(
                10L, fechaFutura, Cita.ESTADO_CANCELADA)).thenReturn(false);
        when(citaRepository.existsByMascotaIdAndFechaHoraAndEstadoNot(
                1L, fechaFutura, Cita.ESTADO_CANCELADA)).thenReturn(false);
        when(citaRepository.save(any(Cita.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cita cita = citaService.agendarCita(1L, 10L, fechaFutura, "Control");

        assertEquals(Cita.ESTADO_AGENDADA, cita.getEstado());
        verify(citaRepository).existsByVeterinarioIdAndFechaHoraAndEstadoNot(
                10L, fechaFutura, Cita.ESTADO_CANCELADA);
    }

    @Test
    @DisplayName("Obtiene una cita por su id")
    void obtenerCitaPorId() {
        Cita cita = Cita.builder().id(5L).build();
        when(citaRepository.findById(5L)).thenReturn(Optional.of(cita));

        assertTrue(citaService.obtenerCitaPorId(5L).isPresent());
        assertEquals(5L, citaService.obtenerCitaPorId(5L).orElseThrow().getId());
    }

    @Test
    @DisplayName("Devuelve vacío cuando la cita no existe")
    void obtenerCitaInexistente() {
        when(citaRepository.findById(5L)).thenReturn(Optional.empty());

        assertTrue(citaService.obtenerCitaPorId(5L).isEmpty());
    }

    @Test
    @DisplayName("Actualiza el estado de una cita con un estado válido")
    void actualizarEstadoValido() {
        Cita cita = Cita.builder().id(5L).estado(Cita.ESTADO_AGENDADA).build();
        when(citaRepository.findById(5L)).thenReturn(Optional.of(cita));
        when(citaRepository.save(cita)).thenReturn(cita);

        Cita actualizada = citaService.actualizarEstadoCita(5L, " Completada ");

        assertEquals(Cita.ESTADO_COMPLETADA, actualizada.getEstado());
        verify(citaRepository).save(cita);
    }

    @Test
    @DisplayName("Rechaza un estado de cita inválido")
    void actualizarEstadoInvalido() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> citaService.actualizarEstadoCita(5L, "En espera"));

        assertTrue(error.getMessage().contains("Estado de cita inválido"));
        verifyNoInteractions(citaRepository);
    }

    @Test
    @DisplayName("Rechaza un estado nulo")
    void actualizarEstadoNulo() {
        assertThrows(IllegalArgumentException.class,
                () -> citaService.actualizarEstadoCita(5L, null));
        verifyNoInteractions(citaRepository);
    }

    @Test
    @DisplayName("No actualiza el estado de una cita inexistente")
    void actualizarEstadoDeCitaInexistente() {
        when(citaRepository.findById(5L)).thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> citaService.actualizarEstadoCita(5L, Cita.ESTADO_COMPLETADA));

        assertEquals("Cita no encontrada", error.getMessage());
        verify(citaRepository, never()).save(any(Cita.class));
    }

    @Test
    @DisplayName("Cancela una cita cambiando su estado a Cancelada")
    void cancelarCitaExitosa() {
        Cita cita = Cita.builder().id(5L).estado(Cita.ESTADO_AGENDADA).build();
        when(citaRepository.findById(5L)).thenReturn(Optional.of(cita));
        when(citaRepository.save(cita)).thenReturn(cita);

        citaService.cancelarCita(5L);

        assertEquals(Cita.ESTADO_CANCELADA, cita.getEstado());
        verify(citaRepository).save(cita);
    }

    @Test
    @DisplayName("No cancela una cita inexistente")
    void cancelarCitaInexistente() {
        when(citaRepository.findById(5L)).thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> citaService.cancelarCita(5L));

        assertEquals("Cita no encontrada", error.getMessage());
        verify(citaRepository, never()).save(any(Cita.class));
    }

    @Test
    @DisplayName("No cancela una cita completada")
    void cancelarCitaCompletadaFalla() {
        Cita cita = Cita.builder().id(5L).estado(Cita.ESTADO_COMPLETADA).build();
        when(citaRepository.findById(5L)).thenReturn(Optional.of(cita));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> citaService.cancelarCita(5L));

        assertEquals("No se puede cancelar una cita completada", error.getMessage());
        verify(citaRepository, never()).save(any(Cita.class));
    }

    @Test
    @DisplayName("Lista las citas por mascota, veterinario y rango de fechas")
    void listarCitas() {
        Cita cita = Cita.builder().id(5L).build();
        LocalDateTime inicio = fechaFutura.minusHours(1);
        LocalDateTime fin = fechaFutura.plusHours(1);

        when(citaRepository.findByMascotaId(1L)).thenReturn(List.of(cita));
        when(citaRepository.findByVeterinarioId(10L)).thenReturn(List.of(cita));
        when(citaRepository.findByFechaHoraBetween(inicio, fin)).thenReturn(List.of(cita));

        assertEquals(1, citaService.listarCitasPorMascota(1L).size());
        assertEquals(1, citaService.listarCitasPorVeterinario(10L).size());
        assertEquals(1, citaService.listarCitasEntreFechas(inicio, fin).size());
    }
}
