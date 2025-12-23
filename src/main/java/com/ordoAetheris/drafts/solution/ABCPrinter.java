package com.ordoAetheris.drafts.solution;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ABCPrinter {
    private final int n;


    private enum Turn{A, B, C}
    private Turn turn = Turn.A;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition aCond = lock.newCondition();
    private final Condition bCond = lock.newCondition();
    private final Condition cCond = lock.newCondition();


    public ABCPrinter(int n) {
        this.n = n;
    }

    public void printA(Runnable printA) throws InterruptedException {
        for (int i = 0; i < n; i++) {
            lock.lock();
            try {
                while (turn != Turn.A) aCond.await();
                printA.run();
                turn = Turn.B;
                bCond.signal();
            } finally {
                lock.unlock();
            }
        }
    }

    public void printB(Runnable printB) throws InterruptedException {
        for (int i = 0; i < n; i++) {
            lock.lock();
            try {
                while (turn != Turn.B) bCond.await();
                printB.run();
                turn = Turn.C;
                cCond.signal();
            } finally {
                lock.unlock();
            }
        }
    }

    public void printC(Runnable printC) throws InterruptedException {
        for (int i = 0; i < n; i++) {
            lock.lock();
            try {
                while (turn != Turn.C) cCond.await();
                printC.run();
                turn = Turn.A;
                aCond.signal();
            } finally {
                lock.unlock();
            }
        }
    }

}
