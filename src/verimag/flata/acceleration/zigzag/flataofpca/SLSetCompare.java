package verimag.flata.acceleration.zigzag.flataofpca;

import java.util.LinkedList;

import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

import verimag.flata.acceleration.zigzag.*;
import verimag.flata.common.Answer;
import verimag.flata.common.CR;
import verimag.flata.common.FlataJavaSMT;

// PresTAF
import application.*;

public class SLSetCompare {

	private static boolean bb = true;
	
	private static BooleanFormula[] toJSMT(FlataJavaSMT fjsmt, LinSet ls, IntegerFormula k, IntegerFormula n, IntegerFormula dummy) {
		Point base = ls.getBase();
		Point gen = ls.getGenerator();
		
		IntegerFormula a = fjsmt.getIfm().makeNumber(base.getLength());
		IntegerFormula b = fjsmt.getIfm().makeNumber(base.getWeight());
		IntegerFormula c = fjsmt.getIfm().makeNumber((gen == null)? 0 : gen.getLength());
		IntegerFormula d = fjsmt.getIfm().makeNumber((gen == null)? 0 : gen.getWeight());

		IntegerFormula product1 = fjsmt.getIfm().multiply(k, c);
		IntegerFormula sum1 = fjsmt.getIfm().add(a, product1);
		BooleanFormula formula1 = fjsmt.getIfm().equal(n, sum1);

		IntegerFormula product2 = fjsmt.getIfm().multiply(k, d);
		IntegerFormula sum2 = fjsmt.getIfm().add(b, product2);
		BooleanFormula formula2 = fjsmt.getIfm().lessOrEquals(dummy, sum2);

		return new BooleanFormula[]{formula1, formula2};
	}
	private static BooleanFormula toJSMT(FlataJavaSMT fjsmt, SLSet sls, IntegerFormula n, IntegerFormula dummy) {
		if (sls == null || sls.empty()) {
			return fjsmt.getBfm().makeTrue();
		}

		IntegerFormula k = fjsmt.getIfm().makeVariable("k");

		// Begin AND
		LinkedList<BooleanFormula> formulasAND = new LinkedList<>();
		for (LinSet ls : sls.getLinearSets()) {
			BooleanFormula[] formulas = toJSMT(fjsmt, ls, k, n, dummy);
			formulasAND.add(fjsmt.getBfm().implication(formulas[0], formulas[1]));
		}
		// End AND
		BooleanFormula formulaAND = fjsmt.getBfm().and(formulasAND);
		return fjsmt.getQfm().forall(fjsmt.getIfm().makeVariable("k"), formulaAND);
	}

	public static BooleanFormula implicationJSMT(FlataJavaSMT fjsmt, SLSet sls1, SLSet sls2, IntegerFormula n, IntegerFormula dummy) {
		return fjsmt.getBfm().implication(toJSMT(fjsmt, sls1, n, dummy), toJSMT(fjsmt, sls2, n, dummy));
	}

	public static Answer equalJSMT(SLSet sls1, SLSet sls2) {
		FlataJavaSMT fjsmt = CR.flataJavaSMT;

		IntegerFormula n = fjsmt.getIfm().makeVariable("n");
		IntegerFormula dummy = fjsmt.getIfm().makeVariable("x");

		// Begin AND
		BooleanFormula geq = fjsmt.getIfm().greaterOrEquals(n, fjsmt.getIfm().makeNumber(1));

		// Begin NAND
		BooleanFormula impl1 = implicationJSMT(fjsmt, sls1, sls2, n, dummy);
		BooleanFormula impl2 = implicationJSMT(fjsmt, sls2, sls1, n, dummy);

		// End NAND
		BooleanFormula nand = fjsmt.getBfm().not(fjsmt.getBfm().and(impl1, impl2));

		// End AND
		return fjsmt.isSatisfiable(fjsmt.getBfm().and(geq, nand));
	}

	// ##########################################################################
	
