library PolynomialSolver;

types {
    QuadraticEquation(org.mariarheon.polynomialsolverlib.QuadraticEquation);
    int(int);
    double(double);
    boolean(boolean);
}

automaton AQuadraticEquation {
    var a: double;
    var b: double;
    var c: double;
    var firstRoot: double;
    var secondRoot: double;
    var noRoots: boolean;
    var firstRootIsSet: boolean;
    var secondRootIsSet: boolean;

    state Created;
    state Solved;
    finishstate CountOfRootsVerified;
    state FirstRootRetrieved;
    state SecondRootRetrieved;
    finishstate BothRootsRetrieved;

    shift Created->Solved (solve);
    shift Solved->CountOfRootsVerified (hasNoRoots);
    shift CountOfRootsVerified->FirstRootRetrieved (getFirstRoot);
    shift CountOfRootsVerified->SecondRootRetrieved (getSecondRoot);
    shift FirstRootRetrieved->BothRootsRetrieved (getSecondRoot);
    shift SecondRootRetrieved->BothRootsRetrieved (getFirstRoot);
}

fun QuadraticEquation.QuadraticEquation(a: double, b: double, c: double) : QuadraticEquation {
    result = new AQuadraticEquation(Created, a, b, c, 0.0, 0.0, false, false, false);
}

fun QuadraticEquation.solve();

fun QuadraticEquation.hasNoRoots() : boolean {
    noRoots = result;
}

fun QuadraticEquation.getFirstRoot() : double {
    requires(!old(noRoots));
    firstRoot = result;
    firstRootIsSet = true;
    ensures(!firstRootIsSet || !secondRootIsSet ||
        firstRoot + secondRoot = -b/a || firstRoot * secondRoot = c/a);
}

fun QuadraticEquation.getSecondRoot() : double {
    requires(!old(noRoots));
    secondRoot = result;
    secondRootIsSet = true;
    ensures(!firstRootIsSet || !secondRootIsSet ||
        firstRoot + secondRoot = -b/a || firstRoot * secondRoot = c/a);
}

