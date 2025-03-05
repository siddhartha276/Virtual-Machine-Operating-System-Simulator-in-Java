#include <iostream>
#include <fstream>
#include <string>
#include <cstdlib>
#include <ctime>

using namespace std;

class Notations {
public:
    char M[300][4];     // Memory
    char IR[4];         // InStruction Register
    int IC;             // Increment Counter
    char R[4];          // R register
    int C;              // Toggle Bit for T and F
    int PTR;            // Page Table Register (4 bytes) 
    int EM ;            //Error Message  
};

class ProcessControlBlock{
    public:
        int jobID;          // Job ID
        int TTC;            //Total Time Counter 
        int LLC;            //Line Limit Counter 
        int TTL;            //Total Time Limit 
        int TLL;            //Total Line Limit 
};

class Interrupt {
    public:
        int SI;
        int TI;
        int PI;
};

class Phase2 {
    Notations notation;
    ProcessControlBlock pcb;
    Interrupt interrupt;
    fstream inputFile;
    ofstream outputFile;
    bool allocatedPages[30];
    bool isTerminate;

public:
    Phase2() : inputFile("input.txt"), outputFile("output.txt", ios::app) {
        if (!inputFile.is_open()) {
            cerr << "Error: Unable to open input file!" << endl;
        }
        if (!outputFile.is_open()) {
            cerr << "Error: Unable to open output file!" << endl;
        }
    }

    void init() {

        isTerminate = false;

        for(int i=0;i<30;i++){
            allocatedPages[i] = false;
        }

        for (int i = 0; i < 300; i++) {
            for (int j = 0; j < 4; j++) {
                notation.M[i][j] = '\0';
            }
        }

        for (int i = 0; i < 4; i++) {
            notation.IR[i] = '\0';
            notation.R[i] = '\0';
        }

        notation.IC = 0;
        notation.C = 0;
        pcb.LLC = 0;
        pcb.TTC = 0;

        cout << "Every thig cleared" << endl;

    }

    // Allocate Random page from available pages
    // Ensure the seed is set once
    void initializeRandom() {
        srand(131616554); 
    }

    int allocatePage() {
        // Generate a random number between 1 and 30
        int randomBlock = rand() % 30;

        // Ensure the selected page is not already allocated
        while (allocatedPages[randomBlock] == true) {
            randomBlock = rand() % 30;
        }

        allocatedPages[randomBlock] = true;
        return randomBlock;
    }

    int mapAddress(int VA){

        // Operand Code Error & Time Limit Exceeded
        if(( VA < 0 || VA > 99 ) && pcb.TTC > pcb.TTL){
            interrupt.SI = 0;
            interrupt.PI = 2;
            interrupt.TI = 2;
            MasterMode();
        }

        // Operand Code Error
        if( VA < 0 || VA > 99){
            interrupt.SI = 0;
            interrupt.PI = 2;
            interrupt.TI = 0;
            MasterMode();
        }

        int tableLocation = notation.PTR + (VA/10);                                                         // Fetching the number from PTR 1-30
        int address = (notation.M[tableLocation][2] - '0') * 10 + (notation.M[tableLocation][3] - '0');     // Converting it to int
        int physicalAddress = address * 10 + (VA % 10);                                                     // Converting to physical address

        // Page Fault & Time Limit Exceeded
        if(( physicalAddress < 0 || physicalAddress > 299 ) && pcb.TTC > pcb.TTL){
            interrupt.SI = 0;
            interrupt.PI = 3;
            interrupt.TI = 2;
            MasterMode();
        }

        // Page Fault
        if(physicalAddress < 0 || physicalAddress > 299){
            interrupt.SI = 0;
            interrupt.PI = 3;
            interrupt.TI = 0;
            MasterMode();
            physicalAddress = -1;
        }

        return physicalAddress;
    }

    void StartFileReading() {

        int codeLines;  // To store the number of code lines
        string line;

        while (getline(inputFile, line)) {
            if (line.substr(0, 4) == "$AMJ") {
                codeLines = 0;
                int block = allocatePage();            // Generating random block for storing the data
                cout << "PTR Generated = " << block * 10 << endl;
                pcb.jobID = stoi(line.substr(4,4));
                pcb.TTL = stoi(line.substr(8,4));
                pcb.TLL = stoi(line.substr(12,4));
                notation.PTR = block * 10;
                init();
                cout << "Starting new job  " << line << endl; 

            } else if (line.substr(0, 4) == "$DTA") {
                cout << "Started Executing" << endl;
                printMemory();
                startExecution();
            } else if (line.substr(0, 4) == "$END") {
                if (inputFile.eof()) {
                    return;
                } else {
                    continue;
                }
            } else {

                cout << "Reading code" << endl;
                int page = allocatePage();

                // Storing the random physical address generated into the PTR Block
                notation.M[notation.PTR + codeLines][0] = (codeLines + 1) + '0';
                notation.M[notation.PTR + codeLines][1] = '-';
                notation.M[notation.PTR + codeLines][2] = (page / 10) + '0';
                notation.M[notation.PTR + codeLines][3] = (page % 10) + '0';

                page *= 10;

                int pointer = 0;
                for (int start = page; start < page + 10 && pointer < line.length(); start++) {
                    for (int j = 0; j < 4 && pointer < line.length(); j++) {
                        if(line[pointer] == 'H'){
                            notation.M[start][j] = line[pointer++];
                            break;
                        }else {
                            notation.M[start][j] = line[pointer++];
                        }
                    }
                };
                codeLines++;
            }
        }
    }

