package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;

import java.io.EOFException;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see nachos.vm.VMProcess
 * @see nachos.network.NetProcess
 */
public class UserProcess {
    /**
     * Allocate a new process.
     */
    public UserProcess() {
        int numPhysPages = Machine.processor().getNumPhysPages();
        pageTable = new TranslationEntry[numPhysPages];
        openFiles = new FileTriple[16];
        for (int i = 0; i < numPhysPages; i++)
            pageTable[i] = new TranslationEntry(i, i, true, false, false, false);
        insertFileTable(UserKernel.console.openForReading()); //STDIN, fd0
        insertFileTable(UserKernel.console.openForWriting()); //STDOUT,fd1
        boolean intStatus = Machine.interrupt().disable();
        pid = pidCounter++;
        numAlive++;
        Machine.interrupt().restore(intStatus);
    }

    /**
     * Allocate and return a new process of the correct class. The class name
     * is specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
     * @return a new process of the correct class.
     */
    public static UserProcess newUserProcess() {
        return (UserProcess) Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param name the name of the file containing the executable.
     * @param args the arguments to pass to the executable.
     * @return <tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args) {
        if (!load(name, args))
            return false;

        new UThread(this).setName(name).fork();

        return true;
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
        Machine.processor().setPageTable(pageTable);
    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read
     * at most <tt>maxLength + 1</tt> bytes from the specified address, search
     * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param vaddr     the starting virtual address of the null-terminated
     *                  string.
     * @param maxLength the maximum number of characters in the string,
     *                  not including the null terminator.
     * @return the string read, or <tt>null</tt> if no null terminator was
     * found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength) {
        Lib.assertTrue(maxLength >= 0);

        byte[] bytes = new byte[maxLength + 1];

        int bytesRead = readVirtualMemory(vaddr, bytes);

        for (int length = 0; length < bytesRead; length++) {
            if (bytes[length] == 0)
                return new String(bytes, 0, length);
        }

        return null;
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param vaddr the first byte of virtual memory to read.
     * @param data  the array where the data will be stored.
     * @return the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
        return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param vaddr  the first byte of virtual memory to read.
     * @param data   the array where the data will be stored.
     * @param offset the first byte to write in the array.
     * @param length the number of bytes to transfer from virtual memory to
     *               the array.
     * @return the number of bytes successfully transferred. -1 in event of an error.
     */
    public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
        if (!(offset >= 0 && length >= 0 && offset + length <= data.length)) {
//            System.out.println((offset >= 0 && length >= 0) + " : " + (offset + length <= data.length));
//            System.out.println("First thing failed: " + offset + ", " + length + ", " + (offset + length) + ", " + data.length);
            return -1;
        }

        byte[] memory = Machine.processor().getMemory();

        // for now, just assume that virtual addresses equal physical addresses
        if (vaddr < 0 || vaddr >= memory.length)
            return 0;

        int amount = Math.min(length, memory.length - vaddr);
        System.arraycopy(memory, vaddr, data, offset, amount);

        return amount;
    }

