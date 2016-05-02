package com.silicolife.textmining.ie.ner.chemistrytagger.configuration;

import java.util.Map;

import com.silicolife.textmining.core.datastructures.process.ner.NERConfigurationImpl;
import com.silicolife.textmining.core.interfaces.core.document.corpus.ICorpus;
import com.silicolife.textmining.ie.ner.chemistrytagger.ChemistryTagger;

public class NERChemistryTaggerConfiguration extends NERConfigurationImpl implements INERChemistryTaggerConfiguration{

	public static String nerChemistryTaggerUID = "ner.chemistrytagger";

	private boolean chemistryElements;
	private boolean chemistryCompounds;
	private boolean chemistrylIon;
	
	public NERChemistryTaggerConfiguration(ICorpus corpus,boolean chemistryElements,boolean chemistryCompound,boolean chemistrylIon) {
		super(corpus, ChemistryTagger.nerChemistryTagger,ChemistryTagger.nerChemistryTagger);
		this.chemistryElements = chemistryElements;
		this.chemistryCompounds = chemistryCompound;
		this.chemistrylIon = chemistrylIon;
	}

	public boolean findChemistryElements() {
		return chemistryElements;
	}

	public boolean findChemistryCompounds() {
		return chemistryCompounds;
	}

	public boolean findChemistrylIon() {
		return chemistrylIon;
	}

	@Override
	public Map<String, String> getNERProperties() {
		Map<String, String> properties = new java.util.HashMap<>();
		properties.put(NERChemistryTaggerDefaultSettings.CHEMISTRY_ELEMENTS, String.valueOf(chemistryElements));
		properties.put(NERChemistryTaggerDefaultSettings.CHEMISTRY_COMPOUNDS, String.valueOf(chemistryCompounds));
		properties.put(NERChemistryTaggerDefaultSettings.CHEMISTRY_ION, String.valueOf(chemistrylIon));
		return properties;
	}

	@Override
	public void setConfiguration(Object obj) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getConfigurationUID() {
		return NERChemistryTaggerConfiguration.nerChemistryTaggerUID;
	}

	@Override
	public void setConfigurationUID(String uid) {
		NERChemistryTaggerConfiguration.nerChemistryTaggerUID=uid;
		
	}

}
