package org.runetekk.cs;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.evelus.fs.FileIndex;
import org.evelus.fs.archives.FileContainer;
import org.evelus.fs.tables.IndexTable;

/**
 * Dump.java
 * @version 1.0.0
 * @author RuneTekk Development (SiniSoul)
 */
public final class Dump {
    
    /**
     * The instruction name map. 
     */
    private static Map<Integer, String> INSN_NAMES;
    
    /**
     * The starting point for the program.
     * @param args The command line arguments.
     */
    public static void main(String[] args) {
        args = new String[] { "./bin/files/cs614", "./bin/files/dat614", "./bin/files/t614", "./bin/scripts/" };
        System.out.println("Dump::CScript > Written By SiniSoul");
        RandomAccessFile indexFile, dataFile, tableFile;
        try {
            indexFile = new RandomAccessFile(args[0], "r");
            dataFile = new RandomAccessFile(args[1], "r");
            tableFile = new RandomAccessFile(args[2], "r");
        } catch(FileNotFoundException ex) {
            throw new RuntimeException("Failed to find the index and data files"
                                     + " to dump the client scripts...");
        }
        FileIndex tableIndex = new FileIndex(255, tableFile, dataFile);
        FileIndex fileIndex = new FileIndex(12, indexFile, dataFile);
        FileContainer tableContainer = new FileContainer(); 
        try {
            tableIndex.read(tableContainer, 12);
        } catch(IOException ex) {
            throw new RuntimeException("Failed to read the table container file "
                                     + "from the table index...");
        }
        IndexTable indexTable;
        try {
            indexTable = new IndexTable(tableContainer);
        } catch(IOException ex) {
            throw new RuntimeException("Failed to parse the script index table...");
        }
        System.out.println("Preparing to dump "+indexTable.getAmountEntries()+" scripts...");
        ClientScript[] scripts = new ClientScript[indexTable.getAmountEntries()];
        FileContainer scriptFile;
        for(int i = 0; i < indexTable.getAmountEntries(); i++) {
            scriptFile = new FileContainer();
            try {
                ClientScript script = new ClientScript();
                fileIndex.read(scriptFile, i);  
                ByteBuffer buffer = ByteBuffer.wrap(scriptFile.unpack());
                buffer.position(buffer.limit() - 2);
                int length = buffer.getShort();
                int footerOffset = buffer.limit() - length - 12 - 2;
                buffer.position(footerOffset);
                int operandSize = buffer.getInt();
                script.intArgCount = buffer.getShort();
                script.strArgCount = buffer.getShort();
                script.intStkCount = buffer.getShort();
                script.strStkCount = buffer.getShort();
                int jumpCount = buffer.get();
                if(jumpCount > 0) {
                    script.jumpTables = new HashMap[jumpCount];
                    for(int j = 0; j < jumpCount; j++) {
                        int size = buffer.getShort();
                        script.jumpTables[j] = new HashMap<Integer, Integer>(size);
                        while(size-- > 0) {
                            int index = buffer.getInt();
                            int value = buffer.getInt();
                            script.jumpTables[j].put(index, value);
                        }
                    }
                }
                buffer.position(0);
                script.name = getString(buffer);
                script.insnOpcodes = new int[operandSize];
                script.insnOperands = new int[operandSize];
                script.strOperands = new String[operandSize];
                int offset = 0;
                while(buffer.position() < footerOffset) {
                    int opcode = buffer.getShort();
                    if(opcode == 3) {
                        script.strOperands[offset] = getString(buffer).intern();
                    } else if(opcode >= 100|| opcode == 21 || opcode == 38 || opcode == 39)
                        script.insnOperands[offset] = buffer.get();
                    else
                        script.insnOperands[offset] = buffer.getInt();
                    script.insnOpcodes[offset++] = opcode;
                }
                scripts[i] = script;
            } catch(IOException ex) {
                throw new RuntimeException("Failed to read script "+i+"...");
            }
        }  
        System.out.println("Dumping the scripts to text files...");
        BufferedWriter writer;
        for(int i = 0; i < scripts.length; i++) {
            ClientScript script = scripts[i];
            try {
                writer = new BufferedWriter(new FileWriter(args[3] + i +".txt"));
                writer.write("================================================\n");
                writer.write("      Client Script "+i+" Dumped by SiniSoul    \n");
                writer.write("      Instruction Size: "+script.insnOpcodes.length+"\n");
                writer.write("      Integer Arguments: "+script.intArgCount+" \n");
                writer.write("      String  Arguments: "+script.strArgCount+" \n");
                writer.write("================================================\n\n");
                writer.write("================================================\n");
                writer.write("                 Instructions                   \n");
                writer.write("================================================\n\n");               
                for(int j = 0; j < script.insnOpcodes.length; j++) {
                    int insnOpcode = script.insnOpcodes[j];
                    String name = INSN_NAMES.get(insnOpcode);
                    writer.write("Offset: "+j+", ");
                    if(name != null)
                        writer.write(name + " ");
                    else
                        if(insnOpcode > 99)           
                            writer.write("Unref Method: "+insnOpcode);
                        else
                            writer.write("Unref Opcode: "+insnOpcode);
                    Object operand = insnOpcode == 3 ? script.strOperands[j] : script.insnOperands[j];
                    writer.write(", Operand: " + operand + "\n");
                }
                if(script.jumpTables != null) {
                    for(int j = 0; j < script.jumpTables.length; j++) {
                        writer.write("\n================================================\n");
                        writer.write("                Switch Table "+j+"                \n");
                        writer.write("================================================\n\n");
                        Map<Integer, Integer> map = script.jumpTables[j];
                        Set<Integer> keys = map.keySet();
                        for(int key : keys) {
                            writer.write("Key: "+key+", Value: "+map.get(key)+"\n");
                        }
                    }
                }
                writer.flush();
                writer.close();
            } catch(IOException ex) {
                throw new RuntimeException("Failed to dump script "+i+"...");
            }            
        }
    }
    
