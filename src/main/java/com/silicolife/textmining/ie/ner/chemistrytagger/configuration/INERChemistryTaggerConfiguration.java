package com.silicolife.textmining.ie.ner.chemistrytagger.configuration;

import com.silicolife.textmining.core.interfaces.process.IE.ner.INERConfiguration;

public interface INERChemistryTaggerConfiguration extends INERConfiguration{
	public boolean findChemistryElements();
	public boolean findChemistryCompounds();
	public boolean findChemistrylIon();

}
