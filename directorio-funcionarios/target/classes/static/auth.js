// ============================================================
// Login y registro - JavaScript puro (sin frameworks)
// ============================================================
const mensaje = document.getElementById("mensaje");

// ---------- Utilidades ----------
function mostrarMensaje(texto, tipo) {
    mensaje.textContent = texto;
    mensaje.className = "mensaje " + tipo; // exito | info | error
    mensaje.hidden = false;
}

function ocultarMensaje() {
    mensaje.hidden = true;
}

// Spring guarda el token CSRF en la cookie XSRF-TOKEN; hay que reenviarlo en una cabecera
function tokenCsrf() {
    const m = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
    return m ? decodeURIComponent(m[1]) : "";
}

async function leerJson(respuesta) {
    try {
        return await respuesta.json();
    } catch (e) {
        return {};
    }
}

function formatearTiempo(segundos) {
    const m = Math.floor(segundos / 60);
    const s = String(segundos % 60).padStart(2, "0");
    return `${m}:${s}`;
}

// Mostrar / ocultar contraseñas
const chkMostrar = document.getElementById("mostrarPassword");
if (chkMostrar) {
    chkMostrar.addEventListener("change", () => {
        document.querySelectorAll(".pw").forEach(i => {
            i.type = chkMostrar.checked ? "text" : "password";
        });
    });
}

// ============================================================
// LOGIN
// ============================================================
const formLogin = document.getElementById("formLogin");

if (formLogin) {
    const btnIngresar = document.getElementById("btnIngresar");
    let bloqueado = false;
    let temporizador = null;

    // Si ya hay sesión activa, ir directo al directorio
    fetch("/api/auth/me").then(r => { if (r.ok) location.replace("/index.html"); }).catch(() => {});

    const params = new URLSearchParams(location.search);
    if (params.has("registrado")) mostrarMensaje("Usuario creado correctamente. Ya puedes iniciar sesión.", "exito");
    if (params.has("salio")) mostrarMensaje("Cerraste sesión correctamente.", "info");

    // Bloquea el botón y muestra el tiempo que falta
    function iniciarCuentaRegresiva(segundos) {
        bloqueado = true;
        btnIngresar.disabled = true;
        const fin = Date.now() + segundos * 1000;
        clearInterval(temporizador);

        const pintar = () => {
            const restante = Math.max(0, Math.ceil((fin - Date.now()) / 1000));
            if (restante === 0) {
                clearInterval(temporizador);
                bloqueado = false;
                btnIngresar.disabled = false;
                mostrarMensaje("Ya puedes intentar iniciar sesión de nuevo.", "info");
                return;
            }
            mostrarMensaje("Cuenta bloqueada por demasiados intentos fallidos. Intenta de nuevo en "
                + formatearTiempo(restante) + ".", "error");
        };
        pintar();
        temporizador = setInterval(pintar, 1000);
    }

    formLogin.addEventListener("submit", async (evento) => {
        evento.preventDefault();
        if (bloqueado) return;

        const username = document.getElementById("username").value.trim();
        const password = document.getElementById("password").value;

        if (!username || !password) {
            mostrarMensaje("Escribe tu usuario y tu contraseña.", "error");
            return;
        }

        btnIngresar.disabled = true;
        try {
            const respuesta = await fetch("/login", {
                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded",
                    "X-XSRF-TOKEN": tokenCsrf(),
                },
                body: new URLSearchParams({ username, password }),
            });
            const data = await leerJson(respuesta);

            if (respuesta.ok && data.ok) {
                location.replace("/index.html");
                return;
            }
            if (respuesta.status === 403) {
                mostrarMensaje("La sesión de seguridad expiró. Recarga la página e inténtalo de nuevo.", "error");
            } else if (data.segundos > 0) {
                iniciarCuentaRegresiva(data.segundos);
                return;
            } else {
                mostrarMensaje(data.mensaje || "No fue posible iniciar sesión.", "error");
            }
        } catch (e) {
            mostrarMensaje("No se pudo conectar con el servidor.", "error");
        }
        if (!bloqueado) btnIngresar.disabled = false;
    });
}

// ============================================================
// REGISTRO
// ============================================================
const formRegistro = document.getElementById("formRegistro");