    void startExecution() {

        while (!isTerminate) {

            int physicalAddress = mapAddress(notation.IC);
            setIR(physicalAddress);

            pcb.TTC++;
            checkTimeLimit();

            if (notation.IR[0] == 'G' && notation.IR[1] == 'D') {
                interrupt.SI = 1;
                interrupt.PI = 0;
                interrupt.TI = 0;
                MasterMode();
            } else if (notation.IR[0] == 'P' && notation.IR[1] == 'D') {
                interrupt.SI = 2;
                interrupt.PI = 0;
                interrupt.TI = 0;
                MasterMode();
                if(pcb.TTC > pcb.TTL){
                    interrupt.SI = 2;
                    interrupt.PI = 0;
                    interrupt.TI = 2;
                    MasterMode();
                }
            } else if (notation.IR[0] == 'H') {
                if(pcb.TTC > pcb.TTL){
                    interrupt.SI = 3;
                    interrupt.PI = 0;
                    interrupt.TI = 2;
                    MasterMode();
                }
                interrupt.SI = 3;
                interrupt.PI = 0;
                interrupt.TI = 0;
                MasterMode();
            }
             else if (notation.IR[0] == 'L' && notation.IR[1] == 'R') {
                loadRegister();
            } else if (notation.IR[0] == 'S' && notation.IR[1] == 'R') {
                storeRegister();
            } else if (notation.IR[0] == 'C' && notation.IR[1] == 'R') {
                compareRegister();
            } else if (notation.IR[0] == 'B' && notation.IR[1] == 'T') {
                branchTo();
            }
            else{
                cout << "Termination Bcz of this" << endl;
                interrupt.SI = 0;
                interrupt.PI = 1;
                interrupt.TI = 0;
                MasterMode();
            }
            notation.IC++;
        }
    }

    void setIR(int block) {
        for (int i = 0; i < 4; i++) {
            notation.IR[i] = notation.M[block][i];
        }
    }

    void MasterMode() {

        if (interrupt.SI == 1 && interrupt.PI == 0 && interrupt.TI == 0) {

            // Reading the data card to memory
            read();

        } else if (interrupt.SI == 2 && interrupt.PI == 0 && interrupt.TI == 0) {

            // Writing to the output file
            write();

        } else if (interrupt.SI == 3 && interrupt.PI == 0 && interrupt.TI == 0) {

            // Terminating the Code
            notation.EM = 0;
            terminate();

        }else if( interrupt.SI == 1 && interrupt.PI == 0 && interrupt.TI == 2 ){

            // While reading file Time Limit Exceeded
            notation.EM = 3;
            terminate();

        } else if( interrupt.SI == 2 && interrupt.PI == 0 && interrupt.TI == 2 ){

            // Write into the file and then terminate with Time Limit Exceeded
            notation.EM = 3;
            terminate();
            
        } else if( interrupt.SI == 3 && interrupt.PI == 0 && interrupt.TI == 2 ){

            // Time Limit Exceeded while finishing the job
            notation.EM = 0;
            terminate();
            
        } else if( interrupt.SI == 0 && interrupt.PI == 1 && interrupt.TI == 0 ){

            // Operation Error E.g. -> GP instead of GD
            notation.EM = 4;
            terminate();
            
        } else if( interrupt.SI == 0 && interrupt.PI == 2 && interrupt.TI == 0 ){

            // Operand Error: Invalid Memory Input
            notation.EM = 5;
            terminate();
            
        } else if(interrupt.SI == 0 && interrupt.PI == 3 && interrupt.TI == 0) {
            
            if((notation.IR[0] == 'G' && notation.IR[1] == 'D') || (notation.IR[0] == 'S' && notation.IR[1] == 'R')){
                // Page Fault
                int memory = (notation.IR[2] - '0') * 10 + (notation.IR[3] - '0');
                int page = allocatePage();
                int pageTableEntry = notation.PTR + (memory/10);
                notation.M[pageTableEntry][2] = (page / 10) + '0';
                notation.M[pageTableEntry][3] = (page % 10) + '0';
                notation.IC--;
            }else{
                notation.EM = 6;
                terminate();
            }

        } else if( interrupt.SI == 0 && interrupt.PI == 1 && interrupt.TI == 2 ){

            notation.EM = 3+4;
            terminate();
            
        } else if( interrupt.SI == 0 && interrupt.PI == 2 && interrupt.TI == 2 ){

            notation.EM = 3+5;
            terminate();
            
        } else if( interrupt.SI == 0 && interrupt.PI == 3 && interrupt.TI == 2 ){

            notation.EM = 3;
            terminate();
            
        }
    }

