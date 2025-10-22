package src;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

public final class Allocator {

    public Allocator() {}

    private static final int SPILL_ADDR_BASE = 32768;
    private static final int WORD = 4;

    // Primary allocation function
    public static void allocate(IRList ir, int k, int maxVR, int maxLive) {
        if (ir == null || ir.head == null) return;

        // If k < maxLive, we need to reserve a PR for spill address calculations
        final boolean maySpill = (maxLive > k);
        final int addrPR = maySpill ? (k - 1) : -1; // PR used for spill addresses
        final int usableK = maySpill ? (k - 1) : k; // Usable PRs for allocation
        if (usableK <= 0) throw new IllegalArgumentException("k too small for allocation");

        // Initialize primitive arrays for mappings with null values.
        final int[] vr2pr = new int[Math.max(1, maxVR)];
        Arrays.fill(vr2pr, -1);
        final int[] pr2vr = new int[usableK];
        Arrays.fill(pr2vr, -1);
        final int[] prNU  = new int[usableK];
        Arrays.fill(prNU, Integer.MAX_VALUE);

        // Free list of available PRs
        final Deque<Integer> free = new ArrayDeque<>(usableK);
        for (int pr = usableK - 1; pr >= 0; --pr) free.push(pr);

        final int[] vrSpillAddr = new int[Math.max(1, maxVR)];
        Arrays.fill(vrSpillAddr, -1);

        // Per-VR state for spill costs & rematerialization
        final boolean[] vrRemat = new boolean[Math.max(1, maxVR)];   // true if defined by LOADI
        final int[] vrRematImm = new int[Math.max(1, maxVR)];    // the LOADI immediate
        final byte[] vrCleanState = new byte[Math.max(1, maxVR)]; // 0=unknown, 1=clean (in spill slot or remat), 2=dirty

        final boolean[] marked = new boolean[usableK];

        // Main allocation loop, iteratve over each Op node
        for (Op n = ir.head; n != null; n = n.next) {
            Arrays.fill(marked, false);

            //  Allocate PRs for uses
            for (int slot : useSlotIdx(n)) {
                final int v = n.vr[slot];
                if (v < 0) continue;
                int pr = (v < vr2pr.length) ? vr2pr[v] : -1;

                if (pr < 0) {
                    pr = getAPR(v, n.nu[slot], usableK, free, marked, prNU, pr2vr, vr2pr,
                                ir, n, addrPR, maySpill, vrSpillAddr, vrRemat, vrCleanState, vrRematImm);
                    //  Restore: rematerialize if possible; else load from our spill slot
                    if (vrRemat[v]) {
                        insertBefore(ir, n, mkLoadI(n.line, vrRematImm[v], pr));
                        vrCleanState[v] = 2; // dirty (live in PR, not yet copied to spill slot)
                    } else {
                        // normal restore
                        ensureSpillAddr(ir, v, vrSpillAddr);
                        insertBefore(ir, n, mkLoadI(n.line, vrSpillAddr[v], addrPR));
                        insertBefore(ir, n, mkLoad(n.line, addrPR, pr));
                        vrCleanState[v] = 1; // now also has a clean copy in slot
                    }
                }
                // Assign allocated PR
                n.pr[slot] = pr;
                marked[pr] = true;
                prNU[pr] = n.nu[slot];
            }

            // Free PRs whose use is last here
            for (int slot : useSlotIdx(n)) {
                final int v = n.vr[slot];
                if (v < 0) continue;
                // If this is the last use, free the PR
                if (n.nu[slot] == Integer.MAX_VALUE) {
                    final int pr = vr2pr[v];
                    if (pr >= 0 && pr2vr[pr] == v) {
                        vr2pr[v] = -1;
                        pr2vr[pr] = -1;
                        prNU[pr] = Integer.MAX_VALUE;
                        free.push(pr);
                    }
                }
            }

            Arrays.fill(marked, false);

            // Allocate PR for DEF (slot 2 for def opcodes)
            if (isDef(n)) {
                final int v = n.vr[2];
                if (v >= 0) {
                    int pr = vr2pr[v];
                    if (pr < 0) {
                        pr = getAPR(v, n.nu[2], usableK, free, marked, prNU, pr2vr, vr2pr,
                                    ir, n, addrPR, maySpill, vrSpillAddr, vrRemat, vrCleanState, vrRematImm);
                    }
                    // Assign allocated PR
                    n.pr[2] = pr;
                    marked[pr] = true;
                    prNU[pr] = n.nu[2];

                    // Track defining opcode for cost model
                    if (n.opc == Op.Opcode.LOADI) {
                        vrRemat[v] = true;
                        vrRematImm[v] = n.sr[0];
                        vrCleanState[v] = 2; // result produced in PR
                    } else { // Arithmetic or load result: treat as dirty until (if ever) we spill to slot
                        vrRemat[v] = false;
                        vrCleanState[v] = 2;
                    }
                }
            }
        }
        printAllocated(ir); // Print completed allocation
    }

