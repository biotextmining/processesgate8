package com.silicolife.wrappergate.tagger;

import gate.Factory;
import gate.FeatureMap;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.lingpipe.POSTaggerPR;
import gate.lingpipe.SentenceSplitterPR;
import gate.util.GateException;

import java.io.File;
import java.net.MalformedURLException;

import com.silicolife.textmining.core.interfaces.core.annotation.re.IDirectionality;
import com.silicolife.textmining.core.interfaces.core.annotation.re.IPolarity;
import com.silicolife.textmining.core.interfaces.resource.lexicalwords.ILexicalWords;
import com.silicolife.wrappergate.GateInit;

public class LingPipePosTagger extends Gate7PosTagger{
	
	private POSTaggerPR tagger;
	private SentenceSplitterPR regExpSentenceSplitter;

	public LingPipePosTagger() {
		super();
	}
	
	public LingPipePosTagger(IDirectionality rulesDir,IPolarity rulesPol,ILexicalWords verbFilter,ILexicalWords verbAddition)
	{
		super(rulesDir,rulesPol,verbFilter,verbAddition);
	}
	
	public void completePLSteps(File file) throws MalformedURLException, GateException
	{
		
		GateInit.getInstance().creoleRegister("plugins/LingPipe");
		super.gateDocument(file);
		super.tokeniser();
		this.sentenceSpliter();
		this.posTagging();
		super.morphological();
		super.verbGroup();
	}
	
	public void posTaggingSteps(String text)
	{
		try {
			GateInit.getInstance().creoleRegister("plugins/LingPipe");
			super.createDocumentForStringContent(text);
			super.tokeniser();
			this.sentenceSpliter();
			this.posTagging();
		} catch (ResourceInstantiationException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (GateException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Method that create a Pos-Tagging
	 * Need Tokeniser and Sentence Spliter first
	 * @throws ResourceInstantiationException 
	 * @throws ExecutionException 
	 */
	private void posTagging() throws ResourceInstantiationException, ExecutionException
	{
		if(tagger == null)
		{
			FeatureMap features = Factory.newFeatureMap();
			FeatureMap params = Factory.newFeatureMap();
			params = Factory.newFeatureMap();
			String url = "resources/models/posenbiogeniaHiddenMarkovModel";
			params.put("modelFileUrl", url);
			tagger = (POSTaggerPR) Factory.createResource("gate.lingpipe.POSTaggerPR",params,features);	
		}
		tagger.setParameterValue("document", getGateDoc());	
		tagger.execute();		
	}

	
	/**
	 * Method that create a sentence Spliter based on RegExp
	 * Need Tokeniser first
	 * @throws ExecutionException 
	 * @throws ResourceInstantiationException 
	 */
	private void sentenceSpliter() throws ExecutionException, ResourceInstantiationException
	{
		if(regExpSentenceSplitter == null)
		{
			FeatureMap features = Factory.newFeatureMap();
			FeatureMap params = Factory.newFeatureMap();
			regExpSentenceSplitter = (SentenceSplitterPR) Factory.createResource("gate.lingpipe.SentenceSplitterPR", params, features);
		}
		regExpSentenceSplitter.setParameterValue("document", this.getGateDoc());
		regExpSentenceSplitter.execute();
	}
	
	
	public void cleanALL(){
		super.cleanALL();
		if(this.tagger!=null){
			Factory.deleteResource(tagger);
//			tagger.cleanup();
//			tagger=null;
		}
		if(this.regExpSentenceSplitter != null){
			Factory.deleteResource(regExpSentenceSplitter);
//			regExpSentenceSplitter.cleanup();
//			regExpSentenceSplitter = null;
			
		}
	}
	
	public String toString(){
		return "Ling Pipe POS-Tagger";
	}
	
	public String getUID() {
		return "Ling Pipe POS-Tagger";
	}

}
