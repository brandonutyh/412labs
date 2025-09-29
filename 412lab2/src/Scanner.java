package src;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Scanner {
    private final BufferedReader reader;
    private int lineNumber;
    private int ch;  // current char
    private boolean lastReturnedNewLine = false;
    private boolean lexError;

    public Scanner(String fileName) throws IOException {
        this.reader = new BufferedReader(new FileReader(fileName));
        this.lineNumber = 1;
        this.ch = reader.read(); // Get first char rdy
        this.lexError = false;
    }

    // After finishing line, increment read and lineNumber to be recorded on token
     private void advance() throws IOException {
        if (ch == '\n') {
            lineNumber++;
        }
        ch = reader.read();
    }

    private void skipSpace() throws IOException {
        while (ch == ' ' || ch == '\t' || ch == '\r') {
            advance();
        }
    }

    // Method to let scanner know lexical error has been met
    public boolean pollLexError() {
        boolean b = lexError;
        lexError = false;
        return b;
    }
    private Token errorAndContinue(String msg) throws IOException {
        // stderr the msg
        System.err.println("ERROR " + lineNumber + ": " + msg);
        lexError = true;
        // Skip remainder of line
        while (ch != -1 && ch != '\n') {
            ch = reader.read();
        }
        if (ch == '\n') {
            int ln = lineNumber; // report new line
            advance();
            return tokenCheck(new Token(Token.TokenCategory.NEWLINE, null, 0, ln));
        } else { // add a newline if there isnt one
            int ln = lineNumber;
            lineNumber++;
            return tokenCheck(new Token(Token.TokenCategory.NEWLINE, null, 0, ln));
        }
    }

    private Token tokenCheck(Token t) {
        lastReturnedNewLine = (t.getCategory() == Token.TokenCategory.NEWLINE);
        return t;
    }

    public Token nextToken() throws IOException {
        // NEWLINE token
        if (ch == '\n') {
            int ln = lineNumber;
            advance();
            return tokenCheck(new Token(Token.TokenCategory.NEWLINE, null, 0, ln));
        }

        // Skip whitespace, check for newline again
        skipSpace();
        if (ch == '\n') {
            int ln = lineNumber;
            advance();
            return tokenCheck(new Token(Token.TokenCategory.NEWLINE, null, 0, ln));
        }
        // EOF
        if (ch == -1) {
            if (!lastReturnedNewLine) {
                int ln = lineNumber;
                lineNumber++;
                return tokenCheck(new Token(Token.TokenCategory.NEWLINE, null, 0, ln));
            }
            return tokenCheck(new Token(Token.TokenCategory.ENDFILE, null, 0, lineNumber));
        }

        // Comment //
        if (ch == '/') {
            advance();
            if (ch == '/') {
                while (ch != -1 && ch != '\n') {
                    ch = reader.read();
                }
                if (ch == '\n') { // new line exists, just go next
                    int ln = lineNumber;
                    advance();
                    return tokenCheck(new Token(Token.TokenCategory.NEWLINE, null, 0, ln));
                }
                else { // newline doesn't exist, add and go next.
                    int ln = lineNumber;
                    lineNumber++;
                    return tokenCheck(new Token(Token.TokenCategory.NEWLINE, null, 0, ln));

                }
            } else {
                return errorAndContinue("stray '/'");
            }
        }

        // Comma
        if (ch == ',') {
            int ln = lineNumber;
            advance();
            return tokenCheck(new Token(Token.TokenCategory.COMMA, Token.TokenLexeme.COMMA_LEX, 0, ln));
        }

        // Arrow =>
        if (ch == '=') {
            int ln = lineNumber;
            advance();
            if (ch == '>') {
                advance();
                return tokenCheck(new Token(Token.TokenCategory.INTO, Token.TokenLexeme.INTO_LEX, 0, ln));
            } else {
                return errorAndContinue("'=' not followed by '>'");
            }
        }

        // "r" branch: could be REG or rshift
        if (ch == 'r') {
            int ln = lineNumber;
            advance();
            if (Character.isDigit(ch)) {
                // Register r<num>
                StringBuilder sb = new StringBuilder();
                while (Character.isDigit(ch)) {
                    sb.append((char) ch);
                    advance();
                }
                int regNum = Integer.parseInt(sb.toString());
                return tokenCheck(new Token(Token.TokenCategory.REG, null, regNum, ln));
            } else if (ch == 's') {
                // rshift
                advance();
                if (ch == 'h') { advance();
                    if (ch == 'i') { advance();
                        if (ch == 'f') { advance();
                            if (ch == 't') { advance();
                                return tokenCheck(new Token(Token.TokenCategory.ARITHOP, Token.TokenLexeme.RSHIFT_LEX ,0, ln));
                            } else {
                                return errorAndContinue("invalid token starting with 'rshif...'");
                            }
                        } else {
                            return errorAndContinue("invalid token starting with 'rshi...'");
                        }
                    } else {
                        return errorAndContinue("invalid token starting with 'rsh...'");
                    }
                } else {
                    return errorAndContinue("invalid token starting with 'rs...'");
                }
            } else {
                return errorAndContinue("'r' not followed by digits or 'shift'");
            }
        }

        // Constants
        if (Character.isDigit(ch)) {
            int ln = lineNumber;
            StringBuilder sb = new StringBuilder();
            while (Character.isDigit(ch)) {
                sb.append((char) ch);
                advance();
            }
            int constVal = Integer.parseInt(sb.toString());
            return tokenCheck(new Token(Token.TokenCategory.CONST, null, constVal, ln));
        }

        // Opcodes
        int ln = lineNumber;
        if (ch == 'l') { // load, loadI, lshift
            advance();
            if (ch == 'o') { // load / loadI
                advance();
                if (ch == 'a') { advance();
                    if (ch == 'd') { advance();
                        if (ch == 'I') { advance();
                            return tokenCheck(new Token(Token.TokenCategory.LOADI, Token.TokenLexeme.LOADI_LEX, 0, ln));
                        }
                        return tokenCheck(new Token(Token.TokenCategory.MEMOP, Token.TokenLexeme.LOAD_LEX, 0, ln));
                    } else { return errorAndContinue("invalid token starting with 'loa...'");}
                } else { return errorAndContinue("invalid token starting with 'lo...'");}
            } else if (ch == 's') { // lshift
                advance(); if (ch == 'h') { advance();
                    if (ch == 'i') { advance();
                        if (ch == 'f') { advance();
                            if (ch == 't') { advance();
                                return tokenCheck(new Token(Token.TokenCategory.ARITHOP, Token.TokenLexeme.LSHIFT_LEX, 0, ln));
                            } else { return errorAndContinue("invalid token starting with 'lshif...'");}
                        } else { return errorAndContinue("invalid token starting with 'lshi...'");}
                    } else {return errorAndContinue("invalid token starting with 'lsh...'");}
                } else { return errorAndContinue("invalid token starting with 'ls...'");}
            } else { return errorAndContinue("invalid token starting with 'l...'");}
        } 

        if (ch == 's') { // store, sub
            advance();
            if (ch == 't') { advance();
                if (ch == 'o') { advance();
                    if (ch == 'r') { advance();
                        if (ch == 'e') { advance();
                            return tokenCheck(new Token(Token.TokenCategory.MEMOP, Token.TokenLexeme.STORE_LEX, 0, ln));
                        } else {return errorAndContinue("invalid token starting with 'stor...");}
                    } else { return errorAndContinue("invalid token starting with 'sto...'");}
                } else { return errorAndContinue("invalid token starting with 'st...'");}
            } else if (ch == 'u') { advance();
                if (ch == 'b') { advance();
                    return tokenCheck(new Token(Token.TokenCategory.ARITHOP, Token.TokenLexeme.SUB_LEX, 0, ln));
                } else { return errorAndContinue("invalid token starting with 'su...'");}
            } else { return errorAndContinue("invalid token starting with 's...'");}
        }

        if (ch == 'a') { // add
            advance(); 
            if (ch == 'd') { advance();
                if (ch == 'd') { advance();
                    return tokenCheck(new Token(Token.TokenCategory.ARITHOP, Token.TokenLexeme.ADD_LEX, 0, ln));
                } else { return errorAndContinue("invalid token starting with 'ad...'");}
            } else { return errorAndContinue("invalid token starting with 'a...'");}
        }

        if (ch == 'm') { // mult
            advance(); if (ch == 'u') { advance();
                if (ch == 'l') { advance();
                    if (ch == 't') { advance();
                        return tokenCheck(new Token(Token.TokenCategory.ARITHOP, Token.TokenLexeme.MULT_LEX, 0, ln));
                    } else { return errorAndContinue("invalid token starting with 'mul...'"); }
                } else { return errorAndContinue("invalid token starting with 'mu...'"); }
            } else { return errorAndContinue("invalid token starting with 'm...'"); }
        }

        if (ch == 'o') { // output
            advance(); if (ch == 'u') { advance();
                if (ch == 't') { advance();
                    if (ch == 'p') { advance();
                        if (ch == 'u') { advance();
                            if (ch == 't') { advance();
                                return tokenCheck(new Token(Token.TokenCategory.OUTPUT, Token.TokenLexeme.OUTPUT_LEX, 0, ln));
                            } else { return errorAndContinue("invalid token starting with 'outpu...'"); }
                        } else { return errorAndContinue("invalid token starting with 'outp...'"); }
                    } else { return errorAndContinue("invalid token starting with 'out...'"); }
                } else { return errorAndContinue("invalid token starting with 'ou...'"); }
            } else { return errorAndContinue("invalid token starting with 'o...'"); }
        }

        if (ch == 'n') { // nop
            advance(); if (ch == 'o') { advance();
                if (ch == 'p') { advance();
                    return tokenCheck(new Token(Token.TokenCategory.NOP, Token.TokenLexeme.NOP_LEX, 0, ln));
                } else { return errorAndContinue("invalid token starting with 'no...'"); }
            } else { return errorAndContinue("invalid token starting with 'n...'"); }
        }

        // Unknown char
        char bad = (char) ch;
        return errorAndContinue("invalid char '" + bad + "'");
    }
}
