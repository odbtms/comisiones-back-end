package com.cordillera.comisiones.service;

import java.util.Hashtable;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Valida que el dominio de un email pueda recibir correos, consultando DNS por
 * registros MX (con fallback a A). No envia ningun mail: solo verifica que el
 * dominio exista y tenga servidor de correo. Asi se rechazan dominios falsos o
 * con typos (ej. @gmial.com) sin necesidad de un proveedor de email.
 *
 * Se puede desactivar con app.registro.validar-dominio=false (util en entornos
 * sin salida a DNS). Ante errores DNS transitorios NO bloquea (fail-open), para
 * no rechazar usuarios legitimos por una caida momentanea del DNS.
 */
@Component
public class EmailDominioValidador {

    private static final Logger log = LoggerFactory.getLogger(EmailDominioValidador.class);

    private final boolean habilitado;

    public EmailDominioValidador(
        @Value("${app.registro.validar-dominio:true}") boolean habilitado) {
        this.habilitado = habilitado;
    }

    /** True si el dominio del email puede recibir correo (o si la validacion esta off). */
    public boolean dominioPuedeRecibirMail(String email) {
        if (!habilitado) {
            return true;
        }
        if (email == null) {
            return false;
        }
        int at = email.lastIndexOf('@');
        if (at < 0 || at == email.length() - 1) {
            return false;
        }
        String dominio = email.substring(at + 1).trim();
        if (dominio.isEmpty()) {
            return false;
        }

        Hashtable<String, String> env = new Hashtable<>();
        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        env.put("com.sun.jndi.dns.timeout.initial", "3000");
        env.put("com.sun.jndi.dns.timeout.retries", "1");

        DirContext ctx = null;
        try {
            ctx = new InitialDirContext(env);
            Attributes attrs = ctx.getAttributes(dominio, new String[] {"MX", "A"});
            Attribute mx = attrs.get("MX");
            if (mx != null && mx.size() > 0) {
                return true;
            }
            Attribute a = attrs.get("A");
            return a != null && a.size() > 0;
        } catch (NameNotFoundException e) {
            return false; // el dominio no existe
        } catch (NamingException e) {
            // Error transitorio de DNS: no bloqueamos (fail-open).
            log.warn("No se pudo verificar el dominio '{}' por DNS: {}", dominio, e.getMessage());
            return true;
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException ignored) {
                    // nada que hacer
                }
            }
        }
    }
}
