package com.silicolife.textmining.processes.ie.ner.chemistrytagger;

import static org.junit.Assert.assertTrue;
import gate.util.GateException;

import java.io.IOException;

import org.junit.Test;

import com.silicolife.textmining.core.datastructures.exceptions.process.InvalidConfigurationException;
import com.silicolife.textmining.core.datastructures.init.exception.InvalidDatabaseAccess;
import com.silicolife.textmining.core.interfaces.core.dataaccess.exception.ANoteException;
import com.silicolife.textmining.core.interfaces.core.document.corpus.ICorpus;
import com.silicolife.textmining.core.interfaces.core.report.processes.INERProcessReport;
import com.silicolife.textmining.core.interfaces.process.IR.exception.InternetConnectionProblemException;
import com.silicolife.textmining.ie.ner.chemistrytagger.ChemistryTagger;
import com.silicolife.textmining.ie.ner.chemistrytagger.configuration.INERChemistryTaggerConfiguration;
import com.silicolife.textmining.ie.ner.chemistrytagger.configuration.NERChemistryTaggerConfiguration;
import com.silicolife.textmining.processes.DatabaseConnectionInit;
import com.silicolife.textmining.processes.ie.ner.abner.AbnerTaggerTest;
import com.silicolife.wrappergate.GateInit;

public class ChemistryTaggerTest{
	
	@Test
	public void test() throws InvalidDatabaseAccess, ANoteException, InternetConnectionProblemException, IOException, GateException, InvalidConfigurationException {
		
		DatabaseConnectionInit.init("localhost","3306","createdatest","root","admin");
		GateInit.getInstance().init("gate8",null);
		ICorpus corpus = AbnerTaggerTest.createCorpus().getCorpus();		
		System.out.println("Chemistry Tagger Tagger");
		boolean chemistryCompound = true;
		boolean chemistryElements= true;;
		boolean chemistrylIon= true;;
		INERChemistryTaggerConfiguration configuration = new NERChemistryTaggerConfiguration(corpus, chemistryElements, chemistryCompound, chemistrylIon);
		ChemistryTagger chemTagger = new ChemistryTagger();
		INERProcessReport report = chemTagger.executeCorpusNER(configuration);
		assertTrue(report.isFinishing());
	}

}
