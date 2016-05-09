package com.silicolife.textmining.ie.re.relation.configuration;

import java.util.Set;
import java.util.SortedSet;

import com.silicolife.textmining.core.interfaces.process.IE.IIEProcess;
import com.silicolife.textmining.core.interfaces.process.IE.re.IRelationsType;

public interface IRERelationAdvancedConfiguration {
	
	// For manual curation
	public boolean useManualCurationFromOtherProcess();
	public IIEProcess getManualCurationFromOtherProcess();
	
	public boolean usingVerbEntitiesDistance();
	public int getVerbEntitieMaxDistance();
	public boolean usingOnlyVerbNearestEntities();
	public SortedSet<IRelationsType> getRelationsType();
	public boolean usingOnlyEntitiesNearestVerb();
	// Remove Synonyms
	public boolean groupingSynonyms();
	
	// Shallow parsing and other
	public Set<String> getBlackListVerbs();
	public boolean allowverbswithinParenthesis();
	
	// Deep Parsing
	public boolean useDeepParsing();
	public boolean entitiesAndVerbsInTheSameSentacePhase();
	public boolean removeEntitiesThatHasINPropositionBefore();
	public boolean removeEntitiesThatHasINOfPropositionAfter();
	public Set<String> getINRemoveList();
	public boolean jumpverbwithCCBefore();
	public Set<String> getCCJumpList();
	public Set<String> getINRemoveListAfter();
	public Set<String> getRemoveListAfter();

}
