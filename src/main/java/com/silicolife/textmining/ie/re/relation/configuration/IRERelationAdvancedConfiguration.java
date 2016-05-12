package com.silicolife.textmining.ie.re.relation.configuration;

import java.util.Set;
import java.util.SortedSet;

import com.silicolife.textmining.core.interfaces.process.IE.re.IRelationsType;

public interface IRERelationAdvancedConfiguration {
		
	public boolean isUsingVerbEntitiesDistance();
	public int getVerbEntitieMaxDistance();
	public boolean isUsingOnlyVerbNearestEntities();
	public SortedSet<IRelationsType> getRelationsType();
	public boolean isUsingOnlyEntitiesNearestVerb();
	// Remove Synonyms
	public boolean isGroupingSynonyms();
	
	// Shallow parsing and other
	public Set<String> getBlackListVerbs();
	public boolean isAllowverbswithinParenthesis();
	
	// Deep Parsing
	public boolean isUseDeepParsing();
	public boolean isEntitiesAndVerbsInTheSameSentencePhase();
	public boolean isRemoveEntitiesThatHasINPropositionBefore();
	public boolean isRemoveEntitiesThatHasINOfPropositionAfter();
	public Set<String> getiNRemoveSet();
	public boolean isJumpverbwithCCBefore();
	public Set<String> getCcJumpSet();
	public Set<String> getInRemoveAfterSet();
	public Set<String> getRemoveWordsSetAfter();

}
