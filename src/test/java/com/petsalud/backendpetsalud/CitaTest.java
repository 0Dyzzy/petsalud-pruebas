package com.petsalud.backendpetsalud;

import com.petsalud.backendpetsalud.entity.Cita;
import com.petsalud.backendpetsalud.entity.Mascota;
import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas de la entidad Cita")
class CitaTest {

    @Test
    @DisplayName("Una cita nueva parte en estado Agendada")
    void citaNuevaUsaEstadoAgendadaPorDefecto() {
        Cita cita = Cita.builder().build();

        assertEquals(Cita.ESTADO_AGENDADA, cita.getEstado());
    }

    @Test
    @DisplayName("Construye una cita con mascota, veterinario, fecha y motivo")
    void creaCitaConRelacionesValidas() {
        Usuario propietario = Usuario.builder()
                .id(20L)
                .email("cliente@petsalud.cl")
                .nombre("Juan")
                .password("hash")
                .rol(Rol.CLIENTE)
                .build();

        Usuario veterinario = Usuario.builder()
                .id(10L)
                .email("vet@petsalud.cl")
                .nombre("Ana")
                .password("hash")
                .rol(Rol.VETERINARIO)
                .build();

        Mascota mascota = Mascota.builder()
                .id(1L)
                .nombre("Firulais")
                .especie("Perro")
                .propietario(propietario)
                .build();

        LocalDateTime fechaHora = LocalDateTime.of(2030, 1, 15, 10, 30);

        Cita cita = Cita.builder()
                .mascota(mascota)
                .veterinario(veterinario)
                .fechaHora(fechaHora)
                .motivo("Control general")
                .build();

        assertAll(
                () -> assertEquals(Cita.ESTADO_AGENDADA, cita.getEstado()),
                () -> assertSame(mascota, cita.getMascota()),
                () -> assertSame(veterinario, cita.getVeterinario()),
                () -> assertEquals(fechaHora, cita.getFechaHora()),
                () -> assertEquals("Control general", cita.getMotivo()),
                () -> assertEquals(Rol.VETERINARIO, cita.getVeterinario().getRol()),
                () -> assertNotNull(cita.getMascota().getPropietario())
        );
    }

    @Test
    @DisplayName("Expone los estados válidos del ciclo de vida de una cita")
    void estadosValidosDeCita() {
        assertAll(
                () -> assertEquals("Agendada", Cita.ESTADO_AGENDADA),
                () -> assertEquals("Cancelada", Cita.ESTADO_CANCELADA),
                () -> assertEquals("Completada", Cita.ESTADO_COMPLETADA)
        );
    }

    @Test
    @DisplayName("Permite cambiar el estado de una cita")
    void cambiaEstadoDeCita() {
        Cita cita = Cita.builder().build();
        cita.setEstado(Cita.ESTADO_COMPLETADA);

        assertEquals(Cita.ESTADO_COMPLETADA, cita.getEstado());
    }
}
