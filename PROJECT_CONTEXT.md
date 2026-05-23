# PROJECT_CONTEXT.md — Ariatus

## 1. Visión general

**Ariatus** es un proyecto privado de servidor Minecraft orientado a crear una experiencia **Survival semi-RPG / MMORPG sandbox modular**, con una arquitectura propia y escalable.

El objetivo no es depender de muchos plugins externos, sino construir un ecosistema propio donde cada sistema importante sea desarrollado como módulo independiente sobre un núcleo central llamado **AriatusCore**.

La meta final es que Ariatus se sienta como un mundo persistente, inmersivo y distribuido, no como un simple survival con plugins.

---

## 2. Arquitectura general de red

La arquitectura planeada será:

```txt
Velocity
├── Auth
├── PreLobby
├── Ariath
├── Aeth
├── Argand
├── Atalla
└── Aetus
```

### Velocity

Será el proxy principal de la red.

Responsabilidades futuras:

* Conectar jugadores a servidores internos.
* Gestionar transferencias entre regiones.
* Coordinar comunicación global.
* Enrutamiento entre Auth, PreLobby y regiones.
* Futuro soporte para chat global entre mundos.

### Auth

Servidor dedicado para usuarios no-premium.

* Los jugadores no-premium entran aquí para registrarse/loguearse.
* Los jugadores premium deberían saltarse este servidor e ir directamente al PreLobby.

### PreLobby

Servidor inicial de experiencia inmersiva.

Funciones futuras:

* Cinemática de entrada.
* Menú de perfil/personaje.
* Selector de región/mundo.
* Preferencias iniciales.
* Entrada al mundo principal.

### Regiones/Mundos

Cada región será un servidor separado.

Ejemplos:

```txt
Ariath
Aeth
Argand
Atalla
Aetus
```

Ventajas:

* Mejor rendimiento.
* Cada región tiene su propio TPS.
* Eventos regionales independientes.
* Economía regional futura.
* Viajes inmersivos entre mundos.
* Escalabilidad a otros VPS/dedicados en el futuro.

---

## 3. Filosofía técnica

Ariatus se basa en estos principios:

```txt
AriatusCore = infraestructura
Módulos externos = gameplay / sistemas reales
```

AriatusCore NO debe contener sistemas de gameplay.

AriatusCore debe encargarse de:

* Cargar módulos externos.
* Descargar módulos.
* Hotreload.
* Configs por módulo.
* Resources por módulo.
* Sistema de comandos dinámicos.
* Registro de listeners.
* Registro de tasks.
* DatabaseService.
* ServiceRegistry.
* MessageService.
* LoggerService.
* Profiler.
* APIs compartidas.

Los módulos deben encargarse de:

* Perfiles.
* Economía.
* Scoreboard/UI.
* Permisos.
* Chat.
* Criaturas.
* Skills.
* Misiones.
* Mercado.
* Crates.
* Cinemáticas.
* Regiones.

---

## 4. AriatusCore

**AriatusCore** es el corazón del ecosistema.

Package principal usado:

```txt
net.ariatus.project
```

Clase principal:

```txt
AriatusCore
```

### Funciones actuales del Core

AriatusCore ya tiene o está previsto que tenga:

```txt
✔ ModuleLoader externo
✔ carga desde plugins/AriatusCore/modules/
✔ ModuleDataManager
✔ ModuleConfigManager
✔ carga de resources desde el JAR correcto del módulo
✔ DatabaseService con HikariCP/MariaDB
✔ MigrationManager
✔ LoggerService
✔ MessageService con Legacy + MiniMessage
✔ ServiceRegistry
✔ TaskManager
✔ ListenerManager
✔ CommandManager dinámico
✔ ModuleProfiler
✔ Health command
✔ Version command
✔ Hotreload de módulos
✔ Shutdown ordenado
✔ Classloader cleanup
```

### AriatusPlugins

AriatusCore implementa una idea propia similar a PlugMan, pero limitada a lo necesario:

```txt
/ariatus module load <id>
/ariatus module unload <id>
/ariatus module hotreload <id>
/ariatus module enable <id>
/ariatus module disable <id>
/ariatus modules
/ariatus health
/ariatus profiler
```

No se desea depender de PlugMan.

---

## 5. Sistema modular