	private static Presburger ls2presb(LinSet ls, String n, String k, String dummy) {
		Point base = ls.getBase();
		Point gen = ls.getGenerator();
		
		int a = base.getLength();
		int b = base.getWeight();
		int c = (gen == null)? 0 : gen.getLength();
		int d = (gen == null)? 0 : gen.getWeight();
		
		Presburger p1 = Presburger.equals(Term.variable(n),Term.integer(a).plus(Term.factor(c,k)));
		Presburger p2 = Presburger.equals(Term.variable(dummy), Term.integer(b).plus(Term.factor(d, k)));
		
		return p1.imply(p2).bracket();
	}
	
	private static Presburger sls2presb(SLSet sls, String n, String k, String dummy) {
		// construct true
		Presburger p = Presburger.equals(Term.variable(n), Term.variable(n));
		
		for (LinSet ls : sls.getLinearSets()) {
			p = p.and(ls2presb(ls, n, k, dummy));
		}
		
		Presburger n1 = Presburger.greaterEquals(Term.variable(n), Term.integer(1));
		return p.bracket().forall(Term.getIndex(k), k).and(n1);
	}
	
	public static boolean equalPrestaf(SLSet sls1, SLSet sls2) {
		String n = "n";
		String k = "k";
		String dummy = "X";
		
		Presburger p1 = sls2presb(sls1,n,k,dummy);
		Presburger p2 = sls2presb(sls2,n,k,dummy);
		
		Presburger p = p1.equiv(p2);
		return p.getNPF().isOne();
	}
	
	// ##########################################################################

	private static Presburger prestaf_ls2presb(LinSet ls, String n, String v, String k) {
		Point base = ls.getBase();
		Point gen = ls.getGenerator();
		
		int a = base.getLength();
		int b = base.getWeight();
		int c = (gen == null)? 0 : gen.getLength();
		int d = (gen == null)? 0 : gen.getWeight();
		
		Presburger p1 = Presburger.equals(Term.variable(n),Term.integer(a).plus(Term.factor(c,k)));
		Presburger p2 = Presburger.equals(Term.variable(v), Term.integer(b).plus(Term.factor(d, k)));
		
		return p1.and(p2).bracket();
	}
	
	private static Presburger prestaf_sls2presb(LinSet[] sls, String n, String v, String k) {
		// construct false !!! BE CAREFUL
		//Presburger p = Presburger.notEquals(Term.variable(n), Term.variable(n));
		
		Presburger p = null;
		
		for (LinSet ls : sls) {
			if (p != null)
				p = p.or(prestaf_ls2presb(ls, n, v, k));
			else
				p = prestaf_ls2presb(ls, n, v, k);
		}
		
		if (p == null) // case when sls does not contain any lin. constraint
			p = Presburger.notEquals(Term.variable(n), Term.variable(n));
		
		Presburger kGEQ0 = Presburger.greaterEquals(Term.variable(k), Term.integer(0));
		return p.bracket().and(kGEQ0).bracket().exists(Term.getIndex(k), k).bracket(); //.forall(Term.getIndex(k), k).and(n1);
	}
	
	public static Presburger n_geq1(String n) {
		return Presburger.greaterEquals(Term.variable(n), Term.integer(1));
	}
	public static Presburger n_geq1(Presburger p, String n) {
		return n_geq1(n)
			.imply(p).bracket();
	}
	public static Presburger forall_n_geq1(Presburger p, String n) {
		return n_geq1(p,n)
			.forall(Term.getIndex(n), n);
	}
	
