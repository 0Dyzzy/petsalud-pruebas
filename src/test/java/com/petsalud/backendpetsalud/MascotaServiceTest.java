package com.petsalud.backendpetsalud;

import com.petsalud.backendpetsalud.entity.Mascota;
import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.repository.MascotaRepository;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import com.petsalud.backendpetsalud.service.MascotaServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas unitarias de la relación Usuario-Mascota")
class MascotaServiceTest {

    @Mock
    private MascotaRepository mascotaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private MascotaServiceImpl mascotaService;

    private Usuario crearCliente() {
        return Usuario.builder()
                .id(20L)
                .email("cliente@petsalud.cl")
                .nombre("Juan")
                .password("hash")
                .rol(Rol.CLIENTE)
                .build();
    }

    private Mascota crearMascota() {
        return Mascota.builder()
                .id(1L)
                .nombre("Firulais")
                .especie("Perro")
                .raza("Labrador")
                .build();
    }

    @Test
    @DisplayName("Un cliente puede registrar una mascota con datos válidos")
    void registrarMascotaDeCliente() {
        Mascota mascota = crearMascota();
        Usuario cliente = crearCliente();

        when(usuarioRepository.findById(20L)).thenReturn(Optional.of(cliente));
        when(mascotaRepository.save(any(Mascota.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Mascota registrada = mascotaService.registrarMascota(mascota, 20L);

        assertAll(
                () -> assertSame(cliente, registrada.getPropietario()),
                () -> assertEquals("Firulais", registrada.getNombre()),
                () -> assertEquals("Perro", registrada.getEspecie()),
                () -> assertEquals(Rol.CLIENTE, registrada.getPropietario().getRol())
        );
        verify(mascotaRepository).save(mascota);
    }

    @Test
    @DisplayName("Rechaza una mascota nula")
    void registrarMascotaNulaFalla() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> mascotaService.registrarMascota(null, 20L));

        assertEquals("La mascota es obligatoria", error.getMessage());
        verifyNoInteractions(mascotaRepository, usuarioRepository);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Rechaza una mascota sin nombre válido")
    void registrarMascotaSinNombreFalla(String nombre) {
        Mascota mascota = crearMascota();
        mascota.setNombre(nombre);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> mascotaService.registrarMascota(mascota, 20L));

        assertEquals("El nombre de la mascota es obligatorio", error.getMessage());
        verifyNoInteractions(mascotaRepository, usuarioRepository);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Rechaza una mascota sin especie válida")
    void registrarMascotaSinEspecieFalla(String especie) {
        Mascota mascota = crearMascota();
        mascota.setEspecie(especie);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> mascotaService.registrarMascota(mascota, 20L));

        assertEquals("La especie de la mascota es obligatoria", error.getMessage());
        verifyNoInteractions(mascotaRepository, usuarioRepository);
    }

    @Test
    @DisplayName("Rechaza registrar una mascota si el propietario no existe")
    void registrarMascotaSinPropietarioRegistradoFalla() {
        Mascota mascota = crearMascota();
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> mascotaService.registrarMascota(mascota, 99L));

        assertEquals("Propietario no encontrado", error.getMessage());
        verifyNoInteractions(mascotaRepository);
    }

    @Test
    @DisplayName("Obtiene una mascota por id")
    void obtenerMascotaPorId() {
        Mascota mascota = crearMascota();
        when(mascotaRepository.findById(1L)).thenReturn(Optional.of(mascota));

        assertEquals(mascota, mascotaService.obtenerMascotaPorId(1L).orElseThrow());
    }

    @Test
    @DisplayName("Actualiza los datos de una mascota")
    void actualizarMascota() {
        Mascota existente = crearMascota();
        Mascota cambios = Mascota.builder()
                .nombre("  Rocky  ")
                .especie("  Perro  ")
                .raza("Beagle")
                .build();

        when(mascotaRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(mascotaRepository.save(existente)).thenReturn(existente);

        Mascota actualizada = mascotaService.actualizarMascota(1L, cambios);

        assertAll(
                () -> assertEquals("Rocky", actualizada.getNombre()),
                () -> assertEquals("Perro", actualizada.getEspecie()),
                () -> assertEquals("Beagle", actualizada.getRaza())
        );
    }

    @Test
    @DisplayName("Rechaza actualizar una mascota con datos inválidos")
    void actualizarMascotaInvalidaFalla() {
        Mascota cambios = Mascota.builder().nombre("Rocky").especie(" ").build();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> mascotaService.actualizarMascota(1L, cambios));

        assertEquals("La especie de la mascota es obligatoria", error.getMessage());
        verifyNoInteractions(mascotaRepository);
    }

    @Test
    @DisplayName("No actualiza una mascota inexistente")
    void actualizarMascotaInexistenteFalla() {
        Mascota cambios = crearMascota();
        when(mascotaRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> mascotaService.actualizarMascota(1L, cambios));

        assertEquals("Mascota no encontrada", error.getMessage());
        verify(mascotaRepository, never()).save(any(Mascota.class));
    }

    @Test
    @DisplayName("Elimina una mascota por id")
    void eliminarMascota() {
        mascotaService.eliminarMascota(1L);

        verify(mascotaRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Lista las mascotas de un propietario")
    void listarMascotasPorPropietario() {
        when(mascotaRepository.findByPropietarioId(20L)).thenReturn(List.of(crearMascota()));

        assertEquals(1, mascotaService.listarMascotasPorPropietario(20L).size());
    }
}
