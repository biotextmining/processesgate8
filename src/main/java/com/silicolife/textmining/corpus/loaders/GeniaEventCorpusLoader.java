package com.silicolife.textmining.corpus.loaders;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.creole.ResourceInstantiationException;
import gate.util.GateException;
import gate.util.InvalidOffsetException;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.Stack;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.silicolife.textmining.core.datastructures.annotation.AnnotationType;
import com.silicolife.textmining.core.datastructures.annotation.ner.EntityAnnotationImpl;
import com.silicolife.textmining.core.datastructures.annotation.re.EventAnnotationImpl;
import com.silicolife.textmining.core.datastructures.annotation.re.EventPropertiesImpl;
import com.silicolife.textmining.core.datastructures.documents.AnnotatedDocumentImpl;
import com.silicolife.textmining.core.datastructures.documents.PublicationExternalSourceLinkImpl;
import com.silicolife.textmining.core.datastructures.documents.PublicationImpl;
import com.silicolife.textmining.core.datastructures.general.AnoteClass;
import com.silicolife.textmining.core.datastructures.general.ClassPropertiesManagement;
import com.silicolife.textmining.core.datastructures.textprocessing.NormalizationForm;
import com.silicolife.textmining.core.datastructures.utils.GenericPairComparable;
import com.silicolife.textmining.core.datastructures.utils.GenericPairImpl;
import com.silicolife.textmining.core.datastructures.utils.GenericTriple;
import com.silicolife.textmining.core.interfaces.core.annotation.IEntityAnnotation;
import com.silicolife.textmining.core.interfaces.core.annotation.IEventAnnotation;
import com.silicolife.textmining.core.interfaces.core.annotation.re.IEventProperties;
import com.silicolife.textmining.core.interfaces.core.corpora.loaders.ICorpusEventAnnotationLoader;
import com.silicolife.textmining.core.interfaces.core.dataaccess.exception.ANoteException;
import com.silicolife.textmining.core.interfaces.core.document.IAnnotatedDocument;
import com.silicolife.textmining.core.interfaces.core.document.IPublication;
import com.silicolife.textmining.core.interfaces.core.document.IPublicationExternalSourceLink;
import com.silicolife.textmining.core.interfaces.core.document.labels.IPublicationLabel;
import com.silicolife.textmining.core.interfaces.core.document.structure.IPublicationField;
import com.silicolife.textmining.core.interfaces.core.general.classe.IAnoteClass;
import com.silicolife.textmining.core.interfaces.core.utils.IGenericPair;
import com.silicolife.wrappergate.GateCorpusReaderLoader;
import com.silicolife.wrappergate.GateInit;

public class GeniaEventCorpusLoader implements ICorpusEventAnnotationLoader{
	
	private int corpusSize;
	private int corpusLoaderPosition;
	private List<IPublication> documents;
	private Document gateDoc;
	private List<String> corruptFiles;
	private Set<String> validTags;
	private String fulltext;
	private Map<String,IEntityAnnotation> EntityIDEntity;
	private Map<String,Integer> classesClassesID;
	private boolean stop = false;
	private String pubmedExternalLink = "PUBMED";
	private List<IEventAnnotation> events;
	private Map<Long, IAnnotatedDocument> documentsWithEntities;
	private Map<Long, IAnnotatedDocument> documentsWithEvents;
 
	public GeniaEventCorpusLoader()
	{
		this.corpusLoaderPosition = 1;
		this.documents = new ArrayList<>();
		this.validTags = putValidTags();
		this.fulltext = new String();
		this.events = new ArrayList<>();
		this.documentsWithEntities = new TreeMap<>();
		this.documentsWithEvents = new TreeMap<>();
		this.EntityIDEntity = new HashMap<>();
	}
	
