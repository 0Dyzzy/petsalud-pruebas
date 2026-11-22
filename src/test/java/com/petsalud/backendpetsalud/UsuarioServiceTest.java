package com.petsalud.backendpetsalud;

import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import com.petsalud.backendpetsalud.service.UsuarioServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas unitarias de la entidad Usuario")
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    private Usuario crearUsuarioValido() {
        return Usuario.builder()
                .email("juan.perez@example.com")
                .password("password123")
                .nombre("Juan")
                .apellido("Perez")
                .telefono("1234567890")
                .direccion("Calle Falsa 123")
                .rol(Rol.CLIENTE)
                .build();
    }

    @Test
    @DisplayName("Registra un usuario válido y cifra su contraseña")
    void registrarUsuarioValido() {
        Usuario usuario = crearUsuarioValido();
        usuario.setNombre("  Juan  ");

        when(usuarioRepository.findByEmail("juan.perez@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashSeguro");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Usuario registrado = usuarioService.registrarUsuario(usuario);

        assertAll(
                () -> assertEquals("juan.perez@example.com", registrado.getEmail()),
                () -> assertEquals("Juan", registrado.getNombre()),
                () -> assertEquals("$2a$10$hashSeguro", registrado.getPassword()),
                () -> assertEquals(Rol.CLIENTE, registrado.getRol())
        );
        verify(passwordEncoder).encode("password123");
        verify(usuarioRepository).save(usuario);
    }

    @Test
    @DisplayName("Rechaza un usuario nulo")
    void registrarUsuarioNuloFalla() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> usuarioService.registrarUsuario(null));

        assertEquals("El usuario es obligatorio", error.getMessage());
        verifyNoInteractions(usuarioRepository, passwordEncoder);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Rechaza un email nulo, vacío o en blanco")
    void registrarUsuarioConEmailInvalidoFalla(String email) {
        Usuario usuario = crearUsuarioValido();
        usuario.setEmail(email);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> usuarioService.registrarUsuario(usuario));

        assertEquals("El email es obligatorio", error.getMessage());
        verifyNoInteractions(usuarioRepository, passwordEncoder);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Rechaza un nombre nulo, vacío o en blanco")
    void registrarUsuarioConNombreInvalidoFalla(String nombre) {
        Usuario usuario = crearUsuarioValido();
        usuario.setNombre(nombre);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> usuarioService.registrarUsuario(usuario));

        assertEquals("El nombre es obligatorio", error.getMessage());
        verifyNoInteractions(usuarioRepository, passwordEncoder);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Rechaza una contraseña nula, vacía o en blanco")
    void registrarUsuarioConPasswordInvalidaFalla(String password) {
        Usuario usuario = crearUsuarioValido();
        usuario.setPassword(password);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> usuarioService.registrarUsuario(usuario));

        assertEquals("La contraseña es obligatoria", error.getMessage());
        verifyNoInteractions(usuarioRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Rechaza un rol nulo")
    void registrarUsuarioSinRolFalla() {
        Usuario usuario = crearUsuarioValido();
        usuario.setRol(null);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> usuarioService.registrarUsuario(usuario));

        assertEquals("El rol es obligatorio", error.getMessage());
        verifyNoInteractions(usuarioRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Rechaza un email ya registrado")
    void registrarUsuarioConEmailDuplicadoFalla() {
        Usuario usuario = crearUsuarioValido();
        when(usuarioRepository.findByEmail("juan.perez@example.com"))
                .thenReturn(Optional.of(crearUsuarioValido()));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> usuarioService.registrarUsuario(usuario));

        assertEquals("El email ya está registrado", error.getMessage());
        verify(usuarioRepository, never()).save(any(Usuario.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("Obtiene un usuario por id")
    void obtenerUsuarioPorId() {
        Usuario usuario = crearUsuarioValido();
        usuario.setId(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        assertEquals(usuario, usuarioService.obtenerUsuarioPorId(1L).orElseThrow());
    }

    @Test
    @DisplayName("Obtiene un usuario por email")
    void obtenerUsuarioPorEmail() {
        Usuario usuario = crearUsuarioValido();
        when(usuarioRepository.findByEmail("juan.perez@example.com")).thenReturn(Optional.of(usuario));

        assertEquals(usuario, usuarioService.obtenerUsuarioPorEmail("juan.perez@example.com").orElseThrow());
    }

    @Test
    @DisplayName("Actualiza los datos editables sin cambiar email ni contraseña")
    void actualizarUsuario() {
        Usuario existente = crearUsuarioValido();
        existente.setId(1L);
        String passwordOriginal = existente.getPassword();

        Usuario cambios = Usuario.builder()
                .nombre("  Juan Carlos  ")
                .apellido("Perez Soto")
                .telefono("999999999")
                .direccion("Nueva Direccion 456")
                .email("otro@example.com")
                .password("otraClave")
                .build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.save(existente)).thenReturn(existente);

        Usuario actualizado = usuarioService.actualizarUsuario(1L, cambios);

        assertAll(
                () -> assertEquals("Juan Carlos", actualizado.getNombre()),
                () -> assertEquals("Perez Soto", actualizado.getApellido()),
                () -> assertEquals("999999999", actualizado.getTelefono()),
                () -> assertEquals("Nueva Direccion 456", actualizado.getDireccion()),
                () -> assertEquals("juan.perez@example.com", actualizado.getEmail()),
                () -> assertEquals(passwordOriginal, actualizado.getPassword())
        );
    }

    @Test
    @DisplayName("Rechaza actualizar con un nombre vacío")
    void actualizarUsuarioSinNombreFalla() {
        Usuario cambios = Usuario.builder().nombre("  ").build();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> usuarioService.actualizarUsuario(1L, cambios));

        assertEquals("El nombre del usuario es obligatorio", error.getMessage());
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    @DisplayName("No actualiza un usuario inexistente")
    void actualizarUsuarioInexistenteFalla() {
        Usuario cambios = Usuario.builder().nombre("Nuevo Nombre").build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> usuarioService.actualizarUsuario(1L, cambios));

        assertEquals("Usuario no encontrado", error.getMessage());
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Elimina un usuario por id")
    void eliminarUsuario() {
        usuarioService.eliminarUsuario(1L);

        verify(usuarioRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Lista todos los usuarios")
    void listarUsuarios() {
        when(usuarioRepository.findAll()).thenReturn(List.of(crearUsuarioValido()));

        assertEquals(1, usuarioService.listarUsuarios().size());
    }
}
