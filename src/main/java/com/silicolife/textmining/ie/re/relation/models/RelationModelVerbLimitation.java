package com.silicolife.textmining.ie.re.relation.models;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.silicolife.textmining.core.datastructures.annotation.re.EventAnnotationImpl;
import com.silicolife.textmining.core.datastructures.annotation.re.EventPropertiesImpl;
import com.silicolife.textmining.core.datastructures.textprocessing.TextProcessor;
import com.silicolife.textmining.core.interfaces.core.annotation.IEntityAnnotation;
import com.silicolife.textmining.core.interfaces.core.annotation.IEventAnnotation;
import com.silicolife.textmining.core.interfaces.core.annotation.re.DirectionallyEnum;
import com.silicolife.textmining.core.interfaces.core.annotation.re.IEventProperties;
import com.silicolife.textmining.core.interfaces.core.annotation.re.PolarityEnum;
import com.silicolife.textmining.core.interfaces.core.dataaccess.exception.ANoteException;
import com.silicolife.textmining.core.interfaces.core.document.IAnnotatedDocument;
import com.silicolife.textmining.core.interfaces.core.document.structure.ISentenceSintaxRepresentation;
import com.silicolife.textmining.core.interfaces.process.IE.re.IRelationModel;
import com.silicolife.textmining.core.interfaces.process.IE.re.clue.IVerbInfo;
import com.silicolife.textmining.ie.re.relation.configuration.IRERelationAdvancedConfiguration;
import com.silicolife.textmining.ie.re.relation.configuration.IRERelationConfiguration;
import com.silicolife.textmining.ie.re.relation.configuration.RERelationNames;
import com.silicolife.textmining.ie.re.relation.models.specialproperties.VerbClassificationInSentenceEnum;
/**
 * @author Hugo Costa
 * 
 *
 */
public class RelationModelVerbLimitation extends RelationModelSimple implements IRelationModel{

	private IRERelationAdvancedConfiguration advancedConfiguration;

	public RelationModelVerbLimitation(IRERelationConfiguration configuration) {
		super(configuration);
		this.advancedConfiguration = configuration.getAdvancedConfiguration();
	}
	
	public Set<String> getRelationTerminations() 
	{
		HashSet<String> terminations= new HashSet<String>(); 
		terminations.add(".");
		terminations.add("!");
		terminations.add(",");
		terminations.add(":");
		terminations.add(";");
		terminations.add("?");
		return terminations;		
	}

