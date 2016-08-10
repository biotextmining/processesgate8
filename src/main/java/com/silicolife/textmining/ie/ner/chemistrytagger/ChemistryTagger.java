package com.silicolife.textmining.ie.ner.chemistrytagger;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.creole.splitter.RegexSentenceSplitter;
import gate.creole.tokeniser.SimpleTokeniser;
import gate.util.GateException;
import gate.util.InvalidOffsetException;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import mark.chemistry.Tagger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.silicolife.textmining.core.datastructures.annotation.AnnotationPosition;
import com.silicolife.textmining.core.datastructures.annotation.AnnotationPositions;
import com.silicolife.textmining.core.datastructures.annotation.ner.EntityAnnotationImpl;
import com.silicolife.textmining.core.datastructures.dataaccess.database.schema.TableAnnotation;
import com.silicolife.textmining.core.datastructures.documents.AnnotatedDocumentImpl;
import com.silicolife.textmining.core.datastructures.documents.PublicationImpl;
import com.silicolife.textmining.core.datastructures.exceptions.process.InvalidConfigurationException;
import com.silicolife.textmining.core.datastructures.general.AnoteClass;
import com.silicolife.textmining.core.datastructures.init.InitConfiguration;
import com.silicolife.textmining.core.datastructures.language.LanguageProperties;
import com.silicolife.textmining.core.datastructures.process.IEProcessImpl;
import com.silicolife.textmining.core.datastructures.process.ProcessOriginImpl;
import com.silicolife.textmining.core.datastructures.process.ProcessTypeImpl;
import com.silicolife.textmining.core.datastructures.report.processes.NERProcessReportImpl;
import com.silicolife.textmining.core.datastructures.textprocessing.NormalizationForm;
import com.silicolife.textmining.core.datastructures.utils.FileHandling;
import com.silicolife.textmining.core.datastructures.utils.GenerateRandomId;
import com.silicolife.textmining.core.datastructures.utils.GenericPairImpl;
import com.silicolife.textmining.core.datastructures.utils.Utils;
import com.silicolife.textmining.core.datastructures.utils.conf.GlobalNames;
import com.silicolife.textmining.core.datastructures.utils.conf.GlobalOptions;
import com.silicolife.textmining.core.interfaces.core.annotation.IEntityAnnotation;
import com.silicolife.textmining.core.interfaces.core.dataaccess.exception.ANoteException;
import com.silicolife.textmining.core.interfaces.core.document.IAnnotatedDocument;
import com.silicolife.textmining.core.interfaces.core.document.IDocumentSet;
import com.silicolife.textmining.core.interfaces.core.document.IPublication;
import com.silicolife.textmining.core.interfaces.core.document.IPublicationExternalSourceLink;
import com.silicolife.textmining.core.interfaces.core.document.labels.IPublicationLabel;
import com.silicolife.textmining.core.interfaces.core.document.structure.IPublicationField;
import com.silicolife.textmining.core.interfaces.core.general.classe.IAnoteClass;
import com.silicolife.textmining.core.interfaces.core.report.processes.INERProcessReport;
import com.silicolife.textmining.core.interfaces.process.IProcessOrigin;
import com.silicolife.textmining.core.interfaces.process.IE.IIEProcess;
import com.silicolife.textmining.core.interfaces.process.IE.INERProcess;
import com.silicolife.textmining.core.interfaces.process.IE.ner.INERConfiguration;
import com.silicolife.textmining.ie.ner.chemistrytagger.configuration.INERChemistryTaggerConfiguration;
import com.silicolife.wrappergate.GateInit;

public class ChemistryTagger implements INERProcess{

	public final static String nerChemistryTagger = "Chemistry Tagger";
	public static final IProcessOrigin nerChemistryOrigin= new ProcessOriginImpl(GenerateRandomId.generateID(),nerChemistryTagger);

	private Document gateDocument;
	private File file = new File("fileNERchemistryTagger.txt");
//	private IAnoteClass klass;
	private boolean stop = false;
	protected static final int characteres = 500000;
	private Tagger tagger;
	private SimpleTokeniser tokeniser;
	private RegexSentenceSplitter regExpSentenceSplitter;
	private static IAnoteClass klass = new AnoteClass("compound");



	public ChemistryTagger() {

	}
	
