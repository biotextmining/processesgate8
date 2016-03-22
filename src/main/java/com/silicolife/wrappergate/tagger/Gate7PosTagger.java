package com.silicolife.wrappergate.tagger;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.creole.ExecutionException;
import gate.creole.POSTagger;
import gate.creole.ResourceInstantiationException;
import gate.creole.VPChunker;
import gate.creole.morph.Morph;
import gate.creole.splitter.RegexSentenceSplitter;
import gate.creole.splitter.SentenceSplitter;
import gate.creole.tokeniser.SimpleTokeniser;
import gate.util.GateException;
import gate.util.InvalidOffsetException;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.silicolife.textmining.core.datastructures.annotation.re.VerbInfoImpl;
import com.silicolife.textmining.core.datastructures.documents.structure.POSTokenImpl;
import com.silicolife.textmining.core.datastructures.documents.structure.SentenceImpl;
import com.silicolife.textmining.core.datastructures.process.re.SentenceSintaxRepresentationImpl;
import com.silicolife.textmining.core.datastructures.utils.GenericPairImpl;
import com.silicolife.textmining.core.datastructures.utils.conf.GlobalNames;
import com.silicolife.textmining.core.interfaces.core.annotation.re.DirectionallyEnum;
import com.silicolife.textmining.core.interfaces.core.annotation.re.IDirectionality;
import com.silicolife.textmining.core.interfaces.core.annotation.re.IPolarity;
import com.silicolife.textmining.core.interfaces.core.annotation.re.PolarityEnum;
import com.silicolife.textmining.core.interfaces.core.dataaccess.exception.ANoteException;
import com.silicolife.textmining.core.interfaces.core.document.structure.IPOSToken;
import com.silicolife.textmining.core.interfaces.core.document.structure.ISentence;
import com.silicolife.textmining.core.interfaces.core.document.structure.ISentenceSintaxRepresentation;
import com.silicolife.textmining.core.interfaces.core.utils.IGenericPair;
import com.silicolife.textmining.core.interfaces.process.IE.re.clue.IVerbInfo;
import com.silicolife.textmining.core.interfaces.process.IE.re.clue.VerbTenseEnum;
import com.silicolife.textmining.core.interfaces.process.IE.re.clue.VerbVoiceEnum;
import com.silicolife.textmining.core.interfaces.resource.lexicalwords.ILexicalWords;
import com.silicolife.wrappergate.GateInit;
import com.silicolife.wrappergate.IGatePosTagger;



/**
 * 
 * @author Hugo Costa
 *
 */
public class Gate7PosTagger implements IGatePosTagger{
	
	
	private Document gateDoc;
	private IPolarity polarityRules;
	private IDirectionality directionalyRules;
	private ILexicalWords verbFilter;
	private ILexicalWords verbAddition;
	private SimpleTokeniser tokeniser;
	private SentenceSplitter sentenceSplitter;
	private RegexSentenceSplitter regExpSentenceSplitter;
	private POSTagger tagger;
	private Morph morph;
	private VPChunker verbChunker;
	
	public Gate7PosTagger()
	{
		this.gateDoc = null;
		this.polarityRules=null;
		this.directionalyRules=null;
	}
	
	public Gate7PosTagger(IDirectionality rulesDir,IPolarity rulesPol,ILexicalWords verbFilter,ILexicalWords verbAddition)
	{
		this.gateDoc = null;
		this.directionalyRules= rulesDir;
		this.polarityRules = rulesPol;
		this.verbFilter=verbFilter;
		this.verbAddition=verbAddition;
	}

