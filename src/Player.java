public class Player {
    private String id;
    private String symbol;
    private boolean isMyTurn;
    
    public Player(String id, String symbol) {
        this.id = id;
        this.symbol = symbol;
        this.isMyTurn = false;
    }
    
    public String getId() {
        return id;
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    public boolean isMyTurn() {
        return isMyTurn;
    }
    
    public void setMyTurn(boolean myTurn) {
        isMyTurn = myTurn;
    }
    
    @Override
    public String toString() {
        return "Player{id='" + id + "', symbol='" + symbol + "'}";
    }
}