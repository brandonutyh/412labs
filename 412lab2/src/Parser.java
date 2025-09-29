package src;

import java.io.IOException;

public final class Parser {
    private final Scanner sc;
    private Token tok;

    private final IRList ir = new IRList();
    private boolean hadErrors = false;

    public Parser(Scanner sc) { this.sc = sc; }

    public boolean hadErrors() { return hadErrors; }
    public IRList result()     { return ir; }

    // Invoke scanner to get next token
    private void advance() throws IOException { 
        tok = sc.nextToken(); 
        readLexError();
    }
    // Have parser skip new lines
    private void skipNewlines() throws IOException {
        while (tok.getCategory() == Token.TokenCategory.NEWLINE) advance();
    }
    // Helper to determine if file had errors, report
    private void readLexError() {
        if (sc.pollLexError()) {
            hadErrors = true;
        }
    }
    // Helper to print syntax error in correct format
    private void syntaxError(int line, String msg) {
        System.err.printf("ERROR %d:\t%s%n", line, msg);
        hadErrors = true;
    }
    
    private void recoverToEOL() throws IOException {
        // skip until we hit NEWLINE or ENDFILE, 
        // then advance one more time to start at new line's first token
        while (tok.getCategory() != Token.TokenCategory.NEWLINE &&
               tok.getCategory() != Token.TokenCategory.ENDFILE) {
            advance();
        }
        if (tok.getCategory() == Token.TokenCategory.NEWLINE) advance();
    }

    // Helpers to determine if grammar is correct
    // Reg should have constant after
    private Integer expectReg(int errLine, String msg) throws IOException {
        if (tok.getCategory() == Token.TokenCategory.REG) {
            int r = tok.getIntValue();
            advance();
            return r;
        }
        syntaxError(errLine, msg);
        recoverToEOL();
        return null;
    }

    // Constant should be valid integer
    private Integer expectConst(int errLine, String msg) throws IOException {
        if (tok.getCategory() == Token.TokenCategory.CONST) {
            int v = tok.getIntValue();
            advance();
            return v;
        }
        syntaxError(errLine, msg);
        recoverToEOL();
        return null;
    }

    // Comma should be of type COMMA
    private boolean expectComma(int errLine, String msg) throws IOException {
        if (tok.getCategory() == Token.TokenCategory.COMMA) { advance(); return true; }
        syntaxError(errLine, msg);
        recoverToEOL();
        return false;
    }
    
    // Into should be of type INTO
    private boolean expectInto(int errLine, String msg) throws IOException {
        if (tok.getCategory() == Token.TokenCategory.INTO) { advance(); return true; }
        syntaxError(errLine, msg);
        recoverToEOL();
        return false;
    }

    // Main parsing function, consumes tokens from scanner.
    // builds Op nodes for each relevant token
    public IRList parse() throws IOException {
        advance();                // Get next token from scanner
        while (true) {
            skipNewlines();
            // Base case
            if (tok.getCategory() == Token.TokenCategory.ENDFILE) break;

            final int line = tok.getLineNumber();

            switch (tok.getCategory()) {
                case LOADI: {
                    advance(); // consume 'loadI'
                    // Check grammar
                    Integer constant  = expectConst(line, "Missing constant in loadI.");
                    if (constant == null) break;

                    if (!expectInto(line, "Missing '=>' in loadI.")) break;
                    Integer regDest = expectReg(line, "Missing destination register in loadI.");
                    if (regDest == null) break;

                    // Create new Op node
                    Op op = new Op(line, Op.Opcode.LOADI);
                    op.sr[0] = constant;
                    op.sr[2] = regDest;
                    ir.append(op);
                    break;
                }

                case MEMOP: {
                    Token.TokenLexeme lx = tok.getLexeme();
                    // Check which lexeme it is
                    if (lx == Token.TokenLexeme.LOAD_LEX) {
                        advance();
                        Integer regSource = expectReg(line, "Missing source register in load.");
                        if (regSource == null) break;
                        if (!expectInto(line, "Missing '=>' in load.")) break;
                        Integer regDest = expectReg(line, "Missing destination register in load.");
                        if (regDest == null) break;

                        Op op = new Op(line, Op.Opcode.LOAD);
                        op.sr[0] = regSource; op.sr[2] = regDest;
                        ir.append(op);
                    } else if (lx == Token.TokenLexeme.STORE_LEX) {
                        advance();
                        Integer regSource = expectReg(line, "Missing source register in store.");
                        if (regSource == null) break;
                        if (!expectInto(line, "Missing '=>' in store.")) break;
                        Integer regDest = expectReg(line, "Missing destination register in store.");
                        if (regDest == null) break;

                        Op op = new Op(line, Op.Opcode.STORE);
                        op.sr[0] = regSource; op.sr[2] = regDest;
                        ir.append(op);
                    } else {
                        syntaxError(line, "Unrecognized memory operation.");
                        recoverToEOL();
                    }
                    break;
                }

                case ARITHOP: {
                    final Op.Opcode opc;
                    switch (tok.getLexeme()) {
                        case ADD_LEX:   opc = Op.Opcode.ADD;   break;
                        case SUB_LEX:   opc = Op.Opcode.SUB;   break;
                        case MULT_LEX:  opc = Op.Opcode.MULT;  break;
                        case LSHIFT_LEX:opc = Op.Opcode.LSHIFT;break;
                        case RSHIFT_LEX:opc = Op.Opcode.RSHIFT;break;
                        default:
                            syntaxError(line, "Unrecognized arithmetic operation.");
                            recoverToEOL();
                            opc = null;
                            break;
                    }
                    if (tok.getCategory() != Token.TokenCategory.ARITHOP) break;

                    advance(); // consume the opcode token

                    Integer regSource1 = expectReg(line, "Missing first source register in " + opcName(opc) + ".");
                    if (regSource1 == null) break;
                    if (!expectComma(line, "Missing comma in " + opcName(opc) + ".")) break;
                    Integer regSource2 = expectReg(line, "Missing second source register in " + opcName(opc) + ".");
                    if (regSource2 == null) break;
                    if (!expectInto(line, "Missing '=>' in " + opcName(opc) + ".")) break;
                    Integer regDest  = expectReg(line, "Missing destination register in " + opcName(opc) + ".");
                    if (regDest == null) break;

                    Op op = new Op(line, opc);
                    op.sr[0] = regSource1; op.sr[1] = regSource2; op.sr[2] = regDest;
                    ir.append(op);
                    break;
                }

                case OUTPUT: {
                    advance(); // 'output'
                    Integer constant = expectConst(line, "Missing constant in output.");
                    if (constant == null) break;

                    Op op = new Op(line, Op.Opcode.OUTPUT);
                    op.sr[0] = constant;
                    ir.append(op);
                    break;
                }

                case NOP: {
                    advance(); // 'nop'
                    Op op = new Op(line, Op.Opcode.NOP);
                    ir.append(op);
                    break;
                }

                default:
                    syntaxError(line, "Unrecognized operation.");
                    recoverToEOL();
                    break;
            }
        }
        return ir;
    }

    private static String opcName(Op.Opcode o) {
        switch (o) {
            case ADD: return "add";
            case SUB: return "sub";
            case MULT: return "mult";
            case LSHIFT: return "lshift";
            case RSHIFT: return "rshift";
            default: return o.name().toLowerCase();
        }
    }
}
