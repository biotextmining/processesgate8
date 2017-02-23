package com.silicolife.textmining.ie.re.relation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.silicolife.textmining.core.datastructures.annotation.AnnotationPosition;
import com.silicolife.textmining.core.datastructures.annotation.AnnotationPositions;
import com.silicolife.textmining.core.datastructures.annotation.AnnotationType;
import com.silicolife.textmining.core.datastructures.annotation.re.EventAnnotationImpl;
import com.silicolife.textmining.core.datastructures.dataaccess.database.dataaccess.CorpusProcessAnnotationLogs;
import com.silicolife.textmining.core.datastructures.documents.AnnotatedDocumentImpl;
import com.silicolife.textmining.core.datastructures.exceptions.process.InvalidConfigurationException;
import com.silicolife.textmining.core.datastructures.general.ClassPropertiesManagement;
import com.silicolife.textmining.core.datastructures.init.InitConfiguration;
import com.silicolife.textmining.core.datastructures.process.ProcessOriginImpl;
import com.silicolife.textmining.core.datastructures.report.processes.REProcessReportImpl;
import com.silicolife.textmining.core.datastructures.utils.FileHandling;
import com.silicolife.textmining.core.datastructures.utils.GenerateRandomId;
import com.silicolife.textmining.core.datastructures.utils.GenericPairImpl;
import com.silicolife.textmining.core.datastructures.utils.Utils;
import com.silicolife.textmining.core.datastructures.utils.conf.GlobalNames;
import com.silicolife.textmining.core.datastructures.utils.conf.GlobalOptions;
import com.silicolife.textmining.core.interfaces.core.annotation.AnnotationLogTypeEnum;
import com.silicolife.textmining.core.interfaces.core.annotation.IAnnotationLog;
import com.silicolife.textmining.core.interfaces.core.annotation.IEntityAnnotation;
import com.silicolife.textmining.core.interfaces.core.annotation.IEventAnnotation;
import com.silicolife.textmining.core.interfaces.core.dataaccess.exception.ANoteException;
import com.silicolife.textmining.core.interfaces.core.document.IAnnotatedDocument;
import com.silicolife.textmining.core.interfaces.core.document.IDocumentSet;
import com.silicolife.textmining.core.interfaces.core.document.IPublication;
import com.silicolife.textmining.core.interfaces.core.document.structure.ISentenceSintaxRepresentation;
import com.silicolife.textmining.core.interfaces.core.general.classe.IAnoteClass;
import com.silicolife.textmining.core.interfaces.core.report.processes.IREProcessReport;
import com.silicolife.textmining.core.interfaces.process.IProcessOrigin;
import com.silicolife.textmining.core.interfaces.process.IE.IIEProcess;
import com.silicolife.textmining.core.interfaces.process.IE.IREProcess;
import com.silicolife.textmining.core.interfaces.process.IE.re.IREConfiguration;
import com.silicolife.textmining.core.interfaces.process.IE.re.IRelationModel;
import com.silicolife.textmining.ie.re.relation.configuration.IRERelationAdvancedConfiguration;
import com.silicolife.textmining.ie.re.relation.configuration.IRERelationConfiguration;
import com.silicolife.textmining.ie.re.relation.configuration.RERelationNames;
import com.silicolife.textmining.ie.re.relation.datastructures.POSTaggerHelp;
import com.silicolife.wrappergate.IGatePosTagger;

import gate.util.GateException;


public class RelationsExtraction implements IREProcess{
	
	private Pattern findOffset = Pattern.compile("(\\d+)-(\\d+)");

	public final static String relationName = "Rel@tion RE";
	public final static IProcessOrigin relationProcessType = new ProcessOriginImpl(GenerateRandomId.generateID(),relationName);
	
	protected static final int characteres = 500000;
	private boolean stop=false;

	private IGatePosTagger posTagger;
	private static final String tempfilename = "refileTmp.txt";
	
	
	public RelationsExtraction()
	{

	}

