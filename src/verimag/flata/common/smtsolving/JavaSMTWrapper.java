package verimag.flata.common.smtsolving;

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

import verimag.flata.presburger.*;

public class JavaSMTWrapper {

    private Configuration config;
    private ShutdownNotifier notifier;
    private LogManager logger;
    private SolverContext context;

    // TODO: Add options for smt solving
    public JavaSMTWrapper() {
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

    private IntegerFormulaManager getIntegerFormulaManager() {
        return context.getFormulaManager().getIntegerFormulaManager();
    }

    // TODO: better names, currently copied style of toSBYicesFull
    // TODO: remove linConstraints, use LinearRel directly (toCol() returns linConstraints)
    public Boolean toSMTFull(LinearRel linRel, Boolean negate) {

        // TODO: be more consistent with use of FormulaManager
        FormulaManager fm = context.getFormulaManager();
        IntegerFormulaManager ifm = fm.getIntegerFormulaManager();

        // // Define Variables
        // Set<Variable> vars = new HashSet<Variable>();
		// for (LinearConstr c : linConstraints) {
		// 	c.variables(vars);
        // }
        // defineVars(vars, ifm);

        // Add constraints
        BooleanFormula constraints = toSMTConj(ifm, linRel.toCol(), negate);

        // Print constraints
        // System.out.println("Constraints: " + constraints);

        Boolean isSatisfiable = false;

        try (ProverEnvironment prover = context.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
            prover.addConstraint(constraints);
            isSatisfiable = !prover.isUnsat();

            // System.out.println("Satisfiable? " + isSatisfiable);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isSatisfiable;
    }

    public Boolean includesSMT(LinearRel linRel1, LinearRel linRel2) {
        // StringWriter sw = new StringWriter();
		// IndentedWriter iw = new IndentedWriter(sw);

		// iw.writeln("(reset)");

		// // define
		// Set<Variable> vars = this.variables();
		// other.refVars(vars);
		// CR.yicesDefineVars(iw, vars);

		// iw.writeln("(assert");
		// iw.indentInc();

		// // other \subseteq this
		// iw.writeln("(and");
		// iw.indentInc();
		// other.toSBYicesList(iw, false); // not negated

        FormulaManager fm = context.getFormulaManager();
        IntegerFormulaManager ifm = fm.getIntegerFormulaManager();

        ArrayList<BooleanFormula> constraints1 = toSMTList(ifm, linRel2.toCol(), false);

		// iw.writeln("(or");
		// iw.indentInc();
		// this.toSBYicesList(iw, true); // negated

        ArrayList<BooleanFormula> constraints2 = toSMTList(ifm, linRel1.toCol(), true);
		// iw.indentDec();
		// iw.writeln(")"); // or
        BooleanFormula orConstraint = this.context.getFormulaManager().getBooleanFormulaManager().or(constraints2);

		// iw.indentDec();
		// iw.writeln(")"); // and
        constraints1.add(orConstraint);
        BooleanFormula constraints = this.context.getFormulaManager().getBooleanFormulaManager().and(constraints1);

        // TODO: remove this
        // Print constraints
        // System.out.println("Constraints: " + constraints);

        //TODO: check if this is correct
        boolean isSatisfiable = false;
        try (ProverEnvironment prover = context.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
            prover.addConstraint(constraints);
            isSatisfiable = !prover.isUnsat();

            // System.out.println("Satisfiable? " + isSatisfiable);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return isSatisfiable; // TODO: default null? == DONTKNOW (yicesanswer)
		// iw.indentDec();
		// iw.writeln(")"); // assert

		// iw.writeln("(check)");

		// StringBuffer yc = new StringBuffer();
		// YicesAnswer ya = CR.isSatisfiableYices(sw.getBuffer(), yc);

		// // unsat implies that relation is included
		// return Answer.createFromYicesUnsat(ya);

    }

    public BooleanFormula toSMTConj(IntegerFormulaManager ifm, List<LinearConstr> linConstraints, Boolean negate) {
        ArrayList<BooleanFormula> constraints = toSMTList(ifm, linConstraints, negate);

        // Add conjunction of constraints
        return this.context.getFormulaManager().getBooleanFormulaManager().and(constraints);
    }

    public ArrayList<BooleanFormula> toSMTList(IntegerFormulaManager ifm, List<LinearConstr> linConstraints, Boolean negate) {
        ArrayList<BooleanFormula> constraints = new ArrayList<BooleanFormula>(); 
        
		for (LinearConstr c : linConstraints) {
            // TODO: send linear constraint c with functions
            if (negate) {
                // Add negation of constraint
                constraints.add(ifm.greaterThan(toSMTLinearConstr(c), ifm.makeNumber(0)));

            } else {
                constraints.add(ifm.lessOrEquals(toSMTLinearConstr(c), ifm.makeNumber(0)));
            }
		}

        return constraints;
    }

    // TODO: there are multiple toSBYices functions (LinearConstr, LinearTerm, etc.)
    public IntegerFormula toSMTLinearConstr(LinearConstr c) {
        Collection<LinearTerm> values = c.values();
		Iterator<LinearTerm> iter = values.iterator();
		int size = values.size();

        if (size == 0) {
            return toSMTLinearTerm(iter.next());
        }

        ArrayList<IntegerFormula> termFormulas = new ArrayList<IntegerFormula>();

        while (iter.hasNext()) {
            LinearTerm term = iter.next();
            termFormulas.add(toSMTLinearTerm(term));
        }

        // Sum terms
        return this.getIntegerFormulaManager().sum(termFormulas);
    }

    public IntegerFormula toSMTLinearTerm(LinearTerm term) {
        Variable var = term.getVariable();
        int coeff = term.getCoeff();
        IntegerFormula coeffFormula = this.getIntegerFormulaManager().makeNumber(coeff);

        String xx;
        if (var == null) {
            return coeffFormula;
        } else if (var.isPrimed()) {
            xx = var.getUnprimedName() + "_p"; // TODO: either remove or add suffixes as parameters as done in toSBYices
        } else {
            xx = var.name();
        }

        if (var != null) {
            xx = "V_" + var.name();
            IntegerFormula xxFormula = this.getIntegerFormulaManager().makeVariable(xx);
            return this.getIntegerFormulaManager().multiply(coeffFormula, xxFormula);
        } else {
            IntegerFormula one = this.getIntegerFormulaManager().makeNumber(1);
            return this.getIntegerFormulaManager().multiply(coeffFormula, one);
        }
    }

}
