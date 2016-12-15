package xmlrcs;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IRSequence;
import edu.cmu.cs.fluid.tree.Tree;
import edu.cmu.cs.fluid.util.Iteratable;
import edu.cmu.cs.fluid.version.Version;
import edu.cmu.cs.fluid.version.VersionMarker;
import edu.uwm.cs.molhado.delta.AttributeAddition;
import edu.uwm.cs.molhado.delta.AttributeDeletion;
import edu.uwm.cs.molhado.delta.AttributeUpdate;
import edu.uwm.cs.molhado.delta.ChildrenUpdate;
import edu.uwm.cs.molhado.delta.Edit;
import edu.uwm.cs.molhado.delta.NameUpdate;
import edu.uwm.cs.molhado.delta.NodeAddition;
import edu.uwm.cs.molhado.delta.Revision;
import edu.uwm.cs.molhado.delta.RevisionHistory;
import edu.uwm.cs.molhado.delta.RevisionList;
import edu.uwm.cs.molhado.merge.XmlDocMerge;
import edu.uwm.cs.molhado.util.Attribute;
import edu.uwm.cs.molhado.util.AttributeList;
import edu.uwm.cs.molhado.util.Property;
import edu.uwm.cs.molhado.xml.simple.SimpleXmlParser3;
import edu.uwm.cs.molhado.xml.simple.VDocXmlParser;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.Key;
import java.security.KeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.*;
import java.util.ArrayList;
import javax.xml.crypto.dsig.keyinfo.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.Vector;
import javax.swing.JOptionPane;
import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.KeySelectorException;
import javax.xml.crypto.KeySelectorResult;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
//import testnodeapi.momergeui;

/**
 *
 * @author chengt
 */
public class XmlRcs {

	public static void main(String[] args) throws Exception {
		new XmlRcs().checkin2(new File("test.xml"));
	}

	public ArrayList<Integer> getRevisionNumbers(File versionedFile) throws Exception {
		VDocXmlParser p = new VDocXmlParser();
		RevisionHistory rh = p.parseRevisionHistory(versionedFile);
		ArrayList<Integer> array = new ArrayList<Integer>();
		for (int i = 0; i < rh.count(); i++) {
			array.add(i);
		}
		array.add(rh.count());
		return array;
	}

	public void checkin(File infile) throws Exception {
		//String fname = infile.getName();
		//int dot = fname.lastIndexOf(".");
		//String fext = fname.substring(dot+1);
		//String vfname = fname.substring(0, dot) + ".v" + fext;
		//File versionedFile = new File(vfname);
		checkin2(infile);
		//checkin(infile, versionedFile);
	}

	public void checkin2(File infile) throws Exception {
		VDocXmlParser p = new VDocXmlParser();
		RevisionHistory rh = new RevisionHistory();
		IRNode root = p.parse(infile, rh);

		//if (rh.count() == 0){
		//	System.out.println("Initial check in");
		//	SimpleXmlParser3.writeToFile(infile, root);
		//}

		Version parent = Version.getVersion();

		p.parseNewVDocContent(root, infile);
		Version child = Version.getVersion();

		ArrayList<Edit> edits = generateEdits(root, parent, child);
		if (edits.size() > 0) {
			Revision r = rh.newRevision("test");
			r.setEdits(edits);
			VDocXmlParser.writeToFileWithDelta(infile, root, rh);
		} else {
			SimpleXmlParser3.writeToFile(infile, root);
		}
	}

	/*
	public void checkin(File infile, File versionedFile) throws Exception {
	SimpleXmlParser3 p = new SimpleXmlParser3(0);
	if (!versionedFile.exists()){
	IRNode root = p.parse(infile);
	SimpleXmlParser3.writeToFile(versionedFile, root);
	System.out.println("Checked in as revision 0" );
	} else {
	RevisionHistory rh = new RevisionHistory();
	IRNode root = p.parse(versionedFile, rh);
	Version parent = Version.getVersion();
	
	p.parse(root, infile);
	Version child = Version.getVersion();
	
	Revision r = rh.newRevision();
	ArrayList<Edit> edits = generateEdits(root, parent, child);
	r.setEdits(edits);
	SimpleXmlParser3.writeToFileWithDelta(versionedFile, root, rh);
	System.out.println("Checked in as revision " + (rh.count()-1));
	}
	}
	
	 *
	 */
	public void checkoutContentToFile(String revision, File versionedFile, File outFile)
			  throws Exception {
		SimpleXmlParser3.writeToFile(outFile, checkoutIRTree(revision, versionedFile));
	}

