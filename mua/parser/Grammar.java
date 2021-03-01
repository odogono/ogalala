package com.ogalala.mua;

public class Grammar
{
    
    static final int VERB = 0; 
    static final int ADVERB = 1;
    static final int ADJECTIVE = 2;
    static final int NOUN = 3;
    static final int PRONOUN = 4;
    static final int PREPOSITION = 5;
    static final int CONNECTOR = 6;
    static final int NEGATION = 7;
    static final int STRING = 8;
    static final int NUMERIC = 9;
    static final int ARTICLE = 10;
    static final int SUPERLATIVE = 11;
    static final int STOPWORD = 12;
    static final int TERMINATOR = 13;
    static final int RAWVERB = 14;
    static final int DIRECTION = 15;

    //int frame[][] = new int[16][];
    
    
    public Grammar()
    {
    	//int frame[][] = new int[16][];
        int[][] frame	= {
        					  { 
	        					  	Word.WT_ADVERB ,		//VERB
				                	Word.WT_DIRECTION ,
				                	Word.WT_NOUN ,
				                	Word.WT_STRING ,
				                	Word.WT_ARTICLE ,
				                	Word.WT_PRONOUN ,
				                	Word.WT_NUMERIC ,
				                	Word.WT_SUPERLATIVE ,
				                	Word.WT_ADJECTIVE ,
				                	Word.WT_PREPOSITION ,
				                	Word.WT_CONNECTOR
			                    },
			                    {
			                     	Word.WT_ARTICLE, //ADVERB
									Word.WT_VERB,
									Word.WT_NOUN,
									Word.WT_ADJECTIVE,
									Word.WT_CONNECTOR,
									Word.WT_NUMERIC,
									Word.WT_PREPOSITION,
									Word.WT_ADVERB,
			                    } ,
			                    {
			                    	Word.WT_NOUN, 		//ADJECTIVE
									Word.WT_PRONOUN,
									Word.WT_ADJECTIVE,
									Word.WT_CONNECTOR
								},
								{
									Word.WT_NOUN,		//NOUN
									Word.WT_ARTICLE,
									Word.WT_PREPOSITION,
									Word.WT_STRING,
									Word.WT_NEGATION,
									Word.WT_CONNECTOR,
									Word.WT_NUMERIC,
									Word.WT_ADVERB
								},
								{
									Word.WT_ARTICLE,
									Word.WT_NOUN,
									Word.WT_ADJECTIVE,
									Word.WT_PREPOSITION,
									Word.WT_CONNECTOR
								}
			       		};
    		//{{INIT_CONTROLS
		//}}
}
	//{{DECLARE_CONTROLS
	//}}
}