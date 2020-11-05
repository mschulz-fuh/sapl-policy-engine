package io.sapl.reimpl.prp.index.canonical;

import io.sapl.grammar.sapl.SAPL;
import io.sapl.prp.inmemory.indexed.Bitmask;
import io.sapl.prp.inmemory.indexed.DisjunctiveFormula;
import io.sapl.prp.inmemory.indexed.improved.CTuple;
import io.sapl.prp.inmemory.indexed.improved.Predicate;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Value
public class CanonicalIndexDataContainer {

    Map<DisjunctiveFormula, Set<SAPL>> formulaToDocuments;

    List<Predicate> predicateOrder;

    List<Set<DisjunctiveFormula>> relatedFormulas;

    Map<DisjunctiveFormula, Bitmask> relatedCandidates;

    Map<Integer, Set<CTuple>> conjunctionsInFormulasReferencingConjunction;

    int[] numberOfLiteralsInConjunction;

    int[] numberOfFormulasWithConjunction;

}
