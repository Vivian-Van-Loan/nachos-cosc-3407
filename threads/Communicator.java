package nachos.threads;

import nachos.machine.*;

import java.util.ArrayList;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    /**
     * Allocate a new communicator.
     */
    public Communicator() {
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {
        lock.acquire();
        while (spoken) {
            listener.wakeAll();
            speaker.sleep();
        }
        this.word = word;
        spoken = true;
        listener.wake();
        lock.release();
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    public int listen() {
        lock.acquire();
        while (!spoken) {
            listener.sleep();
        }
        int listenedWord = this.word;
        spoken = false;
        speaker.wake();
        lock.release();
	return listenedWord;
    }

    private static class SingleTest implements Runnable {
        boolean speaker;
        static Communicator communicator = new Communicator();
        SingleTest(boolean speaker) {
            this.speaker = speaker;
        }

        public void run() {
            if (speaker)
                communicator.speak((int) (1 + Math.random() * ((100 - 1) + 1)));
            else
                System.out.println("Speaker says: " + communicator.listen());
        }
    }

    private static void RunSingleTest() {
        System.out.println("RunSingleTest:");
        KThread speaker = new KThread(new SingleTest(true));
        KThread listener = new KThread(new SingleTest(false));
        speaker.fork();
        listener.fork();
        listener.join();
    }

    private static class MultiDisjointTest implements Runnable {
        int communicatorIdx;
        boolean speaker;
        static Communicator[] communicators = new Communicator[10];
        MultiDisjointTest(boolean speaker, int idx) {
            this.speaker = speaker;
            communicatorIdx = idx;
            if (speaker) {
                communicators[communicatorIdx] = new Communicator();
            }
        }

        public void run() {
            if (speaker)
                communicators[communicatorIdx].speak((int) (1 + Math.random() * ((100 - 1) + 1)));
            else
                System.out.println("Speaker says: " + communicators[communicatorIdx].listen());
        }
    }

    private static class Pair<T, K> {
        public final T first;
        public final K second;

        private Pair(T first, K second) {
            this.first = first;
            this.second = second;
        }
    }

    private static void RunMultiDisjointTest() {
        System.out.println("RunMultiDisjointTest:");
        ArrayList<Pair<KThread, KThread>> pairArray = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            KThread first = new KThread(new MultiDisjointTest(true, i));
            KThread second = new KThread(new MultiDisjointTest(false, i));
            pairArray.add(new Pair<>(first, second));
        }
        for (int i = 0; i < 10; i++)
            pairArray.get(i).first.fork();
        for (int i = 0; i < 10; i++)
            pairArray.get(i).second.fork();
        for (int i = 0; i < 10; i++) {
            pairArray.get(i).first.join();
            pairArray.get(i).second.join();
        }
    }

    private static class MultiTest implements Runnable {
        boolean speaker;
        static Communicator communicator = new Communicator();
        MultiTest(boolean speaker) {
            this.speaker = speaker;
        }

        public void run() {
            if (speaker)
                communicator.speak((int) (1 + Math.random() * ((100 - 1) + 1)));
            else
                System.out.println("Speaker says: " + communicator.listen());
        }
    }

    private static void RunMultiTest() {
        System.out.println("RunMultiTest:");

        ArrayList<Pair<KThread, KThread>> pairArray = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            KThread first = new KThread(new MultiTest(true));
            KThread second = new KThread(new MultiTest(false));
            pairArray.add(new Pair<>(first, second));
        }
        for (int i = 0; i < 5; i++)
            pairArray.get(i).first.fork();
        for (int i = 0; i < 5; i++)
            pairArray.get(i).second.fork();
        for (int i = 0; i < 5; i++) {
            pairArray.get(i).first.join();
            pairArray.get(i).second.join();
        }
    }

    public static void SelfTest() {
        System.out.println("\nCommunicator.java tests:");

        long time1 = Machine.timer().getTime();
        RunSingleTest();
        long time2 = Machine.timer().getTime();
        System.out.println("Ran in: " + (time2 - time1) + "ms");

        time1 = Machine.timer().getTime();
        RunMultiDisjointTest();
        time2 = Machine.timer().getTime();
        System.out.println("Ran in: " + (time2 - time1) + "ms");

        time1 = Machine.timer().getTime();
        RunMultiTest();
        time2 = Machine.timer().getTime();
        System.out.println("Ran in: " + (time2 - time1) + "ms\n\n");

    }
    private Lock lock = new Lock();
    private Condition2 listener = new Condition2(lock);
    private Condition2 speaker = new Condition2(lock);
    private int word;
    boolean spoken;
}
