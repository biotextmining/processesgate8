package com.silicolife.textmining.ie.re.relation.models;

import com.silicolife.textmining.core.interfaces.process.IE.re.IRelationModel;
import com.silicolife.textmining.ie.re.relation.configuration.IRERelationConfiguration;

public enum RelationsModelEnem {
	Binary_Biomedical_Verbs{
		public IRelationModel getRelationModel(IRERelationConfiguration configuration) {
			return 	new RelationModelBinaryBiomedicalVerbs(configuration);
		}

		public String getDescription(){
			return "Binary Selected Verbs Only";
		}
		
		public String getImagePath() {
			return "icons/relation_model_binary_verb_select_user.png";
		}

		public String toString(){
			return "Binary Selected Verbs Only (1 x 1)*";
		}
	}
	,
	Binary_Verb_limitation{
		public IRelationModel getRelationModel(IRERelationConfiguration configuration) {
			return 	new RelationModelBinaryVerbLimitation(configuration);
		}

		public String getDescription(){
			return "Binary Verb Limitation";
		}
		
		public String getImagePath() {
			return "icons/relation_model_binary_verb_limitation.png";
		}

		public String toString(){
			return "Binary Verb Limitation (1 x 1)";
		}
	}
	,
	Verb_Limitation{
		public IRelationModel getRelationModel(IRERelationConfiguration configuration) {
			return 	new RelationModelVerbLimitation(configuration);
		}

		public String getDescription(){
			return "Verb Limitation";
		}
		
		public String getImagePath() {
			return "icons/relation_model_verb_limitation.png";
		}

		public String toString(){
			return "Verb Limitation (M x M)";
		}
	},
	
	Simple_Model{
		public IRelationModel getRelationModel(IRERelationConfiguration configuration) {
			return new RelationModelSimple(configuration);
		}

		public String getDescription() {
			return "Simple Model";
		}
		
		public String getImagePath() {
			return "icons/relation_model_simple.png";
		}

		public String toString(){
			return "Simple Model (M x M )";
		}		
	};



	public IRelationModel getRelationModel(IRERelationConfiguration configuration){
		return this.getRelationModel(configuration);
	}
	
	public String getDescription() {
		return this.getDescription();
	}
	
	public String getImagePath() {
		return this.getImagePath();
	}

	public static RelationsModelEnem convertStringToRelationsModelEnem(String str)
	{	
		if(str.equals("Binary Selected Verbs Only (1 x 1)*"))
		{
			return RelationsModelEnem.Binary_Biomedical_Verbs;
		}
		else if(str.equals("Binary Verb Limitation (1 x 1)"))
		{
			return RelationsModelEnem.Binary_Verb_limitation;
		}
		else if(str.equals("Verb Limitation (M x M)"))
		{
			return RelationsModelEnem.Verb_Limitation;

		}
		else if(str.equals("Simple Model (M x M )"))
		{
			return RelationsModelEnem.Simple_Model;

		}
		return null;
	}
	
}
