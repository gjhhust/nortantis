package nortantis.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import javax.swing.BoxLayout;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.imgscalr.Scalr.Method;

import com.formdev.flatlaf.FlatDarkLaf;

import nortantis.DebugFlags;
import nortantis.ImageCache;
import nortantis.MapSettings;
import nortantis.editor.CenterEdit;
import nortantis.editor.DisplayQuality;
import nortantis.editor.EdgeEdit;
import nortantis.editor.ExportAction;
import nortantis.editor.MapUpdater;
import nortantis.editor.UserPreferences;
import nortantis.geom.Rectangle;
import nortantis.graph.voronoi.Center;
import nortantis.graph.voronoi.Edge;
import nortantis.platform.Image;
import nortantis.platform.PlatformFactory;
import nortantis.platform.awt.AwtFactory;
import nortantis.util.AssetsPath;
import nortantis.util.ILoggerTarget;
import nortantis.util.ImageHelper;
import nortantis.util.Logger;

@SuppressWarnings("serial")
public class MainWindow extends JFrame implements ILoggerTarget
{
	private JTextArea txtConsoleOutput;
	private Path openSettingsFilePath;
	private boolean forceSaveAs;
	MapSettings lastSettingsLoadedOrSaved;
	boolean hasDrawnCurrentMapAtLeastOnce;
	static final String frameTitleBase = "Nortantis";
	public MapEdits edits;
	public JMenuItem clearEditsMenuItem;

	JScrollPane mapEditingScrollPane;
	// Controls how large 100% zoom is, in pixels.
	final double oneHundredPercentMapWidth = 4096;
	public MapEditingPanel mapEditingPanel;
	JMenuItem undoButton;
	JMenuItem redoButton;
	private JMenuItem clearEntireMapButton;
	public Undoer undoer;
	double zoom;
	double displayQualityScale;
	ThemePanel themePanel;
	ToolsPanel toolsPanel;
	MapUpdater updater;
	private JCheckBoxMenuItem highlightLakesButton;
	private JCheckBoxMenuItem highlightRiversButton;
	private JScrollPane consoleOutputPane;
	double exportResolution;
	ExportAction defaultMapExportAction;
	ExportAction defaultHeightmapExportAction;
	String imageExportPath;
	double heightmapExportResolution;
	String heightmapExportPath;
	private JMenuItem saveMenuItem;
	private JMenuItem saveAsMenItem;
	private JMenuItem exportMapAsImageMenuItem;
	private JMenuItem exportHeightmapMenuItem;
	private JMenu editMenu;
	private JMenu viewMenu;
	private JMenu recentSettingsMenuItem;
	java.awt.Point mouseLocationForMiddleButtonDrag;
	private JMenu helpMenu;
	private JMenuItem refreshMenuItem;
	private JMenuItem customImagesMenuItem;
	private JMenu toolsMenu;
	private JMenuItem nameGeneratorMenuItem;
	protected String customImagesPath;
	private JMenu fileMenu;
	private JMenuItem newMapWithSameThemeMenuItem;
	private JMenuItem searchTextMenuItem;
	private TextSearchDialog textSearchDialog;

