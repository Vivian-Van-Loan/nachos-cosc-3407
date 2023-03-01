package nachos.threads;

import nachos.machine.*;
import java.util.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    
    private static PriorityQueue<WaitThread> threadQueue = new PriorityQueue<>();
    
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
	Machine.timer().setInterruptHandler(new Runnable() {
		public void run() { timerInterrupt(); }
	    });
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {
        boolean state = Machine.interrupt().disable(); 
        
        while(!threadQueue.isEmpty()) {
            WaitThread thread = threadQueue.peek();
            
            if (thread.getWakeTime() <= Machine.timer().getTime()){
                    threadQueue.poll();
                    thread.getThread().ready();
            }
            else
                break;
	}
        Machine.interrupt().restore(state);
	KThread.currentThread().yield();
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
	//get the wake time and the current thread
	boolean state = Machine.interrupt().disable();  
   	KThread thread = KThread.currentThread();
        long wakeTime = Machine.timer().getTime() + x;
   	 
   	//add the thread to the queue
   	WaitThread waitThread = new WaitThread(thread, wakeTime);
   	threadQueue.add(waitThread);
   	 
   	//put it to sleep to be woken up by the timer interrupt handler
   	thread.sleep();
	Machine.interrupt().restore(state);

    }
    
    public static class WaitThread implements Comparable {
	long wakeTime;
	KThread thread;

	public WaitThread(KThread thread, long wakeTime) {
            this.thread = thread;
            this.wakeTime = wakeTime;
        }
        
        public long getWakeTime(){
            return wakeTime;
        }
        
        public KThread getThread() {
            return thread;
        }
        
        @Override
        public int compareTo(Object thread) {
            return (int)(this.wakeTime - ((WaitThread)thread).wakeTime);
        }
    }
    
    public static void selfTest() {
        System.out.println("Alarm.java Tests:");
        
        class WaitTest implements Runnable {
            int waitTime;
            int tid;
            Alarm alarm;
            
            public WaitTest(int waitTime, int tid) {
                this.waitTime = waitTime;
                this.tid = tid;
                alarm = new Alarm();
            }
            
            public void run() {
                System.out.println("Running test #" + tid);
                System.out.println("Testing wait time of " + waitTime + "ms, "
                        + "going to sleep at: " + Machine.timer().getTime());
               alarm.waitUntil(waitTime);
               System.out.println("Waking up at " + Machine.timer().getTime());
            }
        }
        
        KThread t1 = new KThread(new WaitTest(5,1));
        t1.fork();
        t1.join();
        KThread t2 = new KThread(new WaitTest(600,2));
        t2.fork();
        t2.join();
        KThread t3 = new KThread(new WaitTest(1200,3));
        t3.fork();
        t3.join();
        System.out.println("Performance Test: (wait time set at 7000ms)");
        KThread t4 = new KThread(new WaitTest(7000,4));
        t4.fork();
        t4.join();
    }
}
