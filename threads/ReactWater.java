/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package threads;

import java.util.PriorityQueue;
import nachos.threads.Alarm.*;
import nachos.machine.*;
import nachos.threads.Lock;

public class ReactWater{

    private static PriorityQueue<WaitThread> hQueue = new PriorityQueue<>();
    private static PriorityQueue<WaitThread> oQueue = new PriorityQueue<>();
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
        WaitThread wthread = new WaitThread(KThread.currentThread(), 10);
        hQueue.add(wthread);
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
        WaitThread wthread = new WaitThread(KThread.currentThread(), 10);
        oQueue.add(wthread);
	++oCount;
	if(oCount >= 1 && hCount >= 2)
		Makewater();
    } // end of oReady()
    
    /** 
     *   Print out the message of "water was made!".
     **/
    public void Makewater() {
        if(reactLock.isHeldByCurrentThread()) {
            do {
		hCount -= 2;
		--oCount;
		oQueue.remove();
		hQueue.remove();
                hQueue.remove();
                System.out.println("Water has been made");
            }while (oCount >= 1 && hCount >=2);
	}

    } // end of Makewater()
    
    
    
} // end of class ReactWater
