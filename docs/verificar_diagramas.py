"""Contrasta los diagramas PlantUML con el código fuente (Java, JavaScript y HTML) y con las
convenciones de modelado de la cátedra.

Uso:  python docs/verificar_diagramas.py .      (desde la carpeta del proyecto)

Diagrama de clases:
  - cada método existe en la clase, con los mismos parámetros (nombre y tipo) y el mismo retorno
    (new() se compara con el constructor de Java);
  - cada atributo existe con el mismo tipo; los controles de la pantalla existen en el objeto ui de
    seguimiento.js y su tipo coincide con el elemento HTML de seguimiento.html;
  - al revés: ningún método ni atributo del código de las clases del CU falta en el diagrama
    (salvo el identificador técnico "id" de JPA, las constantes y las referencias a clases no modeladas);
  - convención: las asociaciones (línea continua) sólo unen entidades, con el rol igual al atributo;
    entre clases de fabricación pura y de control a entidad se usan dependencias (punteadas), y
    cada dependencia corresponde a un uso real en el código;
  - cada atributo cuyo tipo es otra clase del diagrama tiene su relación dibujada;
  - no se modelan los detalles técnicos: excepciones, cliente HTTP, DTO y constantes (atributos estáticos);
  - representación etiquetada (hide circle), sin agrupaciones de clases (paquetes) ni secciones dentro
    de una clase, y con los atributos separados de los métodos.
Diagramas de secuencia (la completa y el extracto del patrón):
  - cada mensaje es un método existente en la clase receptora, con los mismos parámetros y retorno;
  - cada mensaje está en el diagrama de clases que le corresponde (01 para la secuencia completa, 01b para
    el extracto del patrón), en la clase de la línea de vida y con la misma firma;
  - cada línea de vida es de una clase que está en el diagrama de clases, y no se repiten los
    objetos al pie (hide footbox);
  - ningún mensaje va a un actor secundario (lo representa su clase de interfaz o el Adaptable) y todos
    los mensajes son métodos con parámetros tipados y retorno;
  - convención: sin flechas de retorno (la flecha punteada es sólo para new()), y una iteración de un
    único mensaje se marca con asterisco, no con un fragmento loop.
"""
import glob
import os
import re
import sys

RAIZ = sys.argv[1]
DIAG = os.path.join(RAIZ, "docs", "diagramas")
# Los diagramas pueden estar en subcarpetas de docs/diagramas: se ubican por nombre de archivo.
RUTAS = {f: os.path.join(raiz, f) for raiz, _, archivos in os.walk(DIAG) for f in archivos if f.endswith(".puml")}


def ruta_diagrama(archivo):
    if archivo not in RUTAS:
        sys.exit(f"No se encontró {archivo} dentro de docs/diagramas (ni en sus subcarpetas).")
    return RUTAS[archivo]
STATIC = os.path.join(RAIZ, "src", "main", "resources", "static")
errores, ok = [], 0


def bien():
    global ok
    ok += 1


def norm(t):
    return re.sub(r"\s+", "", t or "")


def partir_params(texto):
    partes, prof, actual = [], 0, ""
    for c in texto:
        if c in "<([":
            prof += 1
        elif c in ">)]":
            prof -= 1
        if c == "," and prof == 0:
            partes.append(actual)
            actual = ""
        else:
            actual += c
    if actual.strip():
        partes.append(actual)
    return [p.strip() for p in partes if p.strip()]


def sin_anotaciones(p):
    resultado, i = "", 0
    while i < len(p):
        if p[i] == "@":
            m = re.match(r"@\w+", p[i:])
            i += m.end()
            if i < len(p) and p[i] == "(":
                prof = 0
                while True:
                    if p[i] == "(":
                        prof += 1
                    elif p[i] == ")":
                        prof -= 1
                    i += 1
                    if prof == 0:
                        break
        else:
            resultado += p[i]
            i += 1
    return resultado.strip()


def params_java(texto):
    salida = []
    for p in partir_params(texto):
        p = sin_anotaciones(p).replace("final ", "")
        tipo, nombre = p.rsplit(None, 1)
        salida.append((nombre, norm(tipo)))
    return salida


