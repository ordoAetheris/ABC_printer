
import com.ordoAetheris.drafts.solution.ABCPrinter;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class ABCPrinterTest {

    @Test
    void printsStrictABC() throws Exception {
        int n = 300;
        ABCPrinter printer = new ABCPrinter(n);

        StringBuffer out = new StringBuffer();
        Runnable a = () -> out.append('A');
        Runnable b = () -> out.append('B');
        Runnable c = () -> out.append('C');

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(3);

        Future<?> fa = pool.submit(() -> { await(start); call(() -> printer.printA(a)); });
        Future<?> fb = pool.submit(() -> { await(start); call(() -> printer.printB(b)); });
        Future<?> fc = pool.submit(() -> { await(start); call(() -> printer.printC(c)); });

        start.countDown();

        fa.get(3, TimeUnit.SECONDS);
        fb.get(3, TimeUnit.SECONDS);
        fc.get(3, TimeUnit.SECONDS);

        pool.shutdownNow();

        assertEquals(3 * n, out.length());
        for (int i = 0; i < n; i++) {
            int base = i * 3;
            assertEquals('A', out.charAt(base));
            assertEquals('B', out.charAt(base + 1));
            assertEquals('C', out.charAt(base + 2));
        }
    }

    @Test
    void printsStrictABC_withJitter() throws Exception {
        int n = 500;
        ABCPrinter printer = new ABCPrinter(n);

        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        StringBuffer out = new StringBuffer();

        Runnable a = () -> { jitter(rnd); out.append('A'); };
        Runnable b = () -> { jitter(rnd); out.append('B'); };
        Runnable c = () -> { jitter(rnd); out.append('C'); };

        runIn3Threads(printer, a, b, c);

        assertStrictABC(out.toString(), n);
    }


    private static void jitter(ThreadLocalRandom rnd) {
        // 0..2ms джиттера
        try {
            Thread.sleep(rnd.nextInt(3));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }


    @Test
    void printsStrictABC_manyRuns() throws Exception {
        int runs = 200;
        int n = 200;

        for (int r = 0; r < runs; r++) {
            ABCPrinter printer = new ABCPrinter(n);
            StringBuffer out = new StringBuffer();

            Runnable a = () -> out.append('A');
            Runnable b = () -> out.append('B');
            Runnable c = () -> out.append('C');

            runIn3Threads(printer, a, b, c);

            try {
                assertStrictABC(out.toString(), n);
            } catch (AssertionError e) {
                // добавим контекст, чтобы не гадать какой прогон упал
                throw new AssertionError("Failed on run=" + r + ", outLen=" + out.length() +
                        ", outPrefix=" + prefix(out.toString(), 60), e);
            }
        }
    }

    private static String prefix(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }



    private static void runIn3Threads(ABCPrinter printer, Runnable a, Runnable b, Runnable c) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(3);

        Future<?> fa = pool.submit(() -> { await(start); call(() -> printer.printA(a)); });
        Future<?> fb = pool.submit(() -> { await(start); call(() -> printer.printB(b)); });
        Future<?> fc = pool.submit(() -> { await(start); call(() -> printer.printC(c)); });

        start.countDown();

        fa.get(3, TimeUnit.SECONDS);
        fb.get(3, TimeUnit.SECONDS);
        fc.get(3, TimeUnit.SECONDS);

        pool.shutdownNow();
    }

    private static void assertStrictABC(String s, int n) {
        if (s.length() != 3 * n) throw new AssertionError("bad length: " + s.length());
        for (int i = 0; i < n; i++) {
            int base = i * 3;
            if (s.charAt(base) != 'A') throw new AssertionError("expected A at " + base);
            if (s.charAt(base + 1) != 'B') throw new AssertionError("expected B at " + (base + 1));
            if (s.charAt(base + 2) != 'C') throw new AssertionError("expected C at " + (base + 2));
        }
    }

    private static void await(CountDownLatch latch) {
        try { latch.await(); } catch (InterruptedException e) { throw new RuntimeException(e); }
    }

    private static void call(ThrowingRunnable r) {
        try { r.run(); } catch (Exception e) { throw new RuntimeException(e); }
    }

    @FunctionalInterface
    interface ThrowingRunnable {
        void run() throws Exception;
    }

}