	public String checkoutContentAsString(String revision, File versionedFile)
			  throws Exception {
		return SimpleXmlParser3.toStringWithID(checkoutIRTree(revision, versionedFile));
	}

	/*
	 * first,
	parent version = read the latest content + delta (using DeltaParsingHandler)
	load inskcape with file
	save content with inkscape
	
	child version = read the new content from file using (using ContentParsingHandler)
	
	compute delta
	
	write delta and latest content back to file.
	
	 */
	public void runApplication(String app, File inputfile) throws Exception {
//		if (!validateXmlDoc(inputfile.toString())){
//			System.out.println("Version history has been altered");
//			return ;
//		}
		long lastModified = inputfile.lastModified();
		VDocXmlParser p = new VDocXmlParser();
		//RevisionHistory tmp  = p.parseRevisionHistory(inputfile);

		RevisionHistory rh = new RevisionHistory();
		Version.setVersion(Version.getInitialVersion());

		//must set the max ID here
		//p.setMouid(tmp.getMaxNodeId());
		IRNode n = p.parse(inputfile, rh);
		//System.out.println(SimpleXmlParser3.toStringWithID(n));

		//System.out.println(SimpleXmlParser3.toStringWithDelta(n,rh));
		Version parent = Version.getVersion();

		//first time, need to stamp the elements with ID before loading
		if (rh.count() == 0) {
			SimpleXmlParser3.writeToFileWithID(inputfile, n);
		}

		//start process to edit document
		Process process = Runtime.getRuntime().exec(app + " " + inputfile.getAbsolutePath());
		process.waitFor();

		//did document change?
		boolean changed = lastModified < inputfile.lastModified() ? true : false;
		if (!changed) {
			return;
		}

		//System.out.println("=====================");
		//if content has changed...read inputfile
		Version.setVersion(parent);
		p.parseNewVDocContent(n, inputfile);
		//System.out.println(SimpleXmlParser3.toStringWithID(n));
		Version child = Version.getVersion();

		ArrayList<Edit> edits = generateEdits(n, parent, child);
		if (edits.size() > 0 || changed) {
			String s = (String) JOptionPane.showInputDialog(null,
					  "Please give a version name or tag", "Version Name",
					  JOptionPane.PLAIN_MESSAGE, null, null, null);

			String name = "";
			if ((s != null) && (s.length() > 0)) {
				name = s;
			}

			/**
			 * give curRevision an ID, a user name, parents (if any)
			 */
			UUID curId = Revision.createId();
			String user = System.getProperty("user.name");

			//current's parent is the last's current's ID
			rh.clearParentIds();
			rh.setCurRevisionId(curId);
			String prevUser = rh.getCurRevisionUser();
			rh.setCurRevisionUser(user);

			System.out.println("revisions count:" + rh.count());
			if (rh.count() > 0) {
				System.out.println("=================");
				UUID parentId = rh.getLast().getId();
				Revision r = rh.newRevision(name);
				Revision x = rh.get(parentId);
				r.addParent(x);
				r.setEdits(edits);
				r.setUser(prevUser);
				rh.addCurParentIds(r.getId());
			} else {
				System.out.println("-------------------");
				Revision r = rh.newRevision(name);
				//UUID parentId = rh.getLast().getId();
				r.setEdits(edits);
				//rh.addCurParentIds(r.getId());
			}
			VDocXmlParser.writeToFileWithDelta(inputfile, n, rh);
			signXmlDoc(inputfile.toString());
		} else {
			//	SimpleXmlParser3.writeToFile(inputfile, n);
		}
	}

