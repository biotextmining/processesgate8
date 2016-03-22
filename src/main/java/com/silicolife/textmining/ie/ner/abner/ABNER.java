package com.silicolife.textmining.ie.ner.abner;

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

import com.silicolife.textmining.core.datastructures.annotation.AnnotationPosition;
import com.silicolife.textmining.core.datastructures.annotation.AnnotationPositions;
import com.silicolife.textmining.core.datastructures.annotation.ner.EntityAnnotationImpl;
import com.silicolife.textmining.core.datastructures.dataaccess.database.schema.TableAnnotation;
import com.silicolife.textmining.core.datastructures.documents.AnnotatedDocumentImpl;
import com.silicolife.textmining.core.datastructures.documents.PublicationImpl;
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
import com.silicolife.textmining.core.datastructures.utils.GenericTriple;
import com.silicolife.textmining.core.datastructures.utils.Utils;
import com.silicolife.textmining.core.datastructures.utils.conf.GlobalNames;
import com.silicolife.textmining.core.interfaces.core.annotation.IEntityAnnotation;
import com.silicolife.textmining.core.interfaces.core.dataaccess.exception.ANoteException;
import com.silicolife.textmining.core.interfaces.core.document.IAnnotatedDocument;
import com.silicolife.textmining.core.interfaces.core.document.IDocumentSet;
import com.silicolife.textmining.core.interfaces.core.document.IPublication;
import com.silicolife.textmining.core.interfaces.core.document.IPublicationExternalSourceLink;
import com.silicolife.textmining.core.interfaces.core.document.corpus.ICorpus;
import com.silicolife.textmining.core.interfaces.core.document.labels.IPublicationLabel;
import com.silicolife.textmining.core.interfaces.core.document.structure.IPublicationField;
import com.silicolife.textmining.core.interfaces.core.general.classe.IAnoteClass;
import com.silicolife.textmining.core.interfaces.core.report.processes.INERProcessReport;
import com.silicolife.textmining.core.interfaces.process.IProcessOrigin;
import com.silicolife.textmining.core.interfaces.process.IE.INERProcess;
import com.silicolife.textmining.ie.ner.abner.configuration.INERAbnerConfiguration;
import com.silicolife.wrappergate.GateInit;

public class ABNER extends IEProcessImpl implements INERProcess{

	public final static String nerAbner = "Abner Tagger";

	public static final IProcessOrigin nerAbnerOrigin= new ProcessOriginImpl(GenerateRandomId.generateID(),nerAbner);
	static{
		//		Logger.getLogger(gate.abner.AbnerTagger.class).setLevel(Level.OFF);	
	}

	private ABNERTrainingModel model;
	private Document gateDocument;
	protected static final int characteres = 500000;
	private File file = new File("fileNERAbner.txt");
	private boolean stop = false;
	private AbnerTagger abTagger;

	public ABNER(INERAbnerConfiguration configuration) 
	{
		super(configuration.getCorpus(), 
				nerAbner + " " +Utils.SimpleDataFormat.format(new Date()), 
				null,
				ProcessTypeImpl.getNERProcessType(),
				nerAbnerOrigin,
				gerateProperties(configuration.getModel()));
		this.model=configuration.getModel();
	}

	private static Properties gerateProperties(ABNERTrainingModel model) {
		Properties pro = new Properties();
		pro.put(GlobalNames.nerAbnerModel, model.toValue());
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

	private void documetsRelationExtraction(Map<Long, GenericPairImpl<Integer, Integer>> docLimits, INERProcessReport report, StringBuffer stringWhitManyDocuments) throws ANoteException {
		String fileText = stringWhitManyDocuments.toString();
		try {
			FileHandling.writeInformationOnFile(file,fileText);

			performeGateAbner();		
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
					InitConfiguration.getDataAccess().addProcessDocumentEntitiesAnnotations(this, document, entityAnnotations);
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
					IEntityAnnotation entity = new EntityAnnotationImpl(start-startDoc, end-startDoc, klass, null,value, NormalizationForm.getNormalizationForm(value),null);
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

	private void performeGateAbner() throws ANoteException {
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
				params.put("abnerMode", model.toValue());
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

	public INERProcessReport executeCorpusNER(ICorpus corpus) throws ANoteException {
		InitConfiguration.getDataAccess().createIEProcess(this);
		GateInit();
		setCorpus(corpus);
		INERProcessReport report =  new NERProcessReportImpl(LanguageProperties.getLanguageStream("pt.uminho.anote2.nergate.abner.report.title"),this);
		int step = 0;
		IDocumentSet docs = corpus.getArticlesCorpus();
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
			IAnnotatedDocument annotDoc = new AnnotatedDocumentImpl(pub, this, getCorpus());
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
					documetsRelationExtraction(docLimits,report,stringWhitManyDocuments);
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
			documetsRelationExtraction(docLimits,report,stringWhitManyDocuments);
		}
		actualTime = GregorianCalendar.getInstance().getTimeInMillis();
		report.setTime(actualTime-startTime);
		file.delete();
		cleanAll();
		return report;
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

}
