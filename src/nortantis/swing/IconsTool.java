package nortantis.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;

import org.imgscalr.Scalr.Method;

import nortantis.DebugFlags;
import nortantis.IconDrawTask;
import nortantis.IconType;
import nortantis.ImageAndMasks;
import nortantis.ImageCache;
import nortantis.MapSettings;
import nortantis.editor.CenterEdit;
import nortantis.editor.CenterIcon;
import nortantis.editor.CenterIconType;
import nortantis.editor.CenterTrees;
import nortantis.editor.FreeIcon;
import nortantis.editor.MapUpdater;
import nortantis.geom.IntDimension;
import nortantis.geom.Point;
import nortantis.geom.Rectangle;
import nortantis.geom.RotatedRectangle;
import nortantis.graph.voronoi.Center;
import nortantis.platform.Color;
import nortantis.platform.Image;
import nortantis.platform.ImageType;
import nortantis.platform.Painter;
import nortantis.platform.awt.AwtFactory;
import nortantis.util.AssetsPath;
import nortantis.util.ImageHelper;
import nortantis.util.Range;
import nortantis.util.Tuple2;
import nortantis.util.Tuple4;

public class IconsTool extends EditorTool
{
	private JRadioButton mountainsButton;
	private JRadioButton treesButton;
	private JComboBox<ImageIcon> brushSizeComboBox;
	private RowHider brushSizeHider;
	private JRadioButton hillsButton;
	private JRadioButton dunesButton;
	private IconTypeButtons mountainTypes;
	private IconTypeButtons hillTypes;
	private IconTypeButtons duneTypes;
	private IconTypeButtons treeTypes;
	private NamedIconSelector cityButtons;
	private NamedIconSelector decorationButtons;
	private JSlider densitySlider;
	private Random rand;
	private RowHider densityHider;
	private JRadioButton allButton;
	private JRadioButton citiesButton;
	private JRadioButton decorationsButton;
	private DrawModeWidget modeWidget;
	private FreeIcon iconToEdit;
	private java.awt.Point editStart;
	private boolean isMoving;
	private boolean isScaling;

	public IconsTool(MainWindow parent, ToolsPanel toolsPanel, MapUpdater mapUpdater)
	{
		super(parent, toolsPanel, mapUpdater);
		rand = new Random();
	}

	@Override
	public String getToolbarName()
	{
		return "Icons";
	}

	@Override
	public String getImageIconFilePath()
	{
		return Paths.get(AssetsPath.getInstallPath(), "internal/Icon tool.png").toString();
	}

	@Override
	public void onBeforeSaving()
	{
	}

	@Override
	public void onSwitchingAway()
	{
		mapEditingPanel.clearAllToolSpecificSelectionsAndHighlights();
	}

