package com.silicolife.textmining.ie.re.relation.models;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

import com.silicolife.textmining.core.datastructures.documents.structure.SentenceDeepParsingStructureImpl;
import com.silicolife.textmining.core.datastructures.utils.GenericPairImpl;
import com.silicolife.textmining.core.interfaces.core.annotation.IEntityAnnotation;
import com.silicolife.textmining.core.interfaces.core.annotation.IEventAnnotation;
import com.silicolife.textmining.core.interfaces.core.dataaccess.exception.ANoteException;
import com.silicolife.textmining.core.interfaces.core.document.IAnnotatedDocument;
import com.silicolife.textmining.core.interfaces.core.document.structure.ISentence;
import com.silicolife.textmining.core.interfaces.core.document.structure.ISentenceDeepParsingStructure;
import com.silicolife.textmining.core.interfaces.core.document.structure.ISentenceSintaxRepresentation;
import com.silicolife.textmining.core.interfaces.process.IE.re.clue.IVerbInfo;
import com.silicolife.textmining.core.interfaces.resource.lexicalwords.ILexicalWords;
import com.silicolife.textmining.ie.re.relation.configuration.IRERelationConfiguration;
import com.silicolife.textmining.ie.re.relation.models.deepannalysis.BinaryBiomedicalVerbModelDeepAnnalysis;
import com.silicolife.textmining.ie.re.relation.models.specialproperties.VerbClassificationInSentenceEnum;
import com.silicolife.textmining.processes.nlptools.structure.SimpliedSentenceSyntaxTree;

public class RelationModelBinaryBiomedicalVerbs extends RelationModelBinaryVerbLimitation {

	private ILexicalWords biomedicalVerbs;
	private Set<String> rejectedVerbs;
	private Set<String> aceptedVerbs;
	
	public RelationModelBinaryBiomedicalVerbs(IRERelationConfiguration configuration) {
		super(configuration);
		this.biomedicalVerbs = configuration.getVerbsClues();
		this.rejectedVerbs = new HashSet<String>();
		this.aceptedVerbs = new HashSet<String>();
	}
	
