package com.silicolife.textmining.ie.ner.abner.configuration;

import java.util.Date;
import java.util.Map;

import com.silicolife.textmining.core.datastructures.process.IEProcessImpl;
import com.silicolife.textmining.core.datastructures.process.ProcessRunStatusConfigurationEnum;
import com.silicolife.textmining.core.datastructures.process.ProcessTypeImpl;
import com.silicolife.textmining.core.datastructures.process.ner.NERConfigurationImpl;
import com.silicolife.textmining.core.datastructures.utils.Utils;
import com.silicolife.textmining.core.interfaces.core.document.corpus.ICorpus;
import com.silicolife.textmining.core.interfaces.process.IE.IIEProcess;
import com.silicolife.textmining.ie.ner.abner.ABNER;
import com.silicolife.textmining.ie.ner.abner.ABNERTrainingModel;
import com.silicolife.textmining.processes.ie.ner.linnaeus.adapt.martin.common.Properties;

public class NERAbnerConfiguration extends NERConfigurationImpl implements INERAbnerConfiguration{

	public static String nerAbnerUID = "ner.abner";

	
	private ABNERTrainingModel model;
	
	public NERAbnerConfiguration(ICorpus corpus, ABNERTrainingModel model,ProcessRunStatusConfigurationEnum processRunStatusConfigurationEnum) {
		super(corpus,nerAbnerUID,build(corpus),processRunStatusConfigurationEnum);
		this.model = model;
	}
	
	private static IIEProcess build(ICorpus corpus)
	{
		String description = ABNER.nerAbner  + " " +Utils.SimpleDataFormat.format(new Date());
		String notes =  new String();
		Properties properties = new Properties();
		IIEProcess process = new IEProcessImpl(corpus, description , notes, ProcessTypeImpl.getNERProcessType(), ABNER.nerAbnerOrigin, properties);
		return process;
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
