package com.example.sparktest;

import com.clearspring.analytics.util.Lists;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;


public class Main {
    public static Random rand = new Random(123);//if you add a seed 123, 15 iterations, damping factor= 0.85, 5 pages and 16 connections the result will be A=0.6801 B=0.3346 C=0.6237 D=0.4391 E=0.4344
    public static int pagecount = 0;
    public static Page[] allpages; //array of all the pages
    public static Graph connectedPairs = new Graph();
    public static Scanner sc = new Scanner(System.in);
    public static double[][] Ranks; //rows are iterations and cols are pages
    public static int iterations = 0;
    public static double d = 1.1;





    public static void main(String[] args) throws FileNotFoundException {
        //determine which mode the program will run on
        int parameter = getMode();

        initialize();

        connectedPairs.print(); //print connections with adjacency list

        while (d < 0.0 || d > 0.99) {
            System.out.println("Enter the damping factor (usually 0.85, max is 0.99): ");
            d = sc.nextDouble();
        }

        long startTime = System.currentTimeMillis();

        if (parameter == 1) {
            PageRank(iterations, d);
        } else if (parameter == 2) {
            parallelPageRank(iterations, d);
        }
        else if (parameter == 3){
            distributedPageRank(iterations,d);
        }


        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;


        printRanks();

        System.out.println("Total runtime: " + totalTime + " milliseconds");

        //System.out.println("Estimated runtime for 1 iteration is "+ totalTime/iterations);
        //this doesnt work because the value is too small to be portrayed so it just says 0 every time

        writeResults();

        writeGraph();

        System.out.println("Done");
    }





















    public static int getMode() {
        int parameter = 0;
        while (parameter <= 0 || parameter > 3) {
            System.out.println("Which mode would you like the program to run on? (Enter from 1 to 3)");
            System.out.println("1. Sequential");
            System.out.println("2. Parallel");
            System.out.println("3. Distributed");
            parameter = sc.nextInt();
        }
        return parameter;
    }

    //method that to do everything needed when making the pages and connections
    public static void initialize() {
        //pages
        int V = 0;
        while (V <= 0){
            System.out.println("Enter the number of pages that you want: ");
            V = sc.nextInt();
        }
        allpages = new Page[V];
        createPages(V);
        setInitPageRank(allpages);
        //connections
        int E = 0;
        while (E <= 0 || E > V * (V - 1)) {
            System.out.println("Enter the number of connections between pages: (max is numofpages * (numofpages-1) : ");
            E = sc.nextInt();
        }
        connections(E);
        //iterations
        while (iterations <=0){
            System.out.println("Enter the number of iterations you want the rank function to execute for: ");
            iterations = sc.nextInt();
        }
        Ranks = new double[iterations][V]; //initialize size of array of ranks (rows are iterations and cols are pages)
        //input the initial page ranks of all the pages into the Ranks array's first row
        for (int i = 0; i < allpages.length; i++) {
            Ranks[0][i] = allpages[i].rank;
        }
    }

    //method to calculate the initial page rank of the pages before the connections are added (0th iteration page ranks are 1/n)
    public static void setInitPageRank(Page[] pages) {
        for (int i = 0; i < pages.length; i++) {
            pages[i].rank = 1.0 / pagecount;
        }
    }

    //method to create the pages and add them in the global var array
    public static void createPages(int V) {
        for (int i = 0; i < V; i++) {
            char pageName = (char) ('A' + i);
            Page page = new Page(pageName);
            allpages[i] = page;
            connectedPairs.addPage(page);
        }
    }

    //method to create connections between pages (gets random page to map to other random page that's not the same one)
    public static void connections(int E) {
        int numOfConnections = E;
        while (connectedPairs.getTotalEdges() < numOfConnections) {
            int randomNum1 = rand.nextInt(allpages.length);
            int randomNum2 = rand.nextInt(allpages.length);

            if (randomNum1 != randomNum2 && !connectedPairs.checkEdge(randomNum1, randomNum2)) {
                connectedPairs.addEdge(randomNum1, randomNum2);
                Page pageFrom = allpages[randomNum1];
                Page pageTo = allpages[randomNum2];
                pageFrom.connectsToPages.add(pageTo);
                pageTo.isConnectedByPages.add(pageFrom);
                pageTo.numConnectsToIt++;
            }
        }
    }
    public static void printRanks() {
        System.out.println("Page Ranks:");
        for (int i = 0; i < Ranks.length; i++) {
            System.out.print("Iteration " + i + ": ");
            for (int j = 0; j < Ranks[i].length; j++) {
                System.out.printf("%s=%.4f ", allpages[j].pageName, Ranks[i][j]);
            }
            System.out.println();
        }
    }

