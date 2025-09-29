package src;

import java.util.Objects;

// Class representing a node in the linked list
public final class Op {
    public enum Opcode {
        LOAD, LOADI, STORE,
        ADD, SUB, MULT, LSHIFT, RSHIFT,
        OUTPUT, NOP
    }

    // Doubly-linked list, node has prev and next
    public Op prev, next;

    // Source line number (start of the op)
    public final int line;

    // Which operation
    public final Opcode opc;

    // Operand slots
    public final int[] sr = new int[3];
    public final int[] vr = new int[3];
    public final int[] pr = new int[3];
    public final int[] nu = new int[3];

    // Represents 1 node in linked list
    public Op(int line, Opcode opc) {
        this.line = line;
        this.opc  = Objects.requireNonNull(opc);
    }

    private String opcodeName() {
        switch (opc) {
            case LOADI:  return "loadI";
            case LOAD:   return "load";
            case STORE:  return "store";
            case ADD:    return "add";
            case SUB:    return "sub";
            case MULT:   return "mult";
            case LSHIFT: return "lshift";
            case RSHIFT: return "rshift";
            case OUTPUT: return "output";
            case NOP:    return "nop";
            default:     return "<unk>";
        }
    }

    // Put registers in SR array.
    private String slotText(int i) {
        switch (opc) {
            case LOADI:
                if (i == 0) return "val " + sr[0]; // constant
                if (i == 2) return "sr" + sr[2];   // dest
                return "";
            case OUTPUT:
                if (i == 0) return "val " + sr[0];
                return "";
            case LOAD:   // load rS => rD
            case STORE:  // store rS => rD
                if (i == 0) return "sr" + sr[0];
                if (i == 2) return "sr" + sr[2];
                return "";
            case ADD:
            case SUB:
            case MULT:
            case LSHIFT:
            case RSHIFT:
                // all three used: sr0, sr1, sr2
                return "sr" + sr[i];
            case NOP:
                return "";
            default:
                return "";
        }
    }

    @Override public String toString() {
        String s0 = box(slotText(0));
        String s1 = box(slotText(1));
        String s2 = box(slotText(2));
        return String.format("%-7s %s, %s, %s", opcodeName(), s0, s1, s2);
    }

    private static String box(String v) {
        return (v == null || v.isEmpty()) ? "[ ]" : "[ " + v + " ]";
    }
}