	private Set<String> putValidTags() {
		Set<String> validTags = new HashSet<String>();
		validTags.add("Annotation");
		validTags.add("PubmedArticleSet");
		validTags.add("PubmedArticle");
		validTags.add("MedlineCitation");
		validTags.add("PMID");
		validTags.add("Article");
		validTags.add("ArticleTitle");
		validTags.add("sentence");
		validTags.add("term");
		validTags.add("cons");
		validTags.add("event");
		validTags.add("type");
		validTags.add("theme");
		validTags.add("clue");
		validTags.add("cause");
		validTags.add("clueType");
		validTags.add("clueLoc");
		validTags.add("linkCause");
		validTags.add("linkTheme");
		validTags.add("comment");
		validTags.add("Abstract");
		validTags.add("frag");
		validTags.add("clueTime");
		validTags.add("AbstractText");
		validTags.add("site");
		validTags.add("corefTheme");
		validTags.add("clueExperiment");
		validTags.add("corefCause");
		validTags.add("product");
		validTags.add("corefSite");
		return validTags;
	}

	@Override
	public List<IPublication> processFile(File fileOrDirectory,
			Properties properties) throws ANoteException, IOException {
		if(validateFile(fileOrDirectory))
		{
			try {
				GateInit.getInstance().init();
			} catch (GateException e1) {
				e1.printStackTrace();
			}
			for(File file:fileOrDirectory.listFiles())
			{
				if(stop)
				{
					return	getDocuments();
				}
				if(!file.isDirectory())
				{
					processFile(file);
					this.events = new ArrayList<IEventAnnotation>();
					this.EntityIDEntity = new HashMap<>();
					this.fulltext = new String();
				}
			} 
			return 	getDocuments();
		}
		else
		{
			return null;
		}	
	}

	private void processFile(File file) throws ANoteException {
		try{
		gateDoc = GateCorpusReaderLoader.createGateDocument(file);;
		AnnotationSet annotSetOriginal = gateDoc.getAnnotations("Original markups");
		String title = getDocumentTitle(gateDoc,annotSetOriginal);
		String abstractText = getDocumentAbstract(gateDoc,annotSetOriginal);
		String pmid = getDocumentPMID(gateDoc,annotSetOriginal);
		IPublicationExternalSourceLink externalID = new PublicationExternalSourceLinkImpl(pmid, pubmedExternalLink);
		List<IPublicationExternalSourceLink> publicationExternalIDSource = new ArrayList<>();
		publicationExternalIDSource.add(externalID);
		IPublication pub = new PublicationImpl(title, "", "", "", "", "", "", "", "", "", abstractText, "", true, "", "", publicationExternalIDSource, new ArrayList<IPublicationField>(), new ArrayList<IPublicationLabel>());
		pub.setFullTextContent(getFulltext());
		getDocuments().add(pub);
		processeSentence(annotSetOriginal);
		List<IEntityAnnotation> entitiesToAdd = getEntityAnnotationsIN();
		List<IEventAnnotation> eventsToAdd = getEventAnnotaionIN();
		IAnnotatedDocument docResult = new AnnotatedDocumentImpl(pub,null, null,entitiesToAdd,eventsToAdd);
		getDocumentEventAnnotations().put(pub.getId(),docResult);
		List<IEntityAnnotation> entitiesToDuplicate = new ArrayList<>();
		for(IEntityAnnotation entity : entitiesToAdd){
			IEntityAnnotation newEntity = entity.clone();
			newEntity.generateNewId();
			entitiesToDuplicate.add(newEntity);
		}
		IAnnotatedDocument docNERResult = new AnnotatedDocumentImpl(pub,null, null,entitiesToDuplicate);
		getDocumentEntityAnnotations().put(pub.getId(),docNERResult);
		gateDoc.cleanup();
		}catch(InvalidOffsetException | ResourceInstantiationException e){
			throw new ANoteException(e);
		}
	}

