package lightUpGame;

import java.util.Random;

class PuzzleDatabase {
    private static final int[][][] PUZZLES = {
        // Puzzle 1 - 7x7 Easy (with 0, 1, 2)
        {
            {5,5,5,5,5},
            {5,5,1,5,5},
            {5,0,-1,-1,5},
            {5,5,2,5,5},
            {5,5,5,5,5}
        },
        
        // Puzzle 2 - 7x7 Medium (with 1, 2, 3)
        {
            {5,5,-1,5,5},
            {5,4,5,2,5},
            {5,5,4,5,5},
            {5,-1,5,2,5},
            {5,5,5,5,5}
        },
        
        {
            {5,5,1,5,5},
            {5,5,5,5,5},
            {3,5,-1,5,-1},
            {5,5,5,5,5},
            {5,5,-1,5,5}
        },

        {
            {5,0,5,5,5},
            {5,5,5,5,0},
            {5,5,5,5,5},
            {0,5,5,5,5},
            {0,0,0,2,0}
        },
        
        {
            {5,5,5,-1,5},
            {-1,5,5,5,5},
            {5,5,5,5,5},
            {5,5,5,5,1},
            {5,3,5,5,5}
        }
        
    };
    
    private static Random random = new Random();
    
    public static int[][] getRandomPuzzle() {
        int index = random.nextInt(PUZZLES.length);
        int[][] puzzle = PUZZLES[index];
        
        // Create a deep copy to avoid modifying the original
        int[][] copy = new int[puzzle.length][puzzle[0].length];
        for (int i = 0; i < puzzle.length; i++) {
            System.arraycopy(puzzle[i], 0, copy[i], 0, puzzle[i].length);
        }
        
        System.out.println("Loading Puzzle #" + (index + 1) + " (" + puzzle.length + "x" + puzzle[0].length + ")");
        return copy;
    }
    
    public static int getPuzzleCount() {
        return PUZZLES.length;
    }
}
