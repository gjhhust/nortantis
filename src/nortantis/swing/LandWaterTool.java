package nortantis.swing;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.border.EtchedBorder;

import nortantis.DebugFlags;
import nortantis.MapCreator;
import nortantis.MapSettings;
import nortantis.Region;
import nortantis.editor.CenterEdit;
import nortantis.editor.EdgeEdit;
import nortantis.editor.MapUpdater;
import nortantis.editor.RegionEdit;
import nortantis.graph.voronoi.Center;
import nortantis.graph.voronoi.Corner;
import nortantis.graph.voronoi.Edge;
import nortantis.graph.voronoi.VoronoiGraph;
import nortantis.platform.awt.AwtFactory;
import nortantis.util.AssetsPath;
import nortantis.util.Tuple2;

public class LandWaterTool extends EditorTool
{

	private JPanel colorDisplay;
	private RowHider colorChooserHider;

	private JRadioButton landButton;
	private JRadioButton oceanButton;
	private JRadioButton lakesButton;
	private JRadioButton riversButton;
	private RowHider riverOptionHider;
	private JSlider riverWidthSlider;
	private Corner riverStart;
	private RowHider modeHider;
	private JRadioButton fillRegionColorButton;
	private JRadioButton paintRegionButton;
	private JRadioButton mergeRegionsButton;
	private Region selectedRegion;
	private JToggleButton selectColorFromMapButton;

	private JComboBox<ImageIcon> brushSizeComboBox;
	private RowHider brushSizeHider;
	private RowHider selectColorHider;
	private JCheckBox onlyUpdateLandCheckbox;

	private JSlider hueSlider;
	private JSlider saturationSlider;
	private JSlider brightnessSlider;
	private boolean areRegionColorsVisible;
	private RowHider onlyUpdateLandCheckboxHider;
	private RowHider generateColorButtonHider;
	private RowHider colorGeneratorSettingsHider;
	private JPanel baseColorPanel;
	private ActionListener brushActionListener;
	private DrawModeWidget modeWidget;
	static String toolbarName = "Land and Water";
	static String colorGeneratorSettingsName = "Color Generator Settings";

	public LandWaterTool(MainWindow mainWindow, ToolsPanel toolsPanel, MapUpdater mapUpdater)
	{
		super(mainWindow, toolsPanel, mapUpdater);
	}

	@Override
	public String getToolbarName()
	{
		return toolbarName;
	}

	@Override
	public String getImageIconFilePath()
	{
		return Paths.get(AssetsPath.getInstallPath(), "internal/Land Water tool.png").toString();
	}

	@Override
	public void onBeforeSaving()
	{
	}