def params_diagrama(texto):
    salida = []
    for p in partir_params(texto):
        nombre, tipo = [x.strip() for x in p.split(":", 1)]
        salida.append((nombre, norm(tipo)))
    return salida


# ---------------------------------------------------------------- código Java
java = {}
for archivo in glob.glob(os.path.join(RAIZ, "src", "main", "java", "**", "*.java"), recursive=True):
    fuente = open(archivo, encoding="utf-8").read()
    clase = os.path.basename(archivo)[:-5]
    info = {"metodos": {}, "campos": {}, "estaticos": set(), "implementa": [], "fuente": fuente, "record": None}
    # Métodos de búsqueda (@Lookup): Spring los usa para entregar dependencias; no se modelan.
    info["lookup"] = set(re.findall(r"@Lookup\s+(?:public |protected )?[\w<>]+\s+(\w+)\(", fuente))
    m = re.search(r"implements ([\w, ]+)\s*\{", fuente)
    if m:
        info["implementa"] = [x.strip() for x in m.group(1).split(",")]
    m = re.search(r"interface \w+ extends JpaRepository<(\w+), \w+>", fuente)
    if m:
        info["metodos"]["findAll"] = [([], "List<" + m.group(1) + ">")]
    m = re.search(r"public record (\w+)\((.*?)\)\s*\{", fuente, re.S)
    if m:
        info["record"] = params_java(m.group(2))
    es_interfaz = re.search(r"public interface ", fuente) is not None
    patron = r"^\s*(?:(?:public|private|protected)\s+)?((?:static\s+|final\s+|abstract\s+)*)([\w<>\[\]?, ]+?)\s+(\w+)\s*\("
    for mm in re.finditer(patron, fuente, re.M):
        tipo, nombre = mm.group(2).strip(), mm.group(3)
        if tipo in ("return", "new", "throw", "else", "public", "private", "protected") or nombre in ("if", "for", "while", "switch", "catch", clase):
            continue
        linea = fuente[fuente.rfind("\n", 0, mm.start()) + 1: mm.end()]
        if not re.match(r"\s*(public|private|protected)", linea) and not es_interfaz:
            continue
        i, prof = mm.end() - 1, 0
        while True:
            if fuente[i] == "(":
                prof += 1
            elif fuente[i] == ")":
                prof -= 1
                if prof == 0:
                    break
            i += 1
        info["metodos"].setdefault(nombre, []).append((params_java(fuente[mm.end(): i]), norm(tipo)))
    for mm in re.finditer(r"^\s*(?:private|protected|public)\s+(static\s+)?(?:final\s+)?([\w<>\[\]?, ]+?)\s+(\w+)\s*(?:=|;)", fuente, re.M):
        info["campos"][mm.group(3)] = norm(mm.group(2))
        if mm.group(1):
            info["estaticos"].add(mm.group(3))
    for mm in re.finditer(r"^\s*public (\w+)\((.*?)\)\s*\{", fuente, re.M | re.S):
        if mm.group(1) == clase:
            info["metodos"].setdefault("new", []).append((params_java(mm.group(2)), "<constructor>"))
    java[clase] = info

# ---------------------------------------------------------------- vista: JavaScript y HTML
JS_SEGUIMIENTO = open(os.path.join(STATIC, "js", "seguimiento.js"), encoding="utf-8").read()
HTML_SEGUIMIENTO = open(os.path.join(STATIC, "seguimiento.html"), encoding="utf-8").read()
js = {}
for nombre_archivo, clase in (("seguimiento.js", "PantallaConsultarSeguimientoBolsines"), ("mapa.js", "InterfazMapa")):
    fuente = open(os.path.join(STATIC, "js", nombre_archivo), encoding="utf-8").read()
    funciones = {}
    for mm in re.finditer(r"(?:async\s+)?function\s+(\w+)\s*\(([^)]*)\)", fuente):
        funciones[mm.group(1)] = [p.strip() for p in mm.group(2).split(",") if p.strip()]
    if clase == "InterfazMapa":
        for mm in re.finditer(r"^ {4}(\w+)\(([^)]*)\)\s*\{", fuente, re.M):
            funciones[mm.group(1)] = [p.strip() for p in mm.group(2).split(",") if p.strip()]
    js[clase] = funciones

