
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Random;

class PCB{
    int jobId;
    int TTL;
    int TLL;
    int LLC;
    int TTC;

    public PCB() {
        // Default constructor
    }

    public PCB(int jobId, int TTL, int TLL) {
        this.jobId = jobId;
        this.TTL = TTL;
        this.TLL = TLL;
        LLC = 0;
        TTC = 0;
    }
}

public class Phase2 {

    // registers of the virtual machine
    private char[] IR;
    private char[] R;
    private int SI;
    private int IC;
    private boolean C;
    private int PTR;
    private int TI;
    private int PI;
    private PCB pcb;
    private boolean[] isAllocated;
    // Memory of the Virtual machine
    private char[][] M;
    // reader and writer for I/O operations
    BufferedReader reader;
    PrintWriter writer;
    // Buffer between memory and IO
    private char[] buffer;
    // Auxiliary variables
    private int pageCounter;
    private boolean isTerminated;

    // Constructor of the OS
    public Phase2() throws IOException {
        // initializing the registers
        IR = new char[4];
        R = new char[4];
        IC = 0;
        SI = 0;
        TI = 0;
        PI = 0;
        C = false;
        // Map to keep track of allocated pages
        isAllocated = new boolean[30];
        // initialize memory
        M = new char[300][4];

        // initialize the IO workers
        buffer = new char[40];
        reader = new BufferedReader(new FileReader("C:\\Users\\santo\\IdeaProjects\\Operating System\\input_new.txt"));
        File file = new File("Output.txt");
        writer = new PrintWriter(file);
    }

    // Function to initialize the registers and memory
    public void initialize(int pageNumber) throws IOException {
        for (int i = 0; i < 300; i++) {
            for (int j = 0; j < 4; j++) {
                M[i][j] = ' ';
            }
        }

        for(int i = 0; i<4; i++) {
            IR[i] = ' ';
            R[i] = ' ';
        }

        IC = 0;
        SI = 0;
        TI = 0;
        PI = 0;
        C = false;
        pcb = new PCB();
        pageCounter = 0;
        isTerminated = false;

        isAllocated[pageNumber] = true;

        for (int j = 0; j < 40; j++) {
            buffer[j] = ' ';
        }
    }

    // Function to allocate a random page from available pages
    private int allocatePage() {
        Random random = new Random();
        int pageNumber = random.nextInt(30);
        while(isAllocated[pageNumber]) {
            pageNumber = random.nextInt(30);
        }
        isAllocated[pageNumber] = true;
        return pageNumber;
    }

    // Function to map a virtual address to corresponding  physical address
    int mapAddress(int virtualAddress) throws IOException {

        // Check for operand error
        if (virtualAddress < 0 || virtualAddress > 99) {
            this.SI = 0;
            this.PI = 2;
            this.TI = 0;
            MOS();
        }

        int pageTableEntry = PTR + (virtualAddress / 10);

        int M_of_PTE = (M[pageTableEntry][2] - '0') * 10 + (M[pageTableEntry][3] - '0');

        int physicalAddress = M_of_PTE * 10 + (virtualAddress % 10);

        if (physicalAddress < 0 || physicalAddress > 299) {
            this.PI = 3;
            this.TI = 0;
            this.SI = 0;
            return -1;
        }

        return physicalAddress;
    }

