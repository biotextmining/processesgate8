package com.silicolife.textmining.ie.re.relation.models.old;

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
import com.silicolife.textmining.ie.re.relation.configuration.RERelationNames;
import com.silicolife.textmining.ie.re.relation.models.RelationModelutils;
import com.silicolife.textmining.ie.re.relation.models.specialproperties.VerbClassificationInSentenceEnum;
import com.silicolife.wrappergate.IGatePosTagger;
/**
 * @author Hugo Costa
 * 
 *
 */
public class RelationModelVerbLimitationOld extends RelationModelSimpleOld implements IRelationModel{

	private IRERelationAdvancedConfiguration advancedConfiguration;

	public RelationModelVerbLimitationOld(IGatePosTagger postagger,IRERelationAdvancedConfiguration advancedConfiguration) {
		super(postagger);
		this.advancedConfiguration = advancedConfiguration;
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
		List<IVerbInfo> verbsInfo = sentenceSintax.getListVerbs();
		Map<Long, IVerbInfo> treeverbPositions = getPostagger().getVerbsPosition(verbsInfo); 
		Set<Long> verbsSet = treeverbPositions.keySet();
		List<Long> verbsList = new ArrayList<Long>(verbsSet);
		Set<Long> entitiesSet = treeEntitiesPositions.keySet();
		List<Long> entitiesList = new ArrayList<Long>(entitiesSet);
		List<IEventAnnotation> relations = new ArrayList<IEventAnnotation>();	
		IEventAnnotation eventAnnotation;
		for(int i=0;i<verbsList.size();i++)
		{
			if(entitiesList.size()>1)
			{
				IVerbInfo verb = treeverbPositions.get(verbsList.get(i));
				if(i==0)
				{
					// Just one verb
					if(i==verbsList.size()-1)
					{
						long startposition = entitiesList.get(0);
						long endPosition = treeEntitiesPositions.get(entitiesList.get(entitiesList.size()-1)).getEndOffset()+1;
						eventAnnotation = extractRelation(document,verb,startposition,endPosition,
								treeverbPositions,entitiesList,treeEntitiesPositions,VerbClassificationInSentenceEnum.UNIQUE);
					}
					// The first One
					else
					{
						long startposition = entitiesList.get(0);
						long endPosition = verbsList.get(i+1);
						long verbEnd = treeverbPositions.get(verbsList.get(i)).getEndOffset();
						int wordsAfter =  numberOffWordsBetweenTwoOffsets(document, verbEnd, endPosition);
						if(wordsAfter <= 2 && !hasEntitiesInMiddle(verbEnd,endPosition,entitiesSet))
						{
							if(verbsList.size() > i+2)
								endPosition = verbsList.get(i+2);
							else
								endPosition = treeEntitiesPositions.get(entitiesList.get(entitiesList.size()-1)).getEndOffset()+1;
						}
						eventAnnotation = extractRelation(document,verb,startposition,endPosition,treeverbPositions,entitiesList,
								treeEntitiesPositions,VerbClassificationInSentenceEnum.FIRST);
					}			
				}
				// last verb
				else if(i==verbsList.size()-1)
				{
					long startposition = treeverbPositions.get(verbsList.get(i-1)).getEndOffset();
					long endPosition = treeEntitiesPositions.get(entitiesList.get(entitiesList.size()-1)).getEndOffset()+1;
					eventAnnotation = extractRelation(document,verb,startposition,endPosition,
							treeverbPositions,entitiesList,treeEntitiesPositions,VerbClassificationInSentenceEnum.LAST);

				}
				// verb in the middle of text
				else
				{
					long startposition = treeverbPositions.get(verbsList.get(i-1)).getEndOffset();
					long endPosition = verbsList.get(i+1);
					long verbEnd = treeverbPositions.get(verbsList.get(i)).getEndOffset();
					int wordsAfter =  numberOffWordsBetweenTwoOffsets(document, verbEnd, endPosition);
					if(wordsAfter <= 2 && !hasEntitiesInMiddle(verbEnd,endPosition,entitiesSet))
					{
						if(verbsList.size() > i+2)
							endPosition = verbsList.get(i+2);
						else
							endPosition = treeEntitiesPositions.get(entitiesList.get(entitiesList.size()-1)).getEndOffset()+1;
					}
					eventAnnotation = extractRelation(document,verb,startposition,endPosition,treeverbPositions,entitiesList
							,treeEntitiesPositions,VerbClassificationInSentenceEnum.MIDDLE);

				}
				if(eventAnnotation.getEntitiesAtLeft().size()+eventAnnotation.getEntitiesAtRight().size()>1)
				{
					// Remove binary relation with the same entity in right and left
					if(!(eventAnnotation.getEntitiesAtLeft().size()==1 && eventAnnotation.getEntitiesAtRight().size() == 1 && eventAnnotation.getEntitiesAtLeft().get(0).equals(eventAnnotation.getEntitiesAtRight().get(0))))
					{
						relations.add(eventAnnotation);	
					}
				}
			}		
		}
		return relations;
	}
	
