/*
 * InterfazMapa: interfaz con el actor secundario "Servidor Google Maps" (RNF 7).
 * - Si en application.properties hay una API key de Google Maps, se usa Google Maps.
 * - Si no, se usa Leaflet + OpenStreetMap (gratis, sin key).
 * Las dos implementaciones ofrecen las mismas funciones: mostrarBolsines() y resaltar().
 */

const CENTRO_ARGENTINA = { lat: -36.5, lng: -64.5 };
const COLORES_MODELO = {
  "XTR-4500L": "#1f7a80",
  "NavTrack QX-7A": "#4153a6",
  "GeoPulse MTR-900": "#9b3f7a",
};

/** Dibuja el precinto (etiqueta con el número del bolsín) que se usa como marcador. */
function svgPrecinto(numero, modelo, seleccionado) {
  const color = COLORES_MODELO[modelo] || "#6b7280";
  const borde = seleccionado ? '#e8b10a" stroke-width="3' : '#ffffff" stroke-width="1.5';
  return (
    '<svg xmlns="http://www.w3.org/2000/svg" width="62" height="30" viewBox="0 0 62 30">' +
    '<path d="M3 4 h41 a11 11 0 0 1 0 22 h-41 a2 2 0 0 1 -2 -2 v-18 a2 2 0 0 1 2 -2 z" fill="' + color + '" stroke="' + borde + '"/>' +
    '<circle cx="10" cy="15" r="3.5" fill="#ffffff"/>' +
    '<text x="34" y="20" text-anchor="middle" font-family="Archivo, Segoe UI, sans-serif" font-size="14" font-weight="800" fill="#ffffff">' +
    numero + "</text></svg>"
  );
}

/**
 * Si dos bolsines están prácticamente en el mismo punto (ej.: recién salidos de la misma CM),
 * sus etiquetas se apilan hacia arriba para que no se tapen. Devuelve número → posición en la pila.
 */
function calcularApilado(bolsines) {
  const porPunto = new Map();
  const apilado = new Map();
  bolsines.forEach((b) => {
    const punto = b.latitud.toFixed(1) + "," + b.longitud.toFixed(1);
    const posicion = porPunto.get(punto) || 0;
    apilado.set(b.numeroBolsin, posicion);
    porPunto.set(punto, posicion + 1);
  });
  return apilado;
}

const ALTO_ETIQUETA = 32;

const Mapas = {
  /** La InterfazMapa le pide su configuración (proveedor y API key) a su parte Java. */
  async crear(contenedor) {
    const configuracion = await Api.get("/api/mapa/configuracion");
    if (configuracion.proveedor === "google" && configuracion.apiKey) {
      try {
        return await crearMapaGoogle(contenedor, configuracion.apiKey);
      } catch (error) {
        console.warn("No se pudo cargar Google Maps; se usa OpenStreetMap.", error);
      }
    }
    return crearMapaLeaflet(contenedor);
  },
};

// ---------------------------------------------------------------------------
// Leaflet + OpenStreetMap
// ---------------------------------------------------------------------------
function crearMapaLeaflet(contenedor) {
  const mapa = L.map(contenedor, { zoomControl: true }).setView([CENTRO_ARGENTINA.lat, CENTRO_ARGENTINA.lng], 5);
  L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
    maxZoom: 18,
    attribution: '&copy; colaboradores de <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
  }).addTo(mapa);

  let marcadores = new Map();
  let bolsinesActuales = [];
  let apilado = new Map();

  function icono(bolsin, seleccionado) {
    return L.divIcon({
      className: "marcador-bolsin",
      html: svgPrecinto(bolsin.numeroBolsin, bolsin.modeloGPSTracker, seleccionado),
      iconSize: [62, 30],
      iconAnchor: [6, 15 + apilado.get(bolsin.numeroBolsin) * ALTO_ETIQUETA],
    });
  }

  return {
    mostrarBolsines(bolsines, alSeleccionar) {
      marcadores.forEach((m) => m.remove());
      marcadores = new Map();
      bolsinesActuales = bolsines;
      apilado = calcularApilado(bolsines);
      bolsines.forEach((b) => {
        const marcador = L.marker([b.latitud, b.longitud], { icon: icono(b, false), title: "Bolsín " + b.numeroBolsin, keyboard: true })
          .bindTooltip("<strong>" + escaparHtml(b.cmDestinoNombre) + "</strong><br>Actualizado el " + escaparHtml(b.fechaHoraUltimaActualizacion),
            { direction: "top", offset: [24, -14] })
          .on("click", () => alSeleccionar(b.numeroBolsin))
          .addTo(mapa);
        marcadores.set(b.numeroBolsin, marcador);
      });
      if (bolsines.length === 1) {
        mapa.setView([bolsines[0].latitud, bolsines[0].longitud], 11);
      } else if (bolsines.length > 1) {
        mapa.fitBounds(L.latLngBounds(bolsines.map((b) => [b.latitud, b.longitud])), { padding: [60, 60], maxZoom: 12 });
      } else {
        mapa.setView([CENTRO_ARGENTINA.lat, CENTRO_ARGENTINA.lng], 5);
      }
    },
    resaltar(numero) {
      bolsinesActuales.forEach((b) => {
        const marcador = marcadores.get(b.numeroBolsin);
        const elegido = b.numeroBolsin === numero;
        marcador.setIcon(icono(b, elegido));
        marcador.setZIndexOffset(elegido ? 1000 : 0);
        if (elegido) mapa.panTo([b.latitud, b.longitud]);
      });
    },
  };
}