    /**
     * Transfer all data from the specified array to this process's virtual
     * memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param vaddr the first byte of virtual memory to write.
     * @param data  the array containing the data to transfer.
     * @return the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
        return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param vaddr  the first byte of virtual memory to write.
     * @param data   the array containing the data to transfer.
     * @param offset the first byte to transfer from the array.
     * @param length the number of bytes to transfer from the array to
     *               virtual memory.
     * @return the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data, int offset,
                                  int length) {
        Lib.assertTrue(offset >= 0 && length >= 0 && offset + length <= data.length);

        byte[] memory = Machine.processor().getMemory();

        // for now, just assume that virtual addresses equal physical addresses
        if (vaddr < 0 || vaddr >= memory.length)
            return 0;

        int amount = Math.min(length, memory.length - vaddr);
        System.arraycopy(data, offset, memory, vaddr, amount);

        return amount;
    }

    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param name the name of the file containing the executable.
     * @param args the arguments to pass to the executable.
     * @return <tt>true</tt> if the executable was successfully loaded.
     */
    private boolean load(String name, String[] args) {
        Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");

        OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
        if (executable == null) {
            Lib.debug(dbgProcess, "\topen failed");
            return false;
        }

        try {
            coff = new Coff(executable);
        } catch (EOFException e) {
            executable.close();
            Lib.debug(dbgProcess, "\tcoff load failed");
            return false;
        }

        // make sure the sections are contiguous and start at page 0
        numPages = 0;
        for (int s = 0; s < coff.getNumSections(); s++) {
            CoffSection section = coff.getSection(s);
            if (section.getFirstVPN() != numPages) {
                coff.close();
                Lib.debug(dbgProcess, "\tfragmented executable");
                return false;
            }
            numPages += section.getLength();
        }

        // make sure the argv array will fit in one page
        byte[][] argv = new byte[args.length][];
        int argsSize = 0;
        for (int i = 0; i < args.length; i++) {
            argv[i] = args[i].getBytes();
            // 4 bytes for argv[] pointer; then string plus one for null byte
            argsSize += 4 + argv[i].length + 1;
        }
        if (argsSize > pageSize) {
            coff.close();
            Lib.debug(dbgProcess, "\targuments too long");
            return false;
        }

        // program counter initially points at the program entry point
        initialPC = coff.getEntryPoint();

        // next comes the stack; stack pointer initially points to top of it
        numPages += stackPages;
        initialSP = numPages * pageSize;

        // and finally reserve 1 page for arguments
        numPages++;

        if (!loadSections())
            return false;

        // store arguments in last page
        int entryOffset = (numPages - 1) * pageSize;
        int stringOffset = entryOffset + args.length * 4;

        this.argc = args.length;
        this.argv = entryOffset;

        for (int i = 0; i < argv.length; i++) {
            byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
            Lib.assertTrue(writeVirtualMemory(entryOffset, stringOffsetBytes) == 4);
            entryOffset += 4;
            Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
                argv[i].length);
            stringOffset += argv[i].length;
            Lib.assertTrue(writeVirtualMemory(stringOffset, new byte[]{0}) == 1);
            stringOffset += 1;
        }

        return true;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into
     * memory. If this returns successfully, the process will definitely be
     * run (this is the last step in process initialization that can fail).
     *
     * @return <tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections() {
        if (numPages > Machine.processor().getNumPhysPages()) {
            coff.close();
            Lib.debug(dbgProcess, "\tinsufficient physical memory");
            return false;
        }

        // load sections
        for (int s = 0; s < coff.getNumSections(); s++) {
            CoffSection section = coff.getSection(s);

            Lib.debug(dbgProcess, "\tinitializing " + section.getName()
                + " section (" + section.getLength() + " pages)");

            for (int i = 0; i < section.getLength(); i++) {
                int vpn = section.getFirstVPN() + i;

                // for now, just assume virtual addresses=physical addresses
                section.loadPage(i, vpn);
            }
        }

        return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
    }

    /**
     * Initialize the processor's registers in preparation for running the
     * program loaded into this process. Set the PC register to point at the
     * start function, set the stack pointer register to point at the top of
     * the stack, set the A0 and A1 registers to argc and argv, respectively,
     * and initialize all other registers to 0.
     */
    public void initRegisters() {
        Processor processor = Machine.processor();

        // by default, everything's 0
        for (int i = 0; i < processor.numUserRegisters; i++)
            processor.writeRegister(i, 0);

        // initialize PC and SP according
        processor.writeRegister(Processor.regPC, initialPC);
        processor.writeRegister(Processor.regSP, initialSP);

        // initialize the first two argument registers to argc and argv
        processor.writeRegister(Processor.regA0, argc);
        processor.writeRegister(Processor.regA1, argv);
    }

    /**
     * Handle the halt() system call.
     */
    private int handleHalt() {
        if (this.pid != 0)
            return -1;

        Machine.halt();

        Lib.assertNotReached("Machine.halt() did not halt machine!");
        return 0;
    }

    int handleOpen(int name) {
        //string length cannot be longer than 256
        String Name = readVirtualMemoryString(name, 256);

        //if the file doesnt exist, return an error
        if (Name == null)
            return -1;

        OpenFile openFile = ThreadedKernel.fileSystem.open(Name, false);

        return insertFileTable(openFile);

    }