    void read() {

        int memory = (notation.IR[2] - '0') * 10 + (notation.IR[3] - '0');
        int physicalAddress = mapAddress(memory);

        if(physicalAddress == -1){
            cout << "Page Fault" << endl;
            return;
        }

        string data;
        getline(inputFile, data);

        if(data.substr(0,4) == "$END"){
            
            notation.EM = 1;
            terminate();

        }

        if(notation.M[physicalAddress][2] != '\0'){
            interrupt.SI = 0;
            interrupt.PI = 2;
            interrupt.TI = 0;
            MasterMode();
        } 

        for (int i = 0; i < data.length(); i++) {
            notation.M[physicalAddress][i % 4] = data[i];
            if (i % 4 == 3) physicalAddress++;
        }
    }

    void write(){
        string outputData;

        pcb.LLC++;
        if(pcb.LLC > pcb.TLL){
            
            notation.EM = 2;
            terminate();
            return;

        }

        int memory = (notation.IR[2] - '0') * 10 + (notation.IR[3] - '0');
        int physicalAddress = mapAddress(memory);

        for (int i = physicalAddress; i < physicalAddress + 10 ; i++) {
            for (int j = 0; j < 4; j++) {
                if (notation.M[i][j] == '\0') break;
                outputData += notation.M[i][j];
            }
        }
        outputFile << outputData << endl;
    }

    void loadRegister() {
        int memory = (notation.IR[2] - '0') * 10 + (notation.IR[3] - '0');
        int physicalAddress = mapAddress(memory);
        if(notation.M[physicalAddress][0] == '\0'){
            interrupt.SI = 0;
            interrupt.PI = 3;
            interrupt.TI = 0;
            MasterMode();
        }
        for (int i = 0; i < 4; i++) {
            notation.R[i] = notation.M[physicalAddress][i];
        }
    }

    void storeRegister() {
        int memory = (notation.IR[2] - '0') * 10 + (notation.IR[3] - '0');
        int physicalAddress = mapAddress(memory);

        for (int i = 0; i < 4; i++) {
            notation.M[physicalAddress][i] = notation.R[i];
        }
    }

    void compareRegister() {
        int memory = (notation.IR[2] - '0') * 10 + (notation.IR[3] - '0');
        int physicalAddress = mapAddress(memory);
        notation.C = 1;
        for (int i = 0; i < 4; i++) {
            if (notation.R[i] != notation.M[memory][i]) {
                notation.C = 0;
                break;
            }
        }
    }

    void branchTo() {

        int memory = (notation.IR[2] - '0') * 10 + (notation.IR[3] - '0');
        notation.IC = memory - 1;
    
    }

    void printMemory(){
        for (int i = 0; i < 300; i++) {
            cout << i << "      ";
            for (int j = 0; j < 4 ; j++) {
                if(notation.M[i][j] == '\0'){
                    cout << " ";
                }else{
                    cout << notation.M[i][j] << " ";
                }
            }
            cout << endl;
        }
    }

    void terminate(){
        outputFile << endl << endl;
        printJobDetails();
        if( notation.EM == 0){
            isTerminate = true;
            outputFile << "No Error";
        }
        if( notation.EM == 1){
            outputFile << "Error : Out of Data";
            nextJob();
        }
        if( notation.EM == 2){
            outputFile << "Error : Line Limit Exceeded ";
            nextJob();
        }
        if( notation.EM == 3){
            outputFile << "Error : Time Limit Exceeded";
            nextJob();
        }
        if( notation.EM == 4){
            outputFile << "Error :Operation Code Error";
            nextJob();
        }
        if( notation.EM == 5){
            outputFile << "Error : Operand Error";
            nextJob();
        }
        if( notation.EM == 6){
            outputFile << "Error : Invalid Page Fault";
            nextJob();
        }
        if( notation.EM == 7){
            outputFile << "Error : Time Limit Exceeded, Operation Code Error";
            nextJob();
        }
        if( notation.EM == 8){
            outputFile << "Error : Time Limit Exceeded, Operand Error";
            nextJob();
        }
        outputFile << endl << endl << endl;

    }

    void nextJob(){
        isTerminate = true;
        string line;
        while(getline(inputFile,line)){
            if(line.substr(0,4) == "$END"){
                break;
            }
        }
    }

    void printJobDetails(){
        outputFile << "Job ID : " << pcb.jobID << ", IC = " << notation.IC << ", LLC = " << pcb.LLC << ", TLL = " << pcb.TLL <<", TTC = " << pcb.TTC << ", TTL = " << pcb.TTL << endl;
    }
    
    void checkTimeLimit(){
        if(pcb.TTC > pcb.TTL){
            interrupt.SI = 1;
            interrupt.PI = 0;
            interrupt.TI = 2;
            MasterMode();
        }
    }
};

int main() {
    Phase2 phase2;
    phase2.StartFileReading();
    return 0;
}