	private IRNode checkoutIRTree(String revision, File versionedFile)
			  throws Exception {
		VDocXmlParser p = new VDocXmlParser();
		RevisionHistory rh = new RevisionHistory();
		IRNode n = p.parse(versionedFile, rh);
		//System.out.println(SimpleXmlParser3.toStringWithID(n));
		Version latest = Version.getVersion();
		Version.setVersion(latest);
		if (revision != null) {
			Revision r = rh.get(revision);
			r.patch(n);
			Version retVersion = Version.getVersion();
		}
		return n;
	}

	
	public void merge2(File lFile, File rFile, File oFile) throws
			  ParserConfigurationException, SAXException, IOException, Exception {

//		boolean versionDataAltered = false;
//		if (!validateXmlDoc(lFile.toString())) {
//			System.out.println(lFile.toString() + ":Version history has been altered");
//			versionDataAltered = true;
//		}
//
//		if (!validateXmlDoc(rFile.toString())) {
//			System.out.println(rFile.toString() + ":Version history has been altered");
//			versionDataAltered = true;
//		}
//
//		if (versionDataAltered) {
//			return;
//		}

		VDocXmlParser lParser = new VDocXmlParser();
		VDocXmlParser rParser = new VDocXmlParser();
		VDocXmlParser mParser = new VDocXmlParser();

		RevisionHistory lRevisionHistory = new RevisionHistory();
		RevisionHistory rRevisionHistory = new RevisionHistory();


		Version.setVersion(Version.getInitialVersion());
		IRNode lNode = lParser.parse(lFile, lRevisionHistory);
		Version lParentVersion = Version.getVersion();
		String lContent = SimpleXmlParser3.toStringWithID(lNode);

		Version.setVersion(Version.getInitialVersion());
		IRNode rNode = rParser.parse(rFile, rRevisionHistory);
		Version rParentVersion = Version.getVersion();
		String rContent = SimpleXmlParser3.toStringWithID(rNode);


		//find the base versions [0] = rev on left tree, [1] rev on right tree
		Revision[] bases = findBase(lRevisionHistory, rRevisionHistory);

		//construct the base document
		Version.setVersion(lParentVersion);
		Version lBaseVersion = null;
		String lBaseContent = null;
		if (bases != null) {
			bases[0].patch(lNode);
			lBaseVersion = Version.getVersion();
			lBaseContent = SimpleXmlParser3.toStringWithID(lNode);
		}

		//creating t0
		Version.setVersion(Version.getInitialVersion());
		IRNode baseRoot = mParser.parse(lBaseContent);
		Version baseVersion = Version.getVersion();

		//create t0
		Version.setVersion(baseVersion);
		mParser.parse(baseRoot, lContent);
		Version v1 = Version.getVersion();

		//create t1
		Version.setVersion(baseVersion);
		mParser.parse(baseRoot, rContent);
		Version v2 = Version.getVersion();

		Version lv = v1;
		Version rv = v2;

		//which document has more nodes?  Let's use the document with the most
		RevisionHistory lrh = lRevisionHistory;
		RevisionHistory rrh = rRevisionHistory;
		Revision lbr = bases[0];
		Revision rbr = bases[1];
		File writeFile = lFile;
		String lbc = lContent;
		String rbc = rContent;
		int maxId = lRevisionHistory.getMaxNodeId();
		if (lRevisionHistory.getMaxNodeId() < rRevisionHistory.getMaxNodeId()) {
			maxId = rRevisionHistory.getMaxNodeId();
			lv = v2;
			rv = v1;
			lrh = rRevisionHistory;
			rrh = lRevisionHistory;
			lbr = bases[1];
			rbr = bases[0];
			lbc = rContent;
			rbc = lContent;
			writeFile = rFile;
		}


		//relabel nodes and delta with new IDs
		Version.setVersion(rv);
		ArrayList<IRNode> toRelabel = minus(baseVersion, rv, baseRoot);
		//old->new ids
		HashMap<Integer, Integer> idMap = new HashMap<Integer, Integer>();
		for (IRNode n : toRelabel) {
			int oldId = n.getSlotValue(SimpleXmlParser3.mouidAttr);
			idMap.put(oldId, maxId);
			n.setSlotValue(SimpleXmlParser3.mouidAttr, maxId++);
		}
		updateIds(rrh, rbr, idMap);
		rv = Version.getVersion();

		//merging
		XmlDocMerge merge = new XmlDocMerge(SimpleXmlParser3.tree, SimpleXmlParser3.changeRecord,
				  baseRoot, new VersionMarker(lv), new VersionMarker(baseVersion), new VersionMarker(rv));

		Version v3 = merge.merge();

		if (v3 != null) {

//			ArrayList<Edit> l1Edits = generateEdits(baseRoot, baseVersion, lv);
//			Revision s = lrh.newRevision(lrh.getLast(), "L1");
//			s.setEdits(l1Edits);

			ArrayList<Edit> l2Edits = generateEdits(baseRoot, lv, v3);
			Revision t = lrh.newRevision(lrh.getLast(), "L");
			t.setEdits(l2Edits);
			t.setUser(lrh.getCurRevisionUser());
//
//			ArrayList<Edit> r1Edits = generateEdits(baseRoot, baseVersion, rv);
//			Revision x = rrh.newRevision(rrh.getLast(), "R1");
//			x.setEdits(r1Edits);

			ArrayList<Edit> r2Edits = generateEdits(baseRoot, rv, v3);
			Revision y = rrh.newRevision(rrh.getLast(), "R");
			y.setEdits(r2Edits);
			y.setUser(rrh.getCurRevisionUser());

			rbr.removeParents();
			rbr.addParent(lbr.getParents().get(0));
			rbr.relabelIds(idMap);


			UUID curId = Revision.createId();
			String user = System.getProperty("user.name");

			//current's parent is the last's current's ID
			lrh.clearParentIds();
			lrh.addCurParentIds(t.getId());
			lrh.addCurParentIds(y.getId());
			lrh.setCurRevisionId(curId);
			String prevUser = lrh.getCurRevisionUser();
			lrh.setCurRevisionUser(user);
			lrh.setMaxNodeId(maxId);

			Version.setVersion(v3);
			VDocXmlParser.writeToFileWithDelta(oFile, baseRoot, lrh);
			signXmlDoc(oFile.toString());
		} else {
			Version.saveVersion(v2);
			Vector<XmlDocMerge.ConflictInfo> conflicts = merge.getConflicts();
			for (XmlDocMerge.ConflictInfo info : conflicts) {
				System.out.println(" :" + info.description);
			}
//				momergeui ui = new momergeui(baseVersion, v1, v2, baseRoot);
//				ui.merge();
//				ui.setLocationRelativeTo(null);
//				ui.setVisible(true);
//				v3 = ui.merge();
//				if (v3 == null) {
//					System.exit(0);
//				}
		}


	}