	private static Properties gerateProperties(IRERelationConfiguration configuration) {
		Properties prop = new Properties();
		prop.putAll(configuration.getRelationModelEnum().getRelationModel(configuration).getProperties());
		prop.put(GlobalNames.taggerName,String.valueOf(configuration.getPosTaggerEnum().toString()));
		prop.put(GlobalNames.entityBasedProcess,String.valueOf(configuration.getEntityBasedProcess().getId()));
		prop.put(GlobalNames.relationModel,configuration.getRelationModelEnum().toString());
		prop.putAll(configuration.getPosTaggerEnum().getPOSTagger(configuration.getVerbsFilter(), configuration.getVerbsAddition()).getProperties());
		if(configuration.getEntityBasedProcess().getProperties().containsKey(GlobalNames.normalization))
		{
			if(Boolean.valueOf(configuration.getEntityBasedProcess().getProperties().getProperty(GlobalNames.normalization)))
			{
				prop.put(GlobalNames.normalization, configuration.getEntityBasedProcess().getProperties().getProperty(GlobalNames.normalization));
			}
		}
		if(configuration.getRelationModelEnum().getRelationModel(configuration).getDescription().equals("Binary Selected Verbs Only"))
		{
			
			prop.put(GlobalNames.verbAdditionOnly,String.valueOf(configuration.getVerbsClues().getId()));
			prop.remove(GlobalNames.verbAddition);
		}
		IRERelationAdvancedConfiguration advancedConfiguration = configuration.getAdvancedConfiguration();
		if(advancedConfiguration!=null)
		{
			if(advancedConfiguration.isUsingOnlyVerbNearestEntities())
			{
				prop.put(RERelationNames.usingOnlyVerbNearestEntities,"true");
			}
			else if(advancedConfiguration.isUsingOnlyEntitiesNearestVerb())
			{
				prop.put(RERelationNames.usingOnlyEntitiesNearestVerb,"true");
			}
			else if(advancedConfiguration.isUsingVerbEntitiesDistance())
			{
				if(advancedConfiguration.getVerbEntitieMaxDistance()>0)
				{
					prop.put(RERelationNames.usingVerbEntitiesDistance,"true");
					prop.put(RERelationNames.verbEntitiesDistance,String.valueOf(advancedConfiguration.getVerbEntitieMaxDistance()));
				}
			}
			if(advancedConfiguration.getRelationsType()==null || advancedConfiguration.getRelationsType().size() == 0)
			{
				prop.put(RERelationNames.relationsTypeSelected,"All");
			}
			else if(advancedConfiguration.getRelationsType()!=null )
			{
				prop.put(RERelationNames.relationsTypeSelected,"Filter");
			}
		}
		return prop;
	}
	

	public IREProcessReport executeRE(IREConfiguration configuration) throws  ANoteException, InvalidConfigurationException {
		validateConfiguration(configuration);
		IRERelationConfiguration reConfiguration = (IRERelationConfiguration) configuration;
		IIEProcess reProcess = buildprocess(configuration, reConfiguration);
		InitConfiguration.getDataAccess().createIEProcess(reProcess);
		InitConfiguration.getDataAccess().registerCorpusProcess(reConfiguration.getCorpus(), reProcess);
		IRelationModel relationModel = reConfiguration.getRelationModelEnum().getRelationModel(reConfiguration);
		posTagger = reConfiguration.getPosTaggerEnum().getPOSTagger(reConfiguration.getVerbsFilter(),reConfiguration.getVerbsAddition());
		IIEProcess processToRetriveMC = reConfiguration.getManualCurationFromOtherProcess();
		IREProcessReport report = new REProcessReportImpl(relationName, reProcess,reProcess,processToRetriveMC != null);
		long startTime = GregorianCalendar.getInstance().getTimeInMillis();
		try {
			relationProcessing(relationModel,reProcess,reConfiguration,report);
		} catch (IOException | GateException e) {
			throw new ANoteException(e);
		}
		long end = GregorianCalendar.getInstance().getTimeInMillis();
		report.setTime(end-startTime);
		return report;
	}

	private IIEProcess buildprocess(IREConfiguration configuration, IRERelationConfiguration reConfiguration) {
		IIEProcess reProcess = configuration.getIEProcess();
		reProcess.setName(relationName+" "+Utils.SimpleDataFormat.format(new Date()));
		reProcess.setProperties(gerateProperties(reConfiguration));
		return reProcess;
	}

