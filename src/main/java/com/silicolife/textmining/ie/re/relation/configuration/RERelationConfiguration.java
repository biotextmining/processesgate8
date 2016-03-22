package com.silicolife.textmining.ie.re.relation.configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

import com.silicolife.textmining.core.datastructures.process.re.REConfigurationImpl;
import com.silicolife.textmining.core.interfaces.core.document.corpus.ICorpus;
import com.silicolife.textmining.core.interfaces.process.IE.IIEProcess;
import com.silicolife.textmining.core.interfaces.process.IE.re.IRelationModel;
import com.silicolife.textmining.core.interfaces.process.IE.re.IRelationsType;
import com.silicolife.textmining.core.interfaces.resource.lexicalwords.ILexicalWords;
import com.silicolife.textmining.ie.re.relation.RelationsExtraction;
import com.silicolife.wrappergate.IGatePosTagger;

public class RERelationConfiguration extends REConfigurationImpl implements IRERelationConfiguration{

	private IGatePosTagger posTagger;
	private IRelationModel relationModel;
	private ILexicalWords verbFilter;
	private ILexicalWords verbAdittion;
	private ILexicalWords verbclues;
	private IRERelationAdvancedConfiguration advancedConfiguration;
	
	public RERelationConfiguration(ICorpus corpus, IIEProcess entityProcess,boolean useManualCurationFromOtherProcess,IIEProcess manualCurationFromOtherProcess,
			IGatePosTagger posTagger,IRelationModel relationModel,
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
	public IGatePosTagger getPOSTagger() {
		return posTagger;
	}

	@Override
	public IRelationModel getRelationModel() {
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
		properties.put(RERelationDefaultSettings.POSTAGGER, posTagger.getUID());
		properties.put(RERelationDefaultSettings.MODEL, relationModel.getUID());
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

}
