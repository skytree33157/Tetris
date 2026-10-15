package storage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import menu.ScoreRecord;


public class ScoreStorage {

    private final String scoreFile;

    public ScoreStorage() {
        this("src/main/java/storage/scores.txt");
    }

    public ScoreStorage(String scoreFile) {
        this.scoreFile = scoreFile;
    }

    public void save(ArrayList<ScoreRecord> records){
        
        try(FileWriter writer = new FileWriter(scoreFile, StandardCharsets.UTF_8);) {
            
            for (ScoreRecord record : records) {
                writer.write(record.getName() + "," + record.getScore() + "," + record.getDifficulty() + "," + record.getMode() + "\n");
            }

        } catch (FileNotFoundException e) {
            System.out.println(e);
        } catch (IOException e) {
            System.out.println(e);
        }
        

    }

    public ArrayList<ScoreRecord> load() {
        
        ArrayList<ScoreRecord> records = new ArrayList<>();

        File file = new File(scoreFile);

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                System.out.println(e);
            }
        }
        
        try (FileReader reader = new FileReader(scoreFile, StandardCharsets.UTF_8);
            BufferedReader buffer = new BufferedReader(reader)) {
            String line = buffer.readLine();

            while (line != null) {

                String[] parts = line.split(",");

                if (parts.length == 4) {
                    try {
                        String name = parts[0].trim();
                        int score = Integer.parseInt(parts[1].trim());
                        String difficulty = parts[2].trim();
                        String mode = parts[3].trim();

                        ScoreRecord record = new ScoreRecord(name, score, difficulty, mode);
                        records.add(record);
                    } catch (NumberFormatException e) {// 잘못된 점수 형식 건너뜀 
                    }
                    
                }

                line = buffer.readLine();
            
            }
        } catch (IOException e)  {
            System.out.println(e);
        }
        
        return records;
    }

    public void addRecord(ScoreRecord record) {
        ArrayList<ScoreRecord> records = load();
        records.add(record);
        save(records);
    }

    public boolean clear() {
        try (FileWriter writer = new FileWriter(scoreFile, StandardCharsets.UTF_8)) {
            return true;
        } catch (IOException e) {
            System.out.println(e);
            return false;
        }
    }
    
}
