package com.silicolife.textmining.ie.re.relation.models.old;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.silicolife.textmining.core.datastructures.annotation.AnnotationType;
import com.silicolife.textmining.core.datastructures.annotation.re.EventAnnotationImpl;
import com.silicolife.textmining.core.datastructures.annotation.re.EventPropertiesImpl;
import com.silicolife.textmining.core.datastructures.process.re.RelationTypeImpl;
import com.silicolife.textmining.core.interfaces.core.annotation.IEntityAnnotation;
import com.silicolife.textmining.core.interfaces.core.annotation.IEventAnnotation;
import com.silicolife.textmining.core.interfaces.core.annotation.re.DirectionallyEnum;
import com.silicolife.textmining.core.interfaces.core.annotation.re.IEventProperties;
import com.silicolife.textmining.core.interfaces.core.annotation.re.PolarityEnum;
import com.silicolife.textmining.core.interfaces.core.dataaccess.exception.ANoteException;
import com.silicolife.textmining.core.interfaces.core.document.IAnnotatedDocument;
import com.silicolife.textmining.core.interfaces.core.document.structure.ISentenceSintaxRepresentation;
import com.silicolife.textmining.core.interfaces.process.IE.re.IRelationsType;
import com.silicolife.textmining.core.interfaces.process.IE.re.clue.IVerbInfo;
import com.silicolife.textmining.ie.re.relation.configuration.IRERelationAdvancedConfiguration;
import com.silicolife.textmining.ie.re.relation.models.RelationModelutils;
import com.silicolife.textmining.ie.re.relation.models.specialproperties.VerbClassificationInSentenceEnum;
import com.silicolife.wrappergate.IGatePosTagger;

public class RelationModelBinaryVerbLimitationOld extends RelationModelVerbLimitationOld {


	public RelationModelBinaryVerbLimitationOld(IGatePosTagger postagger,IRERelationAdvancedConfiguration advanceConfiguration) {
		super(postagger,advanceConfiguration);
	}
	
