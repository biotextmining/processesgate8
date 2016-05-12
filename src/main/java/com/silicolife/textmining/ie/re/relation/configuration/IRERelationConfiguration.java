package com.silicolife.textmining.ie.re.relation.configuration;

import com.silicolife.textmining.core.interfaces.process.IE.re.IREConfiguration;
import com.silicolife.textmining.core.interfaces.resource.lexicalwords.ILexicalWords;
import com.silicolife.textmining.ie.re.relation.datastructures.GatePOSTaggerEnum;
import com.silicolife.textmining.ie.re.relation.models.RelationsModelEnem;

public interface IRERelationConfiguration extends IREConfiguration{
	
	public GatePOSTaggerEnum getPosTaggerEnum();
	public RelationsModelEnem getRelationModelEnum();
	public ILexicalWords getVerbsFilter();
	public ILexicalWords getVerbsAddition();
	public ILexicalWords getVerbsClues();
	public IRERelationAdvancedConfiguration getAdvancedConfiguration();

}
