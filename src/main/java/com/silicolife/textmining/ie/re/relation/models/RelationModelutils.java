package com.silicolife.textmining.ie.re.relation.models;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.silicolife.textmining.core.interfaces.core.annotation.IEntityAnnotation;

public class RelationModelutils {
	
	public static Map<Long,IEntityAnnotation> getEntitiesPosition(List<IEntityAnnotation> semanticLayer)
	{
		TreeMap<Long,IEntityAnnotation> treeEntities = new TreeMap<Long, IEntityAnnotation>();
		
		for(int i=0;i<semanticLayer.size();i++)
		{
			IEntityAnnotation ent = semanticLayer.get(i); // entityID,offset
			treeEntities.put(ent.getStartOffset(), ent); // offset,entityID
		}
		return treeEntities;
	}
	


}