	// TODO
	public static boolean prestaf_checkMinimalSum(LinSet[] op1, LinSet[] op2, LinSet[] res) {
		String n = "n";
		String n1 = "n1";
		String n2 = "n2";
		String v = "v";
		String v1 = "v1";
		String v2 = "v2";
		String k = "k";
		
		Presburger p_op1 = prestaf_sls2presb(op1,n1,v1,k);
		Presburger p_op2 = prestaf_sls2presb(op2,n2,v2,k);
		Presburger p_res = prestaf_sls2presb(res,n,v,k);
		
		Presburger n_EQ_n1n2 = Presburger.equals(Term.variable(n), Term.variable(n1).plus(Term.variable(n2)));
		Presburger v_EQ_v1v2 = Presburger.equals(Term.variable(v), Term.variable(v1).plus(Term.variable(v2)));
		Presburger v_LEQ_v1v2 = Presburger.lessEquals(Term.variable(v), Term.variable(v1).plus(Term.variable(v2)));
		Presburger n1_GEQ_0 = Presburger.greaterEquals(Term.variable(n1), Term.integer(0));
		Presburger n2_GEQ_0 = Presburger.greaterEquals(Term.variable(n2), Term.integer(0));
		
		Presburger p1 = p_op1.and(p_op2).and(n_EQ_n1n2).and(v_EQ_v1v2).and(n1_GEQ_0).and(n2_GEQ_0).bracket()
		.exists(Term.getIndex(v2), v2).exists(Term.getIndex(v1), v1).exists(Term.getIndex(n2), n2).exists(Term.getIndex(n1), n1);
		
		Presburger p2 = p_res.imply(p1).bracket().forall(Term.getIndex(v), v);
		p2 = forall_n_geq1(p2, n);
		
		/*p_op1 = prestaf_sls2presb(op1,n1,v1,k);
		p_op2 = prestaf_sls2presb(op2,n2,v2,k);
		p_res = prestaf_sls2presb(res,n,v,k);
		
		n_EQ_n1n2 = Presburger.equals(Term.variable(n), Term.variable(n1).plus(Term.variable(n2)));
		v_EQ_v1v2 = Presburger.equals(Term.variable(v), Term.variable(v1).plus(Term.variable(v2)));
		v_LEQ_v1v2 = Presburger.lessEquals(Term.variable(v), Term.variable(v1).plus(Term.variable(v2)));
		n1_GEQ_0 = Presburger.greaterEquals(Term.variable(n1), Term.integer(0));
		n2_GEQ_0 = Presburger.greaterEquals(Term.variable(n2), Term.integer(0));*/
		
		Presburger p3 = p_res.and(v_LEQ_v1v2).bracket().exists(Term.getIndex(v), v);
		Presburger p4 = p_op1.and(p_op2).and(n_EQ_n1n2).and(n1_GEQ_0).and(n2_GEQ_0).bracket();
		Presburger p5 = p4.imply(p3).bracket()
		.forall(Term.getIndex(v2), v2).forall(Term.getIndex(v1), v1).forall(Term.getIndex(n2), n2).forall(Term.getIndex(n1), n1);
		p5 = forall_n_geq1(p5, n);
		
		Presburger p = p2.and(p5);

		return p.getNPF().isOne();
	}
	
