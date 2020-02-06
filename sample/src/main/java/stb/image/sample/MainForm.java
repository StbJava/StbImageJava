package stb.image.sample;

import stb.image.ColorComponents;
import stb.image.ImageResult;
import stb.image.sample.utility.Swing;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;

public class MainForm extends JFrame {
	public MainForm()
	{
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		setSize(1200, 800);

		// Main Menu
		JMenuBar mainMenu = new JMenuBar();

		// File
		JMenu menuFile = new JMenu("File");

		// Open
		JMenuItem menuItemOpen = new JMenuItem("Open");
		menuItemOpen.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				MainForm.this.onOpen();
			}
		});
		menuFile.add(menuItemOpen);
		mainMenu.add(menuFile);
		setJMenuBar(mainMenu);
	}

	private static List<FileFilter> getFileFilters() {
		List<FileFilter> result = new LinkedList<>();

		result.add(new FileNameExtensionFilter("JPEG Files(*.jpg)", "jpg"));
		result.add(new FileNameExtensionFilter("BMP Files(*.bmp)", "bmp"));

		return result;
	}

	private void onOpen() {
		String filePath = null;

		JFileChooser fc = new JFileChooser();

		for (FileFilter ff : getFileFilters()) {
			fc.addChoosableFileFilter(ff);
		}

		int dialogResult = fc.showOpenDialog(this);
		if (dialogResult == JFileChooser.APPROVE_OPTION) {
			Open(fc.getSelectedFile().getPath());
		}
	}

	private void Open(String filePath) {
		try {
			byte[] bytes = Files.readAllBytes(new File(filePath).toPath());
			ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
			ImageResult image = ImageResult.FromInputStream(stream, ColorComponents.RedGreenBlueAlpha, false);
			int k = 5;
		} catch (Exception ex) {
			Swing.showErrorMessageBox(this, ex.getMessage());
		}
	}


	public static void main(String[] args) throws ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException {
/*        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        Enumeration keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value != null && value instanceof FontUIResource)
                UIManager.put(key, Resources.DefaultFont);
        }*/

		MainForm frame = new MainForm();
		Swing.centreWindow(frame);
		frame.setVisible(true);
	}
}