	private void processeSentence(AnnotationSet annotSetOriginal) throws ANoteException {

		long[][] articleAbstractLimits = GateCorpusReaderLoader.getGenericOffsetLimits(annotSetOriginal, "AbstractText",false);
		AnnotationSet annotationAbtsract = annotSetOriginal.getContained(articleAbstractLimits[0][0],articleAbstractLimits[0][1]);
		long[][] sentencesLimits = GateCorpusReaderLoader.getGenericOffsetLimits(annotationAbtsract,"sentence",true);
		long[][] sentencesWhitEventLimits = getSentenceLimits(annotationAbtsract);
		long sentenceIndex = sentencesLimits[0][0];
		long error = sentencesLimits[0][0];
		String sentenceText = new String();
		String sentencePlus = new  String();
		for(int i=0;i<sentencesWhitEventLimits.length;i++)
		{
			sentencePlus = GateCorpusReaderLoader.getPartGateDocument(gateDoc,sentencesLimits[i][0], sentencesLimits[i][1]);
			sentenceText = sentenceText + sentencePlus;
			String[][] terms = processSentenceEntities(annotationAbtsract,sentencesLimits[i][0],sentencesLimits[i][1],sentenceIndex,error);
			processSentenceEvents(annotationAbtsract,sentencesWhitEventLimits[i][0],sentencesWhitEventLimits[i][1],sentenceIndex,error,terms,sentencePlus);
			sentenceIndex = sentenceIndex+sentencesLimits[i][1]-sentencesLimits[i][0];
		}
		this.fulltext = sentenceText;
	}
	

	private void processSentenceEvents(AnnotationSet annotSetOriginal, long startsentenceEvent,long endsentenceEvent, long sentenceIndex, long error,String[][] terms,String textToCkeck) {

		AnnotationSet annotationSentence = annotSetOriginal.getContained(startsentenceEvent,endsentenceEvent);
		long[][] eventsOffset = GateCorpusReaderLoader.getGenericOffsetLimits(annotationSentence, "event",true);
		long verbPositionEnd,verbPositionStart;
		IEventProperties eventProperties = new EventPropertiesImpl();
		// eventID -> ArrayList<> lefrentities,ArrayList<> rigth entities
		Map<String, IGenericPair<List<String>, List<String>>> hasheventwhitentities = new HashMap<String, IGenericPair<List<String>,List<String>>>();
		// eventID -> ( Verb(clue),ontologyRelation,offsetStart)
		Map<String,GenericTriple<String,String,Long>> hashEventdetails = new HashMap<String, GenericTriple<String,String,Long>>();
		// ID Genia Event Entity ID-> EntityInfo
		Map<String, String[]> hashentities = getHashTermsIDentityID(terms);
		for(int i=eventsOffset.length-1;i>=0;i--)
		{
			getEventDetails(gateDoc, annotationSentence, eventsOffset,hasheventwhitentities, hashEventdetails,i,textToCkeck);
		}
		Iterator<String> iteventID = hasheventwhitentities.keySet().iterator();
		while(iteventID.hasNext())
		{
			String eventID = iteventID.next();
			String verb = hashEventdetails.get(eventID).getX();
			verbPositionStart =  hashEventdetails.get(eventID).getZ();
			verbPositionEnd =  hashEventdetails.get(eventID).getZ()+verb.length();
			String ontologyRelationClass = hashEventdetails.get(eventID).getY();
			List<String> cause = hasheventwhitentities.get(eventID).getX();
			List<String> them = hasheventwhitentities.get(eventID).getY();
			List<IEntityAnnotation> causeEntities = getEntitiesList(cause,hasheventwhitentities,sentenceIndex,startsentenceEvent,hashentities);
			List<IEntityAnnotation> themEntities = getEntitiesList(them,hasheventwhitentities,sentenceIndex,startsentenceEvent,hashentities);
			IGenericPair<List<IEntityAnnotation>,List<IEntityAnnotation>> entitiesResults = getEntitiesPosition(sentenceIndex+verbPositionStart-error, causeEntities, themEntities);
			if(!(entitiesResults.getX().isEmpty() && entitiesResults.getY().isEmpty())){
				IEventAnnotation ev = new EventAnnotationImpl(sentenceIndex+verbPositionStart-error,sentenceIndex+verbPositionEnd-error, AnnotationType.re.name(), entitiesResults.getX(), entitiesResults.getY(), verb, 0, ontologyRelationClass, eventProperties);
				this.events.add(ev);
			}
		}		
	}
	
