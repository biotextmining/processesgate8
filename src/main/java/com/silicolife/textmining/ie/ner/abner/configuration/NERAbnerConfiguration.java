package com.silicolife.textmining.ie.ner.abner.configuration;

import java.util.Map;

import com.silicolife.textmining.core.datastructures.process.ner.NERConfigurationImpl;
import com.silicolife.textmining.core.interfaces.core.document.corpus.ICorpus;
import com.silicolife.textmining.ie.ner.abner.ABNER;
import com.silicolife.textmining.ie.ner.abner.ABNERTrainingModel;

public class NERAbnerConfiguration extends NERConfigurationImpl implements INERAbnerConfiguration{

	public static String nerAbnerUID = "ner.abner";

	
	private ABNERTrainingModel model;
	
	public NERAbnerConfiguration(ICorpus corpus, ABNERTrainingModel model) {
		super(corpus, ABNER.nerAbner, ABNER.nerAbner);
		this.model = model;
	}

	public boolean isNormalized() {
		return false;
	}

	public ABNERTrainingModel getModel() {
		return model;
	}

	@Override
	public Map<String, String> getNERProperties() {
		Map<String, String> properties = new java.util.HashMap<>();
		properties.put(NERAbnerDefaultSettings.MODEL, model.name());
		return properties;
	}

	@Override
	public void setConfiguration(Object obj) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getConfigurationUID() {
		return NERAbnerConfiguration.nerAbnerUID;
	}

	@Override
	public void setConfigurationUID(String uid) {
		NERAbnerConfiguration.nerAbnerUID = uid;
		
	}

}
