# Etapa 1: build con jlink y zona horaria
FROM amazoncorretto:17-alpine-jdk AS builder

# Configura zona horaria para Colombia
RUN apk add --no-cache tzdata
RUN cp /usr/share/zoneinfo/America/Bogota /etc/localtime
RUN echo "America/Bogota" > /etc/timezone

# Crea un runtime Java más liviano con solo los módulos necesarios
RUN jlink --compress=2 --module-path "$JAVA_HOME/jmods" \
  --add-modules java.base,java.logging,java.xml,jdk.unsupported,java.sql,java.naming,java.desktop,java.management,java.security.jgss,java.instrument,jdk.management,jdk.crypto.cryptoki \
  --no-header-files --no-man-pages --output /jlinked

# Etapa 2: imagen final de ejecución
FROM amazoncorretto:17-alpine-jdk

# Zona horaria y usuario seguro
COPY --from=builder /etc/localtime /etc/localtime
RUN echo "America/Bogota" > /etc/timezone && addgroup -S appuser && adduser -S appuser -G appuser
USER appuser

# Configura JAVA_HOME y PATH
ENV JAVA_HOME=/opt/jdk
ENV PATH=$JAVA_HOME/bin:$PATH

# Copia el runtime liviano
COPY --from=builder /jlinked /opt/jdk/

# Copia el archivo JAR de order-service
COPY target/*.jar app.jar

# Expone el puerto del servicio de órdenes (ajusta si usas otro)
EXPOSE 8083

# Comando para ejecutar la app
ENTRYPOINT ["java", "-jar", "/app.jar"]
