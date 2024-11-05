package nortantis.swing;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileSystemView;

import org.apache.commons.io.FileUtils;

import nortantis.IconType;
import nortantis.MapSettings;
import nortantis.editor.UserPreferences;
import nortantis.util.AssetsPath;
import nortantis.util.FileHelper;
import nortantis.util.Logger;

@SuppressWarnings("serial")
public class CustomImagesDialog extends JDialog
{
	private JTextField customImagesFolderField;

	public CustomImagesDialog(MainWindow mainWindow, String currentCustomImagesPath, Consumer<String> storeResult)
	{
		super(mainWindow, "Custom Images Folder", Dialog.ModalityType.APPLICATION_MODAL);
		setSize(new Dimension(840, 680));
		JPanel content = new JPanel();
		add(content);
		content.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		content.setLayout(new BorderLayout());

		int space = 6;

		GridBagOrganizer organizer = new GridBagOrganizer();
		content.add(organizer.panel, BorderLayout.CENTER);
		organizer.addLeftAlignedComponent(new JLabel(
				"<html>自定义图片文件夹允许您使用自己的图片，而不是 Nortantis 内置的地图图片。"
				+ " 为此，请输入包含您图片的文件夹路径。如果该文件夹为空，Nortantis 将把其安装的图片复制到该文件夹作为起点。"
				+ " 所需的文件夹结构为： </html>"),
				space, space, false);

		int spaceBetweenPaths = 2;
		organizer.addLeftAlignedComponent(new JLabel("<自定义图片文件夹>" + File.separator + "边框" + File.separator
				+ "<边框类型>" + File.separator + "<边框图片>"), space, spaceBetweenPaths, false);
		organizer.addLeftAlignedComponent(new JLabel("<自定义图片文件夹>" + File.separator + "城市"
				+ File.separator + "<城市类型>" + File.separator + "<城市图片>"), spaceBetweenPaths, spaceBetweenPaths, false);
		organizer.addLeftAlignedComponent(new JLabel("<自定义图片文件夹>" + File.separator + "装饰"
				+ File.separator + "<装饰类型>" + File.separator + "<装饰图片>"), spaceBetweenPaths, spaceBetweenPaths, false);
		organizer.addLeftAlignedComponent(new JLabel("<自定义图片文件夹>" + File.separator + "山丘"
				+ File.separator + "<山丘类型>" + File.separator + "<山丘图片>"), spaceBetweenPaths, spaceBetweenPaths, false);
		organizer.addLeftAlignedComponent(new JLabel("<自定义图片文件夹>" + File.separator + "山脉"
				+ File.separator + "<山脉类型>" + File.separator + "<山脉图片>"), spaceBetweenPaths, spaceBetweenPaths, false);
		organizer.addLeftAlignedComponent(new JLabel("<自定义图片文件夹>" + File.separator + "沙子"
				+ File.separator + "<沙丘类型>" + File.separator + "<沙丘图片>"), spaceBetweenPaths, spaceBetweenPaths, false);
		organizer.addLeftAlignedComponent(new JLabel("<自定义图片文件夹>" + File.separator + "树木"
				+ File.separator + "<树木类型>" + File.separator + "<树木图片>"), spaceBetweenPaths, spaceBetweenPaths, false);

		organizer.addLeftAlignedComponent(new JLabel("<html>上述角括号中的名称是您可以配置为任何您想要的文件夹和文件名称。"
				+ " 但是没有角括号的文件夹名称必须与上述描述完全相同，否则 Nortantis 将忽略这些文件夹。"
				+ " 图片必须是 PNG 或 JPG 格式。推荐使用 PNG，因为它支持透明度且不会失真。</html>"), space, space, false);
		organizer.addLeftAlignedComponent(new JLabel("<html>有效的边框图片名称为 'upper_left_corner'、'upper_right_corner'、"
				+ "'lower_left_corner'、'lower_right_corner'、'top_edge'、'bottom_edge'、'left_edge'、'right_edge'。"
				+ " 至少必须提供一个角落和一个边缘。如果角落的宽度大于边缘的宽度，角落将被嵌入到地图中。</html>"),
				space, space, false);

		organizer.addLeftAlignedComponent(new JLabel("<html>关于树木图片，尽管 <树木类型> 文件夹可以有任何名称，"
				+ " 如果您希望新地图根据树木放置的生物群落适当地使用您的树木类型，请使用包含 'cacti'、'deciduous' 和 'pine' 的文件夹名称。</html>"), 
				space, space, false);

		organizer.addLeftAlignedComponent(new JLabel(
				"<html>如果您希望新地图在山脉周围添加山丘，则对于每种山脉类型，创建一个具有相同名称的山丘类型。</html>"),
				space, space, false);

		organizer.addLeftAlignedComponent(new JLabel("<html>在对自定义图片进行更改后，要使 Nortantis 看到这些更改，您"
				+ " 可以选择关闭并重新打开 Nortantis，或使用 " + mainWindow.getFileMenuName() + " -> "
				+ mainWindow.getRefreshImagesMenuName() + "。</html>"), space, space, false);
		organizer.addLeftAlignedComponent(
				new JLabel("<html>要恢复使用 Nortantis 的已安装图片，请清空下面的字段。</html>"), space, 10,
				false);

		JButton openButton = new JButton("打开");  // 汉化按钮文本


		customImagesFolderField = new JTextField();
		customImagesFolderField.getDocument().addDocumentListener(new DocumentListener()
		{

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				openButton.setEnabled(!customImagesFolderField.getText().isEmpty());
			}

			@Override
			public void insertUpdate(DocumentEvent e)
			{
				openButton.setEnabled(!customImagesFolderField.getText().isEmpty());
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				openButton.setEnabled(!customImagesFolderField.getText().isEmpty());
			}
		});
		customImagesFolderField.setText(FileHelper.replaceHomeFolderPlaceholder(currentCustomImagesPath));
		JButton browseButton = new JButton("浏览");
		browseButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				File folder = new File(customImagesFolderField.getText());
				if (!folder.exists())
				{
					folder = FileSystemView.getFileSystemView().getDefaultDirectory();
				}
				JFileChooser folderChooser = new JFileChooser(folder);
				folderChooser.setDialogTitle("选择文件夹");
				folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

