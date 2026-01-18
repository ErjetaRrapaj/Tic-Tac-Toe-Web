public class Game {
    private String[] board;
    private Player player1;
    private Player player2;
    private Player currentTurn;
    private String status;
    private String winner;
    private String gameId;
    
    public Game(String gameId, Player p1, Player p2) {
        this.gameId = gameId;
        this.board = new String[9];
        for(int i = 0; i < 9; i++) {
            board[i] = "";
        }
        this.player1 = p1;
        this.player2 = p2;
        this.currentTurn = player1;
        this.status = "playing";
        this.winner = null;
        player1.setMyTurn(true);
        player2.setMyTurn(false);
    }
    
    public synchronized boolean makeMove(String playerId, int position) {
        if(!status.equals("playing")) {
            return false;
        }
        Player player = getPlayerById(playerId);
        if(player == null || !player.equals(currentTurn)) {
            return false;
        }
        if(!isValidMove(position)) {
            return false;
        }
        board[position] = player.getSymbol();
        checkWinner();
        if(status.equals("playing")) {
            switchTurn();
        }
        return true;
    }
    
    private void checkWinner() {
        for(int i = 0; i < 9; i += 3) {
            if(!board[i].isEmpty() && 
               board[i].equals(board[i+1]) && 
               board[i].equals(board[i+2])) {
                winner = board[i];
                status = "finished";
                return;
            }
        }
        for(int i = 0; i < 3; i++) {
            if(!board[i].isEmpty() && 
               board[i].equals(board[i+3]) && 
               board[i].equals(board[i+6])) {
                winner = board[i];
                status = "finished";
                return;
            }
        }
        if(!board[0].isEmpty() && 
           board[0].equals(board[4]) && 
           board[0].equals(board[8])) {
            winner = board[0];
            status = "finished";
            return;
        }
        if(!board[2].isEmpty() && 
           board[2].equals(board[4]) && 
           board[2].equals(board[6])) {
            winner = board[2];
            status = "finished";
            return;
        }
        boolean isFull = true;
        for(String cell : board) {
            if(cell.isEmpty()) {
                isFull = false;
                break;
            }
        }
        if(isFull) {
            winner = "DRAW";
            status = "finished";
        }
    }
    
    private boolean isValidMove(int position) {
        if(position < 0 || position > 8) {
            return false;
        }
        return board[position].isEmpty();
    }
    
    private void switchTurn() {
        if(currentTurn.equals(player1)) {
            currentTurn = player2;
            player1.setMyTurn(false);
            player2.setMyTurn(true);
        } else {
            currentTurn = player1;
            player1.setMyTurn(true);
            player2.setMyTurn(false);
        }
    }
    
    private Player getPlayerById(String playerId) {
        if(player1.getId().equals(playerId)) {
            return player1;
        } else if(player2.getId().equals(playerId)) {
            return player2;
        }
        return null;
    }
    
    public Player getPlayerBySymbol(String symbol) {
        if(player1.getSymbol().equals(symbol)) {
            return player1;
        } else if(player2.getSymbol().equals(symbol)) {
            return player2;
        }
        return null;
    }
    
    public String getGameId() { 
        return gameId; 
    }
    
    public String[] getBoard() { 
        return board; 
    }
    
    public String getStatus() { 
        return status; 
    }
    
    public String getWinner() { 
        return winner; 
    }
    
    public String getCurrentTurnSymbol() { 
        return currentTurn != null ? currentTurn.getSymbol() : null; 
    }
    
    public Player getPlayer1() {
        return player1;
    }
    
    public Player getPlayer2() {
        return player2;
    }
    
    public boolean hasPlayer(String playerId) {
        return player1.getId().equals(playerId) || player2.getId().equals(playerId);
    }
}