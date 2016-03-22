package com.silicolife.wrappergate;


import gate.Document;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.util.GateException;

import java.io.File;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Properties;

import com.silicolife.textmining.core.interfaces.core.document.structure.ISentence;
import com.silicolife.textmining.core.interfaces.process.IE.IPOSTagger;
import com.silicolife.textmining.core.interfaces.resource.lexicalwords.ILexicalWords;


public interface IGatePosTagger extends IPOSTagger{
	
//	public void completePLSteps(String text) throws MalformedURLException, GateException;
	public void completePLSteps(File file) throws MalformedURLException, GateException;
	public void sentenceSpliter(File file) throws ResourceInstantiationException, ExecutionException, MalformedURLException, GateException;
	public void afterPosTagging() throws MalformedURLException, GateException;
	public void createGateDocument(File file) throws ResourceInstantiationException, MalformedURLException, GateException;
	public Document getGateDoc();
	public void setGateDoc(Document doc);
	public void cleanALL();
	public List<ISentence> getSentences();
	
	public Properties getProperties();
	public ILexicalWords getVerbFilter();
	public ILexicalWords getVerbAddition();
	public String getUID();
		
}
