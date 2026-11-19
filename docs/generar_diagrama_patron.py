"""Genera 01b-clases-patron-adapter.puml copiando TEXTUALMENTE las clases del diagrama completo,
para que la vista enfocada en el patrón sea idéntica en firmas a la vista completa."""
import re
import sys

completo = open(sys.argv[1], encoding="utf-8").read()
salida = sys.argv[2]

CLASES = [
    "GestorConsultarSeguimientoBolsines", "IAdaptadorGPSTracker",
    "AdaptadorGPSTrackerXTR4500L", "AdaptadorGPSTrackerNavTrackQX7A", "AdaptadorGPSTrackerGeoPulseMTR900",
    "ServicioGPSTrackerXTR4500L", "ServicioGPSTrackerNavTrackQX7A", "ServicioGPSTrackerGeoPulseMTR900",
    "UbicacionBolsin", "Bolsin", "GPSTracker", "ComisionMedica",
]

bloques = {}
for m in re.finditer(r"^  (class|interface) (\w+)[^\n]*\{\n.*?^  \}\n", completo, re.S | re.M):
    bloques[m.group(2)] = m.group(0)

faltan = [c for c in CLASES if c not in bloques]
if faltan:
    sys.exit("No se encontraron: " + ", ".join(faltan))

relaciones = []
for linea in completo.splitlines():
    m = re.match(r"^(\w+) .*?(-->|\.\.>|<\|\.\.|\*--) (?:\"[^\"]*\" )?(\w+)", linea)
    if m and m.group(1) in CLASES and m.group(3) in CLASES:
        relaciones.append(linea)

notas = []
for m in re.finditer(r"^note right of (\w+)\n.*?^end note\n", completo, re.S | re.M):
    if m.group(1) in ("IAdaptadorGPSTracker", "GestorConsultarSeguimientoBolsines",
                      "ServicioGPSTrackerXTR4500L", "GPSTracker"):
        notas.append(m.group(0))

cabecera = completo[: completo.index("' ====")]
cabecera = cabecera.replace("@startuml 01-clases-diseno-adapter", "@startuml 01b-clases-patron-adapter")
cabecera = cabecera.replace("Vista estática de diseño (patrón Adapter)",
                            "Estructura del patrón Adapter (extracto de la vista estática completa)")

partes = [cabecera]
partes.append("".join(bloques[c] for c in CLASES))
partes.append("".join(notas))
nota_alcance = re.search(r'^note ".*?" as NotaAlcance$', completo, re.M)
if nota_alcance:
    partes.append(nota_alcance.group(0) + "\n")
partes.append("\n".join(relaciones) + "\n@enduml\n")
open(salida, "w", encoding="utf-8").write("\n".join(partes))
print(len(CLASES), "clases,", len(relaciones), "relaciones,", len(notas), "notas")