bloque_ui = JS_SEGUIMIENTO[JS_SEGUIMIENTO.index("const ui = {"): JS_SEGUIMIENTO.index("};", JS_SEGUIMIENTO.index("const ui = {"))]
controles_ui = dict(re.findall(r'^\s+(\w+): document\.getElementById\("([^"]+)"\)', bloque_ui, re.M))
TIPOS_HTML = {"p": "HTMLParagraphElement", "div": "HTMLDivElement", "span": "HTMLSpanElement", "ul": "HTMLUListElement",
              "input": "HTMLInputElement", "select": "HTMLSelectElement", "button": "HTMLButtonElement",
              "dialog": "HTMLDialogElement", "form": "HTMLFormElement", "a": "HTMLAnchorElement",
              "h1": "HTMLHeadingElement", "h2": "HTMLHeadingElement", "h3": "HTMLHeadingElement"}
etiqueta_por_id = {m.group(2): m.group(1) for m in re.finditer(r'<(\w+)\b[^>]*\bid="([^"]+)"', HTML_SEGUIMIENTO)}


def tipo_html(id_elemento):
    return TIPOS_HTML.get(etiqueta_por_id.get(id_elemento), "HTMLElement")


def verificar_metodo(clase, nombre, params, retorno, origen, preferir_js=False):
    """Busca el método en Java (y si no, en JS) y compara la firma."""
    candidatos_java = java.get(clase, {}).get("metodos", {}).get(nombre)
    candidatos_js = js.get(clase, {}).get(nombre)
    if nombre == "new":
        if retorno not in (None, "void"):
            errores.append(f"{origen}: {clase}.new() no puede devolver {retorno}")
        retorno = "<constructor>"
    if candidatos_java and not preferir_js:
        for p_cod, r_cod in candidatos_java:
            if [n for n, _ in p_cod] == [n for n, _ in params] and [t for _, t in p_cod] == [t for _, t in params] \
                    and (retorno is None or r_cod == norm(retorno)):
                return bien()
        return errores.append(f"{origen}: {clase}.{nombre} no coincide. Diagrama: ({params}) : {retorno} | Código: {candidatos_java}")
    if candidatos_js is not None:
        if candidatos_js == [n for n, _ in params]:
            return bien()
        return errores.append(f"{origen}: {clase}.{nombre} (JS) parámetros distintos. Diagrama: {[n for n, _ in params]} | Código: {candidatos_js}")
    if candidatos_java:
        return verificar_metodo(clase, nombre, params, retorno, origen, False)
    errores.append(f"{origen}: {clase}.{nombre}() NO EXISTE en el código")


