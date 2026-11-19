/*
 * Vista de la PantallaConsultarSeguimientoBolsines (CU 36).
 * Cada función corresponde a un mensaje de la pantalla en el diagrama de secuencia.
 *
 * Los controles de la interfaz (objeto ui) siguen los prefijos de la cátedra:
 * lbl = etiqueta, txt = campo de texto, cmb = desplegable, btn = botón, lst = lista,
 * dlg = diálogo, pnl = panel, frm = formulario.
 */

const ui = {
  lblUsuarioNombre: document.getElementById("usuario-nombre"),
  lblUsuarioCM: document.getElementById("usuario-rol"),
  lblCMUsuario: document.getElementById("cm-usuario"),
  lblResumen: document.getElementById("resumen"),
  frmFiltros: document.getElementById("form-filtros"),
  txtPrecinto: document.getElementById("filtro-precinto"),
  cmbCMDestino: document.getElementById("filtro-cm"),
  btnQuitarFiltros: document.getElementById("boton-quitar-filtros"),
  pnlListas: document.getElementById("zona-listas"),
  lblMensaje: document.getElementById("mensaje"),
  lblTituloUbicados: document.getElementById("titulo-ubicados"),
  lstBolsinesUbicados: document.getElementById("lista-ubicados"),
  lblTituloSinUbicacion: document.getElementById("titulo-sin-ubicacion"),
  lstBolsinesSinUbicacion: document.getElementById("lista-sin-ubicacion"),
  pnlSinBolsines: document.getElementById("sin-bolsines"),
  lblSinBolsines: document.getElementById("sin-bolsines-texto"),
  pnlCargando: document.getElementById("cargando"),
  btnCancelar: document.getElementById("boton-cancelar"),
  dlgConfirmacionCorreo: document.getElementById("dialogo-correo"),
  lblPrecintoDialogo: document.getElementById("dialogo-precinto"),
  lblPregunta: document.getElementById("dialogo-correo-titulo"),
  lblDatoPrecinto: document.getElementById("dato-precinto"),
  lblDatoDestino: document.getElementById("dato-destino"),
  lblDatoFecha: document.getElementById("dato-fecha"),
  lblDatoCoordenadas: document.getElementById("dato-coordenadas"),
  btnNoEnviar: document.getElementById("boton-no-enviar"),
  btnEnviarCorreo: document.getElementById("boton-enviar"),
  dlgResultado: document.getElementById("dialogo-resultado"),
  lblResultadoMensaje: document.getElementById("resultado-mensaje"),
  lblResultadoDestinatario: document.getElementById("resultado-destinatario"),
  lblResultadoCopia: document.getElementById("resultado-copia"),
  btnFinalizar: document.getElementById("boton-finalizar"),
};

const estado = {
  mapa: null,
  bolsinesEnMapa: [],
  numeroSeleccionado: null,
  respondiendo: false,
};

// ---------------------------------------------------------------------------
// Pasos 1 a 6
// ---------------------------------------------------------------------------

async function habilitarPantalla() {
  try {
    const usuario = await Api.get("/api/sesion");
    ui.lblUsuarioNombre.textContent = usuario.nombreCompleto;
    ui.lblUsuarioCM.textContent = usuario.cmNombre;
  } catch (error) {
    window.location.href = "index.html";
    return;
  }
  estado.mapa = await Mapas.crear(document.getElementById("mapa"));
  await opcionConsultarSeguimientoBolsines();
}

/** Paso 1: el EB seleccionó la opción. El backend resuelve los pasos 2 a 5. */
async function opcionConsultarSeguimientoBolsines() {
  ui.pnlCargando.hidden = false;
  try {
    const resultado = await Api.post("/api/seguimiento/opcion");
    mostrarResultado(resultado, true);
  } catch (error) {
    mostrarMensaje(error.message, "advertencia");
  } finally {
    ui.pnlCargando.hidden = true;
  }
}

function mostrarResultado(resultado, esConsultaInicial) {
  mostrarCMUsuario(resultado);
  if (!resultado.hayBolsinesEnviados) {
    mostrarSinBolsinesEnviados(resultado);   // flujo A1
    return;
  }
  if (esConsultaInicial) {
    cargarOpcionesCMDestino(resultado.cmsDestino);
    mostrarResumen(resultado);
  }
  mostrarBolsinesEnLista(resultado.bolsinesUbicados, resultado.bolsinesSinUbicacion);
  mostrarBolsinesEnMapa(resultado.bolsinesUbicados);   // paso 6
  mostrarMensaje(resultado.mensaje, resultado.tipoMensaje);
}

function mostrarCMUsuario(resultado) {
  ui.lblCMUsuario.textContent = resultado.cmUsuarioNombre;
}

