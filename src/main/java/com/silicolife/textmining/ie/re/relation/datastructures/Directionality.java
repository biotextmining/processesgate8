package com.silicolife.textmining.ie.re.relation.datastructures;

import com.silicolife.textmining.core.interfaces.core.annotation.re.DirectionallyEnum;
import com.silicolife.textmining.core.interfaces.core.annotation.re.IDirectionality;
import com.silicolife.textmining.core.interfaces.core.document.structure.IPOSToken;
import com.silicolife.textmining.core.interfaces.process.IE.re.clue.IVerbInfo;
import com.silicolife.textmining.core.interfaces.process.IE.re.clue.VerbVoiceEnum;

public class Directionality implements IDirectionality{

	public Directionality()
	{
		
	}
	

	public DirectionallyEnum getDirectionality(IVerbInfo verbInfo, com.silicolife.textmining.core.interfaces.core.document.structure.ISentenceSintaxRepresentation sentenceSintax) {
	
		return byRule(verbInfo, sentenceSintax);
	}


	private DirectionallyEnum byRule(IVerbInfo verbInfo,com.silicolife.textmining.core.interfaces.core.document.structure.ISentenceSintaxRepresentation sentenceSintax) {
		Long endVerbPos = verbInfo.getEndOffset();
		IPOSToken nextPos = sentenceSintax.getNextElement(endVerbPos);
		if(verbInfo.getVoice()!=null && verbInfo.getVoice().equals(VerbVoiceEnum.PASSIVE))
		{
			return DirectionallyEnum.RightToLeft;
		}
		else if(verbInfo.getVoice()!=null && verbInfo.getVoice().equals(VerbVoiceEnum.ACTIVE))
		{
			return DirectionallyEnum.LeftToRight;
		}
		else if(verbInfo.getVoice()!=null && verbInfo.getVoice().equals(VerbVoiceEnum.NONE))
		{
			return DirectionallyEnum.Both;
		}
		else if(nextPos!=null && nextPos.getText().equals("by"))
			return DirectionallyEnum.RightToLeft;
		else
			return DirectionallyEnum.LeftToRight;
	}

}