# ---------------------------------------------------------------- diagramas de clases
metodos_en_diagrama, atributos_en_diagrama, estereotipos = {}, {}, {}
clases_en_vista_estatica = set()
# Por cada diagrama de clases: clase -> {método: [(parámetros, retorno)]}, para contrastar las secuencias.
firmas_por_diagrama = {}
for archivo in ("01-clases-diseno-adapter.puml", "01b-clases-patron-adapter.puml"):
    texto = open(ruta_diagrama(archivo), encoding="utf-8").read()
    completo = archivo.startswith("01-")
    if "hide circle" not in texto:
        errores.append(f"{archivo}: falta 'hide circle' (la convención usa la representación etiquetada)")
    if re.search(r"^\s*(package|namespace|rectangle|frame|together)\b", texto, re.M):
        errores.append(f"{archivo}: hay agrupaciones de clases (package/rectangle/together); las clases sólo se relacionan con flechas")
    clases_diagrama = {}
    firmas = firmas_por_diagrama.setdefault(archivo, {})
    for bloque in re.finditer(r"^  (class|interface) (\w+)([^\n]*)\{\n(.*?)^  \}", texto, re.S | re.M):
        clase, cuerpo = bloque.group(2), bloque.group(4)
        m = re.search(r"<<(\w+)>>", bloque.group(3))
        estereotipos[clase] = m.group(1) if m else None
        clases_diagrama[clase] = []
        firmas.setdefault(clase, {})
        if completo:
            clases_en_vista_estatica.add(clase)
        vio_metodo = False
        for linea in cuerpo.splitlines():
            linea = linea.strip()
            if re.match(r"^(\.\.|--|==|__)", linea):
                errores.append(f"{archivo}: {clase} tiene secciones internas ({linea}); sólo se separan atributos y métodos")
                continue
            m = re.match(r"^[-+#] (?:\{static\} )?(\w+)\((.*)\)(?: : (.+))?$", linea)
            if m:
                vio_metodo = True
                nombre, params, ret = m.group(1), params_diagrama(m.group(2)), m.group(3)
                firmas[clase].setdefault(nombre, []).append((params, norm(ret) if ret else None))
                if completo:
                    metodos_en_diagrama.setdefault(clase, set()).add(nombre)
                verificar_metodo(clase, nombre, params, ret, archivo)
                continue
            m = re.match(r"^[-+#] (\{static\} )?(\w+) : (.+)$", linea)
            if not m:
                continue
            if m.group(1):
                errores.append(f"{archivo}: {clase}.{m.group(2)} es una constante (estática): las constantes no se modelan")
            m = re.match(r"^[-+#] (?:\{static\} )?(\w+) : (.+)$", linea)
            nombre, tipo = m.group(1), norm(m.group(2))
            if vio_metodo:
                errores.append(f"{archivo}: {clase}.{nombre} es un atributo escrito entre los métodos")
            clases_diagrama[clase].append((nombre, tipo))
            if completo:
                atributos_en_diagrama.setdefault(clase, set()).add(nombre)
            es_control = clase == "PantallaConsultarSeguimientoBolsines" and (tipo.startswith("HTML") or nombre == "mapa")
            if es_control:
                if nombre == "mapa":
                    bien() if re.search(r"\bmapa:", JS_SEGUIMIENTO) and tipo == "InterfazMapa" else \
                        errores.append(f"{archivo}: la vista no tiene el atributo mapa : InterfazMapa")
                elif nombre not in controles_ui:
                    errores.append(f"{archivo}: el control {nombre} no existe en el objeto ui de seguimiento.js")
                elif tipo_html(controles_ui[nombre]) != tipo:
                    errores.append(f"{archivo}: {nombre} es {tipo_html(controles_ui[nombre])} en el HTML, no {tipo}")
                else:
                    bien()
                continue
            info = java.get(clase)
            if info and info["record"] is not None:
                comp = dict(info["record"])
                bien() if comp.get(nombre) == tipo else errores.append(
                    f"{archivo}: record {clase}.{nombre} : {tipo} no coincide ({comp.get(nombre)})")
            elif info and info["campos"].get(nombre) == tipo:
                bien()
            else:
                errores.append(f"{archivo}: atributo {clase}.{nombre} : {tipo} no coincide (código: {info and info['campos'].get(nombre)})")

    relacionadas = set()
    for linea in texto.splitlines():
        m = re.match(r'^(\w+) --> "[^"]*" (\w+) : (\w+)$', linea)
        if m:
            origen_c, destino, rol = m.groups()
            relacionadas.add(frozenset((origen_c, destino)))
            if estereotipos.get(origen_c) != "entity" or estereotipos.get(destino) != "entity":
                errores.append(f"{archivo}: asociación {origen_c} -> {destino}: la convención usa asociaciones sólo entre entidades")
            tipo = java.get(origen_c, {}).get("campos", {}).get(rol)
            if completo:
                atributos_en_diagrama.setdefault(origen_c, set()).add(rol)
            bien() if tipo and re.search(r"\b" + destino + r"\b", tipo) else errores.append(
                f"{archivo}: asociación {origen_c} -> {destino} rol '{rol}' no coincide (campo: {tipo})")
            continue
        m = re.match(r"^(\w+) \.\.> (\w+)(?: : .*)?$", linea)
        if m:
            a, b = m.groups()
            relacionadas.add(frozenset((a, b)))
            if estereotipos.get(a) == "entity" and estereotipos.get(b) == "entity":
                errores.append(f"{archivo}: {a} ..> {b}: entre entidades la convención usa asociación")
            fuentes = java.get(a, {}).get("fuente", "")
            if a == "PantallaConsultarSeguimientoBolsines":
                fuentes += JS_SEGUIMIENTO + texto[texto.index("class PantallaConsultarSeguimientoBolsines"):texto.index("class InterfazMapa ")]
                fuentes += " InterfazMapa" if "Mapas.crear" in JS_SEGUIMIENTO else ""
            bien() if re.search(r"\b" + b + r"\b", fuentes) else errores.append(
                f"{archivo}: la dependencia {a} ..> {b} no corresponde a un uso en el código")
            continue
        m = re.match(r"^(\w+) <\|\.\. (\w+)$", linea)
        if m:
            relacionadas.add(frozenset(m.groups()))
            bien() if m.group(1) in java.get(m.group(2), {}).get("implementa", []) else errores.append(
                f"{archivo}: {m.group(2)} no implementa {m.group(1)}")
    for clase, atributos in clases_diagrama.items():
        for nombre, tipo in atributos:
            for otra in clases_diagrama:
                if otra != clase and re.search(r"\b" + otra + r"\b", tipo) and frozenset((clase, otra)) not in relacionadas:
                    errores.append(f"{archivo}: {clase}.{nombre} es de tipo {otra}, pero no hay relación dibujada entre ambas")

