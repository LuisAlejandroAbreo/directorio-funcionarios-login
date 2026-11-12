# Directorio de funcionarios (Spring Boot + MySQL)

## Requisitos
- JDK 21
- Maven 3.9+ (o usa el de la extensión de VS Code)
- MySQL 8 en ejecución (puerto 3306)
- VS Code con "Extension Pack for Java" y "Spring Boot Extension Pack"

## Configuración
Edita `src/main/resources/application.properties` con tu usuario y contraseña de MySQL.
La base `directorio_db` y las tablas `funcionarios` y `usuarios` se crean solas al iniciar.

## Ejecutar
    mvn spring-boot:run
o desde VS Code: abre `DirectorioApplication.java` y pulsa "Run".

Abre: http://localhost:8080 (te llevará al login; crea tu usuario en "Crear usuario").

## Acceso y seguridad (Spring Security)
- Todo el directorio y la API (`/api/v1/**`) exigen iniciar sesión; sin sesión la API responde 401 y las páginas redirigen a `login.html`.
- `registro.html`: crea usuarios. Reglas de contraseña (mensajes en pantalla y validados también en el servidor): 8 a 64 caracteres, mayúscula, minúscula, número, carácter especial, sin espacios, sin contener el usuario ni ser una clave muy común, y confirmación igual.
- Contraseñas guardadas con BCrypt (nunca en texto plano) en la tabla `usuarios`.
- Máximo 3 intentos fallidos: al tercero la cuenta se bloquea 5 minutos (con cuenta regresiva en el login). Se ajusta en `application.properties`:
  `app.seguridad.max-intentos` y `app.seguridad.bloqueo-minutos`.
- Protección CSRF activa (cookie `XSRF-TOKEN` + cabecera `X-XSRF-TOKEN`), sesión de 30 min y cookie `HttpOnly`.
- La API con `curl` ahora requiere sesión/token CSRF, por lo que los ejemplos de abajo se prueban mejor desde el navegador.

## Pruebas rápidas de la API (requieren sesión iniciada)
    curl -i -X POST localhost:8080/api/v1/funcionarios -H "Content-Type: application/json" -d '{"nombre":"Ana Pérez","cargo":"Contadora","departamento":"Contabilidad","telefono":"3001234567"}'
    curl -i localhost:8080/api/v1/funcionarios
    curl -i "localhost:8080/api/v1/funcionarios?nombre=ana"
    curl -i "localhost:8080/api/v1/funcionarios?departamento=contabilidad"
    curl -i -X DELETE localhost:8080/api/v1/funcionarios/1
