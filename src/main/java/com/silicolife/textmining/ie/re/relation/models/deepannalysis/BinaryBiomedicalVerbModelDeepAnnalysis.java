package com.silicolife.textmining.ie.re.relation.models.deepannalysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.silicolife.textmining.core.interfaces.core.annotation.IEntityAnnotation;
import com.silicolife.textmining.core.interfaces.core.document.structure.IParsingToken;
import com.silicolife.textmining.core.interfaces.core.document.structure.ISentenceDeepParsingStructure;
import com.silicolife.textmining.core.interfaces.process.IE.re.clue.IVerbInfo;
import com.silicolife.textmining.ie.re.relation.configuration.IRERelationAdvancedConfiguration;

public class BinaryBiomedicalVerbModelDeepAnnalysis {


	private IRERelationAdvancedConfiguration reAdvancedConfiguration;
	private ISentenceDeepParsingStructure deepParsingStructure;
	private Map<Long, IVerbInfo> treeverbSubsetPositions;
	private Map<Long, IVerbInfo> treeverbPositions;
	private List<IEntityAnnotation> potencialEntitiesAtLeft;
	private List<IEntityAnnotation> potencialEntitiesAtRight;
	private Map<Long, IEntityAnnotation> treeEntitiesPositions;

	public BinaryBiomedicalVerbModelDeepAnnalysis(IRERelationAdvancedConfiguration reAdvancedConfiguration,ISentenceDeepParsingStructure deepParsingStructure,
			Map<Long, IVerbInfo> treeverbPositions,Map<Long, IVerbInfo> treeverbSubsetPositions, Map<Long, IEntityAnnotation> treeEntitiesPositions)
	{
		this.reAdvancedConfiguration = reAdvancedConfiguration;
		this.deepParsingStructure = deepParsingStructure;
		this.treeverbPositions = treeverbPositions;
		this.treeverbSubsetPositions = treeverbSubsetPositions;
		this.treeEntitiesPositions = treeEntitiesPositions;
	}

	public void executeDeepAnnalysisGeneral(IVerbInfo verb,List<IEntityAnnotation> potencialEntitiesAtLeft, List<IEntityAnnotation> potencialEntitiesAtRight)
	{
		this.potencialEntitiesAtLeft = potencialEntitiesAtLeft;
		this.potencialEntitiesAtRight = potencialEntitiesAtRight; 
		// Using deepParsing to filter relation entities
		if(getAdvancedConfiguration().isUseDeepParsing() && deepParsingStructure!=null)
		{
			boolean overlap = false;
			// Point 3
			if(getAdvancedConfiguration().isJumpverbwithCCBefore() && !getAdvancedConfiguration().getCcJumpSet().isEmpty())
			{
				overlap = junpVerbsWithCCListBefore(verb);
			}
			if(!overlap && getAdvancedConfiguration().isEntitiesAndVerbsInTheSameSentencePhase())
			{
				// Calculate Entities In Verb-Sentence-Range
				calculeEntitiesINSentenceRange(verb);
			}
			// Point 1 - remove entities that has IN ( besides of "by" ) before.
			if(getAdvancedConfiguration().isRemoveEntitiesThatHasINPropositionBefore() && !getAdvancedConfiguration().getiNRemoveSet().isEmpty())
			{
				removeEntitiesthathasINProposionBefore();
			}
			// Point 7 - remove entities that has IN before "off"( besides of "by" ) before.
			if(getAdvancedConfiguration().isRemoveEntitiesThatHasINOfPropositionAfter() && !getAdvancedConfiguration().getInRemoveAfterSet().isEmpty())
			{
				removeEntitiesthathasINProposionAfter();
			}
		}
	}

	public void executeDeepAnnalysisForUniqueVerb(IVerbInfo verb, List<IEntityAnnotation> potencialEntitiesAtLeft, List<IEntityAnnotation> potencialEntitiesAtRight)
	{
		executeDeepAnnalysisForFirstVerb(verb,potencialEntitiesAtLeft,potencialEntitiesAtRight);
	}

