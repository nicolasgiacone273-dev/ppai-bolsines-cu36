# PPAI 2026 DSI · Bolsines · CU 36 "Consultar seguimiento de bolsines"

Implementación del CU 36 rediseñado con el **patrón Adapter** (Gamma), integrada con la
API mock de GPS Tracker que provee la cátedra.

## Detalles de implementación (punto 1 de la consigna)

| Aspecto | Elección |
|---|---|
| Lenguaje | Java 17 |
| Framework | Spring Boot 3.3.5 (la misma versión que la API de la cátedra) |
| Tecnología | Web (RNF 1): backend REST + pantallas HTML/CSS/JavaScript |
| Base de datos | H2 en archivo (`./data/bolsines-db`) |
| Framework de persistencia | Spring Data JPA / Hibernate |
| Mapa | Google Maps si se configura una API key (RNF 7); si no, Leaflet + OpenStreetMap |

## Cómo ejecutarlo

Hace falta Java 17 o superior (JDK). No hace falta instalar Maven: el proyecto trae el Maven Wrapper
(`mvnw` / `mvnw.cmd`), que descarga solo la versión correcta de Maven la primera vez (necesita internet).

1. **Levantar la API de la cátedra** (`dsi-api-bolsines-main`): ejecutar `GpsTrackerMockApplication`.
   Queda en `http://localhost:8080`.
2. **Levantar este proyecto**: abrir la carpeta en IntelliJ, esperar que importe Maven y ejecutar
   `BolsinesApplication`. Desde la consola, en la carpeta del proyecto: `mvnw.cmd spring-boot:run`
   (Windows) o `./mvnw spring-boot:run` (Linux/Mac). Si ya tienen Maven instalado, `mvn spring-boot:run`.
   Queda en `http://localhost:8081`.
3. Abrir **http://localhost:8081** en el navegador.

La primera vez se crea la base H2 y se cargan los datos de prueba. Para volver a los datos
originales, cerrar la aplicación y borrar la carpeta `data`.

Para correr las pruebas unitarias de los adaptadores: `mvnw.cmd test` (Windows) o `./mvnw test`.

Los correos se guardan como archivos `.eml` en la carpeta `correos-enviados` (se pueden abrir con
Outlook, Thunderbird o el Bloc de notas), y además se registran en la consola.

Para mostrar la persistencia se puede usar la consola de H2: `http://localhost:8081/h2-console`
(JDBC URL `jdbc:h2:file:./data/bolsines-db;AUTO_SERVER=TRUE`, usuario `sa`, sin clave).

## Patrón Adapter

### Problema que resuelve

El sistema tiene que pedir la ubicación de cada bolsín a su GPS Tracker, pero hay tres modelos
con APIs **incompatibles entre sí** (Observación 1 del CU 36):

| Modelo | Endpoint | Parámetros | Respuesta |
|---|---|---|---|
| XTR-4500L | `getBolsinLocation()` | API Key, nro. de bolsín, **CM origen** | JSON en un String, con una lista de ítems |
| NavTrack QX-7A | `retrieveTrackingData()` | API Key, nro. de bolsín, **CM destino** | Un único String con los campos separados por comas |
| GeoPulse MTR-900 | `fetchCargoPositions()` | API Key, nro. de bolsín | Una matriz |

Si el Gestor llamara directamente a cada API, tendría un `if/switch` por modelo con la llamada y
el procesamiento de cada formato. Cada modelo nuevo obligaría a modificarlo, lo que rompe el
principio abierto/cerrado y mezcla la lógica del CU con detalles técnicos de terceros.

### Solución: Adaptador de objeto

Se define una interfaz común (**Objetivo**) con la operación que necesita el Gestor, y un
adaptador por modelo que la implementa **envolviendo** la API de ese modelo (**Adaptable**).
Cada adaptador traduce la llamada, la respuesta y los errores.

