package com.example.sparktest;
import java.util.concurrent.RecursiveTask;

public class PageRankTask extends RecursiveTask<Void> {
    private static final int THRESHOLD = 50; //i found that it worked fastest with threshold set to 50
    private int start, end;
    private double d;

    public PageRankTask(int start, int end, double d) {
        this.start = start;
        this.end = end;
        this.d = d;
    }

    @Override
    protected Void compute() {
        //if the elements exceed the threshold ..
        if (end - start <= THRESHOLD) {
            for (int j = start; j < end; j++) {
                double sum = 0.0;
                for (Page p : Main.allpages[j].isConnectedByPages) {
                    sum = sum + (p.rank / p.connectsToPages.size());
                }
                Main.allpages[j].rank = (1.0 - d)/ Main.pagecount + (d * sum);
            }
            //..divide into two parts (tasks) left and right and compute them at the same time
        } else {
            int mid = (start + end) / 2;
            PageRankTask left = new PageRankTask(start, mid, d);
            PageRankTask right = new PageRankTask(mid, end, d);
            invokeAll(left, right); //fork left and right
        }
        return null;
    }
}
