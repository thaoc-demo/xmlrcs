package xmlrcs;

import java.io.File;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;

/**
 *
 * @author chengt
 */
public class Main {

	public static void main(String[] args) throws Exception {


		Option runApp = OptionBuilder.withArgName("run").hasArgs(2).
				  withArgName("app").withArgName("file").
				  withDescription("Execute an app").create("run");

		Option checkin = OptionBuilder.withArgName("checkin").hasArg().
				  withArgName("file").withDescription("Check in changes").
				  create("checkin");

		Option checkout = OptionBuilder.withArgName("checkout").hasArg().
				  withArgName("file").withDescription("Check out a revision").
				  create("checkout");

		Option revision = OptionBuilder.withArgName("rev").hasArg().
				  withArgName("revision").
				  withDescription("Specify revision for checking out").create("rev");

		Option list = OptionBuilder.withArgName("versioned file").hasArg().
				  withDescription("List all revisions").create("list");

		Option mergeOption = new Option("merge", "merge two version-aware XML docs");
		mergeOption.setArgs(3);
		mergeOption.setRequired(true);


		//	Option merge = OptionBuilder.withArgName("merge").hasArgs(2)
		//			  .withDescription("merge two version-aware docs").create("merge");

		//	Option mine = OptionBuilder.withArgName("mine").hasArg().withArgName("mine").create("mine");
		//	Option other= OptionBuilder.withArgName("other").hasArg().withArgName("other").create("other");

		Option help = new Option("help", "Help");

		OptionGroup group0 = new OptionGroup();
		group0.addOption(runApp);
		group0.addOption(checkin);
		group0.addOption(checkout);
		group0.addOption(list);
		group0.addOption(help);
		group0.addOption(mergeOption);
		OptionGroup group1 = new OptionGroup();
		group1.addOption(checkin);
		group1.addOption(checkout);
		group1.addOption(list);
		group1.addOption(help);
		group1.addOption(mergeOption);
		OptionGroup group2 = new OptionGroup();
		group2.addOption(checkin);
		group2.addOption(list);
		group2.addOption(help);
		group2.addOption(revision);
		group2.addOption(mergeOption);
		//	OptionGroup group3 = new OptionGroup();
		//	group3.addOption(merge);
		//	group3.addOption(mine);
		//	group3.addOption(other);

		Options options = new Options();
		options.addOptionGroup(group0);
		options.addOptionGroup(group1);
		options.addOptionGroup(group2);
		//	options.addOptionGroup(group3);

		CommandLineParser parser = new GnuParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine line = null;

		try {
			line = parser.parse(options, args, true);
		} catch (Exception e) {
			System.out.println("xmlrcs: error processing arguments");
			formatter.printHelp("xmlrcs", options);
			System.exit(0);
		}

		XmlRcs rcs = new XmlRcs();

		if (line.hasOption("run")) {
			String[] vals = line.getOptionValues("run");
			String app = vals[0];
			File f = new File(vals[1]);
			if (line.hasOption("rev")) {
				//checkout file /tmp/xmlrcs/file_rev.svg
				String rev = line.getOptionValue("rev");
				int dot = f.getName().lastIndexOf(".");
				String fname = f.getName().substring(0, f.getName().lastIndexOf("."));
				fname = "/tmp/" + f.getName();
				File outFile = new File(fname);
				rcs.checkoutContentToFile(rev, f, outFile);
				rcs.runApplication(app, outFile);
				if (outFile.exists()) {
					outFile.delete();
				}
			} else {
				if (!f.exists()) {
					System.out.println(f.getName() + " does not exist.");
					System.exit(0);
				}
				rcs.runApplication(app, f);
			}
		} else if (line.hasOption("checkin")) {
			File infile = new File(line.getOptionValue("checkin"));
			if (infile.exists()) {
				rcs.checkin2(infile);
			} else {
				System.out.println("xmlrcs: " + infile.getName() + " does not exist.");
				System.exit(0);
			}
		} /*
		else if (line.hasOption("checkin")) {
		String fname = line.getOptionValue("checkin");
		int dot = fname.lastIndexOf(".");
		String ext = fname.substring(dot + 1);
		String name = fname.substring(0, dot);
		if (dot == -1) {
		System.out.println(fname + " has no extension.");
		System.exit(0);
		}
		if (fname.endsWith(".xml") || fname.endsWith(".svg")) {
		File inFile = new File(fname);
		File versionedFile = new File(name + ".v" + ext);
		rcs.checkin(inFile, versionedFile);
		} else {
		System.out.println(fname + " is not supported.");
		System.exit(0);
		}
		}*/ else if (line.hasOption("checkout")) {
			String fname = line.getOptionValue("checkout");
			int dot = fname.lastIndexOf(".");
			if (dot == -1) {
				System.out.println(fname + " has no extension.");
				System.exit(0);
			}
			String ext = fname.substring(dot + 1);
			String name = fname.substring(0, dot);
//			if (fname.endsWith(".vxml") || fname.endsWith(".vsvg")) {
			if (fname.endsWith(".xml") || fname.endsWith(".svg")) {
				File versionedFile = new File(fname);
				if (versionedFile.exists()) {
					String rev = null;
					if (line.hasOption("rev")) {
						rev = line.getOptionValue("rev");
					}
					File outFile = null;
					if (rev == null) {
						outFile = new File(name + "_latest" + "." + ext);
					} else {
						outFile = new File(name + "_" + rev + "." + ext);
					}
					rcs.checkoutContentToFile(rev, versionedFile, outFile);
				} else {
					System.out.println(fname + " does not exist.");
				}
			} else {
				System.out.println(fname + " is not supported.");
				System.exit(0);
			}
		} else if (line.hasOption("list")) {
			list(rcs, new File(line.getOptionValue("list")));
		} else if (line.hasOption("merge")) {
			String[] values = line.getOptionValues("merge");
			if (values.length == 3) {
				String mine = values[0];
				String other = values[1];
				String out = values[2];
				File myFile = new File(mine);
				File otherFile = new File(other);
				File oFile = new File(out);
				if (!myFile.exists() || !otherFile.exists()) {
					System.out.println("xmlrcs -merge : one or both files do not exist");
					System.exit(0);
				}
				rcs.merge2(myFile, otherFile, oFile);
			} else {
				System.out.println("xmlrcs -merge : must two arguments");
				formatter.printHelp("xmlrcs", options);
				System.exit(0);
			}
		} else {
			formatter.printHelp("xmlrcs", options);
		}

	}

	private static void checkIn(File versionfile, File infile) {
	}

	private static void list(XmlRcs rcs, File versionedFile) {
		if (versionedFile.exists()) {
			try {
				ArrayList<Integer> revisions = rcs.getRevisionNumbers(versionedFile);
				System.out.println("Revisions: " + rcs.getRevisionNumbers(versionedFile));
			} catch (Exception ex) {
				Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
			}
		} else {
			System.out.println(versionedFile.getName() + " does not exist.");
		}
	}
}