    // Function to load the program and data cards
    public void load() throws IOException {
        String line = null;

        while((line = reader.readLine()) != null) {

            for (int j = 0; j < 40; j++) {
                buffer[j] = ' ';
            }

            for(int i = 0; i < line.length(); i++) {
                buffer[i] = line.charAt(i);
            }

            if(buffer[0] == '$' && buffer[1] == 'A' && buffer[2] == 'M' && buffer[3] == 'J') {
                int pageNumber = allocatePage();

                int jobId = Character.getNumericValue(buffer[4]) * 1000 + Character.getNumericValue(buffer[5]) * 100 + Character.getNumericValue(buffer[6]) * 10 + Character.getNumericValue(buffer[7]);
                System.out.println(jobId);
                int TTL = Character.getNumericValue(buffer[8]) * 1000 + Character.getNumericValue(buffer[9]) * 100 + Character.getNumericValue(buffer[10]) * 10 + Character.getNumericValue(buffer[11]);
                int TLL = Character.getNumericValue(buffer[12]) * 1000 + Character.getNumericValue(buffer[13]) * 100 + Character.getNumericValue(buffer[14]) * 10 + Character.getNumericValue(buffer[15]);
                this.pcb =  new PCB(jobId, TTL, TLL);
                this.PTR = pageNumber * 10;
                initialize(pageNumber);
                this.pcb =  new PCB(jobId, TTL, TLL);
                for(int i = 0; i < 10; i++) {
                    for(int j = 0; j < 4; j++) {
                        M[PTR + i][j] = '*';
                    }
                }
            }else if(buffer[0] == '$' && buffer[1] == 'D' && buffer[2] == 'T' && buffer[3] == 'A') {
                startExecution();
            }else if(buffer[0] == '$' && buffer[1] == 'E' && buffer[2] == 'N' && buffer[3] == 'D') {
                continue;
            }else {
                System.out.println(line);
                int page = allocatePage();

                M[PTR+pageCounter][2] = (char) ((page / 10) + '0');
                M[PTR+pageCounter][3] = (char) ((page % 10) + '0');
                pageCounter +=1;
                page *= 10;

                int k = 0;

                for(int i = 0; i < 10; i++) {

                    for(int j = 0; j<4; j++) {
                        if(buffer[k] == 'H') {
                            M[page + i][j] = buffer[k++];
                            break;
                        }
                        M[page + i][j] = buffer[k++];
                    }
                }
            }
        }
        writer.close();
    }

    // Start the execution of the program
    private void startExecution() throws IOException {
        IC = 00;
        executeUserProgram();
    }

    // execute the user program
    private void executeUserProgram() throws IOException {
        isTerminated = false;
        while(!isTerminated) {
            int physicalAddress = mapAddress(IC);
            IR = M[physicalAddress];
            IC++;
            if(IR[0] == 'G' && IR[1] == 'D') {
                int operand = Character.getNumericValue(IR[2])*10 + Character.getNumericValue(IR[3]);
                int realOperand = mapAddress(operand);
                if(realOperand == -1) {
                    this.pcb.TTC++;
                    PI = 3;
                    SI = 0;
                    TI = 0;
                    MOS();
                    continue;
                }
                SI = 1;
                TI = 0;
                PI = 0;
                MOS();
            }else if(IR[0] == 'P' && IR[1] == 'D') {
                SI = 2;
                PI = 0;
                MOS();
            }else if(IR[0] == 'H') {
                this.pcb.TTC++;
                SI = 3;
                PI = 0;
                TI = 0;
                MOS();
                break;
            }else if(IR[0] == 'L' && IR[1] == 'R') {
                loadRegister();
            }else if(IR[0] == 'S' && IR[1] == 'R') {

                int operand=0;

                if (Character.isDigit(IR[2]) && Character.isDigit(IR[3])) {
                    operand = Character.getNumericValue(IR[2]) * 10 + Character.getNumericValue(IR[3]);
                    int realOperand = mapAddress(operand);
                    if(realOperand == -1) {
                        this.pcb.TTC++;
                        PI = 3;
                        SI = 0;
                        TI = 0;
                        MOS();
                        continue;
                    }
                    storeRegister();
                } else {
                    this.SI = 0;
                    this.PI = 2;
                    this.TI = 0;
                    MOS();
                    System.out.println(isTerminated);
                }
                //int operand = Character.getNumericValue(IR[2])*10 + Character.getNumericValue(IR[3]);


            }else if(IR[0] == 'C' && IR[1] == 'R') {
                compareRegister();
            }else if(IR[0] == 'B' && IR[1] == 'T') {
                branchTo();
            }else {
                System.out.println("Inside Else");
                PI = 1;
                TI = 0;
                MOS();
                break;
            }

            this.pcb.TTC++;
            if(pcb.TTC == pcb.TTL) {
                SI = 1;
                TI = 2;
                MOS();
            }

            if (isTerminated) {
                System.out.println("inside isTerminated");
                break;
            }

        }
    }

