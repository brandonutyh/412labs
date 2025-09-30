package src;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.File;
import java.io.FileReader;

public class lab2 {

	// Heplper function to stdout help message
	private static void printHelp() {
		System.out.println("Usage:");
		System.out.println("  412alloc -h");
		System.out.println("  412alloc -x <file>          # rename-only (Code Check #1)");
		System.out.println("  412alloc <k> <file>         # allocate to k physical registers");
	}

	public static void main(String[] args) {
		// printHelp();
		
		// Print help commands case
		if (args.length == 0 || args[0] == "-h") { 
			printHelp();
			return;
		}

		boolean renameOnly = false;
		int k;
		String path;
		// -x flag case
		if (args[0] == "-x") {
			if (args.length != 2) { // Must include single pathname
				System.err.println("ERROR: -x requires a single pathname");
				return;
			}
			renameOnly = true;
			k = -1;
			path = args[1];
		} else { // k flag case
			if (args.length != 2) { // Must include single pathname
				System.err.println("ERROR: -x requires a single pathname");
				return;
			}
			try {
				k = Integer.parseInt(args[0]);
			} catch (NumberFormatException nfe) {
				System.err.println("ERROR: first argument must be an integer k (3..64).");
				return;
			}
			if (k < 3 || k > 64) {
				System.err.println("ERROR: k must be in [3, 64].");
				return;
		}
			renameOnly = false;
			path = args[1];

		}
		
		// Happy case, extract the linked list from lab1
		// If renamer flag, invoke Renamer class
		// Else, allocate
		try {
			File f = new File(path);
			if (!f.canRead()) {
				System.err.println("ERROR: cannot open file: " + f.getPath());
				return;
			}
			Scanner scanner = new Scanner(f.getPath());
			Parser parser = new Parser(scanner);
			IRList irList = parser.parse();
			if (parser.hadErrors()) {
				System.err.println("Parse found errors, aborting");
				return;
			}
			// Rename phase
			Renamer renamer = new Renamer(0, 0);
			Renamer result = renamer.rename(irList);
			if (renameOnly) {
				renamer.printVR(irList);
				return;
			}
			
			// Allocation phase
		} catch (IOException e) {
			System.err.println("ERROR: I/O error: " + e.getMessage());
		}
		

		
	}
}