	public MainWindow(String fileToOpen)
	{
		super(frameTitleBase);

		Logger.setLoggerTarget(this);

		try
		{
			createGUI();
		}
		catch (Exception ex)
		{
			try
			{
				JOptionPane.showMessageDialog(null,
						"Unnable to create GUI because of error: " + ex.getMessage() 
								+ "\nVersion: " + MapSettings.currentVersion
								+ "\nOS Name: " + System.getProperty("os.name") 
								+ "\nInstall path: " + AssetsPath.getInstallPath()
								+ "\nStack trace: " + ExceptionUtils.getStackTrace(ex),
						"Error", JOptionPane.ERROR_MESSAGE);
			}
			catch (Exception inner)
			{
			}
			throw ex;
		}

		boolean isMapOpen = false;
		try
		{
			if (fileToOpen != null && !fileToOpen.isEmpty() && fileToOpen.endsWith(MapSettings.fileExtensionWithDot)
					&& new File(fileToOpen).exists())
			{
				openMap(new File(fileToOpen).getAbsolutePath());
				isMapOpen = true;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Logger.printError("打开从命令行传入的地图时出错:", e);
		}

		if (!isMapOpen)
		{
			setPlaceholderImage(new String[] { "Welcome to Notantis. Create or open a map.", "Use the File menu." });
			enableOrDisableFieldsThatRequireMap(false, null);
		}
	}

	void enableOrDisableFieldsThatRequireMap(boolean enable, MapSettings settings)
	{
		newMapWithSameThemeMenuItem.setEnabled(enable);
		saveMenuItem.setEnabled(enable);
		saveAsMenItem.setEnabled(enable);
		exportMapAsImageMenuItem.setEnabled(enable);
		exportHeightmapMenuItem.setEnabled(enable);

		if (!enable || undoer == null)
		{
			undoButton.setEnabled(false);
			redoButton.setEnabled(false);
		}
		else
		{
			undoer.updateUndoRedoEnabled();
		}
		clearEntireMapButton.setEnabled(enable);
		customImagesMenuItem.setEnabled(enable);

		nameGeneratorMenuItem.setEnabled(enable);
		searchTextMenuItem.setEnabled(enable);

		highlightLakesButton.setEnabled(enable);
		highlightRiversButton.setEnabled(enable);

		refreshMenuItem.setEnabled(enable);

		themePanel.enableOrDisableEverything(enable);
		toolsPanel.enableOrDisableEverything(enable, settings);
	}

	private void createGUI()
	{
		getContentPane().setPreferredSize(new Dimension(1400, 780));
		getContentPane().setLayout(new BorderLayout());

		setIconImage(AwtFactory.unwrap(ImageHelper.read(Paths.get(AssetsPath.getInstallPath(), "internal/taskbar icon.png").toString())));
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent event)
			{
				try
				{
					boolean cancelPressed = checkForUnsavedChanges();
					if (!cancelPressed)
					{
						UserPreferences.getInstance().save();
						dispose();
						System.exit(0);
					}
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
					JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
					Logger.printError("Error while closing:", ex);
				}
			}

			@Override
			public void windowActivated(WindowEvent e)
			{
			}
		});

		createMenuBar();

		undoer = new Undoer(this);

		themePanel = new ThemePanel(this);
		createMapEditingPanel();
		createMapUpdater();
		toolsPanel = new ToolsPanel(this, mapEditingPanel, updater);

		createConsoleOutput();

		JSplitPane splitPane0 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, themePanel, consoleOutputPane);
		splitPane0.setDividerLocation(9999999);
		splitPane0.setResizeWeight(1.0);

		JSplitPane splitPane1 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, splitPane0, mapEditingScrollPane);
		splitPane1.setOneTouchExpandable(true);
		JSplitPane splitPane2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, splitPane1, toolsPanel);
		splitPane2.setResizeWeight(1.0);
		splitPane2.setOneTouchExpandable(true);
		getContentPane().add(splitPane2, BorderLayout.CENTER);

		pack();

	}

	private void launchNewSettingsDialog(MapSettings settingsToKeepThemeFrom)
	{
		NewSettingsDialog dialog = new NewSettingsDialog(this, settingsToKeepThemeFrom);
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);
	}

	private void createConsoleOutput()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		txtConsoleOutput = new JTextArea();
		txtConsoleOutput.setEditable(false);
		panel.add(txtConsoleOutput);

		consoleOutputPane = new JScrollPane(panel);
		consoleOutputPane.setMinimumSize(new Dimension(0, 0));
		consoleOutputPane.getVerticalScrollBar().setUnitIncrement(SwingHelper.sidePanelScrollSpeed);
	}

	private void createMapEditingPanel()
	{
		mapEditingPanel = new MapEditingPanel(null);

		mapEditingPanel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (SwingUtilities.isLeftMouseButton(e))
				{
					updater.doIfMapIsReadyForInteractions(() -> toolsPanel.currentTool.handleMouseClickOnMap(e));
				}
			}

			@Override
			public void mousePressed(MouseEvent e)
			{
				if (e.isControlDown() && SwingUtilities.isLeftMouseButton(e) || SwingUtilities.isMiddleMouseButton(e))
				{
					mouseLocationForMiddleButtonDrag = e.getPoint();
				}
				else if (SwingUtilities.isLeftMouseButton(e))
				{
					updater.doIfMapIsReadyForInteractions(() -> toolsPanel.currentTool.handleMousePressedOnMap(e));
				}
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				if (SwingUtilities.isLeftMouseButton(e))
				{
					updater.doIfMapIsReadyForInteractions(() -> toolsPanel.currentTool.handleMouseReleasedOnMap(e));
				}
			}

		});

		mapEditingPanel.addMouseMotionListener(new MouseMotionListener()
		{
			@Override
			public void mouseMoved(MouseEvent e)
			{
				updater.doIfMapIsReadyForInteractions(() -> toolsPanel.currentTool.handleMouseMovedOnMap(e));
			}

			@Override
			public void mouseDragged(MouseEvent e)
			{
				if (e.isControlDown() && SwingUtilities.isLeftMouseButton(e) || SwingUtilities.isMiddleMouseButton(e))
				{
					if (mouseLocationForMiddleButtonDrag != null)
					{
						int deltaX = mouseLocationForMiddleButtonDrag.x - e.getX();
						int deltaY = mouseLocationForMiddleButtonDrag.y - e.getY();
						mapEditingScrollPane.getVerticalScrollBar()
								.setValue(mapEditingScrollPane.getVerticalScrollBar().getValue() + deltaY);
						mapEditingScrollPane.getHorizontalScrollBar()
								.setValue(mapEditingScrollPane.getHorizontalScrollBar().getValue() + deltaX);
					}
				}
				else if (SwingUtilities.isLeftMouseButton(e))
				{
					updater.doIfMapIsReadyForInteractions(() -> toolsPanel.currentTool.handleMouseDraggedOnMap(e));
				}
			}
		});

		mapEditingPanel.addMouseListener(new MouseListener()
		{
			@Override
			public void mouseReleased(MouseEvent e)
			{
			}

			@Override
			public void mousePressed(MouseEvent e)
			{
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				updater.doIfMapIsReadyForInteractions(() -> toolsPanel.currentTool.handleMouseExitedMap(e));
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
			}

			@Override
			public void mouseClicked(MouseEvent e)
			{
			}
		});

		mapEditingPanel.addMouseWheelListener(new MouseWheelListener()
		{
			@Override
			public void mouseWheelMoved(MouseWheelEvent e)
			{
				MainWindow.this.handleMouseWheelChangingZoom(e);
			}

		});

		mapEditingScrollPane = new JScrollPane(mapEditingPanel);
		mapEditingScrollPane.setMinimumSize(new Dimension(500, themePanel.getMinimumSize().height));

		mapEditingScrollPane.addComponentListener(new ComponentAdapter()
		{
			public void componentResized(ComponentEvent componentEvent)
			{
				updateZoomOptionsBasedOnWindowSize();
				if (ToolsPanel.fitToWindowZoomLevel.equals(toolsPanel.getZoomString()))
				{
					updater.createAndShowMapIncrementalUsingCenters(null);
				}
			}
		});

		// Speed up the scroll speed.
		mapEditingScrollPane.getVerticalScrollBar().setUnitIncrement(16);
	}

	private void createMapUpdater()
	{
		updater = new MapUpdater(true)
		{

			@Override
			protected void onBeginDraw()
			{
				showAsDrawing(true);
			}

			@Override
			public MapSettings getSettingsFromGUI()
			{
				MapSettings settings = MainWindow.this.getSettingsFromGUI(false);
				settings.resolution = displayQualityScale;
				return settings;
			}

			@Override
			protected void onFinishedDrawing(Image map, boolean anotherDrawIsQueued, int borderWidthAsDrawn,
					Rectangle incrementalChangeArea, List<String> warningMessages)
			{
				mapEditingPanel.mapFromMapCreator = AwtFactory.unwrap(map);
				mapEditingPanel.setBorderWidth(borderWidthAsDrawn);
				mapEditingPanel.setGraph(mapParts.graph);

				if (!undoer.isInitialized())
				{
					// This has to be done after the map is drawn rather
					// than when the editor frame is first created because
					// the first time the map is drawn is when the edits are
					// created.
					undoer.initialize(MainWindow.this.getSettingsFromGUI(true));
					enableOrDisableFieldsThatRequireMap(true, MainWindow.this.getSettingsFromGUI(false));
				}

				if (!hasDrawnCurrentMapAtLeastOnce)
				{
					hasDrawnCurrentMapAtLeastOnce = true;
					// Drawing for the first time can create or modify the
					// edits, so update them in lastSettingsLoadedOrSaved.
					lastSettingsLoadedOrSaved.edits = edits.deepCopy();
				}

				updateDisplayedMapFromGeneratedMap(false, incrementalChangeArea);

				if (!anotherDrawIsQueued)
				{
					showAsDrawing(false);
				}

				mapEditingPanel.setHighlightRivers(highlightRiversButton.isSelected());
				mapEditingPanel.setHighlightLakes(highlightLakesButton.isSelected());

				// Tell the scroll pane to update itself.
				mapEditingPanel.revalidate();
				mapEditingPanel.repaint();

				if (warningMessages != null && warningMessages.size() > 0)
				{
					JOptionPane.showMessageDialog(MainWindow.this, "<html>" + String.join("<br>", warningMessages) + "</html>",
							"带警告的地图", JOptionPane.WARNING_MESSAGE);
				}

				boolean isChange = settingsHaveUnsavedChanges();
				updateFrameTitle(isChange, !isChange);
			}

			@Override
			protected void onFailedToDraw()
			{
				showAsDrawing(false);
				mapEditingPanel.clearSelectedCenters();
				setPlaceholderImage(new String[] { "由于错误，地图绘制失败.",
						"重试, use " + fileMenu.getText() + " -> " + refreshMenuItem.getText() + "." });

				// In theory, enabling fields now could lead to the undoer not
				// working quite right since edits might not have been created.
				// But leaving fields disabled makes the user unable to fix the
				// error.
				enableOrDisableFieldsThatRequireMap(true, MainWindow.this.getSettingsFromGUI(false));
			}

			@Override
			protected MapEdits getEdits()
			{
				return edits;
			}

			@Override
			protected Image getCurrentMapForIncrementalUpdate()
			{
				return AwtFactory.wrap(mapEditingPanel.mapFromMapCreator);
			}

			@Override
			protected void onDrawSubmitted(UpdateType updateType)
			{
				// Incremental changes are handled in onFinishedDrawing to make
				// the drawing more responsive and to pick up changes caused by
				// the drawing code, such as when icons are removed because they
				// couldn't draw in the space provided.
				if (updateType != UpdateType.Incremental)
				{
					boolean isChange = settingsHaveUnsavedChanges();
					updateFrameTitle(isChange, !isChange);
				}
			}

		};
	}

	private void createMenuBar()
	{
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		fileMenu = new JMenu("文件");
		menuBar.add(fileMenu);

		final JMenuItem newRandomMapMenuItem = new JMenuItem("新随机地图");
		newRandomMapMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK));
		fileMenu.add(newRandomMapMenuItem);
		newRandomMapMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				boolean cancelPressed = checkForUnsavedChanges();
				if (!cancelPressed)
				{
					launchNewSettingsDialog(null);
				}
			}
		});

		newMapWithSameThemeMenuItem = new JMenuItem("主题不变的新地图");
		fileMenu.add(newMapWithSameThemeMenuItem);
		newMapWithSameThemeMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				boolean cancelPressed = checkForUnsavedChanges();
				if (!cancelPressed)
				{
					MapSettings settingsToKeepThemeFrom = getSettingsFromGUI(false);
					settingsToKeepThemeFrom.edits = new MapEdits();

					if (settingsToKeepThemeFrom.drawRegionColors
							&& !UserPreferences.getInstance().hideNewMapWithSameThemeRegionColorsMessage)
					{
						UserPreferences.getInstance().hideNewMapWithSameThemeRegionColorsMessage = SwingHelper.showDismissibleMessage(
							"区域颜色",  // "Region Colors"翻译为"区域颜色"
							"新的区域颜色将根据 " + LandWaterTool.colorGeneratorSettingsName + " 在 "  // 翻译的内容
							+ LandWaterTool.toolbarName + " 工具中生成，而不是根据你当前地图中实际使用的颜色。这意味着，如果你手动选择了区域颜色"  // 翻译的内容
							+ " 而不是生成的，那么你新地图中的区域颜色可能与当前地图中的颜色有很大不同。",  // 翻译的内容
							new Dimension(400, 133), MainWindow.this);
					}
							

					launchNewSettingsDialog(settingsToKeepThemeFrom);
				}
			}
		});

		final JMenuItem loadSettingsMenuItem = new JMenuItem("Open");
		loadSettingsMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
		fileMenu.add(loadSettingsMenuItem);
		loadSettingsMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				boolean cancelPressed = checkForUnsavedChanges();
				if (cancelPressed)
					return;

				Path curPath = openSettingsFilePath == null ? FileSystemView.getFileSystemView().getDefaultDirectory().toPath()
						: openSettingsFilePath;
				File currentFolder = new File(curPath.toString());
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setCurrentDirectory(currentFolder);
				fileChooser.setFileFilter(new FileFilter()
				{
					@Override
					public String getDescription()
					{
						return null;
					}

					@Override
					public boolean accept(File f)
					{
						return f.isDirectory() || f.getName().toLowerCase().endsWith(".properties")
								|| f.getName().toLowerCase().endsWith(MapSettings.fileExtensionWithDot);
					}
				});
				int status = fileChooser.showOpenDialog(MainWindow.this);
				if (status == JFileChooser.APPROVE_OPTION)
				{
					openMap(fileChooser.getSelectedFile().getAbsolutePath());

					if (MapSettings.isOldPropertiesFile(openSettingsFilePath.toString()))
					{
						JOptionPane.showMessageDialog(MainWindow.this, 
							FilenameUtils.getName(openSettingsFilePath.toString())
							+ " 是较旧格式的 '.properties' 文件。 \n保存时，它将被转换为较新格式，即 '"
							+ MapSettings.fileExtensionWithDot + "' 文件。", 
							"文件转换", 
							JOptionPane.INFORMATION_MESSAGE);
						openSettingsFilePath = Paths.get(FilenameUtils.getFullPath(openSettingsFilePath.toString()),
							FilenameUtils.getBaseName(openSettingsFilePath.toString()) + MapSettings.fileExtensionWithDot);
						forceSaveAs = true;
					}					

				}

			}
		});

		recentSettingsMenuItem = new JMenu("打开 最近");
		fileMenu.add(recentSettingsMenuItem);
		createOrUpdateRecentMapMenuButtons();

		saveMenuItem = new JMenuItem("节省");
		saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
		fileMenu.add(saveMenuItem);
		saveMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				saveSettings(MainWindow.this);
			}
		});

		saveAsMenItem = new JMenuItem("另存为...");
		fileMenu.add(saveAsMenItem);
		saveAsMenItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				saveSettingsAs(MainWindow.this);
			}
		});

		exportMapAsImageMenuItem = new JMenuItem("导出为图像");
		fileMenu.add(exportMapAsImageMenuItem);
		exportMapAsImageMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				handleExportAsImagePressed();
			}
		});

		exportHeightmapMenuItem = new JMenuItem("导出高度图");
		fileMenu.add(exportHeightmapMenuItem);
		exportHeightmapMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				handleExportHeightmapPressed();
			}
		});

		refreshMenuItem = new JMenuItem("刷新图像并重新绘制");
		refreshMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK));
		fileMenu.add(refreshMenuItem);
		refreshMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				handleImagesRefresh();
				updater.createAndShowMapFull(() -> mapEditingPanel.clearAllSelectionsAndHighlights());
			}
		});

		editMenu = new JMenu("编辑");
		menuBar.add(editMenu);

		undoButton = new JMenuItem("撤消");
		undoButton.setEnabled(false);
		editMenu.add(undoButton);
		undoButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK));
		undoButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (toolsPanel.currentTool != null)
				{
					updater.doWhenMapIsReadyForInteractions(() ->
					{
						undoer.undo();
					});
				}
			}
		});

		redoButton = new JMenuItem("重做");
		redoButton.setEnabled(false);
		editMenu.add(redoButton);
		redoButton.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK));
		redoButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (toolsPanel.currentTool != null)
				{
					updater.doWhenMapIsReadyForInteractions(() ->
					{
						undoer.redo();
					});
				}
			}
		});

		clearEntireMapButton = new JMenuItem("清除整个地图");
		editMenu.add(clearEntireMapButton);
		clearEntireMapButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				clearEntireMap();
			}
		});
		clearEntireMapButton.setEnabled(false);

		customImagesMenuItem = new JMenuItem("自定义图片文件夹");
		editMenu.add(customImagesMenuItem);
		customImagesMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				handleCustomImagesPressed();
			}
		});

		viewMenu = new JMenu("查看");
		menuBar.add(viewMenu);

		highlightLakesButton = new JCheckBoxMenuItem("突出显示湖泊");  // "Highlight Lakes"翻译为"突出显示湖泊"
		highlightLakesButton.setToolTipText("突出显示湖泊以使其更容易看到。");  // 汉化工具提示
		highlightLakesButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				mapEditingPanel.setHighlightLakes(highlightLakesButton.isSelected());
				mapEditingPanel.repaint();
			}
		});
		viewMenu.add(highlightLakesButton);
		
		highlightRiversButton = new JCheckBoxMenuItem("突出显示河流");  // "Highlight Rivers"翻译为"突出显示河流"
		highlightRiversButton.setToolTipText("突出显示河流以使其更容易看到。");  // 汉化工具提示
		highlightRiversButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				mapEditingPanel.setHighlightRivers(highlightRiversButton.isSelected());
				mapEditingPanel.repaint();
			}
		});
		viewMenu.add(highlightRiversButton);
		
		toolsMenu = new JMenu("工具");  // "Tools"翻译为"工具"
		menuBar.add(toolsMenu);
		
		nameGeneratorMenuItem = new JMenuItem("名称生成器");  // "Name Generator"翻译为"名称生成器"
		toolsMenu.add(nameGeneratorMenuItem);
		nameGeneratorMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				handleNameGeneratorPressed();
			}
		});
		
		searchTextMenuItem = new JMenuItem("搜索文本");  // "Search Text"翻译为"搜索文本"
		searchTextMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK));
		toolsMenu.add(searchTextMenuItem);
		searchTextMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				handleSearchTextPressed();
			}
		});
		
		helpMenu = new JMenu("帮助");  // "Help"翻译为"帮助"
		menuBar.add(helpMenu);
		
		JMenuItem keyboardShortcutsItem = new JMenuItem("键盘快捷键");  // "Keyboard Shortcuts"翻译为"键盘快捷键"
		helpMenu.add(keyboardShortcutsItem);
		keyboardShortcutsItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				JOptionPane.showMessageDialog(MainWindow.this,
					"<html>用于导航地图的键盘快捷键：" + "<ul>" + "<li>缩放：鼠标滚轮</li>"
					+ "<li>平移：按住鼠标中键或CTRL并单击鼠标左键，然后拖动</li>" + "</ul>" + "</html>",
					"键盘快捷键", JOptionPane.INFORMATION_MESSAGE);
			}
		});
		
		JMenuItem aboutNortantisItem = new JMenuItem("关于Nortantis");  // "About Nortantis"翻译为"关于Nortantis"
		helpMenu.add(aboutNortantisItem);
		aboutNortantisItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				showAboutNortantisDialog();
			}
		});
	}		

	void handleImagesRefresh()
	{
		updater.setEnabled(false);
		undoer.setEnabled(false);
		ImageCache.clear();
		MapSettings settings = getSettingsFromGUI(false);
		themePanel.handleImagesRefresh(settings);
		// Tell Icons tool to refresh image previews
		toolsPanel.handleImagesRefresh(settings);
		undoer.setEnabled(true);
		updater.setEnabled(true);
	}

	private void showAboutNortantisDialog()
	{
		AboutDialog dialog = new AboutDialog(this);
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);
	}

	private void createOrUpdateRecentMapMenuButtons()
	{
		recentSettingsMenuItem.removeAll();
		boolean hasRecents = false;

		for (String filePath : UserPreferences.getInstance().getRecentMapFilePaths())
		{
			String fileName = FilenameUtils.getName(filePath);
			JMenuItem item = new JMenuItem(fileName + "  (" + Paths.get(FilenameUtils.getPath(filePath)).toString() + ")");
			recentSettingsMenuItem.add(item);
			hasRecents = true;
			item.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					boolean cancelPressed = checkForUnsavedChanges();
					if (cancelPressed)
					{
						return;
					}

					openMap(filePath);
				}
			});
		}

		recentSettingsMenuItem.setEnabled(hasRecents);
	}

	private void openMap(String absolutePath)
	{
		if (!(new File(absolutePath).exists()))
		{
			JOptionPane.showMessageDialog(null, "地图 '" + absolutePath + "' 无法打开，因为它不存在。",
					"无法打开地图", JOptionPane.ERROR_MESSAGE);
			return;
		}		

		try
		{
			openSettingsFilePath = Paths.get(absolutePath);
			if (!MapSettings.isOldPropertiesFile(absolutePath))
			{
				UserPreferences.getInstance().addRecentMapFilePath(absolutePath);
				createOrUpdateRecentMapMenuButtons();
			}
			MapSettings settings = new MapSettings(openSettingsFilePath.toString());
			convertCustomImagesFolderIfNeeded(settings);

			updater.cancel();
			updater.dowWhenMapIsNotDrawing(() ->
			{
				loadSettingsIntoGUI(settings);
			});

			updateFrameTitle(false, true);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "打开 '" + absolutePath + "' 时出错: " + e.getMessage(), 
					"打开地图时出错", JOptionPane.ERROR_MESSAGE);
			Logger.printError("由于错误，无法打开 '" + absolutePath + "' :", e);
		}
	}

	private void convertCustomImagesFolderIfNeeded(MapSettings settings)
	{
		if (settings.hasOldCustomImagesFolderStructure())
		{
			try
			{
				MapSettings.convertOldCustomImagesFolder(settings.customImagesPath);
	
				JOptionPane.showMessageDialog(null, "您的自定义图像文件夹已自动转换为新结构。",
						"自定义图像文件夹已转换", JOptionPane.INFORMATION_MESSAGE);
			}
			catch (IOException ex)
			{
				String errorMessage = "重组自定义图像文件夹时出错，路径: " + settings.customImagesPath + ": "
						+ ex.getMessage();
				Logger.printError(errorMessage, ex);
				JOptionPane.showMessageDialog(null, errorMessage, "错误", JOptionPane.ERROR_MESSAGE);
			}
		}
	}	

	public void handleMouseWheelChangingZoom(MouseWheelEvent e)
	{
		updater.doIfMapIsReadyForInteractions(() ->
		{
			int scrollDirection = e.getUnitsToScroll() > 0 ? -1 : 1;
			int newIndex = toolsPanel.zoomComboBox.getSelectedIndex() + scrollDirection;
			if (newIndex < 0)
			{
				newIndex = 0;
			}
			else if (newIndex > toolsPanel.zoomComboBox.getItemCount() - 1)
			{
				newIndex = toolsPanel.zoomComboBox.getItemCount() - 1;
			}
			if (newIndex != toolsPanel.zoomComboBox.getSelectedIndex())
			{
				toolsPanel.zoomComboBox.setSelectedIndex(newIndex);
				updateDisplayedMapFromGeneratedMap(true, null);
			}
		});
	}

	public void updateDisplayedMapFromGeneratedMap(boolean updateScrollLocationIfZoomChanged, Rectangle incrementalChangeArea)
	{
		double oldZoom = zoom;
		zoom = translateZoomLevel((String) toolsPanel.zoomComboBox.getSelectedItem());

		if (mapEditingPanel.mapFromMapCreator != null)
		{
			java.awt.Rectangle scrollTo = null;

			if (updateScrollLocationIfZoomChanged && zoom != oldZoom)
			{
				java.awt.Rectangle visible = mapEditingPanel.getVisibleRect();
				double scale = zoom / oldZoom;
				java.awt.Point mousePosition = mapEditingPanel.getMousePosition();
				if (mousePosition != null && (zoom > oldZoom))
				{
					// Zoom toward the mouse's position, keeping the point
					// currently under the mouse the same if possible.
					scrollTo = new java.awt.Rectangle((int) (mousePosition.x * scale) - mousePosition.x + visible.x,
							(int) (mousePosition.y * scale) - mousePosition.y + visible.y, visible.width, visible.height);
				}
				else
				{
					// Zoom toward or away from the current center of the
					// screen.
					java.awt.Point currentCentroid = new java.awt.Point(visible.x + (visible.width / 2), visible.y + (visible.height / 2));
					java.awt.Point targetCentroid = new java.awt.Point((int) (currentCentroid.x * scale),
							(int) (currentCentroid.y * scale));
					scrollTo = new java.awt.Rectangle(targetCentroid.x - visible.width / 2, targetCentroid.y - visible.height / 2,
							visible.width, visible.height);
				}
			}

			toolsPanel.currentTool.onBeforeShowMap();
			mapEditingPanel.setZoom(zoom);
			mapEditingPanel.setResolution(displayQualityScale);
			Method method = zoom < 0.3 ? Method.QUALITY : Method.BALANCED;
			int zoomedWidth = (int) (mapEditingPanel.mapFromMapCreator.getWidth() * zoom);
			if (zoomedWidth <= 0)
			{
				// Prevents a crash if someone collapses the map editing panel.
				zoomedWidth = 600;
			}

			if (method == Method.QUALITY)
			{
				// Can't incrementally zoom. Zoom the whole thing.
				mapEditingPanel.setImage(AwtFactory
						.unwrap(ImageHelper.scaleByWidth(AwtFactory.wrap(mapEditingPanel.mapFromMapCreator), zoomedWidth, method)));
			}
			else
			{

				if (incrementalChangeArea == null)
				{
					// It's important that this image scaling is done using the
					// same method as the incremental case below
					// (when incrementalChangeArea != null), or at least close
					// enough that people can't tell the difference.
					// The reason is that the incremental case will update
					// pieces of the image created below.
					// I don't use ImageHelper.scaleInto for the full image case
					// because it's 5x slower than the below
					// method, which uses ImgScalr.
					mapEditingPanel.setImage(AwtFactory
							.unwrap(ImageHelper.scaleByWidth(AwtFactory.wrap(mapEditingPanel.mapFromMapCreator), zoomedWidth, method)));
				}
				else
				{
					// These two images will be the same if the zoom and display
					// quality are the same, in which case
					// ImageHelper.scaleByWidth called above returns the input
					// image.
					if (mapEditingPanel.mapFromMapCreator != mapEditingPanel.getImage())
					{
						ImageHelper.scaleInto(AwtFactory.wrap(mapEditingPanel.mapFromMapCreator),
								AwtFactory.wrap(mapEditingPanel.getImage()), incrementalChangeArea);
					}
				}
			}

			if (scrollTo != null)
			{
				// For some reason I have to do a whole bunch of revalidation or
				// else scrollRectToVisible doesn't realize the map has changed
				// size.
				mapEditingPanel.revalidate();
				mapEditingScrollPane.revalidate();
				this.revalidate();

				mapEditingPanel.scrollRectToVisible(scrollTo);
			}

			mapEditingPanel.revalidate();
			mapEditingScrollPane.revalidate();
			mapEditingPanel.repaint();
			mapEditingScrollPane.repaint();
		}
	}

	private void updateZoomOptionsBasedOnWindowSize()
	{
		double minZoom = translateZoomLevel(ToolsPanel.fitToWindowZoomLevel);
		String selectedZoom = (String) toolsPanel.getZoomString();
		toolsPanel.zoomComboBox.removeAllItems();
		for (String level : toolsPanel.zoomLevels)
		{
			if (translateZoomLevel(level) >= minZoom || level.equals(ToolsPanel.fitToWindowZoomLevel))
			{
				toolsPanel.zoomComboBox.addItem(level);
			}
		}
		toolsPanel.zoomComboBox.setSelectedItem(selectedZoom);
	}

	private double translateZoomLevel(String zoomLevel)
	{
		if (zoomLevel == null)
		{
			return 1.0;
		}
		else if (zoomLevel.equals(ToolsPanel.fitToWindowZoomLevel))
		{
			if (mapEditingPanel.mapFromMapCreator != null)
			{
				final int additionalWidthToRemoveIDontKnowWhereItsCommingFrom = 2;
				nortantis.geom.Dimension size = new nortantis.geom.Dimension(
						mapEditingScrollPane.getSize().width - additionalWidthToRemoveIDontKnowWhereItsCommingFrom,
						mapEditingScrollPane.getSize().height - additionalWidthToRemoveIDontKnowWhereItsCommingFrom);

				nortantis.geom.Dimension fitted = ImageHelper.fitDimensionsWithinBoundingBox(size,
						mapEditingPanel.mapFromMapCreator.getWidth(), mapEditingPanel.mapFromMapCreator.getHeight());
				return (fitted.width / mapEditingPanel.mapFromMapCreator.getWidth()) * mapEditingPanel.osScale;
			}
			else
			{
				return 1.0;
			}
		}
		else
		{
			double percentage = parsePercentage(zoomLevel);
			if (mapEditingPanel.mapFromMapCreator != null)
			{
				// Divide by the size of the generated map because the map's
				// displayed size should be the same
				// no matter the resolution it generated at.
				return (oneHundredPercentMapWidth * percentage) / mapEditingPanel.mapFromMapCreator.getWidth();
			}
			else
			{
				return 1.0;
			}
		}
	}

	public void showAsDrawing(boolean isDrawing)
	{
		clearEntireMapButton.setEnabled(!isDrawing);
		toolsPanel.showAsDrawing(isDrawing);
		if (textSearchDialog != null)
		{
			textSearchDialog.setAllowSearches(!isDrawing);
		}
	}

	private double parsePercentage(String zoomStr)
	{
		double zoomPercent = Double.parseDouble(zoomStr.substring(0, zoomStr.length() - 1));
		return zoomPercent / 100.0;
	}

	/**
	 * Handles when zoom level changes in the display.
	 */
	public void handleImageQualityChange(DisplayQuality quality)
	{
		updateImageQualityScale(quality);

		ImageCache.clear();
		updater.createAndShowMapFull();
	}

	public void updateImageQualityScale(DisplayQuality quality)
	{
		if (quality == DisplayQuality.Very_Low)
		{
			displayQualityScale = 0.50;
		}
		else if (quality == DisplayQuality.Low)
		{
			displayQualityScale = 0.75;
		}
		else if (quality == DisplayQuality.Medium)
		{
			displayQualityScale = 1.0;
		}
		else if (quality == DisplayQuality.High)
		{
			displayQualityScale = 1.25;
		}
		else if (quality == DisplayQuality.Ultra)
		{
			displayQualityScale = 1.5;
		}
	}

	public void clearEntireMap()
	{
		updater.doWhenMapIsReadyForInteractions(() ->
		{
			if (updater.mapParts == null || updater.mapParts.graph == null)
			{
				return;
			}

			toolsPanel.resetToolsForNewMap();

			// Erase text
			edits.text.clear();

			for (Center center : updater.mapParts.graph.centers)
			{
				// Change land to ocean and erase icons
				CenterEdit newValues = new CenterEdit(center.index, true, false, null, null, null);
				edits.centerEdits.put(center.index, newValues);
			}

			// Erase rivers
			for (Edge edge : updater.mapParts.graph.edges)
			{
				EdgeEdit eEdit = edits.edgeEdits.get(edge.index);
				eEdit.riverLevel = 0;
			}

			// Erase free icons
			edits.freeIcons.clear();

			undoer.setUndoPoint(UpdateType.Full, null);
			updater.createAndShowMapTerrainChange();
		});
	}

	private void handleExportAsImagePressed()
	{
		ImageExportDialog dialog = new ImageExportDialog(this, ImageExportType.Map);
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);
	}

	private void handleExportHeightmapPressed()
	{
		ImageExportDialog dialog = new ImageExportDialog(this, ImageExportType.Heightmap);
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);
	}

	private void handleCustomImagesPressed()
	{
		CustomImagesDialog dialog = new CustomImagesDialog(this, customImagesPath, (value) ->
		{
			customImagesPath = value;
			undoer.setUndoPoint(UpdateType.Full, null, () -> handleImagesRefresh());
			updater.createAndShowMapFull(() -> handleImagesRefresh());
		});
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);
	}

	private void handleNameGeneratorPressed()
	{
		MapSettings settings = getSettingsFromGUI(false);
		NameGeneratorDialog dialog = new NameGeneratorDialog(this, settings);
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);
	}

	private void handleSearchTextPressed()
	{
		if (textSearchDialog == null || !(textSearchDialog.isVisible()))
		{
			textSearchDialog = new TextSearchDialog(this);
			textSearchDialog.setAllowSearches((!updater.isMapBeingDrawn()));

			java.awt.Point parentLocation = getLocation();
			Dimension parentSize = getSize();
			Dimension dialogSize = textSearchDialog.getSize();

			textSearchDialog.setLocation(parentLocation.x + parentSize.width / 2 - dialogSize.width / 2,
					parentLocation.y + parentSize.height - dialogSize.height - 18);

			textSearchDialog.setVisible(true);
		}
		else
		{
			textSearchDialog.requestFocusAndSelectAll();
		}
	}

	public boolean checkForUnsavedChanges()
	{
		if (lastSettingsLoadedOrSaved == null)
		{
			return false;
		}

		if (settingsHaveUnsavedChanges())
		{
			int n = JOptionPane.showConfirmDialog(this, "Settings have been modfied. Save changes?", "", JOptionPane.YES_NO_CANCEL_OPTION);
			if (n == JOptionPane.YES_OPTION)
			{
				saveSettings(this);
			}
			else if (n == JOptionPane.NO_OPTION)
			{
			}
			else if (n == JOptionPane.CANCEL_OPTION)
			{
				return true;
			}
		}

		return false;
	}

	private boolean settingsHaveUnsavedChanges()
	{
		if (lastSettingsLoadedOrSaved == null)
		{
			return true;
		}

		final MapSettings currentSettings = getSettingsFromGUI(false);

		if (DebugFlags.shouldWriteBeforeAndAfterJsonWhenSavePromptShows())
		{
			try
			{
				currentSettings.writeToFile("currentSettings.json");
				lastSettingsLoadedOrSaved.writeToFile("lastSettingsLoadedOrSaved.json");
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}

		if (hasDrawnCurrentMapAtLeastOnce)
		{
			return !currentSettings.equals(lastSettingsLoadedOrSaved);
		}
		else
		{
			// Ignore edits in this comparison because the first draw can create
			// or change edits, and the user cannot modify the
			// edits until the map has been drawn.
			return !currentSettings.equalsIgnoringEdits(lastSettingsLoadedOrSaved);
		}
	}

	public void saveSettings(Component parent)
	{
		if (openSettingsFilePath == null || forceSaveAs)
		{
			saveSettingsAs(parent);
			forceSaveAs = false;
		}
		else
		{
			final MapSettings settings = getSettingsFromGUI(false);
			try
			{
				saveMap(settings, openSettingsFilePath.toString());
			}
			catch (IOException e)
			{
				e.printStackTrace();
				Logger.printError("保存地图时出错。", e);
				JOptionPane.showMessageDialog(null, e.getMessage(), "无法保存设置。", JOptionPane.ERROR_MESSAGE);
			}
			updateFrameTitle(false, true);
		}
	}

	public void saveSettingsAs(Component parent)
	{
		Path curPath = openSettingsFilePath == null ? FileSystemView.getFileSystemView().getDefaultDirectory().toPath()
				: openSettingsFilePath;
		File currentFolder = openSettingsFilePath == null ? curPath.toFile() : new File(FilenameUtils.getFullPath(curPath.toString()));
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setCurrentDirectory(currentFolder);
		fileChooser.setFileFilter(new FileFilter()
		{
			@Override
			public String getDescription()
			{
				return null;
			}

			@Override
			public boolean accept(File f)
			{
				return f.isDirectory() || f.getName().endsWith(MapSettings.fileExtensionWithDot);
			}
		});

		// This is necessary when we want to automatically select a file that
		// doesn't exist to save into, which is done
		// when converting a properties file into a nort file.
		if (openSettingsFilePath != null && !FilenameUtils.getName(openSettingsFilePath.toString()).equals(""))
		{
			fileChooser.setSelectedFile(new File(openSettingsFilePath.toString()));
		}

		int status = fileChooser.showSaveDialog(parent);
		if (status == JFileChooser.APPROVE_OPTION)
		{
			openSettingsFilePath = Paths.get(fileChooser.getSelectedFile().getAbsolutePath());
			if (!openSettingsFilePath.getFileName().toString().endsWith(MapSettings.fileExtensionWithDot))
			{
				openSettingsFilePath = Paths.get(openSettingsFilePath.toString() + MapSettings.fileExtensionWithDot);
			}

			final MapSettings settings = getSettingsFromGUI(false);
			try
			{
				saveMap(settings, openSettingsFilePath.toString());
			}
			catch (IOException e)
			{
				e.printStackTrace();
				Logger.printError("在将设置保存到新文件时发出错误提示：", e);
				JOptionPane.showMessageDialog(null, e.getMessage(), "无法保存设置。", JOptionPane.ERROR_MESSAGE);
			}

			updateFrameTitle(false, true);
		}
	}

	private void saveMap(MapSettings settings, String absolutePath) throws IOException
	{
		settings.writeToFile(absolutePath);
		Logger.println("设置保存到 " + openSettingsFilePath.toString());
		updateLastSettingsLoadedOrSaved(settings);
		UserPreferences.getInstance().addRecentMapFilePath(absolutePath);
		createOrUpdateRecentMapMenuButtons();
	}

	private boolean showUnsavedChangesSymbol = false;

	private void updateFrameTitle(boolean isTriggeredByChange, boolean clearUnsavedChangesSymbol)
	{
		if (isTriggeredByChange)
		{
			showUnsavedChangesSymbol = true;
		}
		if (clearUnsavedChangesSymbol)
		{
			showUnsavedChangesSymbol = false;
		}

		String title;
		if (openSettingsFilePath != null)
		{
			title = (showUnsavedChangesSymbol ? "✎ " : "") + FilenameUtils.getName(openSettingsFilePath.toString()) + " - "
					+ frameTitleBase;
		}
		else
		{
			title = frameTitleBase;
		}
		setTitle(title);
	}

	public void clearOpenSettingsFilePath()
	{
		openSettingsFilePath = null;
	}

	void loadSettingsIntoGUI(MapSettings settings)
	{
		boolean needsImagesRefresh = !Objects.equals(settings.customImagesPath, customImagesPath);
		hasDrawnCurrentMapAtLeastOnce = false;
		mapEditingPanel.clearAllSelectionsAndHighlights();

		updateLastSettingsLoadedOrSaved(settings);
		toolsPanel.resetToolsForNewMap();
		loadSettingsAndEditsIntoThemeAndToolsPanels(settings, false, needsImagesRefresh);
		exportResolution = settings.resolution;
		imageExportPath = settings.imageExportPath;
		heightmapExportResolution = settings.heightmapResolution;
		heightmapExportPath = settings.heightmapExportPath;

		setPlaceholderImage(new String[] { "Drawing map..." });

		undoer.reset();

		if (needsImagesRefresh)
		{
			handleImagesRefresh();
		}

		if (settings.edits != null && settings.edits.isInitialized())
		{
			undoer.initialize(settings);
			enableOrDisableFieldsThatRequireMap(true, settings);
		}
		else
		{
			// Note - this call needs to come after everything that calls into
			// loadSettingsAndEditsIntoThemeAndToolsPanels because the text
			// tool
			// might enable fields when when loading settings, which will cause
			// fields to be enabled before the map is ready.
			enableOrDisableFieldsThatRequireMap(false, settings);
		}

		toolsPanel.resetZoomToDefault();
		updater.createAndShowMapFull();
		updateFrameTitle(false, true);

		defaultMapExportAction = settings.defaultHeightmapExportAction;
		defaultHeightmapExportAction = settings.defaultHeightmapExportAction;
	}

	void loadSettingsAndEditsIntoThemeAndToolsPanels(MapSettings settings, boolean isUndoRedoOrAutomaticChange, boolean willDoImagesRefresh)
	{
		updater.setEnabled(false);
		undoer.setEnabled(false);
		customImagesPath = settings.customImagesPath;
		edits = settings.edits;
		boolean changeEffectsBackgroundImages = themePanel.loadSettingsIntoGUI(settings);
		toolsPanel.loadSettingsIntoGUI(settings, isUndoRedoOrAutomaticChange, changeEffectsBackgroundImages, willDoImagesRefresh);
		undoer.setEnabled(true);
		updater.setEnabled(true);
	}

	private void updateLastSettingsLoadedOrSaved(MapSettings settings)
	{
		lastSettingsLoadedOrSaved = settings.deepCopy();
	}

	MapSettings getSettingsFromGUI(boolean deepCopyEdits)
	{
		if (lastSettingsLoadedOrSaved == null)
		{
			// No settings are loaded.
			return null;
		}

		MapSettings settings = lastSettingsLoadedOrSaved.deepCopyExceptEdits();
		if (deepCopyEdits)
		{
			settings.edits = edits.deepCopy();
		}
		else
		{
			settings.edits = edits;
		}

		// Settings which have a UI in a popup.
		settings.resolution = exportResolution;
		settings.defaultMapExportAction = defaultMapExportAction;
		settings.defaultHeightmapExportAction = defaultHeightmapExportAction;
		settings.imageExportPath = imageExportPath;
		settings.heightmapResolution = heightmapExportResolution;
		settings.heightmapExportPath = heightmapExportPath;
		settings.customImagesPath = customImagesPath;

		themePanel.getSettingsFromGUI(settings);
		toolsPanel.getSettingsFromGUI(settings);

		if (lastSettingsLoadedOrSaved != null)
		{
			// Copy over any settings which do not have a UI element.
			settings.pointPrecision = lastSettingsLoadedOrSaved.pointPrecision;
			settings.textRandomSeed = lastSettingsLoadedOrSaved.textRandomSeed;
			settings.regionsRandomSeed = lastSettingsLoadedOrSaved.regionsRandomSeed;
			settings.randomSeed = lastSettingsLoadedOrSaved.randomSeed;

			// Copy over settings with a UI only in the new map dialog.
			settings.worldSize = lastSettingsLoadedOrSaved.worldSize;
			settings.randomSeed = lastSettingsLoadedOrSaved.randomSeed;
			settings.edgeLandToWaterProbability = lastSettingsLoadedOrSaved.edgeLandToWaterProbability;
			settings.centerLandToWaterProbability = lastSettingsLoadedOrSaved.centerLandToWaterProbability;
			settings.generatedWidth = lastSettingsLoadedOrSaved.generatedWidth;
			settings.generatedHeight = lastSettingsLoadedOrSaved.generatedHeight;
		}

		return settings;
	}

	public Color getLandColor()
	{
		return themePanel.getLandColor();
	}

	private void setPlaceholderImage(String[] message)
	{
		mapEditingPanel.setImage(AwtFactory.unwrap(ImageHelper.createPlaceholderImage(message)));

		// Clear out the map from map creator so that causing the window to
		// re-zoom while the placeholder image
		// is displayed doesn't show the previous map. This can happen when the
		// zoom is fit to window, you create
		// a new map, then resize the window while the new map is drawing for
		// the first time.
		mapEditingPanel.mapFromMapCreator = null;

		mapEditingPanel.repaint();
	}

	void handleThemeChange(boolean changeEffectsBackgroundImages)
	{
		// This check is to filter out automatic changes caused by
		// loadSettingsIntoGUI.
		if (undoer.isEnabled())
		{
			// Allow editor tools to update based on changes in the themes
			// panel.
			toolsPanel.loadSettingsIntoGUI(getSettingsFromGUI(false), true, changeEffectsBackgroundImages, false);
		}
	}

	@Override
	public void appendLoggerMessage(String message)
	{
		txtConsoleOutput.append(message);
		consoleOutputPane.revalidate();
		consoleOutputPane.repaint();
	}

	@Override
	public void clearLoggerMessages()
	{
		txtConsoleOutput.setText("");
		txtConsoleOutput.revalidate();
		txtConsoleOutput.repaint();
		consoleOutputPane.revalidate();
		consoleOutputPane.repaint();
	}

	@Override
	public boolean isReadyForLogging()
	{
		return txtConsoleOutput != null;
	}

	public Path getOpenSettingsFilePath()
	{
		return openSettingsFilePath;
	}

	String getFileMenuName()
	{
		return fileMenu.getText();
	}

	String getRefreshImagesMenuName()
	{
		return refreshMenuItem.getText();
	}

	/**
	 * Launch the application.
	 */
	public static void main(String[] args)
	{
		// Tell drawing code to use AWT.
		PlatformFactory.setInstance(new AwtFactory());

		try
		{
			// UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			UIManager.setLookAndFeel(new FlatDarkLaf());
		}
		catch (Exception e)
		{
			System.out.println("在设置外观和感觉时出错： " + e.getMessage());
			e.printStackTrace();
		}

		String fileToOpen = args.length > 0 ? args[0] : "";
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				try
				{
					MainWindow mainWindow = new MainWindow(fileToOpen);
					mainWindow.setVisible(true);
				}
				catch (Exception e)
				{
					System.out.println("启动程序时出错： " + e.getMessage());
					e.printStackTrace();
				}
			}
		});
	}
}
