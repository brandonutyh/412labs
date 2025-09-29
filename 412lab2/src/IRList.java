package src;

// Class representing the linked list block
public final class IRList {
    public Op head, tail;
    public int count;
    // Linked list helper to attend node and set pointers
    public void append(Op node) {
        if (head == null) head = tail = node;
        else { tail.next = node; node.prev = tail; tail = node; }
        count++;
    }

    // Print for -r
    public void print() {
        for (Op p = head; p != null; p = p.next) {
            System.out.println(p); // uses Op.toString() in reference style
        }
    }
}
