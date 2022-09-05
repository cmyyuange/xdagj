package io.xdag.utils;

import java.util.Random;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Test {


    private static final Logger logger = LoggerFactory.getLogger(Test.class);
    private static final ScheduledExecutorService scheduledExecutor = new ScheduledThreadPoolExecutor(3, new BasicThreadFactory.Builder()
            .namingPattern("MinerManager-Scheduled-Thread-%d")
            .daemon(true)
            .build());

    private static final ExecutorService workerExecutor = Executors.newCachedThreadPool(new BasicThreadFactory.Builder()
            .namingPattern("MinerManager-Worker-Thread-%d")
            .daemon(true)
            .build());

    private static Object object1 = new Object();
    private static Object object2 = new Object();

    public static void main(String[] args) throws InterruptedException {
        Task task = new Task();
        Task task1 = new Task();
        Task1 task2 = new Task1();
//        scheduledExecutor.scheduleAtFixedRate(task, 1, 5, TimeUnit.SECONDS);
        scheduledExecutor.scheduleAtFixedRate(task1, 1, 10, TimeUnit.SECONDS);
        scheduledExecutor.scheduleAtFixedRate(task2, 1, 10, TimeUnit.SECONDS);



        System.out.println("hello1");
        Thread.sleep(1000000000000000L);


    }

    public static class TestTask implements Runnable {

        @Override
        public void run() {
            if (RandomUtils.nextInt(0,10) < 3) { // 1/3的channel会断掉
                synchronized (object2) {
                    System.out.println("TestTask");
                    logger.info("get obj2 TestTask...");
                }
            }
        }
    }

    public static class Task1 extends TimerTask {

        @Override
        public void run() {
            doSomeThing();
        }

        public void doSomeThing() {
            synchronized (object2) {
                logger.info(" get obj2 ...");
            }
        }
    }


    public static class Task extends TimerTask {

        @Override
        public void run() {
            doSomeThing();
        }

        public void doSomeThing() {

            synchronized (object1) {
                logger.info("Task do something");
                Long current = System.currentTimeMillis();
                for (int i = 0;i<60; i++) {
                    logger.info(" 第 {} 个...",i);
                    workerExecutor.submit(new TestTask());
                }
                Long spend = System.currentTimeMillis()-current;
                System.out.println(spend);
            }
        }
    }

}