	public List<IEventAnnotation> extractSentenceRelation(IAnnotatedDocument document,List<IEntityAnnotation> semanticLayer,ISentenceSintaxRepresentation sentenceSintax) throws ANoteException {
		Map<Long, IEntityAnnotation> treeEntitiesPositions = RelationModelutils.getEntitiesPosition(semanticLayer); // offset->entityID
		// If total of entities are less than 2 there are no relations
		if(treeEntitiesPositions.size()<=1)
			return  new ArrayList<IEventAnnotation>();
		List<IVerbInfo> verbsInfo = sentenceSintax.getListVerbs();
		Map<Long, IVerbInfo> treeverbPositions = getPostagger().getVerbsPosition(verbsInfo); 
		Set<Long> verbsSet = treeverbPositions.keySet();
		List<Long> verbsList = new ArrayList<Long>(verbsSet);
		Set<Long> entitiesSet = treeEntitiesPositions.keySet();
		List<Long> entitiesList = new ArrayList<Long>(entitiesSet);
		List<IEventAnnotation> relations = new ArrayList<IEventAnnotation>();	
		List<IEventAnnotation> candidateEventAnnotations;
		for(int i=0;i<verbsList.size();i++)
		{
			IVerbInfo verb = treeverbPositions.get(verbsList.get(i));
			if(i==0)
			{		
				// Just one verb
				if(i==verbsList.size()-1)
				{
					candidateEventAnnotations = processUniqueVerb(document,treeEntitiesPositions, treeverbPositions, verb);
				}
				// The first Verb
				else
				{
					candidateEventAnnotations = processFirstVerb(document,treeEntitiesPositions, treeverbPositions,verbsList, entitiesSet, entitiesList, i, verb);
					
				}			
			}
			// last verb
			else if(i==verbsList.size()-1)
			{
				candidateEventAnnotations = processLastVerb(document,treeEntitiesPositions, treeverbPositions, verbsList, i,verb);
			}
			// verb in the middle of text
			else
			{
				candidateEventAnnotations = processMiddleVerb(document,treeEntitiesPositions, treeverbPositions, verbsList,entitiesSet, entitiesList, i, verb);

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
		return relations;
	}

	protected List<IEventAnnotation> processMiddleVerb(IAnnotatedDocument document,
			Map<Long, IEntityAnnotation> treeEntitiesPositions,
			Map<Long, IVerbInfo> treeverbPositions, List<Long> verbsList,
			Set<Long> entitiesSet, List<Long> entitiesList, int i,
			IVerbInfo verb) throws ANoteException {
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
		return extractRelation(document,verb,previous,further,potencialEntitiesAtLeft,potencialEntitiesAtRight,treeverbPositions,VerbClassificationInSentenceEnum.MIDDLE);
	}

	protected List<IEventAnnotation> processLastVerb(IAnnotatedDocument document,
			Map<Long, IEntityAnnotation> treeEntitiesPositions,
			Map<Long, IVerbInfo> treeverbPositions, List<Long> verbsList,
			int i, IVerbInfo verb) throws ANoteException {
		IVerbInfo previous = treeverbPositions.get(verbsList.get(i-1));;
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
		return extractRelation(document,verb,previous,further,potencialEntitiesAtLeft,potencialEntitiesAtRight,treeverbPositions,VerbClassificationInSentenceEnum.LAST);
	}

	protected List<IEventAnnotation> processFirstVerb(IAnnotatedDocument document,
			Map<Long, IEntityAnnotation> treeEntitiesPositions,
			Map<Long, IVerbInfo> treeverbPositions, List<Long> verbsList,
			Set<Long> entitiesSet, List<Long> entitiesList, int i,
			IVerbInfo verb) throws ANoteException {
		
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
		return extractRelation(document,verb,previous,further,potencialEntitiesAtLeft,potencialEntitiesAtRight,treeverbPositions,VerbClassificationInSentenceEnum.FIRST);
	}

	protected List<IEventAnnotation> processUniqueVerb(IAnnotatedDocument document,
			Map<Long, IEntityAnnotation> treeEntitiesPositions,
			Map<Long, IVerbInfo> treeverbPositions, IVerbInfo verb)
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
		return extractRelation(document,verb,null,null,potencialEntitiesAtLeft,potencialEntitiesAtRight,treeverbPositions,VerbClassificationInSentenceEnum.UNIQUE);
	}
	
	protected List<IEventAnnotation> extractRelation(IAnnotatedDocument document,IVerbInfo verb,IVerbInfo previous,IVerbInfo further,List<IEntityAnnotation> potencialEntitiesAtLeft,
			List<IEntityAnnotation> potencialEntitiesAtRight,Map<Long,IVerbInfo> treeverb,
			VerbClassificationInSentenceEnum verbClassidfication) throws ANoteException
	{
		List<IEntityAnnotation> leftentities=new ArrayList<IEntityAnnotation>();
		List<IEntityAnnotation> rightentities=new ArrayList<IEntityAnnotation>();
		DirectionallyEnum dir = verb.getDirectionality();
		PolarityEnum polarity = verb.getPolarity();
		if(getAdvancedConfiguration()!=null && getAdvancedConfiguration().isUsingOnlyEntitiesNearestVerb())
		{
			processingEntitiesNearestToVerb(verb,previous,further, leftentities, rightentities,potencialEntitiesAtLeft, potencialEntitiesAtRight,verbClassidfication);
		}
		else if(getAdvancedConfiguration()!=null && getAdvancedConfiguration().isUsingVerbEntitiesDistance() && getAdvancedConfiguration().getVerbEntitieMaxDistance() > 0)
		{
			processindEntitiesWithMaxVerbDistance(document,verb,potencialEntitiesAtLeft,potencialEntitiesAtRight, leftentities, rightentities,getAdvancedConfiguration().getVerbEntitieMaxDistance());
		}
		else
		{
			leftentities = potencialEntitiesAtLeft;
			rightentities = potencialEntitiesAtRight;
		}
		String lemma = verb.getLemma();
		IEventProperties eventProperties = new EventPropertiesImpl();
		eventProperties.setDirectionally(dir);
		eventProperties.setLemma(lemma);
		eventProperties.setPolarity(polarity);	
		IEventAnnotation event = new EventAnnotationImpl(verb.getStartOffset(), verb.getEndOffset(),
				"", leftentities, rightentities, verb.getVerb(),-1,"", eventProperties);
		if(getAdvancedConfiguration().isGroupingSynonyms())
		{
			processSynonyms(event,leftentities,rightentities);
		}
		List<IEventAnnotation> result = new ArrayList<IEventAnnotation>();
		result.add(event);
		return result;
	}
	
	protected void processSynonyms(IEventAnnotation event, List<IEntityAnnotation> leftentities,List<IEntityAnnotation> rightentities)
	{
		SortedSet<IEntityAnnotation> setEntitiesAtLeft = new TreeSet<IEntityAnnotation>();	
		SortedSet<IEntityAnnotation> setEntitiesAtRight = new TreeSet<IEntityAnnotation>();			
		for(IEntityAnnotation entLeft :leftentities)
		{
			if(!setEntitiesAtLeft.contains(entLeft))
			{
				setEntitiesAtLeft.add(entLeft);
			}
		}
		for(IEntityAnnotation entRight:rightentities)
		{
			if(!setEntitiesAtRight.contains(entRight))
			{
				setEntitiesAtRight.add(entRight);
			}
		}
		leftentities = new ArrayList<IEntityAnnotation>(setEntitiesAtLeft);
		rightentities = new ArrayList<IEntityAnnotation>(setEntitiesAtRight);
		event.setEntitiesAtLeft(leftentities);
		event.setEntitiesAtRight(rightentities);
	}
	

	protected void processingEntitiesNearestToVerb(IVerbInfo verb,IVerbInfo previousVerb,IVerbInfo futherVerb,
			List<IEntityAnnotation> leftentities,
			List<IEntityAnnotation> rightentities,
			List<IEntityAnnotation> potentialLeftEntities,
			List<IEntityAnnotation> potentialRightEntities,
			VerbClassificationInSentenceEnum verbClassidfication) {
		
		for(IEntityAnnotation potentialEntLeft:potentialLeftEntities)
		{
			// Entity proximity To verb
			// Verb is unique or the first one no left entities restriction
			if(verbClassidfication == VerbClassificationInSentenceEnum.UNIQUE || verbClassidfication == VerbClassificationInSentenceEnum.FIRST)
			{
				leftentities.add(potentialEntLeft);
			}
			else
			{
				long entityDistancePreviousVerb = potentialEntLeft.getStartOffset() - previousVerb.getEndOffset();
				long entityDistanceThisVerb = verb.getStartOffset() - potentialEntLeft.getEndOffset();
				if(entityDistanceThisVerb <= entityDistancePreviousVerb)
				{
					leftentities.add(potentialEntLeft);
				}	
			}
		}
		for(IEntityAnnotation potentialEntRight:potentialRightEntities)
		{

			// Entity proximity To verb
			// Verb is unique or the last one no left entities restriction
			if(verbClassidfication == VerbClassificationInSentenceEnum.UNIQUE || verbClassidfication == VerbClassificationInSentenceEnum.LAST)
			{
				rightentities.add(potentialEntRight);
			}
			else
			{
				long entityDistanceFutherVerb = futherVerb.getStartOffset() - potentialEntRight.getEndOffset();
				long entityDistanceThisVerb = potentialEntRight.getStartOffset() - verb.getEndOffset();
				if(entityDistanceThisVerb <= entityDistanceFutherVerb)
				{
					rightentities.add(potentialEntRight);
				}
			}
		}
	}

	protected void processindEntitiesWithMaxVerbDistance(IAnnotatedDocument document,IVerbInfo verb,
			List<IEntityAnnotation> potentialLeftentities,
			List<IEntityAnnotation> potentialRightentities,
			List<IEntityAnnotation> leftentities,
			List<IEntityAnnotation> rightentities,
			int verbEntitieMaxWordDistance) throws ANoteException {
		for(IEntityAnnotation potentialEntLEft:potentialLeftentities)
		{
			// Verb Distance Condition in Words
			long endEntity = potentialEntLEft.getEndOffset();
			long startVErb = verb.getStartOffset();
			int words = numberOffWordsBetweenTwoOffsets(document,endEntity,startVErb);
			if(words <=verbEntitieMaxWordDistance)
			{
				leftentities.add(potentialEntLEft);
			}
		}
		for(IEntityAnnotation potentialEntRight:potentialRightentities)
		{
			// Verb Distance Condition in Words
			long startEntity = potentialEntRight.getStartOffset();
			long endVerb = verb.getEndOffset();
			int words = numberOffWordsBetweenTwoOffsets(document,endVerb,startEntity);
			if(words <=verbEntitieMaxWordDistance)
			{
				rightentities.add(potentialEntRight);
			}
		}	
	}
	
	protected int numberOffWordsBetweenTwoOffsets(IAnnotatedDocument document,long offsetStart,long longoffsetEnd) throws ANoteException
	{
//		if(document.getDocumentAnnotationText().length() < longoffsetEnd)
//		{
//			return TextProcessor.getNumberOFwords(document.getDocumentAnnotationText().substring((int)offsetStart));
//		}
//		else
		{
			return TextProcessor.getNumberOFwords(document.getDocumentAnnotationText().substring((int)offsetStart, (int)longoffsetEnd));
		}		
	}
	
	protected boolean hasEntitiesInMiddle(long verbEnd, long endPosition,Set<Long> entitiesSet) {
		for(Long entitiesPosStart:entitiesSet)
		{
			if(verbEnd<entitiesPosStart && entitiesPosStart < endPosition)
				return true;
		}
		return false;
	}
	
	public String getDescription(){
		return "Verb Limitation";
	}
	
	public String getImagePath() {
		return "icons/relation_model_verb_limitation.png";
	}

	public String toString(){
		return "Verb Limitation (M x M)";
	}

	public IRERelationAdvancedConfiguration getAdvancedConfiguration() {
		return advancedConfiguration;
	}
	
	public String getUID() {
		return "Verb Limitation (M x M)";
	}

	
	public Properties getProperties()
	{
		Properties prop = super.getProperties();
		if(getAdvancedConfiguration()!=null)
		{
			if(getAdvancedConfiguration().isUsingOnlyVerbNearestEntities())
			{
				prop.put(RERelationNames.usingOnlyVerbNearestEntities,"true");
			}
			else if(getAdvancedConfiguration().isUsingOnlyEntitiesNearestVerb())
			{
				prop.put(RERelationNames.usingOnlyEntitiesNearestVerb,"true");
			}
			else if(getAdvancedConfiguration().isUsingVerbEntitiesDistance())
			{
				if(getAdvancedConfiguration().getVerbEntitieMaxDistance()>0)
				{
					prop.put(RERelationNames.usingVerbEntitiesDistance,"true");
					prop.put(RERelationNames.verbEntitiesDistance,String.valueOf(getAdvancedConfiguration().getVerbEntitieMaxDistance()));
				}
			}
			if(getAdvancedConfiguration().getRelationsType()==null || getAdvancedConfiguration().getRelationsType().size() == 0)
			{
				prop.put(RERelationNames.relationsTypeSelected,"All");
			}
			else if(getAdvancedConfiguration().getRelationsType()!=null )
			{
				prop.put(RERelationNames.relationsTypeSelected,"Filter");
			}
		}
		return prop;
	}
}