    // function to handle the read interrupt
    private void read() throws IOException {
        // Clear the buffer
        for (int i = 0; i < 40; i++) {
            buffer[i] = ' ';
        }

        int k = 0;
        String line;

        if((line = reader.readLine()) != null) {

            for(int i=0; i<line.length(); i++) {
                buffer[i] = line.charAt(i);
            }

            if(line.contains("$END") || (buffer[0] == '$' && buffer[1] == 'E' && buffer[2] == 'N' && buffer[3] == 'D')) {
                terminate(1);
            }else {
                int virtualAddress = Character.getNumericValue(IR[2])*10 + Character.getNumericValue(IR[3]);
                int physicalAddress = mapAddress(virtualAddress);

                for(int i = 0; i<10; i++) {
                    for(int j = 0; j<4; j++) {
                        if(k < 40) {
                            M[physicalAddress + i][j] = buffer[k];
                            k++;
                        }
                    }
                }
            }
        }else {
            System.err.println("End of the input file reached !!!");
        }
    }

    // function to handle the write interrupt
    private void write() throws IOException {
        this.pcb.LLC++;
        if(this.pcb.LLC > this.pcb.TLL) {
            terminate(2);
        }else {
            // Clear the buffer
            for (int i = 0; i < 40; i++) {
                buffer[i] = ' ';
            }
            int k = 0;
            int virtualAddress = Character.getNumericValue(IR[2])*10 + Character.getNumericValue(IR[3]);
            int physicalAddress = mapAddress(virtualAddress);

            if(physicalAddress == -1) {
                PI = 3;
                SI = 0;
                TI = 0;
                MOS();
                return;
            }

            for(int i = 0; i<10; i++) {
                for(int j = 0; j<4; j++) {
                    if(k < 40) {
                        buffer[k] = M[physicalAddress + i][j];
                        k++;
                    }
                }
            }
            writer.println(new String(buffer));
            writer.flush();
        }
    }

    // function to handle the terminate interrupt
    private void terminate(int... EM) {
        for(int i : EM) {
            String message = "Program exited with : ";
            if(i == 0) {
                message += "NO ERRORS ";
                isTerminated = true;
            }else if(i == 1) {
                message += "OUT OF DATA ERROR ";
                isTerminated = true;
            }else if(i == 2) {
                message += "LINE LIMIT EXCEEDED ERROR ";
                isTerminated = true;
            }else if(i == 3) {
                message += "TIME LIMIT EXCEEDED ERROR ";
                isTerminated = true;
            }else if(i == 4) {
                message += "OPERATION CODE ERROR ";
                isTerminated = true;
            }else if(i == 5) {
                message += "OPERAND ERROR ";
                isTerminated = true;
            }else if(i == 6) {
                message += "INVALID PAGE FAULT ERROR ";
                isTerminated = true;
            }
            writer.println(message);
        }
        writer.print("Job Id : "+this.pcb.jobId);
        writer.print("  |  \tIC : "+this.IC);
        writer.print("  |  \tIR : "+Arrays.toString(this.IR));
        writer.print("  |  \tTTC : "+this.pcb.TTC);
        writer.print("  |  \tLLC : "+this.pcb.LLC);
        writer.println();
        writer.println();
        writer.flush();
    }

    // Store the register into memory
    private void storeRegister() throws IOException{
        int virtualAddress = Character.getNumericValue(IR[2])*10 + Character.getNumericValue(IR[3]);
        int operand = mapAddress(virtualAddress);

        for(int i = 0; i<4; i++) {
            M[operand][i] = R[i];
        }
    }

