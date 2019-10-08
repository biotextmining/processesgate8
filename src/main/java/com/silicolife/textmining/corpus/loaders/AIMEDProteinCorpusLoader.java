package com.silicolife.textmining.corpus.loaders;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import com.silicolife.textmining.core.datastructures.annotation.ner.EntityAnnotationImpl;
import com.silicolife.textmining.core.datastructures.documents.AnnotatedDocumentImpl;
import com.silicolife.textmining.core.datastructures.documents.PublicationImpl;
import com.silicolife.textmining.core.datastructures.general.AnoteClass;
import com.silicolife.textmining.core.datastructures.general.ClassPropertiesManagement;
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
import com.silicolife.wrappergate.GateCorpusReaderLoader;
import com.silicolife.wrappergate.GateInit;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.creole.ResourceInstantiationException;
import gate.util.GateException;
import gate.util.InvalidOffsetException;

public class AIMEDProteinCorpusLoader implements ICorpusEntityLoader{

	private int corpusSize;
	private List<IPublication> documents;
	private Document gateDoc;
	private boolean stop = false;

	private static final String fileCorpusCheck = "/corpus-list";
	private static final String fileCorpusTags = "/tag-list";
	private String proteinClass = "Protein";
	private Map<Long,IAnnotatedDocument> documentswithEntities;

	public AIMEDProteinCorpusLoader()
	{
		this.documents = new ArrayList<>();
		this.documentswithEntities = new TreeMap<>();
	}

	@Override
	public List<IPublication> processFile(File fileOrDirectory,
			Properties properties) throws ANoteException, IOException {
		if(validateFile(fileOrDirectory))
		{
			try {
				GateInit.getInstance().init();
				List<String> listFiles = getFilesList(fileOrDirectory);
				setCorpusSize(listFiles.size());
				for(File file:fileOrDirectory.listFiles())
				{
					if(stop)
					{
						return 	getDocuments();
					}
					if(!file.isDirectory())
					{
						if(listFiles.contains(file.getName()))
						{
							processFile(file);
						}
					}
				}
			}catch (GateException e) {
				e.printStackTrace();
			} 
			return 	getDocuments();
		}
		else
		{
			return null;
		}
	}


	/**
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws ANoteException 
	 */
	private void processFile(File file) throws FileNotFoundException, IOException, ANoteException {
		try{

			String fileContent = FileHandling.getFileContent(file);
			fileContent=fileContent.replace("<?>", "");
			fileContent=fileContent.replace("</?>", "");
			fileContent = "<Doc>" + fileContent + "</Doc>";
			File fileTmp = new File("fileTmp.xml");			
			FileHandling.writeInformationOnFile(fileTmp, fileContent);		
			gateDoc = GateCorpusReaderLoader.createGateDocument(fileTmp);;
			AnnotationSet annotSetOriginal = gateDoc.getAnnotations("Original markups");	
			String title = getDocumentTitle(gateDoc,annotSetOriginal);
			String abstractText = getDocumentAbstract(gateDoc,annotSetOriginal);
			IPublication pub = new PublicationImpl(title, "AIMED Team", "", "", "", "", "", "", "", "", abstractText, "", false, "", "","", new ArrayList<IPublicationExternalSourceLink>() , new ArrayList<IPublicationField>(), new ArrayList<IPublicationLabel>());
			getDocuments().add(pub);
			List<IEntityAnnotation> entities = getEntities(gateDoc,annotSetOriginal);
			IAnnotatedDocument docResult = new AnnotatedDocumentImpl(pub,null, null, entities);
			getDocumentEntityAnnotations().put(pub.getId(),docResult);
			gateDoc.cleanup();
		}
		catch(InvalidOffsetException | ResourceInstantiationException e){
			throw new ANoteException(e);
		}
	}

	private List<IEntityAnnotation> getEntities(Document gateDoc, AnnotationSet annotSetOriginal) throws ANoteException, InvalidOffsetException {

		long start = 0,end = 0;
		List<IEntityAnnotation> entities = new ArrayList<IEntityAnnotation>();
		AnnotationSet annotSetOriginalProt = annotSetOriginal.get("prot");
		Iterator<Annotation> annotIterator = annotSetOriginalProt.iterator();

		IAnoteClass klassToAdd = new AnoteClass(proteinClass);
		IAnoteClass klass = ClassPropertiesManagement.getClassIDOrinsertIfNotExist(klassToAdd);
		while(annotIterator.hasNext())
		{
			Annotation annot = annotIterator.next();
			start = annot.getStartNode().getOffset();
			end = annot.getEndNode().getOffset();
			String value =gateDoc.getContent().getContent(start, end).toString();
			IEntityAnnotation entity = new EntityAnnotationImpl(start, end,klass,null, value, false,true, null);
			entities.add(entity);
		}
		return entities;
	}


	private String getDocumentAbstract(Document gateDoc,AnnotationSet annotSetOriginal) throws InvalidOffsetException {
		return gateDoc.getContent().toString();
	}


	private String getDocumentTitle(Document gateDoc,AnnotationSet annotSetOriginal) throws InvalidOffsetException {
		return GateCorpusReaderLoader.getGeneralArticleInfo(gateDoc, annotSetOriginal,"ArticleTitle");
	}

	private List<String> getFilesList(File filepath) throws IOException {
		List<String> files = new ArrayList<String>();
		File filePath = new File(filepath.getAbsolutePath()+fileCorpusCheck);
		files = FileHandling.getFileLinesContent(filePath);
		return files;		
	}


	public boolean validateFile(File directory) {
		if(directory.isDirectory())
		{
			File test1 = new File(directory.getAbsolutePath()+fileCorpusCheck);
			File test2 = new File(directory.getAbsolutePath()+fileCorpusTags);
			return test1.exists() && test2.exists();
		}
		else
		{
			return false;
		}
	}

	public int corpusSize() {
		return this.corpusSize;
	}

	@Override
	public Map<Long,IAnnotatedDocument> getDocumentEntityAnnotations() {
		return documentswithEntities;
	}

	public List<IPublication> getDocuments() {
		return documents;
	}

	public void setCorpusSize(int corpusSize) {
		this.corpusSize = corpusSize;
	}

	@Override
	public void stop() {
		this.stop   = true;		
	}



}