    /**
     * 
     * @param buffer
     * @return 
     */
    private static String getString(ByteBuffer buffer) {
        String str = "";
        int c = 0;
        while((c = buffer.get()) != 0)
            str += (char) c;
        return str;
    }
    
    static {
        INSN_NAMES = new HashMap<Integer, String>();
        INSN_NAMES.put(0, "PUSHI");
        INSN_NAMES.put(3, "PUSHS");
        INSN_NAMES.put(6, "GOTO");
        INSN_NAMES.put(7, "IFNE");
        INSN_NAMES.put(8, "IFEQ");
        INSN_NAMES.put(9, "IFLT");
        INSN_NAMES.put(10, "IFGT");
        INSN_NAMES.put(21, "RETURN");
        INSN_NAMES.put(31, "IFLTEQ");
        INSN_NAMES.put(32, "IFGTEQ");
        INSN_NAMES.put(33, "LOADINT");
        INSN_NAMES.put(34, "STOREINT");
        INSN_NAMES.put(35, "LOADSTR");
        INSN_NAMES.put(36, "STORESTR");
        INSN_NAMES.put(37, "CONCAT");
        INSN_NAMES.put(38, "POPI");
        INSN_NAMES.put(39, "POPS");
        INSN_NAMES.put(40, "CALL");
        INSN_NAMES.put(42, "LOADGINT");
        INSN_NAMES.put(43, "STOREGINT");
        INSN_NAMES.put(44, "NEWARRAY");
        INSN_NAMES.put(45, "AASTORE");
        INSN_NAMES.put(46, "AALOAD");
        INSN_NAMES.put(47, "LOADGSTR");
        INSN_NAMES.put(48, "STOREGSTR");
        INSN_NAMES.put(51, "SWITCH");
        for(int i = 0; i <= 30; i++)
            INSN_NAMES.put(2400 + i, "setInterfaceDefinitionScriptParams"+i);
        INSN_NAMES.put(1300, "setChildInterfaceText");
        INSN_NAMES.put(4000, "IADD");
        INSN_NAMES.put(4001, "ISUB");
        INSN_NAMES.put(4002, "IMUL");
        INSN_NAMES.put(4003, "IDIV");
        INSN_NAMES.put(4004, "getRandom");
        INSN_NAMES.put(4005, "getRandom2");
        INSN_NAMES.put(4008, "toggleBit");
        INSN_NAMES.put(4009, "untoggleBit");
        INSN_NAMES.put(4010, "isBitToggled");
        INSN_NAMES.put(4011, "IMOD");
        INSN_NAMES.put(4012, "IPOW");
        INSN_NAMES.put(4014, "IAND");
        INSN_NAMES.put(4015, "IOR");
        INSN_NAMES.put(4100, "concatIntAndStr");
        INSN_NAMES.put(4101, "concatStrings");
        INSN_NAMES.put(4102, "concatStrAndIntRadix");
        INSN_NAMES.put(4103, "toLowerCase");
        INSN_NAMES.put(4104, "getTimeToString");
        INSN_NAMES.put(4106, "integerToString");
        INSN_NAMES.put(4112, "concatStrAndChar");
        INSN_NAMES.put(4117, "getStringLength");
        INSN_NAMES.put(4119, "discludeTagsInString");
        INSN_NAMES.put(4120, "indexOfCharInString");
        INSN_NAMES.put(4121, "indexOfStrInString");
        INSN_NAMES.put(4122, "charInStringToLower");
        INSN_NAMES.put(4123, "charInStringToUpper");
        INSN_NAMES.put(5600, "setUsernamePassword");
        INSN_NAMES.put(5615, "doLogin");
        INSN_NAMES.put(5620, "isPreferenceStrNull");
        INSN_NAMES.put(5622, "getPreferenceStr");
        INSN_NAMES.put(5623, "isSSKStrNull");    
        INSN_NAMES.put(5624, "getSSK");  
        INSN_NAMES.put(6302, "getCalendarTime");
        INSN_NAMES.put(6303, "getCurrentYear");
    }
}
