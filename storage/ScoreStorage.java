package storage;

import menu.ScoreRecord;
import java.util.ArrayList;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;


public class ScoreStorage {

    private static final String SCORE_FILE = "scores.txt";

    public void save(ArrayList<ScoreRecord> records){
        
        try(FileWriter writer = new FileWriter(SCORE_FILE);) {
            
            for (ScoreRecord record : records) {
                writer.write(record.getName() + "," + record.getScore() + "\n");

            }

        } catch (FileNotFoundException e) {
            System.out.println(e);
        } catch (IOException e) {
            System.out.println(e);
        }
        

    }

    public ArrayList<ScoreRecord> load() {
        
        ArrayList<ScoreRecord> records = new ArrayList<>();
        
        try (FileReader reader = new FileReader(SCORE_FILE);
            BufferedReader buffer = new BufferedReader(reader)) {
            String line = buffer.readLine();

            while (line != null) {

                String[] parts = line.split(",");

                if (parts.length == 2) {
                    try {
                        String name = parts[0].trim();
                        int score = Integer.parseInt(parts[1].trim());

                        ScoreRecord record = new ScoreRecord(name, score);
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
    
}