	public void executeDeepAnnalysisForFirstVerb(IVerbInfo verb,List<IEntityAnnotation> potencialEntitiesAtLeft, List<IEntityAnnotation> potencialEntitiesAtRight)
	{
		this.potencialEntitiesAtLeft = potencialEntitiesAtLeft;
		this.potencialEntitiesAtRight = potencialEntitiesAtRight;
		// Using deepParsing to filter relation entities
		if(getAdvancedConfiguration().isUseDeepParsing() && deepParsingStructure!=null)
		{
			if(getAdvancedConfiguration().isEntitiesAndVerbsInTheSameSentencePhase())
			{
				// Calculate Entities In Verb-Sentence-Range
				calculeEntitiesINSentenceRange(verb);
			}
			// Point 1 - remove entities that has IN ( besides of "by" ) before.
			if(getAdvancedConfiguration().isRemoveEntitiesThatHasINPropositionBefore() && !getAdvancedConfiguration().getiNRemoveSet().isEmpty())
			{
				removeEntitiesthathasINProposionBefore();
			}
			// Point 7 - remove entities that has IN before "off"( besides of "by" ) before.
			if(getAdvancedConfiguration().isRemoveEntitiesThatHasINOfPropositionAfter() && !getAdvancedConfiguration().getInRemoveAfterSet().isEmpty())
			{
				removeEntitiesthathasINProposionAfter();
			}
		}
	}
	
	private void removeEntitiesthathasINProposionAfter() {
		// At right
		List<IEntityAnnotation> potencialEntitiesAtRightDeepFilter = new ArrayList<IEntityAnnotation>();
		for(IEntityAnnotation potencialEntityAtRight:this.potencialEntitiesAtRight)
		{
			IParsingToken token = deepParsingStructure.getParsingNode(potencialEntityAtRight.getStartOffset()-deepParsingStructure.getStartOffset(),
					potencialEntityAtRight.getEndOffset()-deepParsingStructure.getStartOffset());
			if(token!=null)
			{
				IParsingToken afterToken = deepParsingStructure.getNextNode(token);
				if(afterToken!=null)
				{
					if(!(afterToken.getCategory().equals("IN") && getAdvancedConfiguration().getRemoveWordsSetAfter().contains(token.getText().toLowerCase()) && getAdvancedConfiguration().getInRemoveAfterSet().contains(afterToken.getText().toLowerCase())))
					{
						potencialEntitiesAtRightDeepFilter.add(potencialEntityAtRight);
					}
				}
				else
				{
					potencialEntitiesAtRightDeepFilter.add(potencialEntityAtRight);
				}
			}
			else
			{
				potencialEntitiesAtRightDeepFilter.add(potencialEntityAtRight);
			}
		}
		this.potencialEntitiesAtRight = potencialEntitiesAtRightDeepFilter;
		// At Left
		List<IEntityAnnotation> potencialEntitiesAtLeftDeepFilter = new ArrayList<IEntityAnnotation>();
		for(IEntityAnnotation potencialEntityAtLeft:this.potencialEntitiesAtLeft)
		{
			IParsingToken token = deepParsingStructure.getParsingNode(potencialEntityAtLeft.getStartOffset()-deepParsingStructure.getStartOffset(),
					potencialEntityAtLeft.getEndOffset()-deepParsingStructure.getStartOffset());
			if(token!=null)
			{
				IParsingToken afterToken = deepParsingStructure.getNextNode(token);
				if(afterToken!=null)
				{
					if(!(afterToken.getCategory().equals("IN") && getAdvancedConfiguration().getRemoveWordsSetAfter().contains(token.getText().toLowerCase()) && getAdvancedConfiguration().getInRemoveAfterSet().contains(afterToken.getText().toLowerCase())))
					{
						potencialEntitiesAtLeftDeepFilter.add(potencialEntityAtLeft);
					}
				}
				else
				{
					potencialEntitiesAtLeftDeepFilter.add(potencialEntityAtLeft);
				}
			}
			else
			{
				potencialEntitiesAtLeftDeepFilter.add(potencialEntityAtLeft);
			}
		}
		this.potencialEntitiesAtLeft = potencialEntitiesAtLeftDeepFilter;		
	}


