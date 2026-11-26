# GRUPO 3 — Informe completo de análisis

**Rama:** `main branch`  
**Versión:** `1.0.0`  
**Fecha de análisis:** 2026-10-20

> Transcripción organizada del contenido textual del informe original. Los gráficos sin datos y elementos gráficos de marca no se reproducen. Las entradas que el propio PDF abrevia con `...` se mantienen señaladas como abreviadas; no se inventa información ausente.

## Página 1: Resumen Ejecutivo

Tamaño | Líneas de código | Versión | Umbral de Calidad  
S | 4.494 | 1.0.0 | PASA  
Fiabilidad | Seguridad | Mantenibilidad  
C | A | A  
Evidencias Abiertas | Evidencias Abiertas | Hotspots a revisar | Evidencias Abiertas | Ratio de deuda  
46 | 0 | 0 | 59 | 0,2 %  
Fiabilidad en código nuevo | Seguridad en código nuevo | Mantenibilidad en el código nuevo  
A | A | A  
Nuevas evidencias | Nuevas evidencias | Nuevas evidencias  
0 | 0 | 0  
Hotspot de Seguridad | Ratio de Deuda en el código nuevo  
0 | 0  
Evidencias abiertas por  
cualidad del software e impacto  
Cobertura | Duplicados  
0 | 0 | 0  
0 | 0 | 5  
3 | 0 | 3  
0% | 0,4 %  
0 | 0 | 5  
Tests unitarios | Bloques duplicados  
43 | 0 | 46  
0 | 2  
Bloqueante | Fiabilidad  
Alta | Seguridad  
Media | Mantenibilidad | Cobertura en código nuevo | Duplicados en código nuevo  
0% | 0%  
Evidencias aceptadas | Consistencia | Adaptabilidad  
0 | 0 | 4 | Atributos  
Código | 6  
Falsos positivos | Hotspots revisados | Intencionalidad | Responsabilidad  
0 | 0 | 52 | 0  

## Página 2: Actividad


## Página 3: Detalle de Evidencias

### Evidencias más comunes

Severidad Regla | Cualidad del Software | # Evidencias  
Time-based ".now()" methods should specify a ZoneId or a Clock (Java) | 43  
Time-based ".now()" methods should specify a ZoneId or a Clock (Java) | 43  
Multiple variables should not be declared on the same line (Java) | 4  
Track uses of "TODO" tags (Java) | 3  
Cognitive Complexity of methods should not be too high (Java) | 3  
Equals method should be overridden in records containing array fields (Java) | 3  
String literals should not be duplicated (Java) | 2  
Ternary operators should not be nested (Java) | 2  
Methods should not have too many parameters (Java) | 1  
Lambdas should be replaced with method references (Java) | 1  
Nivel de Severidad | Bloqueante | Múltiples severidades encontradas | Cualidad del Software | Fiabilidad  
Alta | Seguridad  
### Mantenibilidad


## Página 4: Detalle de Evidencias

### Evidencias por Impacto de Severidad

Severidad Regla | Cualidad del Software | # Evidencias  
Cognitive Complexity of methods should not be too high (Java) | 3  
String literals should not be duplicated (Java) | 2  
Equals method should be overridden in records containing array fields (Java) | 3  
Ternary operators should not be nested (Java) | 2  
Methods should not have too many parameters (Java) | 1  
Multiple variables should not be declared on the same line (Java) | 4  
Lambdas should be replaced with method references (Java) | 1  
Time-based ".now()" methods should specify a ZoneId or a Clock (Java) | 43  
Time-based ".now()" methods should specify a ZoneId or a Clock (Java) | 43  
Track uses of "TODO" tags (Java) | 3  
Nivel de Severidad | Bloqueante | Múltiples severidades encontradas | Cualidad del Software | Fiabilidad  
Alta | Seguridad  
### Mantenibilidad


## Página 5: Detalle de Evidencias

### Fiabilidad

Severidad Regla | Cualidad del Software | # Evidencias  
Time-based ".now()" methods should specify a ZoneId or a Clock (Java) | 43  
Equals method should be overridden in records containing array fields (Java) | 3  
Nivel de Severidad | Bloqueante | Múltiples severidades encontradas | Cualidad del Software | Fiabilidad  
Alta | Seguridad  
### Mantenibilidad


## Página 6: Detalle de Evidencias

### Seguridad

Severidad Regla | Cualidad del Software | # Evidencias  
No se encontraron evidencias  
Nivel de Severidad | Bloqueante | Múltiples severidades encontradas | Cualidad del Software | Fiabilidad  
Alta | Seguridad  
### Mantenibilidad


