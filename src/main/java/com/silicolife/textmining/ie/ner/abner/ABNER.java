package com.silicolife.textmining.ie.ner.abner;

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
import com.silicolife.textmining.core.datastructures.process.ProcessOriginImpl;
import com.silicolife.textmining.core.datastructures.report.processes.NERProcessReportImpl;
import com.silicolife.textmining.core.datastructures.utils.FileHandling;
import com.silicolife.textmining.core.datastructures.utils.GenerateRandomId;
import com.silicolife.textmining.core.datastructures.utils.GenericPairImpl;
import com.silicolife.textmining.core.datastructures.utils.GenericTriple;
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
import com.silicolife.textmining.ie.ner.abner.configuration.INERAbnerConfiguration;
import com.silicolife.wrappergate.GateInit;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.abner.AbnerTagger;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.util.GateException;
import gate.util.InvalidOffsetException;

public class ABNER implements INERProcess{

	public final static String nerAbner = "Abner Tagger";

	public static final IProcessOrigin nerAbnerOrigin= new ProcessOriginImpl(GenerateRandomId.generateID(),nerAbner);

	private Document gateDocument;
	protected static final int characteres = 500000;
	private File file = new File("fileNERAbner.txt");
	private boolean stop = false;
	private AbnerTagger abTagger;


	public ABNER() 
	{
	
	}
	
	public INERProcessReport executeCorpusNER(INERConfiguration configuration) throws ANoteException, InvalidConfigurationException {
		validateConfiguration(configuration);
		INERAbnerConfiguration abnerConfiguration = (INERAbnerConfiguration) configuration;
		IIEProcess runProcess = getIEProcess(configuration, abnerConfiguration);
		InitConfiguration.getDataAccess().createIEProcess(runProcess);
		InitConfiguration.getDataAccess().registerCorpusProcess(configuration.getCorpus(), runProcess);
		GateInit();
		INERProcessReport report =  new NERProcessReportImpl(LanguageProperties.getLanguageStream("pt.uminho.anote2.nergate.abner.report.title"),runProcess);
		int step = 0;
		IDocumentSet docs = abnerConfiguration.getCorpus().getArticlesCorpus();
		int size = docs.getAllDocuments().size();
		int textSize = 0;
		long startTime = GregorianCalendar.getInstance().getTimeInMillis();
		StringBuffer stringWhitManyDocuments = new StringBuffer();
		Map<Long, GenericPairImpl<Integer, Integer>> docLimits = new HashMap<Long, GenericPairImpl<Integer,Integer>>();
		long actualTime;
		Iterator<IPublication> itDocs = docs.iterator();
		while(itDocs.hasNext())
		{
			if(stop)
			{
				report.setcancel();
				break;
			}
			IPublication pub = itDocs.next();
			IAnnotatedDocument annotDoc = new AnnotatedDocumentImpl(pub, runProcess, abnerConfiguration.getCorpus());
			String text = annotDoc.getDocumentAnnotationText();
			if(text == null || text.length() == 0)
			{
//				Logger logger = Logger.getLogger(Workbench.class.getName());
//				logger.warn("Not available text for publication whit id = "+pub.getId()+" for NER Process");
			}
			else
			{
				int lenthText = text.length();
				stringWhitManyDocuments.append(preprocessingtext(text));
				docLimits.put(pub.getId(), new GenericPairImpl<>(textSize, textSize+lenthText-1));
				textSize = textSize+lenthText;
				if(textSize>characteres && !stop)
				{
					documetsRelationExtraction(runProcess,abnerConfiguration,docLimits,report,stringWhitManyDocuments);
					stringWhitManyDocuments = new StringBuffer();
					textSize=0;
					docLimits = new HashMap<Long, GenericPairImpl<Integer,Integer>>();
					memoryAndProgressAndTime(step, size, startTime);
				}
				else if(stop)
				{
					report.setcancel();
					break;
				}
			}
			step++;
			report.incrementDocument();
		}
		if(stringWhitManyDocuments.length()>0 && !stop)
		{
			documetsRelationExtraction(runProcess,abnerConfiguration,docLimits,report,stringWhitManyDocuments);
		}
		actualTime = GregorianCalendar.getInstance().getTimeInMillis();
		report.setTime(actualTime-startTime);
		file.delete();
		cleanAll();
		return report;
	}

	private IIEProcess getIEProcess(INERConfiguration configuration,
			INERAbnerConfiguration abnerConfiguration) {
		String description = ABNER.nerAbner  + " " +Utils.SimpleDataFormat.format(new Date());
		IIEProcess process = configuration.getIEProcess();
		Properties properties = gerateProperties(abnerConfiguration);
		process.setName(description);
		process.setProperties(properties);
		return process;
	}

	private static Properties gerateProperties(INERAbnerConfiguration abnerConfiguration) {
		Properties pro = new Properties();
		pro.put(GlobalNames.nerAbnerModel, abnerConfiguration.getModel().name());
		return pro;
	}

	private void GateInit() throws ANoteException
	{
		try {
			GateInit.getInstance().init();

			GateInit.getInstance().creoleRegister("plugins/ANNIE");
			GateInit.getInstance().creoleRegister("plugins/Tagger_Abner");
		} catch (GateException | MalformedURLException e) {
			throw new ANoteException(e);
		}
	}	