    int handleCreat(int name) {
        //if the file already exists, just open it
        int flag = handleOpen(name);
        if (flag != -1)
            return flag;

        //if not, need to create a new file
        String Name = readVirtualMemoryString(name, 256);
        OpenFile openFile = ThreadedKernel.fileSystem.open(Name, true);
        return insertFileTable(openFile);
    }

    int handleRead(int desc, int buffer, int count) {
        //return -1 if any invalid values are inputted
        if (desc >= openFiles.length || desc < 0 || buffer < 0)
            return -1;
        OpenFile openFile = openFiles[desc].file;
        if (openFile == null)
            return -1;

        openFile.seek(openFiles[desc].readOffset);

        byte[] Buffer = new byte[pageSize];
        int numReadBytes = 0;
        int startingPos = buffer;
        int readCount = count;
        int fileLength = openFile.length();
        
        System.out.println("fileLength: " + openFile.length());

        while (readCount != 0 && fileLength != 0) {
//            System.out.println(readCount);
            int numToRead = Math.min(readCount, Buffer.length);
//            System.out.println(numToRead);
            int read = openFile.read(Buffer, 0, numToRead);
//            System.out.println(read);
            int write = writeVirtualMemory(startingPos, Buffer, 0, read);

            if (read < 0 || read != write)
                return -1;

            numReadBytes += read;
            startingPos += read;
            openFiles[desc].readOffset += read;
            readCount -= read;
            fileLength -= read;
            if (read == 0)
                break;
        }

        return count - readCount;
    }

    int handleWrite(int desc, int buffer, int count) {
//        System.out.println("handleWrite: " + desc + ", " + buffer + ", " + count);
        //return -1 if any invalid values are inputted
        if (desc >= openFiles.length || desc < 0 || buffer < 0)
            return -1;
        OpenFile openFile = openFiles[desc].file;
        if (openFile == null) {
//            System.out.println("file is null!");
            return -1;
        }

        openFile.seek(openFiles[desc].writeOffset);

        byte[] Buffer = new byte[pageSize];
        int numWriteBytes = 0;
        int startingPos = buffer;
        int writeCount = count;

        while (writeCount != 0) {
//            System.out.println(Math.min(writeCount, Buffer.length));
            int numToWrite = Math.min(writeCount, Buffer.length);
            int read = readVirtualMemory(startingPos, Buffer, 0, numToWrite);
            if (read == -1) {
                System.out.println("Failed to read buffer");
                return -1;
            }
            int write = openFile.write(Buffer, 0, numToWrite);
//            System.out.println(read + " : " + write);

            if (write < 0 || read != write) {
                System.out.println("Failed to write in loop");
                return -1;
            }

            numWriteBytes += write;
            startingPos += write;
            openFiles[desc].writeOffset += write;
            writeCount -= write;
        }

//        System.out.println("Wrote without issue");
        
        return count;
    }

    int insertFileTable(OpenFile openFile) {
        if (openFile != null) {

            for (int i = 0; i < openFiles.length; ++i) {
                if (openFiles[i] == null) {
                    openFiles[i] = new FileTriple(openFile, 0, 0);
                    if (isOpen(openFile.getName()))
                       incrementInstances(openFile);
                    else
                        allOpenFiles.add(new GlobalFilePair(openFile.getName(), 1));
                    return i;
                }
            }
        }

        return -1;
    }

    int handleClose(int desc) {
        if (desc >= openFiles.length || desc < 0 || openFiles[desc] == null)
            return -1;
        decrementInstances(openFiles[desc].file, desc);
        openFiles[desc].file.close();
        openFiles[desc] = null;
        return 0;
    }

    int handleUnlink(int name) {
        //get the name of the file and ensure its length is 256 or less
        String Name = readVirtualMemoryString(name, 256);
        //if the file doesnt exist or its currently open, return an error
        if (Name == null || isOpen(Name))
            return -1;

        boolean flag = ThreadedKernel.fileSystem.remove(Name);
        if (flag)
            return 0;
        else
            return -1;
    }

