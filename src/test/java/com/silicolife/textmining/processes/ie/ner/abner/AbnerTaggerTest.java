package com.silicolife.textmining.processes.ie.ner.abner;

import static org.junit.Assert.assertTrue;
import gate.util.GateException;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.junit.Test;

import com.silicolife.textmining.core.datastructures.corpora.CorpusCreateConfigurationImpl;
import com.silicolife.textmining.core.datastructures.init.exception.InvalidDatabaseAccess;
import com.silicolife.textmining.core.datastructures.process.ir.configuration.IRSearchConfigurationImpl;
import com.silicolife.textmining.core.interfaces.core.corpora.ICorpusCreateConfiguration;
import com.silicolife.textmining.core.interfaces.core.dataaccess.exception.ANoteException;
import com.silicolife.textmining.core.interfaces.core.document.IPublication;
import com.silicolife.textmining.core.interfaces.core.document.corpus.CorpusTextType;
import com.silicolife.textmining.core.interfaces.core.document.corpus.ICorpus;
import com.silicolife.textmining.core.interfaces.core.report.corpora.ICorpusCreateReport;
import com.silicolife.textmining.core.interfaces.core.report.processes.INERProcessReport;
import com.silicolife.textmining.core.interfaces.core.report.processes.ir.IIRSearchProcessReport;
import com.silicolife.textmining.core.interfaces.process.IR.IIRSearchConfiguration;
import com.silicolife.textmining.core.interfaces.process.IR.exception.InternetConnectionProblemException;
import com.silicolife.textmining.ie.ner.abner.ABNER;
import com.silicolife.textmining.ie.ner.abner.ABNERTrainingModel;
import com.silicolife.textmining.ie.ner.abner.configuration.INERAbnerConfiguration;
import com.silicolife.textmining.ie.ner.abner.configuration.NERAbnerConfiguration;
import com.silicolife.textmining.processes.DatabaseConnectionInit;
import com.silicolife.textmining.processes.corpora.loaders.CorpusCreation;
import com.silicolife.textmining.processes.ir.pubmed.PubMedSearch;
import com.silicolife.wrappergate.GateInit;

public class AbnerTaggerTest {
	
	@Test
	public void test() throws InvalidDatabaseAccess, ANoteException, InternetConnectionProblemException, IOException, GateException {
		
		DatabaseConnectionInit.init("localhost","3306","createdatest","root","admin");
		GateInit.getInstance().init("gate8",null);
		ICorpus corpus = createCorpus().getCorpus();		
		ABNERTrainingModel model = ABNERTrainingModel.NLPBA;
//		ABNERTrainingModel model = ABNERTrainingModel.BIOCREATIVE;
		INERAbnerConfiguration configuration = new NERAbnerConfiguration(corpus, model);
		System.out.println("Abner Tagger");
		ABNER abner = new ABNER(configuration );
		INERProcessReport report = abner.executeCorpusNER(corpus);
		assertTrue(report.isFinishing());
	}
	
	public static ICorpusCreateReport createCorpus() throws InvalidDatabaseAccess,
	ANoteException, InternetConnectionProblemException {
		IIRSearchProcessReport report = createQuery();
		System.out.println("Create Corpus");
		CorpusCreation creation = new CorpusCreation();
		String corpusName = "Corpus test";
		CorpusTextType textType = CorpusTextType.Abstract;
		List<IPublication> publictions = report.getQuery().getPublications();
		Set<IPublication> docIds = new HashSet<>(publictions);
		String notes = new String();
		boolean journalRetrievalBefore = false;
		ICorpusCreateConfiguration configuration = new CorpusCreateConfigurationImpl(corpusName , notes , docIds , textType , journalRetrievalBefore);
		ICorpusCreateReport reportCreateCorpus = creation.createCorpus(configuration );
		return reportCreateCorpus;
	}

	public static IIRSearchProcessReport createQuery() throws InvalidDatabaseAccess,
	ANoteException, InternetConnectionProblemException {
		System.out.println("Create Query");
		PubMedSearch pubmedSearch = new PubMedSearch();
		// Properties
		Properties propeties = new Properties();
		// The query name resulted
		String queryName = "Escherichia coli AND Stringent response Advanced";
		// Organism
		String organism = "Escherichia coli";
		// Keywords
		String keywords = "Stringent response";
		// Add Author Filter
		//propeties.put("authors", "");
		// Add Journal Filter
		//propeties.put("journal", "");
		// Add Data Range
		//// From Date
		propeties.put("fromDate", "2008");
		//// To Date
		propeties.put("toDate", "2014");
		// Article Details Content
		//// Abstract Available
		//propeties.put("articleDetails", "abstract");
		//// Free full text
		propeties.put("articleDetails", "freefulltext");
		//// Full Text available
		//propeties.put("articleDetails", "fulltextavailable");
		// Article Source
		//// Medline Only
		//propeties.put("ArticleSource", "med");
		//// Pubmed Central Only
		propeties.put("ArticleSource", "pmc");
		//// Both
		//propeties.put("ArticleSource", "medpmc");
		// Article Type
		//propeties.put("articletype", "Revision");

		IIRSearchConfiguration searchConfiguration = new IRSearchConfigurationImpl(keywords , organism , queryName, propeties );
		IIRSearchProcessReport report = pubmedSearch.search(searchConfiguration);
		return report;
	}

}