    //method to write the Graph to a csv file (edge list format)
    public static void writeGraph() throws FileNotFoundException {
        File csvFile = new File("Graph.csv");
        PrintWriter out = new PrintWriter(csvFile);
        for (LinkedList<Page> currentList : connectedPairs.alist) {
            int size = currentList.size();
            int currentIndex = 0;
            for (Page page : currentList) {
                out.printf("%s", page.pageName);
                //this is so it doesnt look ugly with the comma at the end :)
                if (++currentIndex < size) {
                    out.printf(", ");
                }
            }
            out.printf("\n");
        }
        out.close();
    }

    //method to write the results to a csv file (vertex(name), rank)
    public static void writeResults() throws FileNotFoundException {
        File csvFile = new File("Results.csv");
        PrintWriter out = new PrintWriter(csvFile);
        out.printf("%s,  %s\n", "vertex", "rank");
        for (int i = 0; i < allpages.length; i++) {
            out.printf("%s,       %f\n", allpages[i].pageName, allpages[i].rank);
        }
        out.close();
    }
    //pagerank function
    public static void PageRank(int iteration, double d) {
        for (int i = 1; i < iteration; i++) {
            for (int j = 0; j < pagecount; j++) {
                //below is the calculation of pagerank
                double sum = 0.0;
                for (Page p : allpages[j].isConnectedByPages) {
                        sum = sum + (p.rank / p.connectsToPages.size());
                }
                Ranks[i][j] = (1.0 - d) + (d * sum);
                allpages[j].rank = Ranks[i][j]; //update the rank of the page
            }
        }
    }
    //parallel pagerank function
    public static void parallelPageRank(int iteration, double d) {
        int numOfProcessors = Runtime.getRuntime().availableProcessors();
        ForkJoinPool pool = new ForkJoinPool(numOfProcessors);
        for (int i = 1; i < iteration; i++) {
            pool.invoke(new PageRankTask(0, pagecount, d));
            for (int j = 0; j < pagecount; j++) {
                Ranks[i][j] = allpages[j].rank;
            }
        }
        pool.shutdown();
    }
    public static void distributedPageRank(int iterations, double d) {
        SparkConf conf = new SparkConf().setAppName("Spark PageRank").setMaster("local[*]"); //init distributive environment, local[*] means it uses all of available cores
        JavaSparkContext sc = new JavaSparkContext(conf);

        int numPages = pagecount;

        List<Tuple2<Integer, Integer>> edges = generateEdgeList();

        //create RDD of the edges
        JavaPairRDD<Integer, Integer> linksRDD = sc.parallelizePairs(edges);

        //group by key to get the adjacency list
        JavaPairRDD<Integer, Iterable<Integer>> links = linksRDD.distinct().groupByKey().cache();

        //initialize ranks RDD with each page having a rank of 1.0 / numPages
        JavaPairRDD<Integer, Double> ranks = links.mapValues(value -> 1.0 / numPages);

        for (int i = 0; i < iterations; i++) {
            JavaPairRDD<Integer, Double> contributions = links.join(ranks)
                    .flatMapToPair(s -> {
                        List<Tuple2<Integer, Double>> results = new ArrayList<>();
                        Iterable<Integer> urls = s._2._1;
                        Double rank = s._2._2;
                        int urlCount = Lists.newArrayList(urls).size();
                        for (Integer n : urls) {
                            results.add(new Tuple2<>(n, rank / urlCount));
                        }
                        return results.iterator();
                    });

            ranks = contributions.reduceByKey((a, b) -> a + b)
                    .mapValues(sum -> (1 - d) + d * sum);

            //update ranks array after each iteration
            List<Tuple2<Integer, Double>> output = ranks.collect();
            for (Tuple2<Integer, Double> tuple : output) {
                int pageIndex = tuple._1();
                double rank = tuple._2();
                Ranks[i][pageIndex] = rank;
            }
        }

        //update the allpages array with the final ranks
        List<Tuple2<Integer, Double>> finalOutput = ranks.collect();
        for (Tuple2<Integer, Double> tuple : finalOutput) {
            int pageIndex = tuple._1();
            double rank = tuple._2();
            allpages[pageIndex].rank = rank;
        }

        sc.stop();
    }


    private static List<Tuple2<Integer, Integer>> generateEdgeList() {
        List<Tuple2<Integer, Integer>> edges = new ArrayList<>();
        for (int i = 0; i < allpages.length; i++) {
            for (Page p : allpages[i].connectsToPages) {
                int fromIndex = i;
                int toIndex = Arrays.asList(allpages).indexOf(p);
                edges.add(new Tuple2<>(fromIndex, toIndex));
            }
        }
        return edges;
    }

}
