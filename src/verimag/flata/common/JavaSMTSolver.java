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
    private IntegerFormulaManager ifm;
    private BooleanFormulaManager bfm;
    private QuantifiedFormulaManager qfm;

    private int solverCalls;

    // TODO: Add options for smt solving
    public JavaSMTSolver() {
        try {
            config = Configuration.defaultConfiguration();
            notifier = ShutdownNotifier.createDummy();
            logger = BasicLogManager.create(config);
            context = null;
            context = SolverContextFactory.createSolverContext(config, logger, notifier, Solvers.Z3);
            ifm = context.getFormulaManager().getIntegerFormulaManager();
            bfm = context.getFormulaManager().getBooleanFormulaManager();
            qfm = context.getFormulaManager().getQuantifiedFormulaManager();

            solverCalls = 0;
        } catch (Exception e) {
            e.printStackTrace();
            // Handle other exceptions if necessary
        }
    }

    public SolverContext getContext() {
        return context;
    }

    public IntegerFormulaManager getIfm() {
        return ifm;
    }

    public BooleanFormulaManager getBfm() {
        return bfm;
    }

    public QuantifiedFormulaManager getQfm() {
        return qfm;
    }

    public int getSolverCalls() {
        return solverCalls;
    }

    public Boolean isSatisfiable (BooleanFormula constraints) {
        solverCalls++;
        Boolean isSatisfiable = false;
        try (ProverEnvironment prover = context.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
            prover.addConstraint(constraints);
            isSatisfiable = !prover.isUnsat();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isSatisfiable;
    }
}