Los módulos externos se colocan en:

```txt
plugins/AriatusCore/modules/
```

Los datos de cada módulo se guardan en:

```txt
plugins/AriatusCore/modules-data/<module-id>/
```

Cada módulo debe tener:

```txt
src/main/resources/ariatus-module.yml
```

Ejemplo:

```yml
id: profile
name: AriatusProfile
main: net.ariatus.project.AriatusProfile
version: 0.1.0
dependencies: []
```

Los módulos extienden:

```java
ExternalAriatusModule
```

---

## 6. Comunicación entre módulos

Regla importante:

```txt
Los módulos NO deben depender directamente de clases internas de otros módulos.
```

Correcto:

```txt
AriatusCore contiene APIs compartidas.
AriatusProfile implementa esas APIs.
AriatusScoreboard consume esas APIs.
```

Incorrecto:

```txt
AriatusScoreboard importando clases internas de AriatusProfile.
```

La comunicación debe hacerse mediante:

```txt
AriatusCore ServiceRegistry
```

Ejemplo de diseño:

```txt
AriatusCore
└── api/
    ├── profile/
    │   ├── ProfileService
    │   ├── ProfileView
    │   └── ExperienceProvider
    ├── economy/
    │   └── EconomyService
    ├── permissions/
    │   └── PermissionsService
    └── scoreboard/
        └── ScoreboardAPI
```

### Ejemplo de comunicación correcta

AriatusProfile registra:

```java
services().register(ProfileService.class, profileManager);
services().register(ExperienceProvider.class, experienceService);
```

AriatusScoreboard consume:

```java
ProfileService profileService = services().require(ProfileService.class);
```

---

## 7. APIs compartidas actuales/futuras

### Profile API

Debe vivir en AriatusCore:

```txt
net.ariatus.project.api.profile.ProfileService
net.ariatus.project.api.profile.ProfileView
net.ariatus.project.api.profile.ExperienceProvider
```

### ProfileView

Vista pública segura del perfil.

Debe exponer solo datos que otros módulos necesitan leer:

```txt
name
level
experience
totalExperience
reputation
playtimeSeconds
kills
deaths
blocksBroken
blocksPlaced
```

### ProfileService

Permite a otros módulos obtener la vista pública del perfil.

### ExperienceProvider

Permite consultar:

```txt
requiredExperience(level)
maxLevel()
```

Esto evita que AriatusScoreboard dependa directamente de AriatusProfile.

---

## 8. AriatusScoreboard

Módulo visual/UI del jugador.

Clase principal:

```txt
net.ariatus.project.AriatusScoreboard
```

Aunque se llame Scoreboard, este módulo gestiona varias partes visuales:

```txt
✔ Sidebar
✔ TabList header/footer
✔ TabNames
✔ Nametags
✔ BelowName
✔ Prefix priority system
✔ Tab sorting
✔ Animaciones globales
✔ Comando administrativo
```

### Archivos de configuración

```txt
config.yml       → Sidebar + TabList
nametags.yml    → Prefixes, suffixs, tabnames, nametags, tab sorting
belowname.yml   → BelowName
animations.yml  → Animaciones globales
```

### Características implementadas

```txt
✔ Sidebar anti-flicker
✔ líneas con update-ticks independientes
✔ TabList header/footer
✔ Nametags por permisos
✔ TabNames por permisos
✔ Prefix priority system
✔ Tab sorting por prioridad
✔ Suffixs por permiso
✔ BelowName moderno tipo “Vida: 20/20”
✔ animations.yml
✔ variable %animation_<id>%
✔ comandos /ariatusscoreboard, /asb, /scoreboard
✔ reload de configs sin hotreload completo
✔ toggle individual
✔ debug
```

### Ejemplo de animations.yml

```yml
example:
  update-ticks: 20
  texts:
    - '&eNivel: &f999 &7| &a&lLEYENDA'
    - '&eClan: &fGremio Of Ariatus &7| &cLider'

time:
  update-ticks: 20
  texts:
    - '&7Time &b%time%'
    - '&7Date &b%date%'
```

Uso:

```txt
%animation_example%
%animation_time%
```

### Placeholders actuales de Scoreboard

Básicos:

```txt
%player_name%
%player_uuid%
%world%
%online%
%health%
%max_health%
%level%
%food%
%ping%
%time%
%date%
```

