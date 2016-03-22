package com.silicolife.textmining.ie.re.relation.configuration;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;

import com.silicolife.textmining.core.interfaces.process.IE.IIEProcess;
import com.silicolife.textmining.core.interfaces.process.IE.re.IRelationsType;

public class RERelationAdvancedConfiguration implements IRERelationAdvancedConfiguration{

	private boolean usingVerbEntitiesDistance;
	private int verbEntitieMaxDistance;
	private boolean usingOnlyVerbNearestEntities;
	private boolean usingOnlyEntitiesNearestVerb;
	private SortedSet<IRelationsType> relationsType;
	
	private boolean groupingSynonyms;

	
	// Black List Verbs
	private Set<String> blackListVebs;
	private boolean usingblacklist = true;
	
	// Remove verbs inside of parentities;
	private boolean allowverbswithinparenthesis = false;
	
	// Deep Parsing
	private boolean useDeepParsing = true;
	private boolean entitiesAndVerbsInSameSentence = true; 
	private boolean removeEntitiesThatHasBeforeThatINProposition = true;
	private boolean removeEntitiesThatHasINOfPropositionAfter = true;
	private Set<String> inremove;
	private Set<String> inremoveAfter;
	private Set<String> inremoveWordsAfter;
	private boolean jumpverbwithCCBefore = true;
	private Set<String> ccJump;
	private boolean useManualCurationFromOtherProcess;
	private IIEProcess manualCurationFromOtherProcess;

	
	public RERelationAdvancedConfiguration(boolean usingOnlyVerbNearestEntities,boolean usingOnlyEntitiesNearestVerb,int verbEntitieMaxDistance,boolean groupingSynonyms,
			SortedSet<IRelationsType> relationsType,boolean useManualCurationFromOtherProcess,IIEProcess manualCurationFromOtherProcess)
	{
		this.usingVerbEntitiesDistance = false;
		this.usingOnlyVerbNearestEntities = usingOnlyVerbNearestEntities;
		this.usingOnlyEntitiesNearestVerb = usingOnlyEntitiesNearestVerb;
		this.verbEntitieMaxDistance = verbEntitieMaxDistance;
		this.groupingSynonyms = groupingSynonyms;
		if(this.verbEntitieMaxDistance > 0)
			usingVerbEntitiesDistance = true;
		this.relationsType = relationsType;
		this.blackListVebs = new HashSet<>();
		this.inremove = new HashSet<>();
		this.ccJump = new HashSet<>();
		this.inremoveAfter = new HashSet<String>();
		this.inremoveWordsAfter = new HashSet<String>();
		this.useManualCurationFromOtherProcess = useManualCurationFromOtherProcess;
		this.manualCurationFromOtherProcess = manualCurationFromOtherProcess;
	}
	
	
	@Override
	public boolean usingVerbEntitiesDistance() {
		return usingVerbEntitiesDistance;
	}

	@Override
	public int getVerbEntitieMaxDistance() {
		return verbEntitieMaxDistance;
	}

	@Override
	public boolean usingOnlyVerbNearestEntities() {
		return usingOnlyVerbNearestEntities;
	}

	@Override
	public SortedSet<IRelationsType> getRelationsType() {
		return relationsType;
	}


	@Override
	public boolean usingOnlyEntitiesNearestVerb() {
		return usingOnlyEntitiesNearestVerb;
	}


	@Override
	public Set<String> getBlackListVerbs() {
		if(usingblacklist && blackListVebs.isEmpty())
		{
			blackListVebs.add("maize");
			blackListVebs.add("leaf");
			blackListVebs.add("drought");
			blackListVebs.add("ppGpp");		
			blackListVebs.add("seedlings");		
		}
		return blackListVebs;
	}


	@Override
	public boolean allowverbswithinParenthesis() {
		return allowverbswithinparenthesis;
	}


	@Override
	public boolean groupingSynonyms() {
		return groupingSynonyms;
	}


	@Override
	public boolean useDeepParsing() {
		return useDeepParsing;
	}


	@Override
	public boolean entitiesAndVerbsInTheSameSentacePhase() {
		return entitiesAndVerbsInSameSentence;
	}


	@Override
	public boolean removeEntitiesThatHasINPropositionBefore() {
		return removeEntitiesThatHasBeforeThatINProposition;
	}


	@Override
	public Set<String> getINRemoveList() {
		if(removeEntitiesThatHasBeforeThatINProposition && inremove.isEmpty())
		{
			inremove.add("under");
			inremove.add("in");
			inremove.add("while");
			inremove.add("during");		
			inremove.add("without");		
//			inremove.add("with"); interacts with
//			inremove.add("of");		
		}
		return inremove;
	}


	public boolean jumpverbwithCCBefore() {
		return jumpverbwithCCBefore;
	}


	@Override
	public Set<String> getCCJumpList() {
		if(jumpverbwithCCBefore && ccJump.isEmpty())
		{
			ccJump.add("and");
			ccJump.add("but");		
		}
		return ccJump;
	}


	@Override
	public boolean removeEntitiesThatHasINOfPropositionAfter() {
		return removeEntitiesThatHasINOfPropositionAfter;
	}


	@Override
	public Set<String> getINRemoveListAfter() {
		if(removeEntitiesThatHasINOfPropositionAfter && inremoveAfter.isEmpty())
		{
			inremoveAfter.add("of");	
		}
		return inremoveAfter;
	}


	@Override
	public Set<String> getRemoveListAfter() {
		if(removeEntitiesThatHasINOfPropositionAfter && inremoveWordsAfter.isEmpty())
		{
			inremoveWordsAfter.add("expression");	
			inremoveWordsAfter.add("transcription");
		}
		return inremoveWordsAfter;
	}


	@Override
	public boolean useManualCurationFromOtherProcess() {
		return useManualCurationFromOtherProcess;
	}


	@Override
	public IIEProcess getManualCurationFromOtherProcess() {
		return manualCurationFromOtherProcess;
	}



}