| Participante del patrón | Clase |
|---|---|
| Cliente | `GestorConsultarSeguimientoBolsines` |
| Objetivo (Target) | `IAdaptadorGPSTracker` → `obtenerUbicacion(numeroBolsin, codigoCMOrigen, codigoCMDestino) : UbicacionBolsin` |
| Adaptador (Adapter) | `AdaptadorGPSTrackerXTR4500L`, `AdaptadorGPSTrackerNavTrackQX7A`, `AdaptadorGPSTrackerGeoPulseMTR900` |
| Adaptable (Adaptee) | `ServicioGPSTrackerXTR4500L`, `ServicioGPSTrackerNavTrackQX7A`, `ServicioGPSTrackerGeoPulseMTR900` |

- Todos los adaptadores devuelven lo mismo: un `UbicacionBolsin` con número, latitud, longitud y
  fecha/hora de última actualización (lo que pide la Observación 1).
- Cualquier error de una API (API Key inválida, bolsín inexistente, CM que no coincide, servicio
  caído o respuesta mal formada) se traduce a `UbicacionNoDisponibleException`. Eso es el flujo A3,
  y el Gestor lo maneja siempre igual.
- **¿Cómo elige el Gestor el adaptador?** Cada bolsín tiene su `GPSTracker` con su modelo. Spring
  le inyecta al Gestor la lista de todas las implementaciones de `IAdaptadorGPSTracker`, y el Gestor
  arma un mapa *modelo → adaptador*. No hay ningún `if` por modelo.
- **Agregar un cuarto modelo** = crear su Servicio y su Adaptador anotados con `@Component`. El
  Gestor no se modifica.

Ventajas que se pueden mencionar: responsabilidad única (la conversión de formatos queda fuera
del Gestor) y abierto/cerrado. La desventaja es que aparecen más clases (una interfaz y dos clases por modelo).

## Estructura del proyecto

```
src/main/java/ar/edu/utn/frc/dsi/bolsines
├── boundary/      Pantallas (PantallaConsultarSeguimientoBolsines, PantallaIniciarSesion),
│                  InterfazMapa (configuración del mapa y API key de Google Maps) e
│                  InterfazCorreoElectronico (actor secundario Servidor de Correo)
├── control/       GestorConsultarSeguimientoBolsines (Cliente del Adapter),
│                  GestorNotificarUbicacionBolsin (CU 30 incluido), SesionUsuario
├── adaptador/     IAdaptadorGPSTracker (Objetivo), los 3 Adaptadores, UbicacionBolsin
├── gpstracker/    Los 3 Servicios de GPS Tracker (Adaptables) y el cliente HTTP
├── entity/        Entidades JPA del Modelo de Dominio
├── repository/    Repositorios Spring Data JPA
├── dto/           Datos que viajan a la pantalla (JSON)
└── config/        Carga de datos iniciales
src/main/resources/static      Pantallas web (HTML, CSS, JS) + Leaflet y la fuente, incluidos localmente
src/test/java                  Pruebas unitarias de los adaptadores
docs/diagramas/Diagramas de diseño (CU completo)            01 y 02: vista estática y dinámica de todo el CU
docs/diagramas/Diagramas de diseño (aplicación del patrón)  01b y 02b: sólo la parte donde se aplica el Adapter
                               (cada diagrama en PlantUML editable + PNG + SVG)
docs/verificar_diagramas.py    Compara los diagramas con el código y entre sí
docs/generar_diagrama_patron.py   Genera 01b (extracto del patrón) a partir de 01
docs/generar_secuencia_patron.py  Genera 02b (extracto del patrón) a partir de 02
```

## Diagramas de diseño

Todos son diagramas de **diseño** (con el patrón aplicado, tipos, visibilidad y retornos). Están
separados por alcance: la carpeta *CU completo* tiene la realización entera del CU, y la carpeta
*aplicación del patrón* tiene sólo la parte que se rediseña con el Adapter, que es lo que piden los
puntos 2 y 3 de la consigna.

