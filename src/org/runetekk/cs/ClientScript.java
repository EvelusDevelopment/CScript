package org.runetekk.cs;

import java.util.Map;

/**
 * ClientScript.java
 * @version 1.0.0
 * @author RuneTekk Development (SiniSoul)
 */
public final class ClientScript {
    String name;
    int intArgCount;
    int strArgCount;
    int intStkCount;
    int strStkCount;
    Map<Integer, Integer>[] jumpTables;
    int[] insnOpcodes;
    int[] insnOperands;
    String[] strOperands;
}