	private IEventAnnotation extractRelation(IAnnotatedDocument document,IVerbInfo verb,Long startPosition,Long endPosition, Map<Long,IVerbInfo> treeverb,List<Long> keySetEnt,
			Map<Long, IEntityAnnotation> treeEntitiesPositions,VerbClassificationInSentenceEnum verbClassidfication) throws ANoteException
	{
		List<IEntityAnnotation> leftentities=new ArrayList<IEntityAnnotation>();
		List<IEntityAnnotation> rightentities=new ArrayList<IEntityAnnotation>();
		DirectionallyEnum dir = verb.getDirectionality();
		PolarityEnum polarity = verb.getPolarity();
		SortedSet<IEntityAnnotation> setOfEntitiesAnnotations = new TreeSet<IEntityAnnotation>();
		SortedSet<IEntityAnnotation> setOfRightAnnotations = new TreeSet<IEntityAnnotation>();
		if(getAdvancedConfiguration()!=null && getAdvancedConfiguration().isUsingOnlyEntitiesNearestVerb())
		{
			processingEntitiesNearestToVerb(verb, startPosition, endPosition, keySetEnt,treeEntitiesPositions, leftentities, rightentities,setOfEntitiesAnnotations, setOfRightAnnotations,verbClassidfication);
		}
		else if(getAdvancedConfiguration()!=null && getAdvancedConfiguration().isUsingVerbEntitiesDistance() && getAdvancedConfiguration().getVerbEntitieMaxDistance() > 0)
		{
			processindEntitiesWithMaxVerbDistance(document,verb, startPosition, endPosition, keySetEnt,treeEntitiesPositions, leftentities, rightentities,setOfEntitiesAnnotations, setOfRightAnnotations,getAdvancedConfiguration().getVerbEntitieMaxDistance());
		}
		else
		{
			processindEntitiesWithoutRestrictions(verb, startPosition, endPosition, keySetEnt,treeEntitiesPositions, leftentities, rightentities,setOfEntitiesAnnotations, setOfRightAnnotations);
		}
		String lemma = verb.getLemma();
		IEventProperties eventProperties = new EventPropertiesImpl();
		eventProperties.setDirectionally(dir);
		eventProperties.setLemma(lemma);
		eventProperties.setPolarity(polarity);	
		IEventAnnotation event = new EventAnnotationImpl(verb.getStartOffset(), verb.getEndOffset(),
				"", leftentities, rightentities, verb.getVerb(),eventProperties,false);
		return event;
	}
	

