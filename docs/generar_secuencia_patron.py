"""Genera 02b-secuencia-patron-adapter.puml copiando TEXTUALMENTE los mensajes de los pasos 4 y 5 del
diagrama de secuencia completo, para que la vista dinámica del patrón sea idéntica a la del CU.

Uso:  python generar_secuencia_patron.py 02-secuencia-cu36.puml 02b-secuencia-patron-adapter.puml
"""
import re
import sys

completo = open(sys.argv[1], encoding="utf-8").read()
salida = sys.argv[2]

inicio = completo.index("== Pasos 4 y 5")
fin = completo.index("== Paso 6")
seccion = completo[completo.index("\n", inicio) + 1: fin].rstrip() + "\n"

# Participantes que intervienen en la sección, en el mismo orden que en el diagrama completo.
usados = set()
for m in re.finditer(r"^\s*(\w+) -?->? (\w+) :", seccion, re.M):
    usados.update(m.groups())
usados.update(re.findall(r"^\s*create (\w+)", seccion, re.M))
participantes = [linea for linea in completo.splitlines()
                 if re.match(r'^(actor|boundary|control|entity|participant) ".*" as (\w+)', linea)
                 and re.match(r'^.* as (\w+)', linea).group(1) in usados]

cabecera = completo[: completo.index("\nactor ") + 1]
cabecera = cabecera.replace("@startuml 02-secuencia-cu36", "@startuml 02b-secuencia-patron-adapter")
cabecera = re.sub(r"^title .*$",
                  r"title CU 36 Consultar seguimiento de bolsines - Aplicación del patrón Adapter\\n"
                  r"Pasos 4 y 5 del flujo básico (extracto de la vista dinámica completa; GPS Tracker modelo XTR-4500L)",
                  cabecera, count=1, flags=re.M)

nota = """note over G
  Extracto de la vista dinámica completa: el gestor
  (Cliente) ya buscó los bolsines Enviados de la CM del
  usuario (pasos 2 y 3) y ahora pide la ubicación de cada
  uno a través de la interfaz IAdaptadorGPSTracker (Objetivo).
  El adaptador traduce el pedido a la API del modelo del
  GPS Tracker (Adaptable) y devuelve un UbicacionBolsin.
end note
"""

texto = (cabecera + "\n".join(participantes) + "\n\n" + nota + "activate G\n" + seccion
         + "deactivate G\n@enduml\n")
open(salida, "w", encoding="utf-8").write(texto)
print(len(participantes), "participantes,", len(re.findall(r" -?-> ", seccion)), "mensajes")
