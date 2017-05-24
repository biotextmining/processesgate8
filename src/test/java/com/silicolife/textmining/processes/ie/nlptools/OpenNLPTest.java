package com.silicolife.textmining.processes.ie.nlptools;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import com.silicolife.textmining.core.datastructures.exceptions.SintaticTreeViewException;
import com.silicolife.textmining.core.interfaces.core.document.structure.ISentence;
import com.silicolife.textmining.processes.nlptools.opennlp.OpenNLP;

public class OpenNLPTest {

	@Test
	public void test() throws IOException, SintaticTreeViewException {
		String text = "arboxylate their substrate amino acids in a proton-dependent manner thus raising the internal pH. The decarboxylases include the glutamic acid decarboxylases GadA and GadB, the arginine decarboxylase AdiA, the lysine decarboxylase LdcI and the ornithine decarboxylase SpeF. All of these enzymes utilize pyridoxal-5'-phosphate as a co-factor and function together with inner-membrane substrate-product antiporters that remove decarboxylation products to the external medium in exchange for fresh substrate. In the case of LdcI, the lysine-cadaverine antiporter is called CadB. Recently, we determined the X-ray crystal structure of LdcI to 2.0 Å, and we discovered a novel small-molecule bound to LdcI the stringent response regulator guanosine 5'-diphosphate,3'-diphosphate (ppGpp). The stringent response occurs when exponentially growing cells experience nutrient deprivation or one of a number of other stresses. As a result, cells produce ppGpp which leads to a signaling cascade culminating in the shift from exponential growth to stationary phase growth. We have demonstrated that ppGpp is a specific inhibitor of LdcI. Here we describe the lysine decarboxylase assay, modified from the assay developed by Phan et al., that we have used to determine the activity of LdcI and the effect of pppGpp/ppGpp on that activity. The LdcI decarboxylation reaction removes the α-carboxy group of L-lysine and produces carbon dioxide and the polyamine cadaverine (1,5-diaminopentane). L-lysine and cadaverine can be reacted with 2,4,6-trinitrobenzensulfonic acid (TNBS) at high pH to generate N,N'-bistrinitrophenylcadaverine (TNP-cadaverine) and N,N'-bistrinitrophenyllysine (TNP-lysine), respectively. The TNP-cadaverine can be separated from the TNP-lysine as the former is soluble in organic solvents such as toluene while the latter is not. The linear range of the assay was determined empirically using purified cadaverine.";

		List<ISentence> sentences = OpenNLP.getInstance().getSentencesText(text);
		System.out.println(sentences);
		System.err.println(OpenNLP.getInstance().getSetencesWhitPOSTagging(text));
		System.out.println(OpenNLP.getInstance().getSentenceOrderChunkers(sentences));
		System.err.println(OpenNLP.getInstance().getSentenceParserResults(sentences));
		
		//		String text1 = "Expression of Arabidopsis glycine - rich RNA - binding protein ENT100 or ENT200 improves grain yield of rice ( Oryza sativa ) under ENT00000000000 conditions .";
//		String text2 = "To enable such a broad range of acidic pH survival, E. coli possesses four different inducible amino acid decarboxylases that decarboxylate"
//				+ " their substrate amino acids in a proton-dependent manner thus raising the internal pH.";
//		String text3 = "The decarboxylases include the glutamic acid decarboxylases GadA and GadB, the arginine decarboxylase AdiA, the lysine decarboxylase LdcI and the ornithine decarboxylase SpeF.";
//		String text4 = "All of these enzymes utilize pyridoxal-5'-phosphate as a co-factor and function together with inner-membrane substrate-product antiporters that remove decarboxylation products to the external medium in exchange for fresh substrate.";
//		String test5 = "In the case of LdcI, the lysine-cadaverine antiporter is called CadB.";
//		String test6 = "Recently, we determined the X-ray crystal structure of LdcI to 2.0 Å, and we discovered a novel small-molecule bound to LdcI the stringent response regulator guanosine 5'-diphosphate,3'-diphosphate (ppGpp).";
//		String test7 = "The stringent response occurs when exponentially growing cells experience nutrient deprivation or one of a number of other stresses.";
//		String test8 = "As a result, cells produce ppGpp which leads to a signaling cascade culminating in the shift from exponential growth to stationary phase growth.";
//		String test9 = "We have demonstrated that ppGpp is a specific inhibitor of LdcI.";
//		String test10 = "Here we describe the lysine decarboxylase assay, modified from the assay developed by Phan et al., that we have used to determine the activity of LdcI and the effect of pppGpp/ppGpp on that activity.";
//		String test11 = "The LdcI decarboxylation reaction removes the α-carboxy group of L-lysine and produces carbon dioxide and the polyamine cadaverine (1,5-diaminopentane).";
//		String test12 = "L-lysine and cadaverine can be reacted with 2,4,6-trinitrobenzensulfonic acid (TNBS) at high pH to generate N,N'-bistrinitrophenylcadaverine (TNP-cadaverine) and N,N'-bistrinitrophenyllysine (TNP-lysine), respectively.";
//		String test12b = "ENT1 and cadaverine can be reacted with ENT2 (TNBS) at high pH to generate ENT3 (TNP-cadaverine) and ENT4 (TNP-lysine), respectively.";
//		String test13 = "The TNP-cadaverine can be separated from the TNP-lysine as the former is soluble in organic solvents such as toluene while the latter is not.";
//		String test14 = "The linear range of the assay was determined empirically using purified cadaverine.";
//		String test12simp = "L-lysine and cadaverine can be reacted with 2,4,6-trinitrobenzensulfonic acid (TNBS).";
//
//		ISentence sentence = new SentenceImpl(0, text1.length()-1, text1);
//		sentence.setParsingTokens(nlp.parsingSentence(sentence));
//		SyntaxTreeViewerGUI gui = new SyntaxTreeViewerGUI(sentence);
//		JDialog jDialog = new JDialog();
//		jDialog.setLayout(new BorderLayout());
//		JScrollPane pane = new JScrollPane();
//		pane.setViewportView(gui);
//		jDialog.add(pane);
//		jDialog.setSize(new Dimension(1000, 800));
//		jDialog.setModal(true);
//		jDialog.setVisible(true);
		//		sentence = new Sentence(0, text2.length()-1, text2);
		//		nlp.parsingSentence(sentence);
		//		sentence = new Sentence(0, text3.length()-1, text3);
		//		nlp.parsingSentence(sentence);
		//		sentence = new Sentence(0, text4.length()-1, text4);
		//		nlp.parsingSentence(sentence);
		//		sentence = new Sentence(0, test5.length()-1, test5);
		//		nlp.parsingSentence(sentence);
		//		sentence = new Sentence(0, test6.length()-1, test6);
		//		nlp.parsingSentence(sentence);
		//		sentence = new Sentence(0, test7.length()-1, test7);
		//		nlp.parsingSentence(sentence);
		//		sentence = new Sentence(0, test8.length()-1, test8);
		//		nlp.parsingSentence(sentence);
//		sentence = new Sentence(0, test12simp.length()-1, test12simp);	
//		sentence.setParsingTokens(OpenNLP.getInstance().parsingSentence(sentence));
//		SyntaxTreeViewerPane gui = new SyntaxTreeViewerPane(sentence);
//		JDialog jDialog = new JDialog();
//		jDialog.setLayout(new BorderLayout());
//		JScrollPane pane = new JScrollPane();
//		pane.setViewportView(gui);
//		jDialog.add(pane);
//		jDialog.setSize(new Dimension(1000, 800));
//		jDialog.setModal(true);
//		jDialog.setVisible(true);
		//		sentence = new Sentence(0, test10.length()-1, test10);
		//		nlp.parsingSentence(sentence);
		//		sentence = new Sentence(0, test11.length()-1, test11);
		//		nlp.parsingSentence(sentence);
		//		sentence = new Sentence(0, test12b.length()-1, test12b);
		//		nlp.parsingSentence(sentence);
		//		sentence = new Sentence(0, test13.length()-1, test13);
		//		nlp.parsingSentence(sentence);
		//		sentence = new Sentence(0, test14.length()-1, test14);
		//		nlp.parsingSentence(sentence);
		//		Set<String> posTags = new HashSet<String>();
		//		for(PartOfSpeechLabels label:PartOfSpeechLabels.values())
		//		{
		//			if(label.getEnableDefaultValue())
		//			{
		//				posTags.add(label.value());
		//			}
		//		}
		//		nlp.geTextSegmentsFilterByPOSTags(TermSeparator.termSeparator(text),posTags);
	}

}
