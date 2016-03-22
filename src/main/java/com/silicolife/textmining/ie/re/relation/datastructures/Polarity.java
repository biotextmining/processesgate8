package com.silicolife.textmining.ie.re.relation.datastructures;

import java.util.HashSet;
import java.util.Set;

import com.silicolife.textmining.core.interfaces.core.annotation.re.IPolarity;
import com.silicolife.textmining.core.interfaces.core.annotation.re.PolarityEnum;
import com.silicolife.textmining.core.interfaces.process.IE.re.clue.IVerbInfo;

public class Polarity implements IPolarity{
	
	/**
	 * List of verb who have negativety
	 */
	private Set<String> negVerbs;
	
	/**
	 * List of Conditional Verbs
	 */
	private Set<String> coditionalVerbs;
	
	public Polarity()
	{
		this.negVerbs=getNegativilyVerbs();
		this.coditionalVerbs=getConditionalVerbs();
	}
	
	
	public PolarityEnum getPolarity(IVerbInfo verbInfo)
	{
		for(String codVerbs:coditionalVerbs)
		{
			if(verbInfo.getLemma().contains(codVerbs))
				return PolarityEnum.Conditional;
		}
		if(this.negVerbs.contains(verbInfo.getVerb()))
		{
			return PolarityEnum.Negative;
		}
		else
		{
			return verbInfo.getPolarity();
		}
	}
	
	public Set<String> getNegativilyVerbs()
	{
		Set<String> negVerbs = new HashSet<String>();
		negVerbs.add("lack");
		negVerbs.add("fail");
		negVerbs.add("inactivates");
		negVerbs.add("inactivates");
		negVerbs.add("inactivated");
		negVerbs.add("inactivation");
		negVerbs.add("inhibits");
		negVerbs.add("inhibition");
		return negVerbs;	
	}
	
	public void setNegativilyVerbs(Set<String> negVerbs)
	{
		this.negVerbs=negVerbs;
	}
	
	public Set<String> getConditionalVerbs()
	{
		Set<String> codVerbs = new HashSet<String>();
		codVerbs.add("can");
		codVerbs.add("may");
		codVerbs.add("should");
		codVerbs.add("could");		
		codVerbs.add("would");	
		return codVerbs;	
	}

	public void setConditionalVerbsSet(Set<String> coditionalVerbs) {
		this.coditionalVerbs=coditionalVerbs;
		
	}
	

}