	public List<IEventAnnotation> extractSentenceRelation(IAnnotatedDocument document,List<IEntityAnnotation> semanticLayer, ISentenceSintaxRepresentation sentenceSintax) throws ANoteException{
		Map<Long, IEntityAnnotation> treeEntitiesPositions = RelationModelutils.getEntitiesPosition(semanticLayer); // offset->entityID
		if(treeEntitiesPositions.size()<=1)
			return  new ArrayList<IEventAnnotation>();
		List<IVerbInfo> verbsSubSetInfo = filterVerbs(sentenceSintax.getListVerbs());
		// Remove all verbs that are inside of Parenthesis
		if(!getAdvancedConfiguration().allowverbswithinParenthesis())
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
		SortedMap<Long, IVerbInfo> treeverbPositions = getPostagger().getVerbsPosition(allverbsList); 
		Set<Long> verbsList = treeverbPositions.keySet();
		SortedSet<Long> setAllVerbsPositionsOrder = new TreeSet<>(verbsList);
		List<Long> listAllverbsPosition = new ArrayList<Long>(setAllVerbsPositionsOrder);
		Set<Long> entitiesSet = treeEntitiesPositions.keySet();
		List<Long> entitiesList = new ArrayList<Long>(entitiesSet);
		List<IEventAnnotation> relations = new ArrayList<IEventAnnotation>();
		List<IEventAnnotation> candidateEventAnnotations = new ArrayList<IEventAnnotation>();	
		ISentenceDeepParsingStructure deepParsingStructure =null;
		BinaryBiomedicalVerbModelDeepAnnalysis deepAnnalysis = null;
		if(getAdvancedConfiguration().useDeepParsing())
		{
			try {
				// Get Sentence
				ISentence sentence = sentenceSintax.getSentence();
				List<IEventAnnotation> events = new ArrayList<IEventAnnotation>();
				// Get Deep Parsing with Simplification for Entities
				sentence =  SimpliedSentenceSyntaxTree.simplySentence(sentence,semanticLayer,events,false,false);
				// Create Deep Parsing structure
				deepParsingStructure = new SentenceDeepParsingStructureImpl(sentenceSintax.getSentence());
				// Create Deep Annalysis
				deepAnnalysis = new BinaryBiomedicalVerbModelDeepAnnalysis(getAdvancedConfiguration(),deepParsingStructure,treeverbPositions,treeverbSubsetPositions,treeEntitiesPositions);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		for(int i=0;i<listAllverbsPosition.size();i++)
		{
			if(treeverbSubsetPositions.keySet().contains(listAllverbsPosition.get(i)))
			{
				IVerbInfo verb = treeverbSubsetPositions.get(listAllverbsPosition.get(i));
				if(i==0)
				{		
					// Just one verb
					if(i==verbsList.size()-1)
					{
						candidateEventAnnotations = processUniqueVerb(document,treeEntitiesPositions, treeverbPositions, verb,deepAnnalysis);
					}
					// The first Verb
					else
					{
						candidateEventAnnotations = processFirstVerb(document,treeEntitiesPositions, treeverbPositions,listAllverbsPosition, entitiesSet, entitiesList, i, verb,deepAnnalysis);
					}			
				}
				// last verb
				else if(i==verbsList.size()-1)
				{
					candidateEventAnnotations = processLastVerb(document,treeEntitiesPositions, treeverbPositions, listAllverbsPosition, i,verb,deepAnnalysis);
				}
				// verb in the middle of text
				else
				{
					candidateEventAnnotations = processMiddleVerb(document,treeEntitiesPositions, treeverbPositions, listAllverbsPosition,entitiesSet, entitiesList, i, verb,deepAnnalysis);

				}
				for(IEventAnnotation candidateEvent:candidateEventAnnotations)
				{
					if(candidateEvent.getEntitiesAtLeft().size()+candidateEvent.getEntitiesAtRight().size()>1)
					{
						// Remove binary relation with the same entity in right and left
						if(!(candidateEvent.getEntitiesAtLeft().size()==1 && candidateEvent.getEntitiesAtRight().size() == 1 && candidateEvent.getEntitiesAtLeft().get(0).equals(candidateEvent.getEntitiesAtRight().get(0))))
						{
							relations.add(candidateEvent);	
						}
					}
				}
			}
		}
		return relations;
	}
	
	protected List<IEventAnnotation> processMiddleVerb(IAnnotatedDocument document,
			Map<Long, IEntityAnnotation> treeEntitiesPositions,
			Map<Long, IVerbInfo> treeverbPositions, List<Long> verbsList,
			Set<Long> entitiesSet, List<Long> entitiesList, int i,
			IVerbInfo verb,BinaryBiomedicalVerbModelDeepAnnalysis deepAnnalysis) throws ANoteException {
		IVerbInfo previous = treeverbPositions.get(verbsList.get(i-1));;
		IVerbInfo further = treeverbPositions.get(verbsList.get(i+1));;	
		List<IEntityAnnotation> potencialEntitiesAtLeft = new ArrayList<IEntityAnnotation>();
		List<IEntityAnnotation> potencialEntitiesAtRight = new ArrayList<IEntityAnnotation>();
		long endPosition = further.getStartOffset();
		long startpostion = previous.getEndOffset();
		int wordsAfter =  numberOffWordsBetweenTwoOffsets(document, verb.getEndOffset(), further.getStartOffset());
		if(wordsAfter <= 2 && !hasEntitiesInMiddle(verb.getEndOffset(),further.getStartOffset(),entitiesSet))
		{
			if(verbsList.size() > i+2)
				endPosition = verbsList.get(i+2);
			else
				endPosition = treeEntitiesPositions.get(entitiesList.get(entitiesList.size()-1)).getEndOffset()+1;
		}
		// Find Candidate Entities
		for(IEntityAnnotation ent:treeEntitiesPositions.values())
		{
			if(ent.getEndOffset()<verb.getStartOffset() && ent.getStartOffset() > startpostion)
			{
				potencialEntitiesAtLeft.add(ent);
			}
			else if(ent.getStartOffset() > verb.getEndOffset() && ent.getStartOffset()<=endPosition)
			{
				potencialEntitiesAtRight.add(ent);
			}
		}
		if(getAdvancedConfiguration().useDeepParsing() && deepAnnalysis!=null && deepAnnalysis.getSentenceDeepParsingStructure()!=null)
		{
			deepAnnalysis.executeDeepAnnalysisGeneral(verb,potencialEntitiesAtLeft,potencialEntitiesAtRight);
			potencialEntitiesAtLeft = deepAnnalysis.getEntitiesAtLeft();
			potencialEntitiesAtRight = deepAnnalysis.getEntitiesAtRight();
		}
		return extractRelation(document,verb,previous,further,potencialEntitiesAtLeft,potencialEntitiesAtRight,treeverbPositions,VerbClassificationInSentenceEnum.MIDDLE);
	}

	protected List<IEventAnnotation> processLastVerb(IAnnotatedDocument document,
			Map<Long, IEntityAnnotation> treeEntitiesPositions,
			Map<Long, IVerbInfo> treeverbPositions, List<Long> verbsList,
			int i, IVerbInfo verb,BinaryBiomedicalVerbModelDeepAnnalysis deepAnnalysis) throws ANoteException {
		IVerbInfo previous = treeverbPositions.get(verbsList.get(i-1));
		IVerbInfo further = null;
		long startposition = previous.getEndOffset();
		List<IEntityAnnotation> potencialEntitiesAtLeft = new ArrayList<IEntityAnnotation>();
		List<IEntityAnnotation> potencialEntitiesAtRight = new ArrayList<IEntityAnnotation>();
		// Find Candidate Entities
		for(IEntityAnnotation ent:treeEntitiesPositions.values())
		{
			if(ent.getEndOffset()<verb.getStartOffset() && ent.getStartOffset() > startposition)
			{
				potencialEntitiesAtLeft.add(ent);
			}
			else if(ent.getStartOffset() > verb.getEndOffset())
			{
				potencialEntitiesAtRight.add(ent);
			}
		}
		if(getAdvancedConfiguration().useDeepParsing() && deepAnnalysis!=null && deepAnnalysis.getSentenceDeepParsingStructure()!=null)
		{
			deepAnnalysis.executeDeepAnnalysisGeneral(verb,potencialEntitiesAtLeft,potencialEntitiesAtRight);
			potencialEntitiesAtLeft = deepAnnalysis.getEntitiesAtLeft();
			potencialEntitiesAtRight = deepAnnalysis.getEntitiesAtRight();
		}
		return extractRelation(document,verb,previous,further,potencialEntitiesAtLeft,potencialEntitiesAtRight,treeverbPositions,VerbClassificationInSentenceEnum.LAST);
	}

	protected List<IEventAnnotation> processFirstVerb(IAnnotatedDocument document,
			Map<Long, IEntityAnnotation> treeEntitiesPositions,
			Map<Long, IVerbInfo> treeverbPositions, List<Long> verbsList,
			Set<Long> entitiesSet, List<Long> entitiesList, int i,
			IVerbInfo verb, BinaryBiomedicalVerbModelDeepAnnalysis deepAnnalysis) throws ANoteException {
		
		IVerbInfo previous = null;
		IVerbInfo further = treeverbPositions.get(verbsList.get(i+1));	
		long endPosition =  further.getStartOffset();
		int wordsAfter =  numberOffWordsBetweenTwoOffsets(document, verb.getEndOffset(), further.getStartOffset());
		if(wordsAfter <= 2 && !hasEntitiesInMiddle(verb.getEndOffset(),further.getStartOffset(),entitiesSet))
		{
			if(verbsList.size() > i+2)
				endPosition = verbsList.get(i+2);
			else
			{
				endPosition = treeEntitiesPositions.get(entitiesList.get(entitiesList.size()-1)).getEndOffset()+1;
			}
		}
		List<IEntityAnnotation> potencialEntitiesAtLeft = new ArrayList<IEntityAnnotation>();
		List<IEntityAnnotation> potencialEntitiesAtRight = new ArrayList<IEntityAnnotation>();
		// Find Candidate Entities
		for(IEntityAnnotation ent:treeEntitiesPositions.values())
		{
			if(ent.getEndOffset()<verb.getStartOffset())
			{
				potencialEntitiesAtLeft.add(ent);
			}
			else if(ent.getStartOffset() > verb.getEndOffset() && ent.getStartOffset()<=endPosition)
			{
				potencialEntitiesAtRight.add(ent);
			}
		}
		if(getAdvancedConfiguration().useDeepParsing() && deepAnnalysis!=null && deepAnnalysis.getSentenceDeepParsingStructure()!=null)
		{
			deepAnnalysis.executeDeepAnnalysisForFirstVerb(verb,potencialEntitiesAtLeft,potencialEntitiesAtRight);
			potencialEntitiesAtLeft = deepAnnalysis.getEntitiesAtLeft();
			potencialEntitiesAtRight = deepAnnalysis.getEntitiesAtRight();
		}
		return extractRelation(document,verb,previous,further,potencialEntitiesAtLeft,potencialEntitiesAtRight,treeverbPositions,VerbClassificationInSentenceEnum.FIRST);
	}

	protected List<IEventAnnotation> processUniqueVerb(IAnnotatedDocument document,
			Map<Long, IEntityAnnotation> treeEntitiesPositions,
			Map<Long, IVerbInfo> treeverbPositions, IVerbInfo verb, BinaryBiomedicalVerbModelDeepAnnalysis deepAnnalysis)
			throws ANoteException {
		List<IEntityAnnotation> potencialEntitiesAtLeft = new ArrayList<IEntityAnnotation>();
		List<IEntityAnnotation> potencialEntitiesAtRight = new ArrayList<IEntityAnnotation>();
		// Find Candidate Entities
		for(IEntityAnnotation ent:treeEntitiesPositions.values())
		{
			if(ent.getEndOffset()<verb.getStartOffset())
			{
				potencialEntitiesAtLeft.add(ent);
			}
			else if(ent.getStartOffset() > verb.getEndOffset())
			{
				potencialEntitiesAtRight.add(ent);
			}
		}
		if(getAdvancedConfiguration().useDeepParsing() && deepAnnalysis!=null && deepAnnalysis.getSentenceDeepParsingStructure()!=null)
		{
			deepAnnalysis.executeDeepAnnalysisForUniqueVerb(verb,potencialEntitiesAtLeft,potencialEntitiesAtRight);
			potencialEntitiesAtLeft = deepAnnalysis.getEntitiesAtLeft();
			potencialEntitiesAtRight = deepAnnalysis.getEntitiesAtRight();
		}
		return extractRelation(document,verb,null,null,potencialEntitiesAtLeft,potencialEntitiesAtRight,treeverbPositions,VerbClassificationInSentenceEnum.UNIQUE);
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
		    startParentities.add(index);
		    index = sentence.indexOf("(", index + 1);
		}
		int indexBefore = sentence.indexOf(")");
		while (indexBefore >= 0) 
		{		
			endParentities.add(indexBefore);
			indexBefore = sentence.indexOf(")", indexBefore + 1);
		}
		for(int i=0;i<startParentities.size() && i<endParentities.size();i++)
		{
			int start = startParentities.get(i)+(int)sentenceSintax.getSentence().getStartOffset();
			int end = endParentities.get(i)+(int)sentenceSintax.getSentence().getStartOffset();
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
