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
import com.silicolife.wrappergate.IGatePosTagger;

/**
 * 
 * @author Hugo Costa
 *
 */
public class RelationModelSimple implements IRelationModel{


	private IGatePosTagger postagger;

	public RelationModelSimple(IGatePosTagger postagger) {
		this.postagger = postagger;
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

	public List<IEventAnnotation> extractSentenceRelation(IAnnotatedDocument document,List<IEntityAnnotation> semanticLayer,ISentenceSintaxRepresentation sentenceSintax) throws ANoteException{
		Map<Long, IEntityAnnotation> treeEntitiesPositions = RelationModelutils.getEntitiesPosition(semanticLayer); // offset->entityID
		List<IVerbInfo> verbsInfo = sentenceSintax.getListVerbs();
		Map<Long, IVerbInfo> treeverbPositions = getPostagger().getVerbsPosition(verbsInfo); 		
		List<IEventAnnotation> relations = new ArrayList<IEventAnnotation>();	
		Set<Long> verbPositions = treeverbPositions.keySet();	
		List<IEntityAnnotation> leftentities;
		List<IEntityAnnotation> rightentities;
		IEventProperties eventProperties;
		// For each verb
		for(Long verbPosition: verbPositions)
		{  
			String verb = treeverbPositions.get(verbPosition).getVerb();
			String lemma = treeverbPositions.get(verbPosition).getLemma();
			DirectionallyEnum dir = treeverbPositions.get(verbPosition).getDirectionality();
			PolarityEnum polarity = treeverbPositions.get(verbPosition).getPolarity();			
			leftentities=new ArrayList<IEntityAnnotation>();
			rightentities=new ArrayList<IEntityAnnotation>();		
			Set<Long> entitiesPositions = treeEntitiesPositions.keySet();
			SortedSet<IEntityAnnotation> setOfEntitiesAnnotations = new TreeSet<IEntityAnnotation>();
			SortedSet<IEntityAnnotation> setOfRightAnnotations = new TreeSet<IEntityAnnotation>();
			// For each entity
			for(Long entityPositon:entitiesPositions)
			{
				// Test if entity is on the left
				if(treeEntitiesPositions.get(entityPositon).getEndOffset()<verbPosition)
				{
					// Test synonyms
					if(!setOfEntitiesAnnotations.contains(treeEntitiesPositions.get(entityPositon)))
					{
						leftentities.add(treeEntitiesPositions.get(entityPositon));
						setOfEntitiesAnnotations.add(treeEntitiesPositions.get(entityPositon));
					}				
				}
				// entity is on the right
				else if(entityPositon > verbPosition + verb.length())
				{
					// Test synonyms
					if(!setOfRightAnnotations.contains(treeEntitiesPositions.get(entityPositon)))
					{
						rightentities.add(treeEntitiesPositions.get(entityPositon));
						setOfRightAnnotations.add(treeEntitiesPositions.get(entityPositon));
					}
				}
			}
			eventProperties = new EventPropertiesImpl();
			eventProperties.setDirectionally(dir);
			eventProperties.setLemma(lemma);
			eventProperties.setPolarity(polarity);		
			IEventAnnotation event = new EventAnnotationImpl(treeverbPositions.get(verbPosition).getStartOffset(), treeverbPositions.get(verbPosition).getEndOffset(),
					"", leftentities, rightentities, verb,-1,"", eventProperties);
			if(leftentities.size()+rightentities.size()>1)
			{
				// Remove binary relation with the same entity in right and left
				if(!(leftentities.size()==1 && rightentities.size() == 1 && leftentities.get(0).equals(rightentities.get(0))))
				{					
					relations.add(event);
				}
			}
		}	
		return relations;
	}
	
	public String getDescription() {
		return "Simple Model";
	}
	
	public String getImagePath() {
		return "icons/relation_model_simple.png";
	}

	public String toString(){
		return "Simple Model (M x M )";
	}

	public IGatePosTagger getPostagger() {
		return postagger;
	}

	@Override
	public Properties getProperties() {
		Properties prop = new Properties();
		return prop;
	}

	@Override
	public String getUID() {
		return "Simple Model (M x M )";
	}

}