	// semi-linear set S is a function from set of lengths to set of weights
	// A.n n<1 \/ A.v1 A.v2 (f_S(n,v1) /\ f_S(n,v2)) -> (v1=v2)
	public static boolean prestaf_isFunction(LinSet[] sls) {
		String n = "n";
		String v1 = "v1";
		String v2 = "v2";
		
		String k = "k";
		
		Presburger p1 = prestaf_sls2presb(sls,n,v1,k);
		Presburger p2 = prestaf_sls2presb(sls,n,v2,k);
		
		Presburger v1EQv2 = Presburger.equals(Term.variable(v1), Term.variable(v2));
		Presburger p = p1.and(p2).bracket().imply(v1EQv2)
			.forall(Term.getIndex(v2), v2).forall(Term.getIndex(v1), v1);
		p = forall_n_geq1(p, n);
		//;p = n_geq1(p, n);
		
		return p.getNPF().isOne();
	}
	// S1 is subset of S2
	// A.n n<1 \/ A.v f_S1(n,v) -> f_S2(n,v)
	public static boolean prestaf_subset(LinSet[] sls1, LinSet[] sls2) {
		String n = "n";
		String v = "v";
		
		String k = "k";
		
		Presburger p1 = prestaf_sls2presb(sls1,n,v,k);
		Presburger p2 = prestaf_sls2presb(sls2,n,v,k);
		
		Presburger p = p1.imply(p2).bracket().forall(Term.getIndex(v), v);
		;p = n_geq1(p, n);
		
		return p.getNPF().isOne();
	}
	// S1 contains all minimal points of S2
	// A.n n<1 \/ A.v2 f_S2(n,v2) -> (E.v1 f_S1(n,v1) /\ v1<v2)
	public static boolean prestaf_containsAllMins(LinSet[] sls1, LinSet[] sls2) {
		String n = "n";
		String v1 = "v1";
		String v2 = "v2";
		
		String k = "k";
		
		Presburger v1LEQv2 = Presburger.lessEquals(Term.variable(v1), Term.variable(v2));
		
		Presburger p1 = prestaf_sls2presb(sls2,n,v2,k);
		Presburger p2 = prestaf_sls2presb(sls1,n,v1,k).and(v1LEQv2).bracket().exists(Term.getIndex(v1), v1);
		
		Presburger p = p1.imply(p2).bracket().forall(Term.getIndex(v2), v2);
		p = forall_n_geq1(p, n);
		
		return p.getNPF().isOne();
	}
	// linear set L1 is disjoint with linear set L2
	// A.n A.v  not f(ls1) and not f(ls2)
	public static boolean prestaf_disjoint(LinSet ls1, LinSet ls2) {

		String n = "n";
		String v = "v";
		
		String k = "k";
		
		Presburger p1 = prestaf_ls2presb(ls1,n,v,k).bracket().exists(Term.getIndex(k), k).bracket().not();
		Presburger p2 = prestaf_ls2presb(ls2,n,v,k).bracket().exists(Term.getIndex(k), k).bracket().not();
		
		Presburger p = p1.or(p2).bracket().forall(Term.getIndex(v), v);
		p = forall_n_geq1(p, n);
		
		return p.getNPF().isOne();
	}

	public static boolean prestaf_disjointLinSets(LinSet[] sls) {
		
		for (int i=0; i<sls.length; ++i) {
			for (int j=i+1; j<sls.length; ++j) {
				if (!prestaf_disjoint(sls[i],sls[j]))
					return false;
			}
		}
		return true;
	}

	// parameter1: original set, parameter2: minimized set
	public static boolean prestaf_checkMinimization(LinSet[] sls1, LinSet[] sls2) {
		
		boolean n1 = prestaf_isFunction(sls2); 
		boolean n2 = prestaf_subset(sls2,sls1); 
		boolean n3 = prestaf_containsAllMins(sls2,sls1); 
		boolean n4 = prestaf_disjointLinSets(sls2);
		return n1 && n2 && n3 && n4;
	}

	/*public static boolean prestaf_checkSum(SLSet op1, LinSet op2, SLSet res) {
		LinSet[] aux = new GLinSet[0];
		return prestaf_checkSum(op1.getLinearSets().toArray(aux), new LinSet[]{op2}, res.getLinearSets().toArray(aux));
	}
	public static boolean prestaf_checkSum(SLSet op1, SLSet op2, SLSet res) {
		LinSet[] aux = new GLinSet[0];
		return prestaf_checkSum(op1.getLinearSets().toArray(aux), op2.getLinearSets().toArray(aux), res.getLinearSets().toArray(aux));
	}*/

	
	public static boolean prestaf_checkUnion(LinSet[] op1, LinSet[] op2, LinSet[] res) {
		String n = "n";
		String v = "v";
		String k = "k";
		Presburger p1 = prestaf_sls2presb(op1,n,v,k);
		Presburger p2 = prestaf_sls2presb(op2,n,v,k);
		Presburger pres = prestaf_sls2presb(res,n,v,k);
		
		return p1.or(p2).bracket().equiv(pres).getNPF().isOne();
	}

}
