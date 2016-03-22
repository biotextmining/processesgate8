package com.silicolife.wrappergate;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.creole.ResourceInstantiationException;
import gate.util.InvalidOffsetException;

import java.io.File;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

import com.silicolife.textmining.core.datastructures.utils.GenericPairComparable;

public class GateCorpusReaderLoader {
	
	/**
	 * Generic Static Method for find information about articles
	 * Return Information about article
	 * 
	 * @param gateDoc Gate Document
	 * @param annotations Annotations Gate
	 * @param annotationType Annotations Type
	 * @return Information
	 * @throws InvalidOffsetException 
	 */
	public static String getGeneralArticleInfo(Document gateDoc, AnnotationSet annotations,String annotationType) throws InvalidOffsetException {
		String info="";
		AnnotationSet annotSet = annotations.get(annotationType);	
		Iterator<Annotation> iterAnnot = annotSet.iterator();	
		if(iterAnnot.hasNext())
		{
			Annotation annot = iterAnnot.next();
			long start =  annot.getStartNode().getOffset();
			long end =  annot.getEndNode().getOffset();		
			info=gateDoc.getContent().getContent(start, end).toString();
		}
		if(info==null)
		{
			return "";
		}
		else
		{
			return info;
		}
	}
	
	public static Document createGateDocument(File file) throws ResourceInstantiationException {
		FeatureMap params = Factory.newFeatureMap();
		params.put("sourceUrl",file.toURI().toString());			
		FeatureMap features = Factory.newFeatureMap();
		Document doc = (Document) Factory.createResource("gate.corpora.DocumentImpl", params, features, "GATE Homepage");
		return doc;
	}
	
	/**
	 * Static Method
	 * Method that return a piece of Text.
	 * 
	 * 
	 * @param gateDoc Gate Document
	 * @param startPosition 
	 * @param endPosition
	 * @return
	 */
	public static String getPartGateDocument(Document gateDoc,Long startPosition,Long endPosition)
	{
		String allText = gateDoc.getContent().toString();
		return allText.substring(startPosition.intValue(),endPosition.intValue());
	}
	

	/**
	 * Static Method that return a matrix contain the limits of all 
	 * annotationType
	 * 
	 * @param annotSet Original Annotations
	 * @param annotationType
	 * @return
	 */
	public static long[][] getGenericOffsetLimits(AnnotationSet annotSet,String annotationType,boolean order)
	{
		AnnotationSet annotSetAnnotationType = annotSet.get(annotationType);
		long[][] annotationTypelimits;
		// problema do gate nao pode ser nulo
		if(annotSetAnnotationType.size()<1)
		{
			annotationTypelimits =new long[1][2];
			annotationTypelimits[0][0]=-1;
			annotationTypelimits[0][1]=-1;
			return annotationTypelimits;
		}
		else
		{
			annotationTypelimits = new long[annotSetAnnotationType.size()][2];
			Iterator<Annotation> iterAnnot = annotSetAnnotationType.iterator();
			int i=0;
			while(iterAnnot.hasNext())
			{
				Annotation annot = iterAnnot.next();
				annotationTypelimits[i][0] =  annot.getStartNode().getOffset();
				annotationTypelimits[i][1] =  annot.getEndNode().getOffset();
				i++;
			}
			if(order)
			{
				return orderElements(annotationTypelimits);
			}
			return annotationTypelimits;
		}
		
	}
	
	public static SortedMap<GenericPairComparable<Long,Long> ,Annotation>  getGenericOrderAnnotations(AnnotationSet annotSet,String annotationType)
	{
		SortedMap<GenericPairComparable<Long,Long> ,Annotation> orderAnnotMap = new TreeMap<GenericPairComparable<Long,Long>, Annotation>();
		AnnotationSet annotSetAnnotationType = annotSet.get(annotationType);
		Iterator<Annotation> iterAnnot = annotSetAnnotationType.iterator();
		Annotation annotation;
		Long elemStart, elemEnd;
		while(iterAnnot.hasNext())
		{
			annotation = iterAnnot.next();
			elemStart = annotation.getStartNode().getOffset();
			elemEnd = annotation.getEndNode().getOffset();
			orderAnnotMap.put(new GenericPairComparable<Long, Long>(elemStart,elemEnd), annotation);	
		}
		return orderAnnotMap;
	}
	
	private static long[][] orderElements(long[][] annotationTypelimits)
	{
		long[][] orderElements = new long[annotationTypelimits.length][2];
		TreeMap<Long,Long> orderTree = new TreeMap<Long, Long>();
		for(int i=0;i<annotationTypelimits.length;i++)
		{
			orderTree.put(annotationTypelimits[i][0], annotationTypelimits[i][1]);
		}
		Iterator<Long> itElements = orderTree.keySet().iterator();
		int j=0;
		while(itElements.hasNext())
		{
			Long elemStart = itElements.next();
			Long elemEnd = orderTree.get(elemStart);
			orderElements[j][0]=elemStart;
			orderElements[j][1]=elemEnd;
			j++;	
		}	
		return orderElements;
	}

}