function mostrarResumen(resultado) {
  const ubicados = resultado.bolsinesUbicados.length;
  const sinUbicacion = resultado.bolsinesSinUbicacion.length;
  let texto = ubicados === 1 ? "1 bolsín en el mapa" : ubicados + " bolsines en el mapa";
  if (sinUbicacion > 0) texto += " y " + sinUbicacion + " sin ubicación";
  ui.lblResumen.textContent = texto + ".";
}

function cargarOpcionesCMDestino(cms) {
  const opciones = cms.map((cm) => {
    const opcion = document.createElement("option");
    opcion.value = cm.codigo;
    opcion.textContent = cm.nombre;
    return opcion;
  });
  ui.cmbCMDestino.replaceChildren(new Option("Todas", ""), ...opciones);
}

function mostrarSinBolsinesEnviados(resultado) {
  ui.frmFiltros.hidden = true;
  ui.pnlListas.hidden = true;
  ui.lblResumen.textContent = "";
  ui.lblSinBolsines.textContent = resultado.mensaje;
  ui.pnlSinBolsines.hidden = false;
  estado.mapa.mostrarBolsines([], () => {});
}

function mostrarBolsinesEnLista(ubicados, sinUbicacion) {
  ui.lblTituloUbicados.textContent = ubicados.length ? "En el mapa" : "En el mapa: ninguno";
  ui.lstBolsinesUbicados.replaceChildren(...ubicados.map((b) => {
    const li = document.createElement("li");
    li.innerHTML =
      '<button type="button" class="bolsin" data-numero="' + b.numeroBolsin + '">' +
      '<span class="precinto ' + claseModelo(b.modeloGPSTracker) + '">' + b.numeroBolsin + "</span>" +
      '<span class="destino">' + escaparHtml(b.cmDestinoNombre) + "</span>" +
      '<span class="detalle num">Precinto ' + escaparHtml(b.numeroPrecinto) + ", " + escaparHtml(b.modeloGPSTracker) + "</span>" +
      '<span class="detalle num">Actualizado el ' + escaparHtml(b.fechaHoraUltimaActualizacion) + "</span>" +
      "</button>";
    li.querySelector("button").addEventListener("click", () => tomarSeleccionBolsin(b.numeroBolsin));
    return li;
  }));

  // Flujo A3: bolsines cuyo GPS Tracker no pudo informar la ubicación
  ui.lblTituloSinUbicacion.hidden = sinUbicacion.length === 0;
  ui.lstBolsinesSinUbicacion.replaceChildren(...sinUbicacion.map((b) => {
    const li = document.createElement("li");
    li.innerHTML =
      '<div class="bolsin bolsin-sin-ubicacion">' +
      '<span class="precinto apagado">' + b.numeroBolsin + "</span>" +
      '<span class="destino">' + escaparHtml(b.cmDestinoNombre) + "</span>" +
      '<span class="detalle num">Precinto ' + escaparHtml(b.numeroPrecinto) + ", " + escaparHtml(b.modeloGPSTracker) + "</span>" +
      '<span class="motivo">' + escaparHtml(b.motivo) + "</span>" +
      "</div>";
    return li;
  }));
}

function mostrarBolsinesEnMapa(bolsines) {
  estado.bolsinesEnMapa = bolsines;
  estado.mapa.mostrarBolsines(bolsines, tomarSeleccionBolsin);
}

function mostrarMensaje(texto, tipo) {
  if (!texto) {
    ui.lblMensaje.hidden = true;
    return;
  }
  ui.lblMensaje.textContent = texto;
  ui.lblMensaje.className = "alerta alerta-" + (tipo || "info");
  ui.lblMensaje.hidden = false;
}

/** Paso 6: filtro por número de precinto o CM destino (A2 si el precinto no existe). */
async function tomarFiltros(evento) {
  evento.preventDefault();
  const precinto = ui.txtPrecinto.value.trim();
  if (precinto && !/^\d+$/.test(precinto)) {
    mostrarMensaje("El número de precinto sólo puede tener dígitos.", "advertencia");
    ui.txtPrecinto.focus();
    return;
  }
  const parametros = new URLSearchParams();
  if (precinto) parametros.set("precinto", precinto);
  if (ui.cmbCMDestino.value) parametros.set("cmDestino", ui.cmbCMDestino.value);
  try {
    const resultado = await Api.get("/api/seguimiento/filtro?" + parametros.toString());
    mostrarResultado(resultado, false);
  } catch (error) {
    mostrarMensaje(error.message, "advertencia");
  }
}

async function quitarFiltros() {
  ui.txtPrecinto.value = "";
  ui.cmbCMDestino.value = "";
  const resultado = await Api.get("/api/seguimiento/filtro");
  mostrarResultado(resultado, false);
}