	private IGenericPair<List<IEntityAnnotation>,List<IEntityAnnotation>> getEntitiesPosition(
			Long verbPosition,
			List<IEntityAnnotation> cause,
			List<IEntityAnnotation> them)
			{
		Set<IEntityAnnotation> left = new HashSet<IEntityAnnotation>();
		Set<IEntityAnnotation> right = new HashSet<IEntityAnnotation>();
		for(int i=0;i<cause.size();i++)
		{
			IEntityAnnotation pair = cause.get(i);
			if(pair.getStartOffset()<verbPosition)
				left.add(pair);
			else
				right.add(pair);
		}
		for(int i=0;i<them.size();i++)
		{
			IEntityAnnotation pair = them.get(i);
			if(pair.getStartOffset()<verbPosition)
				left.add(pair);
			else
				right.add(pair);
		}	
		return new GenericPairImpl<List<IEntityAnnotation>,List<IEntityAnnotation>>(new ArrayList<>(left),new ArrayList<>(right));
	}
	
	private List<IEntityAnnotation> getEntitiesList(List<String> entitiesID, Map<String, IGenericPair<List<String>, List<String>>> hasheventwhitentities,long sentenceIndex, long sentenceStaerEvent, Map<String, String[]> hashentities) {
		List<IEntityAnnotation> list = new ArrayList<IEntityAnnotation>();
		Stack<String> stack = new Stack<String>();
		Set<String> haveProcessed = new HashSet<String>();
		for(String id:entitiesID)
		{
			if(hashentities.get(id) == null)
			{
				stack.push(id);
			}
			else
			{
//				info = hashentities.get(id);
//				classID = classesClassesID.get(info[3]);
//				term = info[1];
//				offsetDiff = Long.valueOf(info[0])-sentenceStaerEvent;
//				e = new EntityAnnotation(-1,sentenceIndex+offsetDiff-error, sentenceIndex+offsetDiff+term.length()-error, classID, 0, term, NormalizationForm.getNormalizationForm(term));
				list.add(this.EntityIDEntity.get(id));
			}
		}
		while(stack.size() > 0){
			String currentId = stack.pop();
			if(hasheventwhitentities.get(currentId)!=null&&haveProcessed.contains(currentId)==false) // porque a sentence pode ser de outra frase e n�o esta na hash
			{
				haveProcessed.add(currentId);
				List<String> cause = hasheventwhitentities.get(currentId).getX();
				List<String> them = hasheventwhitentities.get(currentId).getY();
				for(String id2:cause)
					if(hashentities.get(id2) == null)
						stack.push(id2);
					else
					{
//						info = hashentities.get(id2);
//						classID = classesClassesID.get(info[3]);
//						term = info[1];
//						offsetDiff = Long.valueOf(info[0])-sentenceStaerEvent;
//						e = new EntityAnnotation(-1,sentenceIndex+offsetDiff-error, sentenceIndex+offsetDiff+term.length()-error, classID, 0, term, NormalizationForm.getNormalizationForm(term));
						list.add(this.EntityIDEntity.get(id2));
					}
				for(String id3:them)
					if(hashentities.get(id3) == null)
						stack.push(id3);
					else
					{
//						info = hashentities.get(id3);
//						classID = classesClassesID.get(info[3]);
//						term = info[1];
//						offsetDiff = Long.valueOf(info[0])-sentenceStaerEvent;
//						e = new EntityAnnotation(-1,sentenceIndex+offsetDiff-error, sentenceIndex+offsetDiff+term.length()-error, classID, 0, term, NormalizationForm.getNormalizationForm(term));
						list.add(this.EntityIDEntity.get(id3));
					}
			}
		}
		return list;
	}


	/**
	 * Method that receive ner annotations
	 * 
	 * @param annotations
	 * @return
	 */
	private Map<String,String[]> getHashTermsIDentityID(String[][] annotations)
	{
		HashMap<String,String[]> hashentities = new HashMap<String,String[]>();
		for(int i=0;i<annotations.length;i++)
		{
			hashentities.put(annotations[i][2],annotations[i]);
		}
		return hashentities;
	}