| Archivo | Contenido |
|---|---|
| `01-clases-diseno-adapter` | Vista estática completa del CU 36: pantalla, gestores, patrón Adapter, entidades y repositorios. |
| `01b-clases-patron-adapter` | Extracto con la estructura del patrón (para una hoja de la entrega). Se genera copiando las clases del diagrama completo, así que las firmas son idénticas. |
| `02-secuencia-cu36` | Vista dinámica del **flujo básico** del CU (pasos 1 a 11), en un solo diagrama: el Adapter en los pasos 4 y 5 (modelo XTR-4500L) y la inclusión del CU 30. |
| `02b-secuencia-patron-adapter` | Extracto con la aplicación del patrón: los pasos 4 y 5, donde el gestor (Cliente) pide la ubicación a través de `IAdaptadorGPSTracker` (Objetivo) y el adaptador la traduce a la API del modelo (Adaptable). Es lo que pide el punto 3 de la consigna ("la parte del escenario que se rediseña"). Se genera copiando los mensajes del diagrama completo. |

Todos los métodos tienen visibilidad, parámetros con su tipo y tipo de retorno, y en las secuencias
cada mensaje lleva su tipo de retorno. Los nombres son exactamente los del código.

> **Nota sobre el alcance del diagrama de clases.** La vista estática incluye los atributos y métodos
> de diseño de la implementación, y no sólo los que aparecen en los diagramas de secuencia. Los métodos
> de los flujos alternativos (filtros, cancelación, cierre de sesión, errores de formato) no tienen
> diagrama de secuencia propio. Se omiten los detalles técnicos de la implementación web, que siguen
> existiendo en el código:
> - las clases **DTO**, que sólo transportan datos entre el gestor y la vista (en las firmas se usan
>   como tipos de dato, igual que `List` o `LocalDateTime`);
> - el **cliente HTTP** (`ClienteHttpGPSTracker`) que usan los servicios para llamar a la API;
> - las **clases de excepción** (ver abajo);
> - las **constantes** (atributos estáticos), como los nombres de modelos, estados y roles, los formatos
>   de fecha, el logger del correo o las posiciones de los datos en las respuestas de las APIs;
> - los constructores de inyección de dependencias de Spring, `toString()` y el `id` técnico de JPA.
>
> La misma aclaración está como nota en el diagrama.

**Convenciones de modelado de la cátedra aplicadas:**
- Representación **etiquetada** en los diagramas de clases (estereotipos `«boundary»`, `«control»`,
  `«entity»`, sin los círculos de PlantUML) e **icónica** en los de secuencia.
- **Asociaciones** (línea continua, con rol y multiplicidad) **sólo entre entidades**. Entre clases de
  fabricación pura (pantalla, gestores, adaptadores, servicios, repositorios) y de control a
  entidad se usan **dependencias** (línea punteada).
- La **Pantalla no tiene al gestor ni a la sesión como atributos**: su relación con ellos es de
  dependencia, no de asociación. En el código los pide en cada acción con `obtenerGestor()` y
  `obtenerSesion()` (métodos de búsqueda `@Lookup`, con los que Spring entrega el objeto) y los usa como
  variables locales. Esos métodos son el mecanismo de Spring y no se dibujan.
- Los **atributos de la pantalla son sus controles de interfaz**, con prefijo según el tipo:
  `lbl` (etiqueta), `txt` (campo de texto), `cmb` (combo), `btn` (botón), `lst` (lista),
  `dlg` (diálogo), `pnl` (panel), `frm` (formulario). Son los mismos nombres del objeto `ui` de
  `js/seguimiento.js`, y el tipo es el del elemento HTML (`HTMLButtonElement`, `HTMLSelectElement`, …).