	private void documetsRelationExtraction(IIEProcess process,INERAbnerConfiguration configuration,Map<Long, GenericPairImpl<Integer, Integer>> docLimits, INERProcessReport report, StringBuffer stringWhitManyDocuments) throws ANoteException {
		String fileText = stringWhitManyDocuments.toString();
		try {
			FileHandling.writeInformationOnFile(file,fileText);

			performeGateAbner(configuration);		
			for(long documentID:docLimits.keySet())
			{
				Long start = new Long(docLimits.get(documentID).getX());
				Long end = new Long(docLimits.get(documentID).getY());
				AnnotationPositions positions =  getAnnotations(start,end);

				if(!stop)
				{
					List<IEntityAnnotation> entityAnnotations = positions.getEntitiesFromAnnoattionPositions();
					IPublication document =  new PublicationImpl(documentID,
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
		} catch (IOException e) {
			throw new ANoteException(e);
		}
	}

	private AnnotationPositions getAnnotations(Long startDoc, Long endDoc) throws ANoteException {
		AnnotationPositions annotPos = new AnnotationPositions();
		AnnotationSet annotAbner = gateDocument.getAnnotations().get("Tagger").get(startDoc, endDoc);
		Iterator<Annotation> annotIterator = annotAbner.iterator();

		try {
			while(annotIterator.hasNext())
			{
				Annotation annot = annotIterator.next();
				Long start = annot.getStartNode().getOffset();
				Long end = annot.getEndNode().getOffset();
				if(end-start<TableAnnotation.maxAnnotaionElementSize && start>=startDoc && end <= endDoc)
				{
					String value = gateDocument.getContent().getContent(start, end).toString();
					String classe = (String) annot.getFeatures().get("type");
					IAnoteClass klass = new AnoteClass(classe);
					IEntityAnnotation entity = new EntityAnnotationImpl(start-startDoc, end-startDoc, klass, null,value, false,false,null);
					AnnotationPosition position = new AnnotationPosition(Integer.parseInt(String.valueOf(start-startDoc)), Integer.parseInt(String.valueOf(end-startDoc)));
					annotPos.addAnnotationWhitConflicts(position, entity);
				}
				else
				{
					// To long entity
				}
			}
		} catch (InvalidOffsetException e) {
			throw new ANoteException(e);
		}
		return annotPos;
	}

	private void performeGateAbner(INERAbnerConfiguration configuration) throws ANoteException {
		try {
			if(abTagger==null)
			{
				FeatureMap params = Factory.newFeatureMap();
				FeatureMap features = Factory.newFeatureMap();
				params.put("sourceUrl",file.toURI().toString());	

				gateDocument = (Document) Factory.createResource("gate.corpora.DocumentImpl", params, features, "GATE Homepage");

				params = Factory.newFeatureMap();
				features = Factory.newFeatureMap();
				features = Factory.newFeatureMap();
				params.put("abnerMode", configuration.getModel().toValue());
				abTagger = (AbnerTagger) Factory.createResource("gate.abner.AbnerTagger", params, features);
			}
			else
			{
				FeatureMap params = Factory.newFeatureMap();
				FeatureMap features = Factory.newFeatureMap();
				params.put("sourceUrl",file.toURI().toString());	
				gateDocument = (Document) Factory.createResource("gate.corpora.DocumentImpl", params, features, "GATE Homepage");
			}
			abTagger.setDocument(gateDocument);
			abTagger.execute();
		} catch (ResourceInstantiationException | ExecutionException e) {
			throw new ANoteException(e);
		}
	}

	public static List<GenericTriple<Long, Long,Integer>> getGateDocumentlimits(Document doc) {
		List<GenericTriple<Long,Long,Integer>> documentLimits = new ArrayList<GenericTriple<Long,Long,Integer>>();
		AnnotationSet annotSetSentences = doc.getAnnotations("Original markups");
		AnnotationSet annotSetSentences2 = annotSetSentences.get("Doc");
		for(Annotation annot:annotSetSentences2)
		{
			String idS = (String) annot.getFeatures().get("id");
			int id = Integer.valueOf(idS);
			documentLimits.add(new GenericTriple<Long, Long,Integer>(annot.getStartNode().getOffset(),annot.getEndNode().getOffset(),id));
		}
		return documentLimits;
	}

	

	private String preprocessingtext(String text) {
		text=text.replaceAll("'"," ");
		return text;
	}

	private void cleanAll() {
		if(abTagger!=null)
		{
			Factory.deleteResource(abTagger);
		}
		if(gateDocument!=null)
		{
			Factory.deleteResource(gateDocument);

		}
	}

	@Override
	public void stop() {
		stop = true;	
	}


	@Override
	public void validateConfiguration(INERConfiguration configuration)throws InvalidConfigurationException {
		if(configuration instanceof INERAbnerConfiguration)
		{
			INERAbnerConfiguration lexicalResurcesConfiguration = (INERAbnerConfiguration) configuration;
			if(lexicalResurcesConfiguration.getCorpus()==null)
			{
				throw new InvalidConfigurationException("Corpus can not be null");
			}
		}
		else
			throw new InvalidConfigurationException("configuration must be INERAbnerConfiguration isntance");
		
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