	public void merge(File lFile, File rFile, File oFile) throws
			  ParserConfigurationException, SAXException, IOException, Exception {

//		boolean versionDataAltered = false;
//		if (!validateXmlDoc(lFile.toString())) {
//			System.out.println(lFile.toString() + ":Version history has been altered");
//			versionDataAltered = true;
//		}
//
//		if (!validateXmlDoc(rFile.toString())) {
//			System.out.println(rFile.toString() + ":Version history has been altered");
//			versionDataAltered = true;
//		}
//
//		if (versionDataAltered) {
//			return;
//		}

		VDocXmlParser lParser = new VDocXmlParser();
		VDocXmlParser rParser = new VDocXmlParser();
		VDocXmlParser mParser = new VDocXmlParser();

		RevisionHistory lRevisionHistory = new RevisionHistory();
		RevisionHistory rRevisionHistory = new RevisionHistory();


		Version.setVersion(Version.getInitialVersion());
		IRNode lNode = lParser.parse(lFile, lRevisionHistory);
		Version lParentVersion = Version.getVersion();
		String lContent = SimpleXmlParser3.toStringWithID(lNode);

		Version.setVersion(Version.getInitialVersion());
		IRNode rNode = rParser.parse(rFile, rRevisionHistory);
		Version rParentVersion = Version.getVersion();
		String rContent = SimpleXmlParser3.toStringWithID(rNode);


		//find the base versions [0] = rev on left tree, [1] rev on right tree
		Revision[] bases = findBase(lRevisionHistory, rRevisionHistory);

		//construct the base document
		Version.setVersion(lParentVersion);
		Version lBaseVersion = null;
		String lBaseContent = null;
		if (bases != null) {
			bases[0].patch(lNode);
			lBaseVersion = Version.getVersion();
			lBaseContent = SimpleXmlParser3.toStringWithID(lNode);
		}

		//creating t0
		Version.setVersion(Version.getInitialVersion());
		IRNode baseRoot = mParser.parse(lBaseContent);
		Version baseVersion = Version.getVersion();

		//create t0
		Version.setVersion(baseVersion);
		mParser.parse(baseRoot, lContent);
		Version v1 = Version.getVersion();

		//create t1
		Version.setVersion(baseVersion);
		mParser.parse(baseRoot, rContent);
		Version v2 = Version.getVersion();

		Version lv = v1;
		Version rv = v2;

		//which document has more nodes?  Let's use the document with the most
		RevisionHistory lrh = lRevisionHistory;
		RevisionHistory rrh = rRevisionHistory;
		Revision lbr = bases[0];
		Revision rbr = bases[1];
		File writeFile = lFile;
		String lbc = lContent;
		String rbc = rContent;
		int maxId = lRevisionHistory.getMaxNodeId();
		if (lRevisionHistory.getMaxNodeId() < rRevisionHistory.getMaxNodeId()) {
			maxId = rRevisionHistory.getMaxNodeId();
			lv = v2;
			rv = v1;
			lrh = rRevisionHistory;
			rrh = lRevisionHistory;
			lbr = bases[1];
			rbr = bases[0];
			lbc = rContent;
			rbc = lContent;
			writeFile = rFile;
		}


		//relabel nodes and delta with new IDs
		Version.setVersion(rv);
		ArrayList<IRNode> toRelabel = minus(baseVersion, rv, baseRoot);
		//old->new ids
		HashMap<Integer, Integer> idMap = new HashMap<Integer, Integer>();
		for (IRNode n : toRelabel) {
			int oldId = n.getSlotValue(SimpleXmlParser3.mouidAttr);
			idMap.put(oldId, maxId);
			n.setSlotValue(SimpleXmlParser3.mouidAttr, maxId++);
		}
		updateIds(rrh, rbr, idMap);
		rv = Version.getVersion();

		//merging
		XmlDocMerge merge = new XmlDocMerge(SimpleXmlParser3.tree, SimpleXmlParser3.changeRecord,
				  baseRoot, new VersionMarker(lv), new VersionMarker(baseVersion), new VersionMarker(rv));

		Version v3 = merge.merge();

		if (v3 != null) {

			ArrayList<Edit> l1Edits = generateEdits(baseRoot, baseVersion, lv);
			Revision s = lrh.newRevision(lrh.getLast(), "L1");
			s.setEdits(l1Edits);

			ArrayList<Edit> l2Edits = generateEdits(baseRoot, lv, v3);
			Revision t = lrh.newRevision(lrh.getLast(), "L2");
			t.setEdits(l2Edits);

			ArrayList<Edit> r1Edits = generateEdits(baseRoot, baseVersion, rv);
			Revision x = rrh.newRevision(rrh.getLast(), "R1");
			x.setEdits(r1Edits);

			ArrayList<Edit> r2Edits = generateEdits(baseRoot, rv, v3);
			Revision y = rrh.newRevision(rrh.getLast(), "R2");
			y.setEdits(r2Edits);

			rbr.removeParents();
			rbr.addParent(lbr.getParents().get(0));
			rbr.relabelIds(idMap);


			UUID curId = Revision.createId();
			String user = System.getProperty("user.name");

			//current's parent is the last's current's ID
			lrh.clearParentIds();
			lrh.addCurParentIds(s.getId());
			lrh.addCurParentIds(t.getId());
			lrh.setCurRevisionId(curId);
			String prevUser = lrh.getCurRevisionUser();
			lrh.setCurRevisionUser(user);
			lrh.setMaxNodeId(maxId);

			Version.setVersion(v3);
			VDocXmlParser.writeToFileWithDelta(oFile, baseRoot, lrh);
			signXmlDoc(oFile.toString());
		} else {
			Version.saveVersion(v2);
			Vector<XmlDocMerge.ConflictInfo> conflicts = merge.getConflicts();
			for (XmlDocMerge.ConflictInfo info : conflicts) {
				System.out.println(" :" + info.description);
			}
//				momergeui ui = new momergeui(baseVersion, v1, v2, baseRoot);
//				ui.merge();
//				ui.setLocationRelativeTo(null);
//				ui.setVisible(true);
//				v3 = ui.merge();
//				if (v3 == null) {
//					System.exit(0);
//				}
		}


	}