	/**
	 * Method that give event entities and event details (verb(clue) an ontological classification)
	 * 
	 * 
	 * @param gateDoc
	 * @param annotSentenceEvent
	 * @param eventsOffset
	 * @param hasheventwhitentities
	 * @param hashEventdetails
	 * @param i
	 */
	private void getEventDetails( Document gateDoc, AnnotationSet annotSentenceEvent, long[][] eventsOffset,
			Map<String, IGenericPair<List<String>, List<String>>> hasheventwhitentities,
			Map<String, GenericTriple<String, String, Long>> hashEventdetails,
			int i,String textToCkeck) {
		AnnotationSet annotSentenceEvents;
		annotSentenceEvents = annotSentenceEvent.getContained(eventsOffset[i][0],eventsOffset[i][1]);
		AnnotationSet annotEvent = annotSentenceEvents.get("event");
		if(annotEvent.size()>0)
		{
			Iterator<Annotation> itEvent = annotEvent.iterator();
			String eventID = (String) itEvent.next().getFeatures().get("id");
			annotEvent = annotSentenceEvents.get("clueType");
			if(annotEvent.size()>0)
			{
				Iterator<Annotation> itVerb = annotEvent.iterator();
				Annotation annot = itVerb.next();
				Long statVerb = annot.getStartNode().getOffset();
				String verb = GateCorpusReaderLoader.getPartGateDocument(gateDoc,statVerb, annot.getEndNode().getOffset());
				annotEvent = annotSentenceEvents.get("type");
				Iterator<Annotation> itontologyRelationClass = annotEvent.iterator();
				String ontologyRelationClass = (String) itontologyRelationClass.next().getFeatures().get("class");
				processEvent(hasheventwhitentities,annotSentenceEvents,eventID);
				long st = statVerb-eventsOffset[i][0];
				long ed = statVerb-eventsOffset[i][0]+verb.length();
				if(textToCkeck.substring((int)st,(int)ed).equals(verb))
				{
					hashEventdetails.put(eventID, new GenericTriple<String, String,Long>(verb,ontologyRelationClass,statVerb-eventsOffset[i][0]));
				}
				else
				{
					for(int j=1;j<15&&textToCkeck.length()>ed+j;j++)
					{
						if(textToCkeck.substring((int)st+j,(int)ed+j).equals(verb))
						{
							hashEventdetails.put(eventID, new GenericTriple<String, String,Long>(verb,ontologyRelationClass,statVerb-eventsOffset[i][0]+j));
							return;
						}
					}
					for(int j=1;j<15&&st-j>=0;j++)
					{
						if(textToCkeck.substring((int)st-j,(int)ed-j).equals(verb))
						{
							hashEventdetails.put(eventID, new GenericTriple<String, String,Long>(verb,ontologyRelationClass,statVerb-eventsOffset[i][0]-j));
							return;
						}
					}
					hashEventdetails.put(eventID, new GenericTriple<String, String,Long>(verb,ontologyRelationClass,statVerb-eventsOffset[i][0]));
				}
			}
		}
	}
	
	/**
	 * 
	 * Method that add to Hash a new event entities at rigth and left
	 * 
	 * 
	 * @param hasheventwhitentities
	 * @param annotEvent
	 * @param eventID
	 */
	private void processEvent(Map<String,IGenericPair<List<String>,List<String>>> hasheventwhitentities,AnnotationSet annotEvent,String eventID)
	{
		List<String> leftEntities = new ArrayList<String>();
		List<String> rightEntities = new ArrayList<String>();
		
		AnnotationSet cause =  annotEvent.get("cause");	
		if(cause.size()>0)
		{
			Iterator<Annotation> itAnnot = cause.iterator();
			Annotation annot = itAnnot.next();
			String entLeft1 = (String) annot.getFeatures().get("idref");
			String entLeft2 = (String) annot.getFeatures().get("idref1");
			if(entLeft1!=null)
			{
				leftEntities.add(entLeft1);
			}
			if(entLeft2!=null)
			{
				leftEntities.add(entLeft2);
			}		
		}
		AnnotationSet theme =  annotEvent.get("theme");
		if(theme.size()>0)
		{
			Iterator<Annotation> itAnnot = theme.iterator();
			Annotation annot = itAnnot.next();
			String entRight1 = (String) annot.getFeatures().get("idref");
			String entRight2 = (String) annot.getFeatures().get("idref1");
			if(entRight1!=null)
			{
				rightEntities.add(entRight1);
			}
			if(entRight2!=null)
			{
				rightEntities.add(entRight2);
			}
		}	
		hasheventwhitentities.put(eventID, new GenericPairImpl<List<String>, List<String>>(leftEntities,rightEntities));
	}