	protected void relationProcessing(IRelationModel relationModel,IIEProcess reProcess,IRERelationConfiguration configuration,IREProcessReport report) throws IOException, GateException, ANoteException {
		IDocumentSet docs = configuration.getCorpus().getArticlesCorpus();
		Iterator<IPublication> itDocs =docs.iterator();
		int max = docs.size();
		StringBuffer stringWhitManyDocuments = new StringBuffer();
		int size = 0;
		int docInDocumentGate = 0;
		int position = 0;
		long starttime = GregorianCalendar.getInstance().getTimeInMillis();
		Map<Long, GenericPairImpl<Integer, Integer>> docLimits = new HashMap<Long, GenericPairImpl<Integer,Integer>>();
		while(itDocs.hasNext())
		{
			if(stop)
			{
				report.setcancel();
				break;
			}
			docInDocumentGate++;
			IPublication doc = itDocs.next();
			IAnnotatedDocument annotDoc = new AnnotatedDocumentImpl(doc,reProcess, configuration.getCorpus());
			String text = annotDoc.getDocumentAnnotationText();
			docLimits.put(doc.getId(), new GenericPairImpl<>(size, size+text.length()-1));
			size = size+text.length();
			stringWhitManyDocuments.append(Utils.treatSentenceForXMLProblems(text));
			if(text == null || text.length() == 0)
			{
//				Logger logger = Logger.getLogger(Workbench.class.getName());
//				logger.warn("Not available text for publication whit id = "+doc.getId()+" for NER Process");
			}
			else
			{
				if(stop)
				{
					report.setcancel();
					break;
				}
				else if(size>characteres && !stop)
				{
					documetsRelationExtraction(relationModel,reProcess,configuration,docLimits,report,stringWhitManyDocuments);
					stringWhitManyDocuments = new StringBuffer();
					size=0;
					position = position + docInDocumentGate;
					docInDocumentGate=0;
					docLimits = new HashMap<Long, GenericPairImpl<Integer,Integer>>();
					memoryAndProgressAndTime(position, max,starttime);
				}

			}
			report.incrementDocument();
		}
		if(stringWhitManyDocuments.length()>0 && !stop)
		{
			documetsRelationExtraction(relationModel,reProcess,configuration,docLimits,report,stringWhitManyDocuments);
		}
		cleanDocument();
	}

	private void cleanDocument() throws IOException, GateException {	
		posTagger.cleanALL();
	}

	protected void documetsRelationExtraction(IRelationModel relationModel,IIEProcess process,IRERelationConfiguration configuration,Map<Long, GenericPairImpl<Integer, Integer>> docLimits, IREProcessReport report, StringBuffer stringWhitManyDocuments) throws GateException, ANoteException, IOException {
		Set<String> termionations = relationModel.getRelationTerminations();
		if(!stop)
		{
			File fileTmp = new File(tempfilename);
			String fileText = stringWhitManyDocuments.toString();
			FileHandling.writeInformationOnFile(fileTmp,fileText);
			posTagger.completePLSteps(fileTmp);
			CorpusProcessAnnotationLogs annotationLogs = null;
			if(configuration.getManualCurationFromOtherProcess()!=null)
				annotationLogs = new CorpusProcessAnnotationLogs(configuration.getManualCurationFromOtherProcess(),true);
			for(long documentID:docLimits.keySet())
			{
				if(stop)
				{
					report.setcancel();
					break;
				}
				long documentStartOffset = new Long(docLimits.get(documentID).getX());
				long documentEndOffset = new Long(docLimits.get(documentID).getY());
				IPublication doc = configuration.getCorpus().getArticlesCorpus().getDocument(documentID);
				IAnnotatedDocument annotDoc = new AnnotatedDocumentImpl(doc,process, configuration.getCorpus());
				IAnnotatedDocument annotDocSourceNER = new AnnotatedDocumentImpl(doc,configuration.getEntityBasedProcess(), configuration.getCorpus());
				List<GenericPairImpl<Long, Long>> sentencesLimits = POSTaggerHelp.getGateDocumentSentencelimits(posTagger.getGateDoc(),documentStartOffset,documentEndOffset);	
				List<IEntityAnnotation> allDoucmentSemanticLayer = annotDocSourceNER.getEntitiesAnnotations();
				// NER Manual Curation
				if(annotationLogs!=null)
					allDoucmentSemanticLayer = applyMCToNERTerms(documentID,annotationLogs,allDoucmentSemanticLayer,report);
				List<IEntityAnnotation> sentencesemanticLayer = null;
				List<IEventAnnotation> relations = new ArrayList<IEventAnnotation>();
				ISentenceSintaxRepresentation sentenceRepresentation;
				for(GenericPairImpl<Long, Long> setenceLimits:sentencesLimits)
				{
					long sentenceStartOffset = setenceLimits.getX();
					long sentenceEndOffset = setenceLimits.getY();
					sentenceRepresentation = posTagger.getSentenceSintaticLayer(termionations,setenceLimits,documentStartOffset);
					sentencesemanticLayer = getNERProcessEntityAnnotationsSentence(allDoucmentSemanticLayer,sentenceStartOffset-documentStartOffset,sentenceEndOffset-documentStartOffset);
					List<IEventAnnotation> relationsAux = relationModel.extractSentenceRelation(annotDoc,sentencesemanticLayer, sentenceRepresentation);
					relations.addAll(relationsAux);
				}
//				 RE Manual Curation
				if(annotationLogs!=null)
					relations = applyMCToRE(documentID,annotationLogs,report,relations,allDoucmentSemanticLayer);
				// Insert Entities and Relations in Database
				insertAnnotationsInDatabse(process,report,annotDoc,allDoucmentSemanticLayer,relations);
			}
			fileTmp.delete();
		}
	}
	
	

