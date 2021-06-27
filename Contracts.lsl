library Contracts;

types {
    File(org.mariarheon.contractslib.File);
    Accumulator(org.mariarheon.contractslib.Accumulator);
    String(java.lang.String);
    int(int);
}

automaton AFile {
    var may_read: boolean;
    var may_write_or_append: int; // 0: cannot write, 1: can write, 2: write to the end of the file
    var file_should_exist: boolean;

    state Opened;
    finishstate Closed;

    shift any->Closed (close);
}

fun File.File(pathname: String, mode: String) : File {
    when (mode) {
        "r" -> result = new AFile(Opened, true, 0, true);
        "r+" -> result = new AFile(Opened, true, 1, true);
        "w" -> result = new AFile(Opened, false, 1, false);
        "w+" -> result = new AFile(Opened, true, 1, false);
        "a" -> result = new AFile(Opened, false, 2, false);
        "a+" -> result = new AFile(Opened, true, 2, false);
        else -> action ERROR("Bad value for mode");
    }
}

fun File.read() : int {
    requires (may_read = true);
    requires (2+2*2 = 6);
    requires 2+2*2 = 8;
    requires 2*2+2 = 6;
}

fun File.write(ch: int) {
    requires (may_write_or_append > 0);
    ensures (old(may_write_or_append) = 1 || may_write_or_append = 2) && ch > 10;
}

fun File.close() {
}



automaton AAccumulator {
    var base_value: int;

    state Created;
}

fun Accumulator.Accumulator() : Accumulator {
    result = new AAccumulator(Created, 0);
}

fun Accumulator.Accumulator(some_base: int) : Accumulator {
    result = new AAccumulator(Created, some_base);
}

fun Accumulator.add(some_val: int) : int {
    ensures base_value+some_val = result;
}

fun Accumulator.sub(some_val: int) : int {
    ensures base_value-some_val = result;
}


fun Accumulator.mul(some_val: int) : int {
    ensures base_value*some_val = result;
}


fun Accumulator.div(some_val: int) : int {
    ensures base_value/some_val = result;
}

fun Accumulator.mod(some_val: int) : int {
    ensures base_value%some_val = result;
}