    // Allocate a physical register for virtual register vr at its next use nu
    private static int getAPR(
            final int vr, final int nu, final int usableK,
            final Deque<Integer> free, final boolean[] marked,
            final int[] prNU, final int[] pr2vr, final int[] vr2pr,
            final IRList ir, final Op at,
            final int addrPR, final boolean maySpill, final int[] vrSpillAddr,
            final boolean[] vrRemat, final byte[] vrCleanState, final int[] vrRematImm) {

        Integer pick = null;
        while (!free.isEmpty()) {
            int cand = free.pop();
            if (!marked[cand]) { pick = cand; break; }
        }
        int pr;
        if (pick != null) {
            pr = pick;
        } else {
            pr = chooseVictim(usableK, marked, prNU, pr2vr, vrRemat, vrCleanState);
            final int victimVR = pr2vr[pr];
            if (victimVR >= 0) {
                // Spill policy
                if (vrRemat[victimVR]) {
                    // nothing to store
                    vrCleanState[victimVR] = 1;
                } else if (vrCleanState[victimVR] == 1) {
                    // already clean: nothing to do
                } else { // dirty: store to spill slot
                    if (!maySpill) throw new IllegalStateException("Unexpected spill with maySpill=false");
                    ensureSpillAddr(ir, victimVR, vrSpillAddr);
                    insertBefore(ir, at, mkLoadI(at.line, vrSpillAddr[victimVR], addrPR));
                    insertBefore(ir, at, mkStore(at.line, pr, addrPR));
                    vrCleanState[victimVR] = 1; // now has a clean spill copy
                }
                vr2pr[victimVR] = -1;
                pr2vr[pr] = -1;
                prNU[pr] = Integer.MAX_VALUE;
            }
        }
        // Assign new mapping
        vr2pr[vr] = pr;
        pr2vr[pr] = vr;
        prNU[pr] = nu;
        return pr;
    }

    // Prefer farthest next-use, but if multiple PRs are within 1 of the max NU,
    // break ties by spill-cost class: REMAT > CLEAN > DIRTY, then by NU.
    private static int chooseVictim(final int usableK, final boolean[] marked, final int[] prNU,
                                    final int[] pr2vr, final boolean[] vrRemat, final byte[] vrCleanState) {
        int maxNU = -1;
        for (int pr = 0; pr < usableK; pr++) {
            if (marked[pr]) continue;
            if (prNU[pr] > maxNU) maxNU = prNU[pr];
        }
        final int DELTA = 1; // consider “near ties” within 1
        int bestPR = -1;
        int bestClass = -1;
        int bestNU = -1;

        for (int pr = 0; pr < usableK; pr++) {
            if (marked[pr]) continue;
            final int nu = prNU[pr];
            if (nu + DELTA < maxNU) continue;

            final int v = pr2vr[pr];
            int cls = 0;
            if (v >= 0) {
                if (vrRemat[v]) cls = 2;
                else if (vrCleanState[v] == 1) cls = 1;
                else cls = 0;
            }
            if (cls > bestClass || (cls == bestClass && nu > bestNU)) {
                bestClass = cls;
                bestNU = nu;
                bestPR = pr;
            }
        }
        if (bestPR >= 0) return bestPR;

        // Fallback, farthest-NU if something odd happened
        bestPR = -1; bestNU = -1;
        for (int pr = 0; pr < usableK; pr++) {
            if (marked[pr]) continue;
            if (prNU[pr] > bestNU) { bestNU = prNU[pr]; bestPR = pr; }
        }
        return (bestPR >= 0) ? bestPR : 0;
    }