	private void insertAnnotationsInDatabse(IIEProcess process,IREProcessReport report,IAnnotatedDocument annotDoc,List<IEntityAnnotation> entitiesList,List<IEventAnnotation> relationsList) throws ANoteException {
		// Generate new Ids for Entities
		for(IEntityAnnotation entity:entitiesList)
		{
			entity.generateNewId();
		}
		InitConfiguration.getDataAccess().addProcessDocumentEntitiesAnnotations(process, annotDoc, entitiesList);
		report.incrementEntitiesAnnotated(entitiesList.size());
		InitConfiguration.getDataAccess().addProcessDocumentEventAnnoations(process, annotDoc,relationsList);
		report.increaseRelations(relationsList.size());
	}

	private List<IEventAnnotation> applyMCToRE(long documentID, CorpusProcessAnnotationLogs annotationLogs,IREProcessReport report, List<IEventAnnotation> relations, List<IEntityAnnotation> sentencesemanticLayer) throws ANoteException {
		SortedSet<IAnnotationLog> documentAnnotations = annotationLogs.getDocumentRELogAnnotation(documentID);
		AnnotationPositions entAnnotPositions = new AnnotationPositions();
		for(IEntityAnnotation entAnnot : sentencesemanticLayer)
		{
			entAnnotPositions.addAnnotationWhitConflicts(new AnnotationPosition((int)entAnnot.getStartOffset(), (int)entAnnot.getEndOffset()), entAnnot);
		}
		for(IAnnotationLog annotationLog : documentAnnotations)
		{
			applyChangesToAnnotation(annotationLog,relations,entAnnotPositions,annotationLogs,report);
		}
		return relations;
	}

	private void applyChangesToAnnotation(IAnnotationLog annotationLog,List<IEventAnnotation> relations,
			AnnotationPositions entAnnotPositions,
			CorpusProcessAnnotationLogs annotationLogs, IREProcessReport report) throws ANoteException {
		if(annotationLog.getType().equals(AnnotationLogTypeEnum.RELATIONREMOVE))
		{
			removeEventRElationAnnotation(annotationLog,relations, entAnnotPositions, annotationLogs,report);
		}
		else if(annotationLog.getType().equals(AnnotationLogTypeEnum.RELATIONADD))
		{
			addEventAnnotation(annotationLog, relations,entAnnotPositions, annotationLogs,report);
		}
		else if(annotationLog.getType().equals(AnnotationLogTypeEnum.RELATIONUPDATE))
		{
			editEventAnnotation(annotationLog, relations,entAnnotPositions, annotationLogs,report);
		}
		
	}
	
	private void editEventAnnotation(IAnnotationLog annotationLog, List<IEventAnnotation> relations,
			AnnotationPositions entAnnotPositions,CorpusProcessAnnotationLogs annotations,IREProcessReport report) throws ANoteException {
		IEventAnnotation eventUpdated = annotations.getEventRelationAnnotationIDEventAnnotation().get(annotationLog.getOriginalAnnotationID());
		IEventAnnotation eventToEdit = eventsMatching(eventUpdated,relations);
		if(eventToEdit==null)
		{
			IEventAnnotation oldRelationWithouMactinhEntities = calculatesOldRelation(annotationLog,eventUpdated,entAnnotPositions);
			if(oldRelationWithouMactinhEntities!=null)
			{
				IEventAnnotation oldRelation = eventsMatching(oldRelationWithouMactinhEntities,relations);
				if(oldRelation!=null)
				{
					long oldID = oldRelation.getId();
					IEventAnnotation oldRelationWithEntities = tryfindEntities(eventUpdated,entAnnotPositions);
					if(oldRelationWithEntities!=null)
					{		
						eventToEdit = new EventAnnotationImpl(oldID, oldRelationWithEntities.getStartOffset(), oldRelationWithEntities.getEndOffset(), AnnotationType.re.name(),
								oldRelationWithEntities.getEntitiesAtLeft(),
								oldRelationWithEntities.getEntitiesAtRight(), oldRelationWithEntities.getEventClue(),oldRelationWithEntities.getEventProperties(),true,true);
						
						report.getRESchemachemaWithManualCurationReport().addChangedEvent(annotationLog);
						annotations.getEventRelationAnnotationIDEventAnnotation().put(oldRelation.getId(), eventToEdit);
						relations.remove(oldRelation);
						relations.add(eventToEdit);
					}
					else
					{
						report.getRESchemachemaWithManualCurationReport().addMissingAnnotationByMissingEntities(annotationLog);
					}
				}
				else
				{
					report.getRESchemachemaWithManualCurationReport().addMissingAnnotation(annotationLog);
				}
			}
			else
			{
				report.getRESchemachemaWithManualCurationReport().addMissingAnnotation(annotationLog);
			}
		}
		else // Already have the relation
		{
			report.getRESchemachemaWithManualCurationReport().addMissingAnnotationAlreadyAnnotated(annotationLog);
		}
	}
	
