/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.ineo.nlp.language.BaseProcessor
 *  com.ineo.nlp.language.Token
 *  com.ineo.nlp.language.baseprocessors.BasicLTS
 *  com.ineo.nlp.machinelearning.ID3
 */
package com.ineo.nlp.language.baseprocessors;

import com.ineo.nlp.language.BaseProcessor;
import com.ineo.nlp.language.Token;
import com.ineo.nlp.language.baseprocessors.BasicLTS;
import com.ineo.nlp.machinelearning.ID3;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class BasicLemmatizer
implements BaseProcessor {
    ID3 id3;

    public int getProcessorType() {
        return 8;
    }

    public void processTokens(Token[] tokens) {
        ArrayList<String> feats = new ArrayList<String>();
        for (int i = 0; i < tokens.length; ++i) {
            Token t = tokens[i];
            if (Pattern.matches("\\p{Punct}", t.word)) {
                t.lemma = t.word;
                continue;
            }
            int n = t.word.length();
            feats.clear();
            for (int k = 1; k < 8; ++k) {
                if (n - k >= 0) {
                    feats.add("c" + k + ":" + t.word.charAt(n - k));
                    continue;
                }
                feats.add("c" + k + ":_");
            }
            if (t.tag != null) {
                feats.add("m:" + t.tag);
            }
            String[] outs = this.id3.classify(feats).split(" ")[0].split("\\|");
            int sz = Integer.parseInt(outs[0]);
            String rpl = "";
            if (outs.length > 1) {
                rpl = outs[1];
            }
			
			if (n > sz)
				t.lemma = t.word.substring(0, n - sz) + rpl;
			else
				t.lemma = t.word;
        }
    }

    public void loadModel(String folder) {
        try {
            this.id3 = ID3.createFromFile((String)(folder + "/lemma2.id3"));
        }
        catch (IOException ex) {
            Logger.getLogger(BasicLTS.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
