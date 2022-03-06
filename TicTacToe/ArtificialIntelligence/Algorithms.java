package ArtificialIntelligence;

import TicTacToe.Board;
/**
 * Uses various algorithms to play Tic Tac Toe.
 */
public class Algorithms {


    public AlphaBetaAdvanced test2;

    /**
     * Algorithms cannot be instantiated.
     */
    public Algorithms() {

        test2 = new AlphaBetaAdvanced();
    }

    /**
     * Play a random move.
     * @param board     the Tic Tac Toe board to play on
     */

    /**
     * Play using the MiniMax Algorithm.
     * @param board     the Tic Tac Toe board to play on
     */
    public  void miniMax (Board board) {
        MiniMax.run(board.getTurn(), board, Double.POSITIVE_INFINITY);
    }

    /**
     * Play using the MiniMax algorithm. Include a depth limit.
     * @param board     the Tic Tac Toe board to play on
     * @param ply       the maximum depth
     */
    public  void miniMax (Board board, int ply) {
        MiniMax.run(board.getTurn(), board, ply);
    }

    /**
     * Play using the Alpha-Beta Pruning algorithm.
     * @param board     the Tic Tac Toe board to play on
     */
    public  void alphaBetaPruning (Board board) {
        AlphaBetaPruning.run(board.getTurn(), board, Double.POSITIVE_INFINITY);
    }

    /**
     * Play using the Alpha-Beta Pruning algorithm. Include a depth limit.
     * @param board     the Tic Tac Toe board to play on
     * @param ply       the maximum depth
     */
    public  void alphaBetaPruning (Board board, int ply) {
        AlphaBetaPruning.run(board.getTurn(), board, ply);
    }

    /**
     * Play using the Alpha-Beta Pruning algorithm. Include depth in the
     * evaluation function.
     * @param board     the Tic Tac Toe board to play on
     */
    public void alphaBetaAdvanced (Board board) {
        test2.run(board.getTurn(), board, Double.POSITIVE_INFINITY);
    }

    /**
     * Play using the Alpha-Beta Pruning algorithm. Include depth in the
     * evaluation function and a depth limit.
     * @param board     the Tic Tac Toe board to play on
     * @param ply       the maximum depth
     */
    public  void alphaBetaAdvanced (Board board, int ply) {
        test2.run(board.getTurn(), board, ply);
    }

}
