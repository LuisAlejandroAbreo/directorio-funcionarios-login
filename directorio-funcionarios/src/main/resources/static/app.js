// ============================================================
// Directorio de funcionarios - JavaScript puro (sin frameworks)
// ============================================================
const API = "/api/v1/funcionarios";

const tbody = document.getElementById("tbody");
const mensaje = document.getElementById("mensaje");
const tituloFormulario = document.getElementById("tituloFormulario");

const campos = {
    id: document.getElementById("id"),
    nombre: document.getElementById("nombre"),
    cargo: document.getElementById("cargo"),
    departamento: document.getElementById("departamento"),
    telefono: document.getElementById("telefono"),
};

// ---------- Mensajes ----------
function mostrarMensaje(texto, tipo) {
    mensaje.textContent = texto;
    mensaje.className = "mensaje " + tipo; // exito | info | error
    mensaje.hidden = false;
}

function ocultarMensaje() {
    mensaje.hidden = true;
}

// ---------- Tabla ----------
function celda(texto) {
    const td = document.createElement("td");
    td.textContent = texto ?? "";   // textContent evita inyección de HTML
    return td;
}

function pintarTabla(funcionarios) {
    tbody.replaceChildren();
    funcionarios.forEach(f => {
        const tr = document.createElement("tr");
        tr.append(celda(f.id), celda(f.nombre), celda(f.cargo),
                  celda(f.departamento), celda(f.telefono));

        const tdAcc = document.createElement("td");
        tdAcc.className = "acciones-celda";

        const btnEditar = document.createElement("button");
        btnEditar.textContent = "Editar";
        btnEditar.addEventListener("click", () => editar(f.id));

        const btnEliminar = document.createElement("button");
        btnEliminar.textContent = "Eliminar";
        btnEliminar.className = "peligro";
        btnEliminar.addEventListener("click", () => eliminar(f.id, f.nombre));

        tdAcc.append(btnEditar, btnEliminar);
        tr.append(tdAcc);
        tbody.append(tr);
    });
}

// ---------- Consumo de la API ----------
// Spring guarda el token CSRF en la cookie XSRF-TOKEN; hay que reenviarlo en POST/PUT/DELETE
function tokenCsrf() {
    const m = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
    return m ? decodeURIComponent(m[1]) : "";
}

// Devuelve { status, data }. Un 204 no trae cuerpo.
async function llamarApi(url, opciones = {}) {
    const metodo = (opciones.method || "GET").toUpperCase();
    const headers = { ...(opciones.headers || {}) };
    if (metodo !== "GET") headers["X-XSRF-TOKEN"] = tokenCsrf();

    const respuesta = await fetch(url, { ...opciones, headers });

    // Sesión vencida o inexistente: volver al login (la promesa no se resuelve para no mostrar errores)
    if (respuesta.status === 401) {
        window.location.replace("/login.html");
        return new Promise(() => {});
    }

    let data = null;
    if (respuesta.status !== 204) {
        const texto = await respuesta.text();
        try { data = texto ? JSON.parse(texto) : null; } catch (e) { data = null; }
    }
    return { status: respuesta.status, data };
}

// Carga una lista (completa o filtrada) y la muestra en la tabla
async function cargarLista(url, textoSinResultados, textoExito) {
    try {
        const { status, data } = await llamarApi(url);
        if (status === 200) {
            pintarTabla(data);
            if (textoExito) mostrarMensaje(textoExito(data.length), "exito");
            else ocultarMensaje();
        } else if (status === 204) {
            pintarTabla([]);
            mostrarMensaje(textoSinResultados, "info");
        } else {
            mostrarMensaje("Error al consultar el directorio (código " + status + ").", "error");
        }
    } catch (e) {
        mostrarMensaje("No se pudo conectar con el servidor.", "error");
    }
}

function verTodos() {
    return cargarLista(API, "El directorio está vacío.", null);
}

// ---------- Formulario ----------
function limpiarFormulario() {
    Object.values(campos).forEach(c => c.value = "");
    tituloFormulario.textContent = "Registrar funcionario";
}

function leerFormulario() {
    return {
        nombre: campos.nombre.value.trim(),
        cargo: campos.cargo.value.trim(),
        departamento: campos.departamento.value.trim(),
        telefono: campos.telefono.value.trim() || null,
    };
}

async function guardar(evento) {
    evento.preventDefault();
    const datos = leerFormulario();

    if (!datos.nombre || !datos.cargo || !datos.departamento) {
        mostrarMensaje("Nombre, cargo y departamento son obligatorios.", "error");
        return;
    }

    const id = campos.id.value;
    const editando = id !== "";

    try {
        const { status } = await llamarApi(editando ? `${API}/${id}` : API, {
            method: editando ? "PUT" : "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(datos),
        });

        if (status === 200 || status === 201) {
            mostrarMensaje(editando ? "Funcionario actualizado correctamente."
                                    : "Funcionario registrado correctamente.", "exito");
            limpiarFormulario();
            const lista = await llamarApi(API);
            pintarTabla(lista.status === 200 ? lista.data : []);
        } else if (status === 404) {
            mostrarMensaje("El funcionario ya no existe.", "error");
        } else if (status === 400) {
            mostrarMensaje("Datos inválidos: revisa los campos obligatorios.", "error");
        } else {
            mostrarMensaje("Error al guardar (código " + status + ").", "error");
        }
    } catch (e) {
        mostrarMensaje("No se pudo conectar con el servidor.", "error");
    }
}