// ---------------------------------------------------------------------------
// Pasos 7 a 11
// ---------------------------------------------------------------------------

function marcarSeleccion(numero) {
  estado.numeroSeleccionado = numero;
  document.querySelectorAll(".bolsin[data-numero]").forEach((boton) => {
    boton.classList.toggle("seleccionado", Number(boton.dataset.numero) === numero);
  });
  estado.mapa.resaltar(numero);
}

/** Paso 7: el EB selecciona un bolsín del mapa (o de la lista). */
async function tomarSeleccionBolsin(numeroBolsin) {
  try {
    const consulta = await Api.post("/api/seguimiento/seleccion/" + numeroBolsin);
    marcarSeleccion(numeroBolsin);
    solicitarConfirmacionEnvioCorreo(consulta);
  } catch (error) {
    mostrarMensaje(error.message, "advertencia");
  }
}

/** Paso 8: consulta si se desea enviar el correo al Gerente de la CM destino. */
function solicitarConfirmacionEnvioCorreo(consulta) {
  const bolsin = estado.bolsinesEnMapa.find((b) => b.numeroBolsin === consulta.numeroBolsin);
  ui.lblPrecintoDialogo.className = "precinto " + claseModelo(bolsin && bolsin.modeloGPSTracker);
  ui.lblPrecintoDialogo.textContent = consulta.numeroBolsin;
  ui.lblPregunta.textContent = consulta.pregunta;
  ui.lblDatoPrecinto.textContent = consulta.numeroPrecinto;
  ui.lblDatoDestino.textContent = consulta.cmDestinoNombre;
  ui.lblDatoFecha.textContent = consulta.fechaHoraUltimaActualizacion;
  ui.lblDatoCoordenadas.textContent = consulta.latitud.toFixed(4) + ", " + consulta.longitud.toFixed(4);
  ui.dlgConfirmacionCorreo.showModal();
  ui.btnEnviarCorreo.focus();
}

/** Paso 9 (confirma) o flujo A5 (no confirma). */
async function tomarConfirmacionEnvioCorreo(confirma) {
  if (estado.respondiendo) return;
  estado.respondiendo = true;
  ui.btnEnviarCorreo.disabled = true;
  ui.btnNoEnviar.disabled = true;
  try {
    const resultado = await Api.post("/api/seguimiento/confirmacion-correo?confirma=" + confirma);
    ui.dlgConfirmacionCorreo.close();
    if (resultado.finDelCasoDeUso) {
      informarEnvioExitoso(resultado);   // paso 11
    } else {
      mostrarMensaje(resultado.mensaje, confirma ? "advertencia" : "info");
      marcarSeleccion(null);
    }
  } catch (error) {
    ui.dlgConfirmacionCorreo.close();
    mostrarMensaje(error.message, "advertencia");
  } finally {
    estado.respondiendo = false;
    ui.btnEnviarCorreo.disabled = false;
    ui.btnNoEnviar.disabled = false;
  }
}

/** Paso 11: informa la ejecución exitosa. Fin del caso de uso. */
function informarEnvioExitoso(resultado) {
  ui.lblResultadoMensaje.textContent = resultado.mensaje;
  ui.lblResultadoDestinatario.textContent = resultado.destinatario;
  ui.lblResultadoCopia.textContent = "Copia del correo: " + resultado.copiaGuardadaEn;
  ui.dlgResultado.showModal();
  ui.btnFinalizar.focus();
}

/** Flujo A4: el EB cancela la ejecución del caso de uso. */
async function opcionCancelar() {
  try {
    await Api.post("/api/seguimiento/cancelacion");
  } finally {
    window.location.href = "index.html?cancelado";
  }
}

// ---------------------------------------------------------------------------
// Eventos
// ---------------------------------------------------------------------------
ui.frmFiltros.addEventListener("submit", tomarFiltros);
ui.btnQuitarFiltros.addEventListener("click", quitarFiltros);
ui.btnEnviarCorreo.addEventListener("click", () => tomarConfirmacionEnvioCorreo(true));
ui.btnNoEnviar.addEventListener("click", () => tomarConfirmacionEnvioCorreo(false));
ui.dlgConfirmacionCorreo.addEventListener("cancel", (evento) => {   // tecla Esc = "No enviar"
  evento.preventDefault();
  tomarConfirmacionEnvioCorreo(false);
});
ui.btnFinalizar.addEventListener("click", () => { window.location.href = "index.html?finalizado"; });
ui.dlgResultado.addEventListener("cancel", (evento) => evento.preventDefault());
ui.btnCancelar.addEventListener("click", opcionCancelar);

habilitarPantalla();
