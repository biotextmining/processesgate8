package com.silicolife.textmining.processes.ie.re.relation;

import static org.junit.Assert.assertTrue;
import gate.util.GateException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.regex.Pattern;

import org.junit.Test;

import com.silicolife.textmining.core.datastructures.exceptions.process.InvalidConfigurationException;
import com.silicolife.textmining.core.datastructures.init.InitConfiguration;
import com.silicolife.textmining.core.datastructures.init.exception.InvalidDatabaseAccess;
import com.silicolife.textmining.core.datastructures.process.ner.NERCaseSensativeEnum;
import com.silicolife.textmining.core.datastructures.process.ner.ResourcesToNerAnote;
import com.silicolife.textmining.core.datastructures.resources.dictionary.loaders.DictionaryImpl;
import com.silicolife.textmining.core.datastructures.resources.dictionary.loaders.configuration.DictionaryLoaderConfigurationImpl;
import com.silicolife.textmining.core.datastructures.resources.lexiacalwords.LexicalWordsImpl;
import com.silicolife.textmining.core.datastructures.resources.lookuptable.loader.csvstandard.ColumnNames;
import com.silicolife.textmining.core.datastructures.utils.generic.CSVFileConfigurations;
import com.silicolife.textmining.core.datastructures.utils.generic.ColumnDelemiterDefaultValue;
import com.silicolife.textmining.core.datastructures.utils.generic.ColumnParameters;
import com.silicolife.textmining.core.interfaces.core.dataaccess.exception.ANoteException;
import com.silicolife.textmining.core.interfaces.core.document.corpus.ICorpus;
import com.silicolife.textmining.core.interfaces.core.report.processes.INERProcessReport;
import com.silicolife.textmining.core.interfaces.core.report.processes.IREProcessReport;
import com.silicolife.textmining.core.interfaces.process.IE.IIEProcess;
import com.silicolife.textmining.core.interfaces.process.IE.io.export.DefaultDelimiterValue;
import com.silicolife.textmining.core.interfaces.process.IE.io.export.Delimiter;
import com.silicolife.textmining.core.interfaces.process.IE.io.export.TextDelimiter;
import com.silicolife.textmining.core.interfaces.process.IE.re.IRelationsType;
import com.silicolife.textmining.core.interfaces.process.IR.exception.InternetConnectionProblemException;
import com.silicolife.textmining.core.interfaces.resource.IResource;
import com.silicolife.textmining.core.interfaces.resource.IResourceElement;
import com.silicolife.textmining.core.interfaces.resource.dictionary.IDictionary;
import com.silicolife.textmining.core.interfaces.resource.dictionary.configuration.IDictionaryLoaderConfiguration;
import com.silicolife.textmining.core.interfaces.resource.lexicalwords.ILexicalWords;
import com.silicolife.textmining.ie.re.relation.RelationsExtraction;
import com.silicolife.textmining.ie.re.relation.configuration.IRERelationAdvancedConfiguration;
import com.silicolife.textmining.ie.re.relation.configuration.IRERelationConfiguration;
import com.silicolife.textmining.ie.re.relation.configuration.RERelationAdvancedConfigurationImpl;
import com.silicolife.textmining.ie.re.relation.configuration.RERelationConfigurationImpl;
import com.silicolife.textmining.ie.re.relation.datastructures.GatePOSTaggerEnum;
import com.silicolife.textmining.ie.re.relation.models.RelationsModelEnem;
import com.silicolife.textmining.processes.DatabaseConnectionInit;
import com.silicolife.textmining.processes.ie.ner.abner.AbnerTaggerTest;
import com.silicolife.textmining.processes.ie.ner.linnaeus.LinnaeusTagger;
import com.silicolife.textmining.processes.ie.ner.linnaeus.adapt.uk.ac.man.entitytagger.matching.Matcher.Disambiguation;
import com.silicolife.textmining.processes.ie.ner.linnaeus.configuration.INERLinnaeusConfiguration;
import com.silicolife.textmining.processes.ie.ner.linnaeus.configuration.NERLinnaeusConfigurationImpl;
import com.silicolife.textmining.processes.ie.ner.linnaeus.configuration.NERLinnaeusPreProcessingEnum;
import com.silicolife.textmining.processes.resources.dictionary.loaders.byocyc.BioMetaEcoCycFlatFileLoader;
import com.silicolife.textmining.processes.resources.lexicalwords.csvlader.LexicalWordsCSVLoader;
import com.silicolife.wrappergate.GateInit;


