let playerId = null;
let gameStarted = false;
let yourSymbol = null;
let pollingInterval = null;

const joinBtn = document.getElementById("joinBtn");
const gameBoard = document.getElementById("gameBoard");
const statusMessage = document.getElementById("statusMessage");
const playerInfo = document.getElementById("playerInfo");
const gameInfo = document.getElementById("gameInfo");
const yourSymbolSpan = document.getElementById("yourSymbol");
const currentTurnSpan = document.getElementById("currentTurn");
const cells = document.querySelectorAll(".cell");
joinBtn.addEventListener("click", joinGame);
cells.forEach((cell) => {
  cell.addEventListener("click", () => {
    const index = parseInt(cell.getAttribute("data-index"));
    makeMove(index);
  });
});

async function joinGame() {
  joinBtn.disabled = true;
  statusMessage.textContent = "Joining game...";
  try {
    const url = playerId ? `/api/join?playerId=${playerId}` : "/api/join";
    const response = await fetch(url, {
      method: "POST",
    });
    const data = await response.json();
    if (data.status === "waiting") {
      playerId = data.playerId;
      statusMessage.textContent = data.message;
      playerInfo.textContent = `Your ID: ${playerId}`;
      startPolling();
    } else if (data.status === "playing") {
      playerId = data.playerId;
      yourSymbol = data.symbol;
      gameStarted = true;
      statusMessage.textContent = data.message;
      playerInfo.textContent = `Your ID: ${playerId}`;
      gameBoard.style.display = "grid";
      gameInfo.style.display = "block";
      joinBtn.style.display = "none";
      yourSymbolSpan.textContent = yourSymbol;
      yourSymbolSpan.className = "symbol " + yourSymbol;
      startPolling();
    }
  } catch (error) {
    console.error("Error joining game:", error);
    statusMessage.textContent = "Error connecting to server!";
    joinBtn.disabled = false;
  }
}

async function makeMove(position) {
  if (!gameStarted) return;
  try {
    const response = await fetch(
      `/api/move?playerId=${playerId}&position=${position}`,
      {
        method: "POST",
      },
    );
    if (response.ok) {
      const data = await response.json();
      updateGameState(data);
    } else {
      const error = await response.json();
      console.error("Invalid move:", error);
    }
  } catch (error) {
    console.error("Error making move:", error);
  }
}

function startPolling() {
  if (pollingInterval) {
    clearInterval(pollingInterval);
  }

  pollingInterval = setInterval(async () => {
    try {
      const response = await fetch(`/api/state?playerId=${playerId}`);
      const data = await response.json();
      if (data.status === "waiting") {
        statusMessage.textContent = "Waiting for opponent...";
      } else {
        if (!gameStarted && data.status === "playing") {
          gameStarted = true;
          yourSymbol = data.yourSymbol;
          gameBoard.style.display = "grid";
          gameInfo.style.display = "block";
          joinBtn.style.display = "none";
          yourSymbolSpan.textContent = yourSymbol;
          yourSymbolSpan.className = "symbol " + yourSymbol;
        }
        updateGameState(data);
      }
    } catch (error) {
      console.error("Error polling:", error);
    }
  }, 500);
}

function updateGameState(data) {
  const board = data.board;
  cells.forEach((cell, index) => {
    const value = board[index];
    cell.textContent = value;
    if (value) {
      cell.classList.add("filled");
      cell.classList.add(value.toLowerCase());
    } else {
      cell.classList.remove("filled");
      cell.classList.remove("x", "o");
    }
  });

  if (data.currentTurn) {
    currentTurnSpan.textContent = data.currentTurn;
    currentTurnSpan.className = "symbol " + data.currentTurn;
  }

  if (data.isYourTurn && data.status === "playing") {
    cells.forEach((cell) => {
      if (!cell.classList.contains("filled")) {
        cell.classList.remove("disabled");
      }
    });
    statusMessage.textContent = "Your turn!";
    statusMessage.style.color = "#27ae60";
  } else if (data.status === "playing") {
    cells.forEach((cell) => cell.classList.add("disabled"));
    statusMessage.textContent = "Opponent's turn...";
    statusMessage.style.color = "#f39c12";
  }

  if (data.status === "finished") {
    clearInterval(pollingInterval);
    cells.forEach((cell) => cell.classList.add("disabled"));
    if (data.winner === "DRAW") {
      statusMessage.textContent = "It's a draw!";
      statusMessage.style.color = "#f39c12";
    } else if (data.winner === yourSymbol) {
      statusMessage.textContent = "🎉 You won!";
      statusMessage.style.color = "#27ae60";
    } else {
      statusMessage.textContent = "You lost!";
      statusMessage.style.color = "#e74c3c";
    }
    setTimeout(() => {
      const playAgainBtn = document.createElement("button");
      playAgainBtn.className = "btn btn-primary";
      playAgainBtn.textContent = "Play Again";
      playAgainBtn.onclick = () => location.reload();
      document.querySelector(".container").appendChild(playAgainBtn);
    }, 2000);
  }
}