if (formRegistro) {
    const campos = {
        username: document.getElementById("username"),
        email: document.getElementById("email"),
        password: document.getElementById("password"),
        confirmarPassword: document.getElementById("confirmarPassword"),
    };
    const btnRegistrar = document.getElementById("btnRegistrar");
    const listaReglas = document.getElementById("reglas");

    // Mismas reglas que valida el servidor (PasswordValidator.java)
    const REGLAS = [
        { texto: "Entre 8 y 64 caracteres", ok: (p) => p.length >= 8 && p.length <= 64 },
        { texto: "Al menos una letra mayúscula", ok: (p) => /\p{Lu}/u.test(p) },
        { texto: "Al menos una letra minúscula", ok: (p) => /\p{Ll}/u.test(p) },
        { texto: "Al menos un número", ok: (p) => /\p{Nd}/u.test(p) },
        { texto: "Al menos un carácter especial (! @ # $ % & *)", ok: (p) => /[^\p{L}\p{Nd}\s]/u.test(p) },
        { texto: "Sin espacios", ok: (p) => !/\s/.test(p) },
        { texto: "No contiene tu nombre de usuario", ok: (p, u) => u.length < 3 || !p.toLowerCase().includes(u) },
    ];

    REGLAS.forEach(r => {
        r.li = document.createElement("li");
        r.li.textContent = r.texto;
        listaReglas.append(r.li);
    });

    // Devuelve true si se cumplen todas; pinta cada regla en verde/rojo
    function evaluarReglas() {
        const p = campos.password.value;
        const u = campos.username.value.trim().toLowerCase();
        let todas = true;
        REGLAS.forEach(r => {
            const cumple = r.ok(p, u);
            r.li.className = p === "" ? "" : (cumple ? "ok" : "falta");
            if (!cumple) todas = false;
        });
        return todas;
    }

    function limpiarErrores() {
        document.querySelectorAll("[data-error-for]").forEach(e => e.replaceChildren());
    }

    // errores = { campo: ["mensaje", ...] }
    function pintarErrores(errores) {
        Object.entries(errores).forEach(([campo, lista]) => {
            const destino = document.querySelector(`[data-error-for="${campo}"]`);
            if (!destino) return;
            lista.forEach(texto => {
                const linea = document.createElement("span");
                linea.textContent = texto;
                destino.append(linea);
            });
        });
    }

    campos.password.addEventListener("input", evaluarReglas);
    campos.username.addEventListener("input", evaluarReglas);

    formRegistro.addEventListener("submit", async (evento) => {
        evento.preventDefault();
        limpiarErrores();
        ocultarMensaje();

        const datos = {
            username: campos.username.value.trim(),
            email: campos.email.value.trim(),
            password: campos.password.value,
            confirmarPassword: campos.confirmarPassword.value,
        };

        // Validación rápida en el navegador (el servidor vuelve a validar todo)
        const errores = {};
        if (!datos.username) errores.username = ["El usuario es obligatorio."];
        if (!datos.email) errores.email = ["El correo es obligatorio."];
        if (!datos.password) errores.password = ["La contraseña es obligatoria."];
        else if (!evaluarReglas()) errores.password = ["La contraseña no cumple todos los requisitos."];
        if (datos.password !== datos.confirmarPassword) errores.confirmarPassword = ["Las contraseñas no coinciden."];

        if (Object.keys(errores).length > 0) {
            pintarErrores(errores);
            mostrarMensaje("Revisa los campos marcados.", "error");
            return;
        }

        btnRegistrar.disabled = true;
        try {
            const respuesta = await fetch("/api/auth/register", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "X-XSRF-TOKEN": tokenCsrf(),
                },
                body: JSON.stringify(datos),
            });
            const data = await leerJson(respuesta);

            if (respuesta.status === 201) {
                location.replace("/login.html?registrado=1");
                return;
            }
            if (data.errores) {
                pintarErrores(data.errores);
                mostrarMensaje(respuesta.status === 409 ? "Ese usuario o correo ya está registrado."
                                                        : "Revisa los campos marcados.", "error");
            } else if (respuesta.status === 403) {
                mostrarMensaje("La sesión de seguridad expiró. Recarga la página e inténtalo de nuevo.", "error");
            } else {
                mostrarMensaje("No se pudo crear el usuario (código " + respuesta.status + ").", "error");
            }
        } catch (e) {
            mostrarMensaje("No se pudo conectar con el servidor.", "error");
        }
        btnRegistrar.disabled = false;
    });
}