public class RelationExtrationTest{
	
	
	@Test
	public void test() throws InvalidDatabaseAccess, ANoteException, InternetConnectionProblemException, IOException, GateException, InvalidConfigurationException {
		
		DatabaseConnectionInit.init("localhost","3306","createdatest","root","admin");
		GateInit.getInstance().init("gate8",null);
		ICorpus corpus = AbnerTaggerTest.createCorpus().getCorpus();		
		IDictionary dictionary = createDictionaryAndUpdateditWithByocycFiles();
		IIEProcess entityProcess = executeLinnaeus(corpus,dictionary).getNERProcess();
		System.out.println("Relation Extraction");
		IIEProcess manualCurationFromOtherProcess = null;
		ILexicalWords verbClues = new LexicalWordsImpl(getBiomedicalVerbs());
		ILexicalWords verbFilter = null;
		boolean useManualCurationFromOtherProcess = false;
		boolean usingOnlyVerbNearestEntities = false;
		boolean usingOnlyEntitiesNearestVerb = false;
		int verbEntitieMaxDistance = 10;
		SortedSet<IRelationsType> relationsType = null;
		boolean groupingSynonyms = true;
		IRERelationAdvancedConfiguration advancedConfiguration = new RERelationAdvancedConfigurationImpl(usingOnlyVerbNearestEntities, usingOnlyEntitiesNearestVerb, verbEntitieMaxDistance, groupingSynonyms , relationsType,verbClues );
		GatePOSTaggerEnum posTagger = GatePOSTaggerEnum.LingPipe_POS;
		ILexicalWords verbCluesAdittion = null;
		RelationsModelEnem relationModel = RelationsModelEnem.Binary_Biomedical_Verbs;
		IRERelationConfiguration configuration = new RERelationConfigurationImpl(corpus, entityProcess, useManualCurationFromOtherProcess, manualCurationFromOtherProcess, posTagger, relationModel , verbFilter, verbCluesAdittion, verbClues, advancedConfiguration);
		RelationsExtraction relation = new RelationsExtraction();
		IREProcessReport report = relation.executeRE(configuration);
		System.out.println(report.getNumberOFEntities());
		System.out.println(report.getNumberOfRelations());

		assertTrue(report.isFinishing());
	}
	
	public static INERProcessReport executeLinnaeus(ICorpus corpus,IDictionary dictionary) throws ANoteException, InvalidConfigurationException {
		boolean useabreviation = true;
		boolean normalized = true;
		NERCaseSensativeEnum caseSensitive = NERCaseSensativeEnum.INALLWORDS;
		ILexicalWords stopwords = null;
		NERLinnaeusPreProcessingEnum preprocessing = NERLinnaeusPreProcessingEnum.No;
		Disambiguation disambiguation = Disambiguation.OFF;
		ResourcesToNerAnote resourceToNER = new ResourcesToNerAnote();
		resourceToNER.addUsingAnoteClasses(dictionary, dictionary.getResourceClassContent(), dictionary.getResourceClassContent());
		Map<String, Pattern> patterns = new HashMap<String, Pattern>();
		int numThreads = 4;
		boolean usingOtherResourceInfoToImproveRuleAnnotations = false;
		INERLinnaeusConfiguration configurations = new NERLinnaeusConfigurationImpl(corpus, patterns , resourceToNER, useabreviation , disambiguation , caseSensitive , normalized , numThreads , stopwords , preprocessing , usingOtherResourceInfoToImproveRuleAnnotations );
		LinnaeusTagger linnaues = new LinnaeusTagger( );
		System.out.println("Execute Linnaeus");
		INERProcessReport report = linnaues.executeCorpusNER(configurations);
		return report;
	}
	
	public static IDictionary createDictionaryAndUpdateditWithByocycFiles()
			throws ANoteException, IOException {
		System.out.println("Create Dictionary");
		IResource<IResourceElement> resource = createDictionary("Biocyc");
		IDictionary dictionary = new DictionaryImpl(resource);
		BioMetaEcoCycFlatFileLoader loader = new BioMetaEcoCycFlatFileLoader();
		String byocycFolder = "src/test/resources/BioCyc/small";
		File file = new File(byocycFolder);
		if(loader.checkFile(file))
		{
			Properties properties = new Properties();
			String loaderUID = "";
			boolean loadExtendalIDds = true;
			IDictionaryLoaderConfiguration configuration = new DictionaryLoaderConfigurationImpl(loaderUID , dictionary, file, properties , loadExtendalIDds );
			loader.loadTerms(configuration );
		}
		return dictionary;
	}
	
	public static IResource<IResourceElement> createDictionary(String name) throws ANoteException {
		String info = "put notes";
		IResource<IResourceElement> newDictionary = new DictionaryImpl(name, info, true);
		InitConfiguration.getDataAccess().createResource(newDictionary);
		return newDictionary;
	}

	public IResource<IResourceElement> getBiomedicalVerbs() throws ANoteException, IOException
	{
		IResource<IResourceElement> lw = createLexicalWords("Biomedical Verbs");
		LexicalWordsCSVLoader lwCSVLoader = new LexicalWordsCSVLoader();
		boolean hasHeaders = false;
		Map<String, ColumnParameters> columnNameColumnParameters = new HashMap<String, ColumnParameters>();
		ColumnParameters value = new ColumnParameters(0, null, null);
		columnNameColumnParameters.put(ColumnNames.term, value );
		ColumnDelemiterDefaultValue columsDelemiterDefaultValue = new ColumnDelemiterDefaultValue(columnNameColumnParameters);
		Delimiter generalDelimiter = Delimiter.TAB;
		DefaultDelimiterValue defaultValue = DefaultDelimiterValue.NONE;
		TextDelimiter textDelimiters = TextDelimiter.NONE;
		CSVFileConfigurations csvfileconfigurations = new CSVFileConfigurations(generalDelimiter, textDelimiters, defaultValue, columsDelemiterDefaultValue, hasHeaders);
		File file = new File("src/test/resources/biological_verbs.csv");
		lwCSVLoader.loadTermFromGenericCVSFile(lw, file, csvfileconfigurations);
		return lw;
	}
	
	public static IResource<IResourceElement> createLexicalWords(String name) throws ANoteException {
		String info = "put notes";
		IResource<IResourceElement> newLexicalWords = new LexicalWordsImpl(name, info, true);
		InitConfiguration.getDataAccess().createResource(newLexicalWords);
		return newLexicalWords;
	}
}