// ---------------------------------------------------------------------------
// Google Maps
// ---------------------------------------------------------------------------
function cargarScriptGoogle(apiKey) {
  return new Promise((resolver, rechazar) => {
    window.__iniciarGoogleMaps = resolver;
    const script = document.createElement("script");
    script.src = "https://maps.googleapis.com/maps/api/js?key=" + encodeURIComponent(apiKey) + "&callback=__iniciarGoogleMaps&loading=async";
    script.async = true;
    script.onerror = rechazar;
    document.head.append(script);
  });
}

async function crearMapaGoogle(contenedor, apiKey) {
  await cargarScriptGoogle(apiKey);
  const mapa = new google.maps.Map(contenedor, {
    center: CENTRO_ARGENTINA, zoom: 5, mapTypeControl: false, streetViewControl: false, fullscreenControl: false,
  });
  let marcadores = new Map();
  let bolsinesActuales = [];
  let apilado = new Map();

  function icono(bolsin, seleccionado) {
    return {
      url: "data:image/svg+xml;charset=UTF-8," + encodeURIComponent(svgPrecinto(bolsin.numeroBolsin, bolsin.modeloGPSTracker, seleccionado)),
      anchor: new google.maps.Point(6, 15 + apilado.get(bolsin.numeroBolsin) * ALTO_ETIQUETA),
    };
  }

  return {
    mostrarBolsines(bolsines, alSeleccionar) {
      marcadores.forEach((m) => m.setMap(null));
      marcadores = new Map();
      bolsinesActuales = bolsines;
      apilado = calcularApilado(bolsines);
      const limites = new google.maps.LatLngBounds();
      bolsines.forEach((b) => {
        const posicion = { lat: b.latitud, lng: b.longitud };
        const marcador = new google.maps.Marker({
          position: posicion, map: mapa, icon: icono(b, false),
          title: "Bolsín " + b.numeroBolsin + " hacia " + b.cmDestinoNombre,
        });
        marcador.addListener("click", () => alSeleccionar(b.numeroBolsin));
        marcadores.set(b.numeroBolsin, marcador);
        limites.extend(posicion);
      });
      if (bolsines.length === 1) {
        mapa.setCenter({ lat: bolsines[0].latitud, lng: bolsines[0].longitud });
        mapa.setZoom(11);
      } else if (bolsines.length > 1) {
        mapa.fitBounds(limites, 60);
      } else {
        mapa.setCenter(CENTRO_ARGENTINA);
        mapa.setZoom(5);
      }
    },
    resaltar(numero) {
      bolsinesActuales.forEach((b) => {
        const marcador = marcadores.get(b.numeroBolsin);
        const elegido = b.numeroBolsin === numero;
        marcador.setIcon(icono(b, elegido));
        marcador.setZIndex(elegido ? 1000 : 1);
        if (elegido) mapa.panTo({ lat: b.latitud, lng: b.longitud });
      });
    },
  };
}