Desde AriatusProfile:

```txt
%profile_level%
%profile_experience%
%profile_required_experience%
%profile_remaining_experience%
%profile_experience_progress%
%profile_total_experience%
%profile_reputation%
%profile_playtime%
%profile_kills%
%profile_deaths%
%profile_blocks_broken%
%profile_blocks_placed%
```

---

## 9. AriatusProfile

Módulo base de datos de jugador.

Clase principal:

```txt
net.ariatus.project.AriatusProfile
```

AriatusProfile será la base de casi todos los sistemas futuros.

Responsabilidades:

```txt
✔ cargar perfiles
✔ crear perfiles
✔ cache en memoria
✔ guardar async
✔ autosave
✔ save on quit
✔ playtime real
✔ nivel
✔ experiencia actual
✔ experiencia total
✔ reputación
✔ estadísticas básicas
✔ preferencias
✔ metadata
✔ cooldowns
✔ servicio público para otros módulos
```

### Importante sobre economía

`AriatusProfile` NO debe ser dueño de la economía.

No debe guardar dinero principal ni Odrys directamente.

La economía se moverá a:

```txt
AriatusEconomy
```

AriatusEconomy será responsable de:

```txt
moneda principal $
moneda premium Odrys
balances
transacciones
logs
auditoría
mercado dinámico
```

Odrys tendrá valor real:

```txt
1 Odrys = 1,00€
```

Por eso debe tener un sistema más serio que un simple campo en Profile.

### Datos principales de Profile

```txt
uuid
name
level
experience
totalExperience
reputation
playtimeSeconds
firstLogin
lastLogin
stats
preferences
metadata
cooldowns
```

### Experiencia

Se usan 3 valores:

```txt
level              → nivel actual
experience         → XP actual dentro del nivel
totalExperience    → XP histórica total
```

La experiencia requerida para subir de nivel se calcula mediante `ExperienceProvider`.

### Reputación

La reputación será un sistema central.

Ejemplos futuros:

```txt
+1000 = héroe
+500 = honorable
0 = neutral
-500 = sospechoso
-1000 = criminal
-3000 = enemigo público
```

La reputación podrá afectar:

```txt
acceso a ciudades
misiones
NPCs
mercados
guardias
regiones
PvP
clanes
```

### Tablas actuales propuestas

```txt
ariatus_profiles
ariatus_profile_stats
ariatus_profile_preferences
ariatus_profile_metadata
ariatus_profile_cooldowns
```

---

## 10. AriatusEconomy — futuro

AriatusEconomy será un módulo propio inspirado en Vault, pero adaptado al ecosistema Ariatus.

Tendrá dos monedas:

### Moneda principal

Símbolo:

```txt
$
```

Uso:

```txt
compras normales
NPCs
mercados
subastas
misiones
tradeos
```

### Moneda premium

Nombre:

```txt
Odrys
```

Valor:

```txt
1 Odrys = 1,00€
```

Debe tener:

```txt
logs estrictos
transacciones
auditoría
historial de movimientos
control administrativo
protección contra duplicaciones
```

---

## 11. AriatusChat — futuro

El chat será global entre regiones.

Formato deseado:

```txt
[Mundo] %rank% %playername% > %message%
```

Ejemplo:

```txt
[Ariath] Owner Josner > Hola mundo
```

Futuras variantes:

```txt
chat global
chat local
chat staff
chat clan
chat región
chat privado
```

Para comunicación entre servidores probablemente se usará Redis Pub/Sub o un bridge propio con Velocity.

---

## 12. AriatusCinematics / PreLobby — futuro

Sistema para cinemáticas y experiencia inicial.

Uso principal:

```txt
entrada al servidor
menú de perfil
viajes entre regiones
misiones
bosses
dungeons
eventos
```

El PreLobby debería mostrar:

```txt
JUGAR
PERFIL
PREFERENCIAS
SALIR
```

Más adelante:

```txt
colecciones
estadísticas
regiones
diario de aventuras
mercado
```

---

## 13. ItemsAdder

Se recomienda usar ItemsAdder solo como capa visual/resourcepack.

Uso recomendado:

