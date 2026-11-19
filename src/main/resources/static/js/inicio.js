/* Pantalla de inicio: simulación del inicio de sesión (CU 5) y menú de opciones. */

const elementos = {
  aviso: document.getElementById("aviso"),
  bajada: document.getElementById("bajada"),
  seccionUsuarios: document.getElementById("seccion-usuarios"),
  listaEncargados: document.getElementById("lista-encargados"),
  listaOtros: document.getElementById("lista-otros"),
  seccionMenu: document.getElementById("seccion-menu"),
  saludo: document.getElementById("menu-saludo"),
  cm: document.getElementById("menu-cm"),
  opciones: document.getElementById("menu-opciones"),
  sinOpciones: document.getElementById("menu-sin-opciones"),
  cerrarSesion: document.getElementById("boton-cerrar-sesion"),
};

function mostrarAviso(texto, tipo) {
  elementos.aviso.textContent = texto;
  elementos.aviso.className = "alerta alerta-" + tipo;
  elementos.aviso.hidden = false;
}

function avisoDesdeUrl() {
  const parametros = new URLSearchParams(window.location.search);
  if (parametros.has("cancelado")) mostrarAviso("Cancelaste la consulta de seguimiento de bolsines.", "info");
  if (parametros.has("finalizado")) mostrarAviso("La consulta de seguimiento terminó y el correo quedó enviado.", "exito");
  if (parametros.has("cancelado") || parametros.has("finalizado")) {
    history.replaceState(null, "", "index.html");
  }
}

function crearItemUsuario(usuario) {
  const li = document.createElement("li");
  li.innerHTML =
    '<button class="usuario" type="button">' +
    "<strong>" + escaparHtml(usuario.nombreCompleto) + "</strong>" +
    "<span>" + escaparHtml(usuario.rol) + "</span>" +
    "<em>" + escaparHtml(usuario.cmNombre) + "</em>" +
    "</button>";
  li.querySelector("button").addEventListener("click", () => ingresar(usuario.idEmpleado));
  return li;
}

async function mostrarUsuarios() {
  const usuarios = await Api.get("/api/sesion/usuarios");
  elementos.listaEncargados.replaceChildren(...usuarios.filter((u) => u.esEncargadoDeBolsines).map(crearItemUsuario));
  elementos.listaOtros.replaceChildren(...usuarios.filter((u) => !u.esEncargadoDeBolsines).map(crearItemUsuario));
  elementos.seccionMenu.hidden = true;
  elementos.seccionUsuarios.hidden = false;
  elementos.bajada.hidden = false;
}

function mostrarMenu(usuario) {
  elementos.saludo.textContent = "Hola, " + usuario.nombreCompleto.split(" ")[0];
  elementos.cm.textContent = usuario.rol + " en " + usuario.cmNombre;
  elementos.opciones.replaceChildren();
  if (usuario.esEncargadoDeBolsines) {
    // Paso 1 del CU 36: el EB selecciona la opción para consultar la ubicación de los bolsines
    const opcion = document.createElement("a");
    opcion.className = "opcion-principal";
    opcion.href = "seguimiento.html";
    opcion.innerHTML = "<strong>Consultar seguimiento de bolsines</strong><span>Ver en el mapa dónde están los bolsines enviados</span>";
    elementos.opciones.append(opcion);
  }
  elementos.sinOpciones.hidden = usuario.esEncargadoDeBolsines;
  elementos.seccionUsuarios.hidden = true;
  elementos.bajada.hidden = true;
  elementos.seccionMenu.hidden = false;
}

async function ingresar(idEmpleado) {
  try {
    const usuario = await Api.post("/api/sesion/" + idEmpleado);
    elementos.aviso.hidden = true;
    mostrarMenu(usuario);
  } catch (error) {
    mostrarAviso(error.message, "advertencia");
  }
}

elementos.cerrarSesion.addEventListener("click", async () => {
  await Api.post("/api/sesion/cierre");
  elementos.aviso.hidden = true;
  await mostrarUsuarios();
});

(async function iniciar() {
  avisoDesdeUrl();
  try {
    const usuario = await Api.get("/api/sesion");
    mostrarMenu(usuario);
  } catch (error) {
    if (error.status === 401) {
      await mostrarUsuarios();
    } else {
      mostrarAviso("No se pudo conectar con el sistema. Revisá que la aplicación esté en ejecución.", "advertencia");
    }
  }
})();
