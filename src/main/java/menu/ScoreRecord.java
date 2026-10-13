package menu;

public class ScoreRecord {

    private String name;

    private int score;

    private String difficulty;
    
    private String mode;

    public ScoreRecord(String name, int score, String difficulty, String mode) {
        this.name = name;
        this.score = score;
        this.difficulty = difficulty;
        this.mode = mode;
    }

    public String getName() {
        return name;
    }

    public int getScore() {
        return score;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public String getMode() {
        return mode;
    }
}