	private boolean junpVerbsWithCCListBefore(IVerbInfo verb) {
		IParsingToken token = deepParsingStructure.getParsingNode(verb.getStartOffset()-deepParsingStructure.getStartOffset(), verb.getStartOffset()-deepParsingStructure.getStartOffset());
		IParsingToken previousToken = deepParsingStructure.getPreviousNode(token);
		boolean overlap = false;
		if(previousToken.getCategory().equals("CC") && getAdvancedConfiguration().getCcJumpSet().contains(previousToken.getText().toLowerCase()))
		{
			IVerbInfo previousBiomedicalVerb = calculatePreviousBiomedicalVerb(verb);
			long biomedicalVErbBefore = deepParsingStructure.getStartOffset();
			long verbBeforebiomedicalVErbBefore = deepParsingStructure.getStartOffset();
			if(previousBiomedicalVerb!=null)
			{
				biomedicalVErbBefore = previousBiomedicalVerb.getStartOffset();
			}
			IVerbInfo verbBeforepreviousBiomedicalVerb = calculateverbBeforePreviousBiomedicalVerb(verb, biomedicalVErbBefore);
			if(verbBeforepreviousBiomedicalVerb!=null)
			{
				verbBeforebiomedicalVErbBefore = verbBeforepreviousBiomedicalVerb.getStartOffset();
			}
			overlap = true;
			// Find Candidate Entities 
			List<IEntityAnnotation> newpotencialEntitiesAtLeft = new ArrayList<IEntityAnnotation>();
			for(IEntityAnnotation ent:treeEntitiesPositions.values())
			{
				if(ent.getEndOffset() < biomedicalVErbBefore && ent.getStartOffset() >= verbBeforebiomedicalVErbBefore)
				{
					newpotencialEntitiesAtLeft.add(ent);
				}
			}
			this.potencialEntitiesAtLeft = newpotencialEntitiesAtLeft;
		}
		return overlap;
	}

	private IVerbInfo calculateverbBeforePreviousBiomedicalVerb(IVerbInfo verb,
			long biomedicalVErbBefore) {
		IVerbInfo verbBeforepreviousBiomedicalVerb = null;
		for(IVerbInfo vb:treeverbPositions.values())
		{
			if(vb.getStartOffset()<verb.getStartOffset())
			{
				if(verbBeforepreviousBiomedicalVerb==null && vb.getStartOffset() < biomedicalVErbBefore)
				{
					verbBeforepreviousBiomedicalVerb = vb;
				}
				else
				{
					if(verbBeforepreviousBiomedicalVerb!=null && vb.getStartOffset()>verbBeforepreviousBiomedicalVerb.getStartOffset() &&
							vb.getStartOffset() < biomedicalVErbBefore)
					{
						verbBeforepreviousBiomedicalVerb = vb;
					}
				}
			}
		}
		return verbBeforepreviousBiomedicalVerb;
	}

	private IVerbInfo calculatePreviousBiomedicalVerb(IVerbInfo verb) {
		IVerbInfo previousBiomedicalVerb = null;
		for(IVerbInfo verbBio:treeverbSubsetPositions.values())
		{
			if(verbBio.getStartOffset()<verb.getStartOffset())
			{
				if(previousBiomedicalVerb==null)
				{
					previousBiomedicalVerb = verbBio;
				}
				else
				{
					if(verbBio.getStartOffset()>previousBiomedicalVerb.getStartOffset())
					{
						previousBiomedicalVerb = verbBio;
					}
				}
			}
		}
		return previousBiomedicalVerb;
	}



	private void removeEntitiesthathasINProposionBefore() {
		// At right
		List<IEntityAnnotation> potencialEntitiesAtRightDeepFilter = new ArrayList<IEntityAnnotation>();
		for(IEntityAnnotation potencialEntityAtRight:this.potencialEntitiesAtRight)
		{
			IParsingToken token = deepParsingStructure.getParsingNode(potencialEntityAtRight.getStartOffset()-deepParsingStructure.getStartOffset(),
					potencialEntityAtRight.getEndOffset()-deepParsingStructure.getStartOffset());
			if(token!=null)
			{
				IParsingToken previousToken = deepParsingStructure.getPreviousNode(token);
				if(previousToken!=null)
				{
					if(!previousToken.getCategory().equals("IN") || !getAdvancedConfiguration().getiNRemoveSet().contains(previousToken.getText().toLowerCase()))
					{
						potencialEntitiesAtRightDeepFilter.add(potencialEntityAtRight);
					}
				}
				else
				{
					potencialEntitiesAtRightDeepFilter.add(potencialEntityAtRight);
				}
			}
			else
			{
				potencialEntitiesAtRightDeepFilter.add(potencialEntityAtRight);
			}
		}
		this.potencialEntitiesAtRight = potencialEntitiesAtRightDeepFilter;
		// At Left
		List<IEntityAnnotation> potencialEntitiesAtLeftDeepFilter = new ArrayList<IEntityAnnotation>();
		for(IEntityAnnotation potencialEntityAtLeft:this.potencialEntitiesAtLeft)
		{
			IParsingToken token = deepParsingStructure.getParsingNode(potencialEntityAtLeft.getStartOffset()-deepParsingStructure.getStartOffset(),
					potencialEntityAtLeft.getEndOffset()-deepParsingStructure.getStartOffset());
			if(token!=null)
			{
				IParsingToken previousToken = deepParsingStructure.getPreviousNode(token);
				if(previousToken!=null)
				{
					if(!previousToken.getCategory().equals("IN") || !getAdvancedConfiguration().getiNRemoveSet().contains(previousToken.getText().toLowerCase()))
					{
						potencialEntitiesAtLeftDeepFilter.add(potencialEntityAtLeft);
					}
				}
				else
				{
					potencialEntitiesAtLeftDeepFilter.add(potencialEntityAtLeft);
				}
			}
			else
			{
				potencialEntitiesAtLeftDeepFilter.add(potencialEntityAtLeft);
			}
		}
		this.potencialEntitiesAtLeft = potencialEntitiesAtLeftDeepFilter;
	}

