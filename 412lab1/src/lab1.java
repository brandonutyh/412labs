package src;

import java.io.IOException;

public class lab1 {
    public static void main(String[] args) { parseArguments(args); }

    private static void printHelp() {
        System.out.println("Usage: 412fe [flag] <filename>");
        System.out.println("Flags:");
        System.out.println("  -h          Show this help message");
        System.out.println("  -s <file>   Scan the file and print tokens");
        System.out.println("  -p <file>   Parse the file and report success/errors (default)");
        System.out.println("  -r <file>   Print the intermediate representation");
    }

    public static void parseArguments(String[] args) {
        String mode = "-p";
        String filename = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-h": mode = "-h"; break;
                case "-r":
                    if (!mode.equals("-h")) {
                        mode = "-r";
                        if (i + 1 < args.length) filename = args[++i];
                        else { System.err.println("ERROR: Missing filename for -r"); printHelp(); return; }
                    }
                    break;
                case "-s":
                    if (!(mode.equals("-h") || mode.equals("-r"))) {
                        mode = "-s";
                        if (i + 1 < args.length) filename = args[++i];
                        else { System.err.println("ERROR: Missing filename for -s"); printHelp(); return; }
                    }
                    break;
                case "-p":
                    if (!(mode.equals("-h") || mode.equals("-r"))) {
                        mode = "-p";
                        if (i + 1 < args.length) filename = args[++i];
                        else { System.err.println("ERROR: Missing filename for -p"); printHelp(); return; }
                    }
                    break;
                default:
                    if (!args[i].startsWith("-") && filename == null && mode.equals("-p")) {
                        filename = args[i];
                    } else {
                        System.err.println("ERROR: Unknown argument " + args[i]);
                        printHelp();
                        return;
                    }
            }
        }
        if (!mode.equals("-h") && filename == null) {
            System.err.println("ERROR: No filename provided for " + mode + " mode");
            printHelp();
            return;
        }
        try {
            switch (mode) {
                case "-h":
                    printHelp();
                    break;
                case "-s": {
                    Scanner scanner = new Scanner(filename);
                    Token tok;
                    do {
                        tok = scanner.nextToken();
                        System.out.println(tok);
                    } while (tok.getCategory() != Token.TokenCategory.ENDFILE);
                    break;
                }
                case "-p": {
                    Scanner scanner = new Scanner(filename);
                    Parser parser = new Parser(scanner);
                    IRList ir = parser.parse();
                    if (parser.hadErrors()) {
                        System.err.println("Parse found errors.");
                    } else {
                        System.out.printf("Parse succeeded. Processed %d operations.%n", ir.count);
                    }
                    break;
                }
                case "-r": {
                    Scanner scanner = new Scanner(filename);
                    Parser parser = new Parser(scanner);
                    IRList ir = parser.parse();
                    if (parser.hadErrors()) {
                        System.err.println("\nDue to the syntax error, run terminates.");
                    } else {
                        ir.print(); // Print out IR
                    }
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("ERROR: Scan/Parse error on " + filename + ": " + e.getMessage());
        }
    }
}
