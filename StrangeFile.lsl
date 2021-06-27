library StrangeFile;

types {
    StrangeFile(org.mariarheon.strangefilelib.StrangeFile);
    String(java.lang.String);
    int(int);
}

automaton AStrangeFile {
    var may_read: boolean;
    var may_write_or_append: int; // 0: cannot write, 1: can write, 2: write to the end of the file
    var file_should_exist: boolean;

    state Opened;
    state WasUsed;
    finishstate Closed;

    shift Opened->WasUsed (read, write);
    shift WasUsed->self (read, write);
    shift WasUsed->Closed (close);
}

fun StrangeFile.StrangeFile(pathname: String, mode: String) : StrangeFile {
    when (mode) {
        "r" -> result = new AStrangeFile(Opened, true, 0, true);
        "r+" -> result = new AStrangeFile(Opened, true, 1, true);
        "w" -> result = new AStrangeFile(Opened, false, 1, false);
        "w+" -> result = new AStrangeFile(Opened, true, 1, false);
        "a" -> result = new AStrangeFile(Opened, false, 2, false);
        "a+" -> result = new AStrangeFile(Opened, true, 2, false);
        else -> action ERROR("Bad value for mode");
    }
}

fun StrangeFile.read() : int {
    requires (may_read = true);
}

fun StrangeFile.write(ch: int) {
    requires (may_write_or_append > 0);
}

fun StrangeFile.close() {
}
