package com.silicolife.textmining.ie.re.relation.datastructures;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;

import java.util.ArrayList;
import java.util.List;

import com.silicolife.textmining.core.datastructures.utils.GenericPairImpl;
import com.silicolife.textmining.core.datastructures.utils.GenericTriple;

public class POSTaggerHelp {
	
	public static List<GenericPairImpl<Long, Long>> getGateDocumentSentencelimits(Document doc, Long documentStartOffset, Long documentEndOffset) {
		List<GenericPairImpl<Long,Long>> sentenceLimits = new ArrayList<GenericPairImpl<Long,Long>>();
		AnnotationSet annotSetSentences = doc.getAnnotations();
		AnnotationSet annotSetSentences2 = annotSetSentences.get(documentStartOffset, documentEndOffset).get("Sentence");
		for(Annotation annot:annotSetSentences2)
		{
			if(annot.getStartNode().getOffset()<documentStartOffset)
			{
				if(annot.getEndNode().getOffset()>documentEndOffset)
				{
					sentenceLimits.add(new GenericPairImpl<Long, Long>(documentStartOffset,documentEndOffset));
				}
				else
				{
					sentenceLimits.add(new GenericPairImpl<Long, Long>(documentStartOffset,annot.getEndNode().getOffset()));
				}
			}
			else if(annot.getEndNode().getOffset()>documentEndOffset)
			{
				if(annot.getStartNode().getOffset()<documentStartOffset)
				{
					sentenceLimits.add(new GenericPairImpl<Long, Long>(documentStartOffset,documentEndOffset));
				}
				else
				{
					sentenceLimits.add(new GenericPairImpl<Long, Long>(annot.getStartNode().getOffset(),documentEndOffset));
				}
			}
			else
			{
				sentenceLimits.add(new GenericPairImpl<Long, Long>(annot.getStartNode().getOffset(),annot.getEndNode().getOffset()));
			}
		}
		return sentenceLimits;
	}
	
	public static List<GenericTriple<Long, Long,Integer>> getGateDocumentlimits(Document doc) {
		List<GenericTriple<Long,Long,Integer>> sentenceLimits = new ArrayList<GenericTriple<Long,Long,Integer>>();
		AnnotationSet annotSetSentences = doc.getAnnotations("Original markups");
		AnnotationSet annotSetSentences2 = annotSetSentences.get("Doc");
		for(Annotation annot:annotSetSentences2)
		{
			String idS = (String) annot.getFeatures().get("id");
			int id = Integer.valueOf(idS);
			sentenceLimits.add(new GenericTriple<Long, Long,Integer>(annot.getStartNode().getOffset(),annot.getEndNode().getOffset(),id));
		}
		return sentenceLimits;
	}

}