```txt
✔ texturas
✔ modelos
✔ items visuales
✔ bloques custom visuales
✔ HUD/GUI visual
✔ emojis
✔ muebles/decoración
```

No usar ItemsAdder como fuente de lógica central.

La lógica debe estar en módulos Ariatus:

```txt
AriatusItems
AriatusCreatures
AriatusSkills
AriatusEconomy
```

ItemsAdder aporta apariencia; Ariatus decide stats, economía, drops y comportamiento.

---

## 14. Bedrock / Geyser

Decisión actual recomendada:

```txt
Ariatus.NET Java Edition only al inicio
```

Motivo:

* Mejor compatibilidad con resourcepack.
* Menos fricción visual.
* Mejor control sobre cinemáticas.
* Menos doble trabajo con modelos/texturas.

Soporte Bedrock con Geyser/Floodgate puede considerarse en una fase futura.

---

## 15. Problema resuelto con VPS / OVH

Se detectó que OVH ponía el VPS en modo seguro por tráfico TCP SYN hacia:

```txt
134.255.233.188:888
```

Diagnóstico realizado:

```txt
Velocity no era el problema.
Paper limpio no era el problema.
JMusic no era el problema.
AriatusCore no parecía ser el problema.
J3Login era el causante.
```

Decisión:

```txt
J3Login queda desactivado por ahora.
Se reprogramará en el futuro si es necesario.
```

Reglas futuras para J3Login:

```txt
sin dependencias raras
sin sockets externos no documentados
sin telemetry
sin updater automático
sin conexiones salientes ocultas
logs claros de cualquier conexión externa
```

---

## 16. Reglas importantes de desarrollo

### Package principal

Para módulos nuevos:

```txt
net.ariatus.project
```

### Nombres de clases principales

Usar nombres simples:

```txt
AriatusScoreboard
AriatusProfile
AriatusEconomy
AriatusChat
```

No usar:

```txt
AriatusScoreboardModule
```

### Configs por módulo

Cada módulo puede tener varios YAML:

```txt
config.yml
messages.yml
nametags.yml
belowname.yml
animations.yml
```

AriatusCore debe copiar los resources desde el JAR del módulo, no desde el JAR del Core.

### Comunicación entre módulos

Siempre mediante:

```txt
AriatusCore API + ServiceRegistry
```

No importar clases internas entre módulos.

### Economía

No mezclar economía dentro de Profile.

### Hotreload

Los módulos deben limpiar bien:

```txt
tasks
listeners
commands
configs
classloaders
```

Evitar static instances que impidan liberar classloaders.

---

## 17. Estado actual del proyecto

### AriatusCore

Estado: base avanzada funcional.

Pendiente:

```txt
mejorar APIs compartidas
más documentación
module tree / failed command opcional
más pruebas de classloader
```

### AriatusScoreboard

Estado: funcional y bastante completo.

Pendiente futuro:

```txt
Smart Update Optimization
integración persistente con Profile preferences
API pública para otros módulos
suffix selector real
contextual scoreboard
region branding
```

### AriatusProfile

Estado: base funcional inicial.

Ya tiene:

```txt
profile model
cache
load/save async
autosave
playtime
stats
experience system
/profile
/profileadmin
```

Pendiente inmediato:

```txt
refactor API compartida en AriatusCore
integración limpia con AriatusScoreboard
metadata/preference commands
migraciones automáticas propias
```

---

## 18. Roadmap sugerido próximo

Orden recomendado:

```txt
1. Terminar APIs compartidas en AriatusCore
2. Ajustar AriatusProfile para implementar ProfileService/ProfileView/ExperienceProvider
3. Ajustar AriatusScoreboard para consumir esas APIs
4. Integrar scoreboard toggle con ProfilePreferences
5. Crear migraciones automáticas para AriatusProfile
6. Empezar AriatusEconomy
7. Crear AriatusPermissions
8. Crear AriatusChat global
9. Crear AriatusVelocity
10. Crear PreLobby/Cinematics
```

---

## 19. Recordatorio conceptual

Ariatus no debe sentirse como:

```txt
un survival con plugins
```

Debe sentirse como:

```txt
un mundo MMORPG sandbox modular, persistente y distribuido
```

La prioridad es mantener:

```txt
arquitectura limpia
control total
modularidad
inmersión
escalabilidad
identidad visual premium
```