	@Override
	public INERProcessReport executeCorpusNER(INERConfiguration configuration) throws ANoteException,InvalidConfigurationException {
		try {
		validateConfiguration(configuration);
		INERChemistryTaggerConfiguration chimistryTaggerConfiguration = (INERChemistryTaggerConfiguration) configuration;
		IIEProcess runProcess = getIEProcess(chimistryTaggerConfiguration);
		InitConfiguration.getDataAccess().createIEProcess(runProcess);
		InitConfiguration.getDataAccess().registerCorpusProcess(configuration.getCorpus(), runProcess);
		GateInit();
		INERProcessReport report = new NERProcessReportImpl(LanguageProperties.getLanguageStream("pt.uminho.anote2.nergate.chemistry.report.title"),runProcess);
		int step = 0;
		IDocumentSet docs = chimistryTaggerConfiguration.getCorpus().getArticlesCorpus();
		int size = docs.getAllDocuments().size();
		int textSize = 0;
		long startTime = GregorianCalendar.getInstance().getTimeInMillis();
		StringBuffer stringWhitManyDocuments = new StringBuffer();
		long actualTime;
		Iterator<IPublication> itDocs = docs.iterator();
		Map<Long, GenericPairImpl<Integer, Integer>> docLimits = new HashMap<Long, GenericPairImpl<Integer,Integer>>();
		while(itDocs.hasNext())
		{
			if(stop)
			{
				report.setcancel();
				break;
			}
			IPublication pub = itDocs.next();
			IAnnotatedDocument annotDoc = new AnnotatedDocumentImpl(pub, runProcess, chimistryTaggerConfiguration.getCorpus());
			String text = annotDoc.getDocumentAnnotationText();
			if(text == null || text.length() == 0)
			{
//				Logger logger = Logger.getLogger(Workbench.class.getName());
//				logger.warn("No available text for publication whit id = "+pub.getId()+" for NER Process");
			}
			else
			{
				int lenthText = text.length();
				docLimits.put(pub.getId(), new GenericPairImpl<>(textSize, textSize+lenthText-1));
				textSize = textSize+lenthText;
				stringWhitManyDocuments.append(text);		
				if(stop)
				{
					report.setcancel();
					break;
				}
				else if(textSize>characteres && !stop)
				{
					documetsRelationExtraction(runProcess,chimistryTaggerConfiguration,docLimits,report,stringWhitManyDocuments);
					stringWhitManyDocuments = new StringBuffer();
					docLimits = new HashMap<Long, GenericPairImpl<Integer,Integer>>();
					textSize=0;
					memoryAndProgressAndTime(step, size, startTime);
				}
			}	
			step++;
			report.incrementDocument();
		}
		if(stringWhitManyDocuments.length()>0 && !stop)
		{
			documetsRelationExtraction(runProcess,chimistryTaggerConfiguration,docLimits,report,stringWhitManyDocuments);
		}
		actualTime = GregorianCalendar.getInstance().getTimeInMillis();
		report.setTime(actualTime - startTime);
		file.delete();
		cleanAll();
		return report;
		} catch (GateException | IOException e) {
			throw new ANoteException(e);
		}
	}

	private IIEProcess getIEProcess(INERChemistryTaggerConfiguration configuration) {
		String description = ChemistryTagger.nerChemistryTagger  + " " +Utils.SimpleDataFormat.format(new Date());
		String notes = configuration.getProcessNotes();
		Properties properties = gerateProperties(configuration);
		IIEProcess runProcess = new IEProcessImpl(configuration.getCorpus(), description, notes, ProcessTypeImpl.getNERProcessType(), nerChemistryOrigin, properties);
		return runProcess;
	}

	private static Properties gerateProperties(INERChemistryTaggerConfiguration configuration) {
		Properties pro = new Properties();
		pro.put(GlobalNames.nerChemistryTaggerChemistrylon, String.valueOf(configuration.findChemistrylIon()));
		pro.put(GlobalNames.nerChemistryTaggerChemistryCompounds, String.valueOf(configuration.findChemistryCompounds()));
		pro.put(GlobalNames.nerChemistryTaggerChemistryElements, String.valueOf(configuration.findChemistryElements()));		
		return pro;
	}

	private void GateInit() throws GateException, MalformedURLException
	{
		GateInit.getInstance().init();
		GateInit.getInstance().creoleRegister("plugins/ANNIE");
		GateInit.getInstance().creoleRegister("plugins/Tagger_Chemistry");
	}	

	private void performeGateChemistryTagger() throws ResourceInstantiationException, ExecutionException {
		if(tagger==null)
		{
			FeatureMap params = Factory.newFeatureMap();
			FeatureMap features = Factory.newFeatureMap();
			params.put("sourceUrl",file.toURI().toString());	
			gateDocument = (Document) Factory.createResource("gate.corpora.DocumentImpl", params, features, "GATE Homepage");
			params = Factory.newFeatureMap();
			features = Factory.newFeatureMap();
			features = Factory.newFeatureMap();
			tokeniser = (SimpleTokeniser) Factory.createResource("gate.creole.tokeniser.SimpleTokeniser", params, features);
			features = Factory.newFeatureMap();	
			regExpSentenceSplitter = (RegexSentenceSplitter) Factory.createResource("gate.creole.splitter.RegexSentenceSplitter", params, features);
			params = Factory.newFeatureMap();
			params.put("removeElements","false");
			tagger = (Tagger) Factory.createResource("mark.chemistry.Tagger", params, features);
		}
		else
		{
			FeatureMap params = Factory.newFeatureMap();
			FeatureMap features = Factory.newFeatureMap();
			params.put("sourceUrl",file.toURI().toString());	
			gateDocument = (Document) Factory.createResource("gate.corpora.DocumentImpl", params, features, "GATE Homepage");
		}
		tokeniser.setDocument(gateDocument);
		tokeniser.execute();
		regExpSentenceSplitter.setDocument(gateDocument);
		regExpSentenceSplitter.execute();
		tagger.setDocument(gateDocument);
		tagger.execute();
	}

