package se.umu.c12msr.fabricbenchmark;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Created by Mattias Scherer on 3/6/17.
 */
public class Client {
    private static final Log logger = LogFactory.getLog(Client.class);


    public Client() throws Exception {
        int number = 100;
        int threads = 15;
        int waittime = 60;


        Payment[] payments = new Payment[threads];

        for (int i = 0; i < threads; i++) {
            if (i == 0) {
                payments[i] = new Payment(true);
            } else {
                payments[i] = new Payment(false);
            }
        }

        // Only one need to install the chaincode
        payments[0].install();
        payments[0].init(new String[]{"a", "10000", "b", "10000", ""+threads*number}).get(waittime, TimeUnit.SECONDS);
        logger.info("Successfully deploy chaincode");


        Thread[] workers = new Thread[threads];

        for (int i = 0; i < threads; i++) {
            int start = number * i;
            int stop = number * (i + 1);
            workers[i] = new Thread(new Task(payments[i], new String[]{"pay", "a", "b", "5"}, start, stop));
        }


        Thread.sleep(5000);
        logger.info(">>>Check the initial value: a0, b0");
        String[] values = new String[2];
        values[0] = payments[0].query(new String[]{"query", "a0"});
        values[1] = payments[0].query(new String[]{"query", "b0"});
        logger.info(Arrays.toString(values));

        long start = System.nanoTime();
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        logger.info("Starting time");
        for (int i = 0; i < threads; i++) {
            workers[i].start();
        }

        for (int i = 0; i < threads; i++) {
            workers[i].join();
        }
        double duration = (System.nanoTime() - start) / 1.0e9;


        //logger.info(String.format("Query %d times, duration=%.3fs\n", number, duration));
        logger.info(String.format("Pay %d times, duration=%.3fs\n", number*threads, duration));
        logger.info(String.format("Starting time was %s", timestamp.toString()));

        logger.info(">>>Check the value again: a0, b0");
        values[0] = payments[0].query(new String[]{"query", "a0"});
        values[1] = payments[0].query(new String[]{"query", "b0"});
        logger.info(Arrays.toString(values));
    }



    public static void main( String[] args ) {

        try {
            new Client();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    private class Task implements Runnable {

        String[] args;
        Payment payment;
        int start;
        int stop;

        Task(Payment payment, String[] args, int start, int stop) {
            this.payment = payment;
            this.args = args;
            this.start = start;
            this.stop = stop;
        }

        @Override
        public void run() {
                try {
                    String acc1 = args[1];
                    String acc2 = args[2];
                    for (int i = start; i < stop; i++) {
                        args[1] = acc1 + i;
                        args[2] = acc2 + i;
                        payment.invoke(args).thenAccept(transactionEvent -> {
                            if (!transactionEvent.isValid()) {
                                logger.warn("Invalid pay transaction");
                            }


                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
        }
    }
}
