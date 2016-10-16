
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;


/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Yasser
 */
public class TextCP {

    public String CorrectionFile;
    public String predictionFile;
    public String arabicTrainingFile;
    public HashMap<String, Integer> cMap = new HashMap<String, Integer>();
    public HashMap<String, HashMap<String, Integer>> pMap = new HashMap<>();
    public char[] arabicAlph;

    public TextCP() {
        CorrectionFile = "getCorrectionFile.ser";
        predictionFile = "predictionFile.ser";
        arabicTrainingFile = "collection.txt";
        arabicAlph = new char[]{'ا', 'ب', 'ت', 'ث', 'ج', 'ح', 'خ', 'د', 'ذ', 'ر', 'ز', 'س', 'ش', 'ص', 'ض', 'ط', 'ظ', 'ع', 'غ', 'ف', 'ق', 'ك', 'ل', 'م', 'ن', 'ه', 'و', 'ي', 'ئ', 'ؤ', 'ء', 'أ'};
        cMap = new HashMap<String, Integer>();
        pMap = new HashMap<>();

    }

    //train the correction hashmap by counting the number of occurences 
    //of each word in the training text
    public void trainingCorrection() throws IOException {
        Reader r = new InputStreamReader(new FileInputStream(arabicTrainingFile), "Windows-1256");
        // Reader r = new InputStreamReader(new FileInputStream("test2.txt"));
        BufferedReader in = new BufferedReader(r);
        for (String temp = ""; temp != null; temp = in.readLine()) {
            String arr[] = temp.split(" ");
            for (int i = 0; i < arr.length; i++) {
                if (!"".equals(arr[i])) {
                    temp = arr[i];
                    cMap.put(temp, cMap.containsKey(temp) ? cMap.get(temp) + 1 : 1);
                }
            }
        }
        in.close();
    }

    // train the bigram by counting how much two words
    // come after each other
    public void trainingPrediction() throws IOException {
        String previousWord;
        Reader r = new InputStreamReader(new FileInputStream(arabicTrainingFile), "Windows-1256");
        BufferedReader in = new BufferedReader(r);
        for (String temp = ""; temp != null; temp = in.readLine()) {
            previousWord = null;
            String arr[] = temp.split(" ");
            for (int i = 0; i < arr.length; i++) {
                temp = arr[i];
                if (previousWord != null) {
                    HashMap<String, Integer> innerWord;
                    if (pMap.containsKey(previousWord)) {
                        innerWord = pMap.get(previousWord);
                        if (pMap.get(previousWord).containsKey(temp)) {
                            pMap.get(previousWord).put(temp, innerWord.get(temp) + 1);
                        } else {
                            pMap.get(previousWord).put(temp, 1);
                        }
                    } else {
                        innerWord = new HashMap<String, Integer>();
                        innerWord.put(temp, 1);
                        pMap.put(previousWord, innerWord);

                    }
                }
                previousWord = temp;
            }
        }

    }

    //Derive other words from the current word using
    // inserts, deletes, transposes, replaces of letters
    public ArrayList<String> edits(String word) {
        ArrayList<String> result = new ArrayList<String>();
        // delete one letter from word
        for (int i = 0; i < word.length(); ++i) {
            result.add(word.substring(0, i) + word.substring(i + 1));
        }
        //transposes
        for (int i = 0; i < word.length() - 1; ++i) {
            result.add(word.substring(0, i) + word.substring(i + 1, i + 2) + word.substring(i, i + 1) + word.substring(i + 2));
        }
        // replaces
        for (int i = 0; i < word.length(); ++i) {
            for (char c : arabicAlph) {
                result.add(word.substring(0, i) + String.valueOf(c) + word.substring(i + 1));
            }
        }
        // inserts
        for (int i = 0; i <= word.length(); ++i) {
            for (char c : arabicAlph) {
                result.add(word.substring(0, i) + String.valueOf(c) + word.substring(i));
            }
        }
        return result;
    }