- El constructor se escribe `new()`; en las secuencias, la creación es una flecha punteada con `new(...)`.
- En las secuencias **no se dibujan flechas de retorno**: el tipo de retorno va en la firma del mensaje.
- Los fragmentos `loop` llevan la condición `[mientras haya …]`; una iteración de un **único mensaje** se
  marca con **asterisco** (`*getNumeroBolsin()`) en lugar de un fragmento.
- Las instancias únicas tienen **nombre** (`logueado: Empleado`, `cmUsuario: ComisionMedica`,
  `seleccionado: Bolsin`, `destino: ComisionMedica`, `ubicacion: UbicacionBolsin`); las líneas de vida en
  naranja representan un conjunto de objetos de la clase (`:Bolsin`, `:Empleado`, …).
- **El diagrama de secuencia modela un único escenario, el flujo básico**, como en los ejemplos de la
  cátedra ("Escenario del Curso Normal o Flujo Básico") y como pide la consigna ("el escenario
  descripto"). Los flujos alternativos (A1 a A5) no se dibujan: están implementados y se muestran en
  la defensa. Los fragmentos `opt` que quedan no son flujos alternativos sino condiciones dentro del
  flujo básico (qué bolsines se muestran, cuál es el gerente).
- Cada clase tiene sólo dos compartimentos: **atributos** arriba y **métodos** abajo, sin secciones
  internas. En la Pantalla quedan juntos los atributos de la clase Java y los controles de la vista, y
  juntos los métodos de las dos partes. Las acciones del usuario (`opcion...`, `tomar...`) existen en
  las dos partes con el mismo nombre: la vista llama por HTTP al método Java.
- No hay paquetes ni recuadros que agrupen clases: las clases se relacionan sólo con las flechas.
- **Las excepciones no se modelan.** `UbicacionNoDisponibleException` y `ErrorServicioGPSTrackerException`
  existen en el código, pero son el mecanismo de Java para avisar un error, no una responsabilidad del
  diseño ni un rol del patrón (Cliente, Objetivo, Adaptador, Adaptable). Se usan en el flujo A3, que
  no se dibuja: el adaptador informa al gestor que no hay ubicación y el gestor guarda el motivo y sigue
  con el próximo bolsín.
- En las secuencias las líneas de vida terminan abajo sin repetir los objetos.
- **Los actores secundarios no reciben mensajes en la secuencia.** Cada uno está representado por la
  clase de nuestro sistema que habla con él, como en el ejemplo de Adapter de la cátedra (el adaptador
  invoca directamente al servicio externo, que es el Adaptable):
  - el GPS Tracker, por `ServicioGPSTrackerXTR4500L` (el Adaptable), cuyo `getBolsinLocation(...)`
    tiene la firma del endpoint que describe el ERS;
  - el Servidor Google Maps, por `InterfazMapa`;
  - el Servidor de Correo, por `InterfazCorreoElectronico`.

  Así todos los mensajes son métodos con parámetros tipados y retorno; una nota en cada caso lo aclara.
- La **API key de Google Maps es atributo de `InterfazMapa`**, no de la Pantalla, porque sólo el mapa
  la usa: así aumenta la cohesión de las dos clases. `InterfazMapa` tiene, como la Pantalla, una parte
  Java (`boundary/InterfazMapa.java`, que entrega la configuración en `/api/mapa/configuracion`) y una
  parte en la vista (`js/mapa.js`, que dibuja el mapa).

**Verificación:** `python docs/verificar_diagramas.py .` (desde la carpeta del proyecto) revisa
que cada método, atributo, relación y mensaje de los diagramas exista en el código con los mismos
parámetros y el mismo retorno, que los controles de la pantalla existan en el JS y en el HTML con ese
tipo, y que no falte en el diagrama ningún método ni atributo del código. También controla las
convenciones de arriba (asociaciones sólo entre entidades, dependencias que correspondan a un uso real,
relaciones dibujadas para cada atributo de otra clase, sin flechas de retorno, asterisco en lugar de
loops de un solo mensaje). Además, contrasta cada secuencia con su diagrama de clases (02 con 01, y 02b
con 01b): cada objeto de la secuencia tiene su clase en el diagrama de clases, y cada mensaje está en
esa clase con la misma firma. Conviene correrlo después de cualquier cambio.

Si se modifica el diagrama completo, los extractos se regeneran con:

```
python docs/generar_diagrama_patron.py "docs/diagramas/Diagramas de diseño (CU completo)/01-clases-diseno-adapter.puml" "docs/diagramas/Diagramas de diseño (aplicación del patrón)/01b-clases-patron-adapter.puml"
python docs/generar_secuencia_patron.py "docs/diagramas/Diagramas de diseño (CU completo)/02-secuencia-cu36.puml" "docs/diagramas/Diagramas de diseño (aplicación del patrón)/02b-secuencia-patron-adapter.puml"
```

El verificador encuentra los diagramas por nombre de archivo dentro de `docs/diagramas` y sus
subcarpetas, así que las carpetas se pueden renombrar sin romperlo (los nombres de los archivos sí
tienen que quedar iguales).

En la web, la "pantalla" del CU tiene dos partes: la vista que ve el usuario
(`seguimiento.html` + `js/seguimiento.js`) y la clase `PantallaConsultarSeguimientoBolsines`, que
recibe cada acción y se la pasa al Gestor. Los nombres de los métodos siguen el diagrama de secuencia.

## Decisiones de diseño a tener en cuenta

1. **Clase `GPSTracker` agregada al Modelo de Dominio.** El modelo de la cátedra no la tiene,
   pero el RNF 6 dice que cada bolsín lleva su propio dispositivo. Se necesita para saber qué
   modelo (y por lo tanto qué adaptador) corresponde a cada bolsín.
2. **CU 30, no CU 31.** El paso 11 del CU 36 dice "se incluye al CU 31 Notificar ubicación de
   bolsín", pero en el listado de funcionalidades el CU 31 es "Registrar Revisión de Documentación"
   y el de notificar ubicación es el **CU 30**. Conviene comentárselo al tutor.
3. **Inicio de sesión simulado.** Se elige el empleado de una lista; el CU 5 no forma parte del práctico.
4. **Correo simulado.** El RNF 5 pide la API de Outlook (Microsoft Graph), que necesita una cuenta
   corporativa. El envío se encapsula en `InterfazCorreoElectronico`: hoy guarda el `.eml`, y para
   enviarlo de verdad sólo cambia esa clase.
5. **Datos de prueba alineados con la API.** Los números de bolsín (101 a 110) y los códigos de CM
   coinciden con los precargados en la API de la cátedra. XTR-4500L valida la CM **origen** y
   NavTrack QX-7A la CM **destino**, por eso cada bolsín se asignó a un modelo compatible con lo
   que tiene la API (y el 105 a propósito no, para mostrar A3).
6. **Remitos y documentación** no se modelan porque el CU 36 no los usa.

## Mapa: Google Maps o OpenStreetMap

En `src/main/resources/application.properties`:

```
google.maps.api-key=
```

Con la propiedad vacía se usa Leaflet + OpenStreetMap (gratis, sin key). Si se carga una API key de
Google Maps (Maps JavaScript API), se usa Google Maps, como pide el RNF 7. En los dos casos el mapa
necesita internet en el navegador para descargar los mosaicos.

## Problemas comunes

- **"java no se reconoce como un comando"** al usar `mvnw`: falta instalar un JDK 17 o superior, o
  agregarlo al `PATH` (también conviene definir `JAVA_HOME`).
- **Todos los bolsines aparecen "sin ubicación"**: la API de la cátedra no está levantada en el
  puerto 8080, o se cambiaron las API keys.
- **"Port 8081 was already in use"**: hay otra instancia corriendo; cerrarla o cambiar `server.port`.
- **Cambié los datos de prueba y no se ven**: borrar la carpeta `data` (los datos sólo se cargan si
  la base está vacía).