	private void updateIds(RevisionHistory rh, Revision base, HashMap<Integer, Integer> idMap) {
	}

	private boolean isInVersion(IRNode node, Version v, IRNode root) {
		if (node == root) {
			return true;
		}
		Version.saveVersion(v);
		IRNode parent = SimpleXmlParser3.tree.getParentOrNull(node);
		Version.restoreVersion();

		return (parent != null);
	}

	private ArrayList<IRNode> minus(Version v0, Version v2, IRNode root) {
		Version.setVersion(v2);
		Iteratable<IRNode> l = SimpleXmlParser3.tree.topDown(root);
		ArrayList<IRNode> list = new ArrayList<IRNode>();
		while (l.hasNext()) {
			IRNode n = l.next();
			if (!isInVersion(n, v0, root)) {
				list.add(n);
			}
		}
		return list;
	}

	/**
	 * Reassign nodes with new IDs.  We will start with the maxId from one of the
	 * document.  We need a map of old id --> new id so we can update the revisions
	 * that refer to old id with the new id.
	 * @param maxId
	 * @param nodes
	 * @return 
	 */
	private HashMap<Integer, Integer> reassignIds(Version version, ArrayList<IRNode> nodes, int maxId) {
		HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
		for (IRNode n : nodes) {
			System.out.println(">>> " + maxId);
			n.setSlotValue(SimpleXmlParser3.mouidAttr, maxId++);
		}
		return map;
	}

