package verimag.flata.common;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.BasicLogManager;

import org.sosy_lab.java_smt.api.*;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.SolverContextFactory;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;

import com.google.common.collect.ImmutableList;

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

    private ImmutableList<Model.ValueAssignment> modelAssignments;

    // TODO: remove this (maybe)
    private int solverCalls;

    // TODO: Add options for smt solving
    public JavaSMTSolver() {
        try {
            config = Configuration.defaultConfiguration();
            notifier = ShutdownNotifier.createDummy();
            logger = BasicLogManager.create(config);
            // context = null;
            context = SolverContextFactory.createSolverContext(config, logger, notifier, Solvers.Z3);
            ifm = context.getFormulaManager().getIntegerFormulaManager();
            bfm = context.getFormulaManager().getBooleanFormulaManager();
            qfm = context.getFormulaManager().getQuantifiedFormulaManager();

            solverCalls = 0;
        } catch (Exception e) {
            e.printStackTrace();
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

    public ImmutableList<ValueAssignment> getModelAssignments() {
        return modelAssignments;
    }

    public int getSolverCalls() {
        return solverCalls;
    }

    public Answer isSatisfiable (BooleanFormula formula) {
        return isSatisfiable(formula, false, false);
    }

    public Answer isSatisfiable (BooleanFormula formula, Boolean invert) {
        return isSatisfiable(formula, invert, false);
    }

    public Answer isSatisfiable (BooleanFormula formula, Boolean invert, Boolean generateModel) {
        solverCalls++;
        Boolean isSatisfiable = false;
        try (ProverEnvironment prover = generateModel ? context.newProverEnvironment(ProverOptions.GENERATE_MODELS) : context.newProverEnvironment()) {
            prover.addConstraint(formula);
            isSatisfiable = !prover.isUnsat();
            // No model exists when unsat, will throw exception if asked for
            if (generateModel && isSatisfiable) {
                modelAssignments = prover.getModelAssignments();
            }
            if (invert) {
                return Answer.createAnswer(isSatisfiable).negate();
            }
            return Answer.createAnswer(isSatisfiable);
        } catch (Exception e) {
            System.out.println("Error in isSatisfiable: " + e.getMessage());
            System.out.println("isSat: " + isSatisfiable);
            return Answer.DONTKNOW;
        }
    }
}