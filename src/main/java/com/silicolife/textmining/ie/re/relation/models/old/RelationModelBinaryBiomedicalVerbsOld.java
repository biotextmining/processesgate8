package com.silicolife.textmining.ie.re.relation.models.old;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

import com.silicolife.textmining.core.datastructures.utils.GenericPairImpl;
import com.silicolife.textmining.core.interfaces.core.annotation.IEntityAnnotation;
import com.silicolife.textmining.core.interfaces.core.annotation.IEventAnnotation;
import com.silicolife.textmining.core.interfaces.core.dataaccess.exception.ANoteException;
import com.silicolife.textmining.core.interfaces.core.document.IAnnotatedDocument;
import com.silicolife.textmining.core.interfaces.core.document.structure.ISentenceSintaxRepresentation;
import com.silicolife.textmining.core.interfaces.process.IE.re.clue.IVerbInfo;
import com.silicolife.textmining.core.interfaces.resource.lexicalwords.ILexicalWords;
import com.silicolife.textmining.ie.re.relation.configuration.IRERelationAdvancedConfiguration;
import com.silicolife.textmining.ie.re.relation.models.RelationModelutils;
import com.silicolife.textmining.ie.re.relation.models.specialproperties.VerbClassificationInSentenceEnum;
import com.silicolife.wrappergate.IGatePosTagger;

public class RelationModelBinaryBiomedicalVerbsOld extends RelationModelBinaryVerbLimitationOld {

	private ILexicalWords biomedicalVerbs;
	private Set<String> rejectedVerbs;
	private Set<String> aceptedVerbs;
	
	public RelationModelBinaryBiomedicalVerbsOld(IGatePosTagger postagger,ILexicalWords biomedicalVerbs,IRERelationAdvancedConfiguration advancedConfiguration) {
		super(postagger,advancedConfiguration);
		this.biomedicalVerbs = biomedicalVerbs;
		this.rejectedVerbs = new HashSet<String>();
		this.aceptedVerbs = new HashSet<String>();
	}
	
	public List<IEventAnnotation> extractSentenceRelation(IAnnotatedDocument document,List<IEntityAnnotation> semanticLayer, ISentenceSintaxRepresentation sentenceSintax) throws ANoteException{
		Map<Long, IEntityAnnotation> treeEntitiesPositions = RelationModelutils.getEntitiesPosition(semanticLayer); // offset->entityID
		List<IVerbInfo> verbsSubSetInfo = filterVerbs(sentenceSintax.getListVerbs());
		// Remove all verbs that are inside of Parenthesis
		if(!getAdvancedConfiguration().isAllowverbswithinParenthesis())
		{
			verbsSubSetInfo = removeVerbswithinParenthesis(verbsSubSetInfo,document,sentenceSintax);
		}
		Map<Long, IVerbInfo> treeverbSubsetPositions = getPostagger().getVerbsPosition(verbsSubSetInfo); 
		List<IVerbInfo> allverbsList = sentenceSintax.getListVerbs();
		// If Black list is not empty we have to remove elements from list
		if(!getAdvancedConfiguration().getBlackListVerbs().isEmpty())
		{
			allverbsList = removeVerbsInBlackList(allverbsList);
		}
		SortedMap<Long, IVerbInfo> treeAllverbPositions = getPostagger().getVerbsPosition(allverbsList); 
		Set<Long> setAllVerbsPositions = treeAllverbPositions.keySet();
		SortedSet<Long> setAllVerbsPositionsOrder = new TreeSet<>(setAllVerbsPositions);
		List<Long> listAllverbsPosition = new ArrayList<Long>(setAllVerbsPositionsOrder);
		Set<Long> entitiesSet = treeEntitiesPositions.keySet();
		List<Long> entitiesList = new ArrayList<Long>(entitiesSet);
		List<IEventAnnotation> relations = new ArrayList<IEventAnnotation>();	
		for(int i=0;i<listAllverbsPosition.size();i++)
		{
			if(treeverbSubsetPositions.keySet().contains(listAllverbsPosition.get(i)))
			{
				IVerbInfo verb = treeverbSubsetPositions.get(listAllverbsPosition.get(i));
				if(entitiesList.size()>1)
				{
					if(i==0)
					{
						// Just one verb
						if(i==listAllverbsPosition.size()-1)
						{
							long startposition = entitiesList.get(0);
							long endPosition = entitiesList.get(entitiesList.size()-1);
							extractRelation(document,relations,verb,startposition,endPosition,
									treeverbSubsetPositions,entitiesList,treeEntitiesPositions,VerbClassificationInSentenceEnum.UNIQUE);
						}
						// The first One
						else
						{
							long startposition = entitiesList.get(0);
							long endPosition = listAllverbsPosition.get(i+1);
							long verbEnd = treeAllverbPositions.get(listAllverbsPosition.get(i)).getEndOffset();
							int wordsAfter =  numberOffWordsBetweenTwoOffsets(document, verbEnd, endPosition);
							if(wordsAfter <= 2 && !hasEntitiesInMiddle(verbEnd,endPosition,entitiesSet))
							{
								if(listAllverbsPosition.size() > i+2)
									endPosition = listAllverbsPosition.get(i+2);
								else
									endPosition = entitiesList.get(entitiesList.size()-1);
							}
							extractRelation(document,relations,verb,startposition,endPosition,treeverbSubsetPositions,entitiesList,
									treeEntitiesPositions,VerbClassificationInSentenceEnum.FIRST);
						}			
					}
					// last verb
					else if(i==listAllverbsPosition.size()-1)
					{
						long startposition = listAllverbsPosition.get(i-1);
						long endPosition = entitiesList.get(entitiesList.size()-1);
						extractRelation(document,relations,verb,startposition,endPosition,
								treeverbSubsetPositions,entitiesList,treeEntitiesPositions,VerbClassificationInSentenceEnum.LAST);

					}
					// verb in the middle of text
					else
					{
						long startposition = listAllverbsPosition.get(i-1);
						long endPosition = listAllverbsPosition.get(i+1);
						long verbEnd = treeAllverbPositions.get(listAllverbsPosition.get(i)).getEndOffset();
						int wordsAfter =  numberOffWordsBetweenTwoOffsets(document, verbEnd, endPosition);
						if(wordsAfter <= 2 && !hasEntitiesInMiddle(verbEnd,endPosition,entitiesSet))
						{
							if(listAllverbsPosition.size() > i+2)
								endPosition = listAllverbsPosition.get(i+2);
							else
								endPosition = entitiesList.get(entitiesList.size()-1);
						}
						extractRelation(document,relations,verb,startposition,endPosition,treeverbSubsetPositions,entitiesList
								,treeEntitiesPositions,VerbClassificationInSentenceEnum.MIDDLE);

					}
				}	
			}
		}
		return relations;
	}
	