	private IEventAnnotation calculatesOldRelation(IAnnotationLog annotationLog,IEventAnnotation eventUpdated, AnnotationPositions entAnnotPositions) {
		
		List<IEntityAnnotation> entitiesAtRight = new ArrayList<IEntityAnnotation>();
		List<IEntityAnnotation> entitiesAtLeft = new ArrayList<IEntityAnnotation>();
		String entitiesAtLeftStream = annotationLog.getOldString().split("Entities At Left")[1].split("Entities At Right")[0];
		String entitiesAtRightStream = annotationLog.getOldString().split("Entities At Left")[1].split("Entities At Right")[1];
		
		List<AnnotationPosition> posLEft = calculateFromStream(entitiesAtLeftStream);
		List<AnnotationPosition> posRight = calculateFromStream(entitiesAtRightStream);
		for(AnnotationPosition pos:posLEft)
		{
			if(entAnnotPositions.containsKey(pos))
			{
				entitiesAtLeft.add((IEntityAnnotation)entAnnotPositions.get(pos));
			}
			else
			{
				return null;
			}
		}
		for(AnnotationPosition pos:posRight)
		{
			if(entAnnotPositions.containsKey(pos))
			{
				entitiesAtRight.add((IEntityAnnotation)entAnnotPositions.get(pos));
			}
			else
			{
				return null;
			}
		}
		IEventAnnotation result = new EventAnnotationImpl(eventUpdated.getStartOffset(), eventUpdated.getEndOffset(), AnnotationType.re.name(),
				entitiesAtLeft,entitiesAtRight, eventUpdated.getEventClue(), eventUpdated.getEventProperties(),true);
		return result;
	}
	
	private List<AnnotationPosition> calculateFromStream(String entitiesAtLeftStream) {
		List<AnnotationPosition> result = new ArrayList<AnnotationPosition>();
		Matcher m = findOffset.matcher(entitiesAtLeftStream);
		while(m.find())
		{
			int start = Integer.valueOf(m.group(1));
			int end = Integer.valueOf(m.group(2));
			result.add(new AnnotationPosition(start ,end));
		}
		return result;
	}
	
	private void addEventAnnotation(IAnnotationLog annotationLog, List<IEventAnnotation> relations,
			AnnotationPositions entAnnotPositions,CorpusProcessAnnotationLogs annotations,IREProcessReport report) throws ANoteException {
		IEventAnnotation eventToAdd = annotations.getEventRelationAnnotationIDEventAnnotation().get(annotationLog.getOriginalAnnotationID());
		IEventAnnotation event = eventsMatching(eventToAdd,relations);
		if(event==null)
		{

			eventToAdd = tryfindEntities(eventToAdd,entAnnotPositions);
			if(eventToAdd!=null)
			{
				// Add relation In database
				relations.add(eventToAdd);
				report.getRESchemachemaWithManualCurationReport().addEvent(annotationLog);
			}
			else
			{
				report.getRESchemachemaWithManualCurationReport().addMissingAnnotationByMissingEntities(annotationLog);
			}
		}
		else // Already have the relation
		{
			report.getRESchemachemaWithManualCurationReport().addMissingAnnotationAlreadyAnnotated(annotationLog);
		}
	}
	
	
	private IEventAnnotation tryfindEntities(IEventAnnotation eventToAdd, AnnotationPositions entAnnotPositions) {
		
		List<IEntityAnnotation> leftEntities = new ArrayList<IEntityAnnotation>();
		List<IEntityAnnotation> rightEntities = new ArrayList<IEntityAnnotation>();
		for(IEntityAnnotation leftAnnotation : eventToAdd.getEntitiesAtLeft())
		{
			AnnotationPosition pos = new AnnotationPosition((int)leftAnnotation.getStartOffset(), (int)leftAnnotation.getEndOffset());
			if(entAnnotPositions.containsKey(pos) && leftAnnotation.getClassAnnotation().getId() == ((IEntityAnnotation) entAnnotPositions.get(pos)).getClassAnnotation().getId())
			{
				leftEntities.add(((IEntityAnnotation) entAnnotPositions.get(pos)));
			}
			else
			{
				return null;
			}
		}
		for(IEntityAnnotation rightAnnotation : eventToAdd.getEntitiesAtRight())
		{
			AnnotationPosition pos = new AnnotationPosition((int)rightAnnotation.getStartOffset(), (int)rightAnnotation.getEndOffset());
			if(entAnnotPositions.containsKey(pos) && rightAnnotation.getClassAnnotation().getId() == ((IEntityAnnotation) entAnnotPositions.get(pos)).getClassAnnotation().getId())
			{
				rightEntities.add(((IEntityAnnotation) entAnnotPositions.get(pos)));
			}
			else
			{
				return null;
			}
		}	
		IEventAnnotation event = new EventAnnotationImpl(eventToAdd.getStartOffset(), eventToAdd.getEndOffset(),
				AnnotationType.re.name(), leftEntities, rightEntities, eventToAdd.getEventClue(),eventToAdd.getEventProperties(),true);
		return event;
	}
	private void removeEventRElationAnnotation(IAnnotationLog annotationLog, List<IEventAnnotation> relations, AnnotationPositions annotPositions,
			CorpusProcessAnnotationLogs annotations,IREProcessReport report) throws ANoteException {
		IEventAnnotation eventToRemove = annotations.getEventRelationAnnotationIDEventAnnotation().get(annotationLog.getOriginalAnnotationID());
		IEventAnnotation event = eventsMatching(eventToRemove,relations);
		if(event!=null)
		{
			relations.remove(event);
			report.getRESchemachemaWithManualCurationReport().addRemoveChanged(annotationLog);
		}
		else
		{
			report.getRESchemachemaWithManualCurationReport().addMissingAnnotation(annotationLog);
		}
	}
	