	/**
	 * Method that extract all events from sentence (start,end offsets)
	 * 
	 * @param pmid
	 * @param gateDoc
	 * @param annotSetOriginal
	 * @param start
	 * @param end
	 * @throws ANoteException 
	 * @throws DatabaseLoadDriverException 
	 * @throws SQLException 
	 */
	private String[][] processSentenceEntities(AnnotationSet annotSetOriginal, long start, long end,long sentenceIndex,long error) throws ANoteException 
	{

		AnnotationSet annotSentenceEvent = annotSetOriginal.getContained(start,end);
		long startEntity,endEntity,offsetDif,offsetFinalDif;	
		String[][] terms = processTermSentence(annotSentenceEvent);
		for(int i=0;i<terms.length;i++)
		{
			startEntity = Long.valueOf(terms[i][0]);
			endEntity = Long.valueOf(terms[i][1]);
			String entity = GateCorpusReaderLoader.getPartGateDocument(gateDoc, startEntity, endEntity);
			terms[i][1]=entity; /// start,entity,id,sem
			offsetDif = startEntity-start;
			offsetFinalDif = endEntity-start; 
			IAnoteClass klassToAdd = new AnoteClass(terms[i][3]);
			IAnoteClass klass = ClassPropertiesManagement.getClassIDOrinsertIfNotExist(klassToAdd);
			IEntityAnnotation e = new EntityAnnotationImpl(sentenceIndex+offsetDif-error, sentenceIndex+offsetFinalDif-error, klass,null, terms[i][1], false, null);
			this.EntityIDEntity.put(terms[i][2],e);
		}
		return terms;
	}
	

	
	/**
	 * Method that find all entities on sentence
	 * 
	 * @param annotSentencewhitinsentence
	 * @return
	 */
	private String[][] processTermSentence(AnnotationSet annotSentencewhitinsentence) 
	{
		Annotation annot;
		annotSentencewhitinsentence = annotSentencewhitinsentence.get("term");
		String[][] terms = new String[annotSentencewhitinsentence.size()][4]; // startoffset,end,id,sem
		Iterator<Annotation> itAnnotTerms =  annotSentencewhitinsentence.iterator();
		int i=0;
		while(itAnnotTerms.hasNext())
		{
			annot=itAnnotTerms.next();
			long start = annot.getStartNode().getOffset();
			long end = annot.getEndNode().getOffset();
			String id = (String) annot.getFeatures().get("id");
			String sem = (String) annot.getFeatures().get("sem");
			terms[i][0]= String.valueOf(new Long(start));
			terms[i][1]= String.valueOf(new Long(end));
			terms[i][2]= id;
			if(sem==null)
			{
				sem="unknown";
			}
			terms[i][3]= sem;
			i++;
		}
		return terms;
	}
	
	/** 
	 * Method that return a sentenceLimits ( sentence + events ) for order offset
	 * 
	 * @param annotSetOriginal
	 * @return
	 */
	private long[][] getSentenceLimits(AnnotationSet annotSetOriginal)
	{
		long[][] sentenceLimits = GateCorpusReaderLoader.getGenericOffsetLimits(annotSetOriginal,"sentence",true);
		long[][] articlesLimits = GateCorpusReaderLoader.getGenericOffsetLimits(annotSetOriginal,"Article",false);
		for(int i=0;i<sentenceLimits.length-1;i++)
		{
			sentenceLimits[i][1]=sentenceLimits[i+1][0];
		}		
		sentenceLimits[sentenceLimits.length-1][1]=articlesLimits[0][1];		
		return sentenceLimits;
	}

	private String getDocumentPMID(Document gateDoc,AnnotationSet annotSetOriginal) throws InvalidOffsetException {
		return GateCorpusReaderLoader.getGeneralArticleInfo(gateDoc, annotSetOriginal,"PMID");
	}