				int returnValue = folderChooser.showOpenDialog(null);
				if (returnValue == JFileChooser.APPROVE_OPTION)
				{
					customImagesFolderField.setText(folderChooser.getSelectedFile().toString());
				}
			}
		});

		openButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				File folder = new File(customImagesFolderField.getText());
				if (!folder.exists())
				{
					JOptionPane.showMessageDialog(null, "无法打开" + folder.getAbsolutePath() + ". 文件夹不存在",
							"Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (!folder.isDirectory())
				{
					JOptionPane.showMessageDialog(null,
							"无法打开 " + folder.getAbsolutePath() + ". 该路径是一个文件，而不是文件夹.", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}

				if (Desktop.isDesktopSupported())
				{
					try
					{
						Desktop.getDesktop().open(folder);
					}
					catch (IOException ex)
					{
						ex.printStackTrace();
						Logger.printError("Error while trying to open custom images folder: ", ex);
					}
				}
			}
		});
		openButton.setEnabled(!customImagesFolderField.getText().isEmpty());

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		panel.add(new JLabel("Custom images folder:"));
		panel.add(Box.createHorizontalStrut(10));
		panel.add(customImagesFolderField);
		panel.add(Box.createHorizontalStrut(5));
		panel.add(browseButton);
		panel.add(Box.createHorizontalStrut(5));
		panel.add(openButton);

		organizer.addLeftAlignedComponent(panel, false);

		JCheckBox makeDefaultCheckbox = new JCheckBox("Make this the default for new random maps");
		organizer.addLeftAlignedComponent(makeDefaultCheckbox);


		organizer.addVerticalFillerRow();

		JPanel bottomPanel = new JPanel();
		content.add(bottomPanel, BorderLayout.SOUTH);
		bottomPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		JButton okayButton = new JButton("<html><u>O</u>K</html>");
		okayButton.setMnemonic(KeyEvent.VK_O);
		okayButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				boolean isChanged = !Objects.equals(customImagesFolderField.getText(),
						FileHelper.replaceHomeFolderPlaceholder(currentCustomImagesPath));
				if (mergeInstalledImagesIntoCustomFolderIfEmpty(customImagesFolderField.getText()))
				{
					JOptionPane.showMessageDialog(null,
							"Installed images successfully copied into " + Paths.get(customImagesFolderField.getText()).toAbsolutePath(),
							"Success", JOptionPane.INFORMATION_MESSAGE);
				}
				else if (MapSettings.isOldCustomImagesFolderStructure(customImagesFolderField.getText()))
				{
					try
					{
						MapSettings.convertOldCustomImagesFolder(customImagesFolderField.getText());

						JOptionPane.showMessageDialog(null, "Your custom images folder has been automatically converted to the new structure.",
								"Custom Images Folder Converted", JOptionPane.INFORMATION_MESSAGE);
					}
					catch (IOException ex)
					{
						String errorMessage = "Error while restructuring custom images folder for " + customImagesFolderField.getText() + ": " + ex.getMessage();
						Logger.printError(errorMessage, ex);
						JOptionPane.showMessageDialog(null, errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
					}
				}

				// If the custom images folder changed, then store the value, refresh images, and redraw the map.
				if (isChanged)
				{
					storeResult.accept(FileHelper.replaceHomeFolderWithPlaceholder(customImagesFolderField.getText()));
				}

				if (makeDefaultCheckbox.isSelected())
				{
					UserPreferences.getInstance().defaultCustomImagesPath = FileHelper
							.replaceHomeFolderWithPlaceholder(customImagesFolderField.getText());
				}

				dispose();
			}
		});
		bottomPanel.add(okayButton);

		JButton cancelButton = new JButton("<html><u>C</u>ancel</html>");
		cancelButton.setMnemonic(KeyEvent.VK_C);
		cancelButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				dispose();
			}
		});
		bottomPanel.add(cancelButton);
	}

	private boolean mergeInstalledImagesIntoCustomFolderIfEmpty(String customImagesFolder)
	{
		if (customImagesFolder == null || customImagesFolder.isEmpty())
		{
			return false;
		}

		File folder = new File(customImagesFolder);
		if (!folder.exists())
		{
			JOptionPane.showMessageDialog(null,
					"Unable to copy installed images into " + folder.getAbsolutePath() + ". The folder does not exist.", "Error",
					JOptionPane.ERROR_MESSAGE);
			return false;
		}
		else if (!folder.isDirectory())
		{
			JOptionPane.showMessageDialog(null,
					"Unable to copy installed images into " + folder.getAbsolutePath() + ". That path is a file, not a folder.", "Error",
					JOptionPane.ERROR_MESSAGE);
			return false;
		}

		boolean isFolderEmpty;
		try
		{
			isFolderEmpty = !Files.newDirectoryStream(folder.toPath()).iterator().hasNext();
		}
		catch (IOException ex)
		{
			JOptionPane.showMessageDialog(null, "Error while checking if " + folder.getAbsolutePath() + " is empty: " + ex.getMessage(),
					"Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		try
		{
			if (isFolderEmpty)
			{
				FileUtils.copyDirectoryToDirectory(Paths.get(AssetsPath.getInstallPath(), "borders").toFile(), folder);
				for (IconType type : IconType.values())
				{
					FileUtils.copyDirectoryToDirectory(Paths.get(AssetsPath.getInstallPath(), type.toString()).toFile(), folder);					
				}
				return true;
			}
		}
		catch (IOException ex)
		{
			JOptionPane.showMessageDialog(null,
					"Error while copying installed images into " + folder.getAbsolutePath() + ": " + ex.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
		}

		return false;
	}
}