CLASES_CU = ["GestorConsultarSeguimientoBolsines", "PantallaConsultarSeguimientoBolsines", "SesionUsuario", "InterfazMapa",
             "GestorNotificarUbicacionBolsin", "InterfazCorreoElectronico", "IAdaptadorGPSTracker",
             "AdaptadorGPSTrackerXTR4500L", "AdaptadorGPSTrackerNavTrackQX7A", "AdaptadorGPSTrackerGeoPulseMTR900",
             "ServicioGPSTrackerXTR4500L", "ServicioGPSTrackerNavTrackQX7A", "ServicioGPSTrackerGeoPulseMTR900",
             "UbicacionBolsin"]
# Detalles técnicos de la implementación web que no se modelan: las excepciones de Java, el cliente HTTP
# que usan los servicios y los DTO (en las firmas se usan como tipos de dato).
EXCLUIDAS = ["UbicacionNoDisponibleException", "ErrorServicioGPSTrackerException", "ClienteHttpGPSTracker"] + \
            [c for c in java if c.endswith("DTO")]
for c in EXCLUIDAS:
    if c in clases_en_vista_estatica:
        errores.append(f"01-clases-diseno-adapter.puml: {c} es un detalle técnico y no se modela")
ENTIDADES = ["Bolsin", "GPSTracker", "ComisionMedica", "CambioEstadoBolsin", "Estado", "Empleado", "Rol"]
# Los constructores sólo se dibujan (como new()) en las clases cuyas instancias se crean en la vista
# dinámica; los constructores de inyección de dependencias de Spring y toString() se omiten.
CON_CONSTRUCTOR = {"UbicacionBolsin"}
for clase in CLASES_CU:
    faltan = [n for n in java[clase]["metodos"] if n not in metodos_en_diagrama.get(clase, set())
              and n != "toString" and n not in java[clase]["lookup"] and (n != "new" or clase in CON_CONSTRUCTOR)]
    if faltan:
        errores.append(f"Falta en el diagrama de clases: métodos {clase}.{faltan}")
for clase in CLASES_CU + ENTIDADES:
    faltan = [n for n, t in java[clase]["campos"].items()
              if n != "id" and n not in java[clase]["estaticos"] and t not in EXCLUIDAS
              and n not in atributos_en_diagrama.get(clase, set())]
    if faltan:
        errores.append(f"Falta en el diagrama de clases: atributos {clase}.{faltan}")
EVENTOS = {"tomarFiltros", "tomarSeleccionBolsin", "tomarConfirmacionEnvioCorreo", "opcionCancelar",
           "opcionConsultarSeguimientoBolsines"}  # existen en JS y en Java con el mismo nombre
faltan_js = [n for n in js["PantallaConsultarSeguimientoBolsines"]
             if n not in metodos_en_diagrama.get("PantallaConsultarSeguimientoBolsines", set()) and n not in EVENTOS]
if faltan_js:
    errores.append(f"Falta en el diagrama de clases (vista): {faltan_js}")