	private String getDocumentAbstract(Document gateDoc,AnnotationSet annotSetOriginal) throws InvalidOffsetException {	
		String abstractText = new String(),sentence = new String();
		long[][] articleAbstractLimits = GateCorpusReaderLoader.getGenericOffsetLimits(annotSetOriginal, "AbstractText",false);
		AnnotationSet annotationAbtsract = annotSetOriginal.getContained(articleAbstractLimits[0][0],articleAbstractLimits[0][1]);
		SortedMap<GenericPairComparable<Long, Long>, Annotation> annotations = GateCorpusReaderLoader.getGenericOrderAnnotations(annotationAbtsract, "sentence");
		Iterator<GenericPairComparable<Long, Long>> it = annotations.keySet().iterator();
		GenericPairComparable<Long, Long> pos;
		while(it.hasNext())
		{
			pos = it.next();
			sentence= GateCorpusReaderLoader.getPartGateDocument(gateDoc,pos.getX(),pos.getY());
			abstractText = abstractText.concat(sentence.substring(0, sentence.length()-2)+". ");
		}
		return abstractText;
	}

	private String getDocumentTitle(Document gateDoc,AnnotationSet annotSetOriginal) throws InvalidOffsetException {
		String title = new String(),sentence = new String();
		long[][] articleTitleLimits = GateCorpusReaderLoader.getGenericOffsetLimits(annotSetOriginal, "ArticleTitle",false);
		AnnotationSet annotationAbtsract = annotSetOriginal.getContained(articleTitleLimits[0][0],articleTitleLimits[0][1]);
		SortedMap<GenericPairComparable<Long, Long>, Annotation> annotations = GateCorpusReaderLoader.getGenericOrderAnnotations(annotationAbtsract, "sentence");
		Iterator<GenericPairComparable<Long, Long>> it = annotations.keySet().iterator();
		GenericPairComparable<Long, Long> pos;
		while(it.hasNext())
		{
			pos = it.next();
			sentence= GateCorpusReaderLoader.getPartGateDocument(gateDoc,pos.getX(),pos.getY());
			title = title.concat(sentence);
		}
		return title;
	}

	public boolean validateFile(File filepath) {
		if(filepath.isDirectory())
		{
			corruptFiles = new ArrayList<String>();
			for(File file:filepath.listFiles())
			{
				validateOneFile(file);
			}
			if(corruptFiles.size()>0){
				return false;
			}
			else{
				return true;
			}
		}
		else{
			return false;
		}
	}

	private void validateOneFile(File file) {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		org.w3c.dom.Document doc;
		try {
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(file);
			doc.getDocumentElement().normalize();
			NodeList nList = doc.getElementsByTagName("*");
			for (int temp = 0; temp < nList.getLength(); temp++) {	 
				   Node nNode = nList.item(temp);
				   if(!validTags.contains(nNode.getNodeName()))
				   {
					   corruptFiles.add(file.getAbsolutePath());
					   return;
				   }
			}			
		} catch (SAXException e) {
			corruptFiles.add(file.getAbsolutePath());
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			corruptFiles.add(file.getAbsolutePath());
			e.printStackTrace();
		}	
	}

	public List<String> getCorruptFiles() {
		return corruptFiles;
	}

	public int corpusSize() {
		return corpusSize;
	}

	
	public int corpusLoadPosition() {
		return corpusLoaderPosition;
	}

	public List<IPublication> getDocuments() {
		return documents;
	}
	
	public List<IEntityAnnotation> getEntityAnnotationsIN()
	{
		return new ArrayList<IEntityAnnotation>(EntityIDEntity.values());
	}
	
	public List<IEventAnnotation> getEventAnnotaionIN()
	{
		return this.events;
	}
	
	public String getFulltext() {
		return fulltext;
	}

	public Map<String, Integer> getClassesClassesID() {
		return classesClassesID;
	}

	@Override
	public void stop() {
		this.stop = true;		
	}
	
	@Override
	public Map<Long, IAnnotatedDocument> getDocumentEntityAnnotations() {
		return documentsWithEntities;
	}

	@Override
	public Map<Long, IAnnotatedDocument> getDocumentEventAnnotations() {
		return documentsWithEvents;
	}


}