	private IEventAnnotation eventsMatching(IEventAnnotation eventGold,List<IEventAnnotation> toCompareStartingInSentence) {
		for(IEventAnnotation toCompareEvent:toCompareStartingInSentence)
		{
			// Compare Events in pars
			if(matchingEvents(eventGold,toCompareEvent))
			{
				return toCompareEvent;
			}
		}
		return null;
	}
	
	private boolean matchingEvents(IEventAnnotation eventGold,IEventAnnotation toCompareEvent) {
		// Compare clues
		if(!eventGold.getEventClue().isEmpty() && toCompareEvent.getEventClue().isEmpty() && !eventGold.getEventClue().equals(toCompareEvent.getEventClue()))
		{
			return false;
		}
		// Compare Clue Position
		else if(eventGold.getStartOffset() != toCompareEvent.getStartOffset() || eventGold.getEndOffset() != toCompareEvent.getEndOffset())
		{
			return false;
		}
		// Same Clue same position test if entities are the same
		else
		{
			if(sameEntities(eventGold,toCompareEvent))
			{
				return true;
			}
		}
		return false;
	}
	
	private boolean sameEntities(IEventAnnotation eventGold,IEventAnnotation toCompareEvent) {
		List<IEntityAnnotation> entGoldLT = eventGold.getEntitiesAtLeft();
		List<IEntityAnnotation> entToComLT = toCompareEvent.getEntitiesAtLeft();
		// Test if enenties at left are the same
		if(!sameEntityFinder(entGoldLT,entToComLT))
		{
			return false;
		}
		List<IEntityAnnotation> entGoldRT = eventGold.getEntitiesAtRight();
		List<IEntityAnnotation> entToComRT = toCompareEvent.getEntitiesAtRight();
		// Test if the entities at right are the same
		if(!sameEntityFinder(entGoldRT,entToComRT))
		{
			return false;
		}
		return true;
	}
	
	private boolean sameEntityFinder(List<IEntityAnnotation> entsGold,List<IEntityAnnotation> entsToCom) {
		// Test if the size are the same
		if(entsGold.size() != entsToCom.size())
		{
			return false;
		}
		for(IEntityAnnotation entGold:entsGold)
		{
			// test if in entsToCom are the entity entGold
			if(!existsEntity(entGold,entsToCom))
			{
				return false;
			}
		}
		return true;
	}
	
	
	private boolean existsEntity(IEntityAnnotation entGold,List<IEntityAnnotation> entsToCom) {
		for(IEntityAnnotation entToCom:entsToCom)
		{
			// test the same position
			if(entToCom.getStartOffset() == entGold.getStartOffset() && entToCom.getEndOffset() == entGold.getEndOffset())
			{
				// Test the same class
				if(entToCom.getClassAnnotation().getId() == entGold.getClassAnnotation().getId())
				{
					return true;
				}
			}
		}
		return false;
	}


