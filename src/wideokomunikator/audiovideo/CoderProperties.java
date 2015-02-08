/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wideokomunikator.audiovideo;

import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IStreamCoder;
import java.io.Serializable;

/**
 *
 * @author Piotr
 */
public class CoderProperties implements Serializable{
    public final boolean isAudio;
    public final int rate = 1000;
    public CoderProperties(IStreamCoder coder) {
        isAudio = (coder.getCodecType() == ICodec.Type.CODEC_TYPE_AUDIO);
    }
    
}