    private int handleExit(int exitValue) {
        unloadSections();
        for (int i = 0; i < openFiles.length; i++)
            handleClose(i); //close all open files
        if (parentProc != null) {
            parentProc.infoSem.P();
            Tuple4<UserProcess, Semaphore, Integer, Integer> info = parentProc.childInfo.get(this.pid);
            info.third = exitValue;
            info.fourth = 1; //exited normally
            info.second.V(); //wake up any procs joined to this one
            parentProc.infoSem.V();
        }
        boolean intStatus = Machine.interrupt().disable();
        int newNumAlive = --numAlive;
        Machine.interrupt().restore(intStatus);
        if (newNumAlive == 0) {
            Machine.halt(); //last process kills system
        } else {
            UThread.finish();
            //This should loop through all threads for the proc and finish them
            //but atm we can’t actually spawn new threads,
            //so there’s only 1 per proc
        }
        return 0; //Unreachable
    }

    private int handleExec(int namePtr, int argc, int argvPtr) {
        String name = readVirtualMemoryString(namePtr, 256);
        if (argc < 0 || name == null || !name.endsWith(".coff"))//check errors
            return -1;
        String argv[] = new String[argc];
        for (int i = 0; i < argc; i++) {
            byte ptr[] = new byte[4];
            int numRead = readVirtualMemory(argvPtr + i * 4, ptr);
            if (numRead != 4) {
                return -1;
            }
            argv[i] = readVirtualMemoryString(Lib.bytesToInt(ptr, 0), 256);
            if (argv[i] == null)
                return -1;
        }
        UserProcess child = new UserProcess();
        child.parentProc = this;
        if (child.execute(name, argv)) {
            infoSem.P();
            childInfo.put(child.pid, new Tuple4<>(child, new Semaphore(0), -1, 0));
            infoSem.V();
            return child.pid;
        }
        return -1;
    }

    private int handleJoin(int targetPID, int statusPtr) {
        infoSem.P();
        Tuple4<UserProcess, Semaphore, Integer, Integer> info = childInfo.get(targetPID); //Java’s objects by reference
        //means this always has the correct data
        infoSem.V();
        if (info == null)
            return -1;
        info.second.P(); //2nd position, semaphore
        info.second.V(); //Info is only ever written to once so we
        //don’t actually need to keep the lock as all reads are safe now
        byte[] exitValue = Lib.bytesFromInt(info.third); //3rd pos, exit value
        if (writeVirtualMemory(statusPtr, exitValue) < 4)
            return -1;
        return info.fourth; //If the exit was normal or not
    }

    private static final int
        syscallHalt = 0,
        syscallExit = 1,
        syscallExec = 2,
        syscallJoin = 3,
        syscallCreat = 4,
        syscallOpen = 5,
        syscallRead = 6,
        syscallWrite = 7,
        syscallClose = 8,
        syscallUnlink = 9;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
     * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
     * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
     * 								</tt></td></tr>
     * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
     * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
     * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
     * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
     * 								</tt></td></tr>
     * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
     * 								</tt></td></tr>
     * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
     * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
     * </table>
     *
     * @param syscall the syscall number.
     * @param a0      the first syscall argument.
     * @param a1      the second syscall argument.
     * @param a2      the third syscall argument.
     * @param a3      the fourth syscall argument.
     * @return the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
//        System.out.println("Syscall: " + syscall);
        if (Lib.test('m') || Lib.test('M')) {
            switch (syscall) {
                case syscallHalt:
                    System.out.println("Syscall: Halt");
                    break;
                case syscallExit:
                    System.out.println("Syscall: Exit");
                    break;
                case syscallExec:
                    System.out.println("Syscall: Exec");
                    break;
                case syscallJoin:
                    System.out.println("Syscall: Join");
                    break;
                case syscallCreat:
                    System.out.println("Syscall: Creat");
                    break;
                case syscallOpen:
                    System.out.println("Syscall: Open");
                    break;
                case syscallRead:
                    System.out.println("Syscall: Read");
                    break;
                case syscallWrite:
                    System.out.println("Syscall: Write");
                    break;
                case syscallClose:
                    System.out.println("Syscall: Close");
                    break;
                case syscallUnlink:
                    System.out.println("Syscall: Unlink");
                    break;
            }
        }


