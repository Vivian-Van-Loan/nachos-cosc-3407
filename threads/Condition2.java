package nachos.threads;

import nachos.machine.*;

import java.util.LinkedList;
import java.util.Queue;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see	nachos.threads.Condition
 */
public class Condition2 {
    /**
     * Allocate a new condition variable.
     *
     * @param	conditionLock	the lock associated with this condition
     *				variable. The current thread must hold this
     *				lock whenever it uses <tt>sleep()</tt>,
     *				<tt>wake()</tt>, or <tt>wakeAll()</tt>.
     */
    public Condition2(Lock conditionLock) {
	this.conditionLock = conditionLock;
    }

    /**
     * Atomically release the associated lock and go to sleep on this condition
     * variable until another thread wakes it using <tt>wake()</tt>. The
     * current thread must hold the associated lock. The thread will
     * automatically reacquire the lock before <tt>sleep()</tt> returns.
     */
    public void sleep() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());

        boolean intStatus = Machine.interrupt().disable();
        waitList.add(KThread.currentThread());
	conditionLock.release(); //acts as both the lock and the mutex for this section
        KThread.currentThread().sleep();
	conditionLock.acquire();
        Machine.interrupt().restore(intStatus);
    }

    /**
     * Wake up at most one thread sleeping on this condition variable. The
     * current thread must hold the associated lock.
     */
    public void wake() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());

        boolean intStatus = Machine.interrupt().disable();
        if (!waitList.isEmpty())
            waitList.poll().ready();
        Machine.interrupt().restore(intStatus);
    }

    /**
     * Wake up all threads sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    public void wakeAll() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
        boolean intStatus = Machine.interrupt().disable();
        KThread next = waitList.poll();
        while (next != null) {
            next.ready();
            next = waitList.poll();
        }
        Machine.interrupt().restore(intStatus);
    }

    private static class SingleBufferTest implements Runnable {
        static Queue<Integer> buffer = new LinkedList<>();
        static Lock lock = new Lock();
        static Condition2 condition = new Condition2(lock);
        boolean writer;
        SingleBufferTest(boolean writer) {
            this.writer = writer;
        }

        public void run() {
            lock.acquire();
            if (writer) {
                for (int i = 0; i < 2; i++) {
                    for (int j = 0; j < 10; j++)
                        buffer.add(i * 10 + j);
                    condition.wake();
                    condition.sleep();
                }
            } else {
                for (int i = 0; i < 2; i++) {
                    if (buffer.isEmpty()) {
                        condition.wake();
                        condition.sleep();
                    }
                    Integer value = buffer.poll();
                    while (value != null) {
                        System.out.println("Buffer contained: " + value);
                        value = buffer.poll();
                    }
                }
            }
            lock.release();
        }
    }

    private static void RunSingleBufferTest() {
        KThread writer = new KThread(new SingleBufferTest(true));
        KThread consumer = new KThread(new SingleBufferTest(false));
        writer.fork();
        consumer.fork();
        consumer.join();
    }

//    private static class MultiBufferTest implements Runnable {
//        boolean writer;
//        static Queue<Integer> buffer = new LinkedList<>();
//        static Lock lock = new Lock();
//        static Condition2 condition = new Condition2(lock);
//        MultiBufferTest(boolean writer) {
//            this.writer = writer;
//        }
//
//        public void run() {
//            lock.acquire();
//            if (writer) {
//                for (int i = 0; i < 2; i++) {
//                    for (int j = 0; j < 3; j++)
//                        buffer.add(i * 3 + j);
//                    condition.wakeAll();
//                    condition.sleep();
//                }
//            } else {
//
//            }
//            lock.release();
//        }
//    }

    public static void SelfTest() {
        System.out.println("Condition2.java Tests:");

        long time1 = Machine.timer().getTime();
        RunSingleBufferTest();
        long time2 = Machine.timer().getTime();
        System.out.println("Ran in: " + (time2 - time1) + "ms\n\n");
    }

    private Lock conditionLock;
    private LinkedList<KThread> waitList = new LinkedList<>();
}