## Página 7: Detalle de Evidencias

### Mantenibilidad

Severidad Regla | Cualidad del Software | # Evidencias  
Time-based ".now()" methods should specify a ZoneId or a Clock (Java) | 43  
Multiple variables should not be declared on the same line (Java) | 4  
Track uses of "TODO" tags (Java) | 3  
Cognitive Complexity of methods should not be too high (Java) | 3  
String literals should not be duplicated (Java) | 2  
Ternary operators should not be nested (Java) | 2  
Methods should not have too many parameters (Java) | 1  
Lambdas should be replaced with method references (Java) | 1  
Nivel de Severidad | Bloqueante | Múltiples severidades encontradas | Cualidad del Software | Fiabilidad  
Alta | Seguridad  
### Mantenibilidad


## Página 8: Detalle de Hotspots de Seguridad

### Hotspots de Seguridad a revisar

Severidad Regla | Cualidad del Software | # Evidencias  
No se encontraron evidencias  
Nivel de Severidad | Bloqueante | Múltiples severidades encontradas | Cualidad del Software | Fiabilidad  
Alta | Seguridad  
### Mantenibilidad


## Página 9: Detalle de Hotspots de Seguridad

### Hotspots revisados

0 | 0 | 0 | 0 0 | de | 0  
confirmados | arreglados | seguros | revisados | revisados  
### Hotspots más revisados

Severidad Regla | Estado | # Evidencias  
No se han encontrado Hotspots de seguridad Revisados  
Nivel de Severidad | Bloqueante | Múltiples severidades encontradas | Cualidad del Software | Fiabilidad  
Alta | Seguridad  
### Mantenibilidad


## Página 10: Mantenibilidad

S3776  
### Regla | Lenguaje | Evidencias

Cognitive Complexity of methods should not be too high | Java | 3  
Cognitive Complexity is a measure of how hard it is to understand the control flow of a unit of code.  
brain-overload  
### Detalle de Evidencias

- **src/main/java/com/sazloop/afiliacion/service/RevisionAfiliacionService.java — línea 110**
Refactor this method to reduce its Cognitive Complexity from 19 to the 15 allowed.  
- **src/main/java/com/sazloop/identidad/service/IndicadoresEmpresaService.java — línea 55**
Refactor this method to reduce its Cognitive Complexity from 16 to the 15 allowed.  
- **src/main/java/com/sazloop/reservas/service/CompraPresencialService.java — línea 61**
Refactor this method to reduce its Cognitive Complexity from 19 to the 15 allowed.  

## Página 11: Mantenibilidad

S1192  
### Regla | Lenguaje | Evidencias

String literals should not be duplicated | Java | 2  
Duplicated string literals make the process of refactoring complex and error-prone, as any change would  
need to be propagated on all occurrences.  
design  
### Detalle de Evidencias

- **src/main/java/com/sazloop/afiliacion/service/RevisionAfiliacionService.java — línea 194**
Define a constant instead of duplicating this literal "Motivo: " 3 times.  
- **src/main/java/com/sazloop/reservas/service/AvisosReservaService.java — línea 43**
Define a constant instead of duplicating this literal " a las " 3 times.  

## Página 12: Fiabilidad

S6218  
### Regla | Lenguaje | Evidencias

Equals method should be overridden in records containing array fields | Java | 3  
In records, the default behavior of the equals() method is to check the equality by field values.  
java16  
### Detalle de Evidencias

- **src/main/java/com/sazloop/afiliacion/service/SolicitudAfiliacionService.java — línea 183**
Override equals, hashCode and toString to consider array's content in the method  
- **src/main/java/com/sazloop/demo/DatosDemoH2.java — línea 152**
Override equals, hashCode and toString to consider array's content in the method  
- **src/main/java/com/sazloop/identidad/service/PerfilService.java — línea 80**
Override equals, hashCode and toString to consider array's content in the method  

## Página 13: Mantenibilidad

S3358  
### Regla | Lenguaje | Evidencias

Ternary operators should not be nested | Java | 2  
Nested ternaries are hard to read and can make the order of operations complex to understand.  
confusing  
### Detalle de Evidencias

- **src/main/java/com/sazloop/afiliacion/service/RevisionAfiliacionService.java — línea 137**
Extract this nested ternary operation into an independent statement.  
- **src/main/java/com/sazloop/afiliacion/service/RevisionAfiliacionService.java — línea 198**
Extract this nested ternary operation into an independent statement.  

## Página 14: Mantenibilidad

S107  
### Regla | Lenguaje | Evidencias

