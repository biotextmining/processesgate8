package com.silicolife.textmining.ie.re.relation.configuration;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.silicolife.textmining.core.datastructures.process.re.RelationTypeImpl;
import com.silicolife.textmining.core.interfaces.process.IE.re.IRelationsType;
import com.silicolife.textmining.core.interfaces.resource.lexicalwords.ILexicalWords;

public class RERelationAdvancedConfigurationImpl implements IRERelationAdvancedConfiguration{

	private boolean usingVerbEntitiesDistance;
	private int verbEntitieMaxDistance;
	private boolean usingOnlyVerbNearestEntities;
	private boolean usingOnlyEntitiesNearestVerb;
	private SortedSet<IRelationsType> relationsType;
	
	private boolean groupingSynonyms;

	
	// Black List Verbs
	private Set<String> blackListVerbs;
	private boolean usingblacklist = true;
	
	// Remove verbs inside of parentities;
	private boolean allowverbswithinParenthesis = false;
	
	// Deep Parsing
	private boolean useDeepParsing = true;
	private boolean entitiesAndVerbsInTheSameSentencePhase = true; 
	private boolean removeEntitiesThatHasINPropositionBefore = true;
	private boolean removeEntitiesThatHasINOfPropositionAfter = true;
	private Set<String> iNRemoveSet;
	private Set<String> inRemoveAfterSet;
	private Set<String> removeWordsSetAfter;
	private boolean jumpverbwithCCBefore = true;
	private Set<String> ccJumpSet;
	
	public RERelationAdvancedConfigurationImpl()
	{
		
	}

	
	public RERelationAdvancedConfigurationImpl(boolean usingOnlyVerbNearestEntities,boolean usingOnlyEntitiesNearestVerb,int verbEntitieMaxDistance,boolean groupingSynonyms,
			SortedSet<IRelationsType> relationsType,ILexicalWords biomedicalVerbsFilter)
	{
		this.usingVerbEntitiesDistance = false;
		this.usingOnlyVerbNearestEntities = usingOnlyVerbNearestEntities;
		this.usingOnlyEntitiesNearestVerb = usingOnlyEntitiesNearestVerb;
		this.verbEntitieMaxDistance = verbEntitieMaxDistance;
		this.groupingSynonyms = groupingSynonyms;
		if(this.verbEntitieMaxDistance > 0)
			usingVerbEntitiesDistance = true;
		this.relationsType = relationsType;
		this.blackListVerbs = new HashSet<>();
		this.iNRemoveSet = new HashSet<>();
		this.ccJumpSet = new HashSet<>();
		this.inRemoveAfterSet = new HashSet<String>();
		this.removeWordsSetAfter = new HashSet<String>();
	}
	
	
	@Override
	public boolean isUsingVerbEntitiesDistance() {
		return usingVerbEntitiesDistance;
	}

	public void setUsingVerbEntitiesDistance(boolean usingVerbEntitiesDistance) {
		this.usingVerbEntitiesDistance = usingVerbEntitiesDistance;
	}


	@Override
	public int getVerbEntitieMaxDistance() {
		return verbEntitieMaxDistance;
	}
	
	public void setVerbEntitieMaxDistance(int verbEntitieMaxDistance) {
		this.verbEntitieMaxDistance = verbEntitieMaxDistance;
	}


	@Override
	public boolean isUsingOnlyVerbNearestEntities() {
		return usingOnlyVerbNearestEntities;
	}

	public void setUsingOnlyVerbNearestEntities(boolean usingOnlyVerbNearestEntities) {
		this.usingOnlyVerbNearestEntities = usingOnlyVerbNearestEntities;
	}


	@Override
	@JsonDeserialize(contentAs=RelationTypeImpl.class)
	public SortedSet<IRelationsType> getRelationsType() {
		return relationsType;
	}

	public void setRelationsType(SortedSet<IRelationsType> relationsType) {
		this.relationsType = relationsType;
	}


	@Override
	public boolean isUsingOnlyEntitiesNearestVerb() {
		return usingOnlyEntitiesNearestVerb;
	}
	
	public void setUsingOnlyEntitiesNearestVerb(boolean usingOnlyEntitiesNearestVerb) {
		this.usingOnlyEntitiesNearestVerb = usingOnlyEntitiesNearestVerb;
	}


	@Override
	public Set<String> getBlackListVerbs() {
		if(usingblacklist && blackListVerbs.isEmpty())
		{
			blackListVerbs.add("maize");
			blackListVerbs.add("leaf");
			blackListVerbs.add("drought");
			blackListVerbs.add("ppGpp");		
			blackListVerbs.add("seedlings");		
		}
		return blackListVerbs;
	}

