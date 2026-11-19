/* Llamadas al backend compartidas por las pantallas. */

const Api = {
  async pedir(metodo, url) {
    const respuesta = await fetch(url, { method: metodo, headers: { Accept: "application/json" } });
    let cuerpo = null;
    try {
      cuerpo = await respuesta.json();
    } catch (e) {
      cuerpo = null;
    }
    if (respuesta.status === 401 && !url.startsWith("/api/sesion")) {
      window.location.href = "index.html";
      throw new Error("Sesión no iniciada");
    }
    if (!respuesta.ok) {
      const error = new Error((cuerpo && cuerpo.mensaje) || "El servidor respondió con un error " + respuesta.status + ".");
      error.status = respuesta.status;
      throw error;
    }
    return cuerpo;
  },
  get(url) { return this.pedir("GET", url); },
  post(url) { return this.pedir("POST", url); },
};

/** Clase CSS del precinto según el modelo de GPS Tracker (un color por adaptador). */
function claseModelo(modelo) {
  switch (modelo) {
    case "XTR-4500L": return "modelo-xtr";
    case "NavTrack QX-7A": return "modelo-nav";
    case "GeoPulse MTR-900": return "modelo-geo";
    default: return "modelo-otro";
  }
}

function escaparHtml(texto) {
  return String(texto ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}