Methods should not have too many parameters | Java | 1  
Methods with a long parameter list are difficult to use because maintainers must figure out the role  
of each parameter and keep track of their position.  
brain-overload  
### Detalle de Evidencias

- **src/main/java/com/sazloop/demo/DatosDemoH2.java — línea 141**
Method has 11 parameters, which is greater than 7 authorized.  

## Página 15: Mantenibilidad

S1659  
### Regla | Lenguaje | Evidencias

Multiple variables should not be declared on the same line | Java | 4  
Declaring multiple variables on one line is difficult to read.  
convention cert  
### Detalle de Evidencias

- **src/main/java/com/sazloop/identidad/service/IndicadoresEmpresaService.java — línea 37**
Declare "cancelaciones" and all following declarations on a separate line.  
- **src/main/java/com/sazloop/identidad/service/IndicadoresEmpresaService.java — línea 38**
Declare "comisiones" and all following declarations on a separate line.  
- **src/main/java/com/sazloop/identidad/service/IndicadoresEmpresaService.java — línea 56**
Declare "cancelaciones" and all following declarations on a separate line.  
- **src/main/java/com/sazloop/identidad/service/IndicadoresEmpresaService.java — línea 57**
Declare "comisiones" and all following declarations on a separate line.  

## Página 16: Mantenibilidad

S1612  
### Regla | Lenguaje | Evidencias

Lambdas should be replaced with method references | Java | 1  
Method or constructor references are more readable than lambda expressions in many situations, and may  
therefore be preferred.  
java8  
### Detalle de Evidencias

