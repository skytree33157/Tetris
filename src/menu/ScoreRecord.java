package menu;

public class ScoreRecord {

    private String name;
    private int score;

    public ScoreRecord(String name, int score) {
        this.name = name;
        this.score = score;
    }

    public String getName() {
        return name;
    }

    public int getScore() {
        return score;
    }
}