        switch (syscall) {
            case syscallHalt:
                return handleHalt();
            case syscallExit:
                return handleExit(a0);
            case syscallExec:
                return handleExec(a0, a1, a2);
            case syscallJoin:
                return handleJoin(a0, a1);
            case syscallCreat:
                return handleCreat(a0);
            case syscallOpen:
                return handleOpen(a0);
            case syscallRead:
                return handleRead(a0, a1, a2);
            case syscallWrite:
                return handleWrite(a0, a1, a2);
            case syscallClose:
                return handleClose(a0);
            case syscallUnlink:
                return handleUnlink(a0);

            default:
                Lib.debug(dbgProcess, "Unknown syscall " + syscall);
                Lib.assertNotReached("Unknown system call!");
        }
        return 0;
    }

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param cause the user exception that occurred.
     */
    public void handleException(int cause) {
        Processor processor = Machine.processor();

        switch (cause) {
            case Processor.exceptionSyscall:
                int result = handleSyscall(processor.readRegister(Processor.regV0),
                    processor.readRegister(Processor.regA0),
                    processor.readRegister(Processor.regA1),
                    processor.readRegister(Processor.regA2),
                    processor.readRegister(Processor.regA3)
                );
                processor.writeRegister(Processor.regV0, result);
                processor.advancePC();
                break;

            default:
                Lib.debug(dbgProcess, "Unexpected exception: " +
                    Processor.exceptionNames[cause]);
                Lib.assertNotReached("Unexpected exception");
        }
    }

    boolean isOpen(String name) {
        for (GlobalFilePair file : allOpenFiles) {
            if (file.fileName.equals(name))
                return true;
        }
        return false;
    }

    private static class Tuple4<A, B, C, D> {
        A first;
        B second;
        C third;
        D fourth;

        Tuple4(A first, B second, C third, D fourth) {
            this.first = first;
            this.second = second;
            this.third = third;
            this.fourth = fourth;
        }
    }
    
    public void incrementInstances (OpenFile openFile) {
        String name = openFile.getName();
           for (GlobalFilePair file : allOpenFiles) {
            if (file.fileName.equals(name))
                ++file.instances;
        } 
    }
    
    public void decrementInstances (OpenFile openFile, int desc) {
        String name = openFile.getName();
        for (GlobalFilePair file : allOpenFiles) {
            if (file.fileName.equals(name))
                --file.instances;
            if(file.instances == 0) {
                allOpenFiles.remove(file);
                handleUnlink(desc);
            }
        }
    }

    /**
     * The program being run by this process.
     */
    protected Coff coff;

    /**
     * This process's page table.
     */
    protected TranslationEntry[] pageTable;
    /**
     * The number of contiguous pages occupied by the program.
     */
    protected int numPages;

    /**
     * The number of pages in the program's stack.
     */
    protected final int stackPages = 8;

    private int initialPC, initialSP;
    private int argc, argv;
    private int pid;
    private static int pidCounter = 0;
    private static int numAlive = 0;
    private HashMap<Integer, Tuple4<UserProcess, Semaphore, Integer, Integer>> childInfo = new HashMap<>();
    Semaphore infoSem = new Semaphore(0);
    UserProcess parentProc;

    protected FileTriple[] openFiles;
    static protected LinkedList<GlobalFilePair> allOpenFiles = new LinkedList<GlobalFilePair>();
    
    protected static class FileTriple {
        protected OpenFile file;
        protected int writeOffset;
        protected int readOffset;

        FileTriple(OpenFile file, int writeOffset, int readOffset) {
            this.file = file;
            this.writeOffset = writeOffset;
            this.readOffset = readOffset;
        }
    }
    
    protected static class GlobalFilePair {
        protected String fileName;
        protected int instances;
        
        GlobalFilePair(String fileName, int instances) {
            this.fileName = fileName;
            this.instances = instances;
        }
        
    }

    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
}