// Abre el formulario con los datos cargados desde GET /{id}
async function editar(id) {
    try {
        const { status, data } = await llamarApi(`${API}/${id}`);
        if (status === 200) {
            campos.id.value = data.id;
            campos.nombre.value = data.nombre;
            campos.cargo.value = data.cargo;
            campos.departamento.value = data.departamento;
            campos.telefono.value = data.telefono ?? "";
            tituloFormulario.textContent = "Editar funcionario #" + data.id;
            ocultarMensaje();
            window.scrollTo({ top: 0, behavior: "smooth" });
        } else if (status === 404) {
            mostrarMensaje("El funcionario ya no existe.", "error");
            verTodos();
        } else {
            mostrarMensaje("Error al cargar el funcionario (código " + status + ").", "error");
        }
    } catch (e) {
        mostrarMensaje("No se pudo conectar con el servidor.", "error");
    }
}

async function eliminar(id, nombre) {
    // Si la persona cancela, el registro se conserva
    if (!confirm(`¿Eliminar a ${nombre}? Esta acción no se puede deshacer.`)) {
        return;
    }
    try {
        const { status } = await llamarApi(`${API}/${id}`, { method: "DELETE" });
        if (status === 204) {
            mostrarMensaje("Funcionario eliminado correctamente.", "exito");
            if (campos.id.value === String(id)) limpiarFormulario();
            const lista = await llamarApi(API);
            pintarTabla(lista.status === 200 ? lista.data : []);
        } else if (status === 404) {
            mostrarMensaje("El funcionario ya no existe.", "error");
            verTodos();
        } else {
            mostrarMensaje("Error al eliminar (código " + status + ").", "error");
        }
    } catch (e) {
        mostrarMensaje("No se pudo conectar con el servidor.", "error");
    }
}

// ---------- Búsquedas ----------
async function consultarPorId(evento) {
    evento.preventDefault();
    const id = document.getElementById("buscarId").value.trim();
    if (!id) {
        mostrarMensaje("Escribe un ID para consultar.", "error");
        return;
    }
    try {
        const { status, data } = await llamarApi(`${API}/${encodeURIComponent(id)}`);
        if (status === 200) {
            pintarTabla([data]);
            mostrarMensaje("Funcionario encontrado.", "exito");
        } else if (status === 404) {
            pintarTabla([]);
            mostrarMensaje("No existe un funcionario con el ID " + id + ".", "info");
        } else {
            mostrarMensaje("Error en la consulta (código " + status + ").", "error");
        }
    } catch (e) {
        mostrarMensaje("No se pudo conectar con el servidor.", "error");
    }
}

function buscarPorNombre(evento) {
    evento.preventDefault();
    const nombre = document.getElementById("buscarNombre").value.trim();
    if (!nombre) {
        mostrarMensaje("Escribe parte del nombre para buscar.", "error");
        return;
    }
    cargarLista(`${API}?nombre=${encodeURIComponent(nombre)}`,
        `No se encontraron funcionarios con el nombre "${nombre}".`,
        n => `Se encontraron ${n} resultado(s).`);
}

function filtrarPorDepartamento(evento) {
    evento.preventDefault();
    const dep = document.getElementById("buscarDepartamento").value.trim();
    if (!dep) {
        mostrarMensaje("Escribe el nombre completo del departamento.", "error");
        return;
    }
    cargarLista(`${API}?departamento=${encodeURIComponent(dep)}`,
        `No hay funcionarios en el departamento "${dep}".`,
        n => `Se encontraron ${n} resultado(s).`);
}

// ---------- Eventos ----------
document.getElementById("formFuncionario").addEventListener("submit", guardar);
document.getElementById("btnLimpiar").addEventListener("click", () => {
    limpiarFormulario();
    ocultarMensaje();
});
document.getElementById("formId").addEventListener("submit", consultarPorId);
document.getElementById("formNombre").addEventListener("submit", buscarPorNombre);
document.getElementById("formDepartamento").addEventListener("submit", filtrarPorDepartamento);
document.getElementById("btnVerTodos").addEventListener("click", () => {
    document.getElementById("buscarId").value = "";
    document.getElementById("buscarNombre").value = "";
    document.getElementById("buscarDepartamento").value = "";
    verTodos();
});

// ---------- Sesión ----------
document.getElementById("btnSalir").addEventListener("click", async () => {
    try {
        await fetch("/logout", { method: "POST", headers: { "X-XSRF-TOKEN": tokenCsrf() } });
    } finally {
        window.location.replace("/login.html?salio=1");
    }
});

fetch("/api/auth/me")
    .then(r => r.ok ? r.json() : null)
    .then(u => { if (u) document.getElementById("usuarioActual").textContent = "Usuario: " + u.username; })
    .catch(() => {});

// Al abrir la página se muestra el listado completo
verTodos();
