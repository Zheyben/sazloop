package com.sazloop.afiliacion.service;

import com.sazloop.afiliacion.domain.AdministradorPlataforma;
import com.sazloop.afiliacion.repository.AdministradorPlataformaRepository;
import com.sazloop.identidad.service.CodificadorContrasena;
import com.sazloop.shared.LimiteIntentosLogin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.Locale;

/** Acceso reservado al personal interno; jamás expone registro público. */
@Service
public class AdministradorService {
    private final AdministradorPlataformaRepository administradores;
    private final CodificadorContrasena codificador;
    private final LimiteIntentosLogin limite;

    public AdministradorService(AdministradorPlataformaRepository administradores,
                                CodificadorContrasena codificador, LimiteIntentosLogin limite) {
        this.administradores = administradores;
        this.codificador = codificador;
        this.limite = limite;
    }

    @Transactional(readOnly = true)
    public AdministradorPlataforma iniciar(String email, String clave) {
        limite.exigirDisponible(LimiteIntentosLogin.ADMINISTRADOR, email);
        AdministradorPlataforma admin = administradores.findByEmailInstitucionalIgnoreCase(email.trim()).orElse(null);
        // Si el correo no existe se calcula igual el PBKDF2, para responder en el mismo tiempo.
        boolean correcto = admin == null ? codificador.rellenar(clave)
                : codificador.coincide(clave, admin.getPasswordHash()) && admin.isActivo();
        if (!correcto) {
            limite.registrarFallo(LimiteIntentosLogin.ADMINISTRADOR, email);
            throw denegado();
        }
        limite.registrarExito(LimiteIntentosLogin.ADMINISTRADOR, email);
        return admin;
    }

    @Transactional(readOnly = true)
    public AdministradorPlataforma obtener(Long id) {
        return administradores.findById(id).filter(AdministradorPlataforma::isActivo)
                .orElseThrow(AdministradorService::denegado);
    }

    /**
     * Crea la cuenta del administrador al iniciar, si todavía no existe, con el correo y la
     * contraseña de application.properties. Son credenciales de demostración, escritas en el
     * repositorio para ejecutar este proyecto académico en una computadora local; no deben usarse
     * en un entorno real, donde vendrían de variables de entorno. La contraseña de MySQL, en cambio,
     * no está en el repositorio: cada persona la escribe en backend/application-local.properties.
     */
    @Bean
    @Profile("!demo")
    CommandLineRunner provisionarAdministrador(
            @Value("${sazloop.admin.email:}") String email,
            @Value("${sazloop.admin.password:}") String clave) {
        return args -> {
            if (email.isBlank() || clave.isBlank()) return;
            String normalizado = email.trim().toLowerCase(Locale.ROOT);
            if (administradores.findByEmailInstitucionalIgnoreCase(normalizado).isEmpty()) {
                administradores.save(AdministradorPlataforma.builder().nombre("Administrador")
                        .emailInstitucional(normalizado)
                        .passwordHash(codificador.codificar(clave)).build());
            }
        };
    }

    private static ResponseStatusException denegado() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Acceso administrativo inválido");
    }
}
