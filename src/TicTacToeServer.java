import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class TicTacToeServer {
    private static GameManager gameManager;
    public static void main(String[] args) throws IOException {
        int HTTP_PORT = 8080;
        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.isEmpty()) {
            HTTP_PORT = Integer.parseInt(portEnv);
        }
        gameManager = new GameManager();
        HttpServer server = HttpServer.create(new InetSocketAddress(HTTP_PORT), 0);
        server.createContext("/", new StaticFileHandler());
        server.createContext("/api/join", new JoinGameHandler());
        server.createContext("/api/move", new MakeMoveHandler());
        server.createContext("/api/state", new GetStateHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("===========================================");
        System.out.println("  Tic Tac Toe Server started!");
        System.out.println("===========================================");
        System.out.println("  Server running on port: " + HTTP_PORT);
        System.out.println("===========================================");
    }
    
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if(path.equals("/")) {
                path = "/index.html";
            }
            String filePath = "client" + path;
            File file = new File(filePath);
            if(file.exists() && !file.isDirectory()) {
                byte[] fileBytes = Files.readAllBytes(file.toPath());
                String contentType = getContentType(path);
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, fileBytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(fileBytes);
                os.close();
            } else {
                String response = "404 - File Not Found";
                exchange.sendResponseHeaders(404, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
        
        private String getContentType(String path) {
            if(path.endsWith(".html")) return "text/html";
            if(path.endsWith(".css")) return "text/css";
            if(path.endsWith(".js")) return "application/javascript";
            if(path.endsWith(".json")) return "application/json";
            return "text/plain";
        }
    }
    
    static class JoinGameHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if(!exchange.getRequestMethod().equals("POST")) {
                sendResponse(exchange, 405, "{\"error\": \"Method not allowed\"}");
                return;
            }
            String query = exchange.getRequestURI().getQuery();
            String playerId = null;
            if(query != null) {
                Map<String, String> params = parseQueryParams(query);
                playerId = params.get("playerId");
            }
            if(playerId == null || playerId.isEmpty()) {
                playerId = "player_" + UUID.randomUUID().toString().substring(0, 8);
            }
            Game game = gameManager.addPlayer(playerId);
            if(game == null) {
                String response = "{\"status\": \"waiting\", \"playerId\": \"" + playerId + "\", \"message\": \"Waiting for opponent...\"}";
                sendResponse(exchange, 200, response);
            } else {
                Player player = game.hasPlayer(playerId) 
                    ? (game.getPlayer1().getId().equals(playerId) ? game.getPlayer1() : game.getPlayer2())
                    : null;
                String response = "{\"status\": \"playing\", \"playerId\": \"" + playerId + "\", \"symbol\": \"" + 
                    (player != null ? player.getSymbol() : "?") + "\", \"gameId\": \"" + game.getGameId() + 
                    "\", \"message\": \"Game started!\"}";
                sendResponse(exchange, 200, response);
            }
        }
    }
    
    static class MakeMoveHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if(!exchange.getRequestMethod().equals("POST")) {
                sendResponse(exchange, 405, "{\"error\": \"Method not allowed\"}");
                return;
            }
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = parseQueryParams(query);
            String playerId = params.get("playerId");
            String positionStr = params.get("position");
            if(playerId == null || positionStr == null) {
                sendResponse(exchange, 400, "{\"error\": \"Missing parameters\"}");
                return;
            }
            int position = Integer.parseInt(positionStr);
            Game game = gameManager.getGameForPlayer(playerId);
            if(game == null) {
                sendResponse(exchange, 404, "{\"error\": \"Game not found\"}");
                return;
            }
            boolean success = game.makeMove(playerId, position);
            if(success) {
                String response = buildGameStateJSON(game, playerId);
                sendResponse(exchange, 200, response);
            } else {
                sendResponse(exchange, 400, "{\"error\": \"Invalid move\"}");
            }
        }
    }
    
    static class GetStateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = parseQueryParams(query);
            String playerId = params.get("playerId");
            if(playerId == null) {
                sendResponse(exchange, 400, "{\"error\": \"Missing playerId\"}");
                return;
            }
            if(gameManager.isWaiting(playerId)) {
                sendResponse(exchange, 200, "{\"status\": \"waiting\"}");
                return;
            }
            Game game = gameManager.getGameForPlayer(playerId);
            if(game == null) {
                sendResponse(exchange, 404, "{\"error\": \"Game not found\"}");
                return;
            }
            String response = buildGameStateJSON(game, playerId);
            sendResponse(exchange, 200, response);
        }
    }
    
    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
    
    private static String buildGameStateJSON(Game game, String playerId) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"status\": \"").append(game.getStatus()).append("\",");
        json.append("\"board\": [");
        String[] board = game.getBoard();
        for(int i = 0; i < board.length; i++) {
            json.append("\"").append(board[i]).append("\"");
            if(i < board.length - 1) json.append(",");
        }
        json.append("],");
        json.append("\"currentTurn\": \"").append(game.getCurrentTurnSymbol()).append("\",");
        json.append("\"winner\": ").append(game.getWinner() != null ? "\"" + game.getWinner() + "\"" : "null").append(",");
        Player player = game.getPlayer1().getId().equals(playerId) ? game.getPlayer1() : game.getPlayer2();
        json.append("\"yourSymbol\": \"").append(player.getSymbol()).append("\",");
        json.append("\"isYourTurn\": ").append(player.isMyTurn());
        json.append("}");
        return json.toString();
    }
    
    private static Map<String, String> parseQueryParams(String query) {
        Map<String, String> map = new HashMap<>();
        if(query != null && !query.isEmpty()) {
            String[] pairs = query.split("&");
            for(String pair : pairs) {
                String[] kv = pair.split("=");
                if(kv.length == 2) {
                    map.put(kv[0], kv[1]);
                }
            }
        }
        return map;
    }
}