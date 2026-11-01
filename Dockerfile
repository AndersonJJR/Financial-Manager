# ==============================================================================
# Estágio 1: Build (Compilação e empacotamento do fat JAR)
# ==============================================================================
FROM docker.io/library/eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

# Copia a estrutura do Maven Wrapper e o POM para cache eficiente de camadas
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Garante permissão de execução no script mvnw
RUN chmod +x ./mvnw

# Pré-carrega as dependências do Maven (otimização de cache)
RUN ./mvnw dependency:go-offline -B

# Copia o código-fonte
COPY src/ src/

# Executa o build do artefato executável
RUN ./mvnw clean package -DskipTests

# ==============================================================================
# Estágio 2: Runtime (Imagem final enxuta com JRE 21)
# ==============================================================================
FROM docker.io/library/eclipse-temurin:21-jre-jammy

WORKDIR /app

# Criação de usuário e grupo não-root para segurança de execução
RUN addgroup --system spring && adduser --system --ingroup spring spring

# Copia o JAR gerado no estágio de build
COPY --from=builder /app/target/*.jar app.jar

# Define posse dos arquivos para o usuário seguro
RUN chown -R spring:spring /app

# Execução com usuário sem privilégios administrativos
USER spring:spring

# Porta padrão da aplicação Spring Boot
EXPOSE 8080

# Configurações de JVM otimizadas para limites de memória em contêineres
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