    //get corrections from hashmap by getting word derived from the 
    //the current word using function edits then checking if they are
    // in the corrections map if they are return the three highest
    // ones
    public ArrayList<String> getCorrection(String word) {
        ArrayList<String> suggests = new ArrayList<>();
        ArrayList<String> list = edits(word);
        HashMap<Integer, String> candidates = new HashMap<>();

        //edit distance 1
        for (int i = 0; i < list.size(); i++) {
            if (cMap.containsKey(list.get(i))) {
                candidates.put(cMap.get(list.get(i)), list.get(i));
            }
        }
        if (candidates.size() > 0) {
            while (!candidates.isEmpty() && suggests.size() < 3) {
                suggests.add(candidates.get(Collections.max(candidates.keySet())));
                candidates.remove(Collections.max(candidates.keySet()));
            }

        }

        if (suggests.size() >= 3) {
            return suggests;
        }

        //edit distance 2
        for (String s : list) {
            for (String w : edits(s)) {
                if (cMap.containsKey(w)) {
                    candidates.put(cMap.get(w), w);
                }
            }
        }
        while (!candidates.isEmpty() && suggests.size() < 3) {
            suggests.add(candidates.get(Collections.max(candidates.keySet())));
            candidates.remove(Collections.max(candidates.keySet()));
        }

        if (candidates.size() < 3) {
            suggests.add(word);
        }
        return suggests;
    }

    // get prediction from the hashmap using current word to 
    // predict the next one
    public ArrayList<String> getPredictions(String word) {
        ArrayList<String> suggests = new ArrayList<>();

        HashMap<String, Integer> temp = pMap.get(word);
        HashMap<Integer, String> candidates = new HashMap<Integer, String>();
        if (temp != null) {
            for (Map.Entry entry : temp.entrySet()) {
                candidates.put((Integer) entry.getValue(), entry.getKey().toString());
            }
        }
        while (!candidates.isEmpty() && suggests.size() < 3) {
            suggests.add(candidates.get(Collections.max(candidates.keySet())));
            candidates.remove(Collections.max(candidates.keySet()));
        }
        return suggests;
    }

    // write hashmaps to file
    public void writeToFile() {
        try {
            FileOutputStream fos = new FileOutputStream(CorrectionFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(cMap);
            oos.close();
            fos.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        try {
            FileOutputStream fos = new FileOutputStream(predictionFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(pMap);
            oos.close();
            fos.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    // read values from files into hashmap
    public void readFromFile() throws ClassNotFoundException {

        try {
            FileInputStream fis = new FileInputStream(CorrectionFile);
            ObjectInputStream ois = new ObjectInputStream(fis);
            cMap = (HashMap) ois.readObject();
            ois.close();
            fis.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();

        }
        try {
            FileInputStream fis = new FileInputStream(predictionFile);
            ObjectInputStream ois = new ObjectInputStream(fis);
            pMap = (HashMap) ois.readObject();
            ois.close();
            fis.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();

        }
    }

    // display word count from hashmap
    public void displayCorrectionsHash() {
        Set set = cMap.entrySet();
        Iterator iterator = set.iterator();
        int count = 0;
        while (count != 5 && iterator.hasNext()) {
            Map.Entry mEntry = (Map.Entry) iterator.next();
            System.out.print("Key: \"" + mEntry.getKey() + "\" | Value: ");
            System.out.println(mEntry.getValue());
            count++;
        }
    }

    // display bigram from hasmap
    public void displayPredictionsHash() {
        for (Map.Entry<String, HashMap<String, Integer>> entry : pMap.entrySet()) {
            HashMap<String, Integer> x = entry.getValue();
            for (Map.Entry<String, Integer> entry2 : x.entrySet()) {
                System.out.println("Key1: " + entry.getKey() + " Key2: " + entry2.getKey() + " | Value: " + entry2.getValue());
            }
        }
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        TextCP x = new TextCP();
        x.trainingCorrection();
        x.trainingPrediction();
        //x.writeToFile();
        //x.readFromFile();
        System.out.println("Learning Done");
        //displayPredictionsHash();
        //displayCorrectionsHash();

        Scanner sc = new Scanner(System.in);
        String word;
        while (true) {
            System.out.println();
            System.out.println("أدخل كلمة:");
            word = sc.nextLine();
            System.out.println();
            System.out.println("تصحيحات لكلمة " + word + " :");
            ArrayList<String> getCorrection = x.getCorrection(word);
            for (int i = 0; i < getCorrection.size(); i++) {
                System.out.print(getCorrection.get(i));
                if (i != getCorrection.size() - 1) {
                    System.out.print("،   ");
                }
            }
            System.out.println();
            System.out.println("أدخل كلمة:\n");
            word = sc.nextLine();
            System.out.println();
            System.out.println("تنبؤات للكلمة القادمة :");
            ArrayList<String> prediction = x.getPredictions(word);
            for (int i = 0; i < prediction.size(); i++) {
                System.out.print(prediction.get(i));
                if (i != prediction.size() - 1) {
                    System.out.print(",    ");
                }
            }
        }

    }
}