- **src/main/java/com/sazloop/ofertas/service/OfertaService.java — línea 159**
Replace this lambda with method reference 'Resena::getCalificacion'. (sonar.java.source not set. Assuming 8...  

## Página 17: Fiabilidad

S8688  
### Regla | Lenguaje | Evidencias

Time-based ".now()" methods should specify a ZoneId or a Clock | Java | 43  
The java.time API provides various classes for working with dates and times.  
datetime java8 timezone pitfall  
### Detalle de Evidencias

- **src/main/java/com/sazloop/afiliacion/domain/AdministradorPlataforma.java — línea 44**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/afiliacion/domain/DocumentoEstablecimiento.java — línea 44**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/afiliacion/domain/EvaluacionAfiliacion.java — línea 34**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/afiliacion/domain/RevisionDocumento.java — línea 34**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/afiliacion/domain/SolicitudAfiliacion.java — línea 25**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/afiliacion/domain/SuspensionEstablecimiento.java — línea 33**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/afiliacion/service/VigenciaDocumentosService.java — línea 137**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/demo/DatosDemoH2.java — línea 145**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/experiencia/domain/ImpactoCO2Evitado.java — línea 43**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/experiencia/domain/Notificacion.java — línea 45**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/experiencia/domain/Resena.java — línea 44**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/experiencia/service/NotificacionService.java — línea 48**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/experiencia/service/NotificacionService.java — línea 63**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/identidad/domain/Consumidor.java — línea 25**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/identidad/domain/CuentaEstablecimiento.java — línea 37**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/identidad/domain/Empresa.java — línea 48**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  

*El documento original indica que hay más entradas, pero no las muestra en esta página.*


## Página 18: Fiabilidad

S8688  
### Detalle de Evidencias

- **src/main/java/com/sazloop/identidad/domain/Establecimiento.java — línea 48**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/identidad/domain/PropietarioEmpresa.java — línea 28**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/identidad/domain/Usuario.java — línea 53**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/identidad/service/CuentaLocalService.java — línea 50**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/ofertas/domain/OfertaPack.java — línea 59**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/ofertas/domain/PackSorpresa.java — línea 65**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/ofertas/service/OfertaService.java — línea 87**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/ofertas/service/OfertaService.java — línea 118**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/ofertas/service/OfertaService.java — línea 129**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/ofertas/service/OfertaService.java — línea 147**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/reservas/domain/CodigoRecojo.java — línea 33**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/reservas/domain/Recojo.java — línea 28**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/reservas/domain/Reserva.java — línea 53**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/reservas/service/CancelacionService.java — línea 40**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/reservas/service/CancelacionService.java — línea 45**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/reservas/service/CompraPresencialService.java — línea 67**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/reservas/service/CompraPresencialService.java — línea 133**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/reservas/service/CompraPresencialService.java — línea 169**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/reservas/service/CompraRecojoService.java — línea 61**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/reservas/service/CompraRecojoService.java — línea 130**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/reservas/service/CompraRecojoService.java — línea 178**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  

## Página 19: Fiabilidad

S8688  
### Detalle de Evidencias

- **src/main/java/com/sazloop/reservas/service/ReservaVencimientoService.java — línea 25**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/transacciones/domain/BoletaSimulada.java — línea 37**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/transacciones/domain/CancelacionReserva.java — línea 45**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/transacciones/domain/ComisionPlataforma.java — línea 46**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/transacciones/domain/Pago.java — línea 49**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/transacciones/domain/Reembolso.java — línea 36**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  

## Página 20: Mantenibilidad

S8688  
### Regla | Lenguaje | Evidencias

Time-based ".now()" methods should specify a ZoneId or a Clock | Java | 43  
The java.time API provides various classes for working with dates and times.  
datetime java8 timezone pitfall  
### Detalle de Evidencias

- **src/main/java/com/sazloop/afiliacion/domain/AdministradorPlataforma.java — línea 44**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/afiliacion/domain/DocumentoEstablecimiento.java — línea 44**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/afiliacion/domain/EvaluacionAfiliacion.java — línea 34**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/afiliacion/domain/RevisionDocumento.java — línea 34**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/afiliacion/domain/SolicitudAfiliacion.java — línea 25**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/afiliacion/domain/SuspensionEstablecimiento.java — línea 33**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/afiliacion/service/VigenciaDocumentosService.java — línea 137**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/demo/DatosDemoH2.java — línea 145**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/experiencia/domain/ImpactoCO2Evitado.java — línea 43**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/experiencia/domain/Notificacion.java — línea 45**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/experiencia/domain/Resena.java — línea 44**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/experiencia/service/NotificacionService.java — línea 48**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/experiencia/service/NotificacionService.java — línea 63**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/identidad/domain/Consumidor.java — línea 25**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/identidad/domain/CuentaEstablecimiento.java — línea 37**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/identidad/domain/Empresa.java — línea 48**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  

*El documento original indica que hay más entradas, pero no las muestra en esta página.*


## Página 21: Mantenibilidad

S8688  
### Detalle de Evidencias

- **src/main/java/com/sazloop/identidad/domain/Establecimiento.java — línea 48**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/identidad/domain/PropietarioEmpresa.java — línea 28**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/identidad/domain/Usuario.java — línea 53**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/identidad/service/CuentaLocalService.java — línea 50**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/ofertas/domain/OfertaPack.java — línea 59**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/ofertas/domain/PackSorpresa.java — línea 65**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/ofertas/service/OfertaService.java — línea 87**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/ofertas/service/OfertaService.java — línea 118**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/ofertas/service/OfertaService.java — línea 129**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/ofertas/service/OfertaService.java — línea 147**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/reservas/domain/CodigoRecojo.java — línea 33**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/reservas/domain/Recojo.java — línea 28**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/reservas/domain/Reserva.java — línea 53**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/reservas/service/CancelacionService.java — línea 40**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/reservas/service/CancelacionService.java — línea 45**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/reservas/service/CompraPresencialService.java — línea 67**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/reservas/service/CompraPresencialService.java — línea 133**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/reservas/service/CompraPresencialService.java — línea 169**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/reservas/service/CompraRecojoService.java — línea 61**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/reservas/service/CompraRecojoService.java — línea 130**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/reservas/service/CompraRecojoService.java — línea 178**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  

## Página 22: Mantenibilidad

S8688  
### Detalle de Evidencias

- **src/main/java/com/sazloop/reservas/service/ReservaVencimientoService.java — línea 25**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/transacciones/domain/BoletaSimulada.java — línea 37**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/transacciones/domain/CancelacionReserva.java — línea 45**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/transacciones/domain/ComisionPlataforma.java — línea 46**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/transacciones/domain/Pago.java — línea 49**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  
- **src/main/java/com/sazloop/transacciones/domain/Reembolso.java — línea 36**
Explicitly specify the time zone by passing a ZoneId or a Clock to the .now() method.  

## Página 23: Mantenibilidad

S1135  
### Regla | Lenguaje | Evidencias

Track uses of "TODO" tags | Java | 3  
Developers often use TODO tags to mark areas in the code where additional work or improvements are needed  
but are not implemented immediately.  
cwe  
### Detalle de Evidencias

- **src/main/java/com/sazloop/SazloopApplication.java — línea 49**
Complete the task associated to this TODO comment.  
- **src/main/java/com/sazloop/afiliacion/domain/SuspensionEstablecimiento.java — línea 11**
Complete the task associated to this TODO comment.  
- **src/main/java/com/sazloop/identidad/package-info.java — línea 4**
Complete the task associated to this TODO comment.  
