import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameManager {
    private Queue<Player> waitingPlayers;
    private Map<String, Game> activeGames;
    private Map<String, String> playerToGame;
    private int gameIdCounter;
    
    public GameManager() {
        this.waitingPlayers = new LinkedList<>();
        this.activeGames = new ConcurrentHashMap<>();
        this.playerToGame = new ConcurrentHashMap<>();
        this.gameIdCounter = 0;
    }
    
    public synchronized Game addPlayer(String playerId) {
        if(playerToGame.containsKey(playerId)) {
            return activeGames.get(playerToGame.get(playerId));
        }
        if(waitingPlayers.isEmpty()) {
            Player newPlayer = new Player(playerId, "X");
            waitingPlayers.add(newPlayer);
            System.out.println("Player " + playerId + " is waiting for an opponent...");
            return null;
        } else {
            Player player1 = waitingPlayers.poll();
            Player player2 = new Player(playerId, "O");
            String gameId = "game_" + (++gameIdCounter);
            Game game = new Game(gameId, player1, player2);
            activeGames.put(gameId, game);
            playerToGame.put(player1.getId(), gameId);
            playerToGame.put(player2.getId(), gameId);
            System.out.println("Game " + gameId + " started: " + player1.getId() + " vs " + player2.getId());
            return game;
        }
    }
    public Game getGameForPlayer(String playerId) {
        String gameId = playerToGame.get(playerId);
        if(gameId != null) {
            return activeGames.get(gameId);
        }
        return null;
    }
    
    public synchronized void removePlayer(String playerId) {
        waitingPlayers.removeIf(p -> p.getId().equals(playerId));
        String gameId = playerToGame.get(playerId);
        if(gameId != null) {
            activeGames.remove(gameId);
            playerToGame.remove(playerId);
            Game game = activeGames.get(gameId);
            if(game != null) {
                String opponentId = game.getPlayer1().getId().equals(playerId) 
                    ? game.getPlayer2().getId() 
                    : game.getPlayer1().getId();
                playerToGame.remove(opponentId);
            }
            System.out.println("Player " + playerId + " disconnected from game " + gameId);
        }
    }
    
    public boolean isWaiting(String playerId) {
        return waitingPlayers.stream().anyMatch(p -> p.getId().equals(playerId));
    }
    
    public int getWaitingPlayersCount() {
        return waitingPlayers.size();
    }
    
    public int getActiveGamesCount() {
        return activeGames.size();
    }
}