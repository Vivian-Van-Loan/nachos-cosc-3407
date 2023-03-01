/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package nachos.threads;

import java.util.LinkedList;
import nachos.threads.Alarm.*;
import nachos.machine.*;
import nachos.threads.KThread;
import nachos.threads.Lock;

public class ReactWater{

    private static LinkedList<KThread> hQueue = new LinkedList<>();
    private static LinkedList<KThread> oQueue = new LinkedList<>();
    private int hCount;
    private int oCount;
    private Lock reactLock;
    /** 
     *   Constructor of ReactWater
     **/
    public ReactWater(Lock reactLock) {
        hCount = 0;
        oCount = 0;
        this.reactLock = reactLock;
    } // end of ReactWater()
    
    /** 
     *   Constructor of ReactWater
     **/
    public ReactWater() {
        hCount = 0;
        oCount = 0;
    } // end of ReactWater()

    /** 
     *   When H element comes, if there already exist another H element 
     *   and an O element, then call the method of Makewater(). Or let 
     *   H element wait in line. 
     **/ 
    public void hReady() {
        hQueue.add(KThread.currentThread());
	++hCount;
	if(oCount >= 1 && hCount >= 2)
		Makewater();
    } // end of hReady()
 
    /** 
     *   When O element comes, if there already exist another two H
     *   elements, then call the method of Makewater(). Or let O element
     *   wait in line. 
     **/ 
    public void oReady() {
        oQueue.add(KThread.currentThread());
	++oCount;
	if(oCount >= 1 && hCount >= 2)
		Makewater();
    } // end of oReady()
    
    /** 
     *   Print out the message of "water was made!".
     **/
    public void Makewater() {
        if(reactLock.isHeldByCurrentThread()) {
		hCount -= 2;
		--oCount;
		oQueue.removeFirst();
		hQueue.removeFirst();
                hQueue.removeFirst();
                System.out.println("Water has been made");
	}

    } // end of Makewater()
    
    public static void selfTest()
	{
            final Lock lock = new Lock();
            final ReactWater testReact = new ReactWater(lock);
            System.out.println("ReactWater.java Test:");
            
            class HydroThread implements Runnable {
                public void run(){
                    lock.acquire();
                    testReact.hReady();
                    Lib.debug('t', "There are currently " + testReact.hCount + " hydrogen threads and "
                     + testReact.oCount + " oxygen threads.");
                    lock.release();		
                }
            }
            class OxyThread implements Runnable {
                public void run(){
                    lock.acquire();
                    testReact.oReady();
                    lock.release();
                    Lib.debug('t', "There are currently " + testReact.hCount + " hydrogen threads and "
                     + testReact.oCount + " oxygen threads.");
                }
            }
            KThread h1 = new KThread(new HydroThread()); 
            KThread h2 = new KThread(new HydroThread()); 
            KThread h3 = new KThread(new HydroThread());
            KThread h4 = new KThread(new HydroThread()); 
            KThread h5 = new KThread(new HydroThread()); 
            KThread h6 = new KThread(new HydroThread());
            KThread h7 = new KThread(new HydroThread());
            KThread o1 = new KThread(new OxyThread()); 
            KThread o2 = new KThread(new OxyThread()); 
            KThread o3 = new KThread(new OxyThread()); 
            KThread o4 = new KThread(new OxyThread());
            
            System.out.println("Test 1 - 1 oxygen, 0 hydrogen, shouldnt't make water:");
            o1.fork();
            o1.join();
            System.out.println("Test 2 - 1 oxygen, 1 hydrogen, shouldnt't make water:");
            h1.fork();
            h1.join();
            System.out.println("Test 3 - 1 oxygen, 2 hydrogens, should make water:");
            h2.fork();
            h2.join();
            System.out.println("Performance Test:");
            System.out.println("Test 4 - 3 oxygens, 5 hydrogens, should make water twice:");
            long time1 = Machine.timer().getTime();
            h3.fork();
            h4.fork();
            h5.fork();
            h6.fork();
            h7.fork();
            o2.fork();
            o3.fork();
            o4.fork();
            h3.join();
            h4.join();
            h5.join();
            h6.join();
            h7.join();
            o2.join();
            o3.join();
            o4.join();
            long time2 = Machine.timer().getTime();
            System.out.println("Performance of test 4: " + (time2 - time1) + "ms");
    }
    
} // end of class ReactWater
