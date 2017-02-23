package com.silicolife.textmining.ie.re.relation.configuration;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.silicolife.textmining.core.datastructures.process.IEProcessImpl;
import com.silicolife.textmining.core.datastructures.process.ProcessRunStatusConfigurationEnum;
import com.silicolife.textmining.core.datastructures.process.ProcessTypeImpl;
import com.silicolife.textmining.core.datastructures.process.re.REConfigurationImpl;
import com.silicolife.textmining.core.datastructures.resources.lexiacalwords.LexicalWordsImpl;
import com.silicolife.textmining.core.datastructures.utils.Utils;
import com.silicolife.textmining.core.interfaces.core.document.corpus.ICorpus;
import com.silicolife.textmining.core.interfaces.process.IE.IIEProcess;
import com.silicolife.textmining.core.interfaces.process.IE.re.IRelationsType;
import com.silicolife.textmining.core.interfaces.resource.lexicalwords.ILexicalWords;
import com.silicolife.textmining.ie.re.relation.RelationsExtraction;
import com.silicolife.textmining.ie.re.relation.datastructures.GatePOSTaggerEnum;
import com.silicolife.textmining.ie.re.relation.models.RelationsModelEnem;

public class RERelationConfigurationImpl extends REConfigurationImpl implements IRERelationConfiguration{
	
	public static final  String reRelationUID = "re.relation";


	private GatePOSTaggerEnum posTaggerEnum;
	private RelationsModelEnem relationModelEnum;
	private ILexicalWords verbsFilter;
	private ILexicalWords verbsAddition;
	private ILexicalWords verbsClues;
	private IRERelationAdvancedConfiguration advancedConfiguration;
	
	public RERelationConfigurationImpl()
	{
		super();
	}
	
	public RERelationConfigurationImpl(ICorpus corpus,ProcessRunStatusConfigurationEnum processRunStatusConfigurationEnum,IIEProcess entityProcess,boolean useManualCurationFromOtherProcess,IIEProcess manualCurationFromOtherProcess,
			GatePOSTaggerEnum posTaggerEnum,RelationsModelEnem relationModelEnum,
			ILexicalWords verbFilter,ILexicalWords verbAdittion,ILexicalWords verbClues,IRERelationAdvancedConfiguration advancedConfiguration) {
		super(RelationsExtraction.relationName,corpus,build(corpus),processRunStatusConfigurationEnum, entityProcess,useManualCurationFromOtherProcess,manualCurationFromOtherProcess);
		this.posTaggerEnum = posTaggerEnum;
		this.relationModelEnum = relationModelEnum;
		this.verbsFilter = verbFilter;
		this.verbsAddition = verbAdittion;
		this.verbsClues = verbClues;
		this.advancedConfiguration = advancedConfiguration;
	}
	
	private static IIEProcess build(ICorpus corpus)
	{
		String name = RelationsExtraction.relationName+" "+Utils.SimpleDataFormat.format(new Date());
		String notes = new String();
		Properties properties = new Properties();
		IIEProcess reProcess = new IEProcessImpl(corpus, name,notes,ProcessTypeImpl.getREProcessType(), RelationsExtraction.relationProcessType, properties);
		return reProcess;
	}

	@Override
	public GatePOSTaggerEnum getPosTaggerEnum() {
		return posTaggerEnum;
	}
	
	public void setPosTaggerEnum(GatePOSTaggerEnum posTaggerEnum) {
		this.posTaggerEnum = posTaggerEnum;
	}

	@Override
	public RelationsModelEnem getRelationModelEnum() {
		return relationModelEnum;
	}
	
	public void setRelationModelEnum(RelationsModelEnem relationModelEnum) {
		this.relationModelEnum = relationModelEnum;
	}

	@Override
	@JsonDeserialize(as=LexicalWordsImpl.class)
	public ILexicalWords getVerbsFilter() {
		return verbsFilter;
	}
	

	public void setVerbsFilter(ILexicalWords verbsFilter) {
		this.verbsFilter = verbsFilter;
	}

	@Override
	public ILexicalWords getVerbsAddition() {
		return verbsAddition;
	}

	public void setVerbsAddition(ILexicalWords verbsAddition) {
		this.verbsAddition = verbsAddition;
	}

	@Override
	@JsonDeserialize(as=RERelationAdvancedConfigurationImpl.class)
	public IRERelationAdvancedConfiguration getAdvancedConfiguration() {
		return advancedConfiguration;
	}

	public void setAdvancedConfiguration(
			IRERelationAdvancedConfiguration advancedConfiguration) {
		this.advancedConfiguration = advancedConfiguration;
	}

	@Override
	@JsonIgnore
	public Map<String, String> getREProperties() {
		Map<String, String> properties = new HashMap<String, String>();
		properties.put(RERelationDefaultSettings.POSTAGGER, posTaggerEnum.getPOSTagger(verbsFilter,verbsAddition).getUID());
		properties.put(RERelationDefaultSettings.MODEL, relationModelEnum.getRelationModel(this).getUID());
		long verbfilterID = 0;
		if(verbsFilter!=null)
		{
			verbfilterID = verbsFilter.getId();
		}
		properties.put(RERelationDefaultSettings.VERB_FILTER, String.valueOf(verbfilterID !=0));
		properties.put(RERelationDefaultSettings.VERB_FILTER_LEXICAL_WORDS_ID, String.valueOf(verbfilterID));
		long verbAddiction = 0;
		if(verbsAddition!=null)
		{
			verbAddiction = verbsAddition.getId();
		}
		long verbCluesID = 0;
		if(this.verbsClues!=null)
		{
			verbCluesID = verbsClues.getId();
		}
		properties.put(RERelationDefaultSettings.BIOMEDICAL_VERB_MODEL, String.valueOf(verbCluesID));
		properties.put(RERelationDefaultSettings.VERB_ADDITION, String.valueOf(verbAddiction !=0));
		properties.put(RERelationDefaultSettings.VERB_ADDITION_LEXICAL_WORDS_ID, String.valueOf(verbAddiction));
		properties.put(RERelationDefaultSettings.ADVANCED_VERB_ENTITES_MAX_DISTANCE, String.valueOf(0));
		properties.put(RERelationDefaultSettings.ADVANCED_ONLY_USE_ENTITY_TO_NEAREST_VERB, String.valueOf(getAdvancedConfiguration().isUsingOnlyVerbNearestEntities()));
		properties.put(RERelationDefaultSettings.ADVANCED_ONLY_NEAREST_VERB_ENTITIES, String.valueOf(getAdvancedConfiguration().isUsingOnlyEntitiesNearestVerb()));
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
	@JsonDeserialize(as=LexicalWordsImpl.class)
	public ILexicalWords getVerbsClues() {
		return verbsClues;
	}

	public void setVerbsClues(ILexicalWords verbsClues) {
		this.verbsClues = verbsClues;
	}

	@Override
	public String getConfigurationUID() {
		return RERelationConfigurationImpl.reRelationUID;
	}


}