	@Override
	protected JPanel createToolOptionsPanel()
	{
		GridBagOrganizer organizer = new GridBagOrganizer();

		JPanel toolOptionsPanel = organizer.panel;
		toolOptionsPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));


		List<JComponent> radioButtons = new ArrayList<>();
		ButtonGroup group = new ButtonGroup();
		oceanButton = new JRadioButton("Ocean");
		group.add(oceanButton);
		radioButtons.add(oceanButton);
		brushActionListener = new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				mapEditingPanel.clearSelectedCenters();
				if (areRegionColorsVisible)
				{
					boolean isVisible = paintRegionButton.isSelected() || fillRegionColorButton.isSelected();
					colorChooserHider.setVisible(isVisible);
					selectColorHider.setVisible(isVisible);
					generateColorButtonHider.setVisible(isVisible);
					colorGeneratorSettingsHider.setVisible(isVisible);
					onlyUpdateLandCheckboxHider.setVisible(paintRegionButton.isSelected());
				}
				else
				{
					colorChooserHider.setVisible(false);
					selectColorHider.setVisible(false);
					generateColorButtonHider.setVisible(false);
					colorGeneratorSettingsHider.setVisible(false);
					onlyUpdateLandCheckbox.setVisible(false);
				}

				if (brushSizeComboBox != null)
				{
					brushSizeHider.setVisible(paintRegionButton.isSelected() || oceanButton.isSelected() || lakesButton.isSelected()
							|| landButton.isSelected() || (riversButton.isSelected() && modeWidget.isEraseMode()));
				}

				showOrHideRiverOptions();
			}
		};
		oceanButton.addActionListener(brushActionListener);

		lakesButton = new JRadioButton("湖泊");
		group.add(lakesButton);
		radioButtons.add(lakesButton);
		lakesButton.setToolTipText(
				"湖泊与海洋相同，但在海岸线上没有海洋效果（波浪或变暗），并且在启用时不会进行海岸线平滑处理。");
		lakesButton.addActionListener(brushActionListener);

		riversButton = new JRadioButton("河流");
		group.add(riversButton);
		radioButtons.add(riversButton);
		riversButton.addActionListener(brushActionListener);

		paintRegionButton = new JRadioButton("绘制区域");
		fillRegionColorButton = new JRadioButton("填充区域颜色");
		mergeRegionsButton = new JRadioButton("合并区域");
		landButton = new JRadioButton("土地");


		group.add(paintRegionButton);
		radioButtons.add(paintRegionButton);
		paintRegionButton.addActionListener(brushActionListener);


		group.add(fillRegionColorButton);
		radioButtons.add(fillRegionColorButton);
		fillRegionColorButton.addActionListener(brushActionListener);


		group.add(mergeRegionsButton);
		radioButtons.add(mergeRegionsButton);
		mergeRegionsButton.addActionListener(brushActionListener);

		group.add(landButton);
		radioButtons.add(landButton);
		landButton.addActionListener(brushActionListener);

		oceanButton.setSelected(true); // Selected by default
		organizer.addLabelAndComponentsVertical("刷子:", "", radioButtons);

		// River options
		{
			modeWidget = new DrawModeWidget("绘制河流", "清除河流", false, "", false, "",
					() -> brushActionListener.actionPerformed(null));
			modeHider = modeWidget.addToOrganizer(organizer, "绘制还是擦除河流");

			riverWidthSlider = new JSlider(1, 15);
			final int initialValue = 1;
			riverWidthSlider.setValue(initialValue);
			SwingHelper.setSliderWidthForSidePanel(riverWidthSlider);
			SliderWithDisplayedValue sliderWithDisplay = new SliderWithDisplayedValue(riverWidthSlider);
			riverOptionHider = sliderWithDisplay.addToOrganizer(organizer, "Width:",
					"绘制的河流宽度。 请注意，不同的宽度可能看起来一样，这取决于绘制地图的分辨率。");
		}

		// Color chooser
		colorDisplay = SwingHelper.createColorPickerPreviewPanel();
		colorDisplay.setBackground(Color.black);


		JButton chooseButton = new JButton("选择");
		chooseButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				cancelSelectColorFromMap();
				SwingHelper.showColorPickerWithPreviewPanel(toolOptionsPanel, colorDisplay, "区域颜色");
			}
		});
		colorChooserHider = organizer.addLabelAndComponentsHorizontal("Color:", "", Arrays.asList(colorDisplay, chooseButton),
				SwingHelper.colorPickerLeftPadding);


		selectColorFromMapButton = new JToggleButton("从地图上选择颜色");
		selectColorFromMapButton
				.setToolTipText("要选择现有区域的颜色，请单击此按钮，然后单击地图上的该区域。");
		selectColorHider = organizer.addLabelAndComponent("", "", selectColorFromMapButton, 0);


		JButton generateColorButton = new JButton("生成颜色");
		generateColorButton.setToolTipText("根据下面的随机生成设置生成新颜色。");
		generateColorButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				cancelSelectColorFromMap();
				Color newColor = AwtFactory
						.unwrap(MapCreator.generateColorFromBaseColor(new Random(), AwtFactory.wrap(baseColorPanel.getBackground()),
								hueSlider.getValue(), saturationSlider.getValue(), brightnessSlider.getValue()));
				colorDisplay.setBackground(newColor);
			}
		});
		generateColorButtonHider = organizer.addLabelAndComponent("", "", generateColorButton, 2);

		Tuple2<JComboBox<ImageIcon>, RowHider> brushSizeTuple = organizer.addBrushSizeComboBox(brushSizes);
		brushSizeComboBox = brushSizeTuple.getFirst();
		brushSizeHider = brushSizeTuple.getSecond();

		onlyUpdateLandCheckbox = new JCheckBox("仅更新现有土地");
		onlyUpdateLandCheckbox.setToolTipText("使绘制区域画笔在海洋中不创建新土地。");
		
		onlyUpdateLandCheckboxHider = organizer.addLabelAndComponent("", "", onlyUpdateLandCheckbox);


		colorGeneratorSettingsHider = organizer.addLeftAlignedComponent(createColorGeneratorOptionsPanel(toolOptionsPanel));


		showOrHideRegionColoringOptions();


		organizer.addHorizontalSpacerRowToHelpComponentAlignment(0.66);
		organizer.addVerticalFillerRow();
		return toolOptionsPanel;
	}

	private void showOrHideRiverOptions()
	{
		modeHider.setVisible(riversButton.isSelected());
		riverOptionHider.setVisible(riversButton.isSelected() && modeWidget.isDrawMode());
	}

	private JPanel createColorGeneratorOptionsPanel(JPanel toolOptionsPanel)
	{
		GridBagOrganizer organizer = new GridBagOrganizer();
		organizer.panel.setBorder(BorderFactory.createTitledBorder(new EtchedBorder(EtchedBorder.LOWERED), colorGeneratorSettingsName));


		baseColorPanel = SwingHelper.createColorPickerPreviewPanel();
		final JButton baseColorChooseButton = new JButton("选择");
		baseColorChooseButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent arg0)
			{
				SwingHelper.showColorPicker(toolOptionsPanel, baseColorPanel, "基础颜色", () ->
				{
				});
			}
		});
		organizer.addLabelAndComponentsHorizontal("基础颜色:",
				"用于生成新区域颜色的基础颜色。当不着色区域时，这是地图的土地颜色。",
				Arrays.asList(baseColorPanel, baseColorChooseButton), SwingHelper.borderWidthBetweenComponents);
		

		hueSlider = new JSlider();
		hueSlider.setPaintTicks(true);
		hueSlider.setPaintLabels(true);
		hueSlider.setMinorTickSpacing(20);
		hueSlider.setMajorTickSpacing(100);
		hueSlider.setMaximum(360);
		organizer.addLabelAndComponent("色调范围:",
				"生成区域颜色的色调值的可能范围。该范围以基础颜色的色调为中心。", hueSlider);
		
		saturationSlider = new JSlider();
		saturationSlider.setPaintTicks(true);
		saturationSlider.setPaintLabels(true);
		saturationSlider.setMinorTickSpacing(20);
		saturationSlider.setMaximum(255);
		saturationSlider.setMajorTickSpacing(100);
		organizer.addLabelAndComponent("饱和度范围:",
				"生成区域颜色的饱和度值的可能范围。该范围以土地颜色的饱和度为中心。",
				saturationSlider);
		
		brightnessSlider = new JSlider();
		brightnessSlider.setPaintTicks(true);
		brightnessSlider.setPaintLabels(true);
		brightnessSlider.setMinorTickSpacing(20);
		brightnessSlider.setMaximum(255);
		brightnessSlider.setMajorTickSpacing(100);
		organizer.addLabelAndComponent("亮度范围:",
				"生成区域颜色的亮度值的可能范围。该范围以土地颜色的亮度为中心。",
				brightnessSlider);
		
		return organizer.panel;
				
	}

	private void showOrHideRegionColoringOptions()
	{
		paintRegionButton.setVisible(areRegionColorsVisible);
		fillRegionColorButton.setVisible(areRegionColorsVisible);
		mergeRegionsButton.setVisible(areRegionColorsVisible);
		landButton.setVisible(!areRegionColorsVisible);

		colorChooserHider.setVisible(areRegionColorsVisible);
		selectColorHider.setVisible(areRegionColorsVisible);

		if (areRegionColorsVisible && landButton.isSelected())
		{
			paintRegionButton.setSelected(true);
		}
		else if (!areRegionColorsVisible
				&& (paintRegionButton.isSelected() || mergeRegionsButton.isSelected() || fillRegionColorButton.isSelected()))
		{
			landButton.setSelected(true);
		}
		brushActionListener.actionPerformed(null);
	}

	@Override
	protected void handleMouseClickOnMap(MouseEvent e)
	{
	}

	private void handleMousePressOrDrag(MouseEvent e, boolean isMouseDrag)
	{
		if (mergeRegionsButton.isSelected() && isMouseDrag)
		{
			return;
		}

		highlightHoverCentersOrEdgesAndBrush(e);

		if (oceanButton.isSelected() || lakesButton.isSelected())
		{
			Set<Center> selected = getSelectedCenters(e.getPoint());
			boolean hasChange = false;
			for (Center center : selected)
			{
				CenterEdit edit = mainWindow.edits.centerEdits.get(center.index);
				for (Edge edge : center.borders)
				{
					EdgeEdit eEdit = mainWindow.edits.edgeEdits.get(edge.index);
					if (eEdit.riverLevel > VoronoiGraph.riversThisSizeOrSmallerWillNotBeDrawn)
					{
						eEdit.riverLevel = 0;
					}
				}
				hasChange |= !edit.isWater;
				hasChange |= edit.isLake != lakesButton.isSelected();
				// Note that I'm nulling out trees in the assignment below because any trees that failed to draw previously should be
				// cleared out when the Center becomes water.
				mainWindow.edits.centerEdits.put(edit.index,
						new CenterEdit(edit.index, true, lakesButton.isSelected(), edit.regionId, edit.icon, null));
			}
			if (hasChange)
			{
				handleMapChange(selected);
			}
		}
		else if (paintRegionButton.isSelected())
		{
			if (selectColorFromMapButton.isSelected())
			{
				selectColorFromMap(e);
			}
			else
			{
				Set<Center> selected = getSelectedCenters(e.getPoint());

				boolean hasChange = false;
				for (Center center : selected)
				{
					CenterEdit edit = mainWindow.edits.centerEdits.get(center.index);
					if (onlyUpdateLandCheckbox.isSelected() && edit.isWater)
					{
						continue;
					}
					hasChange |= edit.isWater;
					Integer newRegionId = getOrCreateRegionIdForEdit(center, colorDisplay.getBackground());
					hasChange |= (edit.regionId == null) || newRegionId != edit.regionId;
					mainWindow.edits.centerEdits.put(edit.index,
							new CenterEdit(edit.index, false, false, newRegionId, edit.icon, edit.trees));
				}
				if (hasChange)
				{
					handleMapChange(selected);
				}
			}
		}
		else if (landButton.isSelected())
		{
			Set<Center> selected = getSelectedCenters(e.getPoint());
			boolean hasChange = false;
			for (Center center : selected)
			{
				CenterEdit edit = mainWindow.edits.centerEdits.get(center.index);
				// Still need to add region IDs to edits because the user might switch to region editing later.
				Integer newRegionId = getOrCreateRegionIdForEdit(center, mainWindow.getLandColor());
				hasChange |= (edit.regionId == null) || newRegionId != edit.regionId;
				hasChange |= edit.isWater;
				mainWindow.edits.centerEdits.put(edit.index, new CenterEdit(edit.index, false, false, newRegionId, edit.icon, edit.trees));
			}
			if (hasChange)
			{
				handleMapChange(selected);
			}

		}
		else if (fillRegionColorButton.isSelected())
		{
			if (selectColorFromMapButton.isSelected())
			{
				selectColorFromMap(e);
			}
			else
			{
				Center center = updater.mapParts.graph.findClosestCenter(getPointOnGraph(e.getPoint()));
				if (center != null)
				{
					Region region = center.region;
					if (region != null)
					{
						RegionEdit edit = mainWindow.edits.regionEdits.get(region.id);
						edit.color = AwtFactory.wrap(colorDisplay.getBackground());
						Set<Center> regionCenters = region.getCenters();
						handleMapChange(regionCenters);
					}
				}
			}
		}
		else if (mergeRegionsButton.isSelected())
		{
			Center center = updater.mapParts.graph.findClosestCenter(getPointOnGraph(e.getPoint()));
			if (center != null)
			{
				Region region = center.region;
				if (region != null)
				{
					if (selectedRegion == null)
					{
						selectedRegion = region;
						mapEditingPanel.addSelectedCenters(selectedRegion.getCenters());
					}
					else
					{
						if (region == selectedRegion)
						{
							// Cancel the selection
							selectedRegion = null;
							mapEditingPanel.clearSelectedCenters();
						}
						else
						{
							// Loop over edits instead of region.getCenters() because the region assigned to centers is changed by map
							// drawing, but edits is thread safe.
							for (CenterEdit c : mainWindow.edits.centerEdits.values())
							{
								assert c != null;
								if (c.regionId != null && c.regionId == region.id)
								{
									CenterEdit newValues = new CenterEdit(c.index, c.isWater, c.isLake, selectedRegion.id, c.icon, c.trees);
									mainWindow.edits.centerEdits.put(c.index, newValues);
								}

							}
							mainWindow.edits.regionEdits.remove(region.id);
							selectedRegion = null;
							mapEditingPanel.clearSelectedCenters();
							handleMapChange(region.getCenters());
						}
					}
				}
			}
		}
		else if (riversButton.isSelected())
		{
			if (modeWidget.isDrawMode())
			{
				return;
			}
			else
			{
				// When deleting rivers with the single-point brush size,
				// highlight the closest edge instead of a polygon.
				Set<Edge> possibleRivers = getSelectedEdges(e.getPoint(), brushSizes.get(brushSizeComboBox.getSelectedIndex()));
				Set<Edge> changed = new HashSet<>();
				for (Edge edge : possibleRivers)
				{
					EdgeEdit eEdit = mainWindow.edits.edgeEdits.get(edge.index);
					if (eEdit.riverLevel > 0)
					{
						eEdit.riverLevel = 0;
						changed.add(edge);
					}
				}
				mapEditingPanel.clearHighlightedEdges();
				updater.createAndShowMapIncrementalUsingEdges(changed);
			}
		}
	}

	private void selectColorFromMap(MouseEvent e)
	{
		Center center = updater.mapParts.graph.findClosestCenter(getPointOnGraph(e.getPoint()));
		if (center != null)
		{
			if (center != null && center.region != null)
			{
				colorDisplay.setBackground(AwtFactory.unwrap(center.region.backgroundColor));
				selectColorFromMapButton.setSelected(false);
			}
		}
	}

	private void cancelSelectColorFromMap()
	{
		if (selectColorFromMapButton.isSelected())
		{
			selectColorFromMapButton.setSelected(false);
			selectedRegion = null;
			mapEditingPanel.clearSelectedCenters();
		}
	}

	private Set<Center> getSelectedCenters(java.awt.Point point)
	{
		return getSelectedCenters(point, brushSizes.get(brushSizeComboBox.getSelectedIndex()));
	}

	private int getOrCreateRegionIdForEdit(Center center, Color color)
	{
		// If a neighboring center has the desired region color, then use that region.
		for (Center neighbor : center.neighbors)
		{
			CenterEdit neighborEdit = mainWindow.edits.centerEdits.get(neighbor.index);
			if (neighborEdit.regionId != null
					&& AwtFactory.unwrap(mainWindow.edits.regionEdits.get(neighborEdit.regionId).color).equals(color))
			{
				return neighborEdit.regionId;
			}
		}

		// Find the closest region of that color.
		Optional<CenterEdit> opt = mainWindow.edits.centerEdits.values().stream()
				.filter(cEdit1 -> cEdit1.regionId != null
						&& AwtFactory.unwrap(mainWindow.edits.regionEdits.get(cEdit1.regionId).color).equals(color))
				.min((cEdit1, cEdit2) -> Double.compare(updater.mapParts.graph.centers.get(cEdit1.index).loc.distanceTo(center.loc),
						updater.mapParts.graph.centers.get(cEdit2.index).loc.distanceTo(center.loc)));
		if (opt.isPresent())
		{
			return opt.get().regionId;
		}
		else
		{
			int largestRegionId;
			if (mainWindow.edits.regionEdits.isEmpty())
			{
				largestRegionId = -1;
			}
			else
			{
				largestRegionId = mainWindow.edits.regionEdits.values().stream().max((r1, r2) -> Integer.compare(r1.regionId, r2.regionId))
						.get().regionId;
			}

			int newRegionId = largestRegionId + 1;

			RegionEdit regionEdit = new RegionEdit(newRegionId, AwtFactory.wrap(color));
			mainWindow.edits.regionEdits.put(newRegionId, regionEdit);

			return newRegionId;
		}
	}

	private void handleMapChange(Set<Center> centers)
	{
		updater.createAndShowMapIncrementalUsingCenters(centers);
	}

	@Override
	protected void handleMousePressedOnMap(MouseEvent e)
	{
		handleMousePressOrDrag(e, false);

		if (riversButton.isSelected() && modeWidget.isDrawMode())
		{
			riverStart = updater.mapParts.graph.findClosestCorner(getPointOnGraph(e.getPoint()));
		}
	}

	@Override
	protected void handleMouseReleasedOnMap(MouseEvent e)
	{
		if (riversButton.isSelected() && modeWidget.isDrawMode())
		{
			Corner end = updater.mapParts.graph.findClosestCorner(getPointOnGraph(e.getPoint()));
			Set<Edge> river = filterOutOceanAndCoastEdges(updater.mapParts.graph.findPathGreedy(riverStart, end));
			for (Edge edge : river)
			{
				int base = (riverWidthSlider.getValue() - 1);
				int riverLevel = (base * base * 2) + VoronoiGraph.riversThisSizeOrSmallerWillNotBeDrawn + 1;
				mainWindow.edits.edgeEdits.get(edge.index).riverLevel = riverLevel;
			}
			riverStart = null;
			mapEditingPanel.clearHighlightedEdges();
			mapEditingPanel.repaint();

			if (DebugFlags.printRiverEdgeIndexes())
			{
				System.out.println("河流边缘索引：");
				for (Edge edge : river)
				{
					System.out.println(edge.index);
				}
			}

			if (river.size() > 0)
			{
				updater.createAndShowMapIncrementalUsingEdges(river);
			}
		}

		undoer.setUndoPoint(UpdateType.Incremental, this);
	}

	private Set<Edge> filterOutOceanAndCoastEdges(Set<Edge> edges)
	{
		return edges.stream().filter(e -> (e.d0 == null || !e.d0.isWater) && (e.d1 == null || !e.d1.isWater)).collect(Collectors.toSet());
	}

	@Override
	protected void handleMouseMovedOnMap(MouseEvent e)
	{
		highlightHoverCentersOrEdgesAndBrush(e);
	}

	protected void highlightHoverCentersOrEdgesAndBrush(MouseEvent e)
	{
		mapEditingPanel.clearHighlightedCenters();
		mapEditingPanel.clearHighlightedEdges();
		mapEditingPanel.hideBrush();

		if (oceanButton.isSelected() || lakesButton.isSelected() || paintRegionButton.isSelected() && !selectColorFromMapButton.isSelected()
				|| landButton.isSelected())
		{
			Set<Center> selected = getSelectedCenters(e.getPoint());

			if (DebugFlags.printCenterIndexes())
			{
				System.out.println("突出显示中心索引：");
				for (Center center : selected)
				{
					System.out.println(center.index);
				}
			}

			mapEditingPanel.addHighlightedCenters(selected);
			mapEditingPanel.setCenterHighlightMode(HighlightMode.outlineEveryCenter);
		}
		else if (paintRegionButton.isSelected() && selectColorFromMapButton.isSelected() || mergeRegionsButton.isSelected()
				|| fillRegionColorButton.isSelected())
		{
			Center center = updater.mapParts.graph.findClosestCenter(getPointOnGraph(e.getPoint()), true);
			if (center != null)
			{
				if (center.region != null)
				{
					mapEditingPanel.addHighlightedCenters(center.region.getCenters());
				}
				mapEditingPanel.setCenterHighlightMode(HighlightMode.outlineGroup);
			}
		}
		else if (riversButton.isSelected() && modeWidget.isEraseMode())
		{
			int brushDiameter = brushSizes.get(brushSizeComboBox.getSelectedIndex());
			if (brushDiameter > 1)
			{
				mapEditingPanel.showBrush(e.getPoint(), brushDiameter, true);
			}
			Set<Edge> candidates = getSelectedEdges(e.getPoint(), brushDiameter);

			for (Edge edge : candidates)
			{
				EdgeEdit eEdit = mainWindow.edits.edgeEdits.get(edge.index);
				if (eEdit.riverLevel > VoronoiGraph.riversThisSizeOrSmallerWillNotBeDrawn)
				{
					mapEditingPanel.addHighlightedEdge(edge, true);
				}
			}
		}

		mapEditingPanel.repaint();
	}

	@Override
	protected void handleMouseDraggedOnMap(MouseEvent e)
	{
		if (riversButton.isSelected() && modeWidget.isDrawMode())
		{
			if (riverStart != null)
			{
				mapEditingPanel.clearHighlightedEdges();
				Corner end = updater.mapParts.graph.findClosestCorner(getPointOnGraph(e.getPoint()));
				Set<Edge> river = filterOutOceanAndCoastEdges(updater.mapParts.graph.findPathGreedy(riverStart, end));
				mapEditingPanel.addHighlightedEdges(river, false);
				mapEditingPanel.repaint();
			}
		}
		else
		{
			handleMousePressOrDrag(e, true);
		}
	}

	@Override
	protected void handleMouseExitedMap(MouseEvent e)
	{
		mapEditingPanel.clearHighlightedCenters();
		if (riversButton.isSelected() && modeWidget.isEraseMode())
		{
			mapEditingPanel.clearHighlightedEdges();
		}
		mapEditingPanel.hideBrush();
		mapEditingPanel.repaint();
	}

	@Override
	protected void onBeforeShowMap()
	{
	}

	@Override
	public void onSwitchingAway()
	{
		if (selectedRegion != null)
		{
			selectedRegion = null;
			mapEditingPanel.clearSelectedCenters();
		}
	}

	@Override
	protected void onAfterUndoRedo()
	{
		selectedRegion = null;
		mapEditingPanel.clearSelectedCenters();
		mapEditingPanel.clearHighlightedCenters();
		mapEditingPanel.repaint();
	}

	@Override
	public void loadSettingsIntoGUI(MapSettings settings, boolean isUndoRedoOrAutomaticChange, boolean changeEffectsBackgroundImages,
			boolean willDoImagesRefresh)
	{
		areRegionColorsVisible = settings.drawRegionColors;

		// These settings are part of MapSettings, so they get pulled in by undo/redo, but I exclude them here
		// because it feels weird to me to have them change with undo/redo since they don't directly affect the map.
		if (!isUndoRedoOrAutomaticChange)
		{
			baseColorPanel.setBackground(AwtFactory.unwrap(settings.regionBaseColor));
			hueSlider.setValue(settings.hueRange);
			saturationSlider.setValue(settings.saturationRange);
			brightnessSlider.setValue(settings.brightnessRange);

			// I'm setting this color here because I only want it to change when you create new settings or load settings from a file,
			// not on undo/redo or in response to the Theme panel changing.
			colorDisplay.setBackground(AwtFactory.unwrap(settings.regionBaseColor));
		}

		showOrHideRegionColoringOptions();
	}

	@Override
	public void getSettingsFromGUI(MapSettings settings)
	{
		settings.regionBaseColor = AwtFactory.wrap(baseColorPanel.getBackground());
		settings.hueRange = hueSlider.getValue();
		settings.saturationRange = saturationSlider.getValue();
		settings.brightnessRange = brightnessSlider.getValue();
	}

	@Override
	public void handleEnablingAndDisabling(MapSettings settings)
	{
		// There's nothing to do because this tool never disables anything.
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