	public void posTaggingSteps(String text)
	{
		try {
			GateInit();
			this.createDocumentForStringContent(text);
			this.tokeniser();
			this.sentenceSplitRegExp();
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
	
	public void completePLSteps(File file) throws MalformedURLException, GateException
	{
		GateInit();
		this.gateDocument(file);
		this.tokeniser();
		this.sentenceSplitRegExp();
		this.posTagging();
		this.morphological();
		this.verbGroup();
	}
	
	public void sentenceSpliter(File file) throws MalformedURLException, GateException
	{
		GateInit();
		this.gateDocument(file);
		this.tokeniser();
		this.sentenceSplitRegExp();
	}
	
	public void afterPosTagging() throws MalformedURLException, GateException
	{
		GateInit();
		this.morphological();
		this.verbGroup();
	}
	
	private static void GateInit() throws GateException, MalformedURLException
	{
			GateInit.getInstance().init();
			GateInit.getInstance().creoleRegister("plugins/ANNIE");
			GateInit.getInstance().creoleRegister("plugins/Tools");
	}	
	
	public void createGateDocument(File file) throws MalformedURLException, GateException
	{
		GateInit();
		gateDocument(file);
	}
	
	/**
	 * Method that create a Document gate based in String sentences
	 * @throws GateException 
	 * @throws MalformedURLException 
	 */
	public void createDocumentForStringContent(String sentences) throws MalformedURLException, GateException
	{	
		GateInit();
		FeatureMap params = Factory.newFeatureMap();
		FeatureMap features = Factory.newFeatureMap();
		params.put("stringContent",sentences);
		//params.put("markupAware", true);	
		this.gateDoc = (Document) Factory.createResource("gate.corpora.DocumentImpl", params, features, "GATE Homepage");
	}
	
	/**
	 * Method that creates a Gate Document 
	 *
	 * @param file file that origin the gate Document
	 * @throws GateException 
	 * @throws MalformedURLException 
	 */
	protected void gateDocument(File file) throws MalformedURLException, GateException
	{	
		GateInit();
		if(this.gateDoc!=null)
			this.gateDoc.cleanup();
		FeatureMap params = Factory.newFeatureMap();
		params.put("sourceUrl",file.toURI().toString());	
		//params.put("encoding", "UTF-8");
		FeatureMap features = Factory.newFeatureMap();		
		this.gateDoc = (Document) Factory.createResource("gate.corpora.DocumentImpl", params, features, "GATE Homepage");
		this.gateDoc.setName(file.getName());
	}
	
	/**
	 * Method that create a tokeniser of gateDoc
	 * Need Create Document Gate first
	 * @throws GateException 
	 * @throws MalformedURLException 
	 * 
	 */
	protected void tokeniser() throws MalformedURLException, GateException
	{
		GateInit();
		if(tokeniser == null)
		{
			FeatureMap features = Factory.newFeatureMap();
			FeatureMap params = Factory.newFeatureMap();				
			features = Factory.newFeatureMap();	
			tokeniser = (SimpleTokeniser) Factory.createResource("gate.creole.tokeniser.SimpleTokeniser", params, features);
		}
		tokeniser.setParameterValue("document", this.gateDoc);
		tokeniser.execute();
	}
	
	/**
	 * Method that create a sentence Spliter
	 * Need Tokeniser first
	 * @throws GateException 
	 * @throws MalformedURLException 
	 */
	protected void sentenceSplitter() throws MalformedURLException, GateException 
	{
		GateInit();
		if(sentenceSplitter==null)
		{
			FeatureMap features = Factory.newFeatureMap();
			FeatureMap params = Factory.newFeatureMap();	
			//params.put("encoding", "UTF-8");
			params.put("gazetteerListsURL", "resources/sentenceSplitter/gazetteer/lists.def");
			params.put("transducerURL", "resources/sentenceSplitter/grammar/main.jape");			
			features = Factory.newFeatureMap();	
			sentenceSplitter = (SentenceSplitter) Factory.createResource("gate.creole.splitter.SentenceSplitter", params, features);
		}
		sentenceSplitter.setParameterValue("document", this.gateDoc);
		sentenceSplitter.execute();
	}

	/**
	 * Method that create a sentence Spliter based on RegExp
	 * Need Tokeniser first
	 * @throws GateException 
	 * @throws MalformedURLException 
	 */
	protected void sentenceSplitRegExp() throws MalformedURLException, GateException
	{
		GateInit();
		if(regExpSentenceSplitter==null)
		{
			FeatureMap features = Factory.newFeatureMap();
			FeatureMap params = Factory.newFeatureMap();
			regExpSentenceSplitter = (RegexSentenceSplitter) Factory.createResource("gate.creole.splitter.RegexSentenceSplitter", params, features);
		}
		regExpSentenceSplitter.setParameterValue("document", this.gateDoc);
		regExpSentenceSplitter.execute();
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
			//params.put("encoding", "UTF-8");
			params.put("lexiconURL","resources/heptag/lexicon");
			params.put("rulesURL","resources/heptag/ruleset");
			params.put("outputAnnotationType","Token");
			tagger = (POSTagger) Factory.createResource("gate.creole.POSTagger",params,features);		
			tagger.setParameterValue("baseTokenAnnotationType", "Token");
			tagger.setParameterValue("baseSentenceAnnotationType", "Sentence");
			tagger.setParameterValue("outputAnnotationType", "Token");	
		}
		tagger.setParameterValue("document", this.gateDoc);
		tagger.execute();		
	}
	/**
	 * Method that create a morphological analysis of words
	 * Used for find lemma verbs
	 * 
	 * Need : Tokeniser,Sentence Spliter and Pos-Tagging first
	 * @throws GateException 
	 * @throws MalformedURLException 
	 */
	protected void morphological() throws MalformedURLException, GateException
	{
		GateInit();
		if(morph == null)
		{
			FeatureMap features = Factory.newFeatureMap();
			FeatureMap params = Factory.newFeatureMap();	
			features = Factory.newFeatureMap();
			params = Factory.newFeatureMap();
			params.put("rulesFile","resources/morph/default.rul");
			params.put("caseSensitive","false");	
			morph =  (Morph) Factory.createResource("gate.creole.morph.Morph",params,features);
		}
		morph.setParameterValue("document", this.gateDoc);
		morph.execute();
	}

	/**
	 * Method that find in gate document the noun phases
	 * 
	 * Need : Tokeniser,Sentence Spliter and Pos-Tagging first
	 */
//	private void nounPhases()
//	{
//		Gate.getCreoleRegister().registerDirectories(new URL(Gate.getGateHome().toURI().toURL(),"plugins/NP_Chunking"));
//		FeatureMap features = Factory.newFeatureMap();
//		FeatureMap params = Factory.newFeatureMap();
//		features = Factory.newFeatureMap();	
//		params = Factory.newFeatureMap();
//		params.put("posTagURL","pos_tag_dict");
//		params.put("rulesURL","rules");	
//		GATEWrapper hunker = (GATEWrapper) Factory.createResource("mark.chunking.GATEWrapper",params,features);
//		chunker.setParameterValue("document", this.gateDoc);
//		chunker.execute();
//	}
	
	/**
	 * Method that find verbal expressions
	 * 
	 * Need : Tokeniser,Sentence Spliter and Pos Tagging first
	 * @throws GateException 
	 * @throws MalformedURLException 
	 */
	protected void verbGroup() throws MalformedURLException, GateException
	{
		GateInit();
		if(verbChunker==null)
		{
			FeatureMap features = Factory.newFeatureMap();
			FeatureMap params = Factory.newFeatureMap();	
			features = Factory.newFeatureMap();	
			params = Factory.newFeatureMap();	
			verbChunker = (VPChunker) Factory.createResource("gate.creole.VPChunker",params,features);	
		}
		verbChunker.setParameterValue("document", this.gateDoc);
		verbChunker.execute();
	}
	
	/**
	 * Method that clear all annotations of the document gate
	 */
	public void deleteGateDoc()
	{
		this.gateDoc.cleanup();		
	}
	
	public Document getGateDoc() {
		return gateDoc;
	}

	public List<IGenericPair<Long, Long>> getDocumentSentencelimits() {
		List<IGenericPair<Long,Long>> sentenceLimits = new ArrayList<IGenericPair<Long,Long>>();
		AnnotationSet annotSetSentences = gateDoc.getAnnotations().get("Sentence");
		for(Annotation annot:annotSetSentences)
		{
			sentenceLimits.add(new GenericPairImpl<Long, Long>(annot.getStartNode().getOffset(),annot.getEndNode().getOffset()));
		}
		return sentenceLimits;
	}


	public GenericPairImpl<List<IVerbInfo>, List<Long>> getSentenceSintaticLayer( Set<String> termionations, Set<String> filterVerbs, Set<String> additionVerbs, GenericPairImpl<Long, Long> setenceLimits,long documentStartOffset) throws ANoteException  {
		long endSentence = setenceLimits.getY();
		long startSentence = setenceLimits.getX();
		List<IVerbInfo>  verbsInfo = processSintaticSentence(startSentence,endSentence,documentStartOffset);
		if(verbAddition !=null && verbAddition.getLexicalWords().size()>0)
		{
			verbAddition(startSentence,endSentence,verbAddition.getLexicalWords().keySet(),verbsInfo,documentStartOffset);
		}
		List<Long> terminationsInfo = getTerminations(startSentence,endSentence,termionations);
		return new GenericPairImpl<List<IVerbInfo>, List<Long>>(verbsInfo,terminationsInfo);
	}

	
	private boolean alreadyAnnotated(long startAnnot, long endAnnot, List<IVerbInfo> verbsInfo) {
		for(IVerbInfo verb:verbsInfo)
		{
			Long start = verb.getStartOffset();
			Long end = verb.getEndOffset();
			// End in the middle of verb
			if(endAnnot >= start && endAnnot <= end)
			{
				return true;
			}
			// Start in the middle of verb
			if(startAnnot >= start && startAnnot<=end)
			{
				return true;
			}
			// Larger or equal entity
			if(start<=start && end >=endAnnot)
			{
				return true;
			}
		}
		return false;
	}

	private void findVerbs(List<IVerbInfo> verbsInfo,
			AnnotationSet allAnnottaions,
			ISentenceSintaxRepresentation sentenceSintax, Annotation annot,
			DirectionallyEnum directional, PolarityEnum polarity, long startAnnot, long endAnnot,
			String verb,long offsetCorrection) {
		IVerbInfo verbInfo;
		String lemma = getLemma(allAnnottaions,startAnnot,endAnnot);
		if(annot.getFeatures().get("neg")!=null)
			polarity=PolarityEnum.Negative;
		String voice = (String) annot.getFeatures().get("voice");
		String tense = (String) annot.getFeatures().get("tense");
		VerbVoiceEnum verbVoice = getVerbVoice(voice);
		VerbTenseEnum verbTense = getVerbTense(tense);
		IVerbInfo verbInfoAux = new VerbInfoImpl(startAnnot-offsetCorrection,endAnnot-offsetCorrection,verb,lemma,polarity,DirectionallyEnum.Unknown,verbVoice,verbTense);
		if(directionalyRules!=null)
			directional = directionalyRules.getDirectionality(verbInfoAux, sentenceSintax);
		if(polarityRules!=null)
			polarity = polarityRules.getPolarity(verbInfoAux);
		verbInfo = new VerbInfoImpl(startAnnot-offsetCorrection,endAnnot-offsetCorrection,verb,lemma.toLowerCase(),polarity,directional,verbVoice,verbTense);	
		verbsInfo.add(verbInfo);
	}
	
	private ISentenceSintaxRepresentation getSentenceSintax(Long startOffsetSentence, Long endOffsetSentence, AnnotationSet annotationSet,long documentStartOffset) 
	{
		String text = getPartGateDocument(getGateDoc(),startOffsetSentence,endOffsetSentence);
		ISentence sentence = new SentenceImpl(startOffsetSentence, endOffsetSentence, text);
		ISentenceSintaxRepresentation sentenceSintax = new SentenceSintaxRepresentationImpl(sentence);
		Iterator<Annotation> itAnnot = annotationSet.iterator();
		while(itAnnot.hasNext())
		{
			Annotation annot = itAnnot.next();
			String word = (String) annot.getFeatures().get("string");
			String cat = (String) annot.getFeatures().get("category");
			String lemma = (String) annot.getFeatures().get("root");		
			long startAnnotOffset = annot.getStartNode().getOffset();
			long start = startAnnotOffset-documentStartOffset;		
			sentenceSintax.addElement(new POSTokenImpl("", start, start + word.length(), word, cat,lemma));
			
		}
		return sentenceSintax;
	}
	
	/**
	 * Static Method
	 * Method that return a piece of Text.
	 * 
	 * 
	 * @param gateDoc Gate Document
	 * @param startPosition 
	 * @param endPosition
	 * @return
	 */
	public static String getPartGateDocument(Document gateDoc,Long startPosition,Long endPosition)
	{
		String allText = gateDoc.getContent().toString();
		return allText.substring(startPosition.intValue(),endPosition.intValue());
	}
	
	/**
	 * Method that return lemma of offset words under the star and end limits
	 * 
	 * @param allAnnottaions
	 * @param start
	 * @param end
	 * @return
	 */
	private static String getLemma(AnnotationSet allAnnottaions,long start,long end)
	{
		String lemma = new String();
		AnnotationSet annotSet = allAnnottaions.getContained(start, end);
		annotSet = annotSet.get("Token");
		TreeMap<Long,String> tokensOrder = new TreeMap<Long, String>();
		Iterator<Annotation> itAnnot = annotSet.iterator();
		while(itAnnot.hasNext())
		{
			Annotation annot = itAnnot.next();
			tokensOrder.put(annot.getStartNode().getOffset(),(String)annot.getFeatures().get("root"));
		}	
		Iterator<String> itTokens = tokensOrder.values().iterator();
		while(itTokens.hasNext())
			lemma=lemma.concat(itTokens.next()+" ");
		lemma = lemma.substring(0,lemma.length()-1);
		return lemma;
	}
	/**
	 * Method that return the tense of verb
	 * 
	 * @param tense
	 * @return
	 */
	private static VerbTenseEnum getVerbTense(String tense) 
	{
		if(tense.equals("SimPre"))
		{
			return VerbTenseEnum.SIMPLE_PRESENT;
		}
		else if(tense.equals("PreCon"))
		{
			return VerbTenseEnum.PRESENT_CONTINUOS;
		}
		else if(tense.equals("PrePer"))
		{
			return VerbTenseEnum.PRESENT_PERFECT;
		}
		else if(tense.equals("PrePerCon"))
		{
			return VerbTenseEnum.PRESENT_PERFECT_CONTINOUS;
		}
		else if(tense.equals("SinPas")||tense.equals("Pas"))
		{
			return VerbTenseEnum.SIMPLE_PAST;
		}
		else if(tense.equals("PasCon"))
		{
			return VerbTenseEnum.PAST_CONTINOUS;
		}
		else if(tense.equals("PasPer"))
		{
			return VerbTenseEnum.PAST_PERFECT;
		}
		else if(tense.equals("Inf"))
		{
			return VerbTenseEnum.INFINITIVE;
		}
		else if(tense.equals("Pre"))
		{
			return VerbTenseEnum.PRESENT_CONTINUOS;
		}
		else
		{
			return VerbTenseEnum.NONE;
		}
	}

	/**
	 * Method that return the voice verb
	 * 
	 * @param voice
	 * @return
	 */
	private static VerbVoiceEnum getVerbVoice(String voice) 
	{
		
		if(voice.equals("active"))
		{
			return VerbVoiceEnum.ACTIVE;
		}
		else if(voice.equals("passive"))
		{
			return VerbVoiceEnum.PASSIVE;
		}
		else
			return VerbVoiceEnum.NONE;
	}

	/**
	 * Method that find in sentence a possible relation terminatons
	 * 
	 * @param startOffsetSentence
	 * @param endOffsetSentence
	 * @param relationTerminations
	 * @param gateDocument
	 * @return
	 */
	public List<Long> getTerminations(long startOffsetSentence,long endOffsetSentence,Set<String> relationTerminations)
	{
		List<Long> offsetTerminations = new ArrayList<Long>();
		
		AnnotationSet allAnnottaions = getGateDoc().getAnnotations();
		allAnnottaions = allAnnottaions.getContained(startOffsetSentence, endOffsetSentence);
		AnnotationSet tokenAnnotations = allAnnottaions.get("Token");
		Iterator<Annotation> itAnnot = tokenAnnotations.iterator();
		while(itAnnot.hasNext())
		{
			Annotation annot = itAnnot.next();
			String word = (String) annot.getFeatures().get("string");
			if(relationTerminations.contains(word))
			{
				long start = annot.getStartNode().getOffset();
				offsetTerminations.add(start);
			}
		}
		return offsetTerminations;
	}
	
	public SortedMap<Long, IVerbInfo> getVerbsPosition(List<IVerbInfo> verbsInfo)
	{
		TreeMap<Long,IVerbInfo> verbsInfoReturn = new TreeMap<Long, IVerbInfo>();
		for(int i=0;i<verbsInfo.size();i++)
		{
			IVerbInfo verbInfo = verbsInfo.get(i);
			verbsInfoReturn.put(verbInfo.getStartOffset(),verbInfo);
		}	
		return verbsInfoReturn;
	}

	public ISentenceSintaxRepresentation getSentenceSintaticLayer(
			Set<String> termionations, IGenericPair<Long, Long> setenceLimits,
			Long offsetCorrection) throws ANoteException {
		Long endSentence = setenceLimits.getY();
		Long startSentence = setenceLimits.getX();
		List<IVerbInfo>  verbsInfo = processSintaticSentence(startSentence,endSentence,offsetCorrection);
		if(verbAddition !=null && verbAddition.getLexicalWords().size()>0)
		{
			verbAddition(startSentence,endSentence,verbAddition.getLexicalWords().keySet(),verbsInfo,offsetCorrection);
		}
		List<Long> terminationsInfo = getTerminations(startSentence,endSentence,termionations,offsetCorrection);
		String text = getPartGateDocument(getGateDoc(),startSentence,endSentence);
		ISentence sentence = new SentenceImpl(startSentence-offsetCorrection, endSentence-offsetCorrection, text);
		return new SentenceSintaxRepresentationImpl(sentence,verbsInfo,terminationsInfo);
	}

	private void verbAddition(Long startOffsetSentence, Long endOffsetSentence,Set<String> lexicalWords, List<IVerbInfo> verbsInfo, long documetStartOffset) throws ANoteException {
		AnnotationSet allAnnottaions = getGateDoc().getAnnotations();
		allAnnottaions = allAnnottaions.getContained(startOffsetSentence, endOffsetSentence);
		ISentenceSintaxRepresentation sentenceSintax= getSentenceSintax(startOffsetSentence,endOffsetSentence,allAnnottaions.get("Token"),documetStartOffset);
		Iterator<Annotation> itAnnot = allAnnottaions.iterator();
		IVerbInfo verbInfo;
		while(itAnnot.hasNext())
		{
			Annotation annot = itAnnot.next();
			DirectionallyEnum directional = DirectionallyEnum.LeftToRight;
			PolarityEnum polarity = PolarityEnum.Positive;
			long startAnnot = annot.getStartNode().getOffset();
			long endAnnot = annot.getEndNode().getOffset();
			String token = getPartGateDocument(getGateDoc(), startAnnot, endAnnot);
			if(verbAddition.getLexicalWords().keySet().contains(token))
			{
				if(!alreadyAnnotated(startAnnot-documetStartOffset,endAnnot-documetStartOffset,verbsInfo))
				{
					String lemma = getLemma(allAnnottaions,startAnnot,endAnnot);
					if(annot.getFeatures().get("neg")!=null)
						polarity=PolarityEnum.Negative;
					IVerbInfo verbInfoAux = new VerbInfoImpl(startAnnot-documetStartOffset,endAnnot-documetStartOffset,token,lemma,polarity,directional);
					if(directionalyRules!=null)
						directional = directionalyRules.getDirectionality(verbInfoAux, sentenceSintax);
					if(polarityRules!=null)
						polarity = polarityRules.getPolarity(verbInfoAux);
					verbInfo = new VerbInfoImpl(startAnnot-documetStartOffset,endAnnot-documetStartOffset,token,lemma.toLowerCase(),polarity,directional,VerbVoiceEnum.NONE,VerbTenseEnum.NONE);	
					verbsInfo.add(verbInfo);
				}
			}
		}
	}	

	
	private List<IVerbInfo> processSintaticSentence(long startOffsetSentence,long endOffsetSentence,long documetStartOffset) throws ANoteException 
	{
		List<IVerbInfo> verbsInfo = new ArrayList<IVerbInfo>();
		AnnotationSet allAnnottaions = getGateDoc().getAnnotations();
		allAnnottaions = allAnnottaions.getContained(startOffsetSentence, endOffsetSentence);
		ISentenceSintaxRepresentation sentenceSintax= getSentenceSintax(startOffsetSentence,endOffsetSentence,allAnnottaions.get("Token"),documetStartOffset);
		AnnotationSet vpAnnotations = allAnnottaions.get("VG");
		Iterator<Annotation> itAnnot = vpAnnotations.iterator();
		while(itAnnot.hasNext())
		{
			Annotation annot = itAnnot.next();
			DirectionallyEnum directional = DirectionallyEnum.LeftToRight;
			PolarityEnum polarity = PolarityEnum.Positive;
			long startAnnot = annot.getStartNode().getOffset();
			long endAnnot = annot.getEndNode().getOffset();
			String verb = getPartGateDocument(getGateDoc(), startAnnot, endAnnot);
			if(verbFilter == null)
			{
				findVerbs(verbsInfo, allAnnottaions, sentenceSintax, annot,
						directional, polarity, startAnnot, endAnnot, verb,documetStartOffset);
			}
			else if(!verbFilter.getLexicalWords().keySet().contains(verb))
			{
				findVerbs(verbsInfo, allAnnottaions, sentenceSintax, annot,
						directional, polarity, startAnnot, endAnnot, verb,documetStartOffset);
			}
		}	
		return verbsInfo;
	}
	
	public List<IVerbInfo> getVerbChunkers() throws ANoteException
	{
		List<IVerbInfo> verbsInfo = new ArrayList<IVerbInfo>();
		AnnotationSet allAnnottaions = getGateDoc().getAnnotations();
		long start = 0;
		ISentenceSintaxRepresentation sentenceSintax= getSentenceSintax(start,start,allAnnottaions.get("Token"),0);
		AnnotationSet vpAnnotations = allAnnottaions.get("VG");
		Iterator<Annotation> itAnnot = vpAnnotations.iterator();
		while(itAnnot.hasNext())
		{
			Annotation annot = itAnnot.next();
			DirectionallyEnum directional = DirectionallyEnum.LeftToRight;
			PolarityEnum polarity = PolarityEnum.Positive;
			long startAnnot = annot.getStartNode().getOffset();
			long endAnnot = annot.getEndNode().getOffset();
			String verb = getPartGateDocument(getGateDoc(), startAnnot, endAnnot);
			if(verbFilter == null)
			{
				findVerbs(verbsInfo, allAnnottaions, sentenceSintax, annot,
						directional, polarity, startAnnot, endAnnot, verb,0);
			}
			else if(!verbFilter.getLexicalWords().keySet().contains(verb))
			{
				findVerbs(verbsInfo, allAnnottaions, sentenceSintax, annot,
						directional, polarity, startAnnot, endAnnot, verb,0);
			}
		}	
		return verbsInfo;
	}
	
	private List<Long> getTerminations(long startOffsetSentence,long endOffsetSentence,Set<String> relationTerminations, long offsetCorrection)
	{
		List<Long> offsetTerminations = new ArrayList<Long>();
		
		AnnotationSet allAnnottaions = getGateDoc().getAnnotations();
		allAnnottaions = allAnnottaions.getContained(startOffsetSentence, endOffsetSentence);
		AnnotationSet tokenAnnotations = allAnnottaions.get("Token");
		Iterator<Annotation> itAnnot = tokenAnnotations.iterator();
		while(itAnnot.hasNext())
		{
			Annotation annot = itAnnot.next();
			String word = (String) annot.getFeatures().get("string");
			if(relationTerminations.contains(word))
			{
				long start = annot.getStartNode().getOffset();
				offsetTerminations.add(start-offsetCorrection);
			}
		}
		return offsetTerminations;
	}

	public void setGateDoc(Document doc) {
		this.gateDoc=doc;
		
	}

	
	public void cleanALL(){
		if(this.gateDoc!=null){ 
			Factory.deleteResource(gateDoc);
//			gateDoc.cleanup();
//			gateDoc.getAnnotations().clear();
//			try {
//				if(gateDoc.getDataStore()!= null)
//					gateDoc.getDataStore().close();
//			} catch (PersistenceException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			if(gateDoc.getFeatures()!=null)
//				gateDoc.getFeatures().clear();
//			gateDoc.setContent(null);
//			gateDoc=null;
		}
		if(this.tagger!=null){
			Factory.deleteResource(tagger);
//			tagger.cleanup();
//			try {
//				tagger.execute();
//			} catch (ExecutionException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			tagger=null;
		}
		if(this.tokeniser!=null){
			Factory.deleteResource(tokeniser);
//			tokeniser.cleanup();
//			try {
//				tokeniser.execute();
//			} catch (ExecutionException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			tokeniser =null;
		}
		if(this.sentenceSplitter != null){
			Factory.deleteResource(sentenceSplitter);

//			sentenceSplitter.cleanup();
//			try {
//				sentenceSplitter.execute();
//			} catch (ExecutionException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			sentenceSplitter = null;
		}
		if(this.morph != null){
			Factory.deleteResource(morph);
//			morph.cleanup();
//			try {
//				morph.execute();
//			} catch (ExecutionException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			morph = null;
		}
		if(this.regExpSentenceSplitter!=null){
//			regExpSentenceSplitter.cleanup();
			Factory.deleteResource(regExpSentenceSplitter);

//			try {
////				regExpSentenceSplitter.execute();
//			} catch (ExecutionException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			regExpSentenceSplitter = null;
		}
		if(this.verbChunker!=null){
			Factory.deleteResource(verbChunker);
//			verbChunker.cleanup();
//			try {
//				verbChunker.execute();
//			} catch (ExecutionException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			verbChunker = null;
		}
		
	}

	public List<ISentence> getSentences() {
		List<ISentence> sentenceresult = new ArrayList<ISentence>();
		List<IGenericPair<Long, Long>> sentenceDelimiter = getDocumentSentencelimits();
		AnnotationSet allAnnottaions = getGateDoc().getAnnotations();
		for(IGenericPair<Long, Long> sentencesDelimite : sentenceDelimiter)
		{		
			allAnnottaions = allAnnottaions.getContained(sentencesDelimite.getX(), sentencesDelimite.getY());
			ISentenceSintaxRepresentation sentenceSintax= getSentenceSintax(sentencesDelimite.getX(),sentencesDelimite.getY(),allAnnottaions.get("Token"),sentencesDelimite.getY());	
			List<IPOSToken> orderPosTokens = new ArrayList<IPOSToken>();
			Set<Long> keys = sentenceSintax.getSentenceSintax().keySet();
			for(Long keyOffset : keys)
			{
				IPOSToken res = sentenceSintax.getSentenceSintax().get(keyOffset);
				orderPosTokens.add(res);
			}
			String sentenceText = new String();
			try {
				sentenceText = getGateDoc().getContent().getContent(sentencesDelimite.getX(), sentencesDelimite.getY()).toString();
			} catch (InvalidOffsetException e) {
				e.printStackTrace();
			}
			ISentence sentence = new SentenceImpl(sentencesDelimite.getX(), sentencesDelimite.getY(), sentenceText, orderPosTokens );
			sentenceresult.add(sentence);
		}
		return sentenceresult;
	}	
	
	public String toString(){
		return "Gate POS Tagging";
	}

	public Properties getProperties() {
		Properties prop = new Properties();
		if(getVerbFilter()!=null && getVerbFilter().getId() > 0)
		{
			prop.put(GlobalNames.verbFilter,String.valueOf(verbFilter.getId()));
		}
		if(getVerbAddition()!=null  && getVerbAddition().getId() > 0)
		{
			prop.put(GlobalNames.verbAddition,String.valueOf(verbAddition.getId()));
		}
		return prop;
	}

	public ILexicalWords getVerbFilter() {
		return verbFilter;
	}

	public ILexicalWords getVerbAddition() {
		return verbAddition;
	}

	public String getUID() {
		return "Gate POS Tagging";
	}
}
