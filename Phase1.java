import java.io.*;
import java.util.Arrays;

public class Phase1 {

    //Creating a virtual box
    static char[][] M = new char[100][4];          //Physical Memory array
    static char[] IR = new char[4];                //Instruction register to load instructions from memory
    static char[] R = new char[4];                 // General Purpose register
    static char[] buffer = new char[40];           // buffer to store the line from input file
    static char[] output_buffer = new char[50];    //  Store the output of the file in this array before printing in the output.txt
    private int IC, SI;                            // instruction counter
    private boolean C;                             // Toggle bit
    private final BufferedReader read = new BufferedReader(new FileReader("C:\\Users\\santo\\IdeaProjects\\Operating System\\input_new.txt"));
    private final BufferedWriter output = new BufferedWriter(new FileWriter("Output.txt"));
    String line;                                   // Take single line from file and store it int this line variable;
    int program_card_line = 0;
    public Phase1() throws IOException {
    }


    // To initialise the memory contents to Null values
    public void init() {

        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 4; j++) {
                M[i][j] = '\0';
            }
        }
        for(int i =0;i<4;i++){
            IR[i] = '\0';
            R[i] = '\0';
        }

        C = false;
    }

    // Load is used to load all the instructions into the memory
    // It also clears all the arrays before the execution of new program
    // Used recursion to execute the new program and stops when there is no next line to read after the $end instruction
    void load() throws IOException {

        boolean readDTA = false;                  // Read data is taken so to check whether the data card has been read or not. If the data card is read then start reading the data line.

        while ((line = read.readLine()) != null) {
            buffer = line.toCharArray();

            if (buffer[0] == '$' && buffer[1] == 'D' && buffer[3] == 'A') {
                readDTA = true;                                                     // Make the read data line true
            }

            if (buffer.length >= 4 && buffer[0] == '$' && buffer[1] == 'A' && buffer[3] == 'J') {
                init();
            }
            else if (buffer[0] == '$' && buffer[1] == 'E' && buffer[3] == 'D') {
                program_card_line=0;
                load();                                                            // Uses recursion if the line reads $END and if the next line is null then terminates the code or else star executing the next line of code,
                output.close();                                                    // after terminating the code close the write buffer or else the value does not get print in the output file.
            }

            // Stores the program card into the memory
            else if (!readDTA) {
                int x = 0;
                int loop = program_card_line*10;

                for (int i = loop; i < loop+10; i++) {
                    if (buffer[x] == 'H') {
                        M[i][0] = 'H';
                        for (int j = 1; j < 4; j++) {
                            M[i][j] = ' ';
                        }
                        x++;
                    }
                    else{
                        for (int j = 0; j < 4; j++) {
                            if (x < buffer.length && buffer[x] == '\u0000') {
                                break;
                            }
                            else {
                                if (x < buffer.length) {
                                    M[i][j] = buffer[x++];
                                }
                            }
                        }
                    }
                    if (x >= buffer.length || buffer[x] == '\u0000') {
                        break;
                    }
                }
                System.out.println(buffer);
                program_card_line++;
            } else if (readDTA) {
                IC = 0;

                //String data = read.readLine();
                start_execution();

            }
        }

    }

    private void start_execution() throws IOException {

        IC = 0;
        EXECUTEUSERPROGRAM();
    }

    public void EXECUTEUSERPROGRAM() throws IOException {
        int IC=0;
        int j=0;

        while(M[IC][j] != 'H'){

            while(j<4){
                IR[j] = M[IC][j];
                j++;
            }
            IC++;
            System.out.println(IR);
            if(IR[0] == 'G' && IR[1] == 'D'){
                String data = read.readLine();         // read the next line after $DTA into data
                SI = 1;
                MOS(SI,data);
            }else if(IR[0] == 'P'&&IR[1] == 'D'){
                SI=2;
                MOS(SI,"");
            }else if(IR[0] == 'L' && IR[1] == 'R'){
                int m1,m2=0;
                m1 = Integer.parseInt(String.valueOf(IR[2]))*10 + Integer.parseInt(String.valueOf(IR[3]));
                while(m2<4){
                    R[m2] = M[m1][m2];
                    m2++;
                }
            }else if (IR[0] == 'S' && IR[1] == 'R'){
                int m1,m2;
                m1 = Integer.parseInt(String.valueOf(IR[2]))*10 + Integer.parseInt(String.valueOf(IR[3]));
                m2 = 0;
                while(m2<4){
                    M[m1][m2] = R[m2];
                    m2++;
                }
            }else if(IR[0] == 'C' && IR[1] == 'R'){
                int m1,m2;
                m1 = Integer.parseInt(String.valueOf(IR[2]))*10 + Integer.parseInt(String.valueOf(IR[3]));
                m2 = 0;
                //System.out.println(m1);

                String l1 = Arrays.toString(R);
                l1 = l1.replaceAll("[\\[\\] ,]", "");  // produce a string with no spaces
                String l2 = String.valueOf(M[m1][m2++]) + String.valueOf(M[m1][m2++]) + String.valueOf(M[m1][m2++]) + String.valueOf(M[m1][m2]);

                //System.out.println(l1);
                //System.out.println(l2);
                if(l1.equals(l2)){
                    C = true;
                    //System.out.println("True");
                }
            }else if (IR[0] == 'B' && IR[1] == 'T') {
                if (C) {
                    int m1, m2;
                    m1 = Integer.parseInt(String.valueOf(IR[2]))*10 + Integer.parseInt(String.valueOf(IR[3]));
                    IC = m1;
                }
            }
            j=0;
        }
        if(M[IC][j] == 'H'){

            SI=3;
            MOS(SI,"");
        }
    }

    public void MOS(int si,String data) throws IOException {


        switch (SI) {
            // GD instruction execution
            case 1:
                // To get the operand number from instruction e.g.GD10 we need 10
                int m1 = Integer.parseInt(String.valueOf(IR[2])) * 10;
                int m2=0;
                int length = data.length();
                int count_data = 0;
                for(int i=0;i<10;i++){
                    while(m2<4){
                        if(count_data < length) {
                            M[m1][m2] = data.charAt(count_data++);
                            m2++;
                        }else {
                            break;
                        }
                    }
                    m1++;
                    m2=0;
                }


                // Print the contents of memory (M array)
                for (int n = 0; n < 100; n++) {
                    for (int j = 0; j < 4; j++) {
                        //System.out.println("M[" + i + "]\t");
                        System.out.print("["+n+"] ["+j+"] : "+M[n][j]+"  ");
                    }
                    System.out.println();
                }
                break;

            // PD instruction execution
            case 2:
                int p1 = Integer.parseInt(String.valueOf(IR[2])) * 10;
                int p2=0;
                System.out.println();
                int count_buffer = 0;
                for(int i = 0;i<10;i++){
                    while(p2 < 4){

                        if(M[p1][p2] == '\0'){
                            break;
                        }else{
                            output_buffer[count_buffer++] = M[p1][p2];
                            System.out.println(M[p1][p2]);
                            p2++;
                        }
                    }

                    p1++;
                    p2=0;

                }

                // to write the buffer into the output file after the execution of one pd instruction
                for(char buf : output_buffer){
                    if(buf == '\0') {
                        break;
                    }
                    else{
                        output.write(buf);
                    }
                }
                output.write("\n");               // To print on next line after every PD


                //TO clear the output Buffer after every PD instruction

                for(int ii=0;ii<50;ii++){
                    output_buffer[ii] = '\0';
                }

                break;

            case 3:
                System.out.println("CAse 3");
                //terminate();

            default:
                // Handle unknown system interrupt
                terminate();
                break;
        }
    }

    void terminate() {
        try {
            output.write("\n\n");
        } catch (IOException e) {
            System.out.println(e.toString());
        }
    }

    public static void main(String[] args) throws IOException {
        Phase1 p1 = new Phase1();
        p1.load();
    }
}