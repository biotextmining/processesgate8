package com.silicolife.textmining.corpus.loaders;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.creole.ResourceInstantiationException;
import gate.util.GateException;
import gate.util.InvalidOffsetException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.silicolife.textmining.core.datastructures.annotation.ner.EntityAnnotationImpl;
import com.silicolife.textmining.core.datastructures.documents.AnnotatedDocumentImpl;
import com.silicolife.textmining.core.datastructures.documents.PublicationImpl;
import com.silicolife.textmining.core.datastructures.general.AnoteClass;
import com.silicolife.textmining.core.datastructures.general.ClassPropertiesManagement;
import com.silicolife.textmining.core.datastructures.textprocessing.NormalizationForm;
import com.silicolife.textmining.core.datastructures.utils.FileHandling;
import com.silicolife.textmining.core.interfaces.core.annotation.IEntityAnnotation;
import com.silicolife.textmining.core.interfaces.core.corpora.loaders.ICorpusEntityLoader;
import com.silicolife.textmining.core.interfaces.core.dataaccess.exception.ANoteException;
import com.silicolife.textmining.core.interfaces.core.document.IAnnotatedDocument;
import com.silicolife.textmining.core.interfaces.core.document.IPublication;
import com.silicolife.textmining.core.interfaces.core.document.IPublicationExternalSourceLink;
import com.silicolife.textmining.core.interfaces.core.document.labels.IPublicationLabel;
import com.silicolife.textmining.core.interfaces.core.document.structure.IPublicationField;
import com.silicolife.textmining.core.interfaces.core.general.classe.IAnoteClass;
import com.silicolife.wrappergate.GateInit;
import com.silicolife.wrappergate.GateCorpusReaderLoader;

public class ANoteCorpusLoader implements ICorpusEntityLoader{

	private List<IPublication> documents;
	private Document gateDoc;
	private List<String> corruptFiles;
	private Set<String> validTags;
	private boolean stop = false;
	private Map<Long,IAnnotatedDocument> documentsWithEntities;

	public ANoteCorpusLoader()
	{
		this.documents = new ArrayList<>();
		this.documentsWithEntities = new TreeMap<>();
		this.validTags = putValidTags();
	}

	private Set<String> putValidTags() {
		Set<String> validTags = new HashSet<String>();
		validTags.add("ARTICLE");
		validTags.add("JOURNAL");
		validTags.add("TITLE");
		validTags.add("span");
		validTags.add("SECTION");
		validTags.add("PARAGRAPH");
		validTags.add("AUTHORS");
		validTags.add("ABSTRACT");
		validTags.add("KEYWORDS");
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
					return 	getDocuments();
				}
				if(!file.isDirectory())
				{
					processFile(file);
				}
			} 
			return 	getDocuments();
		}
		else
		{
			return null;
		}	
	}


	private void processFile(File file) throws ANoteException, IOException {
		try{
			String fileContente = FileHandling.fileToString(file);
			String newFileCOntent = NormalizationForm.removeOffsetProblemSituationWithoutXMLMarks(fileContente);
			File dir = new File("conf/temp/");
			if(!dir.exists())
				dir.mkdir();
			File fileInput = new File("conf/temp/"+file.getName());
			FileHandling.writeInformationOnFile(fileInput, newFileCOntent);
			gateDoc = GateCorpusReaderLoader.createGateDocument(fileInput);
			String fullText = gateDoc.getContent().toString();
			AnnotationSet annotSetOriginal = gateDoc.getAnnotations("Original markups");
			String title = getDocumentTitle(gateDoc,annotSetOriginal);
			String abstractText = getDocumentAbstract(gateDoc,annotSetOriginal);
			String journal = getDocumentJournal(gateDoc,annotSetOriginal);
			String authors = getDocumentAuthors(gateDoc,annotSetOriginal);
			IPublication pub = new PublicationImpl(title, authors, "", "", "", "", journal, "", "", "", abstractText, "", true, "", "", new ArrayList<IPublicationExternalSourceLink>(), new ArrayList<IPublicationField>(), new ArrayList<IPublicationLabel>());
			pub.setFullTextContent(fullText);
			getDocuments().add(pub);
			List<IEntityAnnotation> entities = getEntities(gateDoc,annotSetOriginal);
			IAnnotatedDocument docResult = new AnnotatedDocumentImpl(pub,null, null, entities);
			getDocumentEntityAnnotations().put(pub.getId(),docResult);
			gateDoc.cleanup();
		}catch(InvalidOffsetException | ResourceInstantiationException e){
			throw new ANoteException(e);
		}
	}

	private List<IEntityAnnotation> getEntities(Document gateDoc2, AnnotationSet annotSetOriginal) throws InvalidOffsetException, ANoteException {
		List<IEntityAnnotation> entityList = new ArrayList<IEntityAnnotation>();
		AnnotationSet annotSetOriginalSpan = annotSetOriginal.get("span");
		Iterator<Annotation> annotIterator = annotSetOriginalSpan.iterator();
		while(annotIterator.hasNext())
		{
			Annotation annot = annotIterator.next();
			long start = annot.getStartNode().getOffset();
			long end = annot.getEndNode().getOffset();
			String value=gateDoc.getContent().getContent(start, end).toString();
			IAnoteClass klassToAdd = new AnoteClass(annot.getFeatures().get("class").toString());
			IAnoteClass klass = ClassPropertiesManagement.getClassIDOrinsertIfNotExist(klassToAdd);
			IEntityAnnotation entity = new EntityAnnotationImpl(start, end,klass,null, value, false, null);
			entityList.add(entity);
		}
		return entityList;
	}

	private String getDocumentAuthors(Document gateDoc2,AnnotationSet annotSetOriginal) throws InvalidOffsetException {
		return GateCorpusReaderLoader.getGeneralArticleInfo(gateDoc, annotSetOriginal,"AUTHORS");
	}

	private String getDocumentJournal(Document gateDoc2,AnnotationSet annotSetOriginal) throws InvalidOffsetException {
		return GateCorpusReaderLoader.getGeneralArticleInfo(gateDoc, annotSetOriginal,"JOURNAL");
	}

	private String getDocumentAbstract(Document gateDoc2,AnnotationSet annotSetOriginal) throws InvalidOffsetException {
		return GateCorpusReaderLoader.getGeneralArticleInfo(gateDoc, annotSetOriginal,"ABSTRACT");
	}

	private String getDocumentTitle(Document gateDoc2,AnnotationSet annotSetOriginal) throws InvalidOffsetException {
		return GateCorpusReaderLoader.getGeneralArticleInfo(gateDoc, annotSetOriginal,"TITLE");
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


	@Override
	public Map<Long,IAnnotatedDocument> getDocumentEntityAnnotations() {
		return documentsWithEntities;
	}

	public List<IPublication> getDocuments() {
		return documents;
	}

	@Override
	public void stop() {
		this.stop  = true;		
	}



}
