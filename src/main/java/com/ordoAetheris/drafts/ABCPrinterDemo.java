package com.ordoAetheris.drafts;

import com.ordoAetheris.drafts.solution.ABCPrinter;
import java.util.concurrent.CountDownLatch;

/**
 * ABC Printer — Cyclic Alternation (A → B → C)
 *
 * Условие
 * =======
 * Есть один общий объект ABCPrinter и три потока (или три роли), которые вызывают методы:
 *
 *   - printA(Runnable printA)  // печатает "A"
 *   - printB(Runnable printB)  // печатает "B"
 *   - printC(Runnable printC)  // печатает "C"
 *
 * Потоки могут стартовать в любом порядке и вызываться конкурентно, но результат должен быть строго
 * упорядочен по циклу:
 *
 *   ABCABCABC... (n раз)
 *
 * Где n задаётся в конструкторе ABCPrinter(int n).
 *
 * Требования
 * ==========
 * 1) Вывод должен иметь длину 3*n.
 * 2) В позиции 0,3,6,... всегда "A"; в позиции 1,4,7,... всегда "B"; в позиции 2,5,8,... всегда "C".
 * 3) Порядок прихода потоков не гарантирован — реализация обязана обеспечить порядок вывода.
 * 4) Методы должны корректно завершаться после печати n символов своего типа (без зависаний).
 *
 * Разрешено
 * =========
 * Использовать любые средства синхронизации Java:
 * synchronized / wait-notify, ReentrantLock/Condition, Semaphore, etc.
 *
 * Подсказка по паттерну
 * =====================
 * Это cyclic alternation: по сути, state machine с состояниями {A, B, C}.
 * Каждый метод ждёт своего "turn", печатает, переключает turn на следующего и будит следующего.
 */
public class ABCPrinterDemo {

    public static void main(String[] args) throws Exception {
        int n = 20;
        ABCPrinter printer = new ABCPrinter(n);

        StringBuffer out = new StringBuffer();
        Runnable a = () -> out.append('A');
        Runnable b = () -> out.append('B');
        Runnable c = () -> out.append('C');

        CountDownLatch start = new CountDownLatch(1);

        Thread ta = new Thread(() -> run(start, () -> printer.printA(a)), "T-A");
        Thread tb = new Thread(() -> run(start, () -> printer.printB(b)), "T-B");
        Thread tc = new Thread(() -> run(start, () -> printer.printC(c)), "T-C");

        ta.start(); tb.start(); tc.start();
        start.countDown();

        ta.join(3000);
        tb.join(3000);
        tc.join(3000);

        if (ta.isAlive() || tb.isAlive() || tc.isAlive()) {
            throw new IllegalStateException("HANG/DEADLOCK detected");
        }

        System.out.println(out);

        validate(out.toString(), n);
        System.out.println("OK");
    }

    private static void run(CountDownLatch start, ThrowingRunnable r) {
        try {
            start.await();
            r.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void validate(String s, int n) {
        if (s.length() != 3 * n) throw new AssertionError("bad length: " + s.length());
        for (int i = 0; i < n; i++) {
            int base = i * 3;
            if (s.charAt(base) != 'A') throw new AssertionError("expected A at " + base);
            if (s.charAt(base + 1) != 'B') throw new AssertionError("expected B at " + (base + 1));
            if (s.charAt(base + 2) != 'C') throw new AssertionError("expected C at " + (base + 2));
        }
    }

    @FunctionalInterface
    interface ThrowingRunnable {
        void run() throws Exception;
    }
}
