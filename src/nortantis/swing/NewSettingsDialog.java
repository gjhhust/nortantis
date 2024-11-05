package nortantis.swing;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.Timer;

import nortantis.IconType;
import nortantis.ImageCache;
import nortantis.MapSettings;
import nortantis.SettingsGenerator;
import nortantis.editor.MapUpdater;
import nortantis.editor.UserPreferences;
import nortantis.geom.Rectangle;
import nortantis.platform.Image;
import nortantis.platform.awt.AwtFactory;
import nortantis.swing.ThemePanel.LandColoringMethod;
import nortantis.util.FileHelper;
import nortantis.util.ImageHelper;
import nortantis.util.Range;

@SuppressWarnings("serial")
public class NewSettingsDialog extends JDialog
{
	JSlider worldSizeSlider;
	JSlider edgeLandToWaterProbSlider;
	JSlider centerLandToWaterProbSlider;
	private JComboBox<String> dimensionsComboBox;
	BooksWidget booksWidget;
	MapSettings settings;
	private JProgressBar progressBar;
	private MapUpdater updater;
	private MapEditingPanel mapEditingPanel;
	private Dimension defaultSize = new Dimension(900, 750);
	private int amountToSubtractFromLeftAndRightPanels = 40;
	private Timer progressBarTimer;
	public final double cityFrequencySliderScale = 100.0 * 1.0 / SettingsGenerator.maxCityProbabillity;
	private JSlider cityFrequencySlider;
	private JComboBox<String> cityIconsTypeComboBox;
	private JPanel mapEditingPanelContainer;
	private JComboBox<LandColoringMethod> landColoringMethodComboBox;
	MainWindow mainWindow;
	private JTextField pathDisplay;

	public NewSettingsDialog(MainWindow mainWindow, MapSettings settingsToKeepThemeFrom)
	{
		super(mainWindow, "Create New Map", Dialog.ModalityType.APPLICATION_MODAL);
		this.mainWindow = mainWindow;

		createGUI(mainWindow);

		if (settingsToKeepThemeFrom == null)
		{
			settings = SettingsGenerator.generate(null);
		}
		else
		{
			settings = settingsToKeepThemeFrom.deepCopy();
			// Clear out export paths so that creating a new map was the same theme doesn't overwrite exported files from the previous map.
			settings.imageExportPath = null;
			settings.heightmapExportPath = null;
			randomizeLand();
		}
		loadSettingsIntoGUI(settings);

		updater.setEnabled(true);
	}