	protected void processingEntitiesNearestToVerb(IVerbInfo verb, Long startPosition, Long endPosition,
			List<Long> keySetEnt,
			Map<Long, IEntityAnnotation> treeEntitiesPositions,
			List<IEntityAnnotation> leftentities,
			List<IEntityAnnotation> rightentities,
			SortedSet<IEntityAnnotation> setOfEntitiesAnnotations,
			SortedSet<IEntityAnnotation> setOfRightAnnotations,
			VerbClassificationInSentenceEnum verbClassidfication) {
		for(int i=0;i<keySetEnt.size();i++)
		{
			
			if(keySetEnt.get(i)>=startPosition&&keySetEnt.get(i)<=endPosition)
			{
				// left Entities
				if(treeEntitiesPositions.get(keySetEnt.get(i)).getEndOffset()<verb.getStartOffset())
				{
					// Entity proximity To verb
					// Verb is unique or the first one no left entities restriction
					if(verbClassidfication == VerbClassificationInSentenceEnum.UNIQUE || verbClassidfication == VerbClassificationInSentenceEnum.FIRST)
					{
						if(!setOfEntitiesAnnotations.contains(treeEntitiesPositions.get(keySetEnt.get(i))))
						{
							leftentities.add(treeEntitiesPositions.get(keySetEnt.get(i)));
							setOfEntitiesAnnotations.add(treeEntitiesPositions.get(keySetEnt.get(i)));
						}
					}
					else
					{
						long entityDistancePreviousVerb = treeEntitiesPositions.get(keySetEnt.get(i)).getStartOffset() - startPosition;
						long entityDistanceThisVerb = verb.getStartOffset() - treeEntitiesPositions.get(keySetEnt.get(i)).getEndOffset();
						if(entityDistanceThisVerb <= entityDistancePreviousVerb)
						{
							if(!setOfEntitiesAnnotations.contains(treeEntitiesPositions.get(keySetEnt.get(i))))
							{
								leftentities.add(treeEntitiesPositions.get(keySetEnt.get(i)));
								setOfEntitiesAnnotations.add(treeEntitiesPositions.get(keySetEnt.get(i)));
							}
						}	
					}
				}
				// right Entities
				else if(treeEntitiesPositions.get(keySetEnt.get(i)).getStartOffset() > verb.getStartOffset() + verb.getVerb().length())
				{
					// Entity proximity To verb
					// Verb is unique or the last one no left entities restriction
					if(verbClassidfication == VerbClassificationInSentenceEnum.UNIQUE || verbClassidfication == VerbClassificationInSentenceEnum.LAST)
					{
						if(!setOfRightAnnotations.contains(treeEntitiesPositions.get(keySetEnt.get(i))))
						{
							rightentities.add(treeEntitiesPositions.get(keySetEnt.get(i)));
							setOfRightAnnotations.add(treeEntitiesPositions.get(keySetEnt.get(i)));
						}
					}
					else
					{
						long entityDistanceFutherVerb = endPosition - treeEntitiesPositions.get(keySetEnt.get(i)).getEndOffset();
						long entityDistanceThisVerb = treeEntitiesPositions.get(keySetEnt.get(i)).getStartOffset() - verb.getStartOffset()-verb.getVerb().length();
						if(entityDistanceThisVerb <= entityDistanceFutherVerb)
						{
							if(!setOfRightAnnotations.contains(treeEntitiesPositions.get(keySetEnt.get(i))))
							{
								rightentities.add(treeEntitiesPositions.get(keySetEnt.get(i)));
								setOfRightAnnotations.add(treeEntitiesPositions.get(keySetEnt.get(i)));
							}
						}
					}

				}
			}
		}		
		
	}

