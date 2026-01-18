FROM openjdk:17-slim
WORKDIR /app
COPY . .
RUN javac src/*.java
EXPOSE 8080
CMD ["java", "-cp", "src", "TicTacToeServer"]