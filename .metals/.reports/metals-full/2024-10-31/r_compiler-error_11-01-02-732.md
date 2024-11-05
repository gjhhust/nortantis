file:///E:/TRPG/fu/地图/nortantis/src/nortantis/swing/AboutDialog.java
### java.util.NoSuchElementException: next on empty iterator

occurred in the presentation compiler.

presentation compiler configuration:


action parameters:
uri: file:///E:/TRPG/fu/地图/nortantis/src/nortantis/swing/AboutDialog.java
text:
```scala
package nortantis.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import nortantis.MapSettings;
import nortantis.platform.awt.AwtFactory;
import nortantis.util.AssetsPath;
import nortantis.util.ImageHelper;
import nortantis.util.Logger;

@SuppressWarnings("serial")
public class AboutDialog extends JDialog
{
	public AboutDialog(MainWindow mainWindow)
	{
		super(mainWindow, "About Nortantis", Dialog.ModalityType.APPLICATION_MODAL);
		setResizable(false);
		setLayout(new BorderLayout());
		JPanel content = new JPanel();
		add(content, BorderLayout.CENTER);
		content.setLayout(new BorderLayout());
		content.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		BufferedImage nortantisImage = AwtFactory.unwrap(ImageHelper
				.read(Paths.get(AssetsPath.getInstallPath(), "internal", "taskbar icon medium size.png").toString()));
		content.add(new ImagePanel(nortantisImage), BorderLayout.WEST);

		JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
		rightPanel.setPreferredSize(new Dimension(nortantisImage.getWidth(), nortantisImage.getHeight()));
		JLabel text = new JLabel("<html>" + "Nortantis version " + MapSettings.currentVersion + "" + "<html>");
		rightPanel.add(text);

		rightPanel.add(new JLabel(" "));

		rightPanel.add(new JLabel("<html>如果您遇到错误并希望报告，请在 Nortantis 项目的 GitHub 问题追踪器中提交： </html>"));
		rightPanel.add(createHyperlink("github.com/jeheydorn/nortantis/issues", "https://github.com/jeheydorn/nortantis/issues"));

		rightPanel.add(new JLabel(" "));
		rightPanel.add(new JLabel("<html>如果您喜欢 Nortantis 并希望支持它，同时喜欢干净、快乐的奇幻浪漫小说，请考虑购买我在以下网址列出的书籍：</html>"));
		rightPanel.add(createHyperlink("jandjheydorn.com/", "https://jandjheydorn.com/"));

		rightPanel.add(Box.createVerticalGlue());

		content.add(rightPanel, BorderLayout.EAST);

		JPanel bottomPanel = new JPanel();
		content.add(bottomPanel, BorderLayout.SOUTH);
		bottomPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		JButton closeButton = new JButton("<html><u>C</u>lose</html>");
		closeButton.setMnemonic(KeyEvent.VK_C);
		closeButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				dispose();
			}
		});
		bottomPanel.add(closeButton);

		pack();
	}

	private JLabel createHyperlink(String text, String URL)
	{
		JLabel link = new JLabel(text);
		link.setForeground(new Color(26, 113, 228));
		link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		link.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				try
				{
					Desktop.getDesktop().browse(new URI(URL));
				}
				catch (IOException | URISyntaxException ex)
				{
					Logger.printError("Error while trying to open URL: " + URL, ex);
				}
			}
		});
		return link;
	}
}

```



#### Error stacktrace:

```
scala.collection.Iterator$$anon$19.next(Iterator.scala:973)
	scala.collection.Iterator$$anon$19.next(Iterator.scala:971)
	scala.collection.mutable.MutationTracker$CheckedIterator.next(MutationTracker.scala:76)
	scala.collection.IterableOps.head(Iterable.scala:222)
	scala.collection.IterableOps.head$(Iterable.scala:222)
	scala.collection.AbstractIterable.head(Iterable.scala:935)
	dotty.tools.dotc.interactive.InteractiveDriver.run(InteractiveDriver.scala:164)
	dotty.tools.pc.MetalsDriver.run(MetalsDriver.scala:45)
	dotty.tools.pc.WithCompilationUnit.<init>(WithCompilationUnit.scala:31)
	dotty.tools.pc.SimpleCollector.<init>(PcCollector.scala:345)
	dotty.tools.pc.PcSemanticTokensProvider$Collector$.<init>(PcSemanticTokensProvider.scala:63)
	dotty.tools.pc.PcSemanticTokensProvider.Collector$lzyINIT1(PcSemanticTokensProvider.scala:63)
	dotty.tools.pc.PcSemanticTokensProvider.Collector(PcSemanticTokensProvider.scala:63)
	dotty.tools.pc.PcSemanticTokensProvider.provide(PcSemanticTokensProvider.scala:88)
	dotty.tools.pc.ScalaPresentationCompiler.semanticTokens$$anonfun$1(ScalaPresentationCompiler.scala:109)
```
#### Short summary: 

java.util.NoSuchElementException: next on empty iterator