# ---------------------------------------------------------------- diagramas de secuencia
# Cada secuencia se contrasta con su diagrama de clases: la completa con la vista estática completa, y el
# extracto del patrón con el extracto del patrón.
PARES = {"02-secuencia-cu36.puml": "01-clases-diseno-adapter.puml",
         "02b-secuencia-patron-adapter.puml": "01b-clases-patron-adapter.puml"}
for archivo, diagrama_clases in PARES.items():
    texto = open(ruta_diagrama(archivo), encoding="utf-8").read()
    firmas_clases = firmas_por_diagrama[diagrama_clases]
    if "hide footbox" not in texto:
        errores.append(f"{archivo}: falta 'hide footbox' (las líneas de vida terminan sin repetir los objetos abajo)")
    alias = {}
    for m in re.finditer(r'^(participant|boundary|control|entity|actor) "([^"]+)" as (\w+)', texto, re.M):
        tipo, nombre, a = m.groups()
        nombre = nombre.replace("\\n", "")
        alias[a] = None if tipo == "actor" else nombre.split(":")[-1].strip()
        if alias[a] and alias[a] not in firmas_clases:
            errores.append(f"{archivo}: la línea de vida {nombre} es de una clase que no está en {diagrama_clases}")
        if alias[a] in EXCLUIDAS:
            errores.append(f"{archivo}: {alias[a]} es un detalle técnico y no se modela")
    pila_loops = []
    for linea in texto.splitlines():
        if re.match(r"^\s*(loop|opt|alt|group)\b", linea):
            pila_loops.append([linea.strip().startswith("loop"), 0, linea.strip()])
            continue
        if re.match(r"^\s*end\s*$", linea) and pila_loops:
            es_loop, mensajes, titulo = pila_loops.pop()
            if es_loop and mensajes == 1:
                errores.append(f"{archivo}: '{titulo}' itera un único mensaje: la convención usa asterisco")
            if pila_loops:
                pila_loops[-1][1] += mensajes
            continue
        m = re.match(r"^\s*(\w+) (-->|->) (\w+) : (.+)$", linea)
        if not m:
            continue
        if pila_loops:
            pila_loops[-1][1] += 1
        flecha, destino, etiqueta = m.group(2), alias.get(m.group(3)), m.group(4)
        etiqueta = re.sub(r"^~?\*", "", etiqueta)
        if flecha == "-->" and not etiqueta.startswith("new("):
            errores.append(f"{archivo}: flecha de retorno '{etiqueta}': la convención no las usa (sólo new())")
            continue
        if destino is None:
            errores.append(f"{archivo}: mensaje a un actor ('{etiqueta}'): los actores secundarios se representan con su "
                           f"clase de interfaz o su Adaptable, y cada mensaje es un método")
            continue
        mm = re.match(r"^(\w+)\((.*)\)(?: : (.+))?$", etiqueta)
        if not mm:
            errores.append(f"{archivo}: mensaje sin formato de método: {etiqueta}")
            continue
        nombre, params, ret = mm.group(1), params_diagrama(mm.group(2)), mm.group(3)
        if ret is None and nombre != "new":
            errores.append(f"{archivo}: el mensaje {destino}.{nombre} no tiene tipo de retorno")
        en_java = nombre in java.get(destino, {}).get("metodos", {})
        verificar_metodo(destino, nombre, params, ret, archivo, preferir_js=not en_java)
        # Consistencia con el diagrama de clases: el método está en la clase receptora, con la misma firma.
        en_clases = firmas_clases.get(destino, {}).get(nombre)
        if not en_clases:
            errores.append(f"{archivo}: {destino}.{nombre}() se usa en la secuencia pero no está en {diagrama_clases}")
        elif (params, norm(ret) if ret else None) not in en_clases and not (nombre == "new" and any(p == params for p, _ in en_clases)):
            errores.append(f"{archivo}: {destino}.{nombre}() tiene otra firma en {diagrama_clases}: "
                           f"secuencia ({params}) : {ret} | clases {en_clases}")
        else:
            bien()

print(f"Comprobaciones correctas: {ok}")
print(f"Diferencias: {len(errores)}")
for e in errores:
    print("  -", e)
sys.exit(1 if errores else 0)