	private void createGUI(MainWindow mainWindow)
	{
		setSize(defaultSize);
		setMinimumSize(defaultSize);

		GridBagOrganizer organizer = new GridBagOrganizer();
		JPanel container = organizer.panel;
		add(container);
		container.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
		container.setPreferredSize(defaultSize);

		JPanel generatorSettingsPanel = new JPanel();
		generatorSettingsPanel.setLayout(new BoxLayout(generatorSettingsPanel, BoxLayout.X_AXIS));
		organizer.addLeftAlignedComponent(generatorSettingsPanel, 0, 0, false);

		createLeftPanel(generatorSettingsPanel);
		generatorSettingsPanel.add(Box.createHorizontalStrut(20));
		createRightPanel(generatorSettingsPanel);


		createMapEditingPanel();
		createMapUpdater();
		organizer.addLeftAlignedComponent(mapEditingPanelContainer, 0, 0, true);


		ActionListener listener = new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				progressBar.setVisible(updater.isMapBeingDrawn());
			}
		};
		progressBarTimer = new Timer(50, listener);
		progressBarTimer.setInitialDelay(500);

		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));


		JButton randomizeThemeButton = new JButton("随机主题");
		randomizeThemeButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				randomizeTheme();
			}
		});
		bottomPanel.add(randomizeThemeButton);
		bottomPanel.add(Box.createHorizontalStrut(5));


		JButton randomizeLandButton = new JButton("随机化土地");
		randomizeLandButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				randomizeLand();
			}
		});
		bottomPanel.add(randomizeLandButton);
		bottomPanel.add(Box.createHorizontalStrut(10));


		progressBar = new JProgressBar();
		progressBar.setStringPainted(true);
		progressBar.setString("绘图...");
		progressBar.setIndeterminate(true);
		progressBar.setVisible(false);
		bottomPanel.add(progressBar);
		bottomPanel.add(Box.createHorizontalGlue());

		JPanel bottomButtonsPanel = new JPanel();
		bottomButtonsPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		JButton createMapButton = new JButton("<html>C<u>r</u>eate Map</html>");
		createMapButton.setMnemonic(KeyEvent.VK_R);
		bottomButtonsPanel.add(createMapButton);
		createMapButton.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{
				onCreateMap(mainWindow);
			}
		});

		bottomPanel.add(bottomButtonsPanel);
		organizer.addLeftAlignedComponent(bottomPanel, 0, 0, false);

		addComponentListener(new ComponentAdapter()
		{
			public void componentResized(ComponentEvent componentEvent)
			{
				handleMapChange();
			}
		});

		pack();
	}

	private void onCreateMap(MainWindow mainWindow)
	{
		mainWindow.updater.cancel();

		// Disable the updater so that it ignores anything triggered by the window resizing as it closes.
		// I'm not if that's even possible but I think I saw it happen a few times and try to draw a tiny
		// map.
		mainWindow.updater.setEnabled(false);

		mainWindow.updater.dowWhenMapIsNotDrawing(() ->
		{
			mainWindow.clearOpenSettingsFilePath();
			MapSettings settings = getSettingsFromGUI();
			mainWindow.loadSettingsIntoGUI(settings);

			dispose();
		});
	}

	private void createLeftPanel(JPanel generatorSettingsPanel)
	{
		GridBagOrganizer organizer = new GridBagOrganizer();

		JPanel leftPanel = organizer.panel;
		generatorSettingsPanel.add(leftPanel);

		dimensionsComboBox = new JComboBox<>();
		for (String dimension : SettingsGenerator.getAllowedDimmensions())
		{
			dimensionsComboBox.addItem(dimension);
		}
		createMapChangeListener(dimensionsComboBox);
		organizer.addLabelAndComponent("尺寸: <br>(在编辑器中无法更改)",
				"导出时地图的尺寸（100% 分辨率），虽然在导出时分辨率可以上下缩放。"
				+ " 如果您添加了边框，这不包括边框。",
				dimensionsComboBox);
		
		worldSizeSlider = new JSlider();
		worldSizeSlider.setSnapToTicks(true);
		worldSizeSlider.setMajorTickSpacing(8000);
		worldSizeSlider.setMinorTickSpacing(SettingsGenerator.worldSizePrecision);
		worldSizeSlider.setPaintLabels(true);
		worldSizeSlider.setPaintTicks(true);
		worldSizeSlider.setMinimum(SettingsGenerator.minWorldSize);
		worldSizeSlider.setMaximum(SettingsGenerator.maxWorldSize);
		createMapChangeListener(worldSizeSlider);
		organizer.addLabelAndComponent("世界大小: <br>(在编辑器中无法更改)",
				"随机生成世界中的多边形数量。", worldSizeSlider);
		
		edgeLandToWaterProbSlider = new JSlider();
		edgeLandToWaterProbSlider.setValue(70);
		edgeLandToWaterProbSlider.setPaintTicks(true);
		edgeLandToWaterProbSlider.setPaintLabels(true);
		edgeLandToWaterProbSlider.setMinorTickSpacing(25);
		edgeLandToWaterProbSlider.setMajorTickSpacing(25);
		{
			Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
			for (int i = edgeLandToWaterProbSlider.getMinimum(); i < edgeLandToWaterProbSlider.getMaximum() + 1; i += edgeLandToWaterProbSlider.getMajorTickSpacing())
			{
				labelTable.put(i, new JLabel(Double.toString(i / 100.0)));
			}
			edgeLandToWaterProbSlider.setLabelTable(labelTable);
		}
		createMapChangeListener(edgeLandToWaterProbSlider);
		organizer.addLabelAndComponent("边缘土地概率:",
				"接触地图边缘的构造板块是土地而不是海洋的概率。",
				edgeLandToWaterProbSlider);
		
		centerLandToWaterProbSlider = new JSlider();
		centerLandToWaterProbSlider.setValue(70);
		centerLandToWaterProbSlider.setPaintTicks(true);
		centerLandToWaterProbSlider.setPaintLabels(true);
		centerLandToWaterProbSlider.setMinorTickSpacing(25);
		centerLandToWaterProbSlider.setMajorTickSpacing(25);
		{
			Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
			for (int i = centerLandToWaterProbSlider.getMinimum(); i < centerLandToWaterProbSlider.getMaximum() + 1; i += centerLandToWaterProbSlider.getMajorTickSpacing())
			{
				labelTable.put(i, new JLabel(Double.toString(i / 100.0)));
			}
			centerLandToWaterProbSlider.setLabelTable(labelTable);
		}
		createMapChangeListener(centerLandToWaterProbSlider);
		organizer.addLabelAndComponent("中心土地概率:",
				"不接触地图边缘的构造板块是土地而不是海洋的概率。",
				centerLandToWaterProbSlider);
		
		landColoringMethodComboBox = new JComboBox<LandColoringMethod>();
		for (LandColoringMethod method : LandColoringMethod.values())
		{
			landColoringMethodComboBox.addItem(method);
		}
		
		createMapChangeListener(landColoringMethodComboBox);
		organizer.addLabelAndComponent("土地着色方法:", "如何着色土地。", landColoringMethodComboBox);
		
		JButton changeButton = new JButton("更改");  // 汉化按钮文本
		pathDisplay = new JTextField();
		pathDisplay.setText(FileHelper.replaceHomeFolderPlaceholder(UserPreferences.getInstance().defaultCustomImagesPath));
		pathDisplay.setEditable(false);
		organizer.addLabelAndComponentsHorizontal("自定义图片文件夹:", "配置在生成此地图时使用的自定义图片。",
				Arrays.asList(pathDisplay, changeButton));
		

		changeButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				CustomImagesDialog dialog = new CustomImagesDialog(mainWindow, settings.customImagesPath, (value) ->
				{
					settings.customImagesPath = value;
					updatePathDisplay();
					initializeCityTypeOptions();

					updater.createAndShowMapFull(() ->
					{
						ImageCache.clear();
					});
				});
				dialog.setLocationRelativeTo(NewSettingsDialog.this);
				dialog.setVisible(true);
			}
		});

		organizer.addLeftAlignedComponent(
				Box.createRigidArea(new Dimension((defaultSize.width / 2) - amountToSubtractFromLeftAndRightPanels, 0)));

		organizer.addVerticalFillerRow();
	}

	private void initializeCityTypeOptions()
	{
		SwingHelper.initializeComboBoxItems(cityIconsTypeComboBox,
				ImageCache.getInstance(settings.customImagesPath).getIconGroupNames(IconType.cities), settings.cityIconTypeName, false);
	}

	private void updatePathDisplay()
	{
		if (settings != null && settings.customImagesPath != null && !settings.customImagesPath.isEmpty())
		{
			pathDisplay.setText(FileHelper.replaceHomeFolderPlaceholder(settings.customImagesPath));
		}
		else
		{
			pathDisplay.setText("");
		}
	}

	private void createRightPanel(JPanel generatorSettingsPanel)
	{
		GridBagOrganizer organizer = new GridBagOrganizer();

		JPanel rightPanel = organizer.panel;
		generatorSettingsPanel.add(rightPanel);


		cityFrequencySlider = new JSlider();
		cityFrequencySlider.setPaintLabels(true);
		cityFrequencySlider.setSnapToTicks(false);
		cityFrequencySlider.setPaintTicks(true);
		cityFrequencySlider.setMinorTickSpacing(10);
		cityFrequencySlider.setMinimum(0);
		cityFrequencySlider.setMaximum(100);
		cityFrequencySlider.setMajorTickSpacing(25);
		createMapChangeListener(cityFrequencySlider);
		organizer.addLabelAndComponent("城市频率:",
        "更高的值会产生更多的城市。较低的值会产生较少的城市。零表示没有城市。",
        cityFrequencySlider);

		cityIconsTypeComboBox = new JComboBox<String>();
		createMapChangeListener(cityIconsTypeComboBox);
		organizer.addLabelAndComponent("城市图标类型:", "要使用的一组城市图像。", cityIconsTypeComboBox);

		booksWidget = new BooksWidget(true, () -> handleMapChange());
		booksWidget.getContentPanel().setPreferredSize(new Dimension(360, 180));
		organizer.addLeftAlignedComponentWithStackedLabel("用于生成文本的书籍:",
				"选择的书籍将用于生成新名称。", booksWidget.getContentPanel());


		organizer.addLeftAlignedComponent(
				Box.createRigidArea(new Dimension((defaultSize.width / 2) - amountToSubtractFromLeftAndRightPanels, 0)));

		organizer.addVerticalFillerRow();
	}

	private void randomizeTheme()
	{
		MapSettings randomSettings = SettingsGenerator.generate(settings.customImagesPath);
		settings.coastShadingLevel = randomSettings.coastShadingLevel;
		settings.oceanEffectsLevel = randomSettings.oceanEffectsLevel;
		settings.concentricWaveCount = randomSettings.concentricWaveCount;
		settings.oceanEffect = randomSettings.oceanEffect;
		settings.riverColor = randomSettings.riverColor;
		settings.roadColor = randomSettings.roadColor;
		settings.coastShadingColor = randomSettings.coastShadingColor;
		settings.oceanEffectsColor = randomSettings.oceanEffectsColor;
		settings.coastlineColor = randomSettings.coastlineColor;
		settings.frayedBorder = randomSettings.frayedBorder;
		settings.frayedBorderSize = randomSettings.frayedBorderSize;
		settings.frayedBorderColor = randomSettings.frayedBorderColor;
		settings.frayedBorderBlurLevel = randomSettings.frayedBorderBlurLevel;
		settings.grungeWidth = randomSettings.grungeWidth;
		settings.generateBackground = randomSettings.generateBackground;
		settings.generateBackgroundFromTexture = randomSettings.generateBackgroundFromTexture;
		settings.colorizeOcean = randomSettings.colorizeOcean;
		settings.colorizeLand = randomSettings.colorizeLand;
		settings.backgroundTextureImage = randomSettings.backgroundTextureImage;
		settings.backgroundRandomSeed = randomSettings.backgroundRandomSeed;
		settings.oceanColor = randomSettings.oceanColor;
		settings.landColor = randomSettings.landColor;
		settings.regionBaseColor = randomSettings.regionBaseColor;
		settings.hueRange = randomSettings.hueRange;
		settings.saturationRange = randomSettings.saturationRange;
		settings.brightnessRange = randomSettings.brightnessRange;
		settings.titleFont = randomSettings.titleFont;
		settings.regionFont = randomSettings.regionFont;
		settings.mountainRangeFont = randomSettings.mountainRangeFont;
		settings.otherMountainsFont = randomSettings.otherMountainsFont;
		settings.riverFont = randomSettings.riverFont;
		settings.boldBackgroundColor = randomSettings.boldBackgroundColor;
		settings.textColor = randomSettings.textColor;
		settings.drawBoldBackground = randomSettings.drawBoldBackground;
		settings.drawRegionColors = randomSettings.drawRegionColors;
		settings.regionsRandomSeed = randomSettings.regionsRandomSeed;
		settings.drawBorder = randomSettings.drawBorder;
		settings.borderType = randomSettings.borderType;
		settings.borderWidth = randomSettings.borderWidth;
		settings.lineStyle = randomSettings.lineStyle;

		handleMapChange();
	}

	private void randomizeLand()
	{
		MapSettings randomSettings = SettingsGenerator.generate(null);
		settings.randomSeed = randomSettings.randomSeed;
		handleMapChange();
	}

	private void createMapEditingPanel()
	{
		BufferedImage placeHolder = AwtFactory.unwrap(ImageHelper.createPlaceholderImage(new String[] { "绘制中..." }));
		mapEditingPanel = new MapEditingPanel(placeHolder);

		mapEditingPanelContainer = new JPanel();
		mapEditingPanelContainer.setLayout(new FlowLayout(FlowLayout.CENTER));
		mapEditingPanelContainer.add(mapEditingPanel);
	}

	private void createMapUpdater()
	{
		updater = new MapUpdater(false)
		{

			@Override
			protected void onBeginDraw()
			{
			}

			@Override
			public MapSettings getSettingsFromGUI()
			{
				MapSettings settings = NewSettingsDialog.this.getSettingsFromGUI();

				// This is only the maximum size because I'm passing in
				// maxDimensions to MapCreator.create.
				settings.resolution = 1.0;

				return settings;
			}

			@Override
			protected void onFinishedDrawing(Image map, boolean anotherDrawIsQueued, int borderWidthAsDrawn,
					Rectangle incrementalChangeArea, List<String> warningMessages)
			{
				mapEditingPanel.setImage(AwtFactory.unwrap(map));

				if (!anotherDrawIsQueued)
				{
					enableOrDisableProgressBar(false);
				}

				NewSettingsDialog.this.revalidate();
				NewSettingsDialog.this.repaint();
			}

			@Override
			protected void onFailedToDraw()
			{
				enableOrDisableProgressBar(false);
			}

			@Override
			protected MapEdits getEdits()
			{
				return settings.edits;
			}

			@Override
			protected Image getCurrentMapForIncrementalUpdate()
			{
				return AwtFactory.wrap(mapEditingPanel.mapFromMapCreator);
			}

		};
		updater.setEnabled(false);
	}

	private nortantis.geom.Dimension getMapDrawingAreaSize()
	{
		final int additionalWidthToRemoveIDontKnowWhereItsCommingFrom = 4;
		return new nortantis.geom.Dimension(
				(mapEditingPanelContainer.getSize().width - additionalWidthToRemoveIDontKnowWhereItsCommingFrom) * mapEditingPanel.osScale,
				(mapEditingPanelContainer.getSize().height - additionalWidthToRemoveIDontKnowWhereItsCommingFrom)
						* mapEditingPanel.osScale);

	}

	private void loadSettingsIntoGUI(MapSettings settings)
	{
		dimensionsComboBox.setSelectedIndex(getDimensionIndexFromDimensions(settings.generatedWidth, settings.generatedHeight));
		worldSizeSlider.setValue(settings.worldSize);
		edgeLandToWaterProbSlider.setValue((int) (settings.edgeLandToWaterProbability * 100));
		centerLandToWaterProbSlider.setValue((int) (settings.centerLandToWaterProbability * 100));
		if (settings.drawRegionColors)
		{
			landColoringMethodComboBox.setSelectedItem(LandColoringMethod.ColorPoliticalRegions);
		}
		else
		{
			landColoringMethodComboBox.setSelectedItem(LandColoringMethod.SingleColor);
		}

		cityFrequencySlider.setValue((int) (settings.cityProbability * cityFrequencySliderScale));
		initializeCityTypeOptions();

		booksWidget.checkSelectedBooks(settings.books);

		updatePathDisplay();
	}

	private MapSettings getSettingsFromGUI()
	{
		MapSettings resultSettings = settings.deepCopy();
		resultSettings.worldSize = worldSizeSlider.getValue();
		resultSettings.edgeLandToWaterProbability = edgeLandToWaterProbSlider.getValue() / 100.0;
		resultSettings.centerLandToWaterProbability = centerLandToWaterProbSlider.getValue() / 100.0;

		Dimension generatedDimensions = getGeneratedBackgroundDimensionsFromGUI();
		resultSettings.generatedWidth = (int) generatedDimensions.getWidth();
		resultSettings.generatedHeight = (int) generatedDimensions.getHeight();

		resultSettings.drawRegionColors = landColoringMethodComboBox.getSelectedItem().equals(LandColoringMethod.ColorPoliticalRegions);

		resultSettings.books = booksWidget.getSelectedBooks();

		resultSettings.cityProbability = cityFrequencySlider.getValue() / cityFrequencySliderScale;
		resultSettings.cityIconTypeName = (String) cityIconsTypeComboBox.getSelectedItem();

		return resultSettings;
	}

	private Dimension getGeneratedBackgroundDimensionsFromGUI()
	{
		String selected = (String) dimensionsComboBox.getSelectedItem();
		return parseGenerateBackgroundDimensionsFromDropdown(selected);
	}

	public static Dimension parseGenerateBackgroundDimensionsFromDropdown(String selected)
	{
		selected = selected.substring(0, selected.indexOf("("));
		String[] parts = selected.split("x");
		return new Dimension(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
	}

	private int getDimensionIndexFromDimensions(int generatedWidth, int generatedHeight)
	{
		for (int i : new Range(dimensionsComboBox.getItemCount()))
		{
			Dimension dim = parseGenerateBackgroundDimensionsFromDropdown(dimensionsComboBox.getItemAt(i));
			if (dim.getWidth() == generatedWidth && dim.getHeight() == generatedHeight)
			{
				return i;
			}
		}
		throw new IllegalArgumentException("没有带尺寸的下拉菜单选项 " + generatedWidth + " x " + generatedHeight);
	}

	private void enableOrDisableProgressBar(boolean enable)
	{
		if (enable)
		{
			progressBarTimer.start();
		}
		else
		{
			progressBarTimer.stop();
			progressBar.setVisible(false);
		}

	}

	public void createMapChangeListener(Component component)
	{
		SwingHelper.addListener(component, () -> handleMapChange());
	}

	public void handleMapChange()
	{
		nortantis.geom.Dimension size = getMapDrawingAreaSize();
		if (size != null && size.width > 0.0 && size.height > 0.0)
		{
			updater.setMaxMapSize(getMapDrawingAreaSize());
			enableOrDisableProgressBar(true);
			updater.createAndShowMapFull();
		}
	}
}