    // Load the memory
    private void loadRegister() throws IOException {
        int virtualAddress = Character.getNumericValue(IR[2])*10 + Character.getNumericValue(IR[3]);
        int operand = mapAddress(virtualAddress);
        if(operand == -1) {
            PI = 3;
            SI = 0;
            TI = 0;
            MOS();
            return;
        }
        for(int i = 0; i<4; i++) {
            R[i] = M[operand][i];
        }
    }

    // Compare the register with the memory
    private void compareRegister() throws IOException {
        char temp[] = new char[4];
        int virtualAddress = Character.getNumericValue(IR[2])*10 + Character.getNumericValue(IR[3]);
        int operand = mapAddress(virtualAddress);
        if(operand == -1) {
            PI = 3;
            SI = 0;
            TI = 0;
            MOS();
            return;
        }
        for(int i = 0; i<4; i++) {
            temp[i] = M[operand][i];
        }

        C = Arrays.equals(temp, R);
    }

    // Branch to give instruction
    private void branchTo(){
        if(C) {
            int newIC = Character.getNumericValue(IR[2])*10 + Character.getNumericValue(IR[3]);
            IC = newIC;
        }
    }

    // Master Operating System
    private void MOS() throws IOException {
        // Software Interrupts
        if(TI == 0 && SI == 1) {
            // read the data
            read();
        }else if(TI == 0 && SI == 2) {
            // write the data
            write();
        }else if(TI == 0 && SI == 3) {
            // normal end of program
            terminate(0);
        }else if(TI == 2 && SI == 1){
            // time limit exceeded
            terminate(3);
        }else if(TI == 2 && SI == 2){
            // write and then terminated
            terminate(3);
        }else if(TI == 2 && SI == 3){
            terminate(0);
        }

        // Program interrupts
        if(TI == 0 && PI == 1) {
            // opcode error
            terminate(4);
        }else if(TI == 0 && PI == 2) {
            // operand error
            terminate(5);
        }else if(TI == 0 && PI == 3) {

            // page fault
            if((IR[0] == 'S' && IR[1] == 'R') || (IR[0] == 'G' && IR[1] == 'D')) {
                int virtualAddress = Character.getNumericValue(IR[2])*10 + Character.getNumericValue(IR[3]);
                int pageTableEntry = PTR + (virtualAddress / 10);
                int page = allocatePage();
                M[pageTableEntry][2] = (char) ((page / 10) + '0');
                M[pageTableEntry][3] = (char) ((page % 10) + '0');
                pageCounter += 1;
                IC--;
            }else{
                if (!isTerminated) {
                    terminate(6);
                    isTerminated = true;
                }
            }
        }else if(TI == 2 && PI == 1) {
            terminate(3, 4);
        }else if(TI == 2 && PI == 2) {
            terminate(3, 5);
        }else if(TI == 3 && PI == 3) {
            terminate(3);
        }
    }

    public void displayMemory() {

        for (int block = 0; block < 300; block++) {
            System.out.print("Block " + block + ": ");
            for (int word = 0; word < 4; word++) {
                char asciiChar = M[block][word];
                String asciiString = Character.toString(asciiChar);
                System.out.print(asciiString + " ");
            }
            System.out.println();
        }

        System.out.print("R : ");
        for(int i = 0; i<4; i++) {
            char asciiChar = R[i];
            String asciiString = Character.toString(asciiChar);
            System.out.print(asciiString+" ");
        }

        System.out.println();
        System.out.println("C : "+C);
        System.out.println("PTR : "+PTR);
        System.out.println("Page Counter : "+pageCounter);
        System.out.println(Arrays.toString(this.isAllocated));
    }

    public static void main(String[] args) throws IOException{
        Phase2 os = new Phase2();
        os.load();
        os.displayMemory();
    }

}