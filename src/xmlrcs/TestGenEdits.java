/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package xmlrcs;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.version.Version;
import edu.uwm.cs.molhado.delta.Edit;
import edu.uwm.cs.molhado.xml.simple.SimpleXmlParser3;
import java.util.ArrayList;

/**
 *
 * @author chengt
 */
public class TestGenEdits {

	public static void main(String[] args) throws Exception{
		String s0 = "<a z='10'></a>";
		String s1 = "<x molhado:id='0' x='0' y='5'><c molhado:id='1'/><d molhado:id='2'/></x>";
		SimpleXmlParser3 p = new SimpleXmlParser3(0,0);
		IRNode n = p.parse(s0);
		Version v0 = Version.getVersion();
		p.parse(n, s1);
		Version v1 = Version.getVersion();
		
		XmlRcs rcs = new XmlRcs();
		ArrayList<Edit> edits = rcs.generateEdits(n, v0, v1);
		for(Edit e:edits){
			System.out.println(e);
		}
		
	}
	
}
