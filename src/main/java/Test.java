import org.apache.log4j.BasicConfigurator;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.text.sentenceiterator.LineSentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentencePreProcessor;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.*;

import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Test {
    static {
        com.mysql.jdbc.Driver.class.getName();
    }

    public static void main(String[]args) throws Exception {
        BasicConfigurator.configure();

        Class.forName("com.mysql.jdbc.Driver");
        Connection conn = null;
        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/dictionary","java", "password");
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        if (conn != null) {
            System.out.println("You made it, take control your database now!");
        } else {
            System.out.println("Failed to make connection!");
        }

        Statement statement;
        PreparedStatement preparedStatement;
        ResultSet resultSet;

        statement = conn.createStatement();

        // Result set get the result of the SQL query
        resultSet = statement
                .executeQuery("select * from dictionary.entries");

        //writeResultSet(resultSet);

        String searchTerm = "bender quotes";
        String stringToMessUp = "Fly with me my young one";
        String filename = "corpus.txt";



        String searchTerms = searchTerm + " site:stackoverflow.com";
        int num = 10; // number of search terms
        String searchURL = "https://www.google.com/search" + "?q="+searchTerm+"&num="+num;
        //without proper User-Agent, we will get 403 error
        String url = "http://www.google.com"; // default output

        try {
            org.jsoup.nodes.Document doc = Jsoup.connect(searchURL).userAgent("Mozilla/5.0").get();
            Elements results = doc.select("h3.r > a");
            for (Element result : results) {
                String linkHref = result.attr("href");
                url = linkHref.substring(7, linkHref.indexOf("&"));
                addUrlContentsToFile(url,filename);
                //String linkText = result.text();
                //System.out.println("Text::" + linkText + ", URL::" + linkHref.substring(6, linkHref.indexOf("&")));
            }
            //String linkHref = results.first().attr("href");
            //url = linkHref.substring(7, linkHref.indexOf("&"));

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        
        SentenceIterator iter = new LineSentenceIterator(new File(filename));
        iter.setPreProcessor(new SentencePreProcessor() {
            @Override
            public String preProcess(String sentence) {
                return sentence.toLowerCase();
            }
        });

        TokenizerFactory t = new DefaultTokenizerFactory();
        t.setTokenPreProcessor(new CommonPreprocessor());

        Word2Vec vec = new Word2Vec.Builder()
                .minWordFrequency(5)
                .iterations(1)
                .layerSize(100)
                .seed(42)
                .windowSize(5)
                .iterate(iter)
                .tokenizerFactory(t)
                .build();

        //log.info("Fitting Word2Vec model....");
        vec.fit();

        double distance = vec.similarity("shiny", "metal");
        System.out.println(distance);

//        // PreparedStatements can use variables and are more efficient
//        preparedStatement = conn
//                .prepareStatement("insert into  feedback.comments values (default, ?, ?, ?, ? , ?, ?)");
//


        conn.close();
    }

    private static void addUrlContentsToFile(String url, String filename) {
        try
        {
            FileWriter fw = new FileWriter(filename,true); //the true will append the new data
            fw.write(urlToString(url));//appends the string to the file
            fw.close();
        }
        catch(IOException ioe)
        {
            System.err.println("IOException: " + ioe.getMessage());
        }
    }

    private static void writeResultSet(ResultSet resultSet) throws SQLException {
        // ResultSet is initially before the first data set
        while (resultSet.next()) {
            // It is possible to get the columns via name
            // also possible to get the columns via the column number
            // which starts at 1
            // e.g. resultSet.getSTring(2);
            String word = resultSet.getString("word");
            System.out.println("Word: " + word);

//            String user = resultSet.getString("myuser");
//            String website = resultSet.getString("webpage");
//            String summary = resultSet.getString("summary");
//            Date date = resultSet.getDate("datum");
//            String comment = resultSet.getString("comments");
//            System.out.println("User: " + user);
//            System.out.println("Website: " + website);
//            System.out.println("summary: " + summary);
//            System.out.println("Date: " + date);
//            System.out.println("Comment: " + comment);
        }
    }

    static String urlToString(String webPage) {
        String result = "";
        try {
            URL url = new URL(webPage);
            try (
            InputStream is = url.openStream();
            InputStreamReader isr = new InputStreamReader(is);
            ) {
                int numCharsRead;
                char[] charArray = new char[2048];
                StringBuffer sb = new StringBuffer();
                while ((numCharsRead = isr.read(charArray)) > 0) {
                    sb.append(charArray, 0, numCharsRead);
                }
                result = sb.toString();
            }
            catch (Exception e) {e.printStackTrace();}
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

}