	public List<IEventAnnotation> extractSentenceRelation(IAnnotatedDocument document,List<IEntityAnnotation> semanticLayer, ISentenceSintaxRepresentation sentenceSintax) throws ANoteException {
		Map<Long, IEntityAnnotation> treeEntitiesPositions = RelationModelutils.getEntitiesPosition(semanticLayer); // offset->entityID
		List<IVerbInfo> verbsInfo = sentenceSintax.getListVerbs();
		Map<Long, IVerbInfo> treeverbPositions = getPostagger().getVerbsPosition(verbsInfo); 
		Set<Long> verbsSet = treeverbPositions.keySet();
		List<Long> verbsList = new ArrayList<Long>(verbsSet);
		Set<Long> entitiesSet = treeEntitiesPositions.keySet();
		List<Long> entitiesList = new ArrayList<Long>(entitiesSet);
		List<IEventAnnotation> relations = new ArrayList<IEventAnnotation>();	
		
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
						extractRelation(document,relations,verb,startposition,endPosition,
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
						extractRelation(document,relations,verb,startposition,endPosition,treeverbPositions,entitiesList,
								treeEntitiesPositions,VerbClassificationInSentenceEnum.FIRST);
					}			
				}
				// last verb
				else if(i==verbsList.size()-1)
				{
					long startposition = treeverbPositions.get(verbsList.get(i-1)).getEndOffset();
					long endPosition = treeEntitiesPositions.get(entitiesList.get(entitiesList.size()-1)).getEndOffset()+1;
					extractRelation(document,relations,verb,startposition,endPosition,
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
					extractRelation(document,relations,verb,startposition,endPosition,treeverbPositions,entitiesList
							,treeEntitiesPositions,VerbClassificationInSentenceEnum.MIDDLE);

				}
			}		
		}
		return relations;
	}
	
	protected void extractRelation(IAnnotatedDocument document,List<IEventAnnotation> relations, IVerbInfo verb,Long startPosition,Long endPosition, Map<Long,IVerbInfo> treeverb,List<Long> keySetEnt,
			Map<Long, IEntityAnnotation> treeEntitiesPositions, VerbClassificationInSentenceEnum verbClassidfication) throws ANoteException
	{
		List<IEntityAnnotation> leftentities=new ArrayList<IEntityAnnotation>();
		List<IEntityAnnotation> rightentities=new ArrayList<IEntityAnnotation>();
		DirectionallyEnum dir = verb.getDirectionality();
		PolarityEnum polarity = verb.getPolarity();	
		String lemma = verb.getLemma();
		IEventProperties eventProperties = new EventPropertiesImpl();
		eventProperties.setDirectionally(dir);
		eventProperties.setLemma(lemma);
		eventProperties.setPolarity(polarity);			
		SortedSet<IEntityAnnotation> setOfEntitiesAnnotations = new TreeSet<IEntityAnnotation>();
		SortedSet<IEntityAnnotation> setOfRightAnnotations = new TreeSet<IEntityAnnotation>();
		if(getAdvancedConfiguration()!=null && getAdvancedConfiguration().isUsingOnlyVerbNearestEntities())
		{
			processindOnlyNearestEntities(verb, startPosition, endPosition,keySetEnt, treeEntitiesPositions, leftentities, rightentities,setOfEntitiesAnnotations, setOfRightAnnotations);
		}
		else if(getAdvancedConfiguration()!=null && getAdvancedConfiguration().isUsingOnlyEntitiesNearestVerb())
		{
			processingEntitiesNearestToVerb(verb, startPosition, endPosition, keySetEnt,treeEntitiesPositions, leftentities, rightentities,setOfEntitiesAnnotations, setOfRightAnnotations,verbClassidfication);
		}
		else if(getAdvancedConfiguration()!=null && getAdvancedConfiguration().isUsingVerbEntitiesDistance() && getAdvancedConfiguration().getVerbEntitieMaxDistance() > 0)
		{
			processindEntitiesWithMaxVerbDistance(document,verb, startPosition, endPosition, keySetEnt,treeEntitiesPositions, leftentities, rightentities,setOfEntitiesAnnotations, setOfRightAnnotations,getAdvancedConfiguration().getVerbEntitieMaxDistance());
		}
		else
		{
			processindEntitiesWithoutRestrictions(verb, startPosition, endPosition,keySetEnt, treeEntitiesPositions, leftentities, rightentities,setOfEntitiesAnnotations, setOfRightAnnotations);
		}
		// Relation Filter By Class
		if(getAdvancedConfiguration()!=null && getAdvancedConfiguration().getRelationsType()!=null && getAdvancedConfiguration().getRelationsType().size() > 0)
		{
			withRelationTypeFilter(relations, verb,leftentities, rightentities, eventProperties,getAdvancedConfiguration().getRelationsType());
		}
		else
		{
			withouRelationTypeFilter(relations, verb,leftentities, rightentities, eventProperties);
		}
	}
	
	
	protected void processindOnlyNearestEntities(IVerbInfo verb,
			Long startPosition, Long endPosition, List<Long> keySetEnt,
			Map<Long, IEntityAnnotation> treeEntitiesPositions,
			List<IEntityAnnotation> leftentities,
			List<IEntityAnnotation> rightentities,
			SortedSet<IEntityAnnotation> setOfEntitiesAnnotations,
			SortedSet<IEntityAnnotation> setOfRightAnnotations) {
		IEntityAnnotation closeastToVerbAtLeft = null;
		IEntityAnnotation closeastToVerbAtRight = null;
		for(int i=0;i<keySetEnt.size();i++)
		{

			if(keySetEnt.get(i)>=startPosition&&keySetEnt.get(i)<=endPosition)
			{
				// left
				if(keySetEnt.get(i)<=verb.getStartOffset())
				{
					if(closeastToVerbAtLeft==null)
					{
						closeastToVerbAtLeft = treeEntitiesPositions.get(keySetEnt.get(i));
					}
					else
					{
						long actualverbDistance = verb.getStartOffset() - closeastToVerbAtLeft.getEndOffset();
						long candidateVerbDistance = verb.getStartOffset() - treeEntitiesPositions.get(keySetEnt.get(i)).getEndOffset();
						if(candidateVerbDistance < actualverbDistance)
						{
							closeastToVerbAtLeft = treeEntitiesPositions.get(keySetEnt.get(i));
						}
					}
				}
				// right
				else
				{
					if(closeastToVerbAtRight == null)
					{
						closeastToVerbAtRight = treeEntitiesPositions.get(keySetEnt.get(i));
					}
					else
					{
						long actualverbDistance =  closeastToVerbAtRight.getEndOffset() - verb.getStartOffset();
						long candidateVerbDistance = treeEntitiesPositions.get(keySetEnt.get(i)).getEndOffset() - verb.getStartOffset();
						if(candidateVerbDistance < actualverbDistance)
						{
							closeastToVerbAtRight = treeEntitiesPositions.get(keySetEnt.get(i));
						}
					}
				}
			}
		}
		if(closeastToVerbAtLeft!=null && closeastToVerbAtRight!=null)
		{
			leftentities.add(closeastToVerbAtLeft);
			rightentities.add(closeastToVerbAtRight);
		}
		
	}


	protected void withRelationTypeFilter(List<IEventAnnotation> relations,IVerbInfo verb,List<IEntityAnnotation> leftentities,
			List<IEntityAnnotation> rightentities,IEventProperties eventProperties,SortedSet<IRelationsType> relationsType) {
		IEventAnnotation event;
		for(IEntityAnnotation left:leftentities)
		{
			for(IEntityAnnotation right:rightentities)
			{

				if(!left.equals(right))
				{
					IRelationsType rt = new RelationTypeImpl(left.getClassAnnotation().getId(), right.getClassAnnotation().getId());
					if(relationsType.contains(rt))
					{			
						List<IEntityAnnotation> rightList = new ArrayList<>();
						rightList.add(right);
						List<IEntityAnnotation> leftList = new ArrayList<>();
						leftList.add(left);
						event = new EventAnnotationImpl(verb.getStartOffset(), verb.getEndOffset(),AnnotationType.re.name(), leftList, rightList , verb.getVerb(),eventProperties,false);
						relations.add(event);
					}
				}
			}
		}
	}

	protected void withouRelationTypeFilter(List<IEventAnnotation> relations,
			IVerbInfo verb,
			List<IEntityAnnotation> leftentities,
			List<IEntityAnnotation> rightentities,
			IEventProperties eventProperties) {
		IEventAnnotation event;
		for(IEntityAnnotation left:leftentities)
		{
			for(IEntityAnnotation right:rightentities)
			{
				
				if(!left.equals(right))
				{
					List<IEntityAnnotation> rightList = new ArrayList<>();
					rightList.add(right);
					List<IEntityAnnotation> leftList = new ArrayList<>();
					leftList.add(left);
					event = new EventAnnotationImpl(verb.getStartOffset(), verb.getEndOffset(),
							"", leftList, rightList, verb.getVerb(),eventProperties,false);
					relations.add(event);
				}
			}
		}
	}

	public String getDescription(){
		return "Binary Verb Limitation";
	}
	
	public String getImagePath() {
		return "icons/relation_model_binary_verb_limitation.png";
	}

	public String toString(){
		return "Binary Verb Limitation (1 x 1)";
	}
	
	public String getUID() {
		return "Binary Verb Limitation (1 x 1)";
	}
	

}