	@Override
	protected JPanel createToolOptionsPanel()
	{
		GridBagOrganizer organizer = new GridBagOrganizer();

		JPanel toolOptionsPanel = organizer.panel;
		toolOptionsPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

		// Tools
		{
			ButtonGroup group = new ButtonGroup();
			List<JComponent> radioButtons = new ArrayList<>();

			mountainsButton = new JRadioButton("山脉");
			group.add(mountainsButton);
			radioButtons.add(mountainsButton);
			mountainsButton.setSelected(true);
			mountainsButton.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent event)
				{
					updateTypePanels();
				}
			});

			hillsButton = new JRadioButton("山区");
			group.add(hillsButton);
			radioButtons.add(hillsButton);
			hillsButton.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent event)
				{
					updateTypePanels();
				}
			});

			dunesButton = new JRadioButton("沙丘");
			group.add(dunesButton);
			radioButtons.add(dunesButton);
			dunesButton.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent event)
				{
					updateTypePanels();
				}
			});

			treesButton = new JRadioButton("树木");
			group.add(treesButton);
			radioButtons.add(treesButton);
			treesButton.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent event)
				{
					updateTypePanels();
				}
			});

			citiesButton = new JRadioButton("城市");
			group.add(citiesButton);
			radioButtons.add(citiesButton);
			citiesButton.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent event)
				{
					updateTypePanels();
				}
			});

			decorationsButton = new JRadioButton("装饰");
			group.add(decorationsButton);
			radioButtons.add(decorationsButton);
			decorationsButton.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent event)
				{
					updateTypePanels();
				}
			});

			allButton = new JRadioButton("All");
			group.add(allButton);
			radioButtons.add(allButton);
			allButton.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent event)
				{
					updateTypePanels();
				}
			});

			organizer.addLabelAndComponentsVertical("类型:", "要添加/编辑的图标类型。", radioButtons);  // "Type:"翻译为"类型:", "The type of icon to add/edit."翻译为"要添加/编辑的图标类型。"
		}
		modeWidget = new DrawModeWidget("使用选定的画笔绘制", "使用选定的画笔擦除", true,
				"使用选定的画笔替换相同类型的现有图标", true, "移动或缩放单个图标",
				() -> handleModeChanged());
		modeWidget.addToOrganizer(organizer, "选择画笔类型时是否绘制或擦除");  // "Whether to draw or erase using the selected brush type"翻译为"选择画笔类型时是否绘制或擦除"
		

		Tuple2<JComboBox<ImageIcon>, RowHider> brushSizeTuple = organizer.addBrushSizeComboBox(brushSizes);
		brushSizeComboBox = brushSizeTuple.getFirst();
		brushSizeHider = brushSizeTuple.getSecond();

		{
			densitySlider = new JSlider(1, 50);
			densitySlider.setValue(7);
			SwingHelper.setSliderWidthForSidePanel(densitySlider);
			SliderWithDisplayedValue sliderWithDisplay = new SliderWithDisplayedValue(densitySlider);
			densityHider = sliderWithDisplay.addToOrganizer(organizer, "密度:", "");
		}

		mountainTypes = createOrUpdateRadioButtonsForIconType(organizer, IconType.mountains, mountainTypes, null);
		hillTypes = createOrUpdateRadioButtonsForIconType(organizer, IconType.hills, hillTypes, null);
		duneTypes = createOrUpdateRadioButtonsForIconType(organizer, IconType.sand, duneTypes, null);
		treeTypes = createOrUpdateRadioButtonsForIconType(organizer, IconType.trees, treeTypes, null);
		selectDefaultTreesButtion(treeTypes);

		createOrUpdateButtonsForCities(organizer, null);

		createOrUpdateDecorationButtons(organizer, null);

		mountainsButton.doClick();

		organizer.addHorizontalSpacerRowToHelpComponentAlignment(0.666);
		organizer.addVerticalFillerRow();

		return toolOptionsPanel;
	}

	/**
	 * Prevents cacti from being the default tree brush
	 */
	private void selectDefaultTreesButtion(IconTypeButtons typeButtons)
	{
		if (typeButtons.buttons.size() > 1 && typeButtons.buttons.get(0).getText().equals("cacti"))
		{
			typeButtons.buttons.get(1).getRadioButton().setSelected(true);
		}
		else if (typeButtons.buttons.size() > 0)
		{
			typeButtons.buttons.get(0).getRadioButton().setSelected(true);
		}
	}

	private void handleModeChanged()
	{
		mapEditingPanel.clearIconEditTools();
		mapEditingPanel.repaint();
		updateTypePanels();
	}

	private void showOrHideBrush(MouseEvent e)
	{
		int brushDiameter = getBrushDiameter();
		if (modeWidget.isDrawMode() || modeWidget.isEditMode() || brushDiameter <= 1)
		{
			mapEditingPanel.hideBrush();
		}
		else
		{
			java.awt.Point mouseLocation = e.getPoint();
			mapEditingPanel.showBrush(mouseLocation, brushDiameter, modeWidget.isEraseMode());
			mapEditingPanel.repaint();
		}

	}

	private void updateTypePanels()
	{
		modeWidget.showOrHideOptions(!allButton.isSelected(), true, !allButton.isSelected(), true);

		mountainTypes.hider.setVisible(mountainsButton.isSelected() && (modeWidget.isDrawMode() || modeWidget.isReplaceMode()));
		hillTypes.hider.setVisible(hillsButton.isSelected() && (modeWidget.isDrawMode() || modeWidget.isReplaceMode()));
		duneTypes.hider.setVisible(dunesButton.isSelected() && (modeWidget.isDrawMode() || modeWidget.isReplaceMode()));
		treeTypes.hider.setVisible(treesButton.isSelected() && (modeWidget.isDrawMode() || modeWidget.isReplaceMode()));
		cityButtons.hider.setVisible(citiesButton.isSelected() && (modeWidget.isDrawMode() || modeWidget.isReplaceMode()));
		decorationButtons.hider.setVisible(decorationsButton.isSelected() && (modeWidget.isDrawMode() || modeWidget.isReplaceMode()));
		densityHider.setVisible(treesButton.isSelected() && (modeWidget.isDrawMode()));
		brushSizeHider.setVisible((modeWidget.isDrawMode() && !citiesButton.isSelected() && !decorationsButton.isSelected())
				|| modeWidget.isReplaceMode() || modeWidget.isEraseMode());
		toolsPanel.revalidate();
		toolsPanel.repaint();
	}

	private IconTypeButtons createOrUpdateRadioButtonsForIconType(GridBagOrganizer organizer, IconType iconType, IconTypeButtons existing,
			String customImagesPath)
	{
		String prevSelection = existing != null ? existing.getSelectedOption() : null;

		ButtonGroup group = new ButtonGroup();
		List<RadioButtonWithImage> radioButtons = new ArrayList<>();
		List<String> groupNames = new ArrayList<>(ImageCache.getInstance(customImagesPath).getIconGroupNames(iconType));
		for (String groupName : groupNames)
		{
			RadioButtonWithImage button = new RadioButtonWithImage(groupName, null);
			group.add(button.getRadioButton());
			radioButtons.add(button);
		}

		IconTypeButtons result;
		if (existing == null)
		{
			JPanel buttonsPanel = new JPanel();
			result = new IconTypeButtons(organizer.addLabelAndComponentsVerticalWithComponentPanel("类型:", "", radioButtons, buttonsPanel),
					radioButtons, buttonsPanel);
		}
		else
		{
			result = existing;
			existing.buttons = radioButtons;
			GridBagOrganizer.updateComponentsPanelVertical(radioButtons, existing.buttonsPanel);
		}

		if (prevSelection == null || !result.selectButtonIfPresent(prevSelection))
		{
			if (radioButtons.size() > 0)
			{
				if (iconType == IconType.trees)
				{
					selectDefaultTreesButtion(result);
				}
				else
				{
					radioButtons.get(0).getRadioButton().setSelected(true);
				}
			}
		}

		return result;
	}

	@Override
	public void handleImagesRefresh(MapSettings settings)
	{
		String customImagesPath = settings == null ? null : settings.customImagesPath;
		mountainTypes = createOrUpdateRadioButtonsForIconType(null, IconType.mountains, mountainTypes, customImagesPath);
		hillTypes = createOrUpdateRadioButtonsForIconType(null, IconType.hills, hillTypes, customImagesPath);
		duneTypes = createOrUpdateRadioButtonsForIconType(null, IconType.sand, duneTypes, customImagesPath);
		treeTypes = createOrUpdateRadioButtonsForIconType(null, IconType.trees, treeTypes, customImagesPath);

		createOrUpdateButtonsForCities(null, settings.customImagesPath);
		createOrUpdateDecorationButtons(null, settings.customImagesPath);

		// Trigger re-creation of image previews
		loadSettingsIntoGUI(settings, false, true, false);
		unselectAnyIconBeingEdited();
	}

	private void updateIconTypeButtonPreviewImages(MapSettings settings)
	{
		String customImagesPath = settings == null ? null : settings.customImagesPath;
		updateOneIconTypeButtonPreviewImages(settings, IconType.mountains, mountainTypes, customImagesPath);
		updateOneIconTypeButtonPreviewImages(settings, IconType.hills, hillTypes, customImagesPath);
		updateOneIconTypeButtonPreviewImages(settings, IconType.sand, duneTypes, customImagesPath);
		updateOneIconTypeButtonPreviewImages(settings, IconType.trees, treeTypes, customImagesPath);

		updateNamedIconButtonPreviewImages(settings, cityButtons);
		updateNamedIconButtonPreviewImages(settings, decorationButtons);
	}

	private void updateOneIconTypeButtonPreviewImages(MapSettings settings, IconType iconType, IconTypeButtons buttons,
			String customImagesPath)
	{
		for (RadioButtonWithImage button : buttons.buttons)
		{
			final String buttonText = button.getText();
			SwingWorker<Image, Void> worker = new SwingWorker<>()
			{
				@Override
				protected Image doInBackground() throws Exception
				{
					return createIconPreviewForGroup(settings, iconType, buttonText, customImagesPath);
				}

				@Override
				public void done()
				{
					Image previewImage;
					try
					{
						previewImage = get();
					}
					catch (InterruptedException | ExecutionException e)
					{
						throw new RuntimeException(e);
					}

					button.setImage(AwtFactory.unwrap(previewImage));
				}
			};

			worker.execute();
		}
	}

	private void updateNamedIconButtonPreviewImages(MapSettings settings, NamedIconSelector selector)
	{
		for (String groupId : ImageCache.getInstance(settings.customImagesPath).getIconGroupNames(selector.type))
		{
			final List<Tuple2<String, JToggleButton>> namesAndButtons = selector.getIconNamesAndButtons(groupId);

			if (namesAndButtons != null)
			{
				SwingWorker<List<Image>, Void> worker = new SwingWorker<>()
				{
					@Override
					protected List<Image> doInBackground() throws Exception
					{
						List<Image> previewImages = new ArrayList<>();
						Map<String, Tuple2<ImageAndMasks, Integer>> iconsInGroup = ImageCache.getInstance(settings.customImagesPath)
								.getIconsWithWidths(selector.type, groupId);

						for (Tuple2<String, JToggleButton> nameAndButton : namesAndButtons)
						{
							String iconNameWithoutWidthOrExtension = nameAndButton.getFirst();
							if (!iconsInGroup.containsKey(iconNameWithoutWidthOrExtension))
							{
								throw new IllegalArgumentException(
										"对于按钮 '" + iconNameWithoutWidthOrExtension + "'，没有 '" + selector.type + "' 图标存在");
							}
							Image icon = iconsInGroup.get(iconNameWithoutWidthOrExtension).getFirst().image;
							Image preview = createIconPreview(settings, Collections.singletonList(icon), 45, 0, selector.type);
							previewImages.add(preview);
						}
								

						return previewImages;
					}

					@Override
					public void done()
					{
						List<Image> previewImages;
						try
						{
							previewImages = get();
						}
						catch (InterruptedException | ExecutionException e)
						{
							throw new RuntimeException(e);
						}

						for (int i : new Range(previewImages.size()))
						{
							selector.getIconNamesAndButtons(groupId).get(i).getSecond()
									.setIcon(new ImageIcon(AwtFactory.unwrap(previewImages.get(i))));
						}
					}
				};

				worker.execute();
			}
		}
	}

	private void createOrUpdateButtonsForCities(GridBagOrganizer organizer, String customImagesPath)
	{
		boolean isNew;
		Tuple2<String, String> selectedCity = null;
		if (cityButtons == null)
		{
			// This is the first time to create the city buttons.
			cityButtons = new NamedIconSelector(IconType.cities);
			JPanel typesPanel = new JPanel();
			typesPanel.setLayout(new BoxLayout(typesPanel, BoxLayout.Y_AXIS));
			cityButtons.typesPanel = typesPanel;
			isNew = true;
		}
		else
		{
			selectedCity = cityButtons.getSelectedButton();
			cityButtons.clearButtons();
			isNew = false;
		}

		updateNamedIconSelector(organizer, customImagesPath, cityButtons, isNew, selectedCity, "装饰: ");
	}

	private void createOrUpdateDecorationButtons(GridBagOrganizer organizer, String customImagesPath)
	{
		boolean isNew;
		Tuple2<String, String> selectedButton = null;
		if (decorationButtons == null)
		{
			// This is the first time to create the buttons.
			decorationButtons = new NamedIconSelector(IconType.decorations);
			JPanel typesPanel = new JPanel();
			typesPanel.setLayout(new BoxLayout(typesPanel, BoxLayout.Y_AXIS));
			decorationButtons.typesPanel = typesPanel;
			isNew = true;
		}
		else
		{
			selectedButton = decorationButtons.getSelectedButton();
			decorationButtons.clearButtons();
			isNew = false;
		}

		updateNamedIconSelector(organizer, customImagesPath, decorationButtons, isNew, selectedButton, "装饰：");
	}

	private static void updateNamedIconSelector(GridBagOrganizer organizer, String customImagesPath, NamedIconSelector selector,
			boolean isNew, Tuple2<String, String> selectedCity, String labelText)
	{
		boolean hasAtLeastOneImage = false;
		for (String groupId : ImageCache.getInstance(customImagesPath).getIconGroupNames(selector.type))
		{
			JPanel typePanel = new JPanel();
			typePanel.setLayout(new WrapLayout());
			typePanel.setBorder(new LineBorder(UIManager.getColor("controlShadow"), 1));
			for (String fileNameWithoutWidthOrExtension : ImageCache.getInstance(customImagesPath)
					.getIconGroupFileNamesWithoutWidthOrExtension(selector.type, groupId))
			{
				JToggleButton toggleButton = new JToggleButton();
				toggleButton.setToolTipText(fileNameWithoutWidthOrExtension);
				toggleButton.addActionListener(new ActionListener()
				{

					@Override
					public void actionPerformed(ActionEvent e)
					{
						if (!toggleButton.isSelected())
						{
							toggleButton.setSelected(true);
						}
						selector.unselectAllButtonsExcept(toggleButton);
						NamedIconSelector.updateToggleButtonBorder(toggleButton);
					}
				});
				NamedIconSelector.updateToggleButtonBorder(toggleButton);

				selector.addButton(groupId, fileNameWithoutWidthOrExtension, toggleButton);
				typePanel.add(toggleButton);
				hasAtLeastOneImage = true;
			}

			// If at least one button was added
			if (selector.getTypes().contains(groupId))
			{
				CollapsiblePanel panel = new CollapsiblePanel(selector.type.toString() + "Type", groupId, typePanel);
				selector.typesPanel.add(panel);
			}
		}

		if (isNew)
		{
			selector.hider = organizer.addLeftAlignedComponentWithStackedLabel(labelText, "", selector.typesPanel);
		}

		if (hasAtLeastOneImage)
		{
			if (selectedCity != null)
			{
				boolean found = selector.selectButtonIfPresent(selectedCity.getFirst(), selectedCity.getSecond());
				if (!found)
				{
					selector.selectFirstButton();
				}
			}
			else
			{
				selector.selectFirstButton();
			}
		}
	}

	private Image createIconPreviewForGroup(MapSettings settings, IconType iconType, String groupName, String customImagesPath)
	{
		List<Image> croppedImages = new ArrayList<>();
		for (ImageAndMasks imageAndMasks : ImageCache.getInstance(customImagesPath).loadIconGroup(iconType, groupName))
		{
			croppedImages.add(imageAndMasks.cropToContent());
		}
		return createIconPreview(settings, croppedImages, 30, 9, iconType);
	}

	private Image createIconPreview(MapSettings settings, List<Image> images, int scaledHeight, int padding, IconType iconType)
	{
		final int maxRowWidth = 168;
		final int horizontalPaddingBetweenImages = 2;

		// Find the size needed for the preview
		int rowCount = 1;
		int largestRowWidth = 0;
		{
			int rowWidth = 0;
			for (int i : new Range(images.size()))
			{
				Image image = images.get(i);
				int scaledWidth = ImageHelper.getWidthWhenScaledByHeight(image, scaledHeight);
				if (rowWidth + scaledWidth > maxRowWidth)
				{
					rowCount++;
					rowWidth = scaledWidth;
				}
				else
				{
					rowWidth += scaledWidth;
					if (i < images.size() - 1)
					{
						rowWidth += horizontalPaddingBetweenImages;
					}
				}

				largestRowWidth = Math.max(largestRowWidth, rowWidth);
			}
		}

		// Create the background image for the preview
		final int fadeWidth = Math.max(padding - 2, 0);
		// Multiply the width padding by 2.2 instead of 2 to compensate for the
		// image library I'm using not always scaling to the size I
		// give.
		IntDimension size = new IntDimension(largestRowWidth + ((int) (padding * 2.2)), (rowCount * scaledHeight) + (padding * 2));

		Image previewImage;

		Tuple4<Image, ImageHelper.ColorifyAlgorithm, Image, ImageHelper.ColorifyAlgorithm> tuple = ThemePanel
				.createBackgroundImageDisplaysImages(size, settings.backgroundRandomSeed, settings.colorizeOcean, settings.colorizeLand,
						settings.generateBackground, settings.generateBackgroundFromTexture, settings.backgroundTextureImage);
		if (iconType == IconType.decorations)
		{
			previewImage = tuple.getFirst();
			previewImage = ImageHelper.colorify(previewImage, settings.oceanColor, tuple.getSecond());
		}
		else
		{
			previewImage = tuple.getThird();	
			previewImage = ImageHelper.colorify(previewImage, settings.landColor, tuple.getFourth());
		}
		

		previewImage = fadeEdges(previewImage, fadeWidth);

		Painter p = previewImage.createPainter();

		int x = padding;
		int y = padding;
		for (int i : new Range(images.size()))
		{
			Image image = images.get(i);
			int scaledWidth = ImageHelper.getWidthWhenScaledByHeight(image, scaledHeight);
			Image scaled = ImageHelper.scaleByWidth(image, scaledWidth, Method.ULTRA_QUALITY);
			if (x - padding + scaled.getWidth() > maxRowWidth)
			{
				x = padding;
				y += scaledHeight;
			}

			p.drawImage(scaled, x, y);

			x += scaled.getWidth();
			if (i < images.size() - 1)
			{
				x += horizontalPaddingBetweenImages;
			}
		}

		return previewImage;
	}

	private Image fadeEdges(Image image, int fadeWidth)
	{
		Image box = Image.create(image.getWidth(), image.getHeight(), ImageType.Grayscale8Bit);
		Painter p = box.createPainter();
		p.setColor(Color.white);
		p.fillRect(fadeWidth, fadeWidth, image.getWidth() - fadeWidth * 2, image.getHeight() - fadeWidth * 2);
		p.dispose();

		// Use convolution to make a hazy background for the text.
		Image hazyBox = ImageHelper.convolveGrayscale(box, ImageHelper.createGaussianKernel(fadeWidth), true, false);

		return ImageHelper.setAlphaFromMask(image, hazyBox, false);
	}

	@Override
	protected void handleMouseClickOnMap(MouseEvent e)
	{
	}

	private void handleMousePressOrDrag(MouseEvent e, boolean isPress)
	{
		showOrHideBrush(e);
		if (modeWidget.isDrawMode())
		{
			handleDrawIcons(e, isPress);
		}
		else if (modeWidget.isReplaceMode())
		{
			handleReplaceIcons(e);
		}
		else if (modeWidget.isEraseMode())
		{
			handleEraseIcons(e);
		}
		else if (modeWidget.isEditMode())
		{
			handleEditIcons(e, isPress);
		}
	}

	private void handleDrawIcons(MouseEvent e, boolean isPress)
	{
		if (treesButton.isSelected())
		{
			eraseTreesThatFailedToDrawDueToLowDensity(e);
		}

		if (mountainsButton.isSelected())
		{
			Set<Center> selected = getSelectedLandCenters(e.getPoint());
			String rangeId = mountainTypes.getSelectedOption();
			for (Center center : selected)
			{
				CenterEdit cEdit = mainWindow.edits.centerEdits.get(center.index);
				CenterIcon newIcon = new CenterIcon(CenterIconType.Mountain, rangeId, Math.abs(rand.nextInt()));
				mainWindow.edits.centerEdits.put(center.index, cEdit.copyWithIcon(newIcon));
			}
			updater.createAndShowMapIncrementalUsingCenters(selected);
		}
		else if (hillsButton.isSelected())
		{
			Set<Center> selected = getSelectedLandCenters(e.getPoint());
			String rangeId = hillTypes.getSelectedOption();
			for (Center center : selected)
			{
				CenterEdit cEdit = mainWindow.edits.centerEdits.get(center.index);
				CenterIcon newIcon = new CenterIcon(CenterIconType.Hill, rangeId, Math.abs(rand.nextInt()));
				mainWindow.edits.centerEdits.put(center.index, cEdit.copyWithIcon(newIcon));
			}
			updater.createAndShowMapIncrementalUsingCenters(selected);
		}
		else if (dunesButton.isSelected())
		{
			Set<Center> selected = getSelectedLandCenters(e.getPoint());
			String rangeId = duneTypes.getSelectedOption();
			for (Center center : selected)
			{
				CenterEdit cEdit = mainWindow.edits.centerEdits.get(center.index);
				CenterIcon newIcon = new CenterIcon(CenterIconType.Dune, rangeId, Math.abs(rand.nextInt()));
				mainWindow.edits.centerEdits.put(center.index, cEdit.copyWithIcon(newIcon));
			}
			updater.createAndShowMapIncrementalUsingCenters(selected);
		}
		else if (treesButton.isSelected())
		{
			Set<Center> selected = getSelectedLandCenters(e.getPoint());
			String treeType = treeTypes.getSelectedOption();
			for (Center center : selected)
			{
				CenterEdit cEdit = mainWindow.edits.centerEdits.get(center.index);
				CenterTrees newTrees = new CenterTrees(treeType, densitySlider.getValue() / 10.0, Math.abs(rand.nextLong()));
				mainWindow.edits.centerEdits.put(center.index, cEdit.copyWithTrees(newTrees));
			}
			updater.createAndShowMapIncrementalUsingCenters(selected);
		}
		else if (citiesButton.isSelected())
		{
			Set<Center> selected = getSelectedLandCenters(e.getPoint());
			Tuple2<String, String> selectedCity = cityButtons.getSelectedButton();
			if (selectedCity == null)
			{
				return;
			}

			String cityType = selectedCity.getFirst();
			String cityName = selectedCity.getSecond();
			for (Center center : selected)
			{
				CenterEdit cEdit = mainWindow.edits.centerEdits.get(center.index);
				CenterIcon cityIcon = new CenterIcon(CenterIconType.City, cityType, cityName);
				mainWindow.edits.centerEdits.put(center.index, cEdit.copyWithIcon(cityIcon));
			}
			updater.createAndShowMapIncrementalUsingCenters(selected);
		}
		else if (decorationsButton.isSelected())
		{
			if (isPress)
			{
				Tuple2<String, String> selectedButton = decorationButtons.getSelectedButton();
				if (selectedButton == null)
				{
					return;
				}

				String groupId = selectedButton.getFirst();
				String iconName = selectedButton.getSecond();
				nortantis.geom.Point point = getPointOnGraph(e.getPoint());
				FreeIcon icon = new FreeIcon(mainWindow.displayQualityScale, point, 1.0, IconType.decorations, groupId, iconName, null);
				mainWindow.edits.freeIcons.addOrReplace(icon);
				updater.createAndShowMapIncrementalUsingIcons(Arrays.asList(icon));
			}
		}
	}

	private void handleReplaceIcons(MouseEvent e)
	{
		if (treesButton.isSelected())
		{
			replaceTreesThatFailedToDrawDueToLowDensity(e);
		}
		List<FreeIcon> iconsSelectedAfter = new ArrayList<>();

		List<FreeIcon> iconsBeforeAndAfterOuter = mainWindow.edits.freeIcons.doWithLockAndReturnResult(() ->
		{
			List<FreeIcon> icons = getSelectedIcons(e.getPoint());
			if (icons.isEmpty())
			{
				return icons;
			}

			List<FreeIcon> iconsBeforeAndAfter = new ArrayList<>();

			for (FreeIcon before : icons)
			{
				iconsBeforeAndAfter.add(before);

				FreeIcon after;
				if (mountainsButton.isSelected())
				{
					after = before.copyWith(mountainTypes.getSelectedOption(), Math.abs(rand.nextInt()));
				}
				else if (hillsButton.isSelected())
				{
					after = before.copyWith(hillTypes.getSelectedOption(), Math.abs(rand.nextInt()));
				}
				else if (dunesButton.isSelected())
				{
					after = before.copyWith(duneTypes.getSelectedOption(), Math.abs(rand.nextInt()));
				}
				else if (treesButton.isSelected())
				{
					after = before.copyWith(treeTypes.getSelectedOption(), Math.abs(rand.nextInt()));
				}
				else if (citiesButton.isSelected())
				{
					Tuple2<String, String> selectedCity = cityButtons.getSelectedButton();
					if (selectedCity == null)
					{
						continue;
					}

					String cityType = selectedCity.getFirst();
					String cityName = selectedCity.getSecond();
					after = before.copyWith(cityType, cityName);
				}
				else if (decorationsButton.isSelected())
				{
					Tuple2<String, String> selectedDecoration = decorationButtons.getSelectedButton();
					if (selectedDecoration == null)
					{
						continue;
					}

					String type = selectedDecoration.getFirst();
					String iconName = selectedDecoration.getSecond();
					after = before.copyWith(type, iconName);
				}
				else
				{
					assert false;
					continue;
				}

				mainWindow.edits.freeIcons.replace(before, after);
				iconsBeforeAndAfter.add(after);
				if (isSelected(e.getPoint(), after))
				{
					iconsSelectedAfter.add(after);
				}
			}

			return iconsBeforeAndAfter;
		});

		mapEditingPanel.setHighlightedAreasFromIcons(updater.mapParts.iconDrawer, iconsSelectedAfter, false);

		if (iconsBeforeAndAfterOuter != null && !iconsBeforeAndAfterOuter.isEmpty())
		{
			updater.createAndShowMapIncrementalUsingIcons(iconsBeforeAndAfterOuter);
		}
	}

	private void handleEraseIcons(MouseEvent e)
	{
		if (allButton.isSelected() || treesButton.isSelected())
		{
			eraseTreesThatFailedToDrawDueToLowDensity(e);
		}

		List<FreeIcon> icons = mainWindow.edits.freeIcons.doWithLockAndReturnResult(() ->
		{
			List<FreeIcon> iconsInner = getSelectedIcons(e.getPoint());
			if (iconsInner.isEmpty())
			{
				return iconsInner;
			}

			mainWindow.edits.freeIcons.removeAll(iconsInner);
			return iconsInner;
		});

		mapEditingPanel.clearHighlightedAreas();

		if (icons != null && !icons.isEmpty())
		{
			updater.createAndShowMapIncrementalUsingIcons(icons);
		}
	}

	private void handleEditIcons(MouseEvent e, boolean isPress)
	{
		if (isPress)
		{
			if (iconToEdit != null)
			{
				isMoving = mapEditingPanel.isInMoveTool(e.getPoint());
				isScaling = mapEditingPanel.isInScaleTool(e.getPoint());
				if (isMoving || isScaling)
				{
					editStart = e.getPoint();
				}
				else
				{
					editStart = null;
				}
			}
			else
			{
				isMoving = false;
				isScaling = false;
				editStart = null;
			}

			if (!isMoving && !isScaling)
			{
				iconToEdit = getLowestSelectedIcon(e.getPoint());
				if (iconToEdit != null)
				{
					mapEditingPanel.showIconEditToolsAt(updater.mapParts.iconDrawer, iconToEdit);
					if (DebugFlags.printIconBeingEdited())
					{
						System.out.println("Selected icon for editing: " + iconToEdit);
					}
				}
				else
				{
					editStart = null;
					mapEditingPanel.clearIconEditTools();
					isMoving = false;
					isScaling = false;
				}
			}
		}
		else
		{
			if (iconToEdit != null && (isMoving || isScaling))
			{
				Point graphPointMouseLocation = getPointOnGraph(e.getPoint());
				Point graphPointMousePressedLocation = getPointOnGraph(editStart);
				Rectangle imageBounds = updater.mapParts.iconDrawer.toIconDrawTask(iconToEdit).createBounds();
				FreeIcon updated = null;

				if (isMoving)
				{
					double deltaX = (int) (graphPointMouseLocation.x - graphPointMousePressedLocation.x);
					double deltaY = (int) (graphPointMouseLocation.y - graphPointMousePressedLocation.y);
					imageBounds = imageBounds.translate(deltaX, deltaY);

					Point scaledOldLocation = iconToEdit.getScaledLocation(mainWindow.displayQualityScale);
					updated = iconToEdit.copyWithLocation(mainWindow.displayQualityScale,
							new Point(scaledOldLocation.x + deltaX, scaledOldLocation.y + deltaY));

				}
				else if (isScaling)
				{
					double scale = calcScale(graphPointMouseLocation, graphPointMousePressedLocation, imageBounds);
					imageBounds = imageBounds.scaleAboutCenter(scale);

					updated = iconToEdit.copyWithScale(iconToEdit.scale * scale);
				}

				if (updated != null)
				{
					boolean isValidPosition = updated.type == IconType.decorations || !updater.mapParts.iconDrawer.isContentBottomTouchingWater(updated);
					mapEditingPanel.showIconEditToolsAt(imageBounds, isValidPosition);
				}
			}
		}
		mapEditingPanel.repaint();

	}

	private double calcScale(Point graphPointMouseLocation, Point graphPointMousePressedLocation, Rectangle imageBounds)
	{
		double scale = graphPointMouseLocation.distanceTo(imageBounds.getCenter())
				/ graphPointMousePressedLocation.distanceTo(imageBounds.getCenter());

		final double minSize = 5;
		double minSideLength = Math.min(imageBounds.width, imageBounds.height);
		double minScale = minSize / minSideLength;
		return Math.max(scale, minScale);
	}

	private void handleFinishEditingIconIfNeeded(MouseEvent e)
	{
		if (iconToEdit != null && (isMoving || isScaling))
		{
			Point graphPointMouseLocation = getPointOnGraph(e.getPoint());
			Point graphPointMousePressedLocation = getPointOnGraph(editStart);

			Rectangle imageBounds = updater.mapParts.iconDrawer.toIconDrawTask(iconToEdit).createBounds();

			FreeIcon updated = null;
			if (isMoving)
			{
				double deltaX = (int) (graphPointMouseLocation.x - graphPointMousePressedLocation.x);
				double deltaY = (int) (graphPointMouseLocation.y - graphPointMousePressedLocation.y);
				Point scaledOldLocation = iconToEdit.getScaledLocation(mainWindow.displayQualityScale);
				FreeIcon updatedIcon = iconToEdit.copyWithLocation(mainWindow.displayQualityScale,
						new Point(scaledOldLocation.x + deltaX, scaledOldLocation.y + deltaY)).copyUnanchored();
				updated = updatedIcon;
				mainWindow.edits.freeIcons.doWithLock(() ->
				{
					mainWindow.edits.freeIcons.replace(iconToEdit, updatedIcon);
				});

				if (iconToEdit.centerIndex != null && !mainWindow.edits.freeIcons.hasTrees(iconToEdit.centerIndex))
				{
					// The user moved the last tree out of that polygon. Remove
					// the invisible CenterTree so that if someone resizes all
					// trees later, trees don't appear out of nowhere on this
					// Center.
					mainWindow.edits.centerEdits.put(iconToEdit.centerIndex,
							mainWindow.edits.centerEdits.get(iconToEdit.centerIndex).copyWithTrees(null));
				}
			}
			else if (isScaling)
			{
				double scale = calcScale(graphPointMouseLocation, graphPointMousePressedLocation, imageBounds);
				FreeIcon updatedIcon = iconToEdit.copyWithScale(iconToEdit.scale * scale);
				updated = updatedIcon;
				mainWindow.edits.freeIcons.doWithLock(() ->
				{
					mainWindow.edits.freeIcons.replace(iconToEdit, updatedIcon);
				});
			}

			if (updated != null)
			{
				undoer.setUndoPoint(UpdateType.Incremental, this);
				updater.createAndShowMapIncrementalUsingIcons(Arrays.asList(iconToEdit, updated));
				iconToEdit = updated;
				boolean isValidPosition = updated.type == IconType.decorations || !updater.mapParts.iconDrawer.isContentBottomTouchingWater(updated);
				if (isValidPosition)
				{
					mapEditingPanel.showIconEditToolsAt(updater.mapParts.iconDrawer, updated);
				}
				else
				{
					mapEditingPanel.clearIconEditTools();
				}
				isMoving = false;
				isScaling = false;
			}
			mapEditingPanel.repaint();
		}
	}

	public void unselectAnyIconBeingEdited()
	{
		if (modeWidget.isEditMode() && iconToEdit != null)
		{
			iconToEdit = null;
			isMoving = false;
			isScaling = false;
			mapEditingPanel.clearIconEditTools();
			mapEditingPanel.repaint();
		}
	}

	private void eraseTreesThatFailedToDrawDueToLowDensity(MouseEvent e)
	{
		Set<Center> selected = getSelectedLandCenters(e.getPoint());
		for (Center center : selected)
		{
			mainWindow.edits.centerEdits.put(center.index, mainWindow.edits.centerEdits.get(center.index).copyWithTrees(null));
		}
	}

	private void replaceTreesThatFailedToDrawDueToLowDensity(MouseEvent e)
	{
		Set<Center> selected = getSelectedLandCenters(e.getPoint());
		for (Center center : selected)
		{
			CenterTrees currentTrees = mainWindow.edits.centerEdits.get(center.index).trees;
			if (currentTrees != null)
			{
				CenterTrees newTrees = currentTrees.copyWithTreeType(treeTypes.getSelectedOption());
				mainWindow.edits.centerEdits.put(center.index, mainWindow.edits.centerEdits.get(center.index).copyWithTrees(newTrees));
			}
		}
	}

	private Set<Center> getSelectedLandCenters(java.awt.Point point)
	{
		Set<Center> selected = getSelectedCenters(point);
		return selected.stream().filter(c -> !c.isWater).collect(Collectors.toSet());
	}

	@Override
	protected void handleMousePressedOnMap(MouseEvent e)
	{
		handleMousePressOrDrag(e, true);
	}

	@Override
	protected void handleMouseReleasedOnMap(MouseEvent e)
	{
		if (modeWidget.isEditMode())
		{
			handleFinishEditingIconIfNeeded(e);
		}
		else
		{
			undoer.setUndoPoint(UpdateType.Incremental, this);
		}
	}

	@Override
	protected void handleMouseMovedOnMap(MouseEvent e)
	{
		if (modeWidget.isDrawMode() && !decorationsButton.isSelected())
		{
			highlightHoverCenters(e);
		}
		else
		{
			highlightHoverIconsAndShowBrush(e);
		}
		mapEditingPanel.repaint();
	}

	private void highlightHoverCenters(MouseEvent e)
	{
		mapEditingPanel.clearHighlightedAreas();
		mapEditingPanel.clearHighlightedCenters();

		Set<Center> selected = getSelectedCenters(e.getPoint());
		mapEditingPanel.addHighlightedCenters(selected);
		mapEditingPanel.setCenterHighlightMode(HighlightMode.outlineEveryCenter);
	}

	private void highlightHoverIconsAndShowBrush(MouseEvent e)
	{
		mapEditingPanel.clearHighlightedAreas();
		mapEditingPanel.clearHighlightedCenters();

		showOrHideBrush(e);

		if (modeWidget.isEditMode())
		{
			if (iconToEdit == null)
			{
				FreeIcon selected = getLowestSelectedIcon(e.getPoint());
				if (selected != null)
				{
					mapEditingPanel.setHighlightedAreasFromIcons(updater.mapParts.iconDrawer, Arrays.asList(selected),
							modeWidget.isEraseMode());
				}
			}
		}
		else if (!(modeWidget.isDrawMode() && decorationsButton.isSelected()))
		{
			List<FreeIcon> icons = getSelectedIcons(e.getPoint());
			mapEditingPanel.setHighlightedAreasFromIcons(updater.mapParts.iconDrawer, icons, modeWidget.isEraseMode());
		}
	}

	@Override
	protected void handleMouseDraggedOnMap(MouseEvent e)
	{
		if (modeWidget.isDrawMode() && !decorationsButton.isSelected())
		{
			highlightHoverCenters(e);
		}
		else
		{
			highlightHoverIconsAndShowBrush(e);
		}
		handleMousePressOrDrag(e, false);
	}

	@Override
	protected void handleMouseExitedMap(MouseEvent e)
	{
		mapEditingPanel.clearHighlightedCenters();
		mapEditingPanel.clearHighlightedAreas();
		mapEditingPanel.hideBrush();
		mapEditingPanel.repaint();
	}

	@Override
	protected void onBeforeShowMap()
	{
	}

	@Override
	protected void onAfterUndoRedo()
	{
		mapEditingPanel.clearHighlightedCenters();
		unselectAnyIconBeingEdited();
		mapEditingPanel.repaint();
	}

	private Set<Center> getSelectedCenters(java.awt.Point pointFromMouse)
	{
		return getSelectedCenters(pointFromMouse, getBrushDiameter());
	}

	protected List<FreeIcon> getSelectedIcons(java.awt.Point pointFromMouse)
	{
		int brushDiameter = getBrushDiameter();

		if (brushDiameter <= 1)
		{
			FreeIcon selected = getLowestSelectedIcon(pointFromMouse);
			if (selected != null)
			{
				return Arrays.asList(selected);
			}
			return Collections.emptyList();
		}
		else
		{
			return getMultipleSelectedIcons(pointFromMouse);
		}
	}

	private List<FreeIcon> getMultipleSelectedIcons(java.awt.Point pointFromMouse)
	{
		List<FreeIcon> selected = new ArrayList<>();
		mainWindow.edits.freeIcons.doWithLock(() ->
		{
			for (FreeIcon icon : mainWindow.edits.freeIcons)
			{
				if (isSelected(pointFromMouse, icon))
				{
					selected.add(icon);
				}
			}
		});

		return selected;
	}

	private boolean isSelected(java.awt.Point pointFromMouse, FreeIcon icon)
	{
		int brushDiameter = getBrushDiameter();
		Point graphPoint = getPointOnGraph(pointFromMouse);

		if (brushDiameter <= 1)
		{
			if (!isSelectedType(icon))
			{
				return false;
			}

			IconDrawTask task = updater.mapParts.iconDrawer.toIconDrawTask(icon);
			if (task == null)
			{
				return false;
			}

			Rectangle rect = task.createBounds();
			return rect.contains(graphPoint);
		}
		else
		{
			int brushRadius = (int) ((double) ((brushDiameter / mainWindow.zoom)) * mapEditingPanel.osScale) / 2;
			if (!isSelectedType(icon))
			{
				return false;
			}
			RotatedRectangle rect = new RotatedRectangle(updater.mapParts.iconDrawer.toIconDrawTask(icon).createBounds());
			return rect.overlapsCircle(graphPoint, brushRadius);
		}
	}

	protected FreeIcon getLowestSelectedIcon(java.awt.Point pointFromMouse)
	{
		List<FreeIcon> underMouse = getMultipleSelectedIcons(pointFromMouse);
		if (underMouse.isEmpty())
		{
			return null;
		}

		FreeIcon lowest = null;
		double lowestBottom = Double.NEGATIVE_INFINITY;

		for (FreeIcon icon : underMouse)
		{
			IconDrawTask task = updater.mapParts.iconDrawer.toIconDrawTask(icon);
			if (task != null)
			{
				double bottom = task.createBounds().getBottom();
				if (lowest == null || bottom > lowestBottom)
				{
					lowest = icon;
					lowestBottom = bottom;
				}
			}
		}
		return lowest;
	}

	private int getBrushDiameter()
	{
		if (brushSizeHider.isVisible())
		{
			return brushSizes.get(brushSizeComboBox.getSelectedIndex());
		}

		return brushSizes.get(0);
	}

	private boolean isSelectedType(FreeIcon icon)
	{
		if (mountainsButton.isSelected() && icon.type == IconType.mountains)
		{
			return true;
		}

		if (hillsButton.isSelected() && icon.type == IconType.hills)
		{
			return true;
		}

		if (dunesButton.isSelected() && icon.type == IconType.sand)
		{
			return true;
		}

		if (treesButton.isSelected() && icon.type == IconType.trees)
		{
			return true;
		}
		
		if (decorationsButton.isSelected() && icon.type == IconType.decorations)
		{
			return true;
		}

		if (citiesButton.isSelected() && icon.type == IconType.cities)
		{
			return true;
		}

		if (allButton.isSelected())
		{
			return true;
		}

		return false;
	}

	@Override
	public void loadSettingsIntoGUI(MapSettings settings, boolean isUndoRedoOrAutomaticChange, boolean changeEffectsBackgroundImages,
			boolean willDoImagesRefresh)
	{
		updateTypePanels();
		// Skip updating icon previews now if there will be an images refresh in
		// a moment, because that will handle it, and because the
		// ImageCache hasn't been cleared yet.
		if (changeEffectsBackgroundImages && !willDoImagesRefresh)
		{
			updateIconTypeButtonPreviewImages(settings);
		}
	}

	@Override
	public void getSettingsFromGUI(MapSettings settings)
	{
	}

	@Override
	public void handleEnablingAndDisabling(MapSettings settings)
	{
	}

	@Override
	public void onBeforeLoadingNewMap()
	{
	}

	@Override
	protected void onBeforeUndoRedo()
	{
	}
}
