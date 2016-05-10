package com.silicolife.textmining.ie.re.relation.configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

import com.silicolife.textmining.core.datastructures.process.re.REConfigurationImpl;
import com.silicolife.textmining.core.interfaces.core.document.corpus.ICorpus;
import com.silicolife.textmining.core.interfaces.process.IE.IIEProcess;
import com.silicolife.textmining.core.interfaces.process.IE.re.IRelationsType;
import com.silicolife.textmining.core.interfaces.resource.lexicalwords.ILexicalWords;
import com.silicolife.textmining.ie.re.relation.RelationsExtraction;
import com.silicolife.textmining.ie.re.relation.datastructures.GatePOSTaggerEnum;
import com.silicolife.textmining.ie.re.relation.models.RelationsModelEnem;

public class RERelationConfiguration extends REConfigurationImpl implements IRERelationConfiguration{
	
	public static String reRelationUID = "re.relation";


	private GatePOSTaggerEnum posTagger;
	private RelationsModelEnem relationModel;
	private ILexicalWords verbFilter;
	private ILexicalWords verbAdittion;
	private ILexicalWords verbclues;
	private IRERelationAdvancedConfiguration advancedConfiguration;
	
	public RERelationConfiguration(ICorpus corpus, IIEProcess entityProcess,boolean useManualCurationFromOtherProcess,IIEProcess manualCurationFromOtherProcess,
			GatePOSTaggerEnum posTagger,RelationsModelEnem relationModel,
			ILexicalWords verbFilter,ILexicalWords verbAdittion,ILexicalWords verbClues,IRERelationAdvancedConfiguration advancedConfiguration) {
		super(RelationsExtraction.relationName,corpus, entityProcess,useManualCurationFromOtherProcess,manualCurationFromOtherProcess);
		this.posTagger = posTagger;
		this.relationModel = relationModel;
		this.verbFilter = verbFilter;
		this.verbAdittion = verbAdittion;
		this.verbclues = verbClues;
		this.advancedConfiguration = advancedConfiguration;
	}

	@Override
	public GatePOSTaggerEnum getPOSTaggerEnum() {
		return posTagger;
	}

	@Override
	public RelationsModelEnem getRelationModelEnum() {
		return relationModel;
	}

	@Override
	public ILexicalWords getVerbsFilter() {
		return verbFilter;
	}

	@Override
	public ILexicalWords getVerbsAddition() {
		return verbAdittion;
	}

	@Override
	public IRERelationAdvancedConfiguration getAdvancedConfiguration() {
		return advancedConfiguration;
	}

	@Override
	public Map<String, String> getREProperties() {
		Map<String, String> properties = new HashMap<String, String>();
		properties.put(RERelationDefaultSettings.POSTAGGER, posTagger.getPOSTagger(verbFilter,verbAdittion).getUID());
		properties.put(RERelationDefaultSettings.MODEL, relationModel.getRelationModel(this).getUID());
		long verbfilterID = 0;
		if(verbFilter!=null)
		{
			verbfilterID = verbFilter.getId();
		}
		properties.put(RERelationDefaultSettings.VERB_FILTER, String.valueOf(verbfilterID !=0));
		properties.put(RERelationDefaultSettings.VERB_FILTER_LEXICAL_WORDS_ID, String.valueOf(verbfilterID));
		long verbAddiction = 0;
		if(verbAdittion!=null)
		{
			verbAddiction = verbAdittion.getId();
		}
		long verbCluesID = 0;
		if(this.verbclues!=null)
		{
			verbCluesID = verbclues.getId();
		}
		properties.put(RERelationDefaultSettings.BIOMEDICAL_VERB_MODEL, String.valueOf(verbCluesID));
		properties.put(RERelationDefaultSettings.VERB_ADDITION, String.valueOf(verbAddiction !=0));
		properties.put(RERelationDefaultSettings.VERB_ADDITION_LEXICAL_WORDS_ID, String.valueOf(verbAddiction));
		properties.put(RERelationDefaultSettings.ADVANCED_VERB_ENTITES_MAX_DISTANCE, String.valueOf(0));
		properties.put(RERelationDefaultSettings.ADVANCED_ONLY_USE_ENTITY_TO_NEAREST_VERB, String.valueOf(getAdvancedConfiguration().usingOnlyVerbNearestEntities()));
		properties.put(RERelationDefaultSettings.ADVANCED_ONLY_NEAREST_VERB_ENTITIES, String.valueOf(getAdvancedConfiguration().usingOnlyEntitiesNearestVerb()));
		properties.put(RERelationDefaultSettings.ADVANCED_RELATIONS_TYPE, convertShortedRelationTypeIntoString(getAdvancedConfiguration().getRelationsType()));
		return properties;
	}
	
	public static String convertShortedRelationTypeIntoString(Object relations)
	{
		if(relations instanceof SortedSet)
		{
			String trToString = new String();
			SortedSet<?> rt = (SortedSet<?>) relations;
			for(Object item:rt)
			{
				if(item instanceof IRelationsType)
				{
					IRelationsType rtT = (IRelationsType) item;
					trToString = trToString + rtT.toString() + ",";
				}
			}
			if(trToString.length() < 3)
			{
				return "all";
			}
			else
			{
				return trToString.substring(0, trToString.length()-1);
			}
		}
		else
		{
			return "all";
		}
		
	}

	@Override
	public ILexicalWords getVerbsClues() {
		return verbclues;
	}

	@Override
	public String getConfigurationUID() {
		return RERelationConfiguration.reRelationUID;
	}

	@Override
	public void setConfigurationUID(String uid) {
		RERelationConfiguration.reRelationUID=uid;
		
	}

}