	private Revision[] findBase(RevisionHistory lrh, RevisionHistory rrh) {
		RevisionList lRevList = lrh.getRevisions();
		RevisionList rRevList = rrh.getRevisions();
		int min = Math.min(lRevList.size(), rRevList.size());

		int i = 0;

		Revision r = null;
		Revision l = null;

		for (; i < min; i++) {
			r = rRevList.get(i);
			l = lRevList.get(i);
			if (!r.equals(l)) {
				break;
			}
		}
		//Revision base = rRevList.get(i);
		Revision[] bases = {lRevList.get(i), rRevList.get(i)};
		return bases;
	}

	private ArrayList<IRNode> getAllNodes(Version v, IRNode root) {
		ArrayList<IRNode> nodes = new ArrayList<IRNode>();
		Version.saveVersion(v);
		Iteratable<IRNode> it = SimpleXmlParser3.tree.depthFirstSearch(root);
		while (it.hasNext()) {
			nodes.add(it.next());
		}
		Version.restoreVersion();
		return nodes;
	}

	private void visitNewNode(ArrayList<Edit> edits, IRNode n, Version v) {
		int nodeId = getNodeId(n, v);
		String name = getTagName(n, v);
		AttributeList attrs = getAttrList(n, v);
		NodeAddition na = new NodeAddition(nodeId, name);
		edits.add(na);
		for (int i = 0; i < attrs.size(); i++) {
			String attrName = attrs.getName(i);
			String attrVal = attrs.getValue(i);
			AttributeAddition au = new AttributeAddition(nodeId, attrName, attrVal);
			edits.add(au);
		}

	}

	private void visitName(ArrayList<Edit> edits, IRNode n, Version pv, Version cv) {
		String name0 = getTagName(n, pv);
		String name1 = getTagName(n, cv);
		if (!name0.equals(name1)) {
			NameUpdate update = new NameUpdate(getNodeId(n, pv), name1, name0);
			edits.add(update);
		}
	}

	private void visitAttributes(ArrayList<Edit> edits, IRNode n, Version pv, Version cv) {
		AttributeList list0 = getAttrList(n, pv);
		AttributeList list1 = getAttrList(n, cv);
		if (!list0.equals(list1)) {
			AttributeList[] l = getAttrDiff(list0, list1);
			int nodeId = getNodeId(n, cv);
			for (int i = 0; i < l[0].size(); i++) {
				Attribute a = l[0].get(i);
				AttributeAddition aa = new AttributeAddition(nodeId, a.getName(), a.getValue());
				edits.add(aa);
			}
			for (int i = 0; i < l[1].size(); i++) {
				Attribute a = l[1].get(i);
				Attribute b = l[2].get(i);
				AttributeUpdate au = new AttributeUpdate(nodeId, a.getName(), b.getValue(), a.getValue());
				edits.add(au);
			}

			for (int i = 0; i < l[3].size(); i++) {
				Attribute a = l[3].get(i);
				AttributeDeletion ad = new AttributeDeletion(nodeId, a.getName(), a.getValue());
				edits.add(ad);
			}
		}

	}

	private void visitChildren(ArrayList<Edit> edits, IRNode root, IRNode n, Version pv, Version cv) {
		ArrayList<IRNode> c0 = getChildren(n, pv);
		ArrayList<IRNode> c1 = getChildren(n, cv);
		ArrayList<Integer> c0Ids = new ArrayList<Integer>();
		ArrayList<Integer> c1Ids = new ArrayList<Integer>();
		if (!c0.equals(c1)) {
			for (IRNode x : c0) {
				c0Ids.add(getNodeId(x, pv));
			}
			for (IRNode x : c1) {
				c1Ids.add(getNodeId(x, cv));
			}
			ChildrenUpdate update = new ChildrenUpdate(getNodeId(n, cv), c1Ids, c0Ids);
			edits.add(update);
		}
	}