	private void documetsRelationExtraction(IIEProcess process,INERChemistryTaggerConfiguration configuration,Map<Long, GenericPairImpl<Integer, Integer>> docLimits, INERProcessReport report, StringBuffer stringWhitManyDocuments) throws ANoteException, IOException {
		String fileText = stringWhitManyDocuments.toString();
		FileHandling.writeInformationOnFile(file,fileText);
		try{
			performeGateChemistryTagger();
			for(long docID:docLimits.keySet())
			{
				Long start = new Long(docLimits.get(docID).getX());
				Long end = new Long(docLimits.get(docID).getY());
				AnnotationPositions annotPos = new AnnotationPositions();
				if(configuration.findChemistryCompounds() & !stop)
					getAnnotations(annotPos,start,end,GlobalNames.nerChemistryTaggerChemistryCompounds);
				if(configuration.findChemistryElements() & !stop)
					getAnnotations(annotPos,start,end,GlobalNames.nerChemistryTaggerChemistryElements);
				if(configuration.findChemistrylIon() & !stop)	
					getAnnotations(annotPos,start,end,GlobalNames.nerChemistryTaggerChemistrylon);

				if(!stop)
				{
					List<IEntityAnnotation> entityAnnotations = annotPos.getEntitiesFromAnnoattionPositions();
					IPublication document =  new PublicationImpl(docID,
							"", "", "", "", "",
							"", "", "", "", "", "",
							"", false, "", "",
							new ArrayList<IPublicationExternalSourceLink>(),
							new ArrayList<IPublicationField>(),
							new ArrayList<IPublicationLabel>());
					InitConfiguration.getDataAccess().addProcessDocumentEntitiesAnnotations(process, document, entityAnnotations);
					report.incrementEntitiesAnnotated(entityAnnotations.size());
				}
				else
				{
					report.setcancel();
					break;
				}
			}
			System.gc();
		}catch (ResourceInstantiationException | ExecutionException | InvalidOffsetException e) {
			throw new ANoteException(e);
		}
	}

	private void getAnnotations(AnnotationPositions annotPos,Long startDoc,Long endDoc,String type) throws InvalidOffsetException {
		AnnotationSet annotAbner = gateDocument.getAnnotations().get(type).get(startDoc, endDoc);
		Iterator<Annotation> annotIterator = annotAbner.iterator();
		
		Long start,end;
		while(annotIterator.hasNext())
		{
			Annotation annot = annotIterator.next();
			start = annot.getStartNode().getOffset();
			end = annot.getEndNode().getOffset();
			if(end-start<TableAnnotation.maxAnnotaionElementSize && start>=startDoc && end <= endDoc)
			{
				String value= gateDocument.getContent().getContent(start, end).toString();
				IEntityAnnotation entity = new EntityAnnotationImpl(start-startDoc, end-startDoc, klass  ,null, value, false, null);
				AnnotationPosition position = new AnnotationPosition(Integer.parseInt(String.valueOf(start-startDoc)), Integer.parseInt(String.valueOf(end-startDoc)));
				annotPos.addAnnotationWhitConflicts(position, entity);
			}
		}
	}

	

	private void cleanAll() {
		if(tokeniser!=null)
		{
			Factory.deleteResource(tokeniser);
		}
		if(regExpSentenceSplitter!=null)
		{
			Factory.deleteResource(regExpSentenceSplitter);
		}
		if(tagger!=null)
		{
			Factory.deleteResource(tagger);
		}
		if(gateDocument!=null)
		{
			Factory.deleteResource(gateDocument);
		}
	}

	public void stop() {
		this.stop = true;

	}

	@Override
	public void validateConfiguration(INERConfiguration configuration)throws InvalidConfigurationException {
		if(configuration instanceof INERChemistryTaggerConfiguration)
		{
			INERChemistryTaggerConfiguration lexicalResurcesConfiguration = (INERChemistryTaggerConfiguration) configuration;
			if(lexicalResurcesConfiguration.getCorpus()==null)
			{
				throw new InvalidConfigurationException("Corpus can not be null");
			}
		}
		else
			throw new InvalidConfigurationException("configuration must be INERChemistryTaggerConfiguration isntance");		
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