	protected void processindEntitiesWithMaxVerbDistance(IAnnotatedDocument document,IVerbInfo verb,
			Long startPosition, Long endPosition, List<Long> keySetEnt,
			Map<Long, IEntityAnnotation> treeEntitiesPositions,
			List<IEntityAnnotation> leftentities,
			List<IEntityAnnotation> rightentities,
			SortedSet<IEntityAnnotation> setOfEntitiesAnnotations,
			SortedSet<IEntityAnnotation> setOfRightAnnotations,
			int verbEntitieMaxWordDistance) throws ANoteException {
		for(int i=0;i<keySetEnt.size();i++)
		{

			if(keySetEnt.get(i)>=startPosition&&keySetEnt.get(i)<=endPosition)
			{				
				// left
				if(treeEntitiesPositions.get(keySetEnt.get(i)).getEndOffset()<verb.getStartOffset())
				{
					// Verb Distance Condition in Words
					Long ent = keySetEnt.get(i);
					long start = treeEntitiesPositions.get(ent).getEndOffset();
					int words = numberOffWordsBetweenTwoOffsets(document,start,verb.getStartOffset());
					if(words <=verbEntitieMaxWordDistance)
					{
						if(!setOfEntitiesAnnotations.contains(treeEntitiesPositions.get(keySetEnt.get(i))))
						{
							leftentities.add(treeEntitiesPositions.get(keySetEnt.get(i)));
							setOfEntitiesAnnotations.add(treeEntitiesPositions.get(keySetEnt.get(i)));
						}
					}
				}
				// right
				else if(treeEntitiesPositions.get(keySetEnt.get(i)).getStartOffset() > verb.getStartOffset() + verb.getVerb().length())
				{
					// Verb Distance Condition in Words
					if(numberOffWordsBetweenTwoOffsets(document,verb.getStartOffset()+verb.getVerb().length(),treeEntitiesPositions.get(keySetEnt.get(i)).getStartOffset())<=verbEntitieMaxWordDistance)
					{
						if(!setOfRightAnnotations.contains(treeEntitiesPositions.get(keySetEnt.get(i))))
						{
							rightentities.add(treeEntitiesPositions.get(keySetEnt.get(i)));
							setOfRightAnnotations.add(treeEntitiesPositions.get(keySetEnt.get(i)));
						}
					}
				}
			}
		}		
	}
	
	protected int numberOffWordsBetweenTwoOffsets(IAnnotatedDocument document,long offsetStart,long longoffsetEnd) throws ANoteException
	{
		// Problem here
		int words = TextProcessor.getNumberOFwords(document.getDocumentAnnotationText().substring((int)offsetStart, (int)longoffsetEnd));
		return words;
		
	}

	protected void processindEntitiesWithoutRestrictions(IVerbInfo verb,Long startPosition,
			Long endPosition, List<Long> keySetEnt,
			Map<Long, IEntityAnnotation> treeEntitiesPositions,
			List<IEntityAnnotation> leftentities,
			List<IEntityAnnotation> rightentities,
			SortedSet<IEntityAnnotation> setOfEntitiesAnnotations,
			SortedSet<IEntityAnnotation> setOfRightAnnotations) {
		for(int i=0;i<keySetEnt.size();i++)
		{

			if(keySetEnt.get(i)>=startPosition&&keySetEnt.get(i)<=endPosition)
			{
				// left
				if(treeEntitiesPositions.get(keySetEnt.get(i)).getEndOffset()<verb.getStartOffset())
				{
					if(!setOfEntitiesAnnotations.contains(treeEntitiesPositions.get(keySetEnt.get(i))))
					{
						leftentities.add(treeEntitiesPositions.get(keySetEnt.get(i)));
						setOfEntitiesAnnotations.add(treeEntitiesPositions.get(keySetEnt.get(i)));
					}
				}
				// right
				else if(treeEntitiesPositions.get(keySetEnt.get(i)).getStartOffset() > verb.getStartOffset() + verb.getVerb().length())
				{
					if(!setOfRightAnnotations.contains(treeEntitiesPositions.get(keySetEnt.get(i))))
					{
						rightentities.add(treeEntitiesPositions.get(keySetEnt.get(i)));
						setOfRightAnnotations.add(treeEntitiesPositions.get(keySetEnt.get(i)));
					}
				}
			}
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
