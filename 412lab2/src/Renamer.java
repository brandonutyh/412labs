package src;

public class Renamer {
        int maxLive;
        int vrCount;
        static int INF = Integer.MAX_VALUE;

        // Constructor that represents the result of the renamer being ran on an IRList
        public Renamer(int maxLive, int vrCount) {
            this.maxLive = maxLive;
            this.vrCount = vrCount;
        }

        public static Renamer rename(IRList ir) {
            int maxSR = maxSR(ir);
            int cap = Math.max(1, maxSR + 1);
            int[] srToVR = new int[cap];
            int[] nextUse = new int[cap];
            // Initialize arrays as -1/INF for now
            for (int i = 0; i < cap; i++) {
                srToVR[i] = -1;
            }
            for (int i = 0; i < cap; i++) {
                nextUse[i] = INF;
            }

            int vrName = 0;
            int index = ir.count;
            int live = 0;
            int maxLiveSeen = 0;
            // Bottom-up scan through IRList
            for (Op n = ir.tail; n != null; n = n.prev) {
                // def first (slot 2)
                if (isDef(n)) {
                    int s = n.sr[2];
                    if (srToVR[s] == -1) { // Unused def
                        srToVR[s] = vrName++;  
                    } else { // Overrwrite
                        live--;                  
                    }
                    n.vr[2] = srToVR[s];
                    n.nu[2] = nextUse[s];
                    srToVR[s] = -1;  
                    nextUse[s] = INF;
                }

                // use (slots depend on opcode)
                int[] useSlots = useSlotIdx(n);
                for (int slot : useSlots) {
                    int s = n.sr[slot];
                    if (srToVR[s] == -1) { // First time
                        srToVR[s] = vrName++;
                        live++;
                    if (live > maxLiveSeen) 
                        maxLiveSeen = live;
                    }
                    n.vr[slot] = srToVR[s];
                    n.nu[slot] = nextUse[s];
                }

            // After uses: mark this instruction as next-use for those SRs
            for (int slot : useSlots) nextUse[n.sr[slot]] = index;

            index--;
            }

            return new Renamer(maxLiveSeen, vrName);
        }

        /**
        Helper functions for rename function
        */ 

        // Checks if a given node is a definition
        private static boolean isDef(Op n) {
            switch (n.opc) {
            case ADD: case SUB: case MULT: case LSHIFT: case RSHIFT:
            case LOAD: case LOADI:
                return true;                    // rZ in slot 2 is a DEF
            case STORE: case OUTPUT: case NOP:
            default:
                return false;
            }
        }
        private static int[] useSlotIdx(Op n) {
            switch (n.opc) {
            case ADD: case SUB: case MULT: case LSHIFT: case RSHIFT:
                return new int[] {0, 1};        // rX, rY => rZ
            case LOAD: // Just dest
                return new int[] {0};          
            case STORE:
                return new int[] {0, 2};        // store rVal => rAddr
            case LOADI:
            case OUTPUT:
            case NOP:
            default:
                return new int[0];
            }
        }

        // Finds the maxSR of a IRList
        private static int maxSR(IRList ir) {
            int max = -1;
            for (Op p = ir.head; p != null; p = p.next) {
            switch (p.opc) {
                case ADD: case SUB: case MULT: case LSHIFT: case RSHIFT:
                max = Math.max(max, p.sr[0]);
                max = Math.max(max, p.sr[1]);
                max = Math.max(max, p.sr[2]);
                break;
                case LOAD:
                max = Math.max(max, p.sr[0]); // addr
                max = Math.max(max, p.sr[2]); // dst
                break;
                case STORE:
                max = Math.max(max, p.sr[0]); // val
                max = Math.max(max, p.sr[2]); // addr
                break;
                case LOADI:
                max = Math.max(max, p.sr[2]); // dst (sr[0] is const)
                break;
                case OUTPUT:
                case NOP:
                default:
                break;
            }
            }
            return Math.max(max, 0);
        }

        // Prints the VR of each node
        public void printVR(IRList ir) {
            for (Op n = ir.head; n != null; n = n.next) {
                switch (n.opc) {
                    case ADD:    System.out.printf("add r%d, r%d => r%d%n",    n.vr[0], n.vr[1], n.vr[2]); break;
                    case SUB:    System.out.printf("sub r%d, r%d => r%d%n",    n.vr[0], n.vr[1], n.vr[2]); break;
                    case MULT:   System.out.printf("mult r%d, r%d => r%d%n",   n.vr[0], n.vr[1], n.vr[2]); break;
                    case LSHIFT: System.out.printf("lshift r%d, r%d => r%d%n", n.vr[0], n.vr[1], n.vr[2]); break;
                    case RSHIFT: System.out.printf("rshift r%d, r%d => r%d%n", n.vr[0], n.vr[1], n.vr[2]); break;
                    case LOAD:   System.out.printf("load r%d => r%d%n",        n.vr[0],           n.vr[2]); break;
                    case LOADI:  System.out.printf("loadI %d => r%d%n",        n.sr[0],           n.vr[2]); break;
                    case STORE:  System.out.printf("store r%d => r%d%n",       n.vr[0],           n.vr[2]); break;
                    case OUTPUT: System.out.printf("output %d%n",              n.sr[0]); break;
                    case NOP:    System.out.println("nop"); break;
                    }
            }
        }

}