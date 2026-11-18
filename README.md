# Banco Andino · Billetera (App 4 del caso RutaLog Perú)

Esta app empezó como el **Reto de la Semana 2** (login con usuario y clave fijos, saldo y movimientos
inventados). Para el proyecto final se convirtió en la **billetera conectada** al núcleo de Banco Andino:
el login, el saldo y los movimientos vienen del banco, y con ella se **pagan los QR que cobra RutaLog**.
La versión de la Semana 2 queda en `respaldo-semana2/` (no se sube al repositorio).

## Qué hace

| Pantalla | Qué hace |
|---|---|
| Ingreso | Usuario y clave contra el banco (`POST /banco/api/clientes/login`). Recuerda el último usuario (reto extra de la Semana 2). La sesión se guarda cifrada. |
| Panel | Saldo y movimientos reales, con su número de operación (`GET /banco/api/clientes/yo`). |
| Pagar con QR | Escanea el QR que muestra RutaLog (o se escribe el código), muestra a quién, cuánto y por qué, y paga (`POST /banco/api/qr/{codigo}/pagar`). |
| Comprobante | Número de operación (`BA-…`). Si el courier no tiene señal, lo anota y RutaLog lo verifica con el banco después (RF-C06). |

Un pago repetido (por ejemplo, si se corta la red y se vuelve a tocar "Pagar") **no se cobra dos veces**:
cada confirmación lleva un identificador único.

## Cuentas de prueba (clave `banco2026`)

| Usuario | Titular | Para qué sirve en la demostración |
|---|---|---|
| `cliente1` | Carlos Mendoza | El destinatario: paga el contraentrega en la puerta. Trae los movimientos del reto de la Semana 2. |
| `lucia` | Lucía Paredes Quispe | Otra destinataria. |
| `andina` | Distribuidora Andina SAC | Remitente (`cliente@rutalog.pe`): paga sus envíos y recibe su liquidación. |
| `gamarra` | Textiles Gamarra EIRL | Remitente (`cliente2@rutalog.pe`). |
| `rutalog` | RutaLog Perú SAC | Cuenta de recaudo de RutaLog: ahí entran los pagos QR y de ahí salen las liquidaciones. |

## El núcleo de Banco Andino

Corre en el mismo servidor que la API de RutaLog, en `https://app-api-rutalog.vercel.app/banco/...`,
pero como un banco aparte: tablas propias (`banco_*`), su propia clave para servidores y su propio login.
Las apps de RutaLog **nunca** llaman al banco: solo la API de RutaLog, por HTTP y con la clave del banco.

| Endpoint | Quién lo usa |
|---|---|
| `POST /banco/api/cobros-qr`, `GET /banco/api/cobros-qr/{id}` | API de RutaLog (cobro QR del envío o del contraentrega) |
| `GET /banco/api/transferencias/{nro}` | API de RutaLog (verifica cada pago antes de aceptarlo) |
| `POST /banco/api/transferencias/lote` | API de RutaLog (liquidación a los remitentes) |
| `POST /banco/api/creditos`, `GET /banco/api/creditos/{id}` | API de RutaLog (Mi crédito) |
| `POST /banco/api/clientes/login`, `GET /banco/api/clientes/yo`, `/banco/api/qr/...` | Esta app |
| `https://app-api-rutalog.vercel.app/banco/comite` | Web del comité de créditos (clave de prueba `comite2026`) |

## Cómo abrirla

**Lo más rápido:** descargar `BancoAndino-Billetera.apk` desde **Releases** (columna derecha de esta página), permitir
"instalar apps de origen desconocido" y abrirla. Usa el banco publicado en `https://app-api-rutalog.vercel.app`.

**O compilarla:**

1. Android Studio → **Open** → esta carpeta.
2. Esperar la sincronización de Gradle y ejecutar en un teléfono con internet.
3. El lector de QR es el de Google Play Services: no pide permiso de cámara.
