package com.silicolife.textmining.ie.re.relation.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import com.silicolife.textmining.core.interfaces.process.IE.re.IRelationsType;
import com.silicolife.textmining.core.interfaces.process.IE.re.clue.IVerbInfo;
import com.silicolife.textmining.ie.re.relation.configuration.IRERelationConfiguration;
import com.silicolife.textmining.ie.re.relation.models.specialproperties.VerbClassificationInSentenceEnum;

public class RelationModelBinaryVerbLimitation extends RelationModelVerbLimitation {


	public RelationModelBinaryVerbLimitation(IRERelationConfiguration configuration) {
		super(configuration);
	}
	
	protected List<IEventAnnotation> extractRelation(IAnnotatedDocument document,IVerbInfo verb,IVerbInfo previous,IVerbInfo further,List<IEntityAnnotation> potencialEntitiesAtLeft,
			List<IEntityAnnotation> potencialEntitiesAtRight,Map<Long,IVerbInfo> treeverb,
			VerbClassificationInSentenceEnum verbClassidfication) throws ANoteException
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
		if(getAdvancedConfiguration()!=null && getAdvancedConfiguration().usingOnlyVerbNearestEntities())
		{
			processindOnlyNearestEntities(verb,leftentities, rightentities,potencialEntitiesAtLeft, potencialEntitiesAtRight);
		}
		else if(getAdvancedConfiguration()!=null && getAdvancedConfiguration().usingOnlyEntitiesNearestVerb())
		{
			processingEntitiesNearestToVerb(verb,previous,further, leftentities, rightentities,potencialEntitiesAtLeft, potencialEntitiesAtRight,verbClassidfication);
		}
		else if(getAdvancedConfiguration()!=null && getAdvancedConfiguration().usingVerbEntitiesDistance() && getAdvancedConfiguration().getVerbEntitieMaxDistance() > 0)
		{
			processindEntitiesWithMaxVerbDistance(document,verb,potencialEntitiesAtLeft,potencialEntitiesAtRight, leftentities, rightentities,getAdvancedConfiguration().getVerbEntitieMaxDistance());
		}
		else
		{
			leftentities = potencialEntitiesAtLeft;
			rightentities = potencialEntitiesAtRight;
		}
		if(getAdvancedConfiguration().groupingSynonyms())
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
		}
		// Relation Filter By Class
		if(getAdvancedConfiguration()!=null && getAdvancedConfiguration().getRelationsType()!=null && getAdvancedConfiguration().getRelationsType().size() > 0)
		{
			return withRelationTypeFilter(verb,leftentities, rightentities, eventProperties,getAdvancedConfiguration().getRelationsType());
		}
		else
		{
			return withouRelationTypeFilter(verb,leftentities, rightentities, eventProperties);
		}
	}
	
	
	protected void processindOnlyNearestEntities(IVerbInfo verb,List<IEntityAnnotation> leftentities,List<IEntityAnnotation> rightentities,
			List<IEntityAnnotation> potencialEntitiesAtLeft,List<IEntityAnnotation> potencialEntitiesAtRight) {
		IEntityAnnotation closeastToVerbAtLeft = null;
		IEntityAnnotation closeastToVerbAtRight = null;
		IEntityAnnotation candidateEntity;
		if(!potencialEntitiesAtLeft.isEmpty())
		{
			closeastToVerbAtLeft = potencialEntitiesAtLeft.get(0);
			for(int i=1;i<potencialEntitiesAtLeft.size();i++)
			{
				candidateEntity = potencialEntitiesAtLeft.get(i);
				long distenceBeetweenCandidateAndVerb = verb.getStartOffset() - candidateEntity.getEndOffset();
				long distenceBeetweencloseastToVerbAtLeftVerb = verb.getStartOffset() - closeastToVerbAtLeft.getEndOffset();
				if(distenceBeetweencloseastToVerbAtLeftVerb>distenceBeetweenCandidateAndVerb)
				{
					closeastToVerbAtLeft = candidateEntity;
				}
			}
		}
		if(!potencialEntitiesAtRight.isEmpty())
		{
			closeastToVerbAtRight = potencialEntitiesAtRight.get(0);
			for(int i=1;i<potencialEntitiesAtRight.size();i++)
			{
				candidateEntity = potencialEntitiesAtRight.get(i);
				long distenceBeetweenCandidateAndVerb = candidateEntity.getStartOffset() - verb.getEndOffset();
				long distenceBeetweencloseastToVerbAtLeftVerb = closeastToVerbAtRight.getStartOffset() - verb.getEndOffset();
				if(distenceBeetweencloseastToVerbAtLeftVerb>distenceBeetweenCandidateAndVerb)
				{
					closeastToVerbAtRight = candidateEntity;
				}
			}
		}
		if(closeastToVerbAtLeft!=null)
			leftentities.add(closeastToVerbAtLeft);
		if(closeastToVerbAtRight!=null)
			rightentities.add(closeastToVerbAtRight);
	}


	protected List<IEventAnnotation> withRelationTypeFilter(IVerbInfo verb,List<IEntityAnnotation> leftentities,
			List<IEntityAnnotation> rightentities,IEventProperties eventProperties,SortedSet<IRelationsType> relationsType) {
		List<IEventAnnotation> relations = new ArrayList<IEventAnnotation>();
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
						event = new EventAnnotationImpl(verb.getStartOffset(), verb.getEndOffset(),AnnotationType.re.name(), leftList, rightList , verb.getVerb(),-1,"", eventProperties);
						relations.add(event);
					}
				}
			}
		}
		return relations;
	}

	protected List<IEventAnnotation> withouRelationTypeFilter(
			IVerbInfo verb,
			List<IEntityAnnotation> leftentities,
			List<IEntityAnnotation> rightentities,
			IEventProperties eventProperties) {
		List<IEventAnnotation> relations = new ArrayList<IEventAnnotation>();
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
							"", leftList, rightList, verb.getVerb(),-1,"", eventProperties);
					relations.add(event);
				}
			}
		}
		return relations;
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
