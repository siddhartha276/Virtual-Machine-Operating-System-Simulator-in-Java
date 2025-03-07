#include <iostream>
#include <fstream>
#include <string>

using namespace std;

class Notations {
public:
    char M[100][4];
    char IR[4];
    int IC;
    char R[4];
    int C;
};

class Phase1 {
    Notations notation;
    fstream inputFile;
    ofstream outputFile;
    bool isTerminate;

public:
    Phase1() : inputFile("input.txt"), outputFile("output.txt", ios::app) {
        if (!inputFile.is_open()) {
            cerr << "Error: Unable to open input file!" << endl;
        }
        if (!outputFile.is_open()) {
            cerr << "Error: Unable to open output file!" << endl;
        }
    }

    void init() {
        for (int i = 0; i < 100; i++) {
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
    }

    void StartFileReading() {

        int memoryBlock;
        string line;

        while (getline(inputFile, line)) {
            if (line.substr(0, 4) == "$AMJ") {
                memoryBlock = 0;
                init();
            } else if (line.substr(0, 4) == "$DTA") {
                printMemory();
                startExecution();
            } else if (line.substr(0, 4) == "$END") {
                if (inputFile.eof()) {
                    return;
                } else {
                    continue;
                }
            } else {
                int pointer = 0;
                for (int start = memoryBlock * 10; start < (memoryBlock * 10) + 10 && pointer < line.length(); start++) {
                    for (int j = 0; j < 4 && pointer < line.length(); j++) {
                        if(line[pointer] == 'H'){
                            notation.M[start][j] = line[pointer++];
                            break;
                        }else{
                            notation.M[start][j] = line[pointer++];
                        }
                    }
                };
                memoryBlock++;
            }
        }
    }

    void startExecution() {
        isTerminate = false;
        while (!isTerminate) {
            printMemory();
            setIR();
            cout << endl;
            cout << endl;
            for (int i = 0; i < 4; i++) {
                cout << notation.IR[i];
            }
            cout << endl;
            cout << endl;
            int SI = 0;

            if (notation.IR[0] == 'G' && notation.IR[1] == 'D') {
                SI = 1;
            } else if (notation.IR[0] == 'P' && notation.IR[1] == 'D') {
                SI = 2;
            } else if (notation.IR[0] == 'H') {
                SI = 3;
            }

            if (SI > 0) {
                MasterMode(SI);
                if (SI == 3) break;
            } else if (notation.IR[0] == 'L' && notation.IR[1] == 'R') {
                loadRegister();
            } else if (notation.IR[0] == 'S' && notation.IR[1] == 'R') {
                storeRegister();
            } else if (notation.IR[0] == 'C' && notation.IR[1] == 'R') {
                compareRegister();
            } else if (notation.IR[0] == 'B' && notation.IR[1] == 'T') {
                branchTo();
            }

            notation.IC++;
        }
    }

    void setIR() {
        for (int i = 0; i < 4; i++) {
            notation.IR[i] = notation.M[notation.IC][i];
        }

    }

    void MasterMode(int SI) {
        if (SI == 1) {
            string data;
            getline(inputFile, data);
            int memory = (notation.IR[2] - '0') * 10 + (notation.IR[3] - '0');
            for (int i = 0; i < data.length() && memory < 100; i++) {
                notation.M[memory][i % 4] = data[i];
                if (i % 4 == 3) memory++;
            }
        } else if (SI == 2) {
            string outputData;
            int memory = (notation.IR[2] - '0') * 10 + (notation.IR[3] - '0');
            for (int i = memory; i < memory + 10 && i < 100; i++) {
                for (int j = 0; j < 4; j++) {
                    if (notation.M[i][j] == '\0') break;
                    outputData += notation.M[i][j];
                }
            }
            outputFile << outputData << endl;
        } else if (SI == 3) {
            outputFile << "Program Executed Successfully" << endl << endl << endl;
            isTerminate = true;
        }
    }

    void loadRegister() {
        int memory = (notation.IR[2] - '0') * 10 + (notation.IR[3] - '0');
        for (int i = 0; i < 4; i++) {
            notation.R[i] = notation.M[memory][i];
        }
    }

    void storeRegister() {
        int memory = (notation.IR[2] - '0') * 10 + (notation.IR[3] - '0');
        for (int i = 0; i < 4; i++) {
            notation.M[memory][i] = notation.R[i];
        }
    }

    void compareRegister() {
        int memory = (notation.IR[2] - '0') * 10 + (notation.IR[3] - '0');
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
        if (notation.C == 1) {
            notation.IC = memory - 1;
        }
    }

    void printMemory(){
        for (int start = 0; start < 100; start++) {
            cout << start << "      ";
            for (int j = 0; j < 4 ; j++) {
                if(notation.M[start][j] == '\0'){
                    cout << " ";
                }else{
                    cout << notation.M[start][j] << " ";
                }
            }
            cout << endl;
        }
    }
};

int main() {
    Phase1 phase1;
    phase1.StartFileReading();
    return 0;
}