	protected ArrayList<Edit> generateEdits(IRNode root, Version parentVersion, Version childVersion) {

		ArrayList<Edit> edits = new ArrayList<Edit>();

		//nodes in parent version
		ArrayList<IRNode> pvNodes = getAllNodes(parentVersion, root);

		//nodes in child version
		ArrayList<IRNode> cvNodes = getAllNodes(childVersion, root);

		//node that exists only in parent version
		ArrayList<IRNode> pvOnlyNodes = new ArrayList<IRNode>();

		//node that exist in both versions
		ArrayList<IRNode> intersect = new ArrayList<IRNode>();

		//add nodes that are new, this must be done first
		for (IRNode n : pvNodes) {
			if (!cvNodes.contains(n)) {
				pvOnlyNodes.add(n);
				visitNewNode(edits, n, parentVersion);
			} else {
				intersect.add(n);
			}
		}


		// update nodes that exist in both version
		for (IRNode n : intersect) {
			//do they have the same name?
			visitName(edits, n, parentVersion, childVersion);

			//do they have the same attributes?
			visitAttributes(edits, n, parentVersion, childVersion);

			//do they have the same children?
			visitChildren(edits, root, n, parentVersion, childVersion);
		}

		//now must update the children of nodes added
		//must be done last or we will have some problems
		//with children already moved.
		for (IRNode n : pvOnlyNodes) {
			ArrayList<IRNode> c0 = getChildren(n, parentVersion);
			if (c0.isEmpty()) {
				continue;
			}
			ArrayList<IRNode> c1 = new ArrayList<IRNode>(); //empty
			ArrayList<Integer> c0Ids = new ArrayList<Integer>();
			ArrayList<Integer> c1Ids = new ArrayList<Integer>();
			for (IRNode x : c0) {
				c0Ids.add(getNodeId(x, parentVersion));
			}
			ChildrenUpdate update = new ChildrenUpdate(getNodeId(n, parentVersion), c1Ids, c0Ids);
			edits.add(update);
		}

		return edits;
	}

	private ArrayList<IRNode> getChildren(IRNode n, Version v) {
		Tree tree = SimpleXmlParser3.tree;
		ArrayList<IRNode> children = new ArrayList<IRNode>();
		Version.saveVersion(v);
		try {
			int numChildren = tree.numChildren(n);
			for (int i = 0; i < numChildren; i++) {
				children.add(tree.getChild(n, i));
			}
		} catch (Exception e) {
		}
		Version.restoreVersion();
		return children;
	}

	private AttributeList getAttrList(IRNode n, Version v) {
		AttributeList attrs = new AttributeList();
		Version.saveVersion(v);
		if (n.valueExists(SimpleXmlParser3.attrsSeqAttr)) {
			IRSequence<Property> seq = n.getSlotValue(SimpleXmlParser3.attrsSeqAttr);
			for (int i = 0; i < seq.size(); i++) {
				Property p = seq.elementAt(i);
				attrs.addAttribute(new Attribute(p.getName(), p.getValue()));
			}
		}
		Version.restoreVersion();
		return attrs;
	}

	private AttributeList[] getAttrDiff(AttributeList l0, AttributeList l1) {
		AttributeList[] l = new AttributeList[4];
		l[0] = new AttributeList();
		l[1] = new AttributeList();
		l[2] = new AttributeList();
		l[3] = new AttributeList();
		for (int i = 0; i < l0.size(); i++) {
			Attribute a = l0.get(i);
			int index = l1.indexOf(a);
			if (index == -1) {
				l[0].addAttribute(a);
			} else {
				Attribute b = l1.get(index);
				if (!a.getValue().equals(b.getValue())) {
					l[1].addAttribute(a);
					l[2].addAttribute(b);
				}
			}
		}

		for (int i = 0; i < l1.size(); i++) {
			Attribute a = l1.get(i);
			int index = l0.indexOf(a);
			if (index == -1) {
				l[3].addAttribute(a);
			}
		}
		return l;
	}

	private String getTagName(IRNode n, Version v) {
		String name = null;
		Version.saveVersion(v);
		if (n.valueExists(SimpleXmlParser3.tagNameAttr)) {
			name = n.getSlotValue(SimpleXmlParser3.tagNameAttr);
		}
		Version.restoreVersion();
		return name;
	}

	private int getNodeId(IRNode n, Version v) {
		Version.saveVersion(v);
		int id = n.getSlotValue(SimpleXmlParser3.mouidAttr);
		Version.restoreVersion();
		return id;
	}

	private KeyPair createKeyPair(XMLSignatureFactory xsf)
			  throws NoSuchAlgorithmException {
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
		kpg.initialize(512);
		KeyPair kp = kpg.generateKeyPair();
		return kp;
	}

	private KeyInfo createKeyInfo(XMLSignatureFactory xsf, KeyPair kp)
			  throws NoSuchAlgorithmException, KeyException {
		KeyInfoFactory kif = xsf.getKeyInfoFactory();
		KeyValue kv = kif.newKeyValue(kp.getPublic());
		KeyInfo ki = kif.newKeyInfo(Collections.singletonList(kv));
		return ki;
	}