	private List<IVerbInfo> removeVerbswithinParenthesis(List<IVerbInfo> verbsSubSetInfo, IAnnotatedDocument document, ISentenceSintaxRepresentation sentenceSintax) throws ANoteException {
		// Fing parentisis sequence 
		List<GenericPairImpl<Integer, Integer>> parentisesPairs = calculateparentises(document,sentenceSintax);
		// if empty retur  the same list
		if(parentisesPairs.isEmpty())
		{
			return verbsSubSetInfo;
		}
		// otherwise check if some ver are present inside parentisis and won´t include that in list
		List<IVerbInfo> result = new ArrayList<IVerbInfo>();
		boolean insert;
		for(IVerbInfo verb : verbsSubSetInfo)
		{
			insert = true;
			for(GenericPairImpl<Integer, Integer> parentises : parentisesPairs)
			{
				if(parentises.getX()<verb.getStartOffset() &&  verb.getStartOffset()<parentises.getY())
				{
					insert = false;
					break;
				}
			}
			if(insert)
				result.add(verb);
		}
		return result;
	}

	private List<GenericPairImpl<Integer, Integer>> calculateparentises(IAnnotatedDocument document, ISentenceSintaxRepresentation sentenceSintax) throws ANoteException {
		List<GenericPairImpl<Integer, Integer>> result = new ArrayList<GenericPairImpl<Integer,Integer>>();
		List<Integer> startParentities = new ArrayList<Integer>();
		List<Integer> endParentities = new ArrayList<Integer>();
		String sentence = sentenceSintax.getSentence().getText();
		int index = sentence.indexOf("(");
		while (index >= 0) 
		{			
		    index = sentence.indexOf("(", index + 1);
		    startParentities.add(index);
		}
		int indexBefore = sentence.indexOf(")");
		while (indexBefore >= 0) 
		{			
			indexBefore = sentence.indexOf(")", indexBefore + 1);
			endParentities.add(indexBefore);
		}
		for(int i=0;i<startParentities.size() && i<endParentities.size();i++)
		{
			int start = startParentities.get(i)+(int)sentenceSintax.getSentence().getStartOffset();
			int end = endParentities.get(i)+(int)sentenceSintax.getSentence().getEndOffset();
			if(start<end)
				result.add(new GenericPairImpl<Integer, Integer>(start,end));
		}
		return result;
	}

	private List<IVerbInfo> removeVerbsInBlackList(List<IVerbInfo> allverbsList) {
		List<IVerbInfo> result = new ArrayList<IVerbInfo>();
		Set<String> blackList = getAdvancedConfiguration().getBlackListVerbs();
		for(IVerbInfo verb: allverbsList)
		{
			if(!blackList.contains(verb.getVerb()))
			{
				result.add(verb);
			}
		}	
		return result;
	}

	private List<IVerbInfo> filterVerbs(List<IVerbInfo> verbsToFilter) throws ANoteException {
		List<IVerbInfo> result = new ArrayList<IVerbInfo>();
		for(IVerbInfo verbInfo:verbsToFilter)
		{
			String verb = verbInfo.getVerb();
			if(!rejectedVerbs.contains(verb))
			{
				if(aceptedVerbs.contains(verb ) || biomedicalVerbs.getLexicalWords().keySet().contains(verb) || getPartialMatchingVerbs(verb))
				{
					if(!getAdvancedConfiguration().getBlackListVerbs().isEmpty() && getAdvancedConfiguration().getBlackListVerbs().contains(verb))
					{
						rejectedVerbs.add(verb);
					}
					else
					{
						aceptedVerbs.add(verb);
						result.add(verbInfo);
					}
				}
				else
				{
					rejectedVerbs.add(verb);
				}
			}
		}
		return result;
	}

	private boolean getPartialMatchingVerbs(String verbCandidate) throws ANoteException {
		for(String verb:biomedicalVerbs.getLexicalWords().keySet())
		{
			if(verbCandidate.contains(" "+verb) || verbCandidate.contains(verb+" "))
			{
				return true;
			}
		}
		return false;
	}
	
	public ILexicalWords getBiomedicalVerbs() {
		return biomedicalVerbs;
	}

	public Set<String> getRejectedVerbs() {
		return rejectedVerbs;
	}

	public Set<String> getAceptedVerbs() {
		return aceptedVerbs;
	}
	
	public String getDescription(){
		return "Binary Selected Verbs Only";
	}
	
	public String getImagePath() {
		return "icons/relation_model_binary_verb_select_user.png";
	}

	public String toString(){
		return "Binary Selected Verbs Only (1 x 1)*";
	}
	
	public String getUID() {
		return "Binary Selected Verbs Only (1 x 1)*";
	}

}
