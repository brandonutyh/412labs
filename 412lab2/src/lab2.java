package src;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;

public class lab2 {

	// Heplper function to stdout help message
	private static void printHelp() {
		System.out.println("Usage:");
		System.out.println("  412alloc -h 		  # Show this help message");
		System.out.println("  412alloc -x <file>          # Rename only, print VR code");
		System.out.println("  412alloc <k> <file>         # Allocate registers with k registers");
	}

	// Helper function to print allocated code
	public static void printAllocated(IRList block) {
		for (Op p = block.head; p != null; p = p.next) {
		switch (p.opc) {
			case LOADI:
			System.out.printf("loadI %d => r%d\n", p.sr[0], p.pr[2]);
			break;
			case LOAD:
			System.out.printf("load r%d => r%d\n", p.pr[0], p.pr[2]);
			break;
			case STORE:
			System.out.printf("store r%d => r%d\n", p.pr[0], p.pr[2]);
			break;
			case ADD:
			System.out.printf("add r%d, r%d => r%d\n", p.pr[0], p.pr[1], p.pr[2]);
			break;
			case SUB:
			System.out.printf("sub r%d, r%d => r%d\n", p.pr[0], p.pr[1], p.pr[2]);
			break;
			case MULT:
			System.out.printf("mult r%d, r%d => r%d\n", p.pr[0], p.pr[1], p.pr[2]);
			break;
			case LSHIFT:
			System.out.printf("lshift r%d, r%d => r%d\n", p.pr[0], p.pr[1], p.pr[2]);
			break;
			case RSHIFT:
			System.out.printf("rshift r%d, r%d => r%d\n", p.pr[0], p.pr[1], p.pr[2]);
			break;
			case OUTPUT:
			System.out.printf("output %d\n", p.sr[0]);
			break;
			case NOP:
			System.out.println("nop");
			break;
			}
		}
	}

	public static void main(String[] args) {
		// Print help commands case
		if (args.length == 0 || args[0].equals("-h")) { 
			printHelp();
			return;
		}

		boolean renameOnly = false;
		int k;
		String path;
		// -x flag case
		if (args[0].equals("-x")) {
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
			if (k < 3 || k > 64) { // k must be valid range
				System.err.println("ERROR: k must be in [3, 64].");
				return;
		}
			renameOnly = false;
			path = args[1];
		}
		
		// Happy case, extract the linked list from lab1
		// If renamer flag, invoke Renamer class printVR to see VR form
		// Else continue with allocation
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
			Renamer renameResult = Renamer.rename(irList);
			if (renameOnly) {
				renameResult.printVR(irList);
				return;
			}
			
			// Allocation phase
			Allocator alloc = new Allocator();
			alloc.allocate(irList, k, renameResult.vrCount, renameResult.maxLive);
		} catch (IOException e) {
			System.err.println("ERROR: I/O error: " + e.getMessage());
		}
		

		
	}
}