    // IRList helpers
    private static void insertBefore(IRList ir, Op at, Op neo) {
        neo.next = at;
        neo.prev = at.prev;
        if (at.prev != null) at.prev.next = neo;
        at.prev = neo;
        if (ir.head == at) ir.head = neo;
        ir.count++;
    }

    private static void ensureSpillAddr(IRList ir, int v, int[] vrSpillAddr) {
        if (vrSpillAddr[v] < 0) {
            vrSpillAddr[v] = SPILL_ADDR_BASE + nextSpillAddrDelta();
        }
    }

    private static int spillCursor = 0;
    private static int nextSpillAddrDelta() {
        int delta = spillCursor;
        spillCursor += WORD;
        return delta;
    }

    private static Op mkLoadI(int line, int imm, int dstPR) {
        Op o = new Op(line, Op.Opcode.LOADI);
        o.sr[0] = imm;
        o.pr[2] = dstPR;
        return o;
    }

    private static Op mkLoad(int line, int addrPR, int dstPR) {
        Op o = new Op(line, Op.Opcode.LOAD);
        o.pr[0] = addrPR;
        o.pr[2] = dstPR;
        return o;
    }

    private static Op mkStore(int line, int srcPR, int addrPR) {
        Op o = new Op(line, Op.Opcode.STORE);
        o.pr[0] = srcPR;
        o.pr[2] = addrPR;
        return o;
    }

    // Op shape helpers
    private static boolean isDef(Op n) {
        switch (n.opc) {
            case ADD: case SUB: case MULT: case LSHIFT: case RSHIFT:
            case LOAD: case LOADI:  return true;
            default:                return false;
        }
    }

    private static int[] useSlotIdx(Op n) {
        switch (n.opc) {
            case ADD: case SUB: case MULT: case LSHIFT: case RSHIFT:
                return new int[]{0, 1};
            case LOAD:
                return new int[]{0};
            case STORE:
                return new int[]{0, 2};
            default:
                return new int[0];
        }
    }

    // Walk over IRList and print allocated code
    private static void printAllocated(IRList ir) {
        for (Op n = ir.head; n != null; n = n.next) {
            switch (n.opc) {
                case LOADI: {
                    int x = n.sr[0];
                    int d = n.pr[2];
                    System.out.println("loadI " + x + " => r" + d);
                    break;
                }
                case LOAD: {
                    int a = n.pr[0];
                    int d = n.pr[2];
                    System.out.println("load r" + a + " => r" + d);
                    break;
                }
                case STORE: {
                    int s = n.pr[0];
                    int a = n.pr[2];
                    System.out.println("store r" + s + " => r" + a);
                    break;
                }
                case ADD: case SUB: case MULT: case LSHIFT: case RSHIFT: {
                    int s0 = n.pr[0], s1 = n.pr[1], d = n.pr[2];
                    String op;
                    switch (n.opc) {
                        case ADD: op = "add"; break;
                        case SUB: op = "sub"; break;
                        case MULT: op = "mult"; break;
                        case LSHIFT: op = "lshift"; break;
                        case RSHIFT: op = "rshift"; break;
                        default: op = "???";
                    }
                    System.out.println(op + " r" + s0 + ", r" + s1 + " => r" + d);
                    break;
                }
                case OUTPUT: {
                    System.out.println("output " + n.sr[0]);
                    break;
                }
                case NOP: {
                    break;
                }
            }
        }
    }
}
