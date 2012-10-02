package test;

import org.jooq.*;
import org.jooq.conf.MappedSchema;
import org.jooq.conf.RenderMapping;
import org.jooq.conf.Settings;
import org.jooq.impl.Factory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static test.schema.tables.TestTable12.TEST_TABLE12;

/**
 * User: Denis Bondarenko
 * Date: 01/10/2012
 * Time: 10:05
 */
public class Main {
    public static void main(String[] args) {
        AtomicInteger notCatched = new AtomicInteger(0);
        int j = 0;
        int oldNotCatched = 0;
        AtomicReference<String> s = new AtomicReference<String>();
        while (notCatched.get() < 20) {
            if (oldNotCatched < notCatched.get())
                System.out.println("test " + j + "(" + notCatched.get() + ")");
            oldNotCatched = notCatched.get();
            j++;
            if (j % 1000 == 0) {
                System.out.println(j + " tests so far");
            }
            Object lock = new Object();
            Factory jooqFactory = new Factory(SQLDialect.ORACLE,
                    new Settings()
                            .withRenderMapping(new RenderMapping()
                                    .withSchemata(new MappedSchema()
                                            .withInput("UPS")
                                            .withOutput("UPS_TEST"))));
            for (int i = 0; i < 2; i++) {
                new Thread(new RunnableTest(lock, jooqFactory, s, notCatched)).start();
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            synchronized (lock) {
                lock.notifyAll();
            }

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    private static class RunnableTest implements Runnable {
        private Object lock;
        private Factory jooqFactory;
        private AtomicReference<String> s;
        private AtomicInteger notCatched;

        private RunnableTest(Object lock, Factory jooqFactory, AtomicReference<String> s, AtomicInteger notCatched) {
            this.lock = lock;
            this.jooqFactory = jooqFactory;
            this.s = s;
            this.notCatched = notCatched;
        }

        @Override
        public void run() {
            try {
                synchronized (lock) {
                    lock.wait();
                }
                RenderContext context = jooqFactory.renderContext();
                TEST_TABLE12.toSQL(context);
                String render = context.render();
                s.set(render);
            } catch (Exception e) {
                e.printStackTrace();
                notCatched.incrementAndGet();
            }
        }
    }

}