	private void calculeEntitiesINSentenceRange(IVerbInfo verb) {
		List<IEntityAnnotation> potencialEntitiesAtLeftDeepFilter = new ArrayList<IEntityAnnotation>();
		List<IEntityAnnotation> potencialEntitiesAtRightDeepFilter = new ArrayList<IEntityAnnotation>();
		for(IEntityAnnotation potencialEntityAtLeft:this.potencialEntitiesAtLeft)
		{
			IParsingToken token = deepParsingStructure.getParsingNode(potencialEntityAtLeft.getStartOffset()-deepParsingStructure.getStartOffset(),
					potencialEntityAtLeft.getEndOffset()-deepParsingStructure.getStartOffset());
			if(token==null ||token.getCategory().equals(".") || insideOFVerbSentence(verb,potencialEntityAtLeft,deepParsingStructure))
			{							
				potencialEntitiesAtLeftDeepFilter.add(potencialEntityAtLeft);
			}
		}
		for(IEntityAnnotation potencialEntityAtRight:this.potencialEntitiesAtRight)
		{
			IParsingToken token = deepParsingStructure.getParsingNode(potencialEntityAtRight.getStartOffset()-deepParsingStructure.getStartOffset(),
					potencialEntityAtRight.getEndOffset()-deepParsingStructure.getStartOffset());
			if(token==null ||token.getCategory().equals(".") ||insideOFVerbSentence(verb,potencialEntityAtRight,deepParsingStructure))
			{
				potencialEntitiesAtRightDeepFilter.add(potencialEntityAtRight);
			}
		}
		this.potencialEntitiesAtLeft = potencialEntitiesAtLeftDeepFilter;
		this.potencialEntitiesAtRight = potencialEntitiesAtRightDeepFilter;
	}


	private boolean insideOFVerbSentence(IVerbInfo verb, IEntityAnnotation ent,ISentenceDeepParsingStructure deepParsingStructure) {
		if(deepParsingStructure==null)
			return true;
		else if(deepParsingStructure.getSNodes().size() == 0)
			return true;
		else
		{
			List<IParsingToken> sentencesPhases = deepParsingStructure.getSNodes();
			List<IParsingToken> sentencesPhasesForVerb = new ArrayList<IParsingToken>();
			for(IParsingToken parsing : sentencesPhases)
			{
				if(parsing.getStartOffset()+deepParsingStructure.getStartOffset()<=verb.getStartOffset() && verb.getEndOffset()<=parsing.getEndOffset()+deepParsingStructure.getStartOffset())
				{
					sentencesPhasesForVerb.add(parsing);
				}
			}
			if(sentencesPhasesForVerb.isEmpty())
				return true;
			IParsingToken lower = sentencesPhasesForVerb.get(0);
			for(int i=1;i<sentencesPhasesForVerb.size();i++)
			{
				IParsingToken candidate = sentencesPhasesForVerb.get(i);
				if(candidate.getStartOffset()>=lower.getStartOffset() && candidate.getEndOffset()<=lower.getEndOffset())
				{
					lower = candidate;
				}
			}
			if(ent.getStartOffset()>=lower.getStartOffset()+deepParsingStructure.getStartOffset() && ent.getEndOffset()<=lower.getEndOffset()+deepParsingStructure.getStartOffset())
			{
				return true;
			}
			else
			{
				return false;
			}
		}
	}

	public IRERelationAdvancedConfiguration getAdvancedConfiguration() {
		return reAdvancedConfiguration;
	}

	public List<IEntityAnnotation> getEntitiesAtLeft() {
		return potencialEntitiesAtLeft;
	}

	public List<IEntityAnnotation> getEntitiesAtRight() {
		return potencialEntitiesAtRight;
	}

	public ISentenceDeepParsingStructure getSentenceDeepParsingStructure() {
		return deepParsingStructure;
	}

}
