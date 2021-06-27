library ProjectDevelopment;

types {
    ProjectDevelopment(org.mariarheon.legacylib.ProjectDevelopment);
}

automaton AProjectDevelopment {
    state initiating;
    state planning;
    state executing;
    state monitoring_and_controlling;
    finishstate closing;

    shift initiating->planning (planning);
    shift planning->executing (executing);
    shift executing->monitoring_and_controlling (monitoringAndControlling);
    shift monitoring_and_controlling->executing (executing);
    shift monitoring_and_controlling->planning (planning);
    shift executing->closing (closing);
}

fun ProjectDevelopment.ProjectDevelopment() : ProjectDevelopment {
    result = new AProjectDevelopment(initiating);
}

fun ProjectDevelopment.planning();
fun ProjectDevelopment.executing();
fun ProjectDevelopment.monitoringAndControlling();
fun ProjectDevelopment.closing();