	private List<IEntityAnnotation> applyMCToNERTerms( long documentID, CorpusProcessAnnotationLogs annotationLogs, List<IEntityAnnotation> sentencesemanticLayer,IREProcessReport report) throws ANoteException {
		SortedSet<IAnnotationLog> documentAnnotations = annotationLogs.getDocumentNERLogAnnotation(documentID);
		AnnotationPositions annotPositions = new AnnotationPositions();
		for(IEntityAnnotation entAnnot : sentencesemanticLayer)
		{
			annotPositions.addAnnotationWhitConflicts(new AnnotationPosition((int)entAnnot.getStartOffset(), (int)entAnnot.getEndOffset()), entAnnot);
		}
		for(IAnnotationLog annotationLog : documentAnnotations)
		{
			applyChangesToAnnotation(annotationLog,annotPositions,annotationLogs,report);
		}
		List<IEntityAnnotation> result = new ArrayList<IEntityAnnotation>();
		for(AnnotationPosition annot: annotPositions.getAnnotations().keySet())
		{
			result.add((IEntityAnnotation) annotPositions.getAnnotations().get(annot));
		}
		return result;
	}
	
	private void applyChangesToAnnotation(IAnnotationLog annotationLog, AnnotationPositions annotPositions,CorpusProcessAnnotationLogs annotations,IREProcessReport report) throws ANoteException {
		if(annotationLog.getType().equals(AnnotationLogTypeEnum.ENTITYREMOVE))
		{
			removeEntityAnnotation(annotationLog, annotPositions, annotations,report);
		}
		else if(annotationLog.getType().equals(AnnotationLogTypeEnum.ENTITYADD))
		{
			addEntityAnnotation(annotationLog, annotPositions, annotations,report);

		}
		else if(annotationLog.getType().equals(AnnotationLogTypeEnum.ENTITYUPDATE))
		{
			editEntityAnnotation(annotationLog, annotPositions, annotations,report);
		}
	}
	
	private void editEntityAnnotation(IAnnotationLog annotationLog, AnnotationPositions annotPositions,CorpusProcessAnnotationLogs annotations,IREProcessReport report) throws ANoteException {
		// Test if annotation log has original entity annotation ID
		if(annotationLog.getOriginalAnnotationID()>0)
		{
			IEntityAnnotation oldEntity = annotations.getEntityAnnotationByID(annotationLog.getOriginalAnnotationID());
			// Test if Entity annotation is different from null
			if(oldEntity!=null)
			{
				AnnotationPosition entPOs = new AnnotationPosition((int)oldEntity.getStartOffset(), (int)oldEntity.getEndOffset());
				// Test if document has entity in same position
				if(annotPositions.containsKey(entPOs))
				{
					IEntityAnnotation entAnnot = (IEntityAnnotation) annotPositions.get(entPOs);
					AnnotationPosition entPOS = new AnnotationPosition((int)entAnnot.getStartOffset(), (int)entAnnot.getEndOffset());
					if(entPOs.equals(entPOS))
					{
						String oldClassName = annotationLog.getOldString().substring(annotationLog.getOldString().indexOf("(")+1,annotationLog.getOldString().indexOf(")"));
						String newClassName = annotationLog.getNewString().substring(annotationLog.getNewString().indexOf("(")+1,annotationLog.getNewString().indexOf(")"));

						// Test the class of the entity is the same
						if(oldClassName!=null && newClassName!=null && !oldClassName.equals(newClassName))
						{
							IAnoteClass newClass = ClassPropertiesManagement.getClassIDClassName(newClassName);
							// Update Memory entity
							entAnnot.setClassAnnotation(newClass);
							report.getNERSchemachemaWithManualCurationReport().addEditChanged(annotationLog);
						}
						else
						{
							report.getNERSchemachemaWithManualCurationReport().addNoMatchingAnnotationByClass(annotationLog);
						}
					}
					else
					{
						report.getNERSchemachemaWithManualCurationReport().addMissingNoMatchingAnnotation(annotationLog);
					}
				}
				else
				{
					report.getNERSchemachemaWithManualCurationReport().addMissingNoMatchingAnnotation(annotationLog);
				}
			}
			else
			{
				report.getNERSchemachemaWithManualCurationReport().addMissingAnnotationByEntityNull(annotationLog);
			}
		}
		else
		{
			report.getNERSchemachemaWithManualCurationReport().addMissingAnnotationByID(annotationLog);
		}
	}
	
