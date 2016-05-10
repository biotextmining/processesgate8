package com.silicolife.textmining.ie.re.relation.datastructures;

import com.silicolife.textmining.core.datastructures.language.LanguageProperties;
import com.silicolife.textmining.core.interfaces.resource.lexicalwords.ILexicalWords;
import com.silicolife.wrappergate.IGatePosTagger;
import com.silicolife.wrappergate.tagger.Gate7PosTagger;
import com.silicolife.wrappergate.tagger.LingPipePosTagger;

public enum GatePOSTaggerEnum {
	Gate_POS
	{
		public IGatePosTagger getPOSTagger(ILexicalWords verbFilter,ILexicalWords verbAddition){
			return new Gate7PosTagger(new Directionality(),new Polarity(),verbFilter,verbAddition);
		}
		
		public String toString(){
			return "Gate POS Tagging";
		}
		
		public String getDescrition()
		{
			return LanguageProperties.getLanguageStream("pt.uminho.anote2.relation.postagging.gateannie.info")+" http://gate.ac.uk/sale/tao/splitch6.html#sec:annie:tagger";
		}
		
		public String getImagePath()
		{
			return "icons/gate.png";
		}
	},
	LingPipe_POS
	{
		public IGatePosTagger getPOSTagger(ILexicalWords verbFilter,ILexicalWords verbAddition){
			return new LingPipePosTagger(new Directionality(),new Polarity(),verbFilter,verbAddition);

		}
		
		public String toString(){
			return "Ling Pipe POS-Tagger";
		}
		
		public String getDescrition()
		{
			return LanguageProperties.getLanguageStream("pt.uminho.anote2.relation.postagging.lingpipe.info")+" http://gate.ac.uk/sale/tao/splitch21.html#sec:misc-creole:lingpipe:postagger	";
		}
		
		public String getImagePath()
		{
			return "icons/gate.png";
		}
	};
	
	public IGatePosTagger getPOSTagger(ILexicalWords verbFilter,ILexicalWords verbAddition) {
		return this.getPOSTagger(verbFilter,verbAddition);
	}
	
	public String getDescrition()
	{
		return this.getDescrition();
	}
	
	public String getImagePath()
	{
		return this.getImagePath();
	}
	
	public static GatePOSTaggerEnum convertStringInPosTaggerEnem(String str)
	{
		if(str.equals("Gate POS Tagging"))
		{
			return GatePOSTaggerEnum.Gate_POS;
		}
		else if(str.equals("Ling Pipe POS-Tagger"))
		{
			return GatePOSTaggerEnum.LingPipe_POS;
		}
		return null;
	}
	
}
