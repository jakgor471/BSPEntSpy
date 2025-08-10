package bspentspy;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.json.JSONException;
import org.json.JSONObject;

import bspentspy.ClassPropertyPanel.GotoEvent;
import bspentspy.Entity.KeyValue;
import bspentspy.Lexer.LexerException;
import bspentspy.SourceBSPFile.Lightmap;
import bspentspy.Undo.Command;

public class BSPEntspy {
	BSPFile map;
	String filename;
	File infile;
	JList<Entity> entList;
	FilteredEntListModel entModel;
	MapInfo info;
	Preferences preferences;
	FGD fgdFile = null;
	HashSet<Entity> previouslySelected = new HashSet<Entity>();
	
	private ArrayList<ActionListener> onMapLoadInternal = new ArrayList<ActionListener>();
	private ArrayList<ActionListener> onMapUnloadInternal = new ArrayList<ActionListener>();
	private ArrayList<ActionListener> onMapSaveInternal = new ArrayList<ActionListener>();

	static ImageIcon esIcon = new ImageIcon(BSPEntspy.class.getResource("/images/newicons/entspy.png"));
	public static final String versionTag = "v1.67";
	public static final String entspyTitle = "BSPEntSpy " + versionTag;
	
	public static JFrame frame = null;
	
	private static void checkForUpdate() throws UnsupportedEncodingException, IOException {
		if(!Preferences.userRoot().node(BSPEntspy.class.getName()).getBoolean("CheckForUpdates", true))
			return;
		
		URL url = new URL("https://api.github.com/repos/jakgor471/bspentspy/releases/latest");
		StringBuilder sb = new StringBuilder();
		
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"))) {
		    for (String line; (line = reader.readLine()) != null;) {
		        sb.append(line);
		    }
		    
		    JSONObject json = new JSONObject(sb.toString());
		    String newTag = json.getString("tag_name");
			
			if(!newTag.equals(versionTag)) {
				String newVersionURL = json.getJSONArray("assets").getJSONObject(0).getString("browser_download_url");
				System.out.println("Latest version available at " + newVersionURL);
				
				Object[] options = {"Copy link to clipboard", "Remind me later"};
				int chosen = JOptionPane.showOptionDialog(null,
						"Latest BSPEntSpy version " + newTag + " available! The version you are using (" + versionTag + ") may contain bugs."
							+ "\nDownload available here:\n\n" + newVersionURL + "\n\nYou can disable this notification in Options menu.",
						"Update available", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options,
						options[1]);
				
				if(chosen == 0) {
					Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
					cb.setContents(new StringSelection(newVersionURL), null);
				}
			}
		} catch(FileNotFoundException | JSONException e) {
			e.printStackTrace();
			System.out.println("Could not check for updates!");
		}
	}

	private void updateEntList(ArrayList<Entity> ents) {
		entModel.setEntityList(ents);
		entList.setModel(entModel);
	}

	private boolean readFile() throws IOException {
		if (map != null)
			map.close();

		try {
			RandomAccessFile in = new RandomAccessFile(this.infile, "rw");
			map = BSPFile.readFile(in);
			frame.setTitle(entspyTitle + " - " + this.filename);
			updateEntList(map.entities);
			
			for(ActionListener al : onMapLoadInternal)
				al.actionPerformed(new ActionEvent(this, 0, "mapload"));
			
		} catch (Exception e) {
			unloadFile();
			
			JOptionPane.showMessageDialog(frame, "Map " + infile.getName() + " couldn't be read!", "ERROR!",
					JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();

			return false;
		}

		return true;
	}
	
	private SecretKeySpec getSecretKey() throws UnsupportedEncodingException {
		if(map == null || map.entities == null)
			return null;
		byte[] key = null;
		
		try {
			MessageDigest msgD = MessageDigest.getInstance("SHA-256");
			for(int i = 0; i < map.entities.size(); ++i) {
				if(VMF.ignoredClasses.contains(map.entities.get(i).getKeyValue("classname")))
					continue;
				key = map.entities.get(i).toStringSpecial().getBytes("UTF-8");
				msgD.update(key);
			}
			
			key = Arrays.copyOf(msgD.digest(), 16);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
		
		SecretKeySpec specKey = new SecretKeySpec(key, "AES");
		return specKey;
	}
	
	
	private String decrypt() {
		String text = "";
		
		try (InputStreamReader rd = new InputStreamReader(BSPEntspy.class.getResourceAsStream("/text/history.html"))) {
			StringBuilder sb = new StringBuilder();

			char[] buffer = new char[4096];
			int charsRead = 0;
			
			while ((charsRead = rd.read(buffer)) != -1) {
				sb.append(buffer, 0, charsRead);
			}
			
			text = sb.toString();
		} catch (IOException | NullPointerException e) {
			e.printStackTrace();
		}
		
		try {
			SecretKeySpec specKey = getSecretKey();
			
			if(specKey == null)
				return "Could not decrypt '/text/history.html'. No map loaded. A right map is the key!";
			
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, specKey);
			byte[] decodedBytes = Base64.getDecoder().decode(text);
			byte[] decryptedBytes = cipher.doFinal(decodedBytes);
			
			if(!(new String(decryptedBytes, 0, 9)).equals("BSPENTSPY"))
				throw new Exception();
			
			return new String(decryptedBytes, 9, decryptedBytes.length - 9, "UTF-8");
		} catch(Exception e) {
			return "Could not decrypt '/text/history.html'. Invalid key! A right map is the key!";
		}
	}

	public int exec(boolean secret) throws IOException {
		preferences = Preferences.userRoot().node(getClass().getName());
		
		Thread updateThread = new Thread() {
			public void run() {
				try {
					checkForUpdate();
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		
		//TODO: uncomment
		updateThread.start();
		
		BSPEntspy.frame = new JFrame(entspyTitle);
		BSPEntspy.frame.setIconImage(esIcon.getImage());

		if (loadFGDFiles(null)) {
			System.out.println("FGD loaded: " + String.join(", ", fgdFile.loadedFgds));
		} else {
			preferences.remove("LastFGDFile");
		}

		DefaultListModel<Entity> dfm = new DefaultListModel<Entity>();
		dfm.add(0, new Entity());

		entModel = new FilteredEntListModel();
		this.entList = new JList<Entity>();
		DefaultListSelectionModel selmodel = new DefaultListSelectionModel();
		selmodel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		entList.setSelectionModel(selmodel);
		entList.setCellRenderer(new EntListRenderer());

		JMenu filemenu = new JMenu("File");
		JMenuItem mload = new JMenuItem("Load BSP");
		JMenuItem msave = new JMenuItem("Save BSP");
		
		mload.setToolTipText("Load an new map file");
		msave.setToolTipText("Save the current map file");
		mload.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.CTRL_DOWN_MASK));
		msave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
		//msave.setEnabled(false);
		filemenu.add(mload);
		filemenu.add(msave);

		JMenuItem msaveas = new JMenuItem("Save BSP as..");
		msaveas.setToolTipText("Save the current map to a chosen file");
		//msaveas.setEnabled(false);
		filemenu.add(msaveas);
		
		JMenuItem munload = new JMenuItem("Unload BSP");
		munload.setToolTipText("Unload the current map file");
		//munload.setEnabled(false);
		filemenu.add(munload);
		
		JMenuItem computeChecksum = new JMenuItem("Compute map Checksum");
		computeChecksum.setToolTipText("Compute the checksum of currently loaded map");
		filemenu.add(computeChecksum);
		
		computeChecksum.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				if(map == null || !(map instanceof SourceBSPFile))
					return;
				
				SourceBSPFile bspmap = (SourceBSPFile)map;
				try {
					long checksum = bspmap.computeChecksum();
					JOptionPane.showMessageDialog(frame, "The CRC32 checksum of the map is: 0x" + Long.toHexString(checksum).toUpperCase(), "ERROR!", JOptionPane.INFORMATION_MESSAGE);
				} catch (IOException e) {
					JOptionPane.showMessageDialog(frame, "An error occured...", "ERROR!", JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		JMenuItem mpatchvmf = new JMenuItem("Patch from VMF");
		mpatchvmf.setToolTipText("Update entity properties based on a VMF file (see more in Help)");
		//mpatchvmf.setEnabled(false);
		filemenu.add(mpatchvmf);

		filemenu.addSeparator();

		JCheckBoxMenuItem mloadfgd = new JCheckBoxMenuItem("Load FGD file");
		mloadfgd.setToolTipText("Load an FGD file to enable Smart Edit");
		mloadfgd.setSelected(fgdFile != null);
		filemenu.add(mloadfgd);
		
		filemenu.addSeparator();
		
		JMenuItem mquit = new JMenuItem("Quit");
		mquit.setToolTipText("Quit Entspy");
		mquit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK));
		filemenu.add(mquit);

		JMenu editmenu = new JMenu("Edit");
		JMenuItem mUndo = new JMenuItem("Undo");
		mUndo.setToolTipText("Undo last edit");
		//mUndo.setEnabled(false);
		mUndo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK));

		JMenuItem mRedo = new JMenuItem("Redo");
		//mRedo.setEnabled(false);
		mRedo.setToolTipText("Redo last edit");
		mRedo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_DOWN_MASK));

		mUndo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				Undo.undo();
				updateEntList(map.getEntities());
				mRedo.setEnabled(Undo.canRedo());
				mUndo.setEnabled(Undo.canUndo());
			}
		});
		mRedo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				Undo.redo();
				updateEntList(map.getEntities());
				mRedo.setEnabled(Undo.canRedo());
				mUndo.setEnabled(Undo.canUndo());
			}
		});
		editmenu.add(mUndo);
		editmenu.add(mRedo);

		Undo.addUpdateListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				mUndo.setEnabled(Undo.canUndo());
				mRedo.setEnabled(Undo.canRedo());
			}
		});

		JMenuItem mInvertSel = new JMenuItem("Invert selection");
		//mInvertSel.setEnabled(true);
		mInvertSel.setToolTipText("Invert the selection");
		mInvertSel.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, KeyEvent.CTRL_DOWN_MASK));
		editmenu.addSeparator();
		editmenu.add(mInvertSel);

		mInvertSel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				int[] selected = entList.getSelectedIndices();

				entList.clearSelection();

				int j = 0;
				int intervalStart = -1;
				int intervalEnd = 0;

				for (int i = 0; i < entModel.getSize(); ++i) {
					if (j < selected.length && selected[j] == i) {
						entList.addSelectionInterval(intervalStart, intervalEnd - 1);
						intervalStart = -1;
						++j;
						continue;
					}

					if (intervalStart < 0) {
						intervalStart = i;
						intervalEnd = i;
					}

					++intervalEnd;
				}

				entList.addSelectionInterval(intervalStart, intervalEnd - 1);
			}
		});

		JMenu helpmenu = new JMenu("Help");
		JMenuItem mhelpSearch = new JMenuItem("Searching");
		helpmenu.add(mhelpSearch);

		mhelpSearch.addActionListener(new HelpActionListener("/text/searchhelp.html"));

		JMenuItem mexportHelp = new JMenuItem("Exporting / Importing entities");
		helpmenu.add(mexportHelp);

		mexportHelp.addActionListener(new HelpActionListener("/text/exporthelp.html"));

		JMenuItem mpatchHelp = new JMenuItem("Patching from VMF");
		helpmenu.add(mpatchHelp);

		mpatchHelp.addActionListener(new HelpActionListener("/text/patchhelp.html"));

		JMenuItem fgdhelp = new JMenuItem("FGD files");
		helpmenu.add(fgdhelp);

		fgdhelp.addActionListener(new HelpActionListener("/text/fgdhelp.html"));
		
		JMenuItem lighthelp = new JMenuItem("Removing light info");
		helpmenu.add(lighthelp);

		lighthelp.addActionListener(new HelpActionListener("/text/lighthelp.html"));
		
		JMenuItem lightmaphelp = new JMenuItem("Browsing / editing lightmaps");
		helpmenu.add(lightmaphelp);

		lightmaphelp.addActionListener(new HelpActionListener("/text/lightmapshelp.html"));
		
		JMenuItem renaminghelp = new JMenuItem("Renaming the map");
		helpmenu.add(renaminghelp);

		renaminghelp.addActionListener(new HelpActionListener("/text/renaminghelp.html"));

		helpmenu.addSeparator();

		JMenuItem mcreditHelp = new JMenuItem("Credits");
		helpmenu.add(mcreditHelp);

		mcreditHelp.addActionListener(new HelpActionListener("/text/credits.html"));

		final ClassPropertyPanel rightEntPanel = new ClassPropertyPanel();
		rightEntPanel.setFGD(fgdFile);

		boolean shouldSmartEdit = fgdFile != null && preferences.getBoolean("SmartEdit", false);
		boolean shouldAddDefaultParams = fgdFile != null && preferences.getBoolean("AutoAddParams", false);
		rightEntPanel.setSmartEdit(shouldSmartEdit);
		rightEntPanel.shouldAddDefaultParameters(shouldAddDefaultParams);

		JMenu optionmenu = new JMenu("Options");

		JCheckBoxMenuItem msmartEditOption = new JCheckBoxMenuItem("Smart Edit");
		msmartEditOption.setToolTipText("If FGD file is loaded Smart Edit can be enabled. See more in Help");
		msmartEditOption.setEnabled(fgdFile != null);
		msmartEditOption.setState(shouldSmartEdit);

		JCheckBoxMenuItem maddDefaultOption = new JCheckBoxMenuItem("Auto-add default parameters");
		maddDefaultOption.setToolTipText(
				"If FGD file is loaded the default class parameters will be added but not applied unless edited");
		maddDefaultOption.setEnabled(fgdFile != null);
		maddDefaultOption.setState(shouldAddDefaultParams);
		
		JCheckBoxMenuItem mCheckForUpdates = new JCheckBoxMenuItem("Check for BSPEntSpy updates");
		mCheckForUpdates.setState(preferences.getBoolean("CheckForUpdates", true));
		
		mCheckForUpdates.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				preferences.putBoolean("CheckForUpdates", mCheckForUpdates.isSelected());
			}
		});

		maddDefaultOption.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				rightEntPanel.shouldAddDefaultParameters(maddDefaultOption.isSelected());
				preferences.putBoolean("AutoAddParams", maddDefaultOption.isSelected());
			}
		});

		msmartEditOption.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				rightEntPanel.setSmartEdit(msmartEditOption.isSelected());
				preferences.putBoolean("SmartEdit", msmartEditOption.isSelected());
			}
		});
		
		JCheckBoxMenuItem mPromptChecksum = new JCheckBoxMenuItem("Prompt checksum preservation on map save");
		mPromptChecksum.setToolTipText(
				"Only applicable for Source BSP!");
		mPromptChecksum.setState(preferences.getBoolean("preserveChecksum", false));
		
		mPromptChecksum.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				preferences.putBoolean("preserveChecksum", mPromptChecksum.isSelected());
			}
		});

		optionmenu.add(msmartEditOption);
		optionmenu.add(maddDefaultOption);
		optionmenu.addSeparator();
		optionmenu.add(mPromptChecksum);
		optionmenu.addSeparator();
		optionmenu.add(mCheckForUpdates);

		JMenu entitymenu = new JMenu("Entity");
		final JMenuItem importEntity = new JMenuItem("Import");
		importEntity.setToolTipText("Import entities from a file");
		//importEntity.setEnabled(false);
		entitymenu.add(importEntity);

		final JMenuItem exportEntity = new JMenuItem("Export");
		exportEntity.setToolTipText("Export selected entities to a file");
		//exportEntity.setEnabled(false);
		entitymenu.add(exportEntity);
		
		JMenu mapmenu = new JMenu("Map");
		
		JCheckBoxMenuItem removeLightInfo = new JCheckBoxMenuItem("Remove light information");
		removeLightInfo.setToolTipText("Remove worldlight lump data for rebaking the lights with VRAD. Takes effect on save.");
		//removeLightInfo.setEnabled(map != null && map instanceof SourceBSPFile);
		mapmenu.add(removeLightInfo);
		removeLightInfo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				if(map == null)
					return;
				
				if(!(map instanceof SourceBSPFile)) {
					return;
				}
				SourceBSPFile bspmap = (SourceBSPFile)map;
				
				bspmap.writeLights = !removeLightInfo.isSelected();
			}
		});
		
		JMenuItem lightmapBrowser = new JMenuItem("Lightmap Browser");
		mapmenu.add(lightmapBrowser);
		lightmapBrowser.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				/*if(map == null)
					return;
				
				if(!(map instanceof SourceBSPFile)) {
					return;
				}
				SourceBSPFile bspmap = (SourceBSPFile)map;
				
				try {
					ArrayList<Lightmap> lightmaps = bspmap.getLightmaps();
					File zipOutput = new File(infile.getParent() + "/" + filename.substring(0, filename.lastIndexOf('.')) + "_lightmaps.zip");
					ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipOutput));
					
					for(Lightmap img : lightmaps) {
						for(int i = 0; i < img.styles * img.axes; ++i) {
							String lmName = img.faceId + "_" + i / img.axes + "_" + i % img.axes + ".png";
							
							ZipEntry entry = new ZipEntry(lmName);
							zos.putNextEntry(entry);
							ImageIO.write(img.images[i], "png", zos);
							
						}
					}
					zos.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}*/
				
				if(map == null)
					return;
				
				if(!(map instanceof SourceBSPFile)) {
					return;
				}
				SourceBSPFile bspmap = (SourceBSPFile)map;
				
				LightmapEditor lightmapEditor = new LightmapEditor(bspmap);
				
				JDialog dialog = new JDialog(frame);
				
				JMenuBar menuBar = new JMenuBar();
				JMenu lightmapMenu = new JMenu("Lightmaps");
				menuBar.add(lightmapMenu);
				
				JMenuItem exportLightmaps = new JMenuItem("Export selected lightmaps");
				exportLightmaps.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						try {
							Lightmap[] lightmaps = lightmapEditor.getSelectedLightmaps();
							File zipOutput = new File(infile.getParent() + "/" + filename.substring(0, filename.lastIndexOf('.')) + "_lightmaps.zip");
							ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipOutput));
							
							for(Lightmap img : lightmaps) {
								for(int i = 0; i < img.styles * img.axes; ++i) {
									String lmName = img.faceId + "_s" + i / img.axes + "_a" + i % img.axes + (img.hdr ? "hdr" : "") + ".png";
									
									ZipEntry entry = new ZipEntry(lmName);
									zos.putNextEntry(entry);
									ImageIO.write(img.images[i], "png", zos);
								}
							}
							zos.close();
							
							JOptionPane.showMessageDialog(dialog, lightmaps.length + " lightmap(s) exported to '" + zipOutput.toString() + "'.");
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				});
				
				JMenuItem importLightmaps = new JMenuItem("Import selected lightmaps");
				importLightmaps.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						Pattern lightmapRegex = Pattern.compile("[/\\\\]*(\\d+)_s(\\d+)_a(\\d+)((?:hdr)*).png$");

						JFileChooser chooser = new JFileChooser(preferences.get("LastFolder", System.getProperty("user.dir")));
						chooser.setMultiSelectionEnabled(true);
						chooser.setFileFilter(new FileNameExtensionFilter("PNG files", "png"));

						chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

						// Show the dialog
						int result = chooser.showOpenDialog(null);
						
						if(result != JFileChooser.APPROVE_OPTION)
							return;
						
						StringBuilder errors = new StringBuilder();
						
						File[] selected = chooser.getSelectedFiles();
						int numImported = 0;
						for(File f : selected) {
							try {
								BufferedImage img = ImageIO.read(f);
								Matcher match = lightmapRegex.matcher(f.getName());
								
								if(!match.matches()) {
									errors.append("'").append(f.getName()).append("' does not match the '<face id>_s<style>_a<axis>.png'!\n");
									continue;
								}
								
								boolean isHdr = match.group(4) != null;
								if(bspmap.setLightmap(img, Integer.parseInt(match.group(1)), Integer.parseInt(match.group(2)), Integer.parseInt(match.group(3)), isHdr))
									numImported++;
							} catch (IOException e) {
								errors.append("'").append(f.getName()).append("' - ").append(e.getMessage());
								e.printStackTrace();
							} catch(IllegalArgumentException e) {
								errors.append("'").append(f.getName()).append("' - ").append(e.getMessage());
							}
						}
						
						if(errors.length() > 0)
							JOptionPane.showMessageDialog(dialog, errors.toString(), "ERROR!", JOptionPane.ERROR_MESSAGE);
						
						JOptionPane.showMessageDialog(dialog, "Imported " + numImported + " lightmap(s)!", "Import success", JOptionPane.INFORMATION_MESSAGE);
						lightmapEditor.refreshList();
					}
				});
				
				lightmapMenu.add(importLightmaps);
				lightmapMenu.add(exportLightmaps);
				dialog.setJMenuBar(menuBar);
				
				dialog.getContentPane().add(lightmapEditor);
				dialog.setTitle("Lightmap Browser v1.0");
				dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
				dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				dialog.setSize(400, 520);
				dialog.setLocation(frame.getLocation());
				dialog.setVisible(true);
			}
		});
		
		mapmenu.addSeparator();
		
		JCheckBoxMenuItem removePak = new JCheckBoxMenuItem("Remove Pak lump");
		removePak.setToolTipText("Remove embedded Pak lump from map file. Takes effect on save. CAUTION!");
		//removePak.setEnabled(map != null && map instanceof SourceBSPFile);
		mapmenu.add(removePak);
		removePak.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				if(map == null)
					return;
				
				if(!(map instanceof SourceBSPFile)) {
					return;
				}
				SourceBSPFile bspmap = (SourceBSPFile)map;
				
				bspmap.writePak = !removePak.isSelected();
				
				if(!bspmap.writePak)
					bspmap.embeddedPak = null;
			}
		});
		
		JCheckBoxMenuItem importPak = new JCheckBoxMenuItem("Import Pak Lump");
		importPak.setToolTipText("Import Zip file as a Pak Lump. Takes effect on save.");
		//importPak.setEnabled(false);
		mapmenu.add(importPak);
		
		importPak.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				if(map == null)
					return;
				
				if(!(map instanceof SourceBSPFile)) {
					JOptionPane.showMessageDialog(frame, "Unsupported version of BSP. This option works only for Source BSP.");
					return;
				}
				
				SourceBSPFile bspmap = (SourceBSPFile)map;
				if(importPak.isSelected()) {
					JFileChooser chooser = new JFileChooser(preferences.get("LastFolder", System.getProperty("user.dir")));
					if(chooser.showOpenDialog(frame) == JFileChooser.CANCEL_OPTION) {
						importPak.setSelected(false);
						return;
					}
					
					File zipfile = chooser.getSelectedFile();
					
					/*int result = JOptionPane.showConfirmDialog(frame, "This action will take effect on save. Continue?", entspyTitle, JOptionPane.YES_NO_OPTION);
					
					if(result == JOptionPane.NO_OPTION)
						return;*/
					
					bspmap.embeddedPak = zipfile;
					bspmap.writePak = true;
					
					removePak.setSelected(false);
				} else {
					bspmap.embeddedPak = null;
				}
				
				removePak.setEnabled(!importPak.isSelected());
			}
		});
		
		JMenuItem exportPak = new JMenuItem("Export Pak Lump");
		exportPak.setToolTipText("Export Pak lump to Zip file");
		//exportPak.setEnabled(false);
		mapmenu.add(exportPak);
		
		exportPak.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				if(map == null)
					return;
				
				if(!(map instanceof SourceBSPFile)) {
					JOptionPane.showMessageDialog(frame, "Unsupported version of BSP. This option works only for Source BSP.");
					return;
				}
				SourceBSPFile bspmap = (SourceBSPFile)map;
				File zipo = new File(infile.getParent() + "\\" + filename.substring(0, filename.lastIndexOf('.')) + "_pak.zip");
				
				try(FileOutputStream os = new FileOutputStream(zipo)) {
					bspmap.writePakToStream(os);
				} catch (IOException e) {
					JOptionPane.showMessageDialog(frame, "IO Error has occurred: " + e.getMessage(), "ERROR!", JOptionPane.ERROR_MESSAGE);
					e.printStackTrace();
					return;
				}
				
				JOptionPane.showMessageDialog(frame, "Pak lump written to '" + zipo.getAbsolutePath() + "'");
			}
		});
		
		
		JCheckBoxMenuItem editCubemaps = new JCheckBoxMenuItem("Edit Cubemaps");
		editCubemaps.setToolTipText("Edit cubemaps (only 'cubemapsize' is editable)");
		//editCubemaps.setEnabled(false);
		mapmenu.addSeparator();
		mapmenu.add(editCubemaps);
		
		editCubemaps.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				if(map == null)
					return;
				
				if(!(map instanceof SourceBSPFile)) {
					JOptionPane.showMessageDialog(frame, "Unsupported version of BSP. This option works only for Source BSP.");
					return;
				}
				SourceBSPFile bspmap = (SourceBSPFile)map;
				try {
					if(editCubemaps.isSelected()) {
						bspmap.loadCubemaps();
					} else {
						int res = JOptionPane.showConfirmDialog(frame, "Unloading the cubemaps will discard the changes. Confirm?", "Unload Cubemaps", JOptionPane.YES_NO_OPTION);
						
						if(res != JOptionPane.YES_OPTION) {
							editCubemaps.setSelected(true);
							return;
						}
						bspmap.unloadCubemaps();
					}
				} catch (IOException e) {
					e.printStackTrace();
					editCubemaps.setSelected(!editCubemaps.isSelected());
				}
				
				updateEntList(map.entities);
			}
		});
		
		JCheckBoxMenuItem editStaticProps = new JCheckBoxMenuItem("Edit Static props");
		editStaticProps.setToolTipText("Edit static props");
		//editStaticProps.setEnabled(false);
		mapmenu.add(editStaticProps);
		
		editStaticProps.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				if(map == null)
					return;
				
				if(!(map instanceof SourceBSPFile)) {
					JOptionPane.showMessageDialog(frame, "Unsupported version of BSP. This option works only for Source BSP.");
					return;
				}
				SourceBSPFile bspmap = (SourceBSPFile)map;
				try {
					if(editStaticProps.isSelected() && !bspmap.isSpropLumpLoaded()) {
						bspmap.loadStaticProps();
					} else {
						int res = JOptionPane.showConfirmDialog(frame, "Unloading the static props will discard the changes. Confirm?", "Unload SpropLump", JOptionPane.YES_NO_OPTION);
						
						if(res != JOptionPane.YES_OPTION) {
							editStaticProps.setSelected(true);
							return;
						}
						
						bspmap.unloadStaticProps();
					}
				} catch(Exception e) {
					JOptionPane.showMessageDialog(frame, "Could not load Static props!\n" + e.getMessage(), "ERROR!", JOptionPane.ERROR_MESSAGE);
					e.printStackTrace();
					editStaticProps.setSelected(!editStaticProps.isSelected());
				}
				
				updateEntList(map.entities);
			}
		});
		
		JCheckBoxMenuItem renameMap = new JCheckBoxMenuItem("Rename map");
		renameMap.setToolTipText("Rename the internal files and materials. Takes effect on save.");
		//renameMap.setEnabled(false);
		mapmenu.addSeparator();
		mapmenu.add(renameMap);
		
		renameMap.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				if(!(map instanceof SourceBSPFile)) {
					return;
				}
				
				SourceBSPFile bspmap = (SourceBSPFile)map;
				
				if(renameMap.isSelected()) {
					JPanel panel = new JPanel(new GridLayout(0, 1));
					JTextField tfield = new JTextField();
					panel.add(new JLabel("Original map name: " + bspmap.getOriginalName()));
					panel.add(new JLabel("Enter new map name:"));
					panel.add(tfield);
			        
			        int result = JOptionPane.showConfirmDialog(frame, panel, entspyTitle + " - Rename map", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
			        if(result == JOptionPane.OK_OPTION) {
			        	bspmap.changeMapName(tfield.getText().trim());
			        } else
			        	renameMap.setSelected(false);
				} else
					bspmap.changeMapName(null);
			}
		});
		
		JMenuItem editMaterials = new JMenuItem("Edit Materials");
		//editMaterials.setEnabled(false);
		mapmenu.addSeparator();
		mapmenu.add(editMaterials);
		
		editMaterials.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(!(map instanceof SourceBSPFile))
					return;
				
				SourceBSPFile bspmap = (SourceBSPFile)map;
				
				JDialog subframe = new JDialog(frame, "Edit Materials");
				
				JMenuBar submenu = new JMenuBar();
				subframe.setJMenuBar(submenu);
				
				JMenu filesub = new JMenu("File");
				submenu.add(filesub);
				
				JMenuItem copyMats = new JMenuItem("Copy to clipboard");
				copyMats.setToolTipText("Copy all materials to clipboard");
				filesub.add(copyMats);
				
				copyMats.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						StringBuilder matStr = new StringBuilder();
						for(String s : bspmap.materials) {
							matStr.append(s).append("\n");
						}
						
						Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
						cb.setContents(new StringSelection(matStr.toString()), null);
					}
				});
				
				JMenuItem pasteMats = new JMenuItem("Paste from clipboard");
				pasteMats.setToolTipText("Paste all materials from clipboard");
				filesub.add(pasteMats);
				
				MaterialTableModel model = new MaterialTableModel(bspmap.materials);
				JTable matTable = new JTable(model);
				matTable.getTableHeader().setReorderingAllowed(false);
				matTable.getColumnModel().getColumn(0).setMaxWidth(50);
				
				subframe.getContentPane().add(new JScrollPane(matTable));
				
				subframe.addWindowListener(new WindowAdapter() {
					public void windowClosing(WindowEvent we) {
						if(matTable.isEditing()) {
							matTable.getCellEditor().stopCellEditing();
						}
					}
					
					public void windowClosed(WindowEvent we) {
						
					}
				});
				
				pasteMats.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
						Transferable cbcontent = cb.getContents(null);

						if (cbcontent == null)
							return;

						try {
							StringReader sr = new StringReader(cbcontent.getTransferData(DataFlavor.stringFlavor).toString());
							BufferedReader br = new BufferedReader(sr);
							
							ArrayList<String> newMats = new ArrayList<String>();
							String line;
							while((line = br.readLine()) != null)
								newMats.add(line);
							
							for(int i = 0; i < Math.min(newMats.size(), bspmap.materials.size()); ++i) {
								bspmap.materials.set(i, newMats.get(i));
							}
							
							model.setMaterials(bspmap.materials);
							
							sr.close();
						} catch (UnsupportedFlavorException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				});
				
				subframe.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
				subframe.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				subframe.setSize(400, 520);
				subframe.setLocation(frame.getLocation());
				subframe.setVisible(true);
			}
		});
		
		JMenu secretMenu = new JMenu("Secret");
		JCheckBoxMenuItem randomizeMaterials = new JCheckBoxMenuItem("__randomize_materials__");
		//randomizeMaterials.setEnabled(false);
		randomizeMaterials.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				if(!(map instanceof SourceBSPFile)) {
					return;
				}
				
				SourceBSPFile bspmap = (SourceBSPFile)map;
				
				bspmap.randomMats = randomizeMaterials.isSelected();
			}
		});
		secretMenu.add(randomizeMaterials);

		JMenuBar menubar = new JMenuBar();
		menubar.add(filemenu);
		menubar.add(editmenu);
		menubar.add(entitymenu);
		menubar.add(mapmenu);
		menubar.add(optionmenu);
		menubar.add(helpmenu);
		
		if(secret)
			menubar.add(secretMenu);
		
		BSPEntspy.frame.setJMenuBar(menubar);

		mload.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if (BSPEntspy.this.checkChanged("Load BSP")) {
					return;
				}
				if (!BSPEntspy.this.loadFile(null)) {
					return;
				}
			}
		});
		msave.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent ev) {
				saveFile(true);
			}

		});
		
		munload.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent ev) {
				try {
					unloadFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		});

		msaveas.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent ev) {
				saveFile(false);
			}

		});

		mpatchvmf.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if(map == null)
					return;
				
				JFileChooser chooser = new JFileChooser(
						preferences.get("LastVMFFolder", System.getProperty("user.dir")));
				chooser.setDialogTitle(entspyTitle + " - Open FGD File");
				if (chooser.showOpenDialog(frame) == 1)
					return;

				File f = chooser.getSelectedFile();
				try {
					patchFromVMF(f);
				} catch (FileNotFoundException | LexerException e1) {
					JOptionPane.showMessageDialog(frame, "Error while loading VMF: " + e1.getMessage(), "ERROR!",
							JOptionPane.ERROR_MESSAGE);
					e1.printStackTrace();
				}

				preferences.put("LastVMFFolder", f.getParent());
				updateEntList(map.getEntities());
			}

		});

		mquit.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if (BSPEntspy.this.checkChanged("Quit Entspy")) {
					return;
				}
				frame.dispose();
			}
		});

		mloadfgd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(!mloadfgd.isSelected() && fgdFile != null) {
					String filename = "unknown.fgd";
					
					if(fgdFile.loadedFgds.size() > 0)
						filename = fgdFile.loadedFgds.get(0);
					
					int result2 = JOptionPane.showConfirmDialog(frame, "Unload '" + filename + "' FGD file?");

					if (result2 == JOptionPane.NO_OPTION) {
						mloadfgd.setSelected(fgdFile != null);
						return;
					}
					preferences.remove("LastFGDFile");
					fgdFile = null;
					
					return;
				}
				
				fgdFile = null;

				JFileChooser chooser = new JFileChooser(preferences.get("LastFGDDir", System.getProperty("user.dir")));
				chooser.setDialogTitle(entspyTitle + " - Open FGD File");
				if (chooser.showOpenDialog(frame) == 1)
					return;

				File f = chooser.getSelectedFile();
				if (loadFGDFiles(f)) {
					preferences.put("LastFGDFile", f.toString());
					preferences.put("LastFGDDir", f.getAbsolutePath());
					JOptionPane.showMessageDialog(frame,
							f.getName() + " successfuly loaded. It will load automaticaly on program start.");
				} else {
					preferences.remove("LastFGDFile");
					rightEntPanel.setSmartEdit(false);
					msmartEditOption.setState(false);
				}

				msmartEditOption.setEnabled(fgdFile != null);
				maddDefaultOption.setEnabled(fgdFile != null);
				
				mloadfgd.setSelected(fgdFile != null);

				rightEntPanel.setFGD(fgdFile);
			}
		});

		JPanel findpanel = new JPanel();

		final JLabel findlabel = new JLabel("Linked from ");
		findpanel.add(findlabel);
		final DefaultComboBoxModel<Entity> findmodel = new DefaultComboBoxModel<Entity>();
		final JComboBox<Entity> findcombo = new JComboBox<Entity>(findmodel);
		findcombo.setPreferredSize(new Dimension(200, findcombo.getPreferredSize().height));
		Font cfont = findcombo.getFont();
		findcombo.setFont(new Font(cfont.getName(), 0, cfont.getSize() - 1));
		findcombo.setToolTipText("Entity that links to this entity");
		findpanel.add(findcombo);
		final JButton findbutton = new JButton("Go to");
		findbutton.setToolTipText("Go to linking entity");
		findpanel.add(findbutton);
		findbutton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(map == null)
					return;
				
				Entity jump = ((Entity) findmodel.getSelectedItem());

				if (jump == null)
					return;

				int ind = entModel.indexOf(map.entities.indexOf(jump));

				if (entModel.indexOf(ind) > -1) {
					entList.setSelectedIndex(ind);
					entList.ensureIndexIsVisible(ind);
				} else {
					rightEntPanel.apply();
					rightEntPanel.setEntity(jump);
				}
			}
		});
		findlabel.setEnabled(false);
		findbutton.setEnabled(false);
		findcombo.setEnabled(false);

		JPanel leftPanel = new JPanel(new BorderLayout());
		leftPanel.add((Component) new JScrollPane(this.entList), "Center");
		JPanel entcpl = new JPanel();
		entcpl.setLayout(new BoxLayout(entcpl, BoxLayout.PAGE_AXIS));

		JPanel entbut = new JPanel();

		JButton updent = new JButton("Update");
		updent.setToolTipText("Update entity links");
		JButton addent = new JButton("Add");
		addent.setToolTipText("Add a new entity");
		final JButton cpyent = new JButton("Duplicate");
		cpyent.setToolTipText("Duplicate the selected entities");
		cpyent.setEnabled(false);
		final JButton delent = new JButton("Del");
		delent.setToolTipText("Delete the selected entities");
		//delent.setEnabled(false);
		entbut.add(updent);
		entbut.add(addent);
		entbut.add(cpyent);
		entbut.add(delent);

		JPanel entexp = new JPanel();

		exportEntity.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				if(map == null)
					return;
				
				ArrayList<Entity> ents = getSelectedEntities();

				JFileChooser chooser = new JFileChooser(preferences.get("LastFolder", System.getProperty("user.dir")));
				chooser.setDialogTitle(entspyTitle + " - Export entities to a file");
				chooser.setDialogType(JFileChooser.SAVE_DIALOG);

				int result = chooser.showOpenDialog(frame);
				if (result == 1) {
					return;
				}

				File f = chooser.getSelectedFile();

				String ext = ".ent";
				if (f.getName().indexOf('.') > -1)
					ext = "";

				try (FileWriter fw = new FileWriter(f + ext)) {
					writeEntsToWriter(ents, fw);

					fw.close();

					preferences.put("LastFolder", f.getParent());
					JOptionPane.showMessageDialog(frame, ents.size() + " entities successfuly exported to " + f, "Info",
							JOptionPane.INFORMATION_MESSAGE);
				} catch (IOException e) {
					JOptionPane.showMessageDialog(frame, e.getMessage(), "ERROR!", JOptionPane.ERROR_MESSAGE);
					e.printStackTrace();
				}

				return;
			}
		});

		importEntity.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				if(map == null)
					return;
				
				JFileChooser chooser = new JFileChooser(preferences.get("LastFolder", System.getProperty("user.dir")));
				chooser.setDialogTitle(entspyTitle + " - Import entities from a file");
				chooser.setDialogType(JFileChooser.SAVE_DIALOG);

				int result = chooser.showOpenDialog(frame);
				if (result == 1) {
					return;
				}

				File f = chooser.getSelectedFile();

				try (FileReader fr = new FileReader(f)) {
					ArrayList<Entity> ents = loadEntsFromReader(fr);

					fr.close();

					CommandAddEntity command = new CommandAddEntity();
					for (Entity e : ents) {
						command.addEntity(e, map.entities.size());
						map.entities.add(e);
					}
					Undo.create();
					Undo.setTarget(map.entities);
					Undo.addCommand(command);
					Undo.finish();

					updateEntList(map.getEntities());
					preferences.put("LastFolder", f.getParent());

					JOptionPane.showMessageDialog(frame, ents.size() + " entities successfuly imported from " + f,
							"Info", JOptionPane.INFORMATION_MESSAGE);
				} catch (Exception | LexerException e) {
					JOptionPane.showMessageDialog(frame, e.getMessage(), "ERROR!", JOptionPane.ERROR_MESSAGE);
					e.printStackTrace();
				}

				return;
			}
		});

		final JButton cpToClipEnt = new JButton("Copy");
		cpToClipEnt.setToolTipText("Copy selected entities to clipboard");
		//cpToClipEnt.setEnabled(false);
		entexp.add(cpToClipEnt);

		final JButton pstFromClipEnt = new JButton("Paste");
		pstFromClipEnt.setToolTipText("Paste entities from clipboard");
		//pstFromClipEnt.setEnabled(true);
		entexp.add(pstFromClipEnt);

		cpToClipEnt.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				if(map == null)
					return;
				
				ArrayList<Entity> ents = getSelectedEntities();

				try (StringWriter sw = new StringWriter(ents.size() * 256)) {
					writeEntsToWriter(ents, sw);

					sw.close();

					Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
					cb.setContents(new StringSelection(sw.toString()), null);
				} catch (IOException e) {
					JOptionPane.showMessageDialog(frame, e.getMessage(), "ERROR!", JOptionPane.ERROR_MESSAGE);
					e.printStackTrace();
				}

				return;
			}
		});

		pstFromClipEnt.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				if(map == null)
					return;
				
				try {
					Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
					Transferable cbcontent = cb.getContents(null);

					if (cbcontent == null)
						return;

					StringReader sr = new StringReader(cbcontent.getTransferData(DataFlavor.stringFlavor).toString());
					ArrayList<Entity> ents = loadEntsFromReader(sr);

					sr.close();

					int i = entList.getMaxSelectionIndex();

					if (i > -1)
						i = Math.min(i + 1, entModel.getSize());
					else
						i = entModel.getSize();

					int[] selectedIndices = new int[ents.size()];
					int j = 0;
					CommandAddEntity command = new CommandAddEntity();
					int originalIndex = entModel.getIndexAt(i);

					if (originalIndex < 0)
						originalIndex = entModel.getSize();

					for (Entity e : ents) {
						command.addEntity(e, originalIndex);
						map.entities.add(originalIndex, e);
						selectedIndices[j++] = originalIndex++;
					}
					Undo.create();
					Undo.setTarget(map.entities);
					Undo.addCommand(command);
					Undo.finish();

					updateEntList(map.getEntities());

					entList.setSelectedIndices(selectedIndices);
				} catch (Exception | LexerException e) {
					JOptionPane.showMessageDialog(frame, "Could not parse data from clipboard!\n" + e.getMessage(),
							"ERROR!", JOptionPane.ERROR_MESSAGE);
					e.printStackTrace();
				}

				return;
			}
		});

		JButton findent = new JButton("Find");
		findent.setToolTipText("Find and select next entity, hold Shift to select all");
		findent.setMnemonic(KeyEvent.VK_F);
		JButton filterEnt = new JButton("Filter");
		filterEnt.setToolTipText("Filter the entitiy list, hold Shift to clear the filter");
		JTextField findtext = new JTextField();
		findtext.setToolTipText("Text to search for");
		Box fbox = Box.createHorizontalBox();

		fbox.add(findtext);
		fbox.add(findent);
		fbox.add(filterEnt);

		entcpl.add((Component) fbox);
		entcpl.add((Component) entbut);
		entcpl.add((Component) entexp);

		leftPanel.add((Component) entcpl, "South");
		findent.addActionListener(new GotoListen(findtext));
		findtext.addActionListener(new FilterListen(findtext));
		filterEnt.addActionListener(new FilterListen(findtext));
		delent.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				CommandRemoveEntity command = new CommandRemoveEntity();

				int[] selected = entList.getSelectedIndices();
				for (int i = 0; i < selected.length; ++i) {
					int index = entModel.getIndexAt(selected[i]);
					command.addEntity(map.entities.get(index), index);
					selected[i] = index;
				}

				for (int i = selected.length - 1; i >= 0; --i) {
					if(!map.entities.get(selected[i]).canBeRemoved())
						continue;
					
					map.entities.remove(selected[i]);
				}

				Undo.create();
				Undo.setTarget(map.entities);
				Undo.addCommand(command);
				Undo.finish();

				int j = entList.getMaxSelectionIndex() - selected.length;
				updateEntList(map.getEntities());

				entList.setSelectedIndex(j + 1);

				map.entDirty = true;
			}
		});
		cpyent.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				int[] selected = entList.getSelectedIndices();
				int[] transIndices = entModel.translateIndices(selected);

				CommandAddEntity command = new CommandAddEntity();
				for (int j = 0; j < selected.length; ++j) {
					selected[j] += j;
					transIndices[j] += j;
					Entity old = map.entities.get(transIndices[j]);
					++selected[j];
					++transIndices[j];

					if (old != null) {
						Entity newE = old.copy();
						command.addEntity(newE, transIndices[j]);
						map.entities.add(transIndices[j], newE);
					}
				}
				Undo.create();
				Undo.setTarget(map.entities);
				Undo.addCommand(command);
				Undo.finish();

				updateEntList(map.getEntities());
				entList.setSelectedIndices(selected);

				BSPEntspy.this.map.entDirty = true;
			}
		});
		addent.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if(map == null)
					return;
				
				Entity newent = new Entity();
				int index = 0;

				CommandAddEntity command;
				if (entList.getMaxSelectionIndex() > 0) {
					index = entList.getMaxSelectionIndex() + 1;
					command = new CommandAddEntity(newent, index);
					map.entities.add(index, newent);
				} else {
					index = map.entities.size() - 1;
					command = new CommandAddEntity(newent, map.entities.size());
					map.entities.add(newent);
				}
				Undo.create();
				Undo.setTarget(map.entities);
				Undo.addCommand(command);
				Undo.finish();

				newent.autoedit = true;

				updateEntList(map.getEntities());
				entList.setSelectedIndex(index);
				entList.ensureIndexIsVisible(index);

			}
		});

		updent.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				if(map == null)
					return;
				
				map.updateLinks();
			}
		});

		this.entList.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent ev) {
				if (ev.getValueIsAdjusting())
					return;
				int[] selected = entList.getSelectedIndices();

				boolean enable = selected.length > 0;

				delent.setEnabled(enable);
				cpyent.setEnabled(enable);
				exportEntity.setEnabled(enable);
				cpToClipEnt.setEnabled(enable);
				// obfEntity.setEnabled(enable);
				findbutton.setEnabled(selected.length == 1);
				findcombo.setEnabled(selected.length == 1);
				findmodel.removeAllElements();

				/*
				 * Emulates hammer editor behaviour. If a new selection is made that does not
				 * contain the previously selected entities, then the changes are applied
				 * automatically
				 */
				HashSet<Entity> newSelection = new HashSet<Entity>();
				boolean shouldApply = selected.length > 0 && previouslySelected.size() > 0;
				for (int i : selected) {
					Entity e = entModel.getElementAt(i);
					newSelection.add(e);

					if (previouslySelected.contains(e)) {
						shouldApply = false;
					}
				}
				previouslySelected = newSelection;
				if (shouldApply) {
					rightEntPanel.apply();
				}

				rightEntPanel.clearEntities();
				for (int i : selected) {
					rightEntPanel.addEntity(entModel.getElementAt(i), false);
				}
				rightEntPanel.gatherKeyValues();

				if (selected.length == 1) {
					setFindList(entModel.getElementAt(selected[0]), findmodel);
				}
			}

		});

		rightEntPanel.addApplyListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int[] selected = entList.getSelectedIndices();
				updateEntList(map.getEntities());
				entList.setSelectedIndices(selected);
			}
		});

		rightEntPanel.addGotoListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(map == null)
					return;
				
				GotoEvent ge = (GotoEvent) e;
				String name = ge.entname;

				boolean found = false;
				int j = 0;
				for (int i = 0; i < 2 && !found; ++i) {
					for (; j < map.entities.size(); ++j) {
						if (map.entities.get(j).targetname.equals(name)) {
							entList.setSelectedIndex(j);
							found = true;
							break;
						}
					}
					j = entList.getSelectedIndex() + 1;
				}

				if (!found) {
					JOptionPane.showMessageDialog(frame, "Couldn't find entity named '" + name + "'.");
				}
			}
		});

		JPanel rightPanel = new JPanel(new BorderLayout());
		rightPanel.add((Component) findpanel, "South");
		rightPanel.add((Component) rightEntPanel, "Center");

		JSplitPane mainSplit = new JSplitPane(1, leftPanel, rightPanel);
		mainSplit.setDividerLocation(275);
		
		onMapLoadInternal.add(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				msave.setEnabled(true);
				msaveas.setEnabled(true);
				mpatchvmf.setEnabled(true);
				importEntity.setEnabled(true);
				munload.setEnabled(true);
				
				updent.setEnabled(true);
				addent.setEnabled(true);
				pstFromClipEnt.setEnabled(true);
				
				removeLightInfo.setSelected(false);
				removePak.setSelected(false);
				
				boolean enable = map != null && map instanceof SourceBSPFile;
				
				removeLightInfo.setEnabled(enable);
				exportPak.setEnabled(enable);
				importPak.setEnabled(enable);
				removePak.setEnabled(enable);
				lightmapBrowser.setEnabled(enable);
				editCubemaps.setEnabled(enable);
				editStaticProps.setEnabled(enable);
				
				editCubemaps.setSelected(false);
				editStaticProps.setSelected(false);
				renameMap.setEnabled(enable);
				randomizeMaterials.setEnabled(enable);
				computeChecksum.setEnabled(enable);
				
				editMaterials.setEnabled(enable);
			}
		});
		
		onMapUnloadInternal.add(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				msave.setEnabled(false);
				msaveas.setEnabled(false);
				mpatchvmf.setEnabled(false);
				importEntity.setEnabled(false);
				munload.setEnabled(false);
				
				removeLightInfo.setSelected(false);
				removePak.setSelected(false);
				
				mUndo.setEnabled(false);
				mRedo.setEnabled(false);
				editMaterials.setEnabled(false);
				
				delent.setEnabled(false);
				cpyent.setEnabled(false);
				exportEntity.setEnabled(false);
				cpToClipEnt.setEnabled(false);
				addent.setEnabled(false);
				updent.setEnabled(false);
				pstFromClipEnt.setEnabled(false);
				
				rightEntPanel.clearEntities();
				setFindList(null, findmodel);
				findcombo.setEnabled(false);
				findbutton.setEnabled(false);
				
				boolean enable = false;
				
				computeChecksum.setEnabled(false);
				removeLightInfo.setEnabled(enable);
				lightmapBrowser.setEnabled(enable);
				exportPak.setEnabled(enable);
				importPak.setSelected(false);
				importPak.setEnabled(enable);
				removePak.setEnabled(enable);
				removePak.setSelected(false);
				editCubemaps.setEnabled(enable);
				editStaticProps.setEnabled(enable);
				renameMap.setEnabled(enable);
				renameMap.setSelected(false);
				
				editCubemaps.setSelected(false);
				editStaticProps.setSelected(false);
				randomizeMaterials.setEnabled(enable);
			}
		});
		
		onMapSaveInternal.add(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				if(map instanceof SourceBSPFile) {
					SourceBSPFile bspmap = (SourceBSPFile)map;
					removePak.setSelected(!bspmap.writePak);
					importPak.setSelected(bspmap.embeddedPak != null);
					renameMap.setSelected(bspmap.newMapName != null);
					removePak.setEnabled(bspmap.embeddedPak == null);
					randomizeMaterials.setSelected(bspmap.randomMats);
				}
			}
		});
		
		for(ActionListener al : onMapUnloadInternal)
			al.actionPerformed(new ActionEvent(this, 0, "mapunload"));
		
		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent ev) {
				if (BSPEntspy.this.checkChanged("Quit Entspy :)")) {
					return;
				}
				frame.dispose();
				if (map != null) {
					try {
						map.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		});
		frame.setSize(720, 520);
		frame.getContentPane().add(mainSplit);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		
		frame.setDropTarget(new DropTarget() {
			private static final long serialVersionUID = 1L;

			public synchronized void drop(DropTargetDropEvent evt) {
				try {
					evt.acceptDrop(DnDConstants.ACTION_COPY);
					List<File> droppedFiles = (List<File>) evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
					
					if(droppedFiles.size() > 0 && !BSPEntspy.this.checkChanged("Load BSP")) {
						BSPEntspy.this.loadFile(droppedFiles.get(0));
					}
					
					evt.dropComplete(true);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});

		return 0;
	}

	public boolean setFindList(Entity sel, DefaultComboBoxModel<Entity> model) {
		model.removeAllElements();
		
		if(sel == null)
			return false;

		List<Entity> ents = map.getLinkedEntities(sel);

		if (ents == null)
			return false;

		for (Entity e : ents)
			model.addElement(e);

		return true;
	}

	public boolean loadFile(File f) {
		if(f == null) {
			JFileChooser chooser = new JFileChooser(preferences.get("LastFolder", System.getProperty("user.dir")));
	
			chooser.setDialogTitle(entspyTitle + " - Open a BSP file");
			chooser.setFileFilter(new EntFileFilter());
			int result = chooser.showOpenDialog(BSPEntspy.frame);
			if (result == JFileChooser.CANCEL_OPTION) {
				return false;
			}
			f = chooser.getSelectedFile();
			
			chooser = null;
		}
		
		this.infile = f;
		
		this.filename = this.infile.getName();
		if (!(this.infile.exists() && this.infile.canRead())) {
			System.out.println("Can't read " + this.filename + "!");
			return false;
		}
		System.out.println("Reading map file " + this.filename);

		preferences.put("LastFolder", this.infile.getParent());
		
		try {
			readFile();
		} catch (IOException e1) {
			e1.printStackTrace();
			return false;
		}

		return true;
	}
	
	private void unloadFile() throws IOException {
		if(checkChanged("Unload BSP"))
			return;
		if(map != null)
			map.close();
		map = null;
		entModel.setEntityList(new ArrayList<Entity>());
		frame.setTitle(entspyTitle);
		
		for(ActionListener al : onMapUnloadInternal)
			al.actionPerformed(new ActionEvent(this, 0, "mapunload"));
	}

	private boolean saveFile(boolean overwrite) {
		if(map == null)
			return false;
		
		File out;

		if (overwrite) {
			out = this.infile;
		} else {
			JFileChooser chooser = new JFileChooser(preferences.get("LastFolder", System.getProperty("user.dir")));
			chooser.setFileFilter(new FileNameExtensionFilter("Binary Space Partitioned maps (.bsp)", "bsp"));
			chooser.setDialogTitle(entspyTitle + " - Save a BSP file");

			int result = chooser.showOpenDialog(BSPEntspy.frame);
			if (result == JFileChooser.CANCEL_OPTION) {
				return false;
			}
			out = chooser.getSelectedFile();
			String outName = out.getName();
			int extIndex = outName.lastIndexOf('.');
			
			if(extIndex < 1 || !outName.substring(extIndex).toLowerCase().equals(".bsp")) {
				out = new File(out.getPath().concat(".bsp"));
			}
		}

		if (out.exists()) {
			int result2 = JOptionPane.showConfirmDialog(frame, "File " + out.getName() + " exists. Override?");

			if (result2 != JOptionPane.YES_OPTION)
				return false;
		}
		
		if(map instanceof SourceBSPFile) {
			SourceBSPFile bspmap = (SourceBSPFile)map;
			
			bspmap.forcePreserveChecksum = false;
			if(preferences.getBoolean("preserveChecksum", false)) {
				int result = JOptionPane.showConfirmDialog(frame, 
						"Do you want to preserve the map checksum?\n"+
						"Proceeding with this option will modify ONLY the Entity lump and the storage may not be optimised."
						, entspyTitle, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
				bspmap.forcePreserveChecksum = result == JOptionPane.YES_OPTION;
			}
			
			if(bspmap.isSpropLumpLoaded()) {
				final String[] versions = {"v4", "v5", "v6"};
				JPanel panel = new JPanel(new GridLayout(0, 1));
				JComboBox<String> combo = new JComboBox<String>(versions);
				combo.setSelectedIndex(2);
				panel.add(new JLabel("Detected Static prop lump loaded (version: " + bspmap.getStaticPropVersion() + ")."));
				panel.add(new JLabel("Please select the version to save it as:"));
				panel.add(combo);
		        
		        int result = JOptionPane.showConfirmDialog(frame, panel, entspyTitle, JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
		        if(result == JOptionPane.OK_OPTION) {
		        	bspmap.setStaticPropVersion((String)combo.getSelectedItem());
		        }
			}
		}
		
		RandomAccessFile output = null;
		File outTemp = out;
		boolean swap = false;
		try {
			if(out.equals(infile)) {
				outTemp = new File(out.getPath() + ".temp");
				swap = true;
			}
			
			output = new RandomAccessFile(outTemp, "rw");
			this.map.save(output, true);
			output.close();
			
			if(swap) {
				map.close();
				
				if(!out.delete() || !outTemp.renameTo(out)) {
					out = outTemp;
					throw new IOException("Could not replace the original BSP file ('" + out.getPath() + "') with new file ('" + outTemp.getPath() + "')!");
				}
			}
		} catch (IOException e) {
			JOptionPane.showMessageDialog(frame, "Error while saving the file!\n" + e.getMessage() + "\nMake sure the file is not open in other software.", "ERROR",
					JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
			
			if(output != null) {
				try {
					output.close();
				} catch (IOException e1) {
				}
			}
			
			return false;
		} finally {
			try {
				infile = out;
				map.close();
				map.bspfile = new RandomAccessFile(out, "rw");
				this.filename = infile.getName();
				frame.setTitle(entspyTitle + " - " + this.filename);
			} catch(IOException e) {
				JOptionPane.showMessageDialog(frame, "Error while re-opening the file!\n" + e.getMessage(), "ERROR",
						JOptionPane.ERROR_MESSAGE);
				loadFile(null);
			}
		}
		
		for(ActionListener al : onMapSaveInternal)
			al.actionPerformed(new ActionEvent(this, 0, "mapsave"));
		
		return true;
	}

	public boolean loadFGDFiles(File file) {
		if (file == null) {
			String lastFgd = preferences.get("LastFGDFile", null);
			if (lastFgd == null)
				return false;

			file = new File(lastFgd);
		}

		if (!file.exists() || !file.canRead())
			return false;

		if (fgdFile == null)
			fgdFile = new FGD();

		try (FileReader fr = new FileReader(file)) {
			final String path = file.getParent();

			FGD.OnIncludeCallback callback = new FGD.OnIncludeCallback() {
				public Boolean call() {
					File f = new File(path + "/" + this.fileToLoad);
					return loadFGDFiles(f);
				}
			};

			fgdFile.loadFromReader(fr, file.getName(), callback);
			fr.close();
		} catch (Exception | LexerException e) {
			JOptionPane.showMessageDialog(frame, e.getMessage(), "ERROR!", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
			fgdFile = null;
			return false;
		}

		return true;
	}

	public boolean checkChanged(String title) {
		if (map == null || !this.map.entDirty && Undo.isEmpty()) {
			return false;
		}
		int result = JOptionPane.showConfirmDialog(frame,
				new Object[] { "File " + filename + " has been edited.", "Discard changes?" }, title, 0);
		if (result == 0) {
			return false;
		}
		return true;
	}

	public String strsize(int bytes) {
		if (bytes < 2000) {
			return "" + bytes + " bytes";
		}
		return new DecimalFormat("0.0").format((float) bytes / 1024.0f) + " kbytes";
	}

	public Entity getSelectedEntity() {
		int index = entList.getSelectedIndex();

		if (index < 0 || index >= map.entities.size())
			return null;

		return map.entities.get(index);
	}

	public ArrayList<Entity> getSelectedEntities() {
		ArrayList<Entity> ents = new ArrayList<Entity>();

		for (int i : entList.getSelectedIndices()) {
			ents.add(entModel.getElementAt(i));
		}

		return ents;
	}

	public boolean patchFromVMF(File vmfFile) throws LexerException, FileNotFoundException {
		if(map == null)
			return false;
		
		VMF temp = new VMF();

		FileReader fr = new FileReader(vmfFile);
		temp.loadFromReader(fr, vmfFile.getName());

		Object[] options = new Object[] { "Only named entities", "All entities" };
		Object[] options2 = new Object[] { "Continue", "Continue all", "Abort" };

		int selected = JOptionPane.showOptionDialog(frame,
				"Patch only named entities (safe) or try to patch entities based on order?", "Patch VMF",
				JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
		boolean ignoreDisorder = false;

		int replaced = 0;
		int missmatches = 0;
		CommandReplaceEntity command = new CommandReplaceEntity();
		if (selected == 0) {
			HashMap<String, Integer> nameMap = new HashMap<String, Integer>();
			HashMap<String, Integer> dupMap = new HashMap<String, Integer>();

			for (int i = 0; i < map.entities.size(); ++i) {
				Entity ent = map.entities.get(i);
				if (ent.targetname.isEmpty())
					continue;
				String key = "\"" + ent.classname + "\" \"" + ent.targetname + "\"";

				if (nameMap.containsKey(key)) {
					int count = dupMap.getOrDefault(key, 1);
					dupMap.put(key, count + 1);
					key += count;
				}

				nameMap.put(key, i);
			}

			dupMap.clear();
			for (int i = 0; i < temp.ents.size(); ++i) {
				Entity ent = temp.ents.get(i);
				if (ent.targetname.isEmpty())
					continue;
				String key = "\"" + ent.classname + "\" \"" + ent.targetname + "\"";

				int count = dupMap.getOrDefault(key, -1);
				dupMap.put(key, ++count);

				if (count > 0) {
					key += count;
				}

				if (!nameMap.containsKey(key)) {
					continue;
				}

				int index = nameMap.get(key);
				Entity original = map.entities.get(index);
				Entity finalReplacement = replaceEntity(original, ent);
				map.entities.set(index, finalReplacement);
				command.addEntity(original, finalReplacement, index);
				++replaced;
			}
		} else {
			for (int i = 0, j = 0; i < map.entities.size() && j < temp.ents.size();) {
				Entity original = map.entities.get(i);
				Entity replacement = temp.ents.get(j);

				if (VMF.ignoredClasses.contains(original.classname)) {
					++i;
					continue;
				}
				if (VMF.ignoredClasses.contains(replacement.classname)
						|| selected == 0 && replacement.targetname.isEmpty()) {
					++j;
					continue;
				}

				// only classname needs to match
				boolean shouldReplace = original.classname.equals(replacement.classname);

				if (!shouldReplace)
					++missmatches;

				if (selected == 1 && !(ignoreDisorder || shouldReplace)) {
					int selected2 = JOptionPane.showOptionDialog(frame,
							"Entity mismatch detected (" + original.classname + "[\"" + original.targetname + "\"], "
									+ replacement.classname + "[\"" + replacement.targetname + "\"])!\n"
									+ "The program will try to match entities as best as possible. Continue?",
							"Patch VMF", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options2,
							options2[1]);
					ignoreDisorder = selected2 == 1;

					if (selected2 == 2)
						break;
				}

				if (shouldReplace) {
					Entity finalReplacement = replaceEntity(original, replacement);
					map.entities.set(i, finalReplacement);
					command.addEntity(original, finalReplacement, i);

					++i;
					++j;
					++replaced;
				} else // if entities do not match advance bsp entities until they match with vmf
						// entities
					++i;
			}
		}

		Undo.create();
		Undo.setTarget(map.entities);
		Undo.addCommand(command);
		Undo.finish();

		JOptionPane.showMessageDialog(frame, "Patched " + replaced + " entities (" + missmatches + " mismatches).");

		return false;
	}

	private static Entity replaceEntity(Entity original, Entity replacement) {
		for (KeyValue kvl : original.keyvalues) {
			if (!replacement.hasKeyValue(kvl.key)) {
				replacement.addKeyVal(kvl.key, kvl.value);
			}

			if (kvl.key.equals("model") && kvl.value.startsWith("*"))
				replacement.setKeyVal("model", kvl.value);
		}
		return replacement;
	}

	public ArrayList<Entity> loadEntsFromReader(Reader in) throws Exception, LexerException {
		VMF temp = new VMF();

		temp.loadFromReader(in, "clipboard");

		for (int i = 0; i < temp.ents.size(); ++i) {
			if (VMF.ignoredClasses.contains(temp.ents.get(i).classname) && !temp.ents.get(i).classname.equals("env_cubemap")) {
				temp.ents.remove(i--);
			}
		}

		return temp.ents;
	}

	public void writeEntsToWriter(List<Entity> ents, Writer out) throws IOException {
		BufferedWriter w = new BufferedWriter(out);

		for (Entity e : ents) {
			w.append(e.toStringSpecial());
		}

		w.flush();
	}

	public static void main(String[] args) throws Exception {
		String help = "Options:\n-rename <oldmapname or \"\" to deduce> <new map name> <path to materials directory>\tRenames directories and updates VMT files in specified directory.\n-help\tDisplays help.";
		
		boolean runGui = true;
		boolean failed = false;
		boolean secret = false; //TODO
		for(int i = 0; i < args.length && !failed; ++i) {
			if(args[i].equals("-rename")) {
				if(i + 3 < args.length) {
					String orgName = args[i + 1];
					String newName = args[i + 2];
					String path = args[i + 3];
					runGui = false;
					
					System.out.println(orgName);
					
					commandRename(orgName, newName, path);
				} else {
					System.out.println("Invalid -rename format!");
					failed = true;
				}
				i += 3;
			} else if(args[i].equals("-help")) {
				System.out.println(help);
			} else if(args[i].equals("-secret2131")){
				secret = true;
			}else {
				failed = true;
			}
		}
		
		if(failed) {
			System.out.println("Unknown command!\n" + help);
		}
		
		if(runGui && !failed) {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			BSPEntspy inst = new BSPEntspy();
			inst.exec(secret);
		}
	}
	
	private static void commandRename(String orgName, String newName, String path) {
		System.out.println("DEPRECATED!!! Use 'Rename map' functionality instead!");
		
		Path dirPath = Paths.get(path);
		Path parent = dirPath.getParent();

		File dir = dirPath.toFile();
		
		if(!dir.exists() || !dir.isDirectory()) {
			System.out.println("ERROR! Specified path is not a directory or does not exist!");
			return;
		}
		
		if(!dir.getName().toLowerCase().equals("materials")) {
			System.out.println("ERROR! Due to safety the specified directory must be 'materials', got '" + dir.getName() + "' instead!");
			return;
		}
		
		if(newName.indexOf('\\') >= 0 || newName.indexOf('/') >= 0 || newName.indexOf('.') >= 0) {
			System.out.println("ERROR! Specified new name is not valid!");
			return;
		}
		
		ArrayList<Path> allFiles = new ArrayList<Path>(128);
		ArrayList<Path> allDirs = new ArrayList<Path>(128);
		File mapDir = new File(path + "/maps");
		Path mapDirPath = parent.relativize(mapDir.toPath());
		
		if(!mapDir.exists() || !mapDir.isDirectory()) {
			System.out.println("ERROR! Specified directory does not contain 'maps' directory!");
			return;
		}
		
		searchForFiles(mapDir, allFiles, allDirs, parent);
		
		if(orgName == null || orgName.trim().isEmpty()) {
			for(Path p : allDirs) {
				if(p.getParent().equals(mapDirPath)) {
					orgName = p.getName(p.getNameCount() - 1).toString();
					break;
				}
			}
		}
			
		if(orgName == null) {
			System.out.println("ERROR! Could not resolve original map name or no name given! (No directory in 'maps/')");
			return;
		}
		
		//HashSet<String> fileSet = new HashSet<String>();
		
		String orgNameSanitized = orgName.trim().toLowerCase();
		Pattern vmtReplacePattern = Pattern.compile("[\\\"']?\\$envmap[\\\"']?\\s*[\\s\\\"'][/\\\\]?(maps[/\\\\][\\w\\/\\d-]*)[\\n\\\"']?", Pattern.CASE_INSENSITIVE);
		for(Path p : allFiles) {
			String filename = p.getName(p.getNameCount() - 1).toString();
			
			if(filename.substring(filename.lastIndexOf('.')).toLowerCase().equals(".vmt")) {
				File f = parent.resolve(p).toFile();
				
				if(!f.exists()) {
					System.out.println("WARNING! Previously found file '" + p + "' does not exist!!!");
					continue;
				}
				
				byte[] block = new byte[4096];
				StringBuilder newContent = null;
				try(FileInputStream fis = new FileInputStream(f); ByteArrayOutputStream bos = new ByteArrayOutputStream();){
					int len;
					int totalLen = 0;
					
					while((len = fis.read(block)) > 0 && totalLen < 65536) {
						bos.write(block, 0, len);
						totalLen += len;
					}
					
					if(totalLen > 65536) {
						System.out.println("WARNING! File '" + p + "' too big! Skipping.");
						continue;
					}
					
					String content = new String(bos.toByteArray());
					Matcher match = vmtReplacePattern.matcher(content);
					
					newContent = new StringBuilder(content.length());
					
					boolean rep = false;
					boolean found = false;
					int beg = 0;
					while(match.find()) {
						String foundStr = match.group(1);
						String[] split = foundStr.split("[\\\\/]");
						
						newContent.append(content.substring(beg, match.start(1)));
						beg = match.end(1);
						rep = true;
						
						for(int i = 0; i < split.length - 1; ++i) {
							if(split[i].toLowerCase().equals(orgNameSanitized)) {
								found = true;
								split[i] = newName;
							}
							newContent.append(split[i]).append("/");
						}
						newContent.append(split[split.length - 1]);
					}
					
					if(!rep || !found) {
						continue;
					}
					
					newContent.append(content.substring(beg));
				} catch(IOException e) {
					e.printStackTrace();
				}
				
				if(newContent == null)
					continue;
				
				try(FileOutputStream fos = new FileOutputStream(f); PrintWriter pw = new PrintWriter(fos)){
					pw.append(newContent);
					pw.flush();
					
					System.out.println("INFO Modified file '" + p + "'");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private static void searchForFiles(File file, List<Path> files, List<Path> dirs, Path relativeTo) {
		if(relativeTo == null)
			relativeTo = file.toPath();
		
		for(File f : file.listFiles()) {
			if(f.isDirectory()) {
				dirs.add(relativeTo.relativize(f.toPath()));
				searchForFiles(f, files, dirs, relativeTo);
				continue;
			}
			
			files.add(relativeTo.relativize(f.toPath()));
		}
	}

	class FilterListen implements ActionListener {
		JTextField textf;

		public FilterListen(JTextField textf) {
			this.textf = textf;
		}

		public void actionPerformed(ActionEvent ae) {
			entList.clearSelection();
			String ftext = textf.getText().trim();
			if (ftext.equals("") || (ae.getModifiers() & ActionEvent.SHIFT_MASK) > 0) {
				entModel.setFilter(null);
				return;
			}
			try {
				entModel.setFilter(SimpleFilter.create(ftext));
			} catch (Exception e) {
				JOptionPane.showMessageDialog(frame, "Invalid filter format!", "ERROR", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	class GotoListen implements ActionListener {
		JTextField textf;

		public GotoListen(JTextField textf) {
			this.textf = textf;
		}

		public void actionPerformed(ActionEvent ae) {
			String ftext = textf.getText().trim();
			if (ftext.equals("")) {
				return;
			}

			IFilter filter;
			try {
				filter = SimpleFilter.create(ftext);
			} catch (Exception e) {
				JOptionPane.showMessageDialog(frame, "Invalid filter format!", "ERROR", JOptionPane.ERROR_MESSAGE);
				return;
			}

			if ((ae.getModifiers() & ActionEvent.SHIFT_MASK) > 0) {
				List<Entity> filtered = entModel.getFilteredEntities();

				for (int i = 0; i < filtered.size(); ++i) {
					if (filter.match(filtered.get(i))) {
						entList.addSelectionInterval(i, i);
					}
				}
			} else {
				int found = -1;
				int j = entList.getMaxSelectionIndex() + 1;
				int k = entModel.getSize();
				for (int i = 0; i < 2 && found < 0; ++i) {
					List<Entity> filtered = entModel.getFilteredEntities();

					for (; j < k; ++j) {
						if (filter.match(filtered.get(j))) {
							found = j;
							break;
						}
					}

					j = 0;
					k = entList.getMinSelectionIndex();
				}

				if (found > -1) {
					entList.setSelectedIndex(found);
					entList.ensureIndexIsVisible(found);
				}
			}
		}
	}

	class HelpActionListener implements ActionListener {
		String file;

		public HelpActionListener(String file) {
			this.file = file;
		}

		public void actionPerformed(ActionEvent ev) {
			HelpWindow help = HelpWindow.openHelp(BSPEntspy.frame, "Help");

			try (BufferedReader rd = new BufferedReader(
					new InputStreamReader(BSPEntspy.class.getResourceAsStream(file)))) {
				StringBuilder sb = new StringBuilder();

				String line = null;
				while ((line = rd.readLine()) != null) {
					sb.append(line);
				}
				
				if(file.endsWith("credits.html")) {
					sb.append("<br>").append(decrypt());
				}
				
				help.setText(sb.toString());
			} catch (IOException | NullPointerException e) {
				help.setText("Couldn't find " + file + "<br>" + e);
				e.printStackTrace();
			}

			help.setSize(720, 520);
			help.setVisible(true);
		}
	}

	private static abstract class CommandEntity implements Command {
		ArrayList<Entity> entities;

		public CommandEntity() {
			entities = new ArrayList<Entity>();
		}

		public CommandEntity(Entity e) {
			entities = new ArrayList<Entity>();
			entities.add(e);
		}

		public Command join(Command previous) {
			CommandEntity prev = (CommandEntity) previous;
			prev.entities.addAll(entities);

			return null;
		}

		public int size() {
			return entities.size();
		}
	}

	private static class CommandReplaceEntity extends CommandEntity {
		ArrayList<Entity> replacements;
		ArrayList<Integer> indices;

		public CommandReplaceEntity() {
			indices = new ArrayList<Integer>();
			replacements = new ArrayList<Entity>();
		}

		public void addEntity(Entity original, Entity replacement, int index) {
			indices.add(index);
			entities.add(original);
			replacements.add(replacement);
		}

		public void undo(Object target) {
			ListIterator<Integer> it = indices.listIterator();

			while (it.hasNext()) {
				int index = it.nextIndex();
				int entindex = it.next();
				((ArrayList<Entity>) target).set(entindex, entities.get(index));
			}
		}

		public void redo(Object target) {
			ListIterator<Integer> it = indices.listIterator();

			while (it.hasNext()) {
				int index = it.nextIndex();
				int entindex = it.next();
				((ArrayList<Entity>) target).set(entindex, replacements.get(index));
			}
		}

		public String toString(String indent) {
			return "";
		}
	}

	private static class CommandAddEntity extends CommandEntity {
		ArrayList<Integer> indices;

		public CommandAddEntity() {
			indices = new ArrayList<Integer>();
		}

		public CommandAddEntity(Entity e, int index) {
			indices = new ArrayList<Integer>();
			addEntity(e, index);
		}

		public void addEntity(Entity e, int index) {
			if(!e.canBeRemoved())
				return;
			entities.add(e);
			indices.add(index);
		}

		public void undo(Object target) {
			ListIterator<Integer> it = indices.listIterator();
			int offset = 0;

			while (it.hasNext()) {
				((ArrayList<Entity>) target).remove(it.next().intValue() - offset++);
			}
		}

		public void redo(Object target) {
			ListIterator<Entity> it = entities.listIterator();
			while (it.hasNext()) {
				((ArrayList<Entity>) target).add(indices.get(it.nextIndex()), it.next());
			}
		}

		public String toString(String indent) {
			StringBuilder sb = new StringBuilder();
			System.out.println("ident" + indent);

			sb.append(indent).append(this.getClass()).append("\n");

			for (int i = 0; i < entities.size(); ++i) {
				sb.append(indent).append("\t\t").append(entities.get(i).toString()).append(" at index ")
						.append(indices.get(i)).append("\n");
			}

			return sb.toString();
		}
	}

	private static class CommandRemoveEntity extends CommandAddEntity {
		public CommandRemoveEntity() {

		}

		public CommandRemoveEntity(Entity e, int index) {
			super(e, index);
		}

		public void undo(Object target) {
			ListIterator<Entity> it = entities.listIterator();

			while (it.hasNext()) {
				((ArrayList<Entity>) target).add(indices.get(it.nextIndex()), it.next());
			}
		}

		public void redo(Object target) {
			ListIterator<Integer> it = indices.listIterator();
			int offset = 0;

			while (it.hasNext()) {
				((ArrayList<Entity>) target).remove(it.next().intValue() - offset++);
			}
		}
	}

}