	private void removeEntityAnnotation(IAnnotationLog annotationLog,AnnotationPositions annotPositions,
			CorpusProcessAnnotationLogs annotations,IREProcessReport report) throws ANoteException {
		// Test if annotation log has original entity annotation ID
		if(annotationLog.getOriginalAnnotationID()>0)
		{
			IEntityAnnotation oldEntity = annotations.getEntityAnnotationByID(annotationLog.getOriginalAnnotationID());
			// Test if Entity annotation is different from null
			if(oldEntity!=null)
			{
				AnnotationPosition entPOs = new AnnotationPosition((int)oldEntity.getStartOffset(), (int)oldEntity.getEndOffset());
				// Test if document has entity in same position
				if(annotPositions.containsKey(entPOs))
				{
					IEntityAnnotation entAnnot = (IEntityAnnotation) annotPositions.get(entPOs);
					AnnotationPosition entPOS = new AnnotationPosition((int)entAnnot.getStartOffset(), (int)entAnnot.getEndOffset());
					if(entPOs.equals(entPOS))
					{
						// Test the class of the entity is the same
						if(entAnnot.getClassAnnotation().getId() == oldEntity.getClassAnnotation().getId())
						{
							report.getNERSchemachemaWithManualCurationReport().addRemoveChanged(annotationLog);
							annotPositions.removeAnnotation(entPOs);
						}
						else
						{
							report.getNERSchemachemaWithManualCurationReport().addNoMatchingAnnotationByClass(annotationLog);
						}
					}
					else
					{
						report.getNERSchemachemaWithManualCurationReport().addMissingNoMatchingAnnotation(annotationLog);
					}
				}
				else
				{
					report.getNERSchemachemaWithManualCurationReport().addMissingNoMatchingAnnotation(annotationLog);
				}
			}
			else
			{
				report.getNERSchemachemaWithManualCurationReport().addMissingAnnotationByEntityNull(annotationLog);
			}
		}
		else
		{
			report.getNERSchemachemaWithManualCurationReport().addMissingAnnotationByID(annotationLog);
		}
	}
	
	private void addEntityAnnotation(IAnnotationLog annotationLog, AnnotationPositions annotPositions,
			CorpusProcessAnnotationLogs annotations,IREProcessReport report) throws ANoteException {
		if(annotationLog.getOriginalAnnotationID()>0)
		{
			IEntityAnnotation addEntity = annotations.getEntityAnnotationByID(annotationLog.getOriginalAnnotationID());
			// Test if Entity annotation is different from null
			if(addEntity!=null)
			{
				AnnotationPosition entPOs = new AnnotationPosition((int)addEntity.getStartOffset(), (int)addEntity.getEndOffset());
				// Test if document don´tn have entity in conflit position
				if(!annotPositions.containsConflits(entPOs))
				{
					report.getNERSchemachemaWithManualCurationReport().addInsertChanged(annotationLog);
					annotPositions.addAnnotationWhitConflicts(entPOs, addEntity);
				}
				else
				{
					report.getNERSchemachemaWithManualCurationReport().addMissingConflitAnnotation(annotationLog);
				}
			}
			else
			{
				report.getNERSchemachemaWithManualCurationReport().addMissingAnnotationByEntityNull(annotationLog);
			}
		}
		else
		{
			report.getNERSchemachemaWithManualCurationReport().addMissingAnnotationByID(annotationLog);
		}		
	}

	private List<IEntityAnnotation> getNERProcessEntityAnnotationsSentence(List<IEntityAnnotation> allDoucmentSemanticLayer, long startSentence, long endSentence) {
		List<IEntityAnnotation> sentenceAnnotation = new ArrayList<IEntityAnnotation>();
		for(IEntityAnnotation ent:allDoucmentSemanticLayer)
		{
			if(ent.getStartOffset() >= startSentence && ent.getStartOffset() < endSentence)
			{
				sentenceAnnotation.add(ent);
			}
		}
		return sentenceAnnotation;
	}

	public void stop() {
		this.stop = true;
		
	}		
	
	@Override
	public void validateConfiguration(IREConfiguration configuration)throws InvalidConfigurationException {
		if(configuration instanceof IRERelationConfiguration)
		{
			IRERelationConfiguration lexicalResurcesConfiguration = (IRERelationConfiguration) configuration;
			if(lexicalResurcesConfiguration.getCorpus()==null)
			{
				throw new InvalidConfigurationException("Corpus can not be null");
			}
		}
		else
			throw new InvalidConfigurationException("configuration must be IRERelationConfiguration isntance");		
	}
	
	@JsonIgnore
	protected void memoryAndProgress(int step, int total) {
		System.out.println((GlobalOptions.decimalformat.format((double) step / (double) total * 100)) + " %...");
		System.gc();
		System.out.println((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024) + " MB ");
	}

	@JsonIgnore
	protected void memoryAndProgressAndTime(int step, int total, long startTime) {
		System.out.println((GlobalOptions.decimalformat.format((double) step / (double) total * 100)) + " %...");
		System.gc();
		System.out.println((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024) + " MB ");
	}

}