	private Document getDocument(String name) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document d = db.parse(name);
		return d;
	}

	private List<Reference> getReferences(XMLSignatureFactory xsf, DigestMethod dm, Document doc)
			  throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
		List<Reference> list = new ArrayList<Reference>();
		Reference r = xsf.newReference("#revision-history", dm,
				  Collections.singletonList(xsf.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)),
				  null, null);
		list.add(r);
		return list;
	}

	private void writeContent(Document doc, String file) throws
			  TransformerConfigurationException, TransformerException,
			  FileNotFoundException {
		TransformerFactory tff = TransformerFactory.newInstance();
		Transformer tf = tff.newTransformer();
		tf.transform(new DOMSource(doc), new StreamResult(new FileOutputStream(file)));
	}

	private void signDocument(String file, XMLSignatureFactory fac, SignedInfo si,
			  KeyInfo ki, KeyPair kp, Document doc) throws MarshalException,
			  XMLSignatureException, TransformerConfigurationException,
			  TransformerException, FileNotFoundException {
		//	DOMSignContext dsc = new DOMSignContext(kp.getPrivate(), doc.getDocumentElement());
		DOMSignContext dsc = new DOMSignContext(kp.getPrivate(), doc.getElementsByTagName("molhado:revision-history").item(0));
		XMLSignature xs = fac.newXMLSignature(si, ki);
		xs.sign(dsc);
		writeContent(doc, file);
	}

	private void signXmlDoc(String file) throws NoSuchAlgorithmException,
			  InvalidAlgorithmParameterException, ParserConfigurationException,
			  SAXException, IOException, KeyException, MarshalException,
			  XMLSignatureException, TransformerConfigurationException,
			  TransformerException {

		Document doc = getDocument(file);

		XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

		CanonicalizationMethod cm = fac.newCanonicalizationMethod(
				  CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null);

		SignatureMethod sm = fac.newSignatureMethod(SignatureMethod.RSA_SHA1, null);

		DigestMethod dm = fac.newDigestMethod(DigestMethod.SHA1, null);

		List<Reference> refList = getReferences(fac, dm, doc);

		SignedInfo si = fac.newSignedInfo(cm, sm, refList, null);

		KeyPair kp = createKeyPair(fac);
		KeyInfo ki = createKeyInfo(fac, kp);

		signDocument(file, fac, si, ki, kp, doc);

	}

	private boolean validateXmlDoc(String file)
			  throws ParserConfigurationException, SAXException, IOException, Exception {
		Document doc = getDocument(file);
		NodeList historyNodes = doc.getElementsByTagName("molhado:revision-history");
		NodeList nodes = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
		if (nodes.getLength() == 0 && historyNodes.getLength() > 0) {
			throw new Exception("Cannot find Signature element");
		} else if (nodes.getLength() == 0 && historyNodes.getLength() == 0) {
			//new document.  ignore validation
			return true;
		}

		XMLSignatureFactory xsf = XMLSignatureFactory.getInstance("DOM");
		KeyValue_Selector kvs = new KeyValue_Selector();
		DOMValidateContext dvc = new DOMValidateContext(kvs, nodes.item(0));

		XMLSignature signature = xsf.unmarshalXMLSignature(dvc);

		return signature.validate(dvc);
	}

	class KeyValue_Selector extends KeySelector {

		//The select method
		public KeySelectorResult select(KeyInfo keyInfo, KeySelector.Purpose keyPurpose,
				  AlgorithmMethod algorithmMethod,
				  XMLCryptoContext xmlCryptoContext)
				  throws KeySelectorException {

			if (keyInfo == null) {
				System.out.println("The keyInfo object is null ...");
			}

			SignatureMethod signatureMethod = (SignatureMethod) algorithmMethod;
			List<Object> contentList = new ArrayList<Object>();
			contentList = keyInfo.getContent();

			for (int i = 0; i < contentList.size(); i++) {

				XMLStructure xmlStructure = (XMLStructure) contentList.get(i);

				if (xmlStructure instanceof KeyValue) {

					try {
						KeyValue keyValue = (KeyValue) xmlStructure;
						final PublicKey publicKey = keyValue.getPublicKey();

						//Is algorithm compatible with method ?
						String sm_algorithm = signatureMethod.getAlgorithm();
						String pk_algorithm = publicKey.getAlgorithm();

						if (((sm_algorithm.equalsIgnoreCase(SignatureMethod.DSA_SHA1))
								  && (pk_algorithm.equalsIgnoreCase("DSA")))
								  || ((sm_algorithm.equalsIgnoreCase(SignatureMethod.RSA_SHA1))
								  && (pk_algorithm.equalsIgnoreCase("RSA")))) {
							return new KeySelectorResult() {

								public Key getKey() {
									return publicKey;
								}
							};
						} else {
							System.out.println("The method is incompatible with algorithm!");
						}
					} catch (KeyException e) {
						System.out.println(e.getMessage());
					}
				}
			}
			throw new KeySelectorException("No <KeyValue> elements!");
		}
	}
}
