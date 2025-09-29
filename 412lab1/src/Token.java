package src;

public class Token {
    public enum TokenCategory {
        MEMOP,     // load, store
        LOADI,     // loadI
        ARITHOP,   // add, sub, mult, lshift, rshift
        OUTPUT,    // output
        NOP,       // nop
        CONST,     // decimal constant
        REG,       // r<digits>
        COMMA,     // ,
        INTO,      // =>
        NEWLINE,   // end of each physical line
        ENDFILE    // end of file
    }

    public enum TokenLexeme { // Store lexemes, only need 1 constructor for TokenLexeme lexeme, and have swithc statements
    // in the toString to print them
        // MEMOP
        LOAD_LEX("load"),
        STORE_LEX("store"),
        LOADI_LEX("loadI"),
        //ARITHOP
        ADD_LEX("add"),
        SUB_LEX("sub"),
        MULT_LEX("mult"),
        LSHIFT_LEX("lshift"),
        RSHIFT_LEX("rshift"),
        // output,nop,comma,into
        OUTPUT_LEX("output"),
        NOP_LEX("nop"),
        COMMA_LEX(","),
        INTO_LEX("=>");

        private final String text;
        TokenLexeme(String text) { this.text = text; }
        public String text() {
            return text;
        }
    }

    private final TokenCategory category;
    private final TokenLexeme lexeme;   // used for opcodes, punctuation, EOF/EOL
    private final int intValue;    // used for REG and CONSTANT
    private final int lineNumber;

    // Constructor for REG and CONSTANT
    public Token(TokenCategory category, TokenLexeme lexeme, Integer intValue, int lineNumber) {
        this.category = category;
        this.lexeme = lexeme;
        this.intValue = intValue;
        this.lineNumber = lineNumber;
    }
    

    public TokenCategory getCategory() { return category; }
    public TokenLexeme getLexeme() { return lexeme; }
    public int getIntValue() { return intValue; }
    public int getLineNumber() { return lineNumber; }

    @Override
    public String toString() {
        String value;
        switch (category) {
            case REG:
                value = "r" + intValue;
                break;
            case CONST:
                value = Integer.toString(intValue);
                break;
            case NEWLINE:
                value = "\\n";
                break;
            case ENDFILE: // empty string
                value = "";          
                break;
            default:
                // MEMOP, LOADI, ARITHOP, OUTPUT, NOP, COMMA, INTO
                value = (lexeme != null) ? lexeme.text() : "";
                break;
        }
        return lineNumber + ": < " + category + ", \"" + value + "\" >";
    }

}
