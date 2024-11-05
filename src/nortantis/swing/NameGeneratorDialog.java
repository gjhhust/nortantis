package nortantis.swing;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Arrays;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import nortantis.MapSettings;
import nortantis.NameCreator;
import nortantis.NotEnoughNamesException;
import nortantis.editor.NameType;
import nortantis.util.Range;

@SuppressWarnings("serial")
public class NameGeneratorDialog extends JDialog
{
	private JTextArea textBox;
	final int numberToGenerate = 50;

	public NameGeneratorDialog(MainWindow mainWindow, MapSettings settings)
	{
		super(mainWindow, "名称生成器", Dialog.ModalityType.APPLICATION_MODAL);
		setSize(new Dimension(500, 810));

		JPanel contents = new JPanel();
		contents.setLayout(new BorderLayout());
		getContentPane().add(contents);

		GridBagOrganizer organizer = new GridBagOrganizer();
		contents.add(organizer.panel, BorderLayout.CENTER);
		ButtonGroup buttonGroup = new ButtonGroup();
		JRadioButton personNameRadioButton = new JRadioButton("人名");
		buttonGroup.add(personNameRadioButton);
		JRadioButton placeNameRadioButton = new JRadioButton("地名");
		buttonGroup.add(placeNameRadioButton);
		organizer.addLabelAndComponentsHorizontal("名称类型:", "", Arrays.asList(personNameRadioButton, placeNameRadioButton));

		final String beginsWithLabel = "以...开始:";
		JTextField beginsWith = new JTextField();
		organizer.addLabelAndComponent(beginsWithLabel, "限制生成的名称以给定字母开头。", beginsWith);

		final String endsWithLabel = "以...结束:";
		JTextField endsWith = new JTextField();
		organizer.addLabelAndComponent(endsWithLabel, "限制生成的名称以这些字母结尾。", endsWith, 0);

		personNameRadioButton.setSelected(true);

		BooksWidget booksWidget = new BooksWidget(true, null);
		booksWidget.checkSelectedBooks(settings.books);
		organizer.addLeftAlignedComponentWithStackedLabel("用于生成名称的书籍:", "选择的书籍将用于生成名称。",
				booksWidget.getContentPanel(), GridBagOrganizer.rowVerticalInset, 2, true, 0.2);

		JPanel generatePanel = new JPanel();
		generatePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		JButton generateButton = new JButton("<html><u>生</u>成名称<html>"); // 生成按钮
		generatePanel.add(generateButton);
		generateButton.setMnemonic(KeyEvent.VK_G);
		organizer.addLeftAlignedComponent(generatePanel, 0, 0, false);
		generateButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (!beginsWith.getText().chars().allMatch(Character::isLetter)
						|| !endsWith.getText().chars().allMatch(Character::isLetter))
				{
					String message = beginsWithLabel.replace(":", "") + " 和 " + endsWithLabel.replace(":", "")
							+ " 必须只包含字母。";
					JOptionPane.showMessageDialog(NameGeneratorDialog.this, message, "错误", JOptionPane.ERROR_MESSAGE);
					return;
				}

				MapSettings settingsToUse = settings.deepCopy();
				settingsToUse.books = booksWidget.getSelectedBooks();
				settingsToUse.textRandomSeed = System.currentTimeMillis();
				NameCreator nameCreator = new NameCreator(settingsToUse);
				NameType type = personNameRadioButton.isSelected() ? NameType.Person : NameType.Place;
				textBox.setText(generateNamesForType(numberToGenerate, type, beginsWith.getText(), endsWith.getText(), nameCreator));
				textBox.setCaretPosition(0);
			}
		});

		textBox = new JTextArea(numberToGenerate, 30);
		JScrollPane textBoxScrollPane = new JScrollPane(textBox);
		organizer.addLeftAlignedComponentWithStackedLabel("生成的名称:", "", textBoxScrollPane, true, 0.8);

		JPanel bottomButtonsPanel = new JPanel();
		contents.add(bottomButtonsPanel, BorderLayout.SOUTH);
		bottomButtonsPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

		JButton doneButton = new JButton("<html><u>D</u>one</html>");
		doneButton.setMnemonic(KeyEvent.VK_D);
		doneButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				settings.books = booksWidget.getSelectedBooks();
				mainWindow.loadSettingsAndEditsIntoThemeAndToolsPanels(settings, false, false);
				mainWindow.updater.reprocessBooks();
				dispose();
			}
		});
		bottomButtonsPanel.add(doneButton);
	}

	private String generateNamesForType(int numberToGenerate, NameType type, String requiredPrefix, String requiredSuffix,
			NameCreator nameCreator)
	{
		final int maxAttempts = 100000;
		String names = "";

		for (@SuppressWarnings("闲置")
		int i : new Range(numberToGenerate))
		{
			String name = "";
			int attemptCount = 0;
			while (true)
			{
				try
				{
					if (type == NameType.Person)
					{
						name = nameCreator.generatePersonName("%s", true, requiredPrefix);
					}
					else
					{
						name = nameCreator.generatePlaceName("%s", true, requiredPrefix);
					}
					if (requiredSuffix == null || requiredSuffix.equals("") || name.toLowerCase().endsWith(requiredSuffix.toLowerCase()))
					{
						if (!name.contains(" "))
						{
							break;
						}
					}
				}
				catch (NotEnoughNamesException ex)
				{
					if (requiredSuffix.length() > 0)
					{
						return names + (names.isEmpty() ? "" : "\n")
								+ "错误：无法使用给定的书籍和请求的后缀生成足够的名称。"
								+ "请尝试添加更多书籍或移除或减少后缀。";
					}
					else if (requiredPrefix.length() > 0)
					{
						return names + (names.isEmpty() ? "" : "\n")
								+ "错误：无法使用给定的书籍和要求的前缀生成足够的名称。"
								+ "请尝试包含更多书籍或移除或减少前缀。";
					}
					return names + (names.isEmpty() ? "" : "\n") + "错误：无法生成足够的名称。请尝试包含更多书籍。";
				}

				catch (Exception ex)
				{
					return names + (names.isEmpty() ? "" : "\n") + "Error: " + ex.getMessage();
				}

				attemptCount++;
				if (attemptCount >= maxAttempts)
				{
					return names + (names.isEmpty() ? "" : "\n") + "无法根据给定的限制条件生成足够的名称。"
							+ "尝试使用更多的书籍或减少后缀。";
				}
			}
			names = names + (names.isEmpty() ? "" : "\n") + name;
		}

		return names;
	}
}
