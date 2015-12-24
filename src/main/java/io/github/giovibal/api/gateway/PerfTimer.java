package io.github.giovibal.api.gateway;

/**
 * Created by Giovanni Baleani on 24/12/2015.
 */
public class PerfTimer {
    long t1,t2,t3;

    public void start() {
        t1=System.currentTimeMillis();
        t2=0;
        t3=0;
    }
    public void stop() {
        if(t1==0) {
            start();
        }
        t2=System.currentTimeMillis();
        t3=t2-t1;
//        t1=t2;
    }
    public void checkpoint(String label) {
        stop();
        printStats(label);
    }

    public void printStats(String label) {
        System.out.println("Performance: ["+ label +"] => "+ t3 + " millis.");
    }

    public void printStats(String label, PerfTimer other) {
        long delta = t3-other.t3;
        System.out.println("Performance: ["+ label +"] => "+ delta + " millis.");
    }
}
