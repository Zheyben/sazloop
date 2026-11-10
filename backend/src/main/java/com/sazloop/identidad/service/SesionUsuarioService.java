package com.sazloop.identidad.service;

import com.sazloop.identidad.domain.Usuario;
import com.sazloop.identidad.dto.InicioSesionRequest;
import com.sazloop.identidad.dto.UsuarioResponse;
import com.sazloop.identidad.repository.UsuarioRepository;
import com.sazloop.shared.LimiteIntentosLogin;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Verifica la identidad del usuario registrado sin exponer la contraseña. */
@Service
public class SesionUsuarioService {
    private final UsuarioRepository usuarios;
    private final CodificadorContrasena codificador;
    private final LimiteIntentosLogin limite;

    public SesionUsuarioService(UsuarioRepository usuarios, CodificadorContrasena codificador,
                                LimiteIntentosLogin limite) {
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.limite = limite;
    }

    @Transactional(readOnly = true)
    public UsuarioResponse iniciar(InicioSesionRequest entrada) {
        String id = entrada.email();
        limite.exigirDisponible(LimiteIntentosLogin.PERSONA, id);
        Usuario usuario = usuarios.findByEmailIgnoreCase(id.trim()).orElse(null);
        // Si el correo no existe se calcula igual el PBKDF2, para responder en el mismo tiempo.
        boolean correcto = usuario == null ? codificador.rellenar(entrada.contrasena())
                : codificador.coincide(entrada.contrasena(), usuario.getPasswordHash()) && usuario.isActivo();
        if (!correcto) {
            limite.registrarFallo(LimiteIntentosLogin.PERSONA, id);
            throw accesoDenegado();
        }
        limite.registrarExito(LimiteIntentosLogin.PERSONA, id);
        return respuesta(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Long id) {
        Usuario usuario = usuarios.findById(id).orElseThrow(SesionUsuarioService::accesoDenegado);
        if (!usuario.isActivo()) {
            throw accesoDenegado();
        }
        return respuesta(usuario);
    }

    private static UsuarioResponse respuesta(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNombres(),
                usuario.getApellidos(), usuario.getEmail(), usuario.getTelefono(), usuario.getFotoPerfil() != null);
    }

    private static ResponseStatusException accesoDenegado() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
    }
}
