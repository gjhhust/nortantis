package nortantis.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;

import org.apache.commons.io.FilenameUtils;

import nortantis.BackgroundGenerator;
import nortantis.FractalBGGenerator;
import nortantis.FreeIconCollection;
import nortantis.IconDrawer;
import nortantis.IconType;
import nortantis.ImageAndMasks;
import nortantis.ImageCache;
import nortantis.MapCreator;
import nortantis.MapSettings;
import nortantis.MapSettings.LineStyle;
import nortantis.MapSettings.OceanEffect;
import nortantis.SettingsGenerator;
import nortantis.WorldGraph;
import nortantis.editor.CenterEdit;
import nortantis.editor.CenterTrees;
import nortantis.editor.FreeIcon;
import nortantis.geom.IntDimension;
import nortantis.geom.Point;
import nortantis.graph.voronoi.Center;
import nortantis.platform.Image;
import nortantis.platform.ImageType;
import nortantis.platform.awt.AwtFactory;
import nortantis.util.Counter;
import nortantis.util.ImageHelper;
import nortantis.util.ListMap;
import nortantis.util.Tuple2;
import nortantis.util.Tuple4;

@SuppressWarnings("serial")
public class ThemePanel extends JTabbedPane
{
	private MainWindow mainWindow;
	private JSlider coastShadingSlider;
	private JSlider oceanEffectsLevelSlider;
	private JSlider concentricWavesLevelSlider;
	private JRadioButton ripplesRadioButton;
	private JRadioButton shadeRadioButton;
	private JRadioButton concentricWavesButton;
	private JRadioButton fadingConcentricWavesButton;
	private JPanel coastShadingColorDisplay;
	private JPanel coastlineColorDisplay;
	private JSlider coastShadingTransparencySlider;
	private RowHider coastShadingTransparencyHider;
	private JPanel oceanEffectsColorDisplay;
	private JPanel riverColorDisplay;
	private JCheckBox enableTextCheckBox;
	private JPanel grungeColorDisplay;
	private JTextField backgroundSeedTextField;
	private JRadioButton rdbtnGeneratedFromTexture;
	private JRadioButton rdbtnFractal;
	private BGColorPreviewPanel oceanDisplayPanel;
	private BGColorPreviewPanel landDisplayPanel;
	private ActionListener backgroundImageButtonGroupListener;
	private Dimension backgroundDisplaySize = new Dimension(150, 110);
	private JComboBox<LandColoringMethod> landColoringMethodComboBox;
	private JSlider grungeSlider;
	private JTextField textureImageFilename;
	private JCheckBox colorizeLandCheckbox;
	private JCheckBox colorizeOceanCheckbox;
	private JButton btnChooseLandColor;
	private JButton btnChooseOceanColor;
	private JButton btnNewBackgroundSeed;
	private ItemListener colorizeCheckboxListener;
	private JComboBox<String> borderTypeComboBox;
	private JSlider borderWidthSlider;
	private JCheckBox drawBorderCheckbox;
	private JSlider frayedEdgeSizeSlider;
	private JSlider frayedEdgeShadingSlider;
	private JCheckBox frayedEdgeCheckbox;
	private JButton btnChooseCoastShadingColor;
	private JRadioButton jaggedLinesButton;
	private JRadioButton splinesLinesButton;
	private JRadioButton splinesWithSmoothedCoastlinesButton;
	private ActionListener oceanEffectsListener;
	private JLabel titleFontDisplay;
	private JLabel regionFontDisplay;
	private JLabel mountainRangeFontDisplay;
	private JLabel otherMountainsFontDisplay;
	private JLabel riverFontDisplay;
	private JPanel textColorDisplay;
	private JPanel boldBackgroundColorDisplay;
	private JCheckBox drawBoldBackgroundCheckbox;
	private RowHider textureImageHider;
	private RowHider colorizeOceanCheckboxHider;
	private RowHider colorizeLandCheckboxHider;
	private RowHider landColorHider;
	private JButton btnChooseBoldBackgroundColor;
	private JButton btnTitleFont;
	private JButton btnRegionFont;
	private JButton btnMountainRangeFont;
	private JButton btnOtherMountainsFont;
	private JButton btnRiverFont;
	private JButton btnChooseTextColor;
	private ActionListener enableTextCheckboxActionListener;
	private ActionListener frayedEdgeCheckboxActionListener;
	private RowHider coastShadingColorHider;
	private RowHider coastShadingColorDisabledMessageHider;
	private JCheckBox drawGrungeCheckbox;
	private ActionListener drawGrungeCheckboxActionListener;
	private JButton grungeColorChooseButton;
	private JCheckBox drawOceanEffectsInLakesCheckbox;
	private JSlider treeHeightSlider;
	private boolean enableSizeSliderListeners;
	private JSlider mountainScaleSlider;
	private JSlider hillScaleSlider;
	private JSlider duneScaleSlider;
	private JSlider cityScaleSlider;


	public ThemePanel(MainWindow mainWindow)
	{
		this.mainWindow = mainWindow;
	
		setPreferredSize(new Dimension(SwingHelper.sidePanelPreferredWidth, mainWindow.getContentPane().getHeight()));
		setMinimumSize(new Dimension(SwingHelper.sidePanelMinimumWidth, getMinimumSize().height));
	
		addTab("背景", createBackgroundPanel(mainWindow));  // "Background"翻译为"背景"
		addTab("边框", createBorderPanel(mainWindow));      // "Border"翻译为"边框"
		addTab("特效", createEffectsPanel(mainWindow));      // "Effects"翻译为"特效"
		addTab("字体", createFontsPanel(mainWindow));        // "Fonts"翻译为"字体"
	}
	

