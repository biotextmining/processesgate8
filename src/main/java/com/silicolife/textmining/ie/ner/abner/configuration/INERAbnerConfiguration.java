package com.silicolife.textmining.ie.ner.abner.configuration;

import com.silicolife.textmining.core.interfaces.process.IE.ner.INERConfiguration;
import com.silicolife.textmining.ie.ner.abner.ABNERTrainingModel;

public interface INERAbnerConfiguration extends INERConfiguration {
	public boolean isNormalized();
	public ABNERTrainingModel getModel();
}
