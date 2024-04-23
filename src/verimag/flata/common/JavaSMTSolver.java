package verimag.flata.common;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.BasicLogManager;

import org.sosy_lab.java_smt.api.*;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.SolverContextFactory;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.ProverEnvironment;

import java.lang.reflect.Array;
import java.util.*;

import verimag.flata.acceleration.zigzag.SLSet;
import verimag.flata.automata.ca.CATransition;
import verimag.flata.presburger.*;

public class JavaSMTSolver {

    private Configuration config;
    private ShutdownNotifier notifier;
    private LogManager logger;
    private SolverContext context;

    // TODO: Add options for smt solving
    public JavaSMTSolver() {
        try {
            config = Configuration.defaultConfiguration();
            notifier = ShutdownNotifier.createDummy();
            logger = BasicLogManager.create(config);
            context = null;
            context = SolverContextFactory.createSolverContext(config, logger, notifier, Solvers.Z3);
        } catch (Exception e) {
            e.printStackTrace();
            // Handle other exceptions if necessary
        }
    }

    public SolverContext getContext() {
        return context;
    }

    public Boolean isSatisfiable (BooleanFormula constraints) {
        Boolean isSatisfiable = false;
        try (ProverEnvironment prover = context.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
            prover.addConstraint(constraints);
            isSatisfiable = !prover.isUnsat();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isSatisfiable;
    }

    // TODO: remove the following functions

    // TODO: implement this function (yices version in ModuloRel.java)
    public Boolean toSMTFull(ModuloRel modRel, Boolean negate) {
        return false;
    }

    // TODO: implement this function (yices version in ModuloRel.java)
    public Boolean includesSMT(Relation otherRel) {
        return false;
    }

    // TODO: implement this function (yices version in DisjRel.java)
    public Boolean impliesSMT(DisjRel disjRel1, DisjRel disjRel2) {
        return false;
    }

    // TODO: implement this function (yices version in CompositeRel.java)
    public Boolean subSumedSMT(CompositeRel compRel, Collection<CompositeRel> CompRels) {
        return false;
    }

    // TODO: implement this function (yices version in CATransition.java)
    public Boolean inclusionCheckSMT(Collection<CATransition> tOld, CATransition tNew) {
        return false;
    }

    // TODO: implement this function (yices version in CA.java) NOTE: may not be necessary
    public Boolean isIncludedSMT(int inx, CATransition[] tt, BitSet bs) {
        return false;
    }

    // TODO: implement this function (yices version in SLSetCompare.java)
    public Boolean equalSMT(SLSet sls1, SLSet sls2) {
        return false;
    }
}