	private Component createBackgroundPanel(MainWindow mainWindow)
	{
		GridBagOrganizer organizer = new GridBagOrganizer();
		JPanel backgroundPanel = organizer.panel;

		backgroundImageButtonGroupListener = new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				updateBackgroundAndRegionFieldStates(mainWindow);
				updateBackgroundImageDisplays();
				handleFullRedraw();
			}
		};

		rdbtnFractal = new JRadioButton("分形噪声");  // "Fractal noise"翻译为"分形噪声"
		rdbtnFractal.addActionListener(backgroundImageButtonGroupListener);
		
		rdbtnGeneratedFromTexture = new JRadioButton("从纹理生成");  // "Generated from texture"翻译为"从纹理生成"
		rdbtnGeneratedFromTexture.addActionListener(backgroundImageButtonGroupListener);
		
		ButtonGroup backgroundImageButtonGroup = new ButtonGroup();
		backgroundImageButtonGroup.add(rdbtnGeneratedFromTexture);
		backgroundImageButtonGroup.add(rdbtnFractal);
		
		organizer.addLabelAndComponentsVertical("背景:", "选择生成背景图像的方式。",
				Arrays.asList(rdbtnFractal, rdbtnGeneratedFromTexture));
		

		textureImageFilename = new JTextField();
		textureImageFilename.getDocument().addDocumentListener(new DocumentListener()
		{
			public void changedUpdate(DocumentEvent e)
			{
				updateBackgroundImageDisplays();
				if (new File(textureImageFilename.getText()).exists())
				{
					handleFullRedraw();
				}
			}

			public void removeUpdate(DocumentEvent e)
			{
				updateBackgroundImageDisplays();
				if (new File(textureImageFilename.getText()).exists())
				{
					handleFullRedraw();
				}
			}

			public void insertUpdate(DocumentEvent e)
			{
				updateBackgroundImageDisplays();
				if (new File(textureImageFilename.getText()).exists())
				{
					handleFullRedraw();
				}
			}
		});

		JButton btnsBrowseTextureImage = new JButton("浏览");
		btnsBrowseTextureImage.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				String filename = chooseImageFile(backgroundPanel, FilenameUtils.getFullPath(textureImageFilename.getText()));
				if (filename != null)
				{
					textureImageFilename.setText(filename);
				}
			}
		});

		JPanel textureFileChooseButtonPanel = new JPanel();
		textureFileChooseButtonPanel.setLayout(new BoxLayout(textureFileChooseButtonPanel, BoxLayout.X_AXIS));
		textureFileChooseButtonPanel.add(btnsBrowseTextureImage);
		textureFileChooseButtonPanel.add(Box.createHorizontalGlue());

		textureImageHider = organizer.addLabelAndComponentsVertical("纹理图像:",
				"用于随机生成背景的纹理图像。",
				Arrays.asList(textureImageFilename, Box.createVerticalStrut(5), textureFileChooseButtonPanel));

		backgroundSeedTextField = new JTextField();
		backgroundSeedTextField.setText(String.valueOf(Math.abs(new Random().nextInt())));
		backgroundSeedTextField.setColumns(10);
		backgroundSeedTextField.getDocument().addDocumentListener(new DocumentListener()
		{
			public void changedUpdate(DocumentEvent e)
			{
				updateBackgroundImageDisplays();
				handleFullRedraw();
			}

			public void removeUpdate(DocumentEvent e)
			{
				updateBackgroundImageDisplays();
				if (!backgroundSeedTextField.getText().isEmpty())
				{
					handleFullRedraw();
				}
			}

			public void insertUpdate(DocumentEvent e)
			{
				updateBackgroundImageDisplays();
				handleFullRedraw();
			}
		});

		btnNewBackgroundSeed = new JButton("新种子");
		btnNewBackgroundSeed.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent arg0)
			{
				backgroundSeedTextField.setText(String.valueOf(Math.abs(new Random().nextInt())));
				updateBackgroundImageDisplays();
			}
		});
		btnNewBackgroundSeed.setToolTipText("生成新的随机种子.");
		organizer.addLabelAndComponentsHorizontal("随机种子:",
				"用于生成背景图像的随机种子。 请注意，背景纹理也会根据"
						+ " 绘制的分辨率。",
				Arrays.asList(backgroundSeedTextField, btnNewBackgroundSeed));

		organizer.addSeperator();
		colorizeLandCheckbox = new JCheckBox("陆地颜色");
		colorizeLandCheckbox
				.setToolTipText("是否将土地纹理更改为自定义颜色，而不是使用纹理图像的颜色");
		colorizeLandCheckboxHider = organizer.addLeftAlignedComponent(colorizeLandCheckbox);

		landColoringMethodComboBox = new JComboBox<LandColoringMethod>();
		for (LandColoringMethod method : LandColoringMethod.values())
		{
			landColoringMethodComboBox.addItem(method);
		}

		landColoringMethodComboBox.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				handleLandColoringMethodChanged();
				handleFullRedraw();
			}
		});
		organizer.addLabelAndComponent("土地着色法:", "如何为土地着色.", landColoringMethodComboBox);

		colorizeCheckboxListener = new ItemListener()
		{
			@Override
			public void itemStateChanged(ItemEvent e)
			{
				updateBackgroundAndRegionFieldStates(mainWindow);
				updateBackgroundImageDisplays();
				handleFullRedraw();
			}
		};

		landDisplayPanel = new BGColorPreviewPanel();
		landDisplayPanel.setLayout(null);
		landDisplayPanel.setPreferredSize(backgroundDisplaySize);
		landDisplayPanel.setMinimumSize(backgroundDisplaySize);
		landDisplayPanel.setBackground(Color.BLACK);

		btnChooseLandColor = new JButton("Choose");
		btnChooseLandColor.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				JColorChooser colorChooser = SwingHelper.createColorChooserWithOnlyGoodPanels(landDisplayPanel.getColor());

				colorChooser.getSelectionModel().addChangeListener(landDisplayPanel);
				colorChooser.setPreviewPanel(new JPanel());
				landDisplayPanel.setColorChooser(colorChooser);
				BGColorCancelHandler cancelHandler = new BGColorCancelHandler(landDisplayPanel.getColor(), landDisplayPanel);
				ActionListener okHandler = new ActionListener()
				{
					@Override
					public void actionPerformed(ActionEvent e)
					{
						landDisplayPanel.finishSelectingColor();
						handleFullRedraw();
					}
				};
				Dialog dialog = JColorChooser.createDialog(mainWindow, "土地颜色", false, colorChooser, okHandler, cancelHandler);
				dialog.setVisible(true);
			}
		});

		{
			JPanel container = new JPanel();
			container.setLayout(new FlowLayout());
			container.add(landDisplayPanel);
			btnChooseLandColor.setAlignmentX(CENTER_ALIGNMENT);

			landColorHider = organizer.addLabelAndComponentsVertical("土地颜色:", "土地背景的颜色。",
					Arrays.asList(container, Box.createVerticalStrut(5), btnChooseLandColor));
		}

		organizer.addSeperator();
		colorizeOceanCheckbox = new JCheckBox("给海洋着色");  // "Color ocean"翻译为"给海洋着色"
		colorizeOceanCheckbox.setToolTipText("是否将海洋纹理更改为自定义颜色，而不是使用纹理图像的颜色。");  // 汉化工具提示
		
		colorizeOceanCheckboxHider = organizer.addLeftAlignedComponent(colorizeOceanCheckbox);

		oceanDisplayPanel = new BGColorPreviewPanel();
		oceanDisplayPanel.setLayout(null);
		oceanDisplayPanel.setPreferredSize(backgroundDisplaySize);
		oceanDisplayPanel.setMinimumSize(backgroundDisplaySize);
		oceanDisplayPanel.setBackground(Color.BLACK);

		btnChooseOceanColor = new JButton("选择");
		btnChooseOceanColor.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent arg0)
			{
				JColorChooser colorChooser = SwingHelper.createColorChooserWithOnlyGoodPanels(oceanDisplayPanel.getColor());

				colorChooser.getSelectionModel().addChangeListener(oceanDisplayPanel);
				colorChooser.setPreviewPanel(new JPanel());
				oceanDisplayPanel.setColorChooser(colorChooser);
				BGColorCancelHandler cancelHandler = new BGColorCancelHandler(oceanDisplayPanel.getColor(), oceanDisplayPanel);
				ActionListener okHandler = new ActionListener()
				{
					@Override
					public void actionPerformed(ActionEvent e)
					{
						oceanDisplayPanel.finishSelectingColor();
						handleFullRedraw();
					}
				};
				Dialog dialog = JColorChooser.createDialog(mainWindow, "海洋颜色", false, colorChooser, okHandler, cancelHandler);
				dialog.setVisible(true);
			}
		});

		{
			JPanel container = new JPanel();
			container.setLayout((new FlowLayout()));
			container.add(oceanDisplayPanel);
			btnChooseOceanColor.setAlignmentX(CENTER_ALIGNMENT);

			organizer.addLabelAndComponentsVertical("海洋颜色:", "The color of the ocean.",
					Arrays.asList(container, Box.createVerticalStrut(5), btnChooseOceanColor));
		}

		organizer.addVerticalFillerRow();
		return organizer.createScrollPane();
	}

	private Component createBorderPanel(MainWindow mainWindow)
	{
		GridBagOrganizer organizer = new GridBagOrganizer();
		JPanel borderPanel = organizer.panel;

		drawBorderCheckbox = new JCheckBox("创建边框");  // "Create border"翻译为"创建边框"
		drawBorderCheckbox.setToolTipText("选中时，会在地图周围绘制边框。");  // 汉化工具提示
		drawBorderCheckbox.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				handleEnablingAndDisabling();
				handleFullRedraw();
			}
		});
		organizer.addLeftAlignedComponent(drawBorderCheckbox);

		borderTypeComboBox = new JComboBox<String>();
		createMapChangeListenerForFullRedraw(borderTypeComboBox);
		organizer.addLabelAndComponent("边框类型:", "绘制边框时使用的图像集", borderTypeComboBox);  // 汉化标签和描述

		{
			borderWidthSlider = new JSlider();
			borderWidthSlider.setToolTipText("");
			borderWidthSlider.setValue(100);
			borderWidthSlider.setSnapToTicks(false);
			borderWidthSlider.setPaintTicks(true);
			borderWidthSlider.setPaintLabels(true);
			borderWidthSlider.setMinorTickSpacing(50);
			borderWidthSlider.setMaximum(600);
			borderWidthSlider.setMajorTickSpacing(200);
			createMapChangeListenerForFullRedraw(borderWidthSlider);
			SwingHelper.setSliderWidthForSidePanel(borderWidthSlider);
			organizer.addLabelAndComponent("边框宽度:", 
					"边框的像素宽度，根据地图绘制时的分辨率进行缩放。", borderWidthSlider);  // 汉化边框宽度描述
		}
		organizer.addHorizontalSpacerRowToHelpComponentAlignment(0.6);

		organizer.addSeperator();
		frayedEdgeCheckbox = new JCheckBox("磨损边缘");  // "Fray edges"翻译为"磨损边缘"
		frayedEdgeCheckbox.setToolTipText("是否磨损地图的边缘。");  // 汉化工具提示
		frayedEdgeCheckboxActionListener = new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				handleEnablingAndDisabling();
				handleFrayedEdgeOrGrungeChange();
			}
		};
		frayedEdgeCheckbox.addActionListener(frayedEdgeCheckboxActionListener);
		organizer.addLeftAlignedComponent(frayedEdgeCheckbox);

		frayedEdgeShadingSlider = new JSlider();
		frayedEdgeShadingSlider.setValue(30);
		frayedEdgeShadingSlider.setPaintTicks(true);
		frayedEdgeShadingSlider.setPaintLabels(true);
		frayedEdgeShadingSlider.setMinorTickSpacing(50);
		frayedEdgeShadingSlider.setMaximum(500);
		frayedEdgeShadingSlider.setMajorTickSpacing(100);
		createMapChangeListenerForFrayedEdgeOrGrungeChange(frayedEdgeShadingSlider);
		SwingHelper.setSliderWidthForSidePanel(frayedEdgeShadingSlider);
		organizer.addLabelAndComponent("磨损阴影宽度:", 
				"绘制在磨损边缘周围的阴影宽度。使用的颜色是磨损颜色。", frayedEdgeShadingSlider);  // 汉化磨损阴影描述

		frayedEdgeSizeSlider = new JSlider();
		frayedEdgeSizeSlider.setPaintTicks(true);
		frayedEdgeSizeSlider.setPaintLabels(true);
		frayedEdgeSizeSlider.setMinorTickSpacing(1);
		frayedEdgeSizeSlider.setMaximum(SettingsGenerator.maxFrayedEdgeSizeForUI);
		frayedEdgeSizeSlider.setMinimum(1);
		frayedEdgeSizeSlider.setMajorTickSpacing(2);
		createMapChangeListenerForFrayedEdgeOrGrungeChange(frayedEdgeSizeSlider);
		SwingHelper.setSliderWidthForSidePanel(frayedEdgeSizeSlider);
		organizer.addLabelAndComponent("磨损大小:", 
				"确定创建磨损边框时使用的多边形数量。更高的值使磨损更大。", frayedEdgeSizeSlider);  // 汉化磨损大小描述

		organizer.addSeperator();

		drawGrungeCheckbox = new JCheckBox("绘制磨损");  // "Draw grunge"翻译为"绘制磨损"
		drawGrungeCheckbox.setToolTipText("是否在地图边缘绘制磨损。");  // 汉化工具提示
		drawGrungeCheckboxActionListener = new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				handleEnablingAndDisabling();
				handleFrayedEdgeOrGrungeChange();
			}
		};
		drawGrungeCheckbox.addActionListener(drawGrungeCheckboxActionListener);
		organizer.addLeftAlignedComponent(drawGrungeCheckbox);

		grungeColorDisplay = SwingHelper.createColorPickerPreviewPanel();

		grungeColorChooseButton = new JButton("选择");  // "Choose"翻译为"选择"
		grungeColorChooseButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent arg0)
			{
				SwingHelper.showColorPicker(borderPanel, grungeColorDisplay, "磨损颜色", () -> handleFrayedEdgeOrGrungeChange());  // 汉化对话框标题
			}
		});
		organizer.addLabelAndComponentsHorizontal("边缘/磨损颜色:", "磨损和磨损边缘阴影将使用此颜色", 
				Arrays.asList(grungeColorDisplay, grungeColorChooseButton), SwingHelper.colorPickerLeftPadding);  // 汉化边缘/磨损颜色描述

		grungeSlider = new JSlider();
		grungeSlider.setValue(0);
		grungeSlider.setPaintTicks(true);
		grungeSlider.setPaintLabels(true);
		grungeSlider.setMinorTickSpacing(250);
		grungeSlider.setMaximum(2000);
		grungeSlider.setMajorTickSpacing(1000);
		createMapChangeListenerForFrayedEdgeOrGrungeChange(grungeSlider);
		SwingHelper.setSliderWidthForSidePanel(grungeSlider);
		organizer.addLabelAndComponent("磨损宽度:", "确定地图边缘的磨损宽度。0表示没有磨损。", 
				grungeSlider);  // 汉化磨损宽度描述

		organizer.addVerticalFillerRow();
		return organizer.createScrollPane();
	}


	private Component createEffectsPanel(MainWindow mainWindow)
	{
		GridBagOrganizer organizer = new GridBagOrganizer();
	
		JPanel effectsPanel = organizer.panel;
	
		jaggedLinesButton = new JRadioButton("锯齿状");  // "Jagged"翻译为"锯齿状"
		createMapChangeListenerForFullRedraw(jaggedLinesButton);
		splinesLinesButton = new JRadioButton("样条线");  // "Splines"翻译为"样条线"
		createMapChangeListenerForFullRedraw(splinesLinesButton);
		splinesWithSmoothedCoastlinesButton = new JRadioButton("带平滑海岸线的样条线");  // "Splines with smoothed coastlines"翻译为"带平滑海岸线的样条线"
		createMapChangeListenerForFullRedraw(splinesWithSmoothedCoastlinesButton);
		
		ButtonGroup lineStyleButtonGroup = new ButtonGroup();
		lineStyleButtonGroup.add(jaggedLinesButton);
		lineStyleButtonGroup.add(splinesLinesButton);
		lineStyleButtonGroup.add(splinesWithSmoothedCoastlinesButton);
		
		organizer.addLabelAndComponentsVertical("线条样式:",  // "Line style:"翻译为"线条样式:"
				"绘制海岸线、湖岸和区域边界时使用的线条样式",  // 汉化描述
				Arrays.asList(jaggedLinesButton, splinesLinesButton, splinesWithSmoothedCoastlinesButton));
		organizer.addSeperator();
	
		coastlineColorDisplay = SwingHelper.createColorPickerPreviewPanel();
	
		JButton buttonChooseCoastlineColor = new JButton("选择");  // "Choose"翻译为"选择"
		buttonChooseCoastlineColor.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				SwingHelper.showColorPicker(effectsPanel, coastlineColorDisplay, "海岸线颜色", () -> handleTerrainChange());  // "Coastline Color"翻译为"海岸线颜色"
			}
		});
		organizer.addLabelAndComponentsHorizontal("海岸线颜色:", "海岸线的颜色",  // "Coastline color:"翻译为"海岸线颜色:"
				Arrays.asList(coastlineColorDisplay, buttonChooseCoastlineColor), SwingHelper.colorPickerLeftPadding);
	
		coastShadingColorDisplay = SwingHelper.createColorPickerPreviewPanel();
	
		btnChooseCoastShadingColor = new JButton("选择");  // "Choose"翻译为"选择"
		btnChooseCoastShadingColor.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				SwingHelper.showColorPicker(effectsPanel, coastShadingColorDisplay, "海岸阴影颜色", () ->  // "Coast Shading Color"翻译为"海岸阴影颜色"
				{
					updateCoastShadingTransparencySliderFromCoastShadingColorDisplay();
					handleTerrainChange();
				});
			}
		});
		String coastShadingColorLabelText = "海岸阴影颜色:";  // "Coast shading color:"翻译为"海岸阴影颜色:"
		coastShadingColorHider = organizer.addLabelAndComponentsHorizontal(coastShadingColorLabelText,
				"靠近海岸的土地将被此颜色阴影覆盖。支持透明度。",
				Arrays.asList(coastShadingColorDisplay, btnChooseCoastShadingColor), SwingHelper.colorPickerLeftPadding);
	
		final String message = "<html>已禁用，因为土地着色方法为 '" + LandColoringMethod.ColorPoliticalRegions + "'.<html>";  // 汉化内容
		coastShadingColorDisabledMessageHider = organizer.addLabelAndComponent(coastShadingColorLabelText, "", new JLabel(message));
		coastShadingColorDisabledMessageHider.setVisible(false);
	
		{
			coastShadingTransparencySlider = new JSlider(0, 100);
			final int initialValue = 0;
			coastShadingTransparencySlider.setValue(initialValue);
			SwingHelper.setSliderWidthForSidePanel(coastShadingTransparencySlider);
			SliderWithDisplayedValue sliderWithDisplay = new SliderWithDisplayedValue(coastShadingTransparencySlider, () ->
			{
				updateCoastShadingColorDisplayFromCoastShadingTransparencySlider();
				handleTerrainChange();
			});
			coastShadingTransparencyHider = sliderWithDisplay.addToOrganizer(organizer, "海岸阴影透明度:",  // "Coast shading transparency:"翻译为"海岸阴影透明度:"
					"靠近海岸的土地阴影的透明度");
		}
	
		coastShadingSlider = new JSlider();
		coastShadingSlider.setValue(30);
		coastShadingSlider.setPaintTicks(true);
		coastShadingSlider.setPaintLabels(true);
		coastShadingSlider.setMinorTickSpacing(5);
		coastShadingSlider.setMaximum(100);
		coastShadingSlider.setMajorTickSpacing(20);
		createMapChangeListenerForTerrainChange(coastShadingSlider);
		SwingHelper.setSliderWidthForSidePanel(coastShadingSlider);
		organizer.addLabelAndComponent("海岸阴影宽度:",  // "Coast shading width:"翻译为"海岸阴影宽度:"
				"从海岸向内阴影的距离。如果绘制区域边界也适用。", coastShadingSlider);
	
		ButtonGroup oceanEffectButtonGroup = new ButtonGroup();
	
		concentricWavesButton = new JRadioButton("同心波");  // "Concentric waves"翻译为"同心波"
		oceanEffectButtonGroup.add(concentricWavesButton);
		oceanEffectsListener = new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				concentricWavesLevelSlider.setVisible(concentricWavesButton.isSelected() || fadingConcentricWavesButton.isSelected());
				oceanEffectsLevelSlider.setVisible(ripplesRadioButton.isSelected() || shadeRadioButton.isSelected());
				handleTerrainChange();
			}
		};
		concentricWavesButton.addActionListener(oceanEffectsListener);
	
		fadingConcentricWavesButton = new JRadioButton("渐变同心波");  // "Fading concentric waves"翻译为"渐变同心波"
		oceanEffectButtonGroup.add(fadingConcentricWavesButton);
		fadingConcentricWavesButton.addActionListener(oceanEffectsListener);
	
		ripplesRadioButton = new JRadioButton("涟漪");  // "Ripples"翻译为"涟漪"
		oceanEffectButtonGroup.add(ripplesRadioButton);
		ripplesRadioButton.addActionListener(oceanEffectsListener);
	
		shadeRadioButton = new JRadioButton("阴影");  // "Shade"翻译为"阴影"
		oceanEffectButtonGroup.add(shadeRadioButton);
		shadeRadioButton.addActionListener(oceanEffectsListener);
		organizer.addLabelAndComponentsVertical("海洋效果类型:",  // "Ocean effects type:"翻译为"海洋效果类型:"
				"如何在海岸线附近绘制波浪或阴影",
				Arrays.asList(concentricWavesButton, fadingConcentricWavesButton, ripplesRadioButton, shadeRadioButton));
	
		oceanEffectsColorDisplay = SwingHelper.createColorPickerPreviewPanel();
	
		JButton btnChooseOceanEffectsColor = new JButton("选择");  // "Choose"翻译为"选择"
		btnChooseOceanEffectsColor.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				SwingHelper.showColorPicker(effectsPanel, oceanEffectsColorDisplay, "海洋效果颜色", () -> handleTerrainChange());  // "Ocean Effects Color"翻译为"海洋效果颜色"
			}
		});
		btnChooseOceanEffectsColor.setToolTipText("选择海岸附近海洋效果的颜色。支持透明度。");
		organizer.addLabelAndComponentsHorizontal("海洋效果颜色:", "海洋效果的颜色。支持透明度。",
				Arrays.asList(oceanEffectsColorDisplay, btnChooseOceanEffectsColor), SwingHelper.colorPickerLeftPadding);
	
		concentricWavesLevelSlider = new JSlider();
		concentricWavesLevelSlider.setMinimum(1);
		concentricWavesLevelSlider.setPaintTicks(true);
		concentricWavesLevelSlider.setPaintLabels(true);
		concentricWavesLevelSlider.setMinorTickSpacing(1);
		concentricWavesLevelSlider.setMaximum(SettingsGenerator.maxConcentricWaveCountInEditor);
		concentricWavesLevelSlider.setMajorTickSpacing(1);
		createMapChangeListenerForTerrainChange(concentricWavesLevelSlider);
		SwingHelper.setSliderWidthForSidePanel(concentricWavesLevelSlider);
	
		oceanEffectsLevelSlider = new JSlider();
		oceanEffectsLevelSlider.setMinorTickSpacing(5);
		oceanEffectsLevelSlider.setValue(2);
		oceanEffectsLevelSlider.setPaintTicks(true);
		oceanEffectsLevelSlider.setPaintLabels(true);
		oceanEffectsLevelSlider.setMajorTickSpacing(20);
		createMapChangeListenerForTerrainChange(oceanEffectsLevelSlider);
		SwingHelper.setSliderWidthForSidePanel(oceanEffectsLevelSlider);
		organizer.addLabelAndComponentsVertical("海洋效果宽度:",  // "Ocean effects width:"翻译为"海洋效果宽度:"
				"海洋效果应延伸多远，距离海岸线。",
				Arrays.asList(concentricWavesLevelSlider, oceanEffectsLevelSlider));
	
		drawOceanEffectsInLakesCheckbox = new JCheckBox("在湖泊中绘制海洋波浪/阴影。");  // "Draw ocean waves/shading in lakes."翻译为"在湖泊中绘制海洋波浪/阴影。"
		createMapChangeListenerForTerrainChange(drawOceanEffectsInLakesCheckbox);
		organizer.addLeftAlignedComponent(drawOceanEffectsInLakesCheckbox);
		organizer.addSeperator();
	
		riverColorDisplay = SwingHelper.createColorPickerPreviewPanel();
	
		JButton riverColorChooseButton = new JButton("选择");  // "Choose"翻译为"选择"
		riverColorChooseButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				SwingHelper.showColorPicker(effectsPanel, riverColorDisplay, "河流颜色", () -> handleTerrainChange());  // "River Color"翻译为"河流颜色"
			}
		});
		organizer.addLabelAndComponentsHorizontal("河流颜色:", "河流将绘制为此颜色。",
				Arrays.asList(riverColorDisplay, riverColorChooseButton), SwingHelper.colorPickerLeftPadding);
	
		organizer.addSeperator();
		mountainScaleSlider = new JSlider(minScaleSliderValue, maxScaleSliderValue);
		mountainScaleSlider.setMajorTickSpacing(2);
		mountainScaleSlider.setMinorTickSpacing(1);
		mountainScaleSlider.setPaintTicks(true);
		mountainScaleSlider.setPaintLabels(true);
		SwingHelper.setSliderWidthForSidePanel(mountainScaleSlider);
		SwingHelper.addListener(mountainScaleSlider, () ->
		{
			if (enableSizeSliderListeners)
			{
				unselectAnyIconBeingEdited();
				repositionMountainsForNewScaleAndTriggerTerrainChange();
			}
		});
		enableSizeSliderListeners = true;
		organizer.addLabelAndComponent("山脉大小:", "改变地图上所有山脉的大小", mountainScaleSlider);  // "Mountain size:"翻译为"山脉大小:"
	
		hillScaleSlider = new JSlider(minScaleSliderValue, maxScaleSliderValue);
		hillScaleSlider.setMajorTickSpacing(2);
		hillScaleSlider.setMinorTickSpacing(1);
		hillScaleSlider.setPaintTicks(true);
		hillScaleSlider.setPaintLabels(true);
		SwingHelper.setSliderWidthForSidePanel(hillScaleSlider);
		SwingHelper.addListener(hillScaleSlider, () ->
		{
			if (enableSizeSliderListeners)
			{
				unselectAnyIconBeingEdited();
				handleTerrainChange();
			}
		});
		organizer.addLabelAndComponent("丘陵大小:", "改变地图上所有丘陵的大小", hillScaleSlider);  // "Hill size:"翻译为"丘陵大小:"
	
		duneScaleSlider = new JSlider(minScaleSliderValue, maxScaleSliderValue);
		duneScaleSlider.setMajorTickSpacing(2);
		duneScaleSlider.setMinorTickSpacing(1);
		duneScaleSlider.setPaintTicks(true);
		duneScaleSlider.setPaintLabels(true);
		SwingHelper.setSliderWidthForSidePanel(duneScaleSlider);
		SwingHelper.addListener(duneScaleSlider, () ->
		{
			if (enableSizeSliderListeners)
			{
				unselectAnyIconBeingEdited();
				handleTerrainChange();
			}
		});
		organizer.addLabelAndComponent("沙丘大小:", "改变地图上所有沙丘的大小", duneScaleSlider);  // "Dune size:"翻译为"沙丘大小:"
	
		// If I change the maximum here, also update densityScale in IconDrawer.drawTreesForCenters.
		treeHeightSlider = new JSlider(minScaleSliderValue, maxScaleSliderValue);
		treeHeightSlider.setMajorTickSpacing(2);
		treeHeightSlider.setMinorTickSpacing(1);
		treeHeightSlider.setPaintTicks(true);
		treeHeightSlider.setPaintLabels(true);
		SwingHelper.setSliderWidthForSidePanel(treeHeightSlider);
		SwingHelper.addListener(treeHeightSlider, () ->
		{
			if (enableSizeSliderListeners)
			{
				unselectAnyIconBeingEdited();
				triggerRebuildAllAnchoredTrees();
				handleTerrainChange();
			}
		});
		enableSizeSliderListeners = true;
		organizer.addLabelAndComponent("树木高度:",  // "Tree height:"翻译为"树木高度:"
				"改变地图上所有树木的高度，并重新分配树木以保持森林密度", treeHeightSlider);
	
		cityScaleSlider = new JSlider(minScaleSliderValue, maxScaleSliderValue);
		cityScaleSlider.setMajorTickSpacing(2);
		cityScaleSlider.setMinorTickSpacing(1);
		cityScaleSlider.setPaintTicks(true);
		cityScaleSlider.setPaintLabels(true);
		SwingHelper.setSliderWidthForSidePanel(cityScaleSlider);
		SwingHelper.addListener(cityScaleSlider, () ->
		{
			if (enableSizeSliderListeners)
			{
				unselectAnyIconBeingEdited();
				handleTerrainChange();
			}
		});
		organizer.addLabelAndComponent("城市大小:", "改变地图上所有城市的大小", cityScaleSlider);  // "City size:"翻译为"城市大小:"
	
		organizer.addVerticalFillerRow();
		return organizer.createScrollPane();
	}
	
	private void unselectAnyIconBeingEdited()
	{
		if (mainWindow.toolsPanel != null && mainWindow.toolsPanel.currentTool != null
				&& mainWindow.toolsPanel.currentTool instanceof IconsTool)
		{
			((IconsTool) mainWindow.toolsPanel.currentTool).unselectAnyIconBeingEdited();
		}
	}
	
	private void triggerRebuildAllAnchoredTrees()
	{
		mainWindow.edits.freeIcons.doWithLock(() ->
		{
			Random rand = new Random();
			// Reassign the random seeds to all CenterTrees that still exist because they failed to create any trees in their previous
			// attempt
			// to draw. Doing this causes those center trees to possibly show up. Without it, they would gradually disappear as you changed
			// the
			// tree height slider, especially on the higher ends of the tree height values.
			// Also mark CenterTrees as not dormant so they will try to draw again.
			// Also remove CenterTrees that are not close to any trees that are visible so that they don't randomly pop up when you change
			// the
			// tree height slider.
			for (Map.Entry<Integer, CenterEdit> entry : mainWindow.edits.centerEdits.entrySet())
			{
				CenterTrees cTrees = entry.getValue().trees;
				if (cTrees != null)
				{
					if (mainWindow.edits.freeIcons.hasTrees(entry.getKey()))
					{
						// Visible trees override invisible ones.
						mainWindow.edits.centerEdits.put(entry.getKey(), entry.getValue().copyWithTrees(null));
					}
					else
					{
						if (hasVisibleTreeWithinDistance(entry.getKey(), cTrees.treeType, 3))
						{
							mainWindow.edits.centerEdits.put(entry.getKey(), entry.getValue()
									.copyWithTrees(new CenterTrees(cTrees.treeType, cTrees.density, rand.nextLong(), false)));
						}
						else
						{
							mainWindow.edits.centerEdits.put(entry.getKey(), entry.getValue().copyWithTrees(null));
						}
					}
				}
			}

			for (int centerIndex : mainWindow.edits.freeIcons.iterateTreeAnchors())
			{
				List<FreeIcon> trees = mainWindow.edits.freeIcons.getTrees(centerIndex);
				if (trees == null || trees.isEmpty())
				{
					continue;
				}

				String treeType = getMostCommonTreeType(trees);
				assert treeType != null;

				double density = trees.stream().mapToDouble(t -> t.density).average().getAsDouble();

				assert density > 0;

				CenterTrees cTrees = new CenterTrees(treeType, density, rand.nextLong());
				CenterEdit cEdit = mainWindow.edits.centerEdits.get(centerIndex);
				mainWindow.edits.centerEdits.put(centerIndex, cEdit.copyWithTrees(cTrees));
			}
		});
	}

	/**
	 * Recalculates where mountains attached to Centers should be positioned so that changing the mountain scale slider keeps the base of
	 * mountains in approximately the same location.
	 * 
	 * I didn't bother doing this with dunes or hills because they tend to be short anyway, and so I've anchored them to the centroid of
	 * centers rather the the bottom.
	 */
	private void repositionMountainsForNewScaleAndTriggerTerrainChange()
	{
		// This is a bit of a hack, but I only call innerRepositionMountainsForNewScaleWithIconDrawer when iconDrawer and graph are not null
		// rather than always call it in doWhenMapIsReadyForInteractions because for many drawing cases the graph and icon drawer are
		// available, and for those cases I don't want to do this step later because it causes trouble with undo points (the changes
		// From this method get mixed in with an undo point from a later actions from the user).
		IconDrawer iconDrawer = mainWindow.updater.mapParts.iconDrawer;
		WorldGraph graph = mainWindow.updater.mapParts.graph;
		if (iconDrawer != null && graph != null)
		{
			innerRepositionMountainsForNewScaleWithIconDrawer(graph, iconDrawer);
			handleTerrainChange();
		}
		else
		{
			mainWindow.updater.doWhenMapIsReadyForInteractions(() ->
			{
				IconDrawer iconDrawer2 = mainWindow.updater.mapParts.iconDrawer;
				WorldGraph graph2 = mainWindow.updater.mapParts.graph;
				if (iconDrawer2 != null && graph2 != null)
				{
					innerRepositionMountainsForNewScaleWithIconDrawer(graph2, iconDrawer2);
					handleTerrainChange();
				}
			});
		}
	}

	private void innerRepositionMountainsForNewScaleWithIconDrawer(WorldGraph graph, IconDrawer iconDrawer)
	{

		FreeIconCollection freeIcons = mainWindow.edits.freeIcons;
		double resolution = mainWindow.displayQualityScale;
		double mountainScale = getScaleForSliderValue(mountainScaleSlider.getValue());
		ListMap<String, ImageAndMasks> iconsByGroup = ImageCache.getInstance(mainWindow.customImagesPath)
				.getAllIconGroupsAndMasksForType(IconType.mountains);
		freeIcons.doWithLock(() ->
		{
			for (FreeIcon icon : freeIcons.iterateAnchoredNonTreeIcons())
			{
				if (icon.type == IconType.mountains)
				{
					if (!iconsByGroup.containsKey(icon.groupId))
					{
						// I don't think this should happen
						assert false;
						continue;
					}
					if (iconsByGroup.get(icon.groupId).isEmpty())
					{
						// I don't think this should happen
						assert false;
						continue;
					}
					Point loc = iconDrawer.getAnchoredMountainDrawPoint(graph.centers.get(icon.centerIndex), icon.groupId, icon.iconIndex,
							mountainScale, iconsByGroup);
					freeIcons.addOrReplace(icon.copyWithLocation(resolution, loc));
				}
			}

			// Do something similar for non-anchored mountains. In this case, we don't have the center the mountain was originally drawn on,
			// so we use the average center height to calculate approximately what the why offset of the image would have been.
			for (FreeIcon icon : freeIcons.iterateNonAnchoredIcons())
			{
				if (icon.type == IconType.mountains)
				{
					double yChange = mainWindow.updater.mapParts.iconDrawer.getUnanchoredMountainYChangeFromMountainScaleChange(icon,
							mountainScale);
					Point scaledLocation = icon.getScaledLocation(resolution);
					FreeIcon updated = icon.copyWithLocation(resolution, new Point(scaledLocation.x, scaledLocation.y + yChange));
					freeIcons.replace(icon, updated);
				}
			}
		});

	}

	private String getMostCommonTreeType(List<FreeIcon> trees)
	{
		Counter<String> counter = new Counter<>();
		trees.stream().forEach(tree -> counter.incrementCount(tree.groupId));
		return counter.argmax();
	}

	private boolean hasVisibleTreeWithinDistance(int centerStartIndex, String treeType, int maxSearchDistance)
	{
		MapEdits edits = mainWindow.edits;
		WorldGraph graph = mainWindow.updater.mapParts.graph;
		Center start = graph.centers.get(centerStartIndex);
		Center found = graph.breadthFirstSearchForGoal((c, distanceFromStart) ->
		{
			return distanceFromStart < maxSearchDistance;
		}, (c) ->
		{
			return edits.freeIcons.hasTrees(c.index);
		}, start);

		return found != null;
	}

	private boolean disableCoastShadingColorDisplayHandler = false;

	private void updateCoastShadingColorDisplayFromCoastShadingTransparencySlider()
	{
		if (!disableCoastShadingColorDisplayHandler)
		{
			Color background = coastShadingColorDisplay.getBackground();
			int alpha = (int) ((1.0 - coastShadingTransparencySlider.getValue() / 100.0) * 255);
			coastShadingColorDisplay.setBackground(new Color(background.getRed(), background.getGreen(), background.getBlue(), alpha));
		}
	}

	private void updateCoastShadingTransparencySliderFromCoastShadingColorDisplay()
	{
		coastShadingTransparencySlider.setValue((int) (((1.0 - coastShadingColorDisplay.getBackground().getAlpha() / 255.0) * 100)));
	}

	private Component createFontsPanel(MainWindow mainWindow)
	{
		GridBagOrganizer organizer = new GridBagOrganizer();
	
		JPanel fontsPanel = organizer.panel;
	
		enableTextCheckBox = new JCheckBox("启用文本");  // "Enable text"翻译为"启用文本"
		enableTextCheckBox.setToolTipText("启用/禁用绘制文本。未选中时，文本仍然存在，但不会显示。");  // 汉化工具提示
		organizer.addLeftAlignedComponent(enableTextCheckBox);
		organizer.addSeperator();
	
		Tuple2<JLabel, JButton> tupleTitle = organizer.addFontChooser("标题字体:", 70, () -> handleFontsChange());  // "Title font:"翻译为"标题字体:"
		titleFontDisplay = tupleTitle.getFirst();
		btnTitleFont = tupleTitle.getSecond();
	
		Tuple2<JLabel, JButton> tupleRegion = organizer.addFontChooser("区域字体:", 40, () -> handleFontsChange());  // "Region font:"翻译为"区域字体:"
		regionFontDisplay = tupleRegion.getFirst();
		btnRegionFont = tupleRegion.getSecond();
	
		Tuple2<JLabel, JButton> tupleMountainRange = organizer.addFontChooser("山脉字体:", 30, () -> handleFontsChange());  // "Mountain range font:"翻译为"山脉字体:"
		mountainRangeFontDisplay = tupleMountainRange.getFirst();
		btnMountainRangeFont = tupleMountainRange.getSecond();
	
		Tuple2<JLabel, JButton> tupleCitiesMountains = organizer.addFontChooser("城市/山脉字体:", 30, () -> handleFontsChange());  // "Cities/mountains font:"翻译为"城市/山脉字体:"
		otherMountainsFontDisplay = tupleCitiesMountains.getFirst();
		btnOtherMountainsFont = tupleCitiesMountains.getSecond();
	
		Tuple2<JLabel, JButton> tupleRiver = organizer.addFontChooser("河流/湖泊字体:", 30, () -> handleFontsChange());  // "River/lake font:"翻译为"河流/湖泊字体:"
		riverFontDisplay = tupleRiver.getFirst();
		btnRiverFont = tupleRiver.getSecond();
	
		organizer.addSeperator();
		textColorDisplay = SwingHelper.createColorPickerPreviewPanel();
	
		btnChooseTextColor = new JButton("选择");  // "Choose"翻译为"选择"
		btnChooseTextColor.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				SwingHelper.showColorPicker(fontsPanel, textColorDisplay, "文本颜色", () -> handleFontsChange());  // "Text Color"翻译为"文本颜色"
			}
		});
		organizer.addLabelAndComponentsHorizontal("文本颜色:", "", Arrays.asList(textColorDisplay, btnChooseTextColor),
				SwingHelper.colorPickerLeftPadding);
	
		organizer.addSeperator();
		drawBoldBackgroundCheckbox = new JCheckBox("区域和标题名称的粗体背景");  // "Bold background for region and title names"翻译为"区域和标题名称的粗体背景"
		drawBoldBackgroundCheckbox.setToolTipText("是否在区域和标题文本后绘制粗体字母以突出显示它们。");  // 汉化工具提示
		organizer.addLeftAlignedComponent(drawBoldBackgroundCheckbox);
	
		boldBackgroundColorDisplay = SwingHelper.createColorPickerPreviewPanel();
	
		btnChooseBoldBackgroundColor = new JButton("选择");  // "Choose"翻译为"选择"
		btnChooseBoldBackgroundColor.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				SwingHelper.showColorPicker(fontsPanel, boldBackgroundColorDisplay, "粗体背景颜色", () -> handleFontsChange());  // "Bold Background Color"翻译为"粗体背景颜色"
			}
		});
		organizer.addLabelAndComponentsHorizontal("粗体背景颜色:",  // "Bold background color:"翻译为"粗体背景颜色:"
				"如果 '" + drawBoldBackgroundCheckbox.getText()
						+ "' 被选中，标题和区域名称将被赋予此颜色的粗体背景。",
				Arrays.asList(boldBackgroundColorDisplay, btnChooseBoldBackgroundColor), SwingHelper.colorPickerLeftPadding);	

		drawBoldBackgroundCheckbox.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				handleEnablingAndDisabling();
				handleFontsChange();
			}
		});

		enableTextCheckboxActionListener = new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				handleEnablingAndDisabling();
				handleTextChange();
			}
		};

		enableTextCheckBox.addActionListener(enableTextCheckboxActionListener);

		organizer.addVerticalFillerRow();
		organizer.addLeftAlignedComponent(Box.createHorizontalStrut(100));
		return organizer.createScrollPane();
	}

	private void updateDrawRegionsCheckboxEnabledAndSelected()
	{
		if (!landSupportsColoring())
		{
			landColoringMethodComboBox.setSelectedItem(LandColoringMethod.SingleColor);
		}

		handleEnablingAndDisabling();
	}

	private boolean landSupportsColoring()
	{
		return rdbtnFractal.isSelected() || (rdbtnGeneratedFromTexture.isSelected() && colorizeLandCheckbox.isSelected());
	}

	private boolean oceanSupportsColoring()
	{
		return rdbtnFractal.isSelected() || (rdbtnGeneratedFromTexture.isSelected() && colorizeOceanCheckbox.isSelected());
	}

	private void updateBackgroundAndRegionFieldStates(MainWindow mainWindow)
	{
		textureImageHider.setVisible(rdbtnGeneratedFromTexture.isSelected());
		colorizeLandCheckboxHider.setVisible(rdbtnGeneratedFromTexture.isSelected());
		colorizeOceanCheckboxHider.setVisible(rdbtnGeneratedFromTexture.isSelected());
		handleEnablingAndDisabling();

		updateDrawRegionsCheckboxEnabledAndSelected();
	}

	private void updateBackgroundImageDisplays()
	{
		IntDimension size = new IntDimension(backgroundDisplaySize.width, backgroundDisplaySize.height);

		SwingWorker<Tuple4<Image, ImageHelper.ColorifyAlgorithm, Image, ImageHelper.ColorifyAlgorithm>, Void> worker = new SwingWorker<Tuple4<Image, ImageHelper.ColorifyAlgorithm, Image, ImageHelper.ColorifyAlgorithm>, Void>()
		{

			@Override
			protected Tuple4<Image, ImageHelper.ColorifyAlgorithm, Image, ImageHelper.ColorifyAlgorithm> doInBackground() throws Exception
			{
				long seed = parseBackgroundSeed();
				return createBackgroundImageDisplaysImages(size, seed, colorizeOceanCheckbox.isSelected(),
						colorizeLandCheckbox.isSelected(), rdbtnFractal.isSelected(), rdbtnGeneratedFromTexture.isSelected(),
						textureImageFilename.getText());
			}

			@Override
			public void done()
			{
				Tuple4<Image, ImageHelper.ColorifyAlgorithm, Image, ImageHelper.ColorifyAlgorithm> tuple;
				try
				{
					tuple = get();
				}
				catch (InterruptedException | ExecutionException e)
				{
					throw new RuntimeException(e);
				}

				Image oceanBackground = tuple.getFirst();
				ImageHelper.ColorifyAlgorithm oceanColorifyAlgorithm = tuple.getSecond();
				Image landBackground = tuple.getThird();
				ImageHelper.ColorifyAlgorithm landColorifyAlgorithm = tuple.getFourth();

				oceanDisplayPanel.setColorifyAlgorithm(oceanColorifyAlgorithm);
				oceanDisplayPanel.setImage(AwtFactory.unwrap(oceanBackground));
				oceanDisplayPanel.repaint();

				landDisplayPanel.setColorifyAlgorithm(landColorifyAlgorithm);
				landDisplayPanel.setImage(AwtFactory.unwrap(landBackground));
				landDisplayPanel.repaint();
			}
		};

		worker.execute();
	}

	static Tuple4<Image, ImageHelper.ColorifyAlgorithm, Image, ImageHelper.ColorifyAlgorithm> createBackgroundImageDisplaysImages(
			IntDimension size, long seed, boolean colorizeOcean, boolean colorizeLand, boolean isFractal, boolean isFromTexture,
			String textureImageFileName)
	{

		Image oceanBackground;
		ImageHelper.ColorifyAlgorithm oceanColorifyAlgorithm;
		Image landBackground;
		ImageHelper.ColorifyAlgorithm landColorifyAlgorithm;

		if (isFractal)
		{
			oceanColorifyAlgorithm = ImageHelper.ColorifyAlgorithm.algorithm2;
			landColorifyAlgorithm = ImageHelper.ColorifyAlgorithm.algorithm2;

			oceanBackground = landBackground = FractalBGGenerator.generate(new Random(seed), 1.3f, size.width, size.height, 0.75f);
		}
		else if (isFromTexture)
		{
			Image texture;
			try
			{
				texture = ImageHelper.read(textureImageFileName);

				if (colorizeOcean)
				{
					oceanColorifyAlgorithm = ImageHelper.ColorifyAlgorithm.algorithm3;

					oceanBackground = BackgroundGenerator.generateUsingWhiteNoiseConvolution(new Random(seed),
							ImageHelper.convertToGrayscale(texture), size.height, size.width);
				}
				else
				{
					oceanColorifyAlgorithm = ImageHelper.ColorifyAlgorithm.none;

					oceanBackground = BackgroundGenerator.generateUsingWhiteNoiseConvolution(new Random(seed), texture, size.height,
							size.width);
				}

				if (colorizeLand == colorizeOcean)
				{
					// No need to generate the same image twice.
					landBackground = oceanBackground;
					if (colorizeLand)
					{
						landColorifyAlgorithm = ImageHelper.ColorifyAlgorithm.algorithm3;
					}
					else
					{
						landColorifyAlgorithm = ImageHelper.ColorifyAlgorithm.none;
					}
				}
				else
				{
					if (colorizeLand)
					{
						landColorifyAlgorithm = ImageHelper.ColorifyAlgorithm.algorithm3;

						landBackground = BackgroundGenerator.generateUsingWhiteNoiseConvolution(new Random(seed),
								ImageHelper.convertToGrayscale(texture), size.height, size.width);
					}
					else
					{
						landColorifyAlgorithm = ImageHelper.ColorifyAlgorithm.none;

						landBackground = BackgroundGenerator.generateUsingWhiteNoiseConvolution(new Random(seed), texture, size.height,
								size.width);
					}
				}
			}
			catch (RuntimeException e)
			{
				oceanColorifyAlgorithm = ImageHelper.ColorifyAlgorithm.none;
				landColorifyAlgorithm = ImageHelper.ColorifyAlgorithm.none;
				oceanBackground = landBackground = Image.create(size.width, size.height, ImageType.ARGB);
			}
		}
		else
		{
			oceanColorifyAlgorithm = ImageHelper.ColorifyAlgorithm.none;
			landColorifyAlgorithm = ImageHelper.ColorifyAlgorithm.none;
			oceanBackground = landBackground = ImageHelper.createBlackImage(size.width, size.height);
		}

		return new Tuple4<>(oceanBackground, oceanColorifyAlgorithm, landBackground, landColorifyAlgorithm);
	}

	private static String chooseImageFile(Component parent, String curFolder)
	{
		File currentFolder = new File(curFolder);
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
				String extension = FilenameUtils.getExtension(f.getName()).toLowerCase();
				return f.isDirectory() || extension.equals("png") || extension.equals("jpg") || extension.equals("jpeg");
			}
		});
		int status = fileChooser.showOpenDialog(parent);
		if (status == JFileChooser.APPROVE_OPTION)
		{
			return fileChooser.getSelectedFile().toString();
		}
		return null;
	}

	private void handleLandColoringMethodChanged()
	{
		boolean colorRegions = areRegionColorsVisible();
		handleEnablingAndDisabling();

		coastShadingColorHider.setVisible(!colorRegions);
		coastShadingColorDisabledMessageHider.setVisible(colorRegions);
		coastShadingTransparencyHider.setVisible(colorRegions);

		landColorHider.setVisible(!colorRegions);
	}

	/**
	 * Loads a map settings file into the GUI.
	 * 
	 * @param path
	 * @return True if the change affects the map's background image. False otherwise.
	 */
	public boolean loadSettingsIntoGUI(MapSettings settings)
	{
		boolean changeEffectsBackgroundImages = doesChangeEffectBackgroundDisplays(settings);

		coastShadingSlider.setValue(settings.coastShadingLevel);
		oceanEffectsLevelSlider.setValue(settings.oceanEffectsLevel);
		concentricWavesLevelSlider.setValue(settings.concentricWaveCount);
		ripplesRadioButton.setSelected(settings.oceanEffect == OceanEffect.Ripples);
		shadeRadioButton.setSelected(settings.oceanEffect == OceanEffect.Blur);
		concentricWavesButton.setSelected(settings.oceanEffect == OceanEffect.ConcentricWaves);
		fadingConcentricWavesButton.setSelected(settings.oceanEffect == OceanEffect.FadingConcentricWaves);
		drawOceanEffectsInLakesCheckbox.setSelected(settings.drawOceanEffectsInLakes);
		oceanEffectsListener.actionPerformed(null);
		coastShadingColorDisplay.setBackground(AwtFactory.unwrap(settings.coastShadingColor));

		// Temporarily disable events on coastShadingColorDisplay while initially setting the value for coastShadingTransparencySlider so
		// that
		// the action listener on coastShadingTransparencySlider doesn't fire and then update coastShadingColorDisplay, because doing so can
		// cause changes in the settings due to integer truncation of the alpha value.
		disableCoastShadingColorDisplayHandler = true;
		updateCoastShadingTransparencySliderFromCoastShadingColorDisplay();
		disableCoastShadingColorDisplayHandler = false;

		coastlineColorDisplay.setBackground(AwtFactory.unwrap(settings.coastlineColor));
		oceanEffectsColorDisplay.setBackground(AwtFactory.unwrap(settings.oceanEffectsColor));
		riverColorDisplay.setBackground(AwtFactory.unwrap(settings.riverColor));
		frayedEdgeCheckbox.setSelected(settings.frayedBorder);
		// Do a click here to update other components on the panel as enabled or
		// disabled.
		frayedEdgeCheckboxActionListener.actionPerformed(null);
		drawGrungeCheckbox.setSelected(settings.drawGrunge);
		drawGrungeCheckboxActionListener.actionPerformed(null);
		grungeColorDisplay.setBackground(AwtFactory.unwrap(settings.frayedBorderColor));
		frayedEdgeShadingSlider.setValue(settings.frayedBorderBlurLevel);
		frayedEdgeSizeSlider.setValue(frayedEdgeSizeSlider.getMaximum() - settings.frayedBorderSize);
		grungeSlider.setValue(settings.grungeWidth);
		if (settings.lineStyle.equals(LineStyle.Jagged))
		{
			jaggedLinesButton.setSelected(true);
		}
		else if (settings.lineStyle.equals(LineStyle.Splines))
		{
			splinesLinesButton.setSelected(true);
		}
		else if (settings.lineStyle.equals(LineStyle.SplinesWithSmoothedCoastlines))
		{
			splinesWithSmoothedCoastlinesButton.setSelected(true);
		}

		// Settings for background images.
		// Remove and add item listeners to the colorize checkboxes to avoid
		// generating backgrounds for display multiple times.
		colorizeOceanCheckbox.removeItemListener(colorizeCheckboxListener);
		colorizeOceanCheckbox.setSelected((settings.colorizeOcean));
		colorizeOceanCheckbox.addItemListener(colorizeCheckboxListener);
		colorizeLandCheckbox.removeItemListener(colorizeCheckboxListener);
		colorizeLandCheckbox.setSelected((settings.colorizeLand));
		colorizeLandCheckbox.addItemListener(colorizeCheckboxListener);
		rdbtnGeneratedFromTexture.setSelected(settings.generateBackgroundFromTexture);
		rdbtnFractal.setSelected(settings.generateBackground);
		updateBackgroundAndRegionFieldStates(mainWindow);

		// Only do this if there is a change so we don't trigger the document listeners unnecessarily.
		if (!textureImageFilename.getText().equals(settings.backgroundTextureImage))
		{
			textureImageFilename.setText(settings.backgroundTextureImage);
		}

		// Only do this if there is a change so we don't trigger the document listeners unnecessarily.
		if (!backgroundSeedTextField.getText().equals(String.valueOf(settings.backgroundRandomSeed)))
		{
			backgroundSeedTextField.setText(String.valueOf(settings.backgroundRandomSeed));
		}

		oceanDisplayPanel.setColor(AwtFactory.unwrap(settings.oceanColor));
		landDisplayPanel.setColor(AwtFactory.unwrap(settings.landColor));

		if (settings.drawRegionColors)
		{
			landColoringMethodComboBox.setSelectedItem(LandColoringMethod.ColorPoliticalRegions);
		}
		else
		{
			landColoringMethodComboBox.setSelectedItem(LandColoringMethod.SingleColor);
		}
		handleLandColoringMethodChanged();

		// Do a click to update other components on the panel as enabled or
		// disabled.
		enableTextCheckBox.setSelected(settings.drawText);
		enableTextCheckboxActionListener.actionPerformed(null);

		titleFontDisplay.setFont(AwtFactory.unwrap(settings.titleFont));
		titleFontDisplay.setText(settings.titleFont.getName());
		regionFontDisplay.setFont(AwtFactory.unwrap(settings.regionFont));
		regionFontDisplay.setText(settings.regionFont.getName());
		mountainRangeFontDisplay.setFont(AwtFactory.unwrap(settings.mountainRangeFont));
		mountainRangeFontDisplay.setText(settings.mountainRangeFont.getName());
		otherMountainsFontDisplay.setFont(AwtFactory.unwrap(settings.otherMountainsFont));
		otherMountainsFontDisplay.setText(settings.otherMountainsFont.getName());
		riverFontDisplay.setFont(AwtFactory.unwrap(settings.riverFont));
		riverFontDisplay.setText(settings.riverFont.getName());
		textColorDisplay.setBackground(AwtFactory.unwrap(settings.textColor));
		boldBackgroundColorDisplay.setBackground(AwtFactory.unwrap(settings.boldBackgroundColor));
		drawBoldBackgroundCheckbox.setSelected(settings.drawBoldBackground);
		drawBoldBackgroundCheckbox.getActionListeners()[0].actionPerformed(null);

		// Borders
		initializeBorderTypeComboBoxItems(settings);
		borderWidthSlider.setValue(settings.borderWidth);
		drawBorderCheckbox.setSelected(settings.drawBorder);
		drawBorderCheckbox.getActionListeners()[0].actionPerformed(null);

		enableSizeSliderListeners = false;
		treeHeightSlider.setValue((int) (Math.round((settings.treeHeightScale - 0.1) * 20.0)));
		mountainScaleSlider.setValue(getSliderValueForScale(settings.mountainScale));
		hillScaleSlider.setValue(getSliderValueForScale(settings.hillScale));
		duneScaleSlider.setValue(getSliderValueForScale(settings.duneScale));
		cityScaleSlider.setValue(getSliderValueForScale(settings.cityScale));
		enableSizeSliderListeners = true;

		if (changeEffectsBackgroundImages)
		{
			updateBackgroundImageDisplays();
		}

		// For some reason I have to repaint to get color display panels to draw
		// correctly.
		repaint();

		return changeEffectsBackgroundImages;
	}

	private final double scaleMax = 3.0;
	private final double scaleMin = 0.5;
	private final double sliderValueFor1Scale = 5;
	private final int minScaleSliderValue = 1;
	private final int maxScaleSliderValue = 15;

	private int getSliderValueForScale(double scale)
	{
		if (scale <= 1.0)
		{
			double slope = (sliderValueFor1Scale - minScaleSliderValue) / (1.0 - scaleMin);
			double yIntercept = sliderValueFor1Scale - slope;
			return (int) Math.round(scale * slope + yIntercept);
		}
		else
		{
			double slope = (maxScaleSliderValue - sliderValueFor1Scale) / (scaleMax - 1.0);
			double yIntercept = sliderValueFor1Scale - slope * (1.0);
			return (int) Math.round(scale * slope + yIntercept);
		}
	}

	private double getScaleForSliderValue(int sliderValue)
	{
		if (sliderValue <= sliderValueFor1Scale)
		{
			double slope = (sliderValueFor1Scale - minScaleSliderValue) / (1.0 - scaleMin);
			double yIntercept = sliderValueFor1Scale - slope;
			return (sliderValue - yIntercept) / slope;
		}
		else
		{
			double slope = (maxScaleSliderValue - sliderValueFor1Scale) / (scaleMax - 1.0);
			double yIntercept = sliderValueFor1Scale - slope * (1.0);
			return (sliderValue - yIntercept) / slope;
		}
	}


	private void initializeBorderTypeComboBoxItems(MapSettings settings)
	{
		SwingHelper.initializeComboBoxItems(borderTypeComboBox, MapCreator.getAvailableBorderTypes(settings.customImagesPath),
				settings.borderType, true);

	}

	private boolean doesChangeEffectBackgroundDisplays(MapSettings settings)
	{
		if (parseBackgroundSeed() != settings.backgroundRandomSeed)
		{
			return true;
		}

		if (colorizeOceanCheckbox.isSelected() != settings.colorizeOcean)
		{
			return true;
		}

		if (colorizeLandCheckbox.isSelected() != settings.colorizeLand)
		{
			return true;
		}

		if (rdbtnFractal.isSelected() != settings.generateBackground)
		{
			return true;
		}

		if (rdbtnGeneratedFromTexture.isSelected() != settings.generateBackgroundFromTexture)
		{
			return true;
		}

		if (!textureImageFilename.getText().equals(settings.backgroundTextureImage))
		{
			return true;
		}

		if (!landDisplayPanel.getColor().equals(AwtFactory.unwrap(settings.landColor)))
		{
			return true;
		}
		
		if (!oceanDisplayPanel.getColor().equals(AwtFactory.unwrap(settings.oceanColor)))
		{
			return true;
		}

		return false;
	}

	private long parseBackgroundSeed()
	{
		try
		{
			return Long.parseLong(backgroundSeedTextField.getText());
		}
		catch (NumberFormatException e)
		{
			return 0;
		}
	}

	public void getSettingsFromGUI(MapSettings settings)
	{
		settings.coastShadingLevel = coastShadingSlider.getValue();
		settings.oceanEffectsLevel = oceanEffectsLevelSlider.getValue();
		settings.concentricWaveCount = concentricWavesLevelSlider.getValue();
		settings.oceanEffect = ripplesRadioButton.isSelected() ? OceanEffect.Ripples
				: shadeRadioButton.isSelected() ? OceanEffect.Blur
						: concentricWavesButton.isSelected() ? OceanEffect.ConcentricWaves : OceanEffect.FadingConcentricWaves;
		settings.drawOceanEffectsInLakes = drawOceanEffectsInLakesCheckbox.isSelected();
		settings.coastShadingColor = AwtFactory.wrap(coastShadingColorDisplay.getBackground());
		settings.coastlineColor = AwtFactory.wrap(coastlineColorDisplay.getBackground());
		settings.oceanEffectsColor = AwtFactory.wrap(oceanEffectsColorDisplay.getBackground());
		settings.riverColor = AwtFactory.wrap(riverColorDisplay.getBackground());
		settings.drawText = enableTextCheckBox.isSelected();
		settings.frayedBorder = frayedEdgeCheckbox.isSelected();
		settings.frayedBorderColor = AwtFactory.wrap(grungeColorDisplay.getBackground());
		settings.frayedBorderBlurLevel = frayedEdgeShadingSlider.getValue();
		// Make increasing frayed edge values cause the number of polygons to
		// decrease so that the fray gets large with
		// larger values of the slider.
		settings.frayedBorderSize = frayedEdgeSizeSlider.getMaximum() - frayedEdgeSizeSlider.getValue();
		settings.drawGrunge = drawGrungeCheckbox.isSelected();
		settings.grungeWidth = grungeSlider.getValue();
		settings.lineStyle = jaggedLinesButton.isSelected() ? LineStyle.Jagged
				: splinesLinesButton.isSelected() ? LineStyle.Splines : LineStyle.SplinesWithSmoothedCoastlines;

		// Background image settings
		settings.generateBackground = rdbtnFractal.isSelected();
		settings.generateBackgroundFromTexture = rdbtnGeneratedFromTexture.isSelected();
		settings.colorizeOcean = colorizeOceanCheckbox.isSelected();
		settings.colorizeLand = colorizeLandCheckbox.isSelected();
		settings.backgroundTextureImage = textureImageFilename.getText();
		try
		{
			settings.backgroundRandomSeed = Long.parseLong(backgroundSeedTextField.getText());
		}
		catch (NumberFormatException e)
		{
			settings.backgroundRandomSeed = 0;
		}
		settings.oceanColor = AwtFactory.wrap(oceanDisplayPanel.getColor());
		settings.drawRegionColors = areRegionColorsVisible();
		settings.landColor = AwtFactory.wrap(landDisplayPanel.getColor());

		settings.titleFont = AwtFactory.wrap(titleFontDisplay.getFont());
		settings.regionFont = AwtFactory.wrap(regionFontDisplay.getFont());
		settings.mountainRangeFont = AwtFactory.wrap(mountainRangeFontDisplay.getFont());
		settings.otherMountainsFont = AwtFactory.wrap(otherMountainsFontDisplay.getFont());
		settings.riverFont = AwtFactory.wrap(riverFontDisplay.getFont());
		settings.textColor = AwtFactory.wrap(textColorDisplay.getBackground());
		settings.boldBackgroundColor = AwtFactory.wrap(boldBackgroundColorDisplay.getBackground());
		settings.drawBoldBackground = drawBoldBackgroundCheckbox.isSelected();

		settings.drawBorder = drawBorderCheckbox.isSelected();
		settings.borderType = (String) borderTypeComboBox.getSelectedItem();
		settings.borderWidth = borderWidthSlider.getValue();

		settings.treeHeightScale = 0.1 + (treeHeightSlider.getValue() * 0.05);
		settings.mountainScale = getScaleForSliderValue(mountainScaleSlider.getValue());
		settings.hillScale = getScaleForSliderValue(hillScaleSlider.getValue());
		settings.duneScale = getScaleForSliderValue(duneScaleSlider.getValue());
		settings.cityScale = getScaleForSliderValue(cityScaleSlider.getValue());
	}

	private boolean areRegionColorsVisible()
	{
		return landColoringMethodComboBox.getSelectedItem().equals(LandColoringMethod.ColorPoliticalRegions);
	}

	public Color getLandColor()
	{
		return landDisplayPanel.getColor();
	}

	public enum LandColoringMethod
	{
		SingleColor("单一颜色"), ColorPoliticalRegions("着色政治区域");


		private final String name;

		private LandColoringMethod(String name)
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	private void createMapChangeListenerForTerrainChange(Component component)
	{
		SwingHelper.addListener(component, () -> handleTerrainChange());
	}

	private void handleTerrainChange()
	{
		mainWindow.handleThemeChange(false);
		mainWindow.undoer.setUndoPoint(UpdateType.Terrain, null);
		mainWindow.updater.createAndShowMapTerrainChange();
	}

	private void handleFontsChange()
	{
		mainWindow.handleThemeChange(false);
		mainWindow.undoer.setUndoPoint(UpdateType.Fonts, null);
		mainWindow.updater.createAndShowMapFontsChange();
	}

	private void handleTextChange()
	{
		mainWindow.handleThemeChange(false);
		mainWindow.undoer.setUndoPoint(UpdateType.Text, null);
		mainWindow.updater.createAndShowMapTextChange();
	}

	private void createMapChangeListenerForFullRedraw(Component component)
	{
		SwingHelper.addListener(component, () -> handleFullRedraw());
	}

	private void handleFullRedraw()
	{
		mainWindow.handleThemeChange(true);
		mainWindow.undoer.setUndoPoint(UpdateType.Full, null);
		mainWindow.updater.createAndShowMapFull();
	}

	private void createMapChangeListenerForFrayedEdgeOrGrungeChange(Component component)
	{
		SwingHelper.addListener(component, () -> handleFrayedEdgeOrGrungeChange());
	}

	private void handleFrayedEdgeOrGrungeChange()
	{
		mainWindow.handleThemeChange(false);
		mainWindow.undoer.setUndoPoint(UpdateType.GrungeAndFray, null);
		mainWindow.updater.createAndShowMapGrungeOrFrayedEdgeChange();
	}

	private void handleEnablingAndDisabling()
	{
		borderWidthSlider.setEnabled(drawBorderCheckbox.isSelected());
		borderTypeComboBox.setEnabled(drawBorderCheckbox.isSelected());

		frayedEdgeShadingSlider.setEnabled(frayedEdgeCheckbox.isSelected());
		frayedEdgeSizeSlider.setEnabled(frayedEdgeCheckbox.isSelected());

		grungeColorChooseButton.setEnabled(drawGrungeCheckbox.isSelected());
		grungeSlider.setEnabled(drawGrungeCheckbox.isSelected());

		btnChooseBoldBackgroundColor.setEnabled(drawBoldBackgroundCheckbox.isSelected());

		btnTitleFont.setEnabled(enableTextCheckBox.isSelected());
		btnRegionFont.setEnabled(enableTextCheckBox.isSelected());
		btnMountainRangeFont.setEnabled(enableTextCheckBox.isSelected());
		btnOtherMountainsFont.setEnabled(enableTextCheckBox.isSelected());
		btnRiverFont.setEnabled(enableTextCheckBox.isSelected());
		btnChooseTextColor.setEnabled(enableTextCheckBox.isSelected());
		btnChooseBoldBackgroundColor.setEnabled(enableTextCheckBox.isSelected());
		drawBoldBackgroundCheckbox.setEnabled(enableTextCheckBox.isSelected());

		landColoringMethodComboBox.setEnabled(landSupportsColoring());

		btnChooseOceanColor.setEnabled(oceanSupportsColoring());
		btnChooseLandColor.setEnabled(landSupportsColoring());

		btnChooseCoastShadingColor.setEnabled(!areRegionColorsVisible());

	}

	void enableOrDisableEverything(boolean enable)
	{
		SwingHelper.setEnabled(this, enable);

		if (enable)
		{
			// Call this to disable any fields that should be disabled.
			handleEnablingAndDisabling();
		}
	}

	void handleImagesRefresh(MapSettings settings)
	{
		initializeBorderTypeComboBoxItems(settings);
	}
}