	public void setBlackListVerbs(Set<String> blackListVerbs) {
		this.blackListVerbs = blackListVerbs;
	}


	@Override
	public boolean isAllowverbswithinParenthesis() {
		return allowverbswithinParenthesis;
	}
	
	public void setAllowverbswithinParenthesis(boolean allowverbswithinParenthesis) {
		this.allowverbswithinParenthesis = allowverbswithinParenthesis;
	}


	@Override
	public boolean isGroupingSynonyms() {
		return groupingSynonyms;
	}

	public void setGroupingSynonyms(boolean groupingSynonyms) {
		this.groupingSynonyms = groupingSynonyms;
	}


	@Override
	public boolean isUseDeepParsing() {
		return useDeepParsing;
	}

	public void setUseDeepParsing(boolean useDeepParsing) {
		this.useDeepParsing = useDeepParsing;
	}


	@Override
	public boolean isEntitiesAndVerbsInTheSameSentencePhase() {
		return entitiesAndVerbsInTheSameSentencePhase;
	}

	public void setEntitiesAndVerbsInTheSameSentencePhase(
			boolean entitiesAndVerbsInTheSameSentencePhase) {
		this.entitiesAndVerbsInTheSameSentencePhase = entitiesAndVerbsInTheSameSentencePhase;
	}


	@Override
	public boolean isRemoveEntitiesThatHasINPropositionBefore() {
		return removeEntitiesThatHasINPropositionBefore;
	}

	public void setRemoveEntitiesThatHasINPropositionBefore(
			boolean removeEntitiesThatHasINPropositionBefore) {
		this.removeEntitiesThatHasINPropositionBefore = removeEntitiesThatHasINPropositionBefore;
	}


	@Override
	public Set<String> getiNRemoveSet() {
		if(isRemoveEntitiesThatHasINPropositionBefore() && iNRemoveSet.isEmpty())
		{
			iNRemoveSet.add("under");
			iNRemoveSet.add("in");
			iNRemoveSet.add("while");
			iNRemoveSet.add("during");		
			iNRemoveSet.add("without");		
//			inremove.add("with"); interacts with
//			inremove.add("of");		
		}
		return iNRemoveSet;
	}

	public void setiNRemoveSet(Set<String> iNRemoveSet) {
		this.iNRemoveSet = iNRemoveSet;
	}


	public boolean isJumpverbwithCCBefore() {
		return jumpverbwithCCBefore;
	}

	public void setJumpverbwithCCBefore(boolean jumpverbwithCCBefore) {
		this.jumpverbwithCCBefore = jumpverbwithCCBefore;
	}


	@Override
	public Set<String> getCcJumpSet() {
		if(isJumpverbwithCCBefore() && ccJumpSet.isEmpty())
		{
			ccJumpSet.add("and");
			ccJumpSet.add("but");		
		}
		return ccJumpSet;
	}

	public void setCcJumpSet(Set<String> ccJumpSet) {
		this.ccJumpSet = ccJumpSet;
	}


	@Override
	public boolean isRemoveEntitiesThatHasINOfPropositionAfter() {
		return removeEntitiesThatHasINOfPropositionAfter;
	}
	
	public void setRemoveEntitiesThatHasINOfPropositionAfter(
			boolean removeEntitiesThatHasINOfPropositionAfter) {
		this.removeEntitiesThatHasINOfPropositionAfter = removeEntitiesThatHasINOfPropositionAfter;
	}


	@Override
	public Set<String> getInRemoveAfterSet() {
		if(isRemoveEntitiesThatHasINOfPropositionAfter() && inRemoveAfterSet.isEmpty())
		{
			inRemoveAfterSet.add("of");	
		}
		return inRemoveAfterSet;
	}
	
	public void setInRemoveAfterSet(Set<String> inRemoveAfterSet) {
		this.inRemoveAfterSet = inRemoveAfterSet;
	}


	@Override
	public Set<String> getRemoveWordsSetAfter() {
		if(isRemoveEntitiesThatHasINOfPropositionAfter() && removeWordsSetAfter.isEmpty())
		{
			removeWordsSetAfter.add("expression");	
			removeWordsSetAfter.add("transcription");
		}
		return removeWordsSetAfter;
	}


	public void setRemoveWordsSetAfter(Set<String> removeWordsSetAfter) {
		this.removeWordsSetAfter = removeWordsSetAfter;
	}


	public boolean isUsingblacklist() {
		return usingblacklist;
	}


	public void setUsingblacklist(boolean usingblacklist) {
		this.usingblacklist = usingblacklist;
	}

	